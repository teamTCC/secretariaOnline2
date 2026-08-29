# T-F1-001 — Dashboard do aluno (BFF)

> **Transação:** `[T-F1-001](../../transaçõesBackend/F1%20—%20Aluno/T-F1-001-DASHBOARD.md)`  
> **Diagrama:** `[US-F1-001](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-001-DASHBOARD.md)`  
> **IDs:** nenhum no path. `alunoId` sai do JWT.  
> **Authority:** `dashboard.view_own`  
> **Código:** `DashboardAlunoController` (HTTP) + `DashboardAlunoQuery` (agregação + cache)

Pré-requisito: login aluno ([T-F0-001](../F0-publico/T-F0-001-login.md) + [02 bootstrap](../02-bootstrap-usuarios-demo.md)). Cookie `access_token` na session **ou** Bearer fallback.

---

## Passo 1 — Agregado

```
GET {{baseUrl}}/bff/dashboard/aluno
```

Com session httpie (`--session` / cookie jar) o cookie `access_token` basta. Fallback:

```
Authorization: Bearer {{accessToken}}
```

**Esperado 200:**

```json
{
  "kpis": {
    "horasFormativas": { "atual": 0.0, "requerido": 120.0, "percentual": 0.0 },
    "atendimentosPendentes": 0
  },
  "pendencias": [],
  "eventos": [],
  "ultimasSolicitacoes": [],
  "_links": {
    "self": "/bff/dashboard/aluno",
    "novaSolicitacao": "/requests/types",
    "formativas": "/formativas/minhas",
    "eventos": "/events?audience=me"
  }
}
```

`novaSolicitacao` só aparece se o JWT tiver `request.open`.

Se um bloco interno falhar: o campo vem `null` e `_degraded: true`, **ainda 200**.

Cache Redis TTL 60 s (chave `aluno:{uuid}` no cache `bff-dashboard`). Segundo GET em <60 s deve ser mais rápido; resposta degradada **não** é cacheada. Ver [T-10.7](../../transaçõesBackend/transversal/T-10.7-REDIS-BFF.md).

---

## Passo 2 — FGAC

Com token de professor (sem `dashboard.view_own`):

**Esperado 403** Problem Details.

Outros dashboards (controllers e queries **próprios**, não o do aluno):

| Perfil     | URL                             | Authority                      | Tutorial |
| ---------- | ------------------------------- | ------------------------------ | -------- |
| Professor  | `GET /bff/dashboard/professor`  | `dashboard.view_self_professor` | [T-F3](../F3-professor/T-F3-professor.md) |
| Secretaria | `GET /bff/dashboard/secretaria` | `dashboard.view_secretary`     | [T-F5](../F5-secretaria/T-F5-secretaria.md) |
| Egresso    | `GET /bff/dashboard/egresso`    | `alumni.view_own`              | [T-F2-001](../F2-egresso/T-F2-001-dashboard-egresso.md) |

Use os `_links` do aluno para o próximo teste (`GET /requests/types` → [T-F1-005](T-F1-005-solicitacoes.md)).
