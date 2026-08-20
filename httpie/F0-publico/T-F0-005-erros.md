# T-F0-005 — Erros RFC 7807

> **Transação:** [`T-F0-005`](../../transaçõesBackend/F0%20—%20Público/T-F0-005-ERRO.md)  
> **Diagrama:** [`US-F0-005`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-005-ERRO.md)

Não há um endpoint `/erro`. Você **provoca** o `GlobalExceptionHandler` com requests ruins e confere o envelope.

Envelope padrão:

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/<tipo>",
  "title": "…",
  "status": 401,
  "detail": "…",
  "instance": "/caminho"
}
```

Header: `Content-Type: application/problem+json`.

---

## Casos para disparar no HTTPie

| # | Request | Esperado |
|---|---------|----------|
| 1 | `GET {{baseUrl}}/me` sem Bearer | **401** `unauthorized` |
| 2 | `GET {{baseUrl}}/bff/dashboard/aluno` com token de admin sem `dashboard.view_own` | **403** `forbidden` |
| 3 | `POST /auth/login` body `{}` | **400** `validation-error` (Jakarta `@Valid`) |
| 4 | `GET {{baseUrl}}/requests/00000000-0000-0000-0000-000000000000` autenticado | **404** `not-found` |
| 5 | `POST /auth/reset-password` senha `fraca` | **422** `weak-password` |
| 6 | 6× login falho no mesmo identificador | **429** `rate-limit` + `retryAfterSeconds` |

Body vazio de login (cole no HTTPie):

```json
{}
```

**Esperado ~400:**

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "errors": [
    { "field": "identificador", "message": "Identificador é obrigatório" },
    { "field": "senha", "message": "Senha é obrigatória" }
  ]
}
```

---

## 5xx e `incidentId`

Se algum request autenticado estourar (MinIO down, etc.):

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/internal-error",
  "title": "Erro interno",
  "status": 500,
  "detail": "…",
  "incidentId": "INC-2026-ab12"
}
```

Anote o `incidentId` e correlacione no log do backend (`br.ufpr.sept.so2`). **Não** deve vir stack trace no JSON.

---

## Checklist

- [ ] 401/403/400/404/422/429 todos `application/problem+json`
- [ ] `status` no body = status HTTP
- [ ] 4xx sem stack
- [ ] 5xx com `incidentId` se ocorrer
