# T-F8-002 — FAQ e tickets de suporte

> **Transação:** [`T-F8-002`](../../transaçõesBackend/F8%20—%20Cross-cutting/T-F8-002-SUPORTE-FAQ.md)  
> **Diagrama:** [`US-F8-002`](../../foundationDocs/sequenceDiagrams/F8%20—%20Cross-cutting/US-F8-002-SUPORTE-FAQ.md)  
> **IDs:** `{{ticketId}}`, `{{faqId}}`  

Paths reais do `SupportController` (sem prefixo `/support` no FAQ):

| Uso | Path |
|-----|------|
| FAQ (lista) | `GET /faq` |
| FAQ admin | `POST/PATCH/DELETE /faq` — [T-F7](../F7-admin/T-F7-admin.md) |
| Abrir ticket | `POST /support/tickets` |
| Meus tickets | `GET /support/tickets/mine` |
| Fila staff | `GET /support/tickets` |
| Responder / fechar | `PATCH /support/tickets/{id}/respond` · `/close` |

O DTO de ticket usa **`descricao`**, não `mensagem`.

---

## Passo 1 — FAQ

```
GET {{baseUrl}}/faq
Authorization: Bearer {{accessTokenAluno}}
```

Seed: [`V013__faq_seed.sql`](../../backend/app/src/main/resources/db/migration/V013__faq_seed.sql). **Esperado 200:** array `{ id, pergunta, resposta, ordem, categoria }` só `ativo=true`.

Se o Swagger ainda listar `GET /support/faq`, teste os dois; o código mapeia `/faq`.

---

## Passo 2 — Abrir ticket (aluno)

```
POST {{baseUrl}}/support/tickets
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "assunto": "Erro ao submeter atividade formativa",
  "descricao": "Ao tentar enviar o comprovante do minicurso, recebo erro 500. Teste HTTPie."
}
```

**Esperado 201** com `id` → `{{ticketId}}`. Outbox de suporte. Rate limit possível → **429**.

```
GET {{baseUrl}}/support/tickets/mine
```

---

## Passo 3 — Staff responde e fecha

```
GET {{baseUrl}}/support/tickets
Authorization: Bearer {{accessTokenSecretaria}}
```

Cole no Body:

```json
{
  "resposta": "Recebemos o chamado. Verifique o formato PDF nativo e tente de novo."
}
```

```
PATCH {{baseUrl}}/support/tickets/{{ticketId}}/respond
PATCH {{baseUrl}}/support/tickets/{{ticketId}}/close
```

Sem permissão na fila global → **403**.
