# Fatia 3 — Motor de solicitações (os 19 tipos, três telas)

**Objetivo:** caixa-preta de **T-F1-005** completo no browser: catálogo, open, draft/submit, anexos MinIO, detalhe V019, timeline, protocolo, **todas** as actions do seed, filtros de lista. Os **19** types usam o **mesmo** wizard.

**Não é:** 19 páginas React. `if (tipoCode === 'SEGUNDA_CHAMADA')` é bug.

**Pré-requisito:** Fatias 0–2.  
**Contrato:** `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md`  
**Oráculos:** `httpie/F1-aluno/T-F1-005-solicitacoes.md` · `httpie/F7-admin/T-F7-003-workflow-engine.md`  
**Código:** `RequestController` + `RequestQuery` + `WorkflowEngine.allowedTransitions`

On-behalf e bulk-deliberate: UI nesta fatia **pode** já expor se `_links` vierem; senão entram na fatia 7 com a mesma página.

---

## 1. Três rotas (cobrem os 19)

| Rota UI | API | Quem usa |
|---------|-----|----------|
| `/solicitacoes` | `GET /requests?estado=&typeCode=&page=&size=` | aluno / sec / prof |
| `/solicitacoes/nova?code=` | types → schema → draft **ou** open | `request.open` |
| `/solicitacoes/:id` | GET detalhe, events, attachments, protocol, POST transitions | view_*; botões = `_links` |

Deliberação = a mesma `/solicitacoes/:id`. Sem `/deliberar`.

Passo 1 do wizard: `<select>` com **todos** os `code` de `GET /requests/types` (19 ativos no seed). Não hardcodar a lista.

---

## 2. Whitelist

```
features/solicitacoes/
  SolicitacoesListPage.tsx
  NovaSolicitacaoPage.tsx      # type picker + DynamicForm + draft/open/submit
  SolicitacaoDetailPage.tsx    # JSON + HateoasBar + anexos + events + protocol
  DynamicForm.tsx              # todos os widgets do seed
shared/ui/AttachmentUpload.tsx # presign → PUT MinIO → confirm (reuso formativa/estágio)
```

**Proibido:** `DeclaracaoMatriculaPage.tsx`, `SegundaChamadaForm.tsx`, Zod por tipo.

---

## 3. Os 19 tipos (seed V011 + V017)

O Grok **não** implementa um form por linha. Implementa widgets; o schema escolhe.

| code | Widgets / regra que a caixa-preta precisa exercitar |
|------|------------------------------------------------------|
| `DECLARACAO_MATRICULA` | select enum + textarea (caminho curto HATEOAS) |
| `HISTORICO_ESCOLAR` | select + textarea |
| `ATESTADO_FREQUENCIA` | select + textarea |
| `AUTORIZACAO_IMAGEM` | selects |
| `DIPLOMA` | textarea + selects (regra de horas/bloqueios no **back**) |
| `COLACAO_SEM_SOLENIDADE` | textarea + date-picker + anexo `COMPROVANTE_MOTIVO` |
| `TRANCAMENTO_PERIODO` | select semestre + textarea |
| `REVISAO_NOTA` | entity-select disciplina + selects + textarea |
| `SEGUNDA_CHAMADA` | entity-select + textarea + anexo **obrigatório** `ATESTADO_MEDICO` |
| `JUSTIFICATIVA_FALTA` | entity-select enrolled + date + anexo `COMPROVANTE_AUSENCIA` |
| `TRANCAMENTO_DISCIPLINA` | **multi-select-table** de disciplinas + textarea |
| `ADIANTAMENTO_PERIODO` | semestre + **multi-select-table** `disciplinasDesejadas` |
| `INCLUSAO_DISCIPLINA` | tabela + textarea |
| `EXCLUSAO_DISCIPLINA` | tabela enrolled + textarea |
| `APROVEITAMENTO_DISCIPLINA` | entity-select + anexos `HISTORICO_ORIGEM`, `EMENTA_DISCIPLINA` |
| `MATRICULA_DISCIPLINA_ISOLADA` | entity-select + semestre + textarea |
| `MATRICULA_DISCIPLINA_ELETIVA` | entity-select `?tipo=ELETIVA` + textarea |
| `APROVEITAMENTO_ESTAGIO` | textarea + anexos `TERMO_ESTAGIO`, `RELATORIO_FINAL`, `AVALIACAO_EMPRESA` |
| `APROVEITAMENTO_ATIVIDADE_COMPLEMENTAR` | select + date + anexo `COMPROVANTE_ATIVIDADE` |

`idCurso` no open: UUID TADS (`GET /academico/cursos`). `<select>` dos cursos, não só input cru — a transação precisa do UUID certo.

Query `enrolled` / `tipo` no endpoint do schema: o back **aceita e ignora** (não 400). Passar `idCurso` no lookup.

---

## 4. Open, draft, submit (todos obrigatórios)

```
POST /requests            { idRequestType, idCurso, dados }     → 201 ABERTA
POST /requests/draft      mesmo body                            → RASCUNHO (não valida schema)
PATCH /requests/{id}/draft  { dados }
POST /requests/{id}/submit                                      → valida schema
```

UI na nova: radio/checkbox “salvar rascunho” vs “abrir já”. Detalhe em `RASCUNHO` mostra rels `update-draft` / `submit` se o back mandar.

`dados` inválido no open/submit → **422** na `ProblemBanner` (fonte de verdade; sem Zod).

---

## 5. Anexos MinIO (obrigatório — SEGUNDA_CHAMADA e afins)

`x-required-attachments` na raiz do schema: a UI lista as categorias e **bloqueia submit** só como UX; o back revalida.

| Quando | Path |
|--------|------|
| Wizard sem id | `POST /requests/attachments/presigned-url` |
| Já existe request | `POST /requests/{id}/attachments/upload-url` |
| Confirm | `POST /requests/{id}/attachments/confirm` |
| Lista | `GET /requests/{id}/attachments` |
| Download | `GET /requests/{id}/attachments/{id}/download-url` |
| Apagar | `DELETE /requests/{id}/attachments/{id}` |

Body presign: `{ filename, contentType, sha256, sizeBytes, categoria }`.  
SHA-256: `crypto.subtle.digest` → hex 64. Servidor recalcula no confirm.

PUT na `uploadUrl` com o mesmo `Content-Type`. Allowlist e 20 MB: `AttachmentPolicy`. Falha CORS MinIO: mostrar `uploadUrl` + campo `storageKey` para colar depois do PUT via HTTPie — o **confirm** ainda tem de existir na UI.

Estados que aceitam anexo: `RASCUNHO`, `ABERTA`, `EM_AJUSTE`.

---

## 6. Lista

`GET /requests?page=0&size=20`  
Filtros nativos: `estado`, `typeCode` (alias `type=`), `idCurso`.

Aluno `view_own`: servidor ignora outro solicitante. Secretaria `view_curso`: curso inteiro.

Tabela: protocolo, tipoCode, estado, prazoEm, link detalhe. Paginação pelos `_links.next`. `JsonPanel` do envelope.

Checkbox lote só se `_links.bulk_deliberate` (ABERTA + authority) — POST vai para fatia 7 se não couber agora, mas o checkbox pode já aparecer.

---

## 7. Detalhe + transições

`GET /requests/{id}`: `estado`, `dados`, `formSchema` (**snapshot V019**), `parecer`, `_links`.

`GET /requests/{id}/events` — deve ter `ABERTURA`.  
`GET /requests/{id}/protocol` → link público fatia 1.

Aluno em `ABERTA`: sem `defer`. Staff `request.deliberate`: `assign` então `defer`/`deny`/…

```
POST /requests/{id}/transitions
{ "action": "ASSIGN", "parecer": "…" }
```

`action` = `actionFromRel(rel)`. **Não existe** `DEFERIR`.

| rel | action |
|-----|--------|
| `assign` | `ASSIGN` |
| `forward-to-deliberator` | `FORWARD_TO_DELIBERATOR` |
| `defer` | `DEFER` |
| `deny` | `DENY` |
| `request-adjustment` | `REQUEST_ADJUSTMENT` |
| `resubmit` | `RESUBMIT` |
| `request-review` | `REQUEST_REVIEW` |

Parecer: um `<input>` na página de detalhe, reusado por defer/deny/ajuste.

422 `invalid-transition` visível (ex.: DEFER sem ASSIGN).

`EM_AJUSTE`: aluno vê `resubmit` + form/anexos; staff não defer naquele estado.

---

## 8. `DynamicForm.tsx`

Ler `formSchema.properties` + `required` + `x-ui.widget` + raiz `x-required-attachments`.

| widget / schema | HTML mínimo |
|-----------------|-------------|
| `select` / `enum` | `<select>` |
| `textarea` / `x-ui.rows` | `<textarea>` |
| `date-picker` / `format: date` | `<input type=date>` |
| string | `<input>` |
| number | `<input type=number>` |
| boolean | `<input type=checkbox>` |
| `entity-select` | `<input list>` ou select preenchido por `GET {x-ui.endpoint}?idCurso=&search=` — gravar **UUID** `id`; mostrar `codigo — nome` |
| `multi-select-table` | `<table>` + botão “adicionar linha”; células = `items.properties` (entity-select reusa o widget) |
| desconhecido | `<textarea>` JSON com `label=nomeDoCampo` |

Zero `switch(tipoCode)`.

---

## 9. Aceite caixa-preta (não negociável)

1. `GET /requests/types` na UI mostra os **19** codes.
2. Open `DECLARACAO_MATRICULA` → aluno sem `defer`; secretaria `ASSIGN` → `DEFER` → `DEFERIDA`.
3. Draft → PATCH dados → submit.
4. Open `SEGUNDA_CHAMADA` **sem** atestado → 422; **com** confirm MinIO → 201.
5. Open `TRANCAMENTO_DISCIPLINA` com ≥1 linha na tabela → 201; `dados.disciplinas[]` no detalhe.
6. Entity-select disciplina grava UUID válido (`GET /academico/disciplinas`).
7. Timeline tem `ABERTURA`; protocolo público abre.
8. POST action `DEFERIR` → 422.
9. Admin publica schema novo (fatia 7): instância velha **mantém** snapshot; nova usa o schema novo.

## 10. Não fazer

- Interpretar `workflow_json` para botões.
- `if (estado === 'ABERTA' && isSecretaria)`.
- Página por tipo.
- Pular anexos “para a demo”.
