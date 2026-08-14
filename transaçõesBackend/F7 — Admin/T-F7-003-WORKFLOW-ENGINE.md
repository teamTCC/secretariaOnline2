# T-F7-003 — Editor de RequestType / Workflow

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md`](../../foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md)  
> **Status:** ✅ `AdminRequestTypeController`  
> **Capability:** `request_type.manage` (listagem também com `request.view_curso`)

O aluno/secretaria consome só tipos **publicados** em `GET /requests/types` (`ativo=true`). Este CRUD edita o catálogo ADR-003.

---

## Arquivo

[`solicitacoes/api/AdminRequestTypeController.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/AdminRequestTypeController.kt)

```
GET    /request-types
POST   /request-types           → cria rascunho (ativo=false)
PATCH  /request-types/{id}      → formSchema / workflowJson / prazoDias
POST   /request-types/{id}/publish  → exige schemas não vazios; ativo=true
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

- [x] CRUD + publish
- [x] Delete bloqueado com histórico
- [x] Tipos publicados aparecem em `GET /requests/types`
