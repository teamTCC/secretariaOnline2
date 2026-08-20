# T-F0-004 — Contato público

> **Transação:** [`T-F0-004`](../../transaçõesBackend/F0%20—%20Público/T-F0-004-CONTATO.md)  
> **Diagrama:** [`US-F0-004`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-004-CONTATO.md)  

Sem JWT. **CSRF obrigatório** no POST. Rate limit POST: 10/min por IP.

---

## Passo 1 — Dados institucionais (também emite CSRF)

```
GET {{baseUrl}}/publico/contato
```

**Esperado 200:**

```json
{
  "nome": "Secretaria SEPT — UFPR",
  "endereco": "Rua Dr. Alcides Vieira Arcoverde, 1225 — …",
  "telefone": "(41) 3360-4900",
  "email": "secretaria.sept@ufpr.br",
  "horario": "Segunda a sexta, 8h–17h",
  "_links": { "enviar": "/publico/contato" }
}
```

Copie o cookie `XSRF-TOKEN` → `{{xsrfToken}}` (se ainda não tiver).

---

## Passo 2 — Enviar mensagem

```
POST {{baseUrl}}/publico/contato
Content-Type: application/json
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "nome": "Ana Silva",
  "email": "ana.aluno@ufpr.br",
  "assunto": "Horário de atendimento",
  "mensagem": "A secretaria atende no sábado? Preciso protocolar uma declaração de matrícula."
}
```

**Esperado 202:**

```json
{
  "id": "0193…",
  "status": "ACEITO",
  "mensagem": "…"
}
```

Persistido em `contact_message` (`NOVO`) + outbox `contato.recebido`.

Sem header CSRF → **403**. Sem body válido → **400**.

---

## Passo 3 — Conferir outbox (admin)

```
GET {{baseUrl}}/admin/outbox?page=0&size=20
Authorization: Bearer {{accessToken}}
```

Procure `contato.recebido`. Mailhog deve receber encaminhamento para `app.contato.email`.
