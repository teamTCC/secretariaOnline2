# Prompt — Auditoria e Implementação do Workflow Engine (ADR-003)

**Objetivo:** auditar o que já existe no repositório para o **motor genérico de solicitações** (`RequestType` + `form_schema` + `workflow_json` + `DynamicForm`) e implementar o que falta, sem duplicar lógica por tipo.

**Princípio diretor:** *"Uma engine, N tipos, três telas"* — adicionar tipo = inserir configuração JSON, **nunca** criar Controller/UseCase/Screen por tipo.

**Idioma de saída:** português do Brasil  
**Não inventar:** contratos fora das fontes listadas em §2 — divergências → registrar em `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` (seção "Gaps") ou perguntar ao usuário.

**Prompt mestre:** execute **uma fase por chat** (§7). Não pule a Fase 0 (auditoria).

---

## 0. Auto-delegação obrigatória (antes de qualquer código)

Ler e aplicar **integralmente** estes agentes, nesta ordem:

| Ordem | Agent | Quando |
|-------|-------|--------|
| 1 | `agents/workflow-engine-specialist.md` | Domínio, `form_schema`, `workflow_json`, widgets, anti-patterns |
| 2 | `agents/security-engineer.md` | FGAC, JWT, anexos MinIO, validação server-side |
| 3 | `agents/database-engineer.md` | Flyway, JSONB, índices, `request_line_item` |
| 4 | `agents/backend-architect.md` | Use cases, controllers, HATEOAS, Clean Architecture |
| 5 | `agents/ux-ui-specialist.md` | DS/*, wizard, estados, DashboardA |
| 6 | `agents/frontend-engineer.md` | `DynamicForm`, TanStack Query, `useActions` |

Anunciar no início da resposta: `Delegando a: workflow-engine-specialist, …`

---

## 1. Fontes canônicas (ordem de precedência)

| Prioridade | Arquivo | Conteúdo |
|------------|---------|----------|
| 1 | `agents/workflow-engine-specialist.md` | Schema de `form_schema` / `workflow_json`, widgets, `multi-select-table` |
| 2 | `foundationDocs/requisitos/por-fase/RF-TR-transversais.md` → RF-TR-001 | Critérios de aceitação do motor |
| 3 | `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` | Escopo MVP v2, APIs, cronograma |
| 4 | `foundationDocs/HUs/F1 — Aluno/US-F1-005-SOLICITACOES.md` | Wizard, lista, detalhe (aluno) |
| 5 | `foundationDocs/HUs/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md` | Editor admin, versionamento |
| 6 | `foundationDocs/HUs/F3 — Professor/US-F3-003-DELIBERAR-SOLICITACOES.md` | Deliberação + deep-link |
| 7 | `foundationDocs/HUs/F5 — Secretaria/US-F5-002-SOLICITACOES.md` | Fila, nova interna, bulk |
| 8 | `telasFigma/telas1/F1.7-solicitacoes-lista.md`, `F1.8-solicitacoes-nova.md`, `F1.9-solicitacoes-detalhe.md` | Layout e componentes |
| 9 | `foundationDocs/designSystem/inventario-design-system.md` §6.9 | `DS/DynamicForm`, `DS/FieldArray`, `DS/ReviewSummary` |
| 10 | `httpie/F1-aluno/` (se existir T-F1-005*) | Smoke tests manuais |

**Código de referência (backend):**

```
backend/modules/solicitacoes/
backend/app/src/main/resources/db/migration/V004__solicitacoes_schema.sql
backend/app/src/main/resources/db/migration/V011__seed_demo_data.sql
```

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

**Lista `/solicitacoes`** usa colunas **fixas** (Número, Tipo, Estado, Prazo, SLA) — não vem do `form_schema`.

---

## 3. Matriz de auditoria — marcar ✅ / ⚠️ parcial / ❌ / N/A

Ao executar a Fase 0, preencher esta matriz com **evidência** (arquivo + linha ou "ausente").

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
| API-06 | `GET /requests/{id}` inclui `formSchema` (ou versão) para render detalhe | F1.9 | **facilita front** |
| API-07 | `POST /requests/{id}/transitions` | conforme transição | RF-TR-001 |
| API-08 | `PATCH /requests/bulk-deliberate` | `request.deliberate` | F5.2-D04 |
| API-09 | `GET /requests/{id}/events` timeline | view | F1.9 |
| API-10 | Anexos: `upload-url` → PUT MinIO → `confirm` + SHA-256 | F1.8-D03 | `RequestAttachmentController` |
| API-11 | `GET /request-types` admin CRUD + publish | `request_type.manage` | F7.4 |
| API-12 | RFC 7807 em 422 (schema inválido) e 403 (transição) | RNF-SEC | `ProblemDetail` |
| API-13 | `POST /requests` com `onBehalfOf` (nova interna) | F5.3 | F5.2 |
| API-14 | Deep-link JWT one-time em transição `generateOneTimeToken` | F3.4 | outbox + IAM |

### 3.5 Frontend Web (`frontend-web/`)

| ID | Item | RF/HU | Evidência esperada |
|----|------|-------|-------------------|
| FE-01 | Rota `/solicitacoes` lista fixa + filtros + SLA | F1.7 | página + hook |
| FE-02 | Rota `/solicitacoes/nova` wizard 3 passos | F1.8 | `WizardStepper` |
| FE-03 | `DynamicForm` — renderiza `form_schema` | F1.8 CA-03 | `features/solicitacoes/` |
| FE-04 | `jsonSchemaToZod` + React Hook Form | workflow-engine | `shared/utils/` |
| FE-05 | Widget `multi-select-table` (colunas de `items.properties`) | workflow-engine | `MultiSelectTableWidget` |
| FE-06 | Widgets: textarea, select, entity-select, date-picker, file-upload | workflow-engine | widget registry |
| FE-07 | `AttachmentUpload` SHA-256 + presigned URL | F1.8 CA-04 | componente |
| FE-08 | Passo 3 `ReviewSummary` read-only | F1.8 | `DS/ReviewSummary` |
| FE-09 | Rascunho local (localStorage) | F1.8-D05 | hook |
| FE-10 | `/solicitacoes/:id` timeline + anexos | F1.9 | página |
| FE-11 | `useActions(_links)` — ActionBar sem checar role | RF-TR-005 | hook HATEOAS |
| FE-12 | Detalhe renderiza `dados` com schema (tabelas dinâmicas) | F1.9 | `SchemaDataView` ou DynamicForm readOnly |
| FE-13 | Admin `/admin/tipos-solicitacao` 3 painéis | F7.4 | página admin |
| FE-14 | Deliberação `/solicitacoes/:id/deliberar` | F3.4 / F5.4 | página |

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
| TST-04 | httpie / Playwright smoke wizard | httpie/ |

---

## 4. Formato do relatório de auditoria (Fase 0)

Criar ou atualizar: `foundationDocs/analysis/workflow_engine_gap_report.md`

```markdown
# Workflow Engine — Gap Report
**Data:** YYYY-MM-DD
**Branch:** ...
**Auditor:** agente

## Resumo executivo
- Implementado: X/Y itens ✅
- Parcial: Z itens ⚠️
- Ausente: W itens ❌
- Bloqueadores P0: ...

## Matriz (copiar de §3 com status)
| ID | Status | Evidência | Notas |

## Gaps P0 (bloqueiam demo MVP v2)
1. ...

## Gaps P1 (necessários para HU completa)
1. ...

## Gaps P2 (post-MVP)
1. ...

## Decisões pendentes (perguntar ao usuário)
1. ...
```

**Regras do relatório:**
- Cada ❌ deve citar **o que implementar** (arquivo/classe sugerida), não só "falta".
- Não marcar ✅ sem evidência em código ou migration.
- Separar **backend funcional** vs **frontend** vs **admin editor**.

---

## 5. Ordem de implementação (após auditoria)

Implementar apenas o que a matriz marcou ❌ ou ⚠️, nesta ordem:

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

Fase 3 — Frontend aluno (P0 MVP v2)
  → FE-01 a FE-12 (wizard + lista + detalhe)
  → Prioridade: DynamicForm + multi-select-table antes de admin

Fase 4 — Deliberação + secretaria (P1)
  → API-08 bulk, API-13 onBehalfOf
  → FE deliberação F3.4 / F5.4

Fase 5 — Admin editor (P1)
  → DB-07/08 versionamento + DRAFT/PUBLISHED
  → APP-07/08 publish com validação
  → FE-13 editor 3 painéis

Fase 6 — Testes + httpie (P0 gate)
  → TST-01 a TST-04
  → `./gradlew :modules:solicitacoes:test`
```

**Anti-patterns proibidos (workflow-engine-specialist):**
- Novo Controller/UseCase por tipo de solicitação
- Botões no front baseados em `userRole === 'SECRETARIO'`
- Confiar só na validação Zod do cliente
- Transição sem `request_event`

---

## 6. Definition of Done (motor completo para demo)

Marcar só quando **todos** os itens abaixo passam:

### Demo aluno (mínimo MVP v2)
- [ ] 2+ `RequestType` publicados com `form_schema` diferentes (um com `multi-select-table`)
- [ ] Wizard abre, valida, envia; backend rejeita payload inválido (422)
- [ ] Lista mostra solicitações reais com SLA
- [ ] Detalhe mostra `dados` (incl. tabela) + timeline + anexos
- [ ] Transição `RESUBMIT` após `EM_AJUSTE` via `_links` + HATEOAS
- [ ] BFF dashboard reflete contagem real

### Demo admin (P1)
- [ ] Criar tipo DRAFT, editar schema, publicar
- [ ] Novo tipo aparece no wizard sem deploy

### Qualidade
- [ ] `WorkflowEngineTest` + ≥1 teste integração Testcontainers
- [ ] ktlint/detekt sem erros novos
- [ ] Sem `TODO` crítico no módulo `solicitacoes/`

---

## 7. Execução por chat (copiar um bloco por sessão)

### Chat A — Fase 0: Auditoria only (sem implementar)

```
Execute o prompt `prompts/PROMPT_workflow_engine_auditoria_e_implementacao.md` — **apenas Fase 0**.

1. Ler agentes §0.
2. Auditar backend `modules/solicitacoes/`, migrations V004/V011, BFF se aplicável.
3. Verificar se `frontend-web/` existe e o que há de solicitações.
4. Preencher matriz §3 com ✅/⚠️/❌ + evidência.
5. Gerar `foundationDocs/analysis/workflow_engine_gap_report.md`.
6. Listar gaps P0 ordenados para implementação.
7. **Não alterar código** neste chat.
```

### Chat B — Fase 1: Backend core

```
Execute Fase 1 do prompt workflow engine (§5).
Pré-requisito: gap report existente.
Implementar: validação form_schema, estado inicial workflow, GET /types/{code}, formSchema no detalhe, RFC 7807.
Testes unitários para validação e WorkflowEngine.
Não implementar frontend neste chat.
```

### Chat C — Fase 2: Anexos + rascunho

```
Execute Fase 2 do prompt workflow engine.
Fluxo completo upload-url → MinIO PUT → confirm; SaveDraft/SubmitDraft estado RASCUNHO.
Teste integração com MinIO/Testcontainers se disponível.
```

### Chat D — Fase 3: Frontend aluno (wizard + tabelas dinâmicas)

```
Execute Fase 3 do prompt workflow engine.
Implementar /solicitacoes, /solicitacoes/nova (DynamicForm + multi-select-table), /solicitacoes/:id.
Usar DS/* e tokens; useActions para ações; Zod derivado do form_schema.
Seed: testar TRANCAMENTO_DISCIPLINA (array disciplinas).
```

### Chat E — Fases 4–6: Deliberação, admin, testes

```
Execute Fases 4–6 do prompt workflow engine conforme gaps restantes no gap report.
Priorizar deliberação secretaria/professor se P0 para sua sprint.
Fechar com httpie smoke e atualizar gap report (marcar ✅).
```

---

## 8. Comandos úteis para o agente

```bash
# Listar módulo solicitações
ls backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/

# Buscar validação de schema
rg -n "form_schema|formSchema|JsonSchema|Konform" backend/modules/solicitacoes

# Buscar DynamicForm no front
rg -n "DynamicForm|multi-select-table|formSchema" frontend-web/

# Testes
cd backend && ./gradlew :modules:solicitacoes:test :modules:solicitacoes:ktlintCheck

# Migrations solicitações
ls backend/app/src/main/resources/db/migration/V*solici*
ls backend/app/src/main/resources/db/migration/V011*
```

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

*Última atualização: 2026-08-29 — alinhado a RF-TR-001, MVP v2, US-F7-003 e conversa sobre `form_schema` / `multi-select-table`.*
