# T-F4-002 — Comissão COE

> **Transação:** [`T-F4-002`](../../transaçõesBackend/F4%20—%20Comissões/T-F4-002-COMISSAO-COE.md)  
> **Diagrama:** [`US-F4-002`](../../foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-002-COMISSAO-COE.md)  
> **IDs:** `{{internshipId}}`, `{{supervisorId}}` (UUID do professor orientador)  
> **Authority:** `internship.review`

Paths reais:

| Uso | Path |
|-----|------|
| Pool | `GET /commissions/coe/pool` |
| Atribuir 1 | `POST /commissions/coe/{internshipId}/assign-supervisor` |
| Lote | `POST /commissions/coe/bulk-assign` |
| Stats | `GET /commissions/coe/stats` |

Não existe “aprovar estágio em lote” (parecer jurídico individual).

Pré-requisito: estágio `EM_ANDAMENTO` sem supervisor ([T-F1-007](../F1-aluno/T-F1-007-008-estagio-tcc.md)). `{{supervisorId}}` = `{{professorId}}`.

---

## Passo 1 — Pool

```
GET {{baseUrl}}/commissions/coe/pool?page=0&size=20
Authorization: Bearer {{accessToken}}
```

**Esperado 200** com `id`, `idAluno`, `empresa`, `estado`. Copie `id` → `{{internshipId}}`.

---

## Passo 2 — Atribuir supervisor

Cole no Body:

```json
{
  "idSupervisor": "{{supervisorId}}"
}
```

```
POST {{baseUrl}}/commissions/coe/{{internshipId}}/assign-supervisor
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200.** Outbox `estagio.supervisor_atribuido` (e-mail aluno + professor).

Cole no Body:

```json
{
  "internshipIds": ["{{internshipId}}"],
  "idSupervisor": "{{supervisorId}}"
}
```

```
POST {{baseUrl}}/commissions/coe/bulk-assign
```

---

## Passo 3 — Stats e conclusão

```
GET {{baseUrl}}/commissions/coe/stats
POST {{baseUrl}}/internships/{{internshipId}}/conclude
```

Conclude → `CONCLUIDO`. Sem `internship.review` → **403**.
