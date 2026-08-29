# T-F7-003 — Editor de RequestType / Workflow

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md`](../../foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md)  
> **Status:** ✅ `AdminRequestTypeController`  
> **Capability:** `request_type.manage` (listagem também com `request.view_curso`)

O aluno/secretaria consome só tipos **ativos** em `GET /requests/types` (`ativo=true` — **não** há enum `DRAFT`/`PUBLISHED` no Flyway). Este CRUD edita o catálogo ADR-003. Publish grava snapshot em **`request_type_version`** (Flyway **V019**); instâncias de `request` apontam `id_request_type_version`.

---

## Arquivo

[`solicitacoes/api/AdminRequestTypeController.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/AdminRequestTypeController.kt) — GET lista/detalhe via `RequestTypeQuery`; mutações via `ManageRequestTypeUseCase` (controller sem JPA).

```
GET    /request-types
POST   /request-types           → cria rascunho (ativo=false)
PATCH  /request-types/{id}      → formSchema / workflowJson / prazoDias
POST   /request-types/{id}/publish  → valida schemas; ativo=true; RequestTypeVersionStore.snapshot (V019)
DELETE /request-types/{id}      → 400 se já houver solicitações (countByIdRequestType)
```

```json
{
  "code": "APROVEITAMENTO_DISCIPLINA",
  "descricao": "Aproveitamento de disciplina",
  "prazoDias": 10,
  "formSchema": { "type": "object", "properties": {} },
  "workflowJson": { "initial": "ABERTA", "transitions": [] }
}
```

---

## Checklist

- [x] CRUD + publish (`ManageRequestTypeUseCase` + `RequestTypeQuery`)
- [x] Publish grava `request_type_version` (V019); GET detalhe da instância usa o snapshot
- [x] Delete bloqueado com histórico
- [x] Tipos `ativo=true` aparecem em `GET /requests/types`
