# Workflow Engine — Gap Report
**Data:** 2026-08-29  
**Branch:** main (working tree)  
**Auditor:** agente (Cursor) — Fases 0–6 (Chats A–E)  
**Escopo desta execução:** backend only (frontend = guia, não código)

---

## Resumo executivo

- Implementado: 37/37 itens backend do MVP ✅ + versionamento P2 (DB-07, APP-08, APP-09 via `V019`)
- Parcial: DB-08 continua ⚠️ P2 — coluna `ativo` boolean (equivalente semântico a DRAFT/PUBLISHED; sem enum)
- Ausente (N/A): DB-06 `request_line_item` (nunca foi tabela Flyway — **não** tratar como implementada)
- Bloqueadores P0 (demo via API): **0**
- Bloqueadores P1 (HU completa backend): **0**
- Frontend: `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md` criado (Chat E). FE-01..FE-14 **documentados**. Sem `src/` / rotas / `.tsx`.
- Contrato as-built: `foundationDocs/analysis/as-built-backend.md` (Flyway `V001`–`V019`).

---

## Matriz de auditoria

### 3.1 Persistência (Flyway + JPA)

| ID | Status | Evidência | Notas |
|----|--------|-----------|-------|
| DB-01 | ✅ | `V004__solicitacoes_schema.sql` L1-12 | `request_type` com `form_schema` JSONB, `workflow_json` JSONB |
| DB-02 | ✅ | `V004` L15-40 | `request` com `dados` JSONB + GIN index `idx_request_dados` |
| DB-03 | ✅ | `V004` L42-53 | `request_event` append-only (sem UPDATE/DELETE) |
| DB-04 | ✅ | `V004` L55-67 | `request_attachment` com sha256, storage_key, content_type |
| DB-05 | ✅ | `V011__seed_demo_data.sql` (3 tipos) + `V017__request_types_complete.sql` (16 tipos) | 19 tipos totais seedados; todos com `form_schema` e `workflow_json` distintos; `ADIANTAMENTO_PERIODO` usa `multi-select-table`; `SEGUNDA_CHAMADA` usa `entity-select` |
| DB-06 | ❌ N/A | Ausente em todas as migrations (V001–V019) | `request_line_item` **não** é tabela as-built. Modelo TCC 2026-06 descrevia; Flyway nunca criou. Manter N/A. |
| DB-07 | ✅ | `V019__request_type_versioning.sql` | Tabela `request_type_version` + FK `request.id_request_type_version`. Publish grava snapshot imutável. |
| DB-08 | ⚠️ P2 | `request_type.ativo` BOOLEAN | Continua `ativo` boolean (OK). Sem enum `DRAFT`/`PUBLISHED`. Semântica: `ativo=false` rascunho, `ativo=true` publicado. |
| DB-09 | ✅ | `workflow_json` em cada tipo | Authorities por tipo embutidas em `transitions[].requiresAuthority` |

### 3.2 Domínio e engine (Kotlin puro)

| ID | Status | Evidência | Notas |
|----|--------|-----------|-------|
| DOM-01 | ✅ | `domain/WorkflowEngine.kt`, `domain/WorkflowDefinition.kt` | Zero imports de Spring, JPA, HTTP — apenas `org.slf4j` no Engine (aceitável) |
| DOM-02 | ✅ | `WorkflowEngine.allowedTransitions()` | Filtra transições por `currentState` + `any { auth in authorities }` |
| DOM-03 | ✅ | `WorkflowEngine.applyTransition()` + `SolicitacoesExceptionHandler` | `InvalidTransitionException` → 422 RFC 7807 |
| DOM-04 | ✅ | `WorkflowEngine.evaluateGuard()` | Guards `actor.id == request.idSolicitante` e `request.allowsReview` |
| DOM-05 | ✅ | `OpenRequestUseCase.kt` | Estado inicial lido de `workflowDef.initial` — não hardcoded |
| DOM-06 | ✅ | `domain/Request.kt` | `dados: Map<String,Any>`, `allowsReview()`: INDEFERIDA + concludedAt < 5 dias |

### 3.3 Application layer (use cases)

| ID | Status | Evidência | Notas |
|----|--------|-----------|-------|
| APP-01 | ✅ | `OpenRequestUseCase.kt` | Cria request + outbox `solicitacoes.aberta`; suporta `onBehalfOf` |
| APP-02 | ✅ | `FormSchemaValidator.validate()` (networknt/json-schema-validator) | Chamado em Open e Submit |
| APP-03 | ✅ | `TransitionRequestUseCase.kt` | Persiste `RequestEventEntity` + enfileira via `OutboxEventPublisher.enqueue(...)` (port `shared`) na mesma `@Transactional`. **Não** injeta `OutboxEventJpaRepository`. |
| APP-04 | ✅ | `SaveDraftUseCase` + `UpdateDraftUseCase` + `SubmitDraftUseCase` | Chat C: PATCH rascunho + `x-required-attachments` |
| APP-05 | ✅ | `BulkDeliberateUseCase.kt` | Itera `TransitionRequestUseCase` — all-or-nothing (409 + rollback) |
| APP-06 | ✅ | `ManageRequestTypeUseCase.update()` + `publish()` | Update emite `AuditPayload`; publish valida schemas |
| APP-07 | ✅ | `FormSchemaValidator.validateSchemaStructure()` + `validateWorkflowStructure()` no publish | |
| APP-08 | ✅ | `ManageRequestTypeUseCase.publish()` + `V019` | Publish grava snapshot em `request_type_version`; instâncias abertas permanecem na versão original. |
| APP-09 | ✅ | `request.id_request_type_version` (V019) | FK para a versão da instância; GET detalhe usa `form_schema` da versão. |
| APP-10 | ✅ | `ManageRequestTypeUseCase` | `AuditPublisher.publish()` em `update` e `publish` |

### 3.4 API REST + HATEOAS

| ID | Endpoint | Status | Evidência | Notas |
|----|----------|--------|-----------|-------|
| API-01 | `GET /requests/types` | ✅ | `RequestController.listTypes()` | `_links` como `Map<String,String>` (self, open, save-draft). **Não** HAL `EntityModel` `{ rel: { href } }`. |
| API-02 | `GET /requests/types/{code}` | ✅ | `RequestController.getTypeByCode()` | `formSchema` + `workflowJson` + `_links` strings |
| API-03 | `POST /requests` | ✅ | `RequestController.open()` | `request.open` ou `request.open_on_behalf`; `@Valid`; 201 Created |
| API-04 | `GET /requests` | ✅ | `RequestController.list()` | Filtros: `estado`, `idCurso`, `typeCode`/`type`; paginado; `protocolo` + `prazoEm` |
| API-05 | `GET /requests/{id}` | ✅ | `RequestController.getById()` | `RequestDetailResponse` + `_links: Map<String,String>` (self, events, attachments, transições kebab-case). Rel = URL string, **não** `{ href }`. |
| API-06 | `GET /requests/{id}` | ✅ | `RequestDetailResponse.formSchema` | `formSchema` incluso |
| API-07 | `POST /requests/{id}/transitions` | ✅ | `RequestController.transition()` | 200 com `estadoNovo` |
| API-08 | `PATCH /requests/bulk-deliberate` | ✅ | `RequestController.bulkDeliberate()` | Chat D: já existia; httpie F5 cobre |
| API-09 | `GET /requests/{id}/events` | ✅ | `RequestController.events()` | Timeline ASC |
| API-10 | Upload presigned + confirm | ✅ | `RequestAttachmentController` | Orphan + canônico; SHA-256 server-side. Chat C |
| API-11 | `GET /request-types` admin | ✅ | `AdminRequestTypeController` | CRUD + publish; `ativo` = rascunho/publicado |
| API-12 | RFC 7807 | ✅ | `SolicitacoesExceptionHandler.kt` | 422 schema; 422 transition; 403 guard |
| API-13 | `onBehalfOf` | ✅ | `OpenRequestDto.idSolicitanteOnBehalf` | Chat D: smoke httpie F5.2b adicionado |
| API-14 | Deep-link JWT | ✅ | `OneTimeTokenPort` + `POST /auth/ott` (`ExchangeOttUseCase`) | Emissão: token + JTI; URL `?ott=`. Exchange: body `{ "token": "<jwt>" }`, audience `request:{uuid}`; 200 mesmo contrato do login + cookies. Replay → 401. |

### 3.5 Frontend Web — documentado (Chat E)

| ID | Status | Evidência |
|----|--------|-----------|
| FE-01..FE-14 | ✅ documentado no guia | `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md` seções 1–16. Sem implementação React. |

### 3.6 Infra, segurança e testes

| ID | Status | Evidência | Notas |
|----|--------|-----------|-------|
| INF-01 | ✅ | `ops/docker-compose.yml` — serviço `minio:` | Chat B |
| INF-02 | ✅ | `DashboardAlunoQuery` / professor / secretaria | BFF lê dados reais |
| SEC-01 | ✅ | `@PreAuthorize` com capabilities | |
| SEC-02 | ✅ | Ownership check em getById/list/protocol/attachments | |
| SEC-03 | ✅ | `RateLimitFilter` — 20/min em `/requests/{uuid}/transitions` | 429 + Retry-After |
| TST-01 | ✅ | `WorkflowEngineTest` + `FormSchemaValidatorTest` | Chat D: `jacksonObjectMapper()` no teste de workflow (Kotlin module) |
| TST-02 | ✅ | `RequestWorkflowIntegrationTest.kt` | Chat B |
| TST-03 | ✅ | Draft + MinIO ITs | Chat C |
| TST-04 | ✅ | httpie F1-005, F3, F5 (bulk + onBehalf), F7-003 | Chat D: F5.2b onBehalfOf |

---

## Frontend (handoff)

- Pasta `frontend-web/src/` existe? **Não** (não criar neste prompt)
- Pasta `frontend-web/docs/` existe? **Sim** — `GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md`
- Itens FE-01..FE-14 → **documentados** (não “ausente sem handoff”)

Gaps conscientes no guia (não inventar path):

1. Seed `x-ui.endpoint` = `/academico/disciplinas`; API real também expõe alias `GET /academico/disciplinas` (`AcademicoController`)
2. ~~Sem endpoint OTT~~ → `POST /auth/ott` existe (`ExchangeOttUseCase`)
3. ~~Versionamento P2~~ → `V019` `request_type_version` + snapshot no publish + FK na instância

---

## Gaps P0 — **TODOS RESOLVIDOS** ✅

| Item | Status |
|------|--------|
| Validação `dados` vs `form_schema` (APP-02) | ✅ |
| Estado inicial de `workflow_json.initial` (DOM-05) | ✅ |
| `GET /requests/types/{code}` (API-02) | ✅ |
| `formSchema` no detalhe (API-06) | ✅ |
| RFC 7807 em 422/403 (API-12) | ✅ |
| 19 tipos seedados (DB-05) | ✅ |

---

## Gaps P1 — ✅ TODOS RESOLVIDOS

INF-01, TST-02 (Chat B), API-10, APP-04, TST-03 (Chat C). Fase 3 (bulk, onBehalf, deep-link emissão) já estava no código; Chat D fechou smoke httpie + teste Jackson.

---

## Gaps P2 (pós-MVP)

1. **DB-07** ✅ — `request_type_version` (`V019`)
2. **DB-08** ⚠️ — continua `ativo` boolean (OK; sem enum DRAFT/PUBLISHED)
3. **APP-08** ✅ — snapshot imutável no publish
4. **APP-09** ✅ — FK `id_request_type_version` em `request`
5. **DB-06** ❌ N/A — `request_line_item` (ainda não é tabela; não documentar como implementada)
6. Exchange OTT → sessão ✅ — `POST /auth/ott` (API-14)

---

## Decisões (fechadas)

1. ~~INF-01 MinIO~~ Chat B
2. ~~TST-02 Testcontainers~~ Chat B
3. **DB-08** — permanece `ativo` boolean (P2 consciente). **DB-07 / APP-08 / APP-09** — fechados em `V019` (snapshot + FK).
4. ~~API-10 SHA-256~~ Chat C
5. Guia frontend — Chat E, arquivo único em `frontend-web/docs/`

---

## Arquivos-chave

| Arquivo | Existência | Observação |
|---------|------------|------------|
| `domain/WorkflowEngine.kt` | ✅ | Puro Kotlin |
| `api/RequestController.kt` | ✅ | CRUD requests + `_links` Map string (não EntityModel HAL) |
| `api/RequestAttachmentController.kt` | ✅ | orphan + bound + confirm |
| `api/AdminRequestTypeController.kt` | ✅ | `/request-types` |
| `V018__request_attachment_event_updated_at.sql` | ✅ | Chat C |
| `V019__request_type_versioning.sql` | ✅ | Última migration as-built (não V018) |
| `httpie/F5-secretaria/T-F5-secretaria.md` | ✅ | Chat D: F5.2b onBehalfOf |
| `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md` | ✅ | **Chat E** |
| `frontend-web/src/` | ❌ | fora de escopo |

---

## Status das fases

| Fase | Chat | Status |
|------|------|--------|
| Fase 0 — Auditoria | Chat A | ✅ Completa |
| Fase 1 — INF-01 + TST-02 P1 | Chat B | ✅ Completa |
| Fase 2 — Anexos + rascunho | Chat C | ✅ Completa |
| Fase 3-5 — Deliberação, admin, testes | Chat D | ✅ Completa (smoke + teste Jackson + httpie onBehalf). Versionamento P2 fechado depois via V019. |
| Fase 6 — Guia frontend | Chat E | ✅ Completa |

## DoD backend (prompt §6)

- [x] `GET /requests` com `prazoEm` (SLA no cliente)
- [x] `GET /requests/{id}` com `dados` + `formSchema` + `/events` + `/attachments`
- [x] `RESUBMIT` em `_links` quando engine permitir
- [x] BFF dashboard com contagens reais
- [x] Demo admin: criar `ativo=false`, PATCH, publish → aparece em `/requests/types`
- [x] Sem TODO crítico em `solicitacoes/`
- [x] Guia frontend §10

---

*Última atualização: 2026-08-29 — alinhado a `as-built-backend.md` (V019, OTT, OutboxEventPublisher, `_links` strings)*
