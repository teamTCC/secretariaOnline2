# T-F4-001 — Comissão CAAF

> **Transação:** [`T-F4-001`](../../transaçõesBackend/F4%20—%20Comissões/T-F4-001-COMISSAO-CAAF.md)  
> **Diagrama:** [`US-F4-001`](../../foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-001-COMISSAO-CAAF.md)  
> **IDs:** `{{formativaId}}`  
> **Authority:** `formative.review`

Os paths **reais** do controller (use estes no HTTPie, não os `/dashboard` e `/assign` do diagrama):

| Diagrama | Código |
|----------|--------|
| `GET /commissions/caaf/dashboard` | `GET /commissions/caaf/pool` |
| `POST /commissions/caaf/assign` | `POST /commissions/caaf/{activityId}/claim` |
| batch | `POST /commissions/caaf/batch-review` |
| stats | `GET /commissions/caaf/stats` |

Pré-requisito: uma formativa `PENDENTE` sem revisor ([T-F1-006](../F1-aluno/T-F1-006-formativas.md)).

Login: professor com `formative.review` (ou admin).

---

## Passo 1 — Pool

```
GET {{baseUrl}}/commissions/caaf/pool?page=0&size=20
Authorization: Bearer {{accessToken}}
```

**Esperado 200** paginado:

```json
{
  "content": [
    {
      "id": "…",
      "idAluno": "…",
      "titulo": "Palestra: Machine Learning Aplicado",
      "categoria": "PALESTRA",
      "cargaHoraria": 4.0,
      "dataRealizacao": "2026-06-15"
    }
  ]
}
```

Copie `id` → `{{formativaId}}`. Sem authority → **403**.

---

## Passo 2 — Self-assign (claim)

```
POST {{baseUrl}}/commissions/caaf/{{formativaId}}/claim
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200:** `{ "id": "…", "idRevisor": "<seu userId>" }`. Já reivindicada → erro 400.

---

## Passo 3 — Batch review

Cole no Body:

```json
{
  "ids": ["{{formativaId}}"],
  "acao": "APROVAR",
  "parecer": "Lote CAAF — comprovantes conferidos (HTTPie)."
}
```

```
POST {{baseUrl}}/commissions/caaf/batch-review
```

**Esperado 200:** `{ "processadas": 1, "estado": "APROVADA" }`. Aprovar emite certificado formativa + outbox `formativas.batch_revisada`.

`acao` só `APROVAR` ou `REJEITAR`.

---

## Passo 4 — Stats

```
GET {{baseUrl}}/commissions/caaf/stats
```

KPIs de pendentes / aprovadas. Use para conferir o efeito do lote.
