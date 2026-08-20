# T-F0-002 — Recuperar senha

> **Transação:** [`T-F0-002`](../../transaçõesBackend/F0%20—%20Público/T-F0-002-RECUPERAR-SENHA.md)  
> **Diagrama:** [`US-F0-002`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-002-RECUPERAR-SENHA.md)  
> **IDs:** e-mail real da base. Token JWT 1-uso nasce no outbox.  

Isento de CSRF. Rate limit: **3 req/hora** por e-mail+IP.

---

## Passo 1 — Pedir o link (e-mail existente)

```
POST {{baseUrl}}/auth/forgot-password
Content-Type: application/json
```

Cole no Body:

```json
{
  "email": "ana.aluno@ufpr.br"
}
```

**Esperado 202:**

```json
{
  "mensagem": "Se este email existir, enviaremos um link válido por 24h."
}
```

O SMTP **não** rodou ainda. O use case só inseriu `outbox_event` (`iam.password_reset_requested`).

---

## Passo 2 — Pegar o token (três jeitos)

### A) Mailhog (preferido)

1. Abra [http://localhost:8025](http://localhost:8025).
2. Espere até 5 s (dispatcher).
3. Abra o e-mail → link `/nova-senha?token=` ou `/redefinir-senha?token=`.
4. Copie o JWT → `{{resetToken}}`.

### B) SQL no payload do outbox

```sql
SELECT id, status, payload->>'email' AS email, payload->>'token' AS token
FROM outbox_event
WHERE event_type = 'iam.password_reset_requested'
ORDER BY created_at DESC
LIMIT 3;
```

### C) Admin outbox (se já estiver autenticado como admin)

```
GET {{baseUrl}}/admin/outbox?page=0&size=20
Authorization: Bearer {{accessToken}}
```

Procure `eventType` = `iam.password_reset_requested` → `payload.token`.

---

## Passo 3 — Anti-enumeração (e-mail inexistente)

Cole no Body:

```json
{
  "email": "nao.cadastrado@ufpr.br"
}
```

**Esperado 202 — body bit-a-bit igual ao Passo 1.** Sem outbox novo, sem e-mail.

Compare o tempo de resposta: não deve ser óbvio (o backend evita work pesado no ramo negativo).

---

## Passo 4 — Rate limit (opcional)

Dispare o Passo 1 quatro vezes em menos de 1 hora.

**Esperado 429:**

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/rate-limit",
  "title": "Muitas tentativas",
  "status": 429,
  "detail": "Muitas tentativas. Aguarde antes de tentar novamente.",
  "retryAfterSeconds": 1847
}
```

Header `Retry-After`.

---

## Próximo

Com `{{resetToken}}` preenchido → [T-F0-003](T-F0-003-nova-senha.md).
