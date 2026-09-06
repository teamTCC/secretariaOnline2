# T-F5-012 — Kanban de tarefas

> **Transação:** [`T-F5-012`](../../transaçõesBackend/F5%20—%20Secretaria/T-F5-012-TAREFAS.md)  
> **Diagrama:** [`US-F5-012`](../../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-012-TAREFAS.md)  
> **IDs:** `{{taskId}}`, `{{secretariaId}}`  

Capability: `task.manage`. Estados: `PENDENTE` · `EM_ANDAMENTO` · `CONCLUIDA`. Delete **só** `PENDENTE`.

---

## Passo 1 — Listar

```
GET {{baseUrl}}/tasks
GET {{baseUrl}}/tasks?estado=PENDENTE
Authorization: Bearer {{accessTokenSecretaria}}
```

---

## Passo 2 — Criar

```
POST {{baseUrl}}/tasks
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "titulo": "Conferir diplomas — lote julho",
  "prioridade": "ALTA",
  "prazoEm": "2026-08-25T17:00:00Z"
}
```

**Esperado 201** com `id` → `{{taskId}}`, estado `PENDENTE`.

---

## Passo 3 — Mover coluna

Cole no Body:

```json
{
  "estado": "EM_ANDAMENTO",
  "idAssignee": "{{secretariaId}}"
}
```

```
PATCH {{baseUrl}}/tasks/{{taskId}}
```

**Esperado 200** `EM_ANDAMENTO`.

---

## Passo 4 — Delete

Em `PENDENTE`:

```
DELETE {{baseUrl}}/tasks/{{taskId}}
```

**Esperado 204.** Se já `EM_ANDAMENTO`/`CONCLUIDA` → **400**. Sem `task.manage` → **403**.
