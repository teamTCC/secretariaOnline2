# T-F0-003 — Definir Nova Senha via Token

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-003-NOVA-SENHA.md`](../../foundationDocs/sequenceDiagrams/F0 — Público/US-F0-003-NOVA-SENHA.md)  
> **Status:** ✅ Totalmente implementado

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller | [`AuthController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AuthController.kt) |
| Use Case | [`ResetPasswordUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/ResetPasswordUseCase.kt) |
| DTO | [`AuthDtos.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/dto/AuthDtos.kt) |
| Argon2 (hash + comparação) | [`Argon2PasswordService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/Argon2PasswordService.kt) |
| JTI Blacklist | [`IamJpaRepositories.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/persistence/IamJpaRepositories.kt) |
| Histórico de senhas | `PasswordHistoryEntity.kt` + `PasswordHistoryJpaRepository` |

---

## F0.3-a — Happy Path: Redefinição bem-sucedida

### Fluxo completo

```
POST /auth/reset-password
  → AuthController.resetPassword()
  → ResetPasswordUseCase.execute(ResetPasswordCommand)
    1. jwtTokenService.verify(token)         → valida assinatura RS256, exp
    2. Verifica audience == "password-reset"
    3. Extrai JTI e sub (usuarioId)
    4. jtiBlacklistRepository.exists(jti)    → garante 1 único uso
    5. usuarioRepository.findById(usuarioId) → busca o usuário
    6. validatePasswordStrength(novaSenha)   → mínimo 12 chars, letras, dígitos, especial
    7. passwordHistoryRepository.findRecentHashes(3) → verifica reuso
    8. passwordService.hash(novaSenha)       → Argon2id (ANTES da TX, CPU-bound)
    — BEGIN TRANSACTION —
    9. usuarioRepository.updatePassword()    → salva novo hash
    10. passwordHistoryRepository.save()     → guarda hash antigo no histórico
    11. jtiBlacklistRepository.add(jti, exp) → invalida o token para sempre
    12. refreshTokenRepository.revokeAllForUser() → logout em todos os dispositivos
    — COMMIT —
    13. AuditPublisher: PASSWORD_CHANGED
  → Response: 200 {mensagem: "Senha redefinida com sucesso. Faça login novamente."}
```

### JSON de entrada

```json
POST /auth/reset-password
Content-Type: application/json

{
  "token": "eyJhbGciOiJSUzI1NiJ9...",
  "novaSenha": "NovaS3nh@Forte2024!"
}
```

### DTO de entrada

```kotlin
// AuthDtos.kt
data class ResetPasswordRequest(
    @field:NotBlank(message = "Token é obrigatório")
    val token: String,
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 12, message = "Senha deve ter no mínimo 12 caracteres")
    val novaSenha: String,
)
```

### JSON de saída — 200

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "mensagem": "Senha redefinida com sucesso. Faça login novamente."
}
```

---

## Validação de força da senha (backend)

```kotlin
// ResetPasswordUseCase.kt — validatePasswordStrength
private fun validatePasswordStrength(password: String) {
    if (password.length < 12) throw WeakPasswordException("mínimo 12 caracteres")
    if (!password.any { it.isUpperCase() }) throw WeakPasswordException("requer pelo menos uma letra maiúscula")
    if (!password.any { it.isLowerCase() }) throw WeakPasswordException("requer pelo menos uma letra minúscula")
    if (!password.any { it.isDigit() }) throw WeakPasswordException("requer pelo menos um dígito")
    if (!password.any { "!@#\$%^&*()_+-=[]{}|;':\",./<>?".contains(it) }) {
        throw WeakPasswordException("requer pelo menos um caractere especial")
    }
}
```

> Validação **duplicada no backend** independentemente do que o frontend faça — regra de segurança inegociável.

---

## Verificação de histórico de senhas

```kotlin
// ResetPasswordUseCase.kt
val recentHashes = passwordHistoryRepository.findRecentHashes(usuarioId, limit = 3)
val isReused = recentHashes.any { hash -> passwordService.verify(command.novaSenha, hash) }
if (isReused) throw PasswordReuseException()
```

> O `Argon2id.verify()` é aplicado em cada um dos 3 hashes do histórico. Operação CPU-bound (proposital — dificulta ataques de força bruta em caso de vazamento do histórico).

---

## JTI Blacklist — garante uso único do token

```kotlin
// ResetPasswordUseCase.kt
// Verificação ANTES de alterar a senha
if (jtiBlacklistRepository.exists(jti)) {
    throw InvalidTokenException("Token já utilizado. Solicite um novo link.")
}

// Registro DENTRO da transação (após alterar a senha com sucesso)
jtiBlacklistRepository.add(jti, OffsetDateTime.ofInstant(
    claims.payload.expiration.toInstant(), java.time.ZoneOffset.UTC
))
```

A inserção na blacklist e a atualização da senha são **atômicas** — não há janela de oportunidade para duplo uso.

---

## F0.3-b — Erro 401: Token inválido/expirado/já consumido

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Token inválido",
  "status": 401,
  "detail": "Token de redefinição de senha inválido ou expirado."
}
```

> Mensagem **não diferencia** se o token foi expirado, inválido ou já usado — anti-enumeração.

---

## F0.3-c — Erro 422: Senha reutilizada

```json
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "https://secretariaonline.ufpr.br/errors/password-reuse",
  "title": "Senha já utilizada",
  "status": 422,
  "detail": "Esta senha já foi utilizada recentemente."
}
```

> O JTI **não** é inserido na blacklist neste caso — o token permanece válido para o usuário tentar uma senha diferente.

---

## Transação atômica — segurança de dados

```kotlin
// ResetPasswordUseCase.kt — tudo dentro de @Transactional
usuarioRepository.updatePassword(usuarioId, newHash)
passwordHistoryRepository.save(usuarioId, usuario.senhaHash)
jtiBlacklistRepository.add(jti, exp)        // invalida token
refreshTokenRepository.revokeAllForUser(usuarioId)  // logout everywhere
```

Se qualquer operação falhar (ex: DB momentaneamente indisponível), **toda a transação reverte**. O token permanece válido e o usuário pode tentar novamente.

---

## Checklist de Verificação

- [x] `POST /auth/reset-password` com token válido e senha forte → `200`
- [x] Verificação de assinatura RS256 + audience="password-reset" + exp
- [x] JTI blacklist: segundo uso do mesmo token → `401`
- [x] Senha < 12 chars, sem maiúscula, sem dígito, sem especial → `422` (WeakPassword)
- [x] Senha igual a uma das 3 últimas → `422` (PasswordReuse) — token **não** blacklistado
- [x] Ao sucesso: todas as sessões revogadas (refresh tokens)
- [x] Audit log: `PASSWORD_CHANGED`
- [x] Operação Argon2id.hash() calculada **antes** do BEGIN TX (CPU-bound fora da transação)
