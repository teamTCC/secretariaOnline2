# T-F0-002 — Solicitar Link de Recuperação de Senha

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-002-RECUPERAR-SENHA.md`](../../foundationDocs/sequenceDiagrams/F0 — Público/US-F0-002-RECUPERAR-SENHA.md)  
> **Status:** ✅ Implementado — Outbox + rate limit 3/hora + `retryAfterSeconds`

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller | [`AuthController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AuthController.kt) |
| Use Case | [`ForgotPasswordUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/ForgotPasswordUseCase.kt) |
| DTO | [`AuthDtos.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/dto/AuthDtos.kt) |
| JWT (token de 1 uso) | [`JwtTokenService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/JwtTokenService.kt) |
| Outbox (produtor) | [`ForgotPasswordUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/ForgotPasswordUseCase.kt) |
| Outbox (handler) | [`PasswordResetOutboxHandler.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/outbox/PasswordResetOutboxHandler.kt) |
| Dispatcher | [`OutboxDispatcher.kt`](../../backend/modules/notificacoes/src/main/kotlin/br/ufpr/sept/so2/modules/notificacoes/OutboxDispatcher.kt) |
| Serviço de e-mail | [`MailService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/MailService.kt) |
| Rate Limit | [`RateLimitFilter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/RateLimitFilter.kt) |

---

## F0.2-a — Happy Path: E-mail cadastrado

### Fluxo completo

```
POST /auth/forgot-password
  → RateLimitFilter (3 req/hora por email+IP; 429 + retryAfterSeconds se exceder)
  → AuthController.forgotPassword()
  → ForgotPasswordUseCase.execute(ForgotPasswordCommand)
    → DB: SELECT usuario BY email (normalizado lowercase)
    → SE existe e ativo:
        → JwtTokenService.issueOneTimeToken(sub=userId, audience="password-reset", ttl=24h)
        → INSERT outbox_event (iam.password_reset_requested, payload={email, nome, token})
        → AuditPublisher: PASSWORD_RESET_REQUESTED
        → COMMIT  (202 sai aqui — o SMTP ainda não rodou)
    → SE não existe:
        → apenas log.debug (sem ação)
  → Response: 202 Accepted {mensagem: "Se este email existir, enviaremos..."}

[assíncrono, a cada 5s]
  OutboxDispatcher
    → PasswordResetOutboxHandler
    → MailService.sendPasswordResetEmail(to, nome, token)
    → UPDATE outbox_event SET status='PROCESSED'
```

### JSON de entrada (Request Body)

```json
POST /auth/forgot-password
Content-Type: application/json

{
  "email": "ana@ufpr.br"
}
```

### DTO de entrada

```kotlin
// AuthDtos.kt
data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Formato de email inválido")
    val email: String,
)
```

### JSON de saída — 202 Accepted (igual para e-mail existente e inexistente)

```json
HTTP/1.1 202 Accepted
Content-Type: application/json

{
  "mensagem": "Se este email existir, enviaremos um link válido por 24h."
}
```

> A resposta é **bit-a-bit idêntica** para ambos os casos (e-mail existente e não existente) — princípio de anti-enumeração de contas.

### Controller — como ele responde

```kotlin
// AuthController.kt
@PostMapping("/forgot-password")
fun forgotPassword(
    @Valid @RequestBody request: ForgotPasswordRequest,
    httpRequest: HttpServletRequest,
): ResponseEntity<Map<String, String>> {
    forgotPasswordUseCase.execute(ForgotPasswordCommand(
        email = request.email,
        ip = httpRequest.remoteAddr,
    ))
    return ResponseEntity
        .status(HttpStatus.ACCEPTED)
        .body(mapOf("mensagem" to "Se este email existir, enviaremos um link válido por 24h."))
}
```

---

## Como o JWT de 1-uso é gerado

```kotlin
// ForgotPasswordUseCase.kt
val token = jwtTokenService.issueOneTimeToken(
    subject = usuario.id,
    audience = "password-reset",
    ttl = Duration.ofHours(24),
)
```

```kotlin
// JwtTokenService.kt
fun issueOneTimeToken(subject: UUID, audience: String, ttl: Duration): String {
    val jti = UUID.randomUUID().toString()  // JTI único para blacklist
    return Jwts.builder()
        .issuer(issuer)
        .id(jti)                  // JTI inserido na blacklist quando usado
        .subject(subject.toString())
        .audience().add(audience).and()  // audience = "password-reset"
        .expiration(Date(System.currentTimeMillis() + ttl.toMillis()))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact()
}
```

O token **não é persistido no banco antes do uso** — ele é gerado em memória e enviado ao e-mail. A blacklist de JTI é populada apenas quando o token é **consumido** em `POST /auth/reset-password` (coberto em [T-F0-003](T-F0-003-NOVA-SENHA.md)).

---

## F0.2-b — Anti-enumeração: E-mail não cadastrado

```kotlin
// ForgotPasswordUseCase.kt
val usuario = usuarioRepository.findByEmail(command.email.trim().lowercase())

if (usuario != null && usuario.ativo) {
    // gera token e envia e-mail
} else {
    // NÃO faz nada — apenas loga para monitoramento de segurança
    log.debug("Tentativa de recuperação de senha para email não cadastrado: {}", command.email)
}
// SEMPRE retorna 202, sem exceção
```

---

## F0.2-c — Rate Limit

O `RateLimitFilter` aplica um bucket separado de **3 req/hora por e-mail+IP** em `POST /auth/forgot-password`. O body é cacheado (`CachedBodyHttpServletRequest`) para o controller ainda conseguir desserializar o JSON.

Resposta ao exceder o limite:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 1847
Content-Type: application/problem+json

{
  "type": "https://secretariaonline.ufpr.br/errors/rate-limit",
  "title": "Muitas tentativas",
  "status": 429,
  "detail": "Muitas tentativas. Aguarde antes de tentar novamente.",
  "retryAfterSeconds": 1847
}
```

---

## Canal de Envio — Padrão Outbox (10.1a + 10.1b)

O use case **não** chama SMTP. Ele grava `outbox_event` na mesma `@Transactional`:

```kotlin
outboxRepo.save(
    OutboxEventEntity(
        eventType = OutboxEventTypes.PASSWORD_RESET_REQUESTED, // iam.password_reset_requested
        aggregateType = "Usuario",
        aggregateId = usuario.id,
        payload = mapOf(
            "email" to usuario.email.value,
            "nome" to usuario.nome,
            "token" to token,
        ),
    ),
)
```

O `OutboxDispatcher` (`@Scheduled(fixedDelay = 5000)`) busca linhas `PENDING`, roteia para `PasswordResetOutboxHandler`, que chama `MailService.sendPasswordResetEmail`. Se o SMTP falhar, a linha permanece `PENDING` com backoff (30s → 5min → 1h); após 8 tentativas vira `DEAD`.

**Por que isso importa:** o `202` não depende do SMTP. Restart do processo não perde o e-mail. Falha transitória de Mailhog/Mailgun é retentada.

---

## Checklist de Verificação

- [x] `POST /auth/forgot-password` → sempre `202 Accepted`
- [x] E-mail existente → token JWT de 1 uso gerado (24h, audience="password-reset", JTI único)
- [x] E-mail existente → `INSERT outbox_event(iam.password_reset_requested)` na mesma TX
- [x] Dispatcher → `PasswordResetOutboxHandler` → `MailService.sendPasswordResetEmail()`
- [x] E-mail não existente → `202` sem token, sem outbox, sem exceção
- [x] Audit log: `PASSWORD_RESET_REQUESTED` para e-mails encontrados
- [x] Rate limit 3/hora em `/auth/forgot-password`
- [x] `retryAfterSeconds` + header `Retry-After` no 429
