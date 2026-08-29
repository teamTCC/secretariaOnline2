# Guia de implementação — Workflow Engine (frontend)

**Público:** equipe de frontend-web (React 18 + TypeScript + TanStack Query).  
**Escopo:** implementar wizard, lista, detalhe, deliberação, secretaria e admin **sem reler o módulo Kotlin**.  
**Não implementar neste documento:** código `.tsx`, rotas, hooks. Só o contrato.

**Princípio diretor:** *uma engine, N tipos, três telas*. Adicionar tipo = publicar JSON no banco. **Nunca** criar `SegundaChamadaForm.tsx` / `TrancamentoPage.tsx`.

**Contrato as-built (2026-08):** `foundationDocs/analysis/as-built-backend.md` — cookies HttpOnly (JSON **sem** JWT), `_links` **sempre** `Map<String,String>`, GET em `RequestQuery`, publish grava `request_type_version` (V019), deep-link `POST /auth/ott`.

**Fontes de verdade (código, não HUs):**

- `backend/modules/solicitacoes/api/RequestController.kt` (HTTP fino)
- `backend/modules/solicitacoes/application/RequestQuery.kt` (GET lista/detalhe/types + montagem de `_links`)
- `backend/modules/solicitacoes/api/RequestAttachmentController.kt`
- `backend/modules/solicitacoes/api/AdminRequestTypeController.kt`
- `backend/modules/solicitacoes/api/dto/SolicitacoesRequests.kt`
- `backend/modules/solicitacoes/api/dto/SolicitacoesResponses.kt`
- `backend/modules/solicitacoes/api/SolicitacoesExceptionHandler.kt`
- `backend/modules/iam/api/AuthController.kt` (`POST /auth/ott`)

**Este arquivo é a cópia canônica** para o time de front. Ponteiro em `tutor/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md`.

**Base URL:** sem `context-path` — paths abaixo são a raiz da API (`http://localhost:8080/requests/...`). CSRF: `GET /auth/csrf` + header `X-XSRF-TOKEN` em POST/PATCH/DELETE autenticados. Cookies HttpOnly (`credentials: 'include'`). JSON de login/ott **nunca** traz tokens.

---

## 1. Princípio diretor

| Regra | Consequência na UI |
|-------|-------------------|
| Uma engine, N tipos | Um `DynamicForm` lê `form_schema`. Zero `if (tipoCode === 'SEGUNDA_CHAMADA')`. |
| UI cega a perfil | Botões **só** se o `rel` existir em `_links`. Proibido `userRole === 'SECRETARIO'` e `estado === 'EM_DELIBERACAO'` para mostrar ActionBar. |
| Validação dupla | Zod no cliente (UX). O servidor **sempre** revalida `dados` vs `form_schema` no `POST /requests` e `POST /{id}/submit`. 422 RFC 7807 é a fonte de verdade. |
| HATEOAS | `useActions(resource)` decide o que renderizar. Transições vêm de `WorkflowEngine.allowedTransitions(estado, authorities)`. |

Rascunho (`POST /requests/draft` e `PATCH /{id}/draft`) **não** valida `form_schema`. Submit e open **validam**.

---

## 2. Mapa de telas → APIs

Rotas de UI alinhadas a `agents/frontend-engineer.md` e telas Figma F1.7–F1.9.

| Rota UI | Método | Path real | Authority | HU / Figma |
|---------|--------|-----------|-----------|------------|
| `/solicitacoes` | GET | `/requests?estado=&idCurso=&typeCode=&type=&page=&size=` | `request.view_own` **ou** `request.view_curso` **ou** `request.deliberate` | US-F1-005 / F1.7 |
| `/solicitacoes/nova` passo 1 | GET | `/requests/types` | autenticado | F1.8 |
| `/solicitacoes/nova` passo 1 (schema) | GET | `/requests/types/{code}` | autenticado; tipo deve estar `ativo=true` | F1.8 |
| `/solicitacoes/nova` rascunho | POST | `/requests/draft` | `request.open` | F1.8-D05 |
| `/solicitacoes/nova` atualizar rascunho | PATCH | `/requests/{id}/draft` | `request.open` | F1.8 |
| `/solicitacoes/nova` submeter | POST | `/requests/{id}/submit` | `request.open` | F1.8 |
| `/solicitacoes/nova` abrir direto | POST | `/requests` | `request.open` (ou `request.open_on_behalf`) | F1.8-D04 |
| Lookup disciplinas | GET | `/academico/disciplinas?idCurso=&search=` **ou** `/academico/cursos/{cursoId}/disciplinas?search=` | autenticado | widget `entity-select` (seed `x-ui.endpoint`) |
| Cursos (wizard) | GET | `/academico/cursos` | autenticado | F1.8 |
| `/solicitacoes/:id` | GET | `/requests/{id}` | view_own / view_curso / deliberate | F1.9 |
| Timeline | GET | `/requests/{id}/events` | idem | F1.9 |
| Anexos lista | GET | `/requests/{id}/attachments` | `request.view_own` ou `request.view_curso` | F1.9 |
| Protocolo | GET | `/requests/{id}/protocol` | view_own / view_curso | F1.9 |
| Público protocolo | GET | `/publico/solicitacoes/{ano}/{numero}` | anônimo | F0 |
| Transição | POST | `/requests/{id}/transitions` | autenticado; FGAC no engine | F3.4 / F5.4 |
| Bulk | PATCH | `/requests/bulk-deliberate` | `request.deliberate` **ou** `image_authorization.review` | F5.2-D04 |
| Nova interna | POST | `/requests` + `idSolicitanteOnBehalf` | `request.open_on_behalf` | F5.3 |
| Presign órfão (wizard) | POST | `/requests/attachments/presigned-url` | `request.open` | F1.8-D03 |
| Presign vinculado | POST | `/requests/{id}/attachments/upload-url` | `request.open` ou `request.deliberate` | F1.8 |
| Confirm anexo | POST | `/requests/{id}/attachments/confirm` | idem | F1.8 |
| Download | GET | `/requests/{id}/attachments/{attachmentId}/download-url` | view_own / view_curso | F1.9 |
| Apagar anexo | DELETE | `/requests/{id}/attachments/{attachmentId}` | `request.open` + dono | F1.8 |
| Admin catálogo | GET | `/request-types` | `request_type.manage` / `system.admin` / `request.view_curso` | F7.4 |
| Admin criar | POST | `/request-types` | `request_type.manage` ou `system.admin` | F7.4 |
| Admin editar | PATCH | `/request-types/{id}` | idem | F7.4 |
| Admin publicar | POST | `/request-types/{id}/publish` | idem | F7.4 |
| Admin excluir | DELETE | `/request-types/{id}` | idem | F7.4 |
| Dashboard aluno | GET | `/bff/dashboard/aluno` | dashboard aluno | F1.1 |
| Dashboard professor | GET | `/bff/dashboard/professor` | `dashboard.view_self_professor` | F3.1 |
| Dashboard secretaria | GET | `/bff/dashboard/secretaria` | `dashboard.view_secretary` | F5.1 |

**Deliberação:** não há path REST separado `/solicitacoes/:id/deliberar`. A tela reusa `GET /requests/{id}` + `POST /requests/{id}/transitions`. Rota UI sugerida: `/solicitacoes/:id` com `DeliberationPanel` se `_links` tiver `defer` / `deny` / `request-adjustment`.

---

## 3. Contratos JSON

### 3.1 `form_schema` (JSON Schema Draft-07 + `x-ui`)

Raiz obrigatória no publish: `"type": "object"` + `"properties"`. Extensões usadas no seed:

| Chave | Onde | Uso no front |
|-------|------|----------------|
| `title` | property | label |
| `enum` | property | opções do `select` |
| `format`: `uuid` / `date` | property | entity-select / date-picker |
| `minLength`, `minimum`, `maximum`, `minItems` | property | Zod + mensagens |
| `x-ui.widget` | property (ou no array) | registry de widgets |
| `x-ui.endpoint` | `entity-select` | URL de lookup — seed usa `/academico/disciplinas` (**existe** alias as-built; ver §6) |
| `x-ui.rows` | textarea | altura |
| `x-required-attachments` | raiz do schema | categorias obrigatórias no open/submit |

### 3.2 `dados` (payload da instância)

Objeto JSONB. Campos de tabela = **arrays de objetos**. Não há tabela `request_line_item` no Flyway (V001–V019).

Exemplo `TRANCAMENTO_DISCIPLINA` (seed `V011`):

```json
{
  "disciplinas": [
    { "idDisciplina": "01932e8a-0000-7000-8000-000000000001" }
  ],
  "justificativa": "Conflito de horário com estágio obrigatório no período vespertino."
}
```

Exemplo `ADIANTAMENTO_PERIODO` (seed `V017`, widget `multi-select-table`):

```json
{
  "semestre": "2026/2",
  "justificativa": "Preciso antecipar disciplinas do 5º período por motivo de mobilidade.",
  "disciplinasDesejadas": [
    { "idDisciplina": "01932e8a-0000-7000-8000-000000000002" }
  ]
}
```

### 3.3 `workflow_json` (o que o front precisa)

O front **não** interpreta a máquina de estados para botões. Use só:

- `initial` — estado após open (quase sempre `ABERTA`; rascunho é `RASCUNHO` independente do `initial`)
- `states` — mapa de badge/cor (DS)
- Detalhe já traz `estado` calculado

Transições relevantes no seed completo:

| `action` | `rel` HATEOAS | Authority típica |
|----------|---------------|------------------|
| `ASSIGN` | `assign` | `request.deliberate` |
| `FORWARD_TO_DELIBERATOR` | `forward-to-deliberator` | `request.deliberate` |
| `DEFER` | `defer` | `request.deliberate` |
| `DENY` | `deny` | `request.deliberate` |
| `REQUEST_ADJUSTMENT` | `request-adjustment` | `request.deliberate` |
| `RESUBMIT` | `resubmit` | `request.open` + guard solicitante |
| `REQUEST_REVIEW` | `request-review` | `request.open` + `allowsReview()` (INDEFERIDA + ≤ 5 dias) |

Geração do `rel` no backend: `action.lowercase().replace('_', '-')` em `RequestQuery.getById`.

### 3.4 Tipos seedados (19)

`ADIANTAMENTO_PERIODO`, `APROVEITAMENTO_DISCIPLINA`, `TRANCAMENTO_DISCIPLINA`, `TRANCAMENTO_PERIODO`, `COLACAO_SEM_SOLENIDADE`, `REVISAO_NOTA`, `SEGUNDA_CHAMADA`, `INCLUSAO_DISCIPLINA`, `EXCLUSAO_DISCIPLINA`, `MATRICULA_DISCIPLINA_ISOLADA`, `MATRICULA_DISCIPLINA_ELETIVA`, `APROVEITAMENTO_ESTAGIO`, `APROVEITAMENTO_ATIVIDADE_COMPLEMENTAR`, `JUSTIFICATIVA_FALTA`, `DECLARACAO_MATRICULA`, `HISTORICO_ESCOLAR`, `DIPLOMA`, `AUTORIZACAO_IMAGEM`, `ATESTADO_FREQUENCIA`.

Smoke rápido: `DECLARACAO_MATRICULA` (form simples). Tabela: `TRANCAMENTO_DISCIPLINA` / `ADIANTAMENTO_PERIODO`. Anexos obrigatórios: `SEGUNDA_CHAMADA` (`ATESTADO_MEDICO`).

---

## 4. HATEOAS

**Um formato só** (as-built 2026-08): `_links` é `Map<String,String>` em **todos** os DTOs de solicitação (lista, detalhe, types, create, draft, protocol, transition). **Não** há HAL `EntityModel` `{ rel: { href } }` neste módulo.

```json
{
  "_links": {
    "self": "/requests/{id}",
    "events": "/requests/{id}/events",
    "attachments": "/requests/{id}/attachments",
    "defer": "/requests/{id}/transitions"
  }
}
```

Dashboard BFF: `_links` do envelope também são strings. Itens de pendência usam `_link` (singular, string) — **não** `_links.acao.href`.

```ts
function hrefOf(links: Record<string, string> | undefined, rel: string): string | undefined {
  return links?.[rel]
}
```

**Nunca** assuma que o `rel` da transição é uma URL de tela. O valor de `defer` aponta para **o mesmo** `POST /requests/{id}/transitions`; o `rel` identifica a `action` (converter `defer` → `DEFER` com `rel.replace(/-/g, '_').toUpperCase()`).

### 4.3 Tabela `rel` → UI

| rel | Rótulo sugerido | Método | Body |
|-----|-----------------|--------|------|
| `self` | (navegação) | GET | — |
| `events` | Timeline | GET | — |
| `attachments` | Anexos | GET | — |
| `open` | Abrir | POST | `OpenRequestDto` |
| `save-draft` | Salvar rascunho | POST | `OpenRequestDto` |
| `submit` | Enviar | POST | vazio |
| `update-draft` | Atualizar rascunho | PATCH | `{ "dados": { ... } }` |
| `upload-url` | Anexar arquivo | POST | `GenerateAttachmentUploadUrlDto` |
| `assign` | Assumir / triagem | POST `/transitions` | `{ "action": "ASSIGN" }` |
| `forward-to-deliberator` | Encaminhar | POST `/transitions` | `{ "action": "FORWARD_TO_DELIBERATOR" }` |
| `defer` | Deferir | POST `/transitions` | `{ "action": "DEFER", "parecer": "..." }` |
| `deny` | Indeferir | POST `/transitions` | `{ "action": "DENY", "parecer": "..." }` |
| `request-adjustment` | Pedir ajuste | POST `/transitions` | `{ "action": "REQUEST_ADJUSTMENT", "parecer": "..." }` |
| `resubmit` | Reenviar | POST `/transitions` | `{ "action": "RESUBMIT" }` |
| `request-review` | Pedir revisão | POST `/transitions` | `{ "action": "REQUEST_REVIEW" }` |
| `bulk_deliberate` | (lista, se ABERTA + authority) | PATCH | ver §10 |
| `public` | Consulta anônima | GET | — |

Rels de rascunho (`submit`, `update-draft`, `upload-url`) só aparecem no detalhe se `estado == RASCUNHO` **e** o usuário é o solicitante. `upload-url` também em `ABERTA` / `EM_AJUSTE` para o dono.

Lista: `bulk_deliberate` só se o ator tem `request.deliberate` ou `image_authorization.review` **e** o item está `ABERTA`.

---

## 5. Wizard 3 passos (F1.8)

### Passo 1 — Tipo

1. `GET /requests/types` → cards (`id`, `code`, `descricao`, `prazoDias`, `formSchema`).
2. Ao selecionar: `GET /requests/types/{code}` para `formSchema` + `workflowJson` completos (tipos inativos → 400).
3. Guardar `idRequestType` (UUID) e `idCurso` (`GET /academico/cursos`).

### Passo 2 — Formulário + anexos

- Render `DynamicForm` a partir de `formSchema`.
- Anexos **antes** de existir request: `POST /requests/attachments/presigned-url` (órfão, prefixo `requests/orphan/`).
- Autosave servidor: `POST /requests/draft` (201) depois `PATCH /requests/{id}/draft`. Schema **não** é validado.
- Rascunho local (`localStorage`) é opcional (FE-09) e **não substitui** o draft servidor se o usuário troca de dispositivo.

### Passo 3 — Review (`ReviewSummary`)

Reusar o mesmo `formSchema` em modo read-only + `dados`. Não inventar labels.

### Submissão

**Opção A (recomendado HU):** draft → anexos vinculados `POST /{id}/attachments/upload-url` + `confirm` → `POST /{id}/submit`.

**Opção B:** `POST /requests` com `dados` (+ `attachments` inline). Create **não** devolve `estado`/`tipoCode` — só `{ id, _links.self }`. Busque o detalhe em seguida.

### Payloads reais (`OpenRequestDto`)

```json
{
  "idRequestType": "0193a0c0-0000-7000-8000-000000000010",
  "idCurso": "0193a0c0-0000-7000-8000-000000000020",
  "dados": { "finalidade": "BOLSA", "observacoes": "..." },
  "attachments": [],
  "idSolicitanteOnBehalf": null
}
```

`PATCH /{id}/draft`:

```json
{ "dados": { "finalidade": "BOLSA", "observacoes": "Completado." } }
```

Create 201:

```json
{ "id": "0193a0c0-…", "_links": { "self": "/requests/0193a0c0-…" } }
```

Draft 201:

```json
{
  "id": "…",
  "estado": "RASCUNHO",
  "_links": {
    "self": "/requests/{id}",
    "submit": "/requests/{id}/submit",
    "update-draft": "/requests/{id}/draft",
    "upload-url": "/requests/{id}/attachments/upload-url"
  }
}
```

Submit 200: `{ "id", "estado": "ABERTA", "protocolo": "2026/0002", "_links": { "self": "…" } }`.

### 422 schema (`SolicitacoesExceptionHandler`)

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/schema-validation-error",
  "title": "Payload inválido segundo o form_schema",
  "status": 422,
  "detail": "Os dados enviados não estão de acordo com o esquema do tipo de solicitação.",
  "erros": ["$.finalidade: is missing but it is required"],
  "timestamp": "2026-08-29T14:00:00Z"
}
```

`erros` é `string[]` (mensagens networknt), **não** `{campo, mensagem}`. Mapear para o passo 2 (toast + destacar campos se o path JSON Pointer estiver na string). Anexos obrigatórios geram `"Anexo obrigatório ausente: ATESTADO_MEDICO"`.

Validação Jakarta (`@Valid`) usa outro 422: `title: "Dados inválidos"` e `erros: [{ "campo", "mensagem" }]` (`GlobalExceptionHandler`).

---

## 6. DynamicForm / widgets

Um único registry. Chave: `property["x-ui"]?.widget` ou fallback por `type`/`format`/`enum`.

| Widget | Shape em `dados` | Contrato extra |
|--------|------------------|----------------|
| (default string) | `string` | input |
| `textarea` | `string` | `x-ui.rows` opcional |
| `select` | `string` (valor do `enum`) | opções = `enum` |
| `entity-select` | `string` UUID | lookup — ver abaixo |
| `multi-select-table` | `array` de objetos | colunas = `items.properties` |
| `date-picker` | `string` `YYYY-MM-DD` (`format: date`) | |
| `file-upload` | **não** vai em `dados` | fluxo de anexos §8; categoria no confirm |

### `entity-select` — seed vs API

Seeds usam `"endpoint": "/academico/disciplinas"` (às vezes `?enrolled=true` / `?tipo=ELETIVA`).

**Paths reais (ambos 200):**

- `GET /academico/disciplinas?idCurso=&search=` — alias as-built; `enrolled`/`tipo` são **aceitos e ignorados** (não 400).
- `GET /academico/cursos/{cursoId}/disciplinas?search=` — mesmo `PageResponse` `{ id, codigo, nome, creditos }`.

Preferir o endpoint do schema; passar `idCurso` do wizard quando filtrar. Exibir `codigo — nome`; gravar só o `id` (UUID).

Não criar um form por `RequestType`.

---

## 7. Tabela dinâmica (`multi-select-table`)

- Schema: `type: array` + `items.properties` = colunas + `x-ui.widget: multi-select-table` (no array).
- Runtime: `dados.<campo> = [ { ...colunas }, ... ]`.
- `minItems` no schema (ex.: 1) é validado no servidor no open/submit.
- Célula com `entity-select` reusa o widget do §6.
- **Não** há tabela `request_line_item` no Flyway (V001–V019). Linhas vivem em `dados` JSONB. Não esperar IDs de linha.

Seed de referência: `TRANCAMENTO_DISCIPLINA.disciplinas`, `ADIANTAMENTO_PERIODO.disciplinasDesejadas` (`V011` / `V017`).

---

## 8. Anexos

Dois presigns, mesmo body (`GenerateAttachmentUploadUrlDto`):

| Quando | Path |
|--------|------|
| Wizard sem request | `POST /requests/attachments/presigned-url` → `storageKey` `requests/orphan/{uuid}_{filename}` |
| Request/rascunho já existe | `POST /requests/{id}/attachments/upload-url` → `requests/{id}/{uuid}_{filename}` |

Body:

```json
{
  "filename": "atestado.pdf",
  "contentType": "application/pdf",
  "sha256": "64 hex (cliente; o servidor recalcula no confirm)",
  "sizeBytes": 204800,
  "categoria": "ATESTADO_MEDICO"
}
```

Resposta: `{ "uploadUrl": "https://minio…", "storageKey": "…" }`.

Fluxo:

1. Presign.
2. `PUT` bytes na `uploadUrl` com o mesmo `Content-Type`.
3. SHA-256 hex 64 chars no cliente (`crypto.subtle` / equivalente).
4. `POST /requests/{id}/attachments/confirm` com `AttachmentInputDto`.

Confirm body:

```json
{
  "storageKey": "requests/orphan/…_atestado.pdf",
  "sha256": "a1b2…64hex",
  "nomeOriginal": "atestado.pdf",
  "contentType": "application/pdf",
  "categoria": "ATESTADO_MEDICO",
  "tamanhoBytes": 204800
}
```

201 → `AttachmentResponse` (`id`, `categoria`, `nomeOriginal`, `contentType`, `tamanhoBytes`, `storageKey`, `sha256`, `createdAt`).

### Limites já aplicados no backend (`AttachmentPolicy`)

- Allowlist: `application/pdf`, `image/jpeg`, `image/png`, `image/webp`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Máximo **20 MB**
- Estado modificável: `RASCUNHO`, `ABERTA`, `EM_AJUSTE`
- SHA-256 **recalculado no MinIO** (hex, case-insensitive)
- `storageKey` só `requests/orphan/…` ou `requests/{esteId}/…`
- Delete: só o solicitante, mesmos estados → 204
- Download URL: TTL 15 min

Falhas de política (`require`) → **400** (`title: "Dados inválidos"`). Hash divergente / objeto ausente → 400. Outro aluno → **403**.

Inline `attachments` no `POST /requests` existe, mas o httpie marca como legado (não verifica existência no MinIO da mesma forma que o confirm). Preferir confirm.

`x-required-attachments` no schema: open e submit exigem que cada categoria exista nos anexos persistidos (ou no array inline do open).

---

## 9. Lista e detalhe

### Lista `GET /requests`

Query: `estado`, `idCurso`, `typeCode` **ou** alias `type`, `page` (default size 20).

Aluno só com `view_own`: o servidor **força** filtro pelo próprio `userId` (não há query `solicitante=me`).

Colunas **fixas** (não vêm do `form_schema`):

| Campo JSON | UI |
|------------|-----|
| `protocolo` | Número (`2026/0001`) |
| `tipoCode` | Tipo |
| `estado` | Badge |
| `prazoEm` | Prazo / SLA (calcular atraso no cliente; **não** há campo `slaStatus`) |
| `idSolicitante` | (staff) |
| `_links.self` | detalhe |
| `_links.bulk_deliberate` | checkbox de lote (só ABERTA + authority) |

Envelope `PageResponse`:

```json
{
  "content": [ { "id", "numeroAnual", "ano", "protocolo", "tipoCode", "estado", "prazoEm", "idSolicitante", "_links": { "self": "…" } } ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1 },
  "_links": { "self": "…?page=0&size=20", "first": "…", "last": "…", "next": null, "prev": null }
}
```

### Detalhe `GET /requests/{id}` (`RequestDetailResponse`)

Campos: `id`, `numeroAnual`, `ano`, `protocolo`, `tipoCode`, `tipoDescricao`, `estado`, `idSolicitante`, `dados`, **`formSchema`** (snapshot da versão da instância, V019), `idRequestTypeVersion`, `parecer`, `prazoEm`, `concludedAt`, `createdAt`, `_links` (mapa string).

Render read-only: `DynamicForm` disabled com `formSchema` + `dados` (FE-12). Sem round-trip extra para o schema. Não misturar com o `formSchema` atual de `GET /requests/types/{code}` se o admin republicou o tipo.

Timeline: `GET /requests/{id}/events` → `[{ tipo, estadoAnterior, estadoNovo, parecer, createdAt }]` ASC.

Anexos: `GET .../attachments` (não vêm embutidos no detalhe).

Protocolo: `GET .../protocol` inclui `_links.public` → `/publico/solicitacoes/{ano}/{numero}` (número **sem** pad na URL pública).

---

## 10. Deliberação e secretaria

### Transição unitária

```json
POST /requests/{id}/transitions
{ "action": "DEFER", "parecer": "Deferido conforme documentação." }
```

200: `{ "mensagem": "Transição 'DEFER' aplicada com sucesso.", "estadoNovo": "DEFERIDA", "_links": { "self": "…" } }`.

Action inválida no estado → **422** `title: "Transição inválida"`. Guard (ex. RESUBMIT de outro usuário) → **403** `title: "Condição não satisfeita"`. Sem `@PreAuthorize` de capability no POST (é `isAuthenticated()`); a engine recusa por authority da transição.

Rate limit: **20/min por sessão** nesse path → **429** + `Retry-After` + `retryAfterSeconds`.

### Bulk

```json
PATCH /requests/bulk-deliberate
{
  "ids": ["uuid-1", "uuid-2"],
  "action": "DEFER",
  "parecer": "Lote HTTPie"
}
```

200: `{ "processados": 2, "action": "DEFER" }`. Falha em qualquer item → **409** (`ResponseStatusException CONFLICT`) e rollback (`@Transactional` all-or-nothing).

Para `AUTORIZACAO_IMAGEM`, a authority alternativa `image_authorization.review` também autoriza o bulk.

### Nova interna (`onBehalfOf`)

Campo: `idSolicitanteOnBehalf` (UUID do aluno) no `POST /requests`. Sem `request.open_on_behalf` → **400**. O `idSolicitante` persistido é o do aluno.

### Deep-link JWT (`?ott=`)

Após transições com `generateOneTimeToken: true` no `workflow_json` (ex. `FORWARD_TO_DELIBERATOR` em `SEGUNDA_CHAMADA`), o outbox monta:

`{frontendUrl}/solicitacoes/{requestId}?ott={jwt}`

- TTL 3 dias; audience `request:{requestId}`; JTI one-shot.
- **As-built:** `POST /auth/ott` `{ "token": "<jwt>" }` (`ExchangeOttUseCase`) — `permitAll`, CSRF ignore, rate limit igual ao login. 200 = mesmo JSON do login (`mustChangePassword`, `mustAcceptLgpd`) + cookies de sessão. Replay → 401.

O que a tela deve fazer:

1. Se `?ott=` na URL: `POST /auth/ott` com o token (sem CSRF).
2. Sucesso → cookies gravados → `navigate(/solicitacoes/:id)` e **apagar** o query param.
3. **Não** enviar o OTT como `Authorization: Bearer` (não é access token).
4. Se o exchange falhar (401): mensagem genérica + ir para `/login` (o deep-link já foi consumido ou expirou).

### BFF (contagens reais)

- Aluno: `ultimasSolicitacoes`, `pendencias` (`_link` → `/requests/{id}`).
- Professor: `solicitacoesPendentes[]` com `_link`.
- Secretaria: `kpis.emTriagem`, `kpis.emDeliberacao`.

---

## 11. Admin tipos (`/request-types`)

Não confundir com `GET /requests/types` (catálogo **publicado** para o wizard).

| Ação | Semântica MVP |
|------|----------------|
| POST | Cria com `ativo=false` (rascunho). Code uppercased, unique. |
| PATCH `/{id}` | Atualiza `descricao`, `formSchema`, `workflowJson`, `prazoDias`. **Não** revalida schema (só o publish). Emite audit `request_type.update`. |
| POST `/{id}/publish` | Valida estrutura do schema e do workflow. Seta `ativo=true`. Grava snapshot imutável em `request_type_version` (V019). Audit `request_type.publish`. |
| DELETE | 204 se `count(request)=0`; senão 400. |

Body `UpsertRequestTypeDto`:

```json
{
  "code": "ATESTADO_MATRICULA_TESTE",
  "descricao": "…",
  "prazoDias": 5,
  "formSchema": { "type": "object", "properties": { }, "required": [] },
  "workflowJson": { "initial": "ABERTA", "states": ["ABERTA", "DEFERIDA"], "transitions": [] }
}
```

Resposta: `{ id, code, descricao, formSchema, workflowJson, prazoDias, ativo }`.

Cliente: preview com o mesmo `DynamicForm`. Publish inválido → 400 (`IllegalArgumentException`), não 422.

**As-built V019:** tabela `request_type_version` + `request.id_request_type_version`. Open/draft carimbam a versão vigente. GET detalhe usa o `form_schema` **da versão da instância**, não o tipo “ao vivo”. Continua **sem** enum `DRAFT`/`PUBLISHED` — equivalente: `ativo=false|true`.

Três painéis (F7.4): lista (inclui rascunhos) | editor JSON schema + workflow | preview. Após publish, o tipo aparece em `GET /requests/types` **sem redeploy**.

---

## 12. Erros → UX

Todos (exceto 429 custom do filtro) são RFC 7807 `ProblemDetail`: `type`, `title`, `status`, `detail`, `timestamp`.

| HTTP | Quando | UX |
|------|--------|-----|
| 401 | cookie/JWT ausente ou expirado | interceptor: `POST /auth/refresh`; se falhar → login |
| 403 | FGAC, ownership, guard de transição | toast `title` + `detail`; não esconder o botão se o `_links` ainda mostrar (reconsultar detalhe) |
| 404 | `NoSuchElementException` | página vazia / voltar à lista |
| 400 | `IllegalArgumentException` (anexo, onBehalf, tipo inativo, delete com histórico, publish inválido) | toast `detail` |
| 409 | bulk parcial; `IllegalStateException` | rollback — não marcar itens como deferidos; recarregar lista |
| 422 schema | `erros: string[]`, `title` schema | destacar wizard passo 2 |
| 422 bean | `erros: [{campo, mensagem}]` | campos do form estático |
| 422 transição | action inválida | desabilitar ActionBar e `GET` detalhe de novo |
| 429 | login 5/min; transições 20/min; forgot 3/h | ler `Retry-After` / `retryAfterSeconds`; countdown |
| 500 | `incidentId` | “tente de novo” + id para suporte |

CSRF ausente em mutação → 403 do Spring Security (não é ProblemDetail do módulo). Sempre `GET /auth/csrf` antes.

---

## 13. Estrutura de pastas sugerida

Alinhar a `agents/frontend-engineer.md`. Blueprint visual: **DashboardA** (ignorar B/C). Tokens: zero hex/px hardcoded.

```
frontend-web/src/
  shared/
    api/client.ts              # axios, cookies, CSRF, 401→refresh
    api/hateoas.ts             # useActions(Record<string, string>) — sem HAL
    api/types/                 # gerados do OpenAPI (preferir a Swagger em /swagger-ui)
    ui/                        # DS: Button, DataTable, ActionBar, Badge…
    tokens/tokens.css
  features/solicitacoes/
    SolicitacoesListPage.tsx
    SolicitacaoDetailPage.tsx
    NovaSolicitacaoPage.tsx
    components/
      DynamicForm.tsx
      WizardStepper.tsx
      AttachmentUpload.tsx
      ReviewSummary.tsx
      DeliberationPanel.tsx
    widgets/
      TextareaWidget.tsx
      SelectWidget.tsx
      EntitySelectWidget.tsx
      MultiSelectTableWidget.tsx
      DatePickerWidget.tsx
    lib/jsonSchemaToZod.ts
    queryKeys.ts
    types.ts
  features/admin/
    TiposSolicitacaoPage.tsx   # /admin/tipos-solicitacao
```

Query keys: `solicitacoesKeys.list(filters)`, `.detail(id)`, `.types()`, `.events(id)`. Invalidar listas+detalhe após transition/submit/confirm.

### Backlog FE-01..FE-14

| ID | Item | Status neste handoff |
|----|------|----------------------|
| FE-01 | Lista fixa + filtros + SLA via `prazoEm` | documentado §9 |
| FE-02 | Wizard 3 passos | documentado §5 |
| FE-03 | `DynamicForm` ← `form_schema` | documentado §6 |
| FE-04 | `jsonSchemaToZod` + RHF; 422 servidor manda | documentado §1, §5, §12 |
| FE-05 | `multi-select-table` | documentado §7 |
| FE-06 | widgets + lookup | documentado §6 (`GET /academico/disciplinas` alias) |
| FE-07 | `AttachmentUpload` SHA-256 | documentado §8 |
| FE-08 | `ReviewSummary` | documentado §5 passo 3 |
| FE-09 | rascunho local vs `SaveDraft` | documentado §5 |
| FE-10 | detalhe timeline + anexos | documentado §9 |
| FE-11 | `useActions(_links)` | documentado §4 |
| FE-12 | detalhe com `formSchema`+`dados` | documentado §9 |
| FE-13 | admin 3 painéis | documentado §11 |
| FE-14 | deliberação + bulk | documentado §10 |

---

## 14. Anti-patterns

Do `agents/workflow-engine-specialist.md` e `frontend-engineer.md`:

- Criar Controller/UseCase/**tela** por tipo de solicitação.
- `if (tipoCode === 'SEGUNDA_CHAMADA')` no form.
- `if (userRole === 'SECRETARIO')` ou `estado === 'EM_DELIBERACAO'` para botões — usar `_links`.
- Confiar só no Zod; ignorar 422.
- Esperar HAL `{ href }` — o back unificou `_links` em **string**.
- Upload direto no backend sem presign; pular `confirm`.
- Enviar `action: "DEFERIR"` — o seed usa `DEFER`, `DENY`, `ASSIGN`, …
- Hardcoded hex/px; copiar DashboardB/C.
- Usar o JWT `ott` como access token — exchange em `POST /auth/ott`.
- Renderizar `formSchema` do catálogo publicado em vez do snapshot do GET detalhe (`idRequestTypeVersion`).

---

## 15. Como testar contra o backend

1. Stack: `ops/docker-compose.yml` (Postgres + **MinIO** 9000/9001). Backend Gradle; Swagger: `http://localhost:8080/swagger-ui.html` (tag **Solicitações** / **Solicitações — Anexos** / **Admin — Tipos de Solicitação**).
2. Credenciais e IDs: `httpie/01-ids-credenciais-e-ambiente.md`, bootstrap `httpie/02-bootstrap-usuarios-demo.md`.
3. Tutoriais HTTP (não Playwright):
   - Aluno (wizard, rascunho, anexos, RESUBMIT): `httpie/F1-aluno/T-F1-005-solicitacoes.md`
   - Secretaria (fila, bulk, **onBehalfOf**): `httpie/F5-secretaria/T-F5-secretaria.md`
   - Professor (deliberar): `httpie/F3-professor/T-F3-professor.md`
   - Admin (CRUD + publish): `httpie/F7-admin/T-F7-003-workflow-engine.md`
   - Protocolo público: `httpie/F0-publico/T-F0-006-007-verificacoes-publicas.md`
4. Seed mínimo para o wizard: `DECLARACAO_MATRICULA`. Para anexos obrigatórios: `SEGUNDA_CHAMADA`. Para tabela: `TRANCAMENTO_DISCIPLINA`.
5. CSRF: `GET /auth/csrf` → header `X-XSRF-TOKEN`.

---

## 16. Fontes

| Recurso | Caminho |
|---------|---------|
| As-built backend | `foundationDocs/analysis/as-built-backend.md` |
| Plano de entregas front | `frontend-web/docs/plano-entregas-frontend.md` |
| Gap report | `foundationDocs/analysis/workflow_engine_gap_report.md` |
| Prompt desta entrega | `prompts/PROMPT_workflow_engine_auditoria_e_implementacao_backend.md` |
| HU aluno | `foundationDocs/HUs/F1 — Aluno/US-F1-005-SOLICITACOES.md` |
| HU admin | `foundationDocs/HUs/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md` |
| HU professor | `foundationDocs/HUs/F3 — Professor/US-F3-003-DELIBERAR-SOLICITACOES.md` |
| HU secretaria | `foundationDocs/HUs/F5 — Secretaria/US-F5-002-SOLICITACOES.md` |
| Telas Figma | `telasFigma/telas1/F1.7-solicitacoes-lista.md`, `F1.8-solicitacoes-nova.md`, `F1.9-solicitacoes-detalhe.md` |
| DS widgets | `foundationDocs/designSystem/inventario-design-system.md` §6.9 (`DS/DynamicForm`, `DS/ReviewSummary`, Pattern/RequestWizard) |
| Agente workflow | `agents/workflow-engine-specialist.md` |
| Agente frontend | `agents/frontend-engineer.md` |
| Seeds | `backend/app/src/main/resources/db/migration/V011__seed_demo_data.sql`, `V017__request_types_complete.sql` |

---

*Última conferência de contratos: 2026-08-29 — as-built (V019, `_links` string, `POST /auth/ott`, `RequestQuery`).*
