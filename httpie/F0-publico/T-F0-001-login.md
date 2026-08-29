# T-F0-001 — Login, refresh, logout, CSRF e `POST /auth/ott`

> **Transação:** [`T-F0-001-LOGIN.md`](../../transaçõesBackend/F0%20—%20Público/T-F0-001-LOGIN.md)  
> **Diagrama:** [`US-F0-001-LOGIN.md`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-001-LOGIN.md)

Controller: `POST /auth/login` · `POST /auth/refresh` · `POST /auth/ott` · `POST /auth/logout` · `GET /auth/csrf`.

---

## Dual Cookie — Como funciona nos testes

Os tokens **não aparecem no body** das respostas. O backend define:

| Cookie | Path | MaxAge |
|--------|------|--------|
| `access_token` | `/` | 900 s (15 min) |
| `refresh_token` | `/auth` | 604800 s (7 dias) |

Com httpie, use `--session <arquivo>` para que os cookies sejam guardados e reenviados automaticamente entre requests.

**Fluxo recomendado (httpie CLI):**

```bash
# Cria/atualiza session file após login — cookies ficam salvos
http --session=./session.json POST {{baseUrl}}/auth/login \
  identificador="admin@ufpr.br" \
  senha="Admin@123456"

# Requests seguintes reenviam access_token via cookie automaticamente
http --session=./session.json GET {{baseUrl}}/me
```

**Fallback Bearer (para Swagger UI ou testes pontuais):**

O filtro aceita `Authorization: Bearer <token>` se o cookie não estiver presente. Para usar no Swagger UI, extraia o valor do cookie `access_token` (DevTools → Application → Cookies) e cole no campo `Authorization`.

---

## Passo 1 — CSRF (sempre primeiro no dia)

| Campo | Valor |
|-------|--------|
| Method | `GET` |
| URL | `{{baseUrl}}/auth/csrf` |
| Auth | none |

**httpie CLI:**

```bash
http --session=./session.json GET {{baseUrl}}/auth/csrf
```

**Esperado 200:**

```json
{
  "token": "e7c1…",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf"
}
```

Salve `token` → `{{xsrfToken}}`. O cookie `XSRF-TOKEN` fica na session.

---

## Passo 2 — Login feliz (admin)

| Campo | Valor |
|-------|--------|
| Method | `POST` |
| URL | `{{baseUrl}}/auth/login` |
| Body | JSON abaixo |
| Nota | CSRF não é exigido neste endpoint |

```bash
http --session=./session.json POST {{baseUrl}}/auth/login \
  identificador="admin@ufpr.br" \
  senha="Admin@123456"
```

**Esperado 200:**

```
HTTP/1.1 200 OK
Set-Cookie: access_token=eyJhbGci…; HttpOnly; Path=/; SameSite=Lax; Max-Age=900
Set-Cookie: refresh_token=abc…;    HttpOnly; Path=/auth; SameSite=Lax; Max-Age=604800

{
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

> **`accessToken` não está mais no body.** Os cookies são gerenciados automaticamente pela session do httpie.

Variantes de body:

- Aluno e-mail:

```bash
http --session=./session.json POST {{baseUrl}}/auth/login \
  identificador="ana.aluno@ufpr.br" senha="AlunoS3nh@Forte!"
```

- Aluno GRR:

```bash
http --session=./session.json POST {{baseUrl}}/auth/login \
  identificador="20210001" senha="AlunoS3nh@Forte!"
```

- Professor:

```bash
http --session=./session.json POST {{baseUrl}}/auth/login \
  identificador="prof.ana@ufpr.br" senha="ProfS3nh@Forte!"
```

- Secretaria:

```bash
http --session=./session.json POST {{baseUrl}}/auth/login \
  identificador="secretaria@ufpr.br" senha="SecrS3nh@Forte!"
```

---

## Passo 3 — Confirmar acesso protegido

```bash
http --session=./session.json GET {{baseUrl}}/me
```

O cookie `access_token` é enviado automaticamente (cookie jar da session).

**Esperado 200:** objeto com `id`, `email`, `roles`, `_links`.

**Sem session (sem cookie):** `401` Problem Details.

**Com Bearer manual (fallback):**

```bash
# Extraia o access_token do cookie na session.json e use como Bearer
http GET {{baseUrl}}/me "Authorization: Bearer eyJhbGci..."
```

---

## Passo 4 — Refresh (rotação via cookie + nova sessão Redis)

```bash
# Sem body — o refresh_token é lido do cookie (Path=/auth)
http --session=./session.json POST {{baseUrl}}/auth/refresh
```

**Esperado 200:**

```
Set-Cookie: access_token=<novo>; HttpOnly; Path=/; Max-Age=900
Set-Cookie: refresh_token=<novo>; HttpOnly; Path=/auth; Max-Age=604800

{
  "mensagem": "Token renovado com sucesso."
}
```

Os cookies são atualizados automaticamente na session. Um novo `sid` (session ID) é gerado e registrado no Redis — o token antigo expira naturalmente no seu TTL (≤ 15 min).

**Reuso (teste negativo):**

Para simular reuso, copie manualmente o `refresh_token` da session.json **antes** do refresh, depois do refresh faça uma segunda chamada com o token antigo usando `Cookie: refresh_token=<token_antigo>`:

```bash
# Segundo refresh com token antigo → 401 + revogação de todas as sessões
http POST {{baseUrl}}/auth/refresh "Cookie: refresh_token=<token_antigo>"
```

Esperado `401` + log `SUSPICIOUS_TOKEN_REUSE` na auditoria.

---

## Passo 5 — Credenciais inválidas (anti-enumeração)

```bash
http POST {{baseUrl}}/auth/login \
  identificador="naoexiste@ufpr.br" \
  senha="SenhaErrada123!"
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

---

## Passo 7 — Logout (session Redis deletada + clear cookies)

```bash
http --session=./session.json POST {{baseUrl}}/auth/logout \
  "X-XSRF-TOKEN: {{xsrfToken}}"
```

**Esperado 200:**

```
Set-Cookie: access_token=; HttpOnly; Path=/; Max-Age=0
Set-Cookie: refresh_token=; HttpOnly; Path=/auth; Max-Age=0

{
  "mensagem": "Sessão encerrada com sucesso."
}
```

O que ocorre por baixo:
1. Extrai o `sid` (Session ID) do payload do access token atual
2. Redis: `DEL auth:session:<sid>` — token invalidado **instantaneamente**, independente do TTL
3. Todos os refresh tokens do usuário são revogados no BD
4. Ambos os cookies são limpos (MaxAge=0)

Após logout, `GET {{baseUrl}}/me` → `401` imediatamente (não aguarda expirar o JWT).

---

## Passo 8 — `POST /auth/ott` (deep-link `?ott=`)

Troca o JWT one-time do e-mail por sessão. CSRF **não** é exigido. `permitAll`. Rate limit **igual ao login** (`RateLimitFilter` 5/min).

Pegue `{{ottJwt}}` no payload do outbox (`solicitacoes.*` com `generateOneTimeToken`) — [T-10.1](../transversal/T-10.1-outbox.md). O compose operacional **não** inclui Mailhog.

```bash
http --session=./session.json POST {{baseUrl}}/auth/ott \
  token="{{ottJwt}}"
```

Body JSON:

```json
{ "token": "{{ottJwt}}" }
```

**Esperado 200** — mesmo contrato do login:

```
Set-Cookie: access_token=…; HttpOnly; Path=/
Set-Cookie: refresh_token=…; HttpOnly; Path=/auth

{
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

JSON **sem** `accessToken`/`refreshToken`. Cookies na session.

**Replay (teste negativo):** dispare o **mesmo** body de novo.

**Esperado 401** Problem Details (`unauthorized`) — JTI já consumido.

**Rate limit:** 6× `POST /auth/ott` no mesmo IP em < 1 min → **429** (mesmo filtro do Passo 6).

---

## Variação `mustChangePassword`

Login de usuário recém-criado:

```json
{
  "mustChangePassword": true,
  "mustAcceptLgpd": true
}
```

Cookies `access_token` e `refresh_token` são definidos normalmente. Use o token (via cookie) apenas em [T-F1-002](../F1-aluno/T-F1-002-primeiro-acesso.md).

---

## Checklist

- [ ] CSRF 200 + `{{xsrfToken}}` preenchido
- [ ] Login 200 + cookies `access_token` e `refresh_token` definidos (NÃO no body)
- [ ] `GET /me` 200 via cookie (sem Authorization header)
- [ ] Refresh 200 e cookies renovados; token antigo recusado
- [ ] Login inválido 401 genérico
- [ ] Logout: cookies limpos + request posterior a `/me` retorna 401
- [ ] `POST /auth/ott` 200 + cookies (flags no JSON, sem tokens)
- [ ] Segundo `POST /auth/ott` com o mesmo token → 401
