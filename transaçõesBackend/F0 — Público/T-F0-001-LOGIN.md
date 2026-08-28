# T-F0-001 — Autenticação de Usuário (Login / Refresh / Logout)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-001-LOGIN.md`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-001-LOGIN.md)  
> **Status:** ✅ Implementado — dual cookie HttpOnly (access_token + refresh_token)

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller (API) | [`backend/modules/iam/src/main/kotlin/.../iam/api/AuthController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AuthController.kt) |
| DTOs de entrada/saída | [`backend/modules/iam/src/main/kotlin/.../iam/api/dto/AuthDtos.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/dto/AuthDtos.kt) |
| Use Case (login) | [`backend/modules/iam/src/main/kotlin/.../iam/application/LoginUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/LoginUseCase.kt) |
| Use Case (renovação de token) | [`backend/modules/iam/src/main/kotlin/.../iam/application/RefreshTokenUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/RefreshTokenUseCase.kt) |
| Revogação Redis (port) | [`backend/modules/iam/src/main/kotlin/.../iam/application/ports/out/TokenRevocationPort.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/ports/out/TokenRevocationPort.kt) |
| Revogação Redis (adapter) | [`backend/modules/iam/src/main/kotlin/.../iam/infrastructure/adapters/RedisTokenRevocationAdapter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/adapters/RedisTokenRevocationAdapter.kt) |
| Rate Limit (Bucket4j) | [`backend/modules/iam/src/main/kotlin/.../iam/security/RateLimitFilter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/RateLimitFilter.kt) |
| JWT (emissão + verificação) | [`backend/modules/iam/src/main/kotlin/.../iam/infrastructure/services/JwtTokenService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/JwtTokenService.kt) |
| Argon2id (verificação de senha) | [`backend/modules/iam/src/main/kotlin/.../iam/infrastructure/services/Argon2PasswordService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/Argon2PasswordService.kt) |
| Filtro JWT (cookie + Bearer fallback) | [`backend/modules/iam/src/main/kotlin/.../iam/security/JwtAuthenticationFilter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/JwtAuthenticationFilter.kt) |

---

## Estratégia de Cookies (Dual Cookie HttpOnly)

Ambos os tokens JWT são entregues **exclusivamente via cookies HttpOnly** — nenhum token aparece no corpo JSON das respostas.

| Cookie | Path | Max-Age | HttpOnly | Secure | SameSite |
|--------|------|---------|----------|--------|----------|
| `access_token` | `/` | TTL do JWT (15 min padrão) | ✅ | env `COOKIE_SECURE` | env `COOKIE_SAME_SITE` |
| `refresh_token` | `/auth` | 7 dias | ✅ | env `COOKIE_SECURE` | env `COOKIE_SAME_SITE` |

**Path=/auth para o refresh token** garante que ele só é enviado para os endpoints `/auth/*`, reduzindo a superfície de exposição.

### Configuração por ambiente

| Ambiente | `COOKIE_SECURE` | `COOKIE_SAME_SITE` |
|----------|-----------------|---------------------|
| Dev local (HTTP) | `false` | `Lax` |
| Contêiner Docker interno | `false` | `Lax` |
| Prod HTTPS (mesmo domínio) | `true` | `Lax` |
| Prod HTTPS (Vercel → API cross-origin) | `true` | `None` |

> `SameSite=None` requer obrigatoriamente `Secure=true`. O `AuthController` aplica `secure=true` automaticamente quando `SameSite=None`.

---

## F0.1-a — Happy Path: Login bem-sucedido

### Fluxo completo

```
POST /auth/login
  → RateLimitFilter (Bucket4j: 5 req/min por IP+identificador)
  → AuthController.login()
  → LoginUseCase.execute(LoginCommand)
    → DB: SELECT usuario BY identificador
    → Argon2id.verify(senha, senhaHash)
    → DB: INSERT refresh_token
    → DB: INSERT audit_log (LOGIN_SUCCESS)
  → Response: 200 {mustChangePassword, mustAcceptLgpd}
             + Set-Cookie: access_token  (HttpOnly, Path=/)
             + Set-Cookie: refresh_token (HttpOnly, Path=/auth)
```

### JSON de entrada (Request Body)

```json
POST /auth/login
Content-Type: application/json

{
  "identificador": "ana@ufpr.br",
  "senha": "MinhaS3nh@Forte!"
}
```

> `identificador` aceita **e-mail @ufpr.br**, **e-mail pessoal** ou **GRR numérico** (ex: `"20210001"`).

### JSON de saída — sucesso (200)

```
HTTP/1.1 200 OK
Set-Cookie: access_token=eyJhbGci...; HttpOnly; Path=/; SameSite=Lax; Max-Age=900
Set-Cookie: refresh_token=abc123...; HttpOnly; Path=/auth; SameSite=Lax; Max-Age=604800

{
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

> **Tokens NUNCA aparecem no corpo JSON.** O frontend não precisa de nenhum interceptor de token — o browser gerencia os cookies automaticamente.

### DTO de saída

```kotlin
data class LoginResponse(
    val mustChangePassword: Boolean,
    val mustAcceptLgpd: Boolean,
)
```

### Como o JWT é emitido (com JTI)

O `JwtTokenService.issueAccessToken(usuario)` cria um RS256 JWT com:
- **jti**: UUID único (necessário para revogação Redis no logout)
- **sub**: UUID do usuário
- **authorities**: lista de capabilities FGAC
- **nome**: nome do usuário
- **exp**: 15 minutos (configurável via `security.jwt.access-token-ttl-seconds`)
- **iss**: `secretaria-online-2`

---

## F0.1-b — Variação: `mustChangePassword = true`

```json
{
  "mustChangePassword": true,
  "mustAcceptLgpd": false
}
```

O SPA redireciona para `/primeiro-acesso`. O token no cookie já está ativo — permite autenticar o `POST /auth/first-access`.

---

## F0.1-c — Erro 401: Credenciais Inválidas

### JSON de saída — 401 (RFC 7807)

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Não autorizado",
  "status": 401,
  "detail": "Credenciais inválidas. Verifique seus dados e tente novamente."
}
```

> Mensagem **idêntica** para e-mail inexistente, senha errada e conta bloqueada (anti-enumeração).

---

## F0.1-d — Erro 429: Rate Limit

```json
HTTP/1.1 429 Too Many Requests
Content-Type: application/problem+json

{
  "type": "https://secretariaonline.ufpr.br/errors/rate-limit",
  "title": "Muitas tentativas",
  "status": 429,
  "detail": "Muitas tentativas. Aguarde antes de tentar novamente."
}
```

Header `Retry-After` presente. Bucket: 5 req/min por IP+identificador.

---

## F0.1-e — Refresh Token: Rotação via Cookie

### Fluxo

```
POST /auth/refresh
  (sem body — refresh_token lido do cookie HttpOnly automaticamente)
  → AuthController.refresh()
    → extrai refresh_token do cookie (HttpOnly)
  → RefreshTokenUseCase.execute()
    → DB: SELECT refresh_token WHERE value = <cookie>
    → isExpired()? → 401
    → isUsed() ou isRevoked()? → RISCO DE ROUBO
      → DB: revokeAllForUser
      → Redis: forceLogoutUser(userId, TTL=accessTokenTTL) ← força expiração imediata
      → 401
    → DB: markUsed(oldToken)
    → DB: INSERT novo refresh_token
    → JWT: novo accessToken
  → Response: 200 {mensagem}
             + Set-Cookie: access_token  (novo, HttpOnly)
             + Set-Cookie: refresh_token (novo, HttpOnly)
```

### Saída — sucesso (200)

```
HTTP/1.1 200 OK
Set-Cookie: access_token=<novo>; HttpOnly; Path=/; SameSite=Lax; Max-Age=900
Set-Cookie: refresh_token=<novo>; HttpOnly; Path=/auth; SameSite=Lax; Max-Age=604800

{
  "mensagem": "Token renovado com sucesso."
}
```

### Detecção de reuso com força-logout Redis

Quando um token já-utilizado é apresentado:
1. Todos os refresh tokens do usuário são revogados no BD
2. Um marcador `auth:force-logout:user:<uuid>` é gravado no Redis com TTL = accessTokenTTL
3. O `JwtAuthenticationFilter` rejeita qualquer access token com `iat < forceLogoutAt`
4. O usuário é efectivamente "expulso" em ≤ tempo máximo de 1 request

---

## F0.1-f — Logout com Blacklist Redis

### Fluxo

```
POST /auth/logout
  Authorization: Bearer <access_token>  (ou cookie access_token)
  X-XSRF-TOKEN: <token>
  → AuthController.logout()
    → extrai JTI + exp do access token atual
    → Redis: SET auth:revoked:jti:<jti> "1" EX <ttl_restante>
    → DB: revokeAllForUser(userId)
    → limpa cookies access_token e refresh_token (MaxAge=0)
  → 200 { mensagem }
```

### Saída — sucesso (200)

```
HTTP/1.1 200 OK
Set-Cookie: access_token=; HttpOnly; Path=/; Max-Age=0
Set-Cookie: refresh_token=; HttpOnly; Path=/auth; Max-Age=0

{
  "mensagem": "Sessão encerrada com sucesso."
}
```

### Policiamento Redis: dois mecanismos

| Mecanismo | Chave Redis | TTL | Uso |
|-----------|-------------|-----|-----|
| JTI blacklist | `auth:revoked:jti:<jti>` | Expiry do token | Logout individual |
| Force-logout do usuário | `auth:force-logout:user:<uuid>` | accessTokenTTL | Reuso de refresh token detectado |

O `JwtAuthenticationFilter` verifica **ambos** antes de aceitar um token:

```kotlin
// 1. JTI individualmente blacklistado?
if (jti != null && tokenRevocationPort.isRevoked(jti)) → reject

// 2. Usuário com force-logout mais recente que o iat do token?
if (issuedAt != null && tokenRevocationPort.isUserForcedLogout(userId, issuedAt)) → reject
```

---

## Validação JWT por request (filtro)

O `JwtAuthenticationFilter` roda em todo request:

```
Prioridade de extração do token:
  1. Cookie access_token (HttpOnly — fluxo browser)
  2. Authorization: Bearer <token> (fallback — httpie, Swagger, testes)
```

```kotlin
private fun extractToken(request: HttpServletRequest): String? {
    // Cookie primeiro
    request.cookies?.firstOrNull { it.name == "access_token" }?.value
        ?.takeIf { it.isNotBlank() }?.let { return it }

    // Bearer fallback
    val header = request.getHeader("Authorization") ?: return null
    return if (header.startsWith("Bearer ")) header.removePrefix("Bearer ").trim() else null
}
```

---

## CSRF — Double Submit Cookie

Mutações autenticadas (`POST`/`PATCH`/`PUT`/`DELETE`) exigem o header `X-XSRF-TOKEN` igual ao cookie `XSRF-TOKEN` (não httpOnly, para o SPA ler).

```
GET /auth/csrf
→ 200 { "token", "headerName": "X-XSRF-TOKEN", "parameterName" }
Set-Cookie: XSRF-TOKEN=…; Path=/; SameSite=Lax
```

Isentos: `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, Swagger, Actuator, JWKS.

---

## Checklist de Verificação

- [x] `POST /auth/login` → `200` com flags `mustChangePassword/mustAcceptLgpd` + `access_token` e `refresh_token` nos cookies HttpOnly
- [x] `accessToken` **não aparece** no corpo JSON
- [x] `refreshToken` **não aparece** no corpo JSON
- [x] Login com e-mail `@ufpr.br`, e-mail pessoal e GRR numérico
- [x] `mustChangePassword: true` quando `senha_alterada = false`
- [x] `401` genérico para credenciais inválidas (anti-enumeração)
- [x] `429` após 5 tentativas em 1 min
- [x] `POST /auth/refresh` sem body → lê cookie → renova par de tokens via novos cookies
- [x] Reuso de refresh token → revokeAllForUser (DB) + forceLogoutUser (Redis) + `401`
- [x] `POST /auth/logout` → blacklist JTI no Redis + revoga refresh tokens no DB + limpa ambos os cookies
- [x] `JwtAuthenticationFilter` verifica JTI blacklist + force-logout antes de aceitar o token
- [x] Bearer fallback funciona para testes via httpie/Swagger
- [x] CSRF Double Submit: `GET /auth/csrf` emite cookie `XSRF-TOKEN` + login/refresh/forgot/reset são isentos
