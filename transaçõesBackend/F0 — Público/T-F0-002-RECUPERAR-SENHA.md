# T-F0-002 — Solicitar Link de Recuperação de Senha

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-002-RECUPERAR-SENHA.md`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-002-RECUPERAR-SENHA.md)  
> **Status:** ✅ Implementado — com divergência no canal de envio (ver nota de gap)

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller | [`AuthController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AuthController.kt) |
| Use Case | [`ForgotPasswordUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/ForgotPasswordUseCase.kt) |
| DTO | [`AuthDtos.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/dto/AuthDtos.kt) |
| JWT (token de 1 uso) | [`JwtTokenService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/JwtTokenService.kt) |
| Serviço de e-mail | [`MailService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/MailService.kt) |
| Rate Limit | [`RateLimitFilter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/RateLimitFilter.kt) |

---

## F0.2-a — Happy Path: E-mail cadastrado

### Fluxo completo

```
POST /auth/forgot-password
  → RateLimitFilter (se configurado para este endpoint)
  → AuthController.forgotPassword()
  → ForgotPasswordUseCase.execute(ForgotPasswordCommand)
    → DB: SELECT usuario BY email (normalizado lowercase)
    → SE existe e ativo:
        → JwtTokenService.issueOneTimeToken(sub=userId, audience="password-reset", ttl=24h)
        → MailService.sendPasswordResetEmail(to, nome, token)
        → AuditPublisher: PASSWORD_RESET_REQUESTED
    → SE não existe:
        → apenas log.debug (sem ação)
  → Response: 202 Accepted {mensagem: "Se este email existir, enviaremos..."}
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

O diagrama especifica proteção de 3 tentativas/hora por e-mail+IP. O `RateLimitFilter` atual implementa proteção apenas para `/auth/login`. A proteção de `/auth/forgot-password` precisa ser adicionada ao filtro com um bucket separado (janela de 1h, 3 req/hora).

> **Gap:** Rate limit de `/auth/forgot-password` **não está implementado** no `RateLimitFilter.kt` atual. O filtro só intercepta `POST /auth/login`.

---

## Nota sobre o Canal de Envio (Gap vs. Diagrama)

O diagrama `F0.2-a` especifica o padrão **Outbox** para o disparo do e-mail:

```
Diagrama:
  UC->>DB: INSERT outbox_event (iam.password_reset_requested)
  [depois] OutboxDispatcher → MailAdapter → e-mail
```

A implementação atual envia o e-mail **sincronamente** via `MailService.sendPasswordResetEmail()` dentro do `@Transactional` do use case:

```kotlin
// ForgotPasswordUseCase.kt — envio síncrono (atual)
mailService.sendPasswordResetEmail(
    to = usuario.email.value,
    nome = usuario.nome,
    token = token,
)
```

**Impacto:** Se o serviço de e-mail estiver lento ou indisponível, a requisição do usuário irá falhar ou demorar. O padrão Outbox tornaria isso assíncrono e resiliente. Para MVP isso é aceitável, mas a migração para Outbox é recomendada conforme o diagrama.

---

## Checklist de Verificação

- [x] `POST /auth/forgot-password` → sempre `202 Accepted`
- [x] E-mail existente → token JWT de 1 uso gerado (24h, audience="password-reset", JTI único)
- [x] E-mail existente → `MailService.sendPasswordResetEmail()` chamado
- [x] E-mail não existente → `202` sem token, sem e-mail, sem exceção
- [x] Audit log: `PASSWORD_RESET_REQUESTED` para e-mails encontrados
- [ ] Rate limit 3/hora em `/auth/forgot-password` — **não implementado**
- [ ] Padrão Outbox para disparo assíncrono — **envio é síncrono atualmente**
