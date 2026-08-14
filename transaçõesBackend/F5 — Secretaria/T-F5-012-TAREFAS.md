# T-F5-012 — Tarefas internas (Kanban)

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-012-TAREFAS.md`](../../foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-012-TAREFAS.md)  
> **Status:** ✅ CRUD + colunas + delete só `PENDENTE`  
> **Capability:** `task.manage`

---

## Arquivo

[`iam/api/SecretaryTaskController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/SecretaryTaskController.kt)

Estados: `PENDENTE` · `EM_ANDAMENTO` · `CONCLUIDA`.

```
GET    /tasks?estado=PENDENTE
POST   /tasks          { "titulo": "Conferir diplomas", "prioridade": "ALTA", "prazoEm": "..." }
PATCH  /tasks/{id}     { "estado": "EM_ANDAMENTO", "idAssignee": "uuid" }
DELETE /tasks/{id}     → 204 se PENDENTE; 400 caso contrário
```

---

## Checklist

- [x] Listar / criar / mover coluna
- [x] DELETE apenas `PENDENTE`
- [x] 403 sem `task.manage`
