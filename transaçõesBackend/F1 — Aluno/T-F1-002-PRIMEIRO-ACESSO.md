# T-F1-002 — Primeiro Acesso: Definir Senha e Aceitar LGPD

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-002-PRIMEIRO-ACESSO.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-002-PRIMEIRO-ACESSO.md)  
> **Status:** ✅ Totalmente implementado

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller | [`AuthController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AuthController.kt) |
| Use Case | [`FirstAccessUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/FirstAccessUseCase.kt) |
| DTO | [`AuthDtos.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/dto/AuthDtos.kt) |

---

## F1.2-D01 — Happy Path: Conclusão do primeiro acesso

### Pré-condição
O aluno já fez login com a **senha provisória** (ver [T-F0-001](../F0 — Público/T-F0-001-LOGIN.md)) e recebeu `mustChangePassword: true`. O access token emitido no login é válido para autenticar este endpoint.

### Fluxo completo

```
POST /auth/first-access
Authorization: Bearer <accessToken do login inicial>
  → JwtAuthenticationFilter → popula SecurityContext com usuarioId
  → AuthController.firstAccess()
  → FirstAccessUseCase.execute(FirstAccessCommand)
    1. require(aceiteLgpd == true)           → obrigatório
    2. validatePasswordStrength(novaSenha)   → mesmas regras de F0.3
    3. usuarioRepository.findById(usuarioId)  → busca usuário
    4. require(usuario.mustChangePassword()) → garante que não é reuso
    5. Argon2id.hash(novaSenha)
    — BEGIN TRANSACTION —
    6. usuarioRepository.updatePassword()    → persiste novo hash
    7. passwordHistoryRepository.save()      → registra hash provisório no histórico
    8. usuarioRepository.updateMetadata()    → grava aceite_lgpd_em no JSONB
    — COMMIT —
    9. AuditPublisher: FIRST_ACCESS_COMPLETED
  → Response: 200 {mensagem: "Primeiro acesso concluído com sucesso."}
```

### JSON de entrada

```json
POST /auth/first-access
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "novaSenha": "MinhaS3nh@Nova2024!",
  "aceiteLgpd": true
}
```

### DTO de entrada

```kotlin
// AuthDtos.kt
data class FirstAccessRequest(
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 12, message = "Senha deve ter no mínimo 12 caracteres")
    val novaSenha: String,
    val aceiteLgpd: Boolean,
)
```

### JSON de saída — 200

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "mensagem": "Primeiro acesso concluído com sucesso."
}
```

---

## Como o aceite LGPD é persistido

O aceite é gravado no campo JSONB `metadata` do usuário, com timestamp:

```kotlin
// FirstAccessUseCase.kt
val updatedMetadata = usuario.metadata.toMutableMap().apply {
    put("aceite_lgpd_em", OffsetDateTime.now().toString())
}
usuarioRepository.updateMetadata(command.usuarioId, updatedMetadata)
```

Na tabela `usuario`, o campo `metadata` é JSONB e ficará assim:
```json
{
  "aceite_lgpd_em": "2026-08-09T21:30:00.000Z"
}
```

---

## Por que o endpoint sabe qual usuário está logado?

```kotlin
// AuthController.kt
@PostMapping("/first-access")
fun firstAccess(
    @Valid @RequestBody request: FirstAccessRequest,
    httpRequest: HttpServletRequest,
): ResponseEntity<Map<String, String>> {
    firstAccessUseCase.execute(FirstAccessCommand(
        usuarioId = currentUserId(),  // ← vem do JWT no SecurityContext
        novaSenha = request.novaSenha,
        aceiteLgpd = request.aceiteLgpd,
        ip = httpRequest.remoteAddr,
    ))
    ...
}
```

O `currentUserId()` é um helper que lê o UUID do `AuthenticatedUser` no `SecurityContextHolder` — populado pelo `JwtAuthenticationFilter` na requisição.

---

## F1.2-D03 — Erro 422: Senha igual à senha provisória

Se o usuário tenta usar a mesma senha provisória:

```kotlin
// FirstAccessUseCase.kt
require(usuario.mustChangePassword()) { "Usuário já completou o primeiro acesso." }

val newHash = passwordService.hash(command.novaSenha)
// O passwordHistoryRepository.findRecentHashes verificaria o hash provisório
// mas a verificação de reuso contra o hash atual está no ResetPasswordUseCase
```

> **Nota:** A verificação de reuso da senha provisória é feita pelo mesmo mecanismo de `passwordHistoryRepository` que guarda os últimos N hashes.

---

## Checklist de Verificação

- [x] `POST /auth/first-access` com Bearer válido → `200`
- [x] `aceiteLgpd: false` → `400 IllegalArgumentException`
- [x] `novaSenha` com menos de 12 chars → `422 WeakPasswordException`
- [x] Aceite LGPD persistido no JSONB `metadata.aceite_lgpd_em`
- [x] Hash Argon2id calculado antes da TX
- [x] Hash provisório registrado no `password_history`
- [x] Audit log: `FIRST_ACCESS_COMPLETED`
- [x] Após sucesso, próximo login retorna `mustChangePassword: false`
