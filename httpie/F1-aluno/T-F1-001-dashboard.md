# T-F1-001 — Dashboard do aluno (BFF)

> **Transação:** `[T-F1-001](../../transaçõesBackend/F1%20—%20Aluno/T-F1-001-DASHBOARD.md)`  
> **Diagrama:** `[US-F1-001](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-001-DASHBOARD.md)`  
> **IDs:** nenhum no path. `alunoId` sai do JWT.  
> **Authority:** `dashboard.view_own`

Pré-requisito: login aluno ([T-F0-001](../F0-publico/T-F0-001-login.md) + [02 bootstrap](../02-bootstrap-usuarios-demo.md)).

---

## Passo 1 — Agregado

```
GET {{baseUrl}}/bff/dashboard/aluno
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

Se um bloco interno falhar: o campo vem `null` e `_degraded: true`, **ainda 200**.

Cachgundo GET em <6e Redis TTL 60 s (chave `aluno:{uuid}`). Se0 s deve ser mais rápido; resposta degradada **não** é cacheada.

---

## Passo 2 — FGAC

Com token de professor (sem `dashboard.view_own`):

**Esperado 403** Problem Details.

Outros dashboards (outros tutoriais):

| Perfil     | URL                             | Authority                                                   |
| ---------- | ------------------------------- | ----------------------------------------------------------- |
| Professor  | `GET /bff/dashboard/professor`  | `dashboard.view_self_professor`                             |
| Secretaria | `GET /bff/dashboard/secretaria` | `dashboard.view_secretary`                                  |
| Egresso    | `GET /bff/dashboard/egresso`    | ver [T-F2-001](../F2-egresso/T-F2-001-dashboard-egresso.md) |

Use os `_links` do aluno para o próximo teste (`GET /requests/types` → [T-F1-005](T-F1-005-solicitacoes.md)).
