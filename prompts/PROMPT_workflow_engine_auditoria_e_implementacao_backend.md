# Prompt — Auditoria e Implementação Backend do Workflow Engine (ADR-003)

**Objetivo:** auditar o que já existe no repositório para o **motor genérico de solicitações** (`RequestType` + `form_schema` + `workflow_json`) e implementar o que falta **no backend**, sem duplicar lógica por tipo.

**Fora de escopo:** **não implementar frontend** (`frontend-web/`, telas, `DynamicForm`, rotas, widgets, Storybook). Telas Figma, HUs de UI e o design system entram **somente como contrato** para o guia da equipe de frontend (§10).

**Princípio diretor:** *"Uma engine, N tipos, três telas"* — adicionar tipo = inserir configuração JSON, **nunca** criar Controller/UseCase por tipo.

**Idioma de saída:** português do Brasil  
**Não inventar:** contratos fora das fontes listadas em §2 — divergências → registrar em `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` (seção "Gaps") ou perguntar ao usuário.

**Prompt mestre:** execute **uma fase por chat** (§7). Não pule a Fase 0 (auditoria).

---

## 0. Auto-delegação obrigatória (antes de qualquer código)

Ler e aplicar **integralmente** estes agentes, nesta ordem:

| Ordem | Agent | Quando |
|-------|-------|--------|
| 1 | `agents/workflow-engine-specialist.md` | Domínio, `form_schema`, `workflow_json`, widgets (como contrato de API), anti-patterns |
| 2 | `agents/security-engineer.md` | FGAC, JWT, anexos MinIO, validação server-side |
| 3 | `agents/database-engineer.md` | Flyway, JSONB, índices, `request_line_item` |
| 4 | `agents/backend-architect.md` | Use cases, controllers, HATEOAS, Clean Architecture, OpenAPI |

**Não carregar** `agents/ux-ui-specialist.md` nem `agents/frontend-engineer.md` para implementação. Eles são referência **somente** na Fase 7 (guia frontend) — ler naquele chat, sem escrever código React/TSX.

Anunciar no início da resposta: `Delegando a: workflow-engine-specialist, security-engineer, database-engineer, backend-architect`

---

## 1. Fontes canônicas (ordem de precedência)

| Prioridade | Arquivo | Conteúdo |
|------------|---------|----------|
| 1 | `agents/workflow-engine-specialist.md` | Schema de `form_schema` / `workflow_json`, widgets, `multi-select-table` |
| 2 | `foundationDocs/requisitos/por-fase/RF-TR-transversais.md` → RF-TR-001 | Critérios de aceitação do motor |
| 3 | `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` | Escopo MVP v2, APIs, cronograma |
| 4 | `foundationDocs/HUs/F1 — Aluno/US-F1-005-SOLICITACOES.md` | Wizard, lista, detalhe (aluno) — **contrato de API** |
| 5 | `foundationDocs/HUs/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md` | Editor admin, versionamento — **contrato de API** |
| 6 | `foundationDocs/HUs/F3 — Professor/US-F3-003-DELIBERAR-SOLICITACOES.md` | Deliberação + deep-link — **contrato de API** |
| 7 | `foundationDocs/HUs/F5 — Secretaria/US-F5-002-SOLICITACOES.md` | Fila, nova interna, bulk — **contrato de API** |
| 8 | `httpie/F1-aluno/` (se existir T-F1-005*) | Smoke tests manuais da API |

**Fontes de UI (não implementar — só alimentar o guia §10):**

| Arquivo | Uso neste prompt |
|---------|------------------|
| `telasFigma/telas1/F1.7-solicitacoes-lista.md`, `F1.8-solicitacoes-nova.md`, `F1.9-solicitacoes-detalhe.md` | Mapear rotas, passos do wizard e dados que a API deve expor |
| `foundationDocs/designSystem/inventario-design-system.md` §6.9 | Nomes de componentes DS (`DynamicForm`, `FieldArray`, `ReviewSummary`) no guia |

**Código de referência (backend):**

```
backend/modules/solicitacoes/
backend/app/src/main/resources/db/migration/V004__solicitacoes_schema.sql
backend/app/src/main/resources/db/migration/V011__seed_demo_data.sql
```

**Gap report existente (se houver):** `foundationDocs/analysis/workflow_engine_gap_report.md` — atualizar, não duplicar.

---

## 2. Modelo mental — três pilares (não confundir)

```
RequestType (configuração em DB)
├── form_schema   → JSON Schema Draft-07: campos, validação, widgets (incl. tabelas dinâmicas)
├── workflow_json → state machine: estados, transições, authorities, guards, notificações
└── prazo_dias, code, descricao, ativo

Request (instância)
├── dados JSONB   → payload do formulário (linhas de tabela = arrays em dados)
├── estado        → controlado pela WorkflowEngine
└── _links        → ações HATEOAS calculadas (estado × authorities)

WorkflowEngine (runtime puro, sem Spring)
├── allowedTransitions(estado, authorities)
└── applyTransition → RequestEvent + OutboxEvent
```

**Tabela dinâmica no formulário** (não é `workflow_json`):

```jsonc
// form_schema.properties.<campo>
{
  "type": "array",
  "items": { "type": "object", "properties": { /* colunas */ } },
  "x-ui": { "widget": "multi-select-table" }
}
// Linhas em request.dados.<campo> = [{ ... }, { ... }]
```

**Lista `/solicitacoes`** usa colunas **fixas** (Número, Tipo, Estado, Prazo, SLA) — não vem do `form_schema`. O backend deve devolver esses campos na listagem paginada; o front só consome.

---

## 3. Matriz de auditoria — marcar ✅ / ⚠️ parcial / ❌ / N/A

Ao executar a Fase 0, preencher esta matriz com **evidência** (arquivo + linha ou "ausente").

Itens **FE-*** **não entram na implementação**. Na auditoria: marcar `N/A — frontend fora de escopo` e, na Fase 7, transformá-los em seções do guia.

### 3.1 Persistência (Flyway + JPA)

| ID | Item | RF/HU | Evidência esperada |
|----|------|-------|-------------------|
| DB-01 | Tabela `request_type` com `form_schema`, `workflow_json` JSONB | RF-TR-001 | `V004__solicitacoes_schema.sql` |
| DB-02 | Tabela `request` com `dados` JSONB + GIN index | RF-TR-001 | `V004` |
| DB-03 | Tabela `request_event` append-only | RF-TR-001 | `V004` |
| DB-04 | Tabela `request_attachment` | RF-TR-001 | `V004` |
| DB-05 | Seed ≥ 2 tipos com schemas distintos | MVP v2 §2.1 | `V011__seed_demo_data.sql` |
| DB-06 | `request_line_item` (schema TCC completo) | analise §5.3 | migration ou N/A MVP |
| DB-07 | Versionamento `request_type` (versão imutável por publish) | RN-F7-003-07 | tabela `request_type_version` ou N/A |
| DB-08 | Status DRAFT vs PUBLISHED (não só `ativo`) | RN-F7-003-08 | coluna `status` ou equivalente |
| DB-09 | `required_auth` / authorities por tipo | workflow-engine | coluna JSONB ou em `workflow_json` |

### 3.2 Domínio e engine (Kotlin puro)

| ID | Item | RF/HU | Evidência esperada |
|----|------|-------|-------------------|
| DOM-01 | `WorkflowDefinition` + `WorkflowEngine` sem Spring/JPA | RF-TR-001 CA-4 | `domain/WorkflowEngine.kt` |
| DOM-02 | `allowedTransitions` filtra por authority | RF-TR-001 | `WorkflowEngineTest.kt` |
| DOM-03 | `applyTransition` rejeita transição inválida | RF-TR-001 | testes + RFC 7807 na API |
| DOM-04 | Guards (`actor.id == request.idSolicitante`, `allowsReview`) | workflow-engine | `evaluateGuard` |
| DOM-05 | Estado inicial lido de `workflow_json.initial` (não hardcoded) | workflow-engine | `OpenRequestUseCase` |
| DOM-06 | `Request` domain entity com `dados`, `allowsReview()` | domain | `domain/Request.kt` |

### 3.3 Application layer (use cases)

| ID | Item | RF/HU | Evidência esperada |
|----|------|-------|-------------------|
| APP-01 | `OpenRequestUseCase` — cria request + outbox | RF-F1-005 | `OpenRequestUseCase.kt` |
| APP-02 | **Validação `dados` contra `form_schema` no backend** | RF-TR-001 CA-6 | Konform / networknt / manual — **crítico** |
| APP-03 | `TransitionRequestUseCase` — evento + outbox | RF-TR-001 | `TransitionRequestUseCase.kt` |
| APP-04 | `SaveDraftUseCase` + `SubmitDraftUseCase` (estado RASCUNHO) | F1.8-D05 | use cases |
| APP-05 | `BulkDeliberateUseCase` | F5.2-D04 | `BulkDeliberateUseCase.kt` |
| APP-06 | `ManageRequestTypeUseCase` — update + publish | RF-F7-003 | `ManageRequestTypeUseCase.kt` |
| APP-07 | Publish valida JSON Schema draft-07 | RN-F7-003-03 | validador no publish |
| APP-08 | Versionamento atômico no publish | RN-F7-003-07 | snapshot versão anterior |
| APP-09 | `request.id_request_type_version` na instância | RN-F7-003-05 | FK versão ou N/A MVP |
| APP-10 | Auditoria `audit_log` em publish/edit | RN-F7-003-10 | `AuditEvent` emitido |

### 3.4 API REST + HATEOAS

| ID | Endpoint | Capability | RF/HU |
|----|----------|------------|-------|
| API-01 | `GET /requests/types` → `formSchema` | autenticado | MVP v2 |
| API-02 | `GET /requests/types/{code}` → schema + workflow resumo | `request.open` | MVP v2 §API |
| API-03 | `POST /requests` | `request.open` | F1.8-D04 |
| API-04 | `GET /requests?solicitante=me` paginado + filtros | `request.view_own` | F1.7 |
| API-05 | `GET /requests/{id}` → `dados` + `_links` transições | view_own / deliberate | F1.9 |
| API-06 | `GET /requests/{id}` inclui `formSchema` (ou versão) para render detalhe | F1.9 | **obrigatório para o front** |
| API-07 | `POST /requests/{id}/transitions` | conforme transição | RF-TR-001 |
| API-08 | `PATCH /requests/bulk-deliberate` | `request.deliberate` | F5.2-D04 |
| API-09 | `GET /requests/{id}/events` timeline | view | F1.9 |
| API-10 | Anexos: `upload-url` → PUT MinIO → `confirm` + SHA-256 | F1.8-D03 | `RequestAttachmentController` |
| API-11 | `GET /request-types` admin CRUD + publish | `request_type.manage` | F7.4 |
| API-12 | RFC 7807 em 422 (schema inválido) e 403 (transição) | RNF-SEC | `ProblemDetail` |
| API-13 | `POST /requests` com `onBehalfOf` (nova interna) | F5.3 | F5.2 |
| API-14 | Deep-link JWT one-time em transição `generateOneTimeToken` | F3.4 | outbox + IAM |

### 3.5 Frontend Web — **fora de implementação** (inventário para o guia)

Não implementar. Na Fase 0, anotar se `frontend-web/` existe. Na Fase 7, cada linha vira uma seção do guia.

| ID | Item | RF/HU | O que o guia deve cobrir |
|----|------|-------|--------------------------|
| FE-01 | Rota `/solicitacoes` lista fixa + filtros + SLA | F1.7 | colunas, query params, paginação |
| FE-02 | Rota `/solicitacoes/nova` wizard 3 passos | F1.8 | endpoints por passo |
| FE-03 | `DynamicForm` — renderiza `form_schema` | F1.8 CA-03 | contrato JSON Schema + `x-ui` |
| FE-04 | `jsonSchemaToZod` + React Hook Form | workflow-engine | validação cliente vs 422 servidor |
| FE-05 | Widget `multi-select-table` | workflow-engine | shape de `dados` (array de objetos) |
| FE-06 | Widgets: textarea, select, entity-select, date-picker, file-upload | workflow-engine | `x-ui.widget` + endpoints de lookup |
| FE-07 | `AttachmentUpload` SHA-256 + presigned URL | F1.8 CA-04 | fluxo upload-url → PUT → confirm |
| FE-08 | Passo 3 `ReviewSummary` read-only | F1.8 | reutilizar `formSchema` + `dados` |
| FE-09 | Rascunho local (localStorage) | F1.8-D05 | vs `SaveDraft` no servidor |
| FE-10 | `/solicitacoes/:id` timeline + anexos | F1.9 | `GET events` + attachments |
| FE-11 | `useActions(_links)` — ActionBar sem checar role | RF-TR-005 | mapa rel → ação |
| FE-12 | Detalhe renderiza `dados` com schema | F1.9 | `formSchema` no GET detalhe |
| FE-13 | Admin `/admin/tipos-solicitacao` 3 painéis | F7.4 | CRUD + publish |
| FE-14 | Deliberação `/solicitacoes/:id/deliberar` | F3.4 / F5.4 | POST transitions + bulk |

### 3.6 Infra, segurança e testes

| ID | Item | RF/HU |
|----|------|-------|
| INF-01 | MinIO no `docker-compose` + env S3 | MVP v2 §8 |
| INF-02 | BFF dashboard lê solicitações reais | MVP v2 §2.1 |
| SEC-01 | `@PreAuthorize` com capabilities (`request.*`) | RF-TR-005 |
| SEC-02 | Ownership check `view_own` | `RequestController.getById` |
| SEC-03 | Rate limit em transições sensíveis | security-engineer |
| TST-01 | `WorkflowEngineTest` ≥ 85% domain | RF-TR-001 |
| TST-02 | Testcontainers: abrir + transicionar + schema inválido | MVP v2 |
| TST-03 | Teste integração anexo presigned | F1.8-D03 |
| TST-04 | httpie smoke da API (não Playwright de UI) | httpie/ |

---

## 4. Formato do relatório de auditoria (Fase 0)

Criar ou atualizar: `foundationDocs/analysis/workflow_engine_gap_report.md`

```markdown
# Workflow Engine — Gap Report
**Data:** YYYY-MM-DD
**Branch:** ...
**Auditor:** agente
**Escopo desta execução:** backend only (frontend = guia, não código)

## Resumo executivo
- Implementado: X/Y itens ✅
- Parcial: Z itens ⚠️
- Ausente: W itens ❌
- Bloqueadores P0: ...

## Matriz (copiar de §3.1–§3.4 e §3.6 com status)
| ID | Status | Evidência | Notas |

## Frontend (não implementar)
- Pasta `frontend-web/` existe? sim/não
- Itens FE-01..FE-14 → documentar no guia (§10)

## Gaps P0 (bloqueiam demo MVP v2 **via API**)
1. ...

## Gaps P1 (necessários para HU completa no backend)
1. ...

## Gaps P2 (post-MVP)
1. ...

## Decisões pendentes (perguntar ao usuário)
1. ...
```

**Regras do relatório:**
- Cada ❌ deve citar **o que implementar** (arquivo/classe sugerida), não só "falta".
- Não marcar ✅ sem evidência em código ou migration.
- Separar **backend funcional** vs **admin editor API** vs **frontend (guia)**.
- Não tratar ausência de telas React como bloqueador P0 deste prompt.

---

## 5. Ordem de implementação (após auditoria)

Implementar apenas o que a matriz marcou ❌ ou ⚠️ **nas seções 3.1–3.4 e 3.6**, nesta ordem:

```
Fase 1 — Backend core (P0)
  → APP-02 validação form_schema
  → DOM-05 estado inicial de workflow_json
  → API-02 GET /types/{code}
  → API-06 formSchema no detalhe (ou versão)
  → RFC 7807 consistente

Fase 2 — Anexos + rascunho (P0 MVP v2)
  → API-10 upload-url / confirm completo
  → APP-04 SaveDraft / SubmitDraft
  → SEC anexos (content-type, tamanho, ownership)

Fase 3 — Deliberação + secretaria (P1)
  → API-08 bulk, API-13 onBehalfOf
  → Deep-link JWT (API-14) se ainda gap

Fase 4 — Admin editor API (P1)
  → DB-07/08 versionamento + DRAFT/PUBLISHED (se no escopo MVP)
  → APP-07/08 publish com validação
  → API-11 CRUD + publish

Fase 5 — Testes + httpie (P0 gate)
  → TST-01 a TST-03
  → TST-04 httpie (sem Playwright de wizard)
  → `./gradlew :modules:solicitacoes:test`

Fase 6 — Guia de implementação frontend (§10)
  → NÃO escrever código em frontend-web/src
  → Criar o documento em frontend-web/docs/
```

**Não existe "Fase frontend aluno"** neste prompt. Wizard, lista e detalhe React ficam para a equipe de frontend, guiada pelo documento da Fase 6.

**Anti-patterns proibidos (workflow-engine-specialist):**
- Novo Controller/UseCase por tipo de solicitação
- Confiar só na validação do cliente (o backend **sempre** valida `dados` contra `form_schema`)
- Transição sem `request_event`
- Expor `_links` que o ator não pode executar
- Documentar no guia "checar `userRole === 'SECRETARIO'`" — o guia deve mandar usar `useActions(_links)`

---

## 6. Definition of Done (motor backend para demo via API)

Marcar só quando **todos** os itens abaixo passam. DoD de telas React **não** se aplica.

### Demo aluno (mínimo MVP v2 — via httpie/Swagger)
- [ ] 2+ `RequestType` publicados com `form_schema` diferentes (um com `multi-select-table`)
- [ ] `POST /requests` aceita payload válido; rejeita inválido com **422** RFC 7807
- [ ] `GET /requests` lista solicitações reais com campos de SLA/prazo
- [ ] `GET /requests/{id}` devolve `dados` (incl. array de tabela) + `formSchema` + timeline (`/events`) + anexos
- [ ] Transição `RESUBMIT` após `EM_AJUSTE` aparece em `_links` e funciona no `POST /transitions`
- [ ] BFF dashboard reflete contagem real

### Demo admin (P1 — API)
- [ ] Criar tipo DRAFT, editar schema, publicar (se versionamento no escopo)
- [ ] Novo tipo aparece em `GET /requests/types` sem redeploy da aplicação

### Qualidade
- [ ] `WorkflowEngineTest` + ≥1 teste integração Testcontainers (se TST-02 no escopo)
- [ ] ktlint/detekt sem erros novos
- [ ] Sem `TODO` crítico no módulo `solicitacoes/`
- [ ] OpenAPI/Swagger dos endpoints de requests alinhado ao código
- [ ] **Guia frontend** criado em `frontend-web/docs/` (§10)

---

## 7. Execução por chat (copiar um bloco por sessão)

### Chat A — Fase 0: Auditoria only (sem implementar)

```
Execute o prompt `prompts/PROMPT_workflow_engine_auditoria_e_implementacao_backend.md` — **apenas Fase 0**.

1. Ler agentes §0.
2. Auditar backend `modules/solicitacoes/`, migrations V004/V011 (e posteriores de request types), BFF se aplicável.
3. Verificar se `frontend-web/` existe — apenas anotar; não implementar UI.
4. Preencher matriz §3.1–§3.4 e §3.6 com ✅/⚠️/❌ + evidência. FE-* = N/A implementação.
5. Gerar/atualizar `foundationDocs/analysis/workflow_engine_gap_report.md`.
6. Listar gaps P0 ordenados para implementação backend.
7. **Não alterar código** neste chat.
```

### Chat B — Fase 1: Backend core

```
Execute Fase 1 do prompt workflow engine backend (§5).
Pré-requisito: gap report existente.
Implementar: validação form_schema, estado inicial workflow, GET /types/{code}, formSchema no detalhe, RFC 7807.
Testes unitários para validação e WorkflowEngine.
Não implementar frontend. Não criar o guia ainda.
```

### Chat C — Fase 2: Anexos + rascunho

```
Execute Fase 2 do prompt workflow engine backend.
Fluxo completo upload-url → MinIO PUT → confirm; SaveDraft/SubmitDraft estado RASCUNHO.
Teste integração com MinIO/Testcontainers se disponível.
Não implementar frontend.
```

### Chat D — Fases 3–5: Deliberação, admin API, testes

```
Execute Fases 3–5 do prompt workflow engine backend conforme gaps restantes no gap report.
Priorizar bulk, onBehalfOf e deep-link se P0/P1 para a sprint.
Fechar com httpie smoke (API) e atualizar gap report (marcar ✅).
Não implementar frontend. Não criar o guia ainda.
```

### Chat E — Fase 6: Guia de implementação frontend (obrigatório ao fechar o motor)

```
Execute a Fase 6 / §10 do prompt `prompts/PROMPT_workflow_engine_auditoria_e_implementacao_backend.md`.

1. Ler `agents/frontend-engineer.md` e `agents/ux-ui-specialist.md` só como referência de estrutura e DS.
2. Ler o código real dos controllers/DTOs/assemblers de `modules/solicitacoes` (não inventar paths).
3. Criar o documento em `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md`.
4. Se `frontend-web/` não existir, criar a pasta `frontend-web/docs/` apenas para este arquivo.
5. **Não escrever** `.tsx`, hooks, rotas nem componentes. Só o guia markdown.
6. Atualizar o gap report: FE-01..FE-14 → "documentado no guia".
```

---

## 8. Comandos úteis para o agente

```bash
# Listar módulo solicitações
ls backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/

# Buscar validação de schema
rg -n "form_schema|formSchema|JsonSchema|Konform" backend/modules/solicitacoes

# Testes
cd backend && ./gradlew :modules:solicitacoes:test :modules:solicitacoes:ktlintCheck

# Migrations solicitações
ls backend/app/src/main/resources/db/migration/V*solici*
ls backend/app/src/main/resources/db/migration/V011*

# Smoke API (não UI)
ls httpie/F1-aluno/
```

Não usar `rg` em `frontend-web/` para implementar features. Só consultar o guia existente, se houver.

---

## 9. Referência rápida — estrutura JSON

### `form_schema` — campo tabela dinâmica

```json
{
  "type": "object",
  "properties": {
    "disciplinas": {
      "type": "array",
      "title": "Disciplinas",
      "items": {
        "type": "object",
        "properties": {
          "idDisciplina": { "type": "string", "format": "uuid", "title": "Disciplina" },
          "operacao": { "type": "string", "enum": ["INCLUSAO", "EXCLUSAO"], "title": "Operação" }
        },
        "required": ["idDisciplina", "operacao"]
      },
      "minItems": 1,
      "x-ui": { "widget": "multi-select-table" }
    }
  },
  "required": ["disciplinas"]
}
```

### `dados` na instância (runtime)

```json
{
  "disciplinas": [
    { "idDisciplina": "01932e8a-...", "operacao": "EXCLUSAO" }
  ]
}
```

### `workflow_json` — transição mínima

```json
{
  "initial": "ABERTA",
  "states": ["RASCUNHO", "ABERTA", "EM_DELIBERACAO", "DEFERIDA", "INDEFERIDA", "EM_AJUSTE"],
  "transitions": [
    { "from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante" }
  ]
}
```

---

## 10. Entregável obrigatório — guia de implementação para a equipe de frontend

**Quando:** após as fases de código backend (ou no Chat E, mesmo que ainda existam gaps P2).  
**Onde:** `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md`  
**Se a pasta não existir:** criar `frontend-web/docs/` (não criar `src/`, rotas nem `package.json` neste prompt).

O guia é o **handoff** para a equipe de frontend. Deve permitir implementar wizard, lista, detalhe, deliberação e admin **sem reler o módulo Kotlin**. Basear-se no código **real** (paths, nomes de campos, `_links`, códigos HTTP), não em exemplos genéricos.

### 10.1 Conteúdo mínimo do guia (seções obrigatórias)

1. **Princípio diretor** — uma engine, N tipos; UI cega a perfil (`useActions(_links)`); validação Zod no cliente **não substitui** 422 do servidor.

2. **Mapa de telas → APIs** — tabela: rota (`/solicitacoes`, `/solicitacoes/nova`, `/solicitacoes/:id`, deliberação, admin) × método × path real × authority × HU/tela Figma.

3. **Contratos JSON** — `form_schema` (Draft-07 + `x-ui.widget`), `dados`, `workflow_json` (só o que o front precisa: `initial`, estados para badge). Exemplos reais dos seeds (ex.: tipo com `multi-select-table`).

4. **HATEOAS** — como `_links` são gerados; tabela `rel` → ação de UI (rótulo, método, body). Proibir `userRole === ...` e `estado === 'EM_DELIBERACAO'` para mostrar botões.

5. **Wizard 3 passos (F1.8)** — sequência de chamadas: listar tipos → GET tipo por code → rascunho/submit → anexos. Payload de `POST /requests` e de draft. Tratamento de 422 (apontar campos de RFC 7807).

6. **DynamicForm / widgets** — registry sugerido (`textarea`, `select`, `entity-select`, `multi-select-table`, `date-picker`, `file-upload`); shape de cada widget; endpoints de lookup (`entity-select`); **não** criar um form por `RequestType`.

7. **Tabela dinâmica** — `items.properties` = colunas; array em `dados`; exemplo de seed.

8. **Anexos** — fluxo `upload-url` → PUT MinIO → `confirm` + SHA-256 no cliente; limites (content-type, tamanho) que o back já aplica.

9. **Lista e detalhe** — colunas fixas da lista + query params; detalhe usa `formSchema` + `dados` (read-only); timeline `GET .../events`.

10. **Deliberação e secretaria** — `POST /transitions`, bulk, `onBehalfOf`; deep-link JWT (o que a tela precisa fazer com o token).

11. **Admin tipos** — CRUD + publish; o que validar no cliente vs o que o publish já valida no servidor.

12. **Erros** — mapa status → UX (401 refresh, 403, 404, 409, 422, 429 rate limit).

13. **Estrutura de pastas sugerida** — alinhada a `agents/frontend-engineer.md` (`features/solicitacoes/`, `shared/api/hateoas.ts`, `jsonSchemaToZod`). Checklist FE-01..FE-14 como backlog da equipe de front.

14. **Anti-patterns** — copiar os do workflow-engine-specialist + frontend-engineer relevantes.

15. **Como testar contra o backend** — apontar `httpie/F1-aluno/T-F1-005*` (ou equivalente) e Swagger; seed mínimo (quais `code` de `RequestType` usar).

16. **Fontes** — links relativos para HUs, telas Figma, `inventario-design-system.md` §6.9, gap report.

### 10.2 Regras do guia

- Idioma: português do Brasil.
- Citar arquivos backend reais (controller, DTO) quando o contrato depender deles.
- Não colar Kotlin longo; preferir exemplos JSON de request/response.
- Não implementar os componentes — descrever **o que** construir e **contra qual contrato**.
- Se um endpoint ainda for gap, marcar `STATUS: pendente no backend` em vez de inventar o path.

### 10.3 Definition of Done do guia

- [ ] Arquivo existe em `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md`
- [ ] Todas as seções 10.1 estão presentes
- [ ] Paths e nomes de campos conferidos no código (não só nas HUs)
- [ ] Gap report atualizado: frontend documentado, não “ausente sem handoff”

---

*Última atualização: 2026-08-29 — derivado de `PROMPT_workflow_engine_auditoria_e_implementacao.md`, escopo backend-only + guia em `frontend-web/docs/`.*
