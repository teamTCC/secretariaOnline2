# T-F0-001 — Login, refresh, logout e CSRF

> **Transação:** [`T-F0-001-LOGIN.md`](../../transaçõesBackend/F0%20—%20Público/T-F0-001-LOGIN.md)  
> **Diagrama:** [`US-F0-001-LOGIN.md`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-001-LOGIN.md)  
> **IDs:** nenhum UUID de negócio. Use `{{adminEmail}}` / `{{alunoEmail}}`.  

Controller: `POST /auth/login` · `POST /auth/refresh` · `POST /auth/logout` · `GET /auth/csrf`.

---

## Passo 1 — CSRF (sempre primeiro no dia)

| Campo | Valor |
|-------|--------|
| Method | `GET` |
| URL | `{{baseUrl}}/auth/csrf` |
| Auth | none |

**Esperado 200:**

```json
{
  "token": "e7c1…",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf"
}
```

Copie `token` → `{{xsrfToken}}`. Confira o cookie `XSRF-TOKEN` no cookie jar.

---

## Passo 2 — Login feliz (admin)

| Campo | Valor |
|-------|--------|
| Method | `POST` |
| URL | `{{baseUrl}}/auth/login` |
| Body | JSON abaixo |

Cole no Body:

```json
{
  "identificador": "admin@ufpr.br",
  "senha": "Admin@123456"
}
```

**Esperado 200:**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9…",
  "tokenType": "Bearer",
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

Cookies: `refresh_token` (HttpOnly, `Path=/auth`, Max-Age 604800).

Copie `accessToken` → `{{accessToken}}`.

Variantes de body:

- Aluno e-mail: 

```json
{
  "identificador": "ana.aluno@ufpr.br",
  "senha": "AlunoS3nh@Forte!"
}
```

- Aluno GRR: 

```json
{
  "identificador": "20210001",
  "senha": "AlunoS3nh@Forte!"
}
```

- Professor:

```json
{
  "identificador": "prof.ana@ufpr.br",
  "senha": "ProfS3nh@Forte!"
}
```

- Secretaria:

```json
{
  "identificador": "secretaria@ufpr.br",
  "senha": "SecrS3nh@Forte!"
}
```

- Coordenador:

```json
{
  "identificador": "coord.tads@ufpr.br",
  "senha": "CoordS3nh@Forte!"
}
```

- Egresso:

```json
{
  "identificador": "ana.egressa@ufpr.br",
  "senha": "EgressoS3nh@Forte!"
}
```

---

## Passo 3 — Confirmar o JWT

```
GET {{baseUrl}}/me
Authorization: Bearer {{accessToken}}
```

**Esperado 200:** objeto com `id`, `email`, `roles`, `_links` (`self`, `update-profile`, `change-password`, …). Copie `id` se for o usuário que você vai testar.

Sem Bearer: **401** Problem Details (passo 5).

---

## Passo 4 — Refresh (rotação)

| Campo | Valor |
|-------|--------|
| Method | `POST` |
| URL | `{{baseUrl}}/auth/refresh` |
| Body | JSON abaixo **ou** vazio se o cookie jar enviar `refresh_token` |

Cole no Body:

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Se o controller exige JSON, cole o valor do cookie `refresh_token` em `{{refreshToken}}`.

**Esperado 200:**

```json
{
  "accessToken": "eyJ…novo…",
  "refreshToken": "uuid-opaco…",
  "tokenType": "Bearer"
}
```

Atualize `{{accessToken}}`. O cookie `refresh_token` é substituído.

**Reuso (teste negativo):** envie o **token antigo** de novo → **401** e todas as sessões revogadas (`SUSPICIOUS_TOKEN_REUSE` no audit). Depois disso o login precisa ser feito de novo.

---

## Passo 5 — Credenciais inválidas (anti-enumeração)

Cole no Body:

```json
{
  "identificador": "naoexiste@ufpr.br",
  "senha": "SenhaErrada123!"
}
```

**Esperado 401** `Content-Type: application/problem+json`:

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Não autorizado",
  "status": 401,
  "detail": "Credenciais inválidas. Verifique seus dados e tente novamente."
}
```

A mensagem é **igual** para e-mail inexistente, senha errada e conta bloqueada. Não deve vazar “usuário não encontrado”.

---

## Passo 6 — Rate limit (opcional, destrutivo)

Dispare o Passo 5 **6 vezes em menos de 1 minuto** com o mesmo identificador.

**Esperado 429:**

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/rate-limit",
  "title": "Muitas tentativas",
  "status": 429,
  "detail": "Muitas tentativas. Aguarde antes de tentar novamente.",
  "retryAfterSeconds": 47
}
```

Header `Retry-After` presente. Bucket: 5 req/min por IP+identificador.

---

## Passo 7 — Logout

```
POST {{baseUrl}}/auth/logout
Authorization: Bearer {{accessToken}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200** (mensagem de sessão encerrada). Cookie `refresh_token` apagado. `POST /auth/refresh` a seguir → 401.

---

## Variação `mustChangePassword`

Login de usuário recém-criado (ainda com senha temporária):

```json
{
  "accessToken": "eyJ…",
  "tokenType": "Bearer",
  "mustChangePassword": true,
  "mustAcceptLgpd": true
}
```

O token **é válido** — use-o só em [T-F1-002](../F1-aluno/T-F1-002-primeiro-acesso.md). `GET /bff/dashboard/aluno` pode ser bloqueado no SPA; na API o `@PreAuthorize` das rotas de negócio ainda vale.

---

## Checklist

- [ ] CSRF 200 + `{{xsrfToken}}` preenchido
- [ ] Login 200 + Bearer no environment
- [ ] `GET /me` 200
- [ ] Refresh 200 e token antigo recusado
- [ ] Login inválido 401 genérico
- [ ] Logout invalida refresh
