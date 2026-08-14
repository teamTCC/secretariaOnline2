# T-F0-001 — Autenticação de Usuário (Login)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-001-LOGIN.md`](../../foundationDocs/sequenceDiagrams/F0 — Público/US-F0-001-LOGIN.md)  
> **Status:** ✅ Totalmente implementado

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller (API) | [`backend/modules/iam/src/main/kotlin/.../iam/api/AuthController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AuthController.kt) |
| DTOs de entrada/saída | [`backend/modules/iam/src/main/kotlin/.../iam/api/dto/AuthDtos.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/dto/AuthDtos.kt) |
| Use Case (regras de negócio) | [`backend/modules/iam/src/main/kotlin/.../iam/application/LoginUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/LoginUseCase.kt) |
| Use Case (renovação de token) | [`backend/modules/iam/src/main/kotlin/.../iam/application/RefreshTokenUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/RefreshTokenUseCase.kt) |
| Rate Limit (Bucket4j) | [`backend/modules/iam/src/main/kotlin/.../iam/security/RateLimitFilter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/RateLimitFilter.kt) |
| JWT (emissão + verificação) | [`backend/modules/iam/src/main/kotlin/.../iam/infrastructure/services/JwtTokenService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/JwtTokenService.kt) |
| Argon2id (verificação de senha) | [`backend/modules/iam/src/main/kotlin/.../iam/infrastructure/services/Argon2PasswordService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/Argon2PasswordService.kt) |
| Filtro JWT (leitura do Bearer) | [`backend/modules/iam/src/main/kotlin/.../iam/security/JwtAuthenticationFilter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/JwtAuthenticationFilter.kt) |
| Teste | [`backend/modules/iam/src/test/kotlin/.../iam/application/LoginUseCaseTest.kt`](../../backend/modules/iam/src/test/kotlin/br/ufpr/sept/so2/modules/iam/application/LoginUseCaseTest.kt) |

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
  → Response: 200 {accessToken} + Set-Cookie: refresh_token (httpOnly)
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

> `identificador` aceita **e-mail @ufpr.br**, **e-mail pessoal** ou **GRR numérico** (ex: `"20210001"`). O `LoginUseCase` normaliza com `.trim().lowercase()` antes do SELECT.

### DTO de entrada

```kotlin
// AuthDtos.kt
data class LoginRequest(
    @field:NotBlank(message = "Identificador é obrigatório")
    val identificador: String,
    @field:NotBlank(message = "Senha é obrigatória")
    val senha: String,
)
```

### JSON de saída — sucesso (200)

```json
HTTP/1.1 200 OK
Set-Cookie: refresh_token=eyJhbGci...; HttpOnly; Secure; Path=/auth; SameSite=Lax; Max-Age=604800

{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer",
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

> **Nota sobre cookies:** O `refreshToken` **NÃO aparece no corpo JSON** da resposta — vai apenas no cookie `httpOnly`. O frontend nunca precisa ler o `refreshToken` via JavaScript. Isso é a implementação correta da segurança conforme o diagrama.

### DTO de saída

```kotlin
// AuthDtos.kt
data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val mustChangePassword: Boolean,
    val mustAcceptLgpd: Boolean,
)
```

### Como o JWT é emitido

O `JwtTokenService.issueAccessToken(usuario)` cria um RS256 JWT com:
- **sub**: UUID do usuário
- **authorities**: lista de capabilities FGAC (ex: `["dashboard.view_own", "request.open", "formative.submit"]`)
- **nome**: nome do usuário (para exibição no frontend)
- **exp**: 15 minutos (configurável via `security.jwt.access-token-ttl-seconds`)
- **iss**: `secretaria-online-2`

```kotlin
// JwtTokenService.kt
fun issueAccessToken(usuario: Usuario): String =
    Jwts.builder()
        .issuer(issuer)
        .subject(usuario.id.toString())
        .claim("authorities", usuario.authorities().toList())
        .claim("nome", usuario.nome)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + accessTtlSeconds * 1000))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact()
```

---

## F0.1-b — Variação: `mustChangePassword = true`

Quando a conta tem `senha_alterada = false` (primeiro acesso ou reset administrativo), a resposta muda apenas no campo `mustChangePassword`:

```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "mustChangePassword": true,
  "mustAcceptLgpd": false
}
```

O frontend deve redirecionar para `/primeiro-acesso` e bloquear todas as outras rotas enquanto esse flag for `true`. O token emitido é válido — permite autenticar o `POST /auth/first-access`.

---

## F0.1-c — Erro 401: Credenciais Inválidas (anti-enumeração)

### Fluxo no UseCase

```kotlin
// LoginUseCase.kt
val usuario = usuarioRepository.findByIdentificador(identificador)

// Usuário inexistente OU inativo → mesma exceção (anti-enumeração)
if (usuario == null || !usuario.ativo) {
    auditPublisher.publish(AuditPayload(acao = "LOGIN_FAILED", ...))
    throw InvalidCredentialsException()
}

// Senha errada → mesma exceção
if (!passwordService.verify(command.senha, usuario.senhaHash)) {
    handleFailedAttempt(usuario, command)
    throw InvalidCredentialsException()
}
```

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

> Não importa se o usuário não existe ou se a senha está errada — a mensagem é **sempre a mesma**, impedindo ataques de enumeração de contas.

---

## F0.1-d — Erro 429: Rate Limit

### Como funciona o `RateLimitFilter`

O filtro intercepta `POST /auth/login` **antes** do `AuthController`. Usa Bucket4j com janela deslizante de 5 requisições por minuto, chave composta por `IP + identificador`.

```kotlin
// RateLimitFilter.kt
val key = "${request.remoteAddr}:$identifier"
val bucket = loginBuckets.computeIfAbsent(key) { buildLoginBucket() }

if (!bucket.tryConsume(1)) {
    response.status = 429
    response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
    response.writer.write(objectMapper.writeValueAsString(mapOf(
        "title" to "Muitas tentativas",
        "status" to 429,
        "detail" to "Muitas tentativas. Aguarde antes de tentar novamente.",
        "type" to "https://secretariaonline.ufpr.br/errors/rate-limit"
    )))
    return // NÃO passa para o próximo filtro
}
```

### JSON de saída — 429

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

> **Nota:** O 429 inclui `retryAfterSeconds` e o header `Retry-After`. O mesmo filtro limita `POST /auth/forgot-password` (3/h por email+IP), `GET /publico/solicitacoes/**` + `GET /publico/verificar-certificado/**` (10/min por IP) e `POST /publico/contato` (10/min por IP).

---

## F0.1-e — Erro 401: Conta Bloqueada

Após 10 tentativas falhas consecutivas, a conta é bloqueada por 15 minutos:

```kotlin
// LoginUseCase.kt — handleFailedAttempt
val newAttempts = usuario.tentativasFalhas + 1
val bloqueadoAte = if (newAttempts >= Usuario.MAX_FAILED_ATTEMPTS) {
    OffsetDateTime.now().plusMinutes(Usuario.LOCK_DURATION_MINUTES)
} else null
usuarioRepository.updateFailedAttempts(usuario.id, newAttempts, bloqueadoAte)

auditPublisher.publish(AuditPayload(
    acao = if (bloqueadoAte != null) "ACCOUNT_BLOCKED" else "LOGIN_FAILED",
    ...
))
```

A resposta HTTP é **idêntica** ao caso de credenciais inválidas (`401` com a mesma mensagem) — não revela ao cliente que a conta está bloqueada.

---

## F0.1-f — Refresh Token: Rotação com Detecção de Reuso

### Fluxo de renovação (normal)

```
POST /auth/refresh
  Body: { "refreshToken": "abc123..." }
  → RefreshTokenUseCase.execute()
    → DB: SELECT refresh_token WHERE value = 'abc123...'
    → isExpired()? → lança InvalidTokenException
    → isUsed() ou isRevoked()? → RISCO DE ROUBO → revoga TODAS as sessões
    → DB: markUsed(oldToken)
    → DB: INSERT novo refresh_token
    → jwtTokenService.issueAccessToken(usuario)
  → Response: { accessToken: "...", refreshToken: "..." } + Set-Cookie atualizado
```

### JSON de entrada

```json
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "abc-def-123-..."
}
```

### JSON de saída — sucesso (200)

```json
HTTP/1.1 200 OK
Set-Cookie: refresh_token=<novo_token>; HttpOnly; Secure; Path=/auth; SameSite=Lax

{
  "accessToken": "eyJhbGci...",
  "refreshToken": "novo-token-aqui",
  "tokenType": "Bearer"
}
```

### Detecção de reuso (token theft protection)

```kotlin
// RefreshTokenUseCase.kt
if (stored.isUsed() || stored.isRevoked()) {
    // POSSÍVEL ROUBO — revogar TODAS as sessões do usuário
    refreshTokenRepository.revokeAllForUser(stored.usuarioId)
    auditPublisher.publish(AuditPayload(
        acao = "SUSPICIOUS_TOKEN_REUSE",
        resultado = "DENIED",
        detalhes = mapOf("razao" to "TOKEN_REUTILIZADO")
    ))
    throw InvalidTokenException("Token já utilizado — todas as sessões foram encerradas por segurança.")
}
```

Resposta: `401` + redirect para `/login`.

---

## CSRF — Double Submit Cookie

Mutações autenticadas (`POST`/`PATCH`/`PUT`/`DELETE`) exigem o header `X-XSRF-TOKEN` igual ao cookie `XSRF-TOKEN` (não httpOnly, para o SPA ler).

```
GET /auth/csrf
→ 200 { "token", "headerName": "X-XSRF-TOKEN", "parameterName" }
Set-Cookie: XSRF-TOKEN=…; Path=/; SameSite=Lax
```

Isentos: `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, Swagger, Actuator, JWKS.

Implementação: `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `SpaCsrfTokenRequestHandler` + `CsrfCookieFilter` em [`SecurityConfig.kt`](../../backend/app/src/main/kotlin/br/ufpr/sept/so2/config/SecurityConfig.kt). CORS permite o header `X-XSRF-TOKEN`.

---

## Como o JWT é validado em cada requisição protegida

O `JwtAuthenticationFilter` roda em todo request:

```kotlin
// JwtAuthenticationFilter.kt
extractToken(request)?.let { token ->
    val claims = jwtTokenService.verify(token)
    val userId = UUID.fromString(claims.payload.subject)
    val authorities = (claims.payload["authorities"] as? List<String>)?.toSet() ?: emptySet()
    
    val principal = AuthenticatedUser(userId = userId, authorities = authorities)
    val authentication = UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities)
    SecurityContextHolder.getContext().authentication = authentication
}
```

Depois, os controllers acessam o usuário autenticado com:

```kotlin
val user = currentUser() // helper em shared/security/AuthenticatedUser.kt
val alunoId = user.userId
val podeDeliberar = user.authorities.contains("request.deliberate")
```

---

## Checklist de Verificação

- [x] `POST /auth/login` → `200` com `accessToken` no body + `refresh_token` no cookie httpOnly
- [x] Login com e-mail `@ufpr.br`, e-mail pessoal e GRR numérico — todos normalizados para lowercase
- [x] `mustChangePassword: true` quando `senha_alterada = false`
- [x] `mustAcceptLgpd: true` quando `aceite_lgpd_em` não está no metadata
- [x] `401` com mensagem genérica para usuário inexistente E senha errada (anti-enumeração)
- [x] `429` após 5 tentativas em 1 min (mesmo IP+identificador)
- [x] Conta bloqueada após 10 falhas consecutivas — 401 sem revelar bloqueio
- [x] `POST /auth/refresh` com token válido → novo par de tokens
- [x] Reuso de refresh token → todas as sessões revogadas + `401`
- [x] `retryAfterSeconds` no corpo 429 + header `Retry-After`
- [x] `POST /auth/logout` → revoga todos os refresh tokens do usuário + apaga o cookie
- [x] CSRF Double Submit: `GET /auth/csrf` emite cookie `XSRF-TOKEN` (não httpOnly) + header `X-XSRF-TOKEN`; login/refresh/forgot/reset são isentos
