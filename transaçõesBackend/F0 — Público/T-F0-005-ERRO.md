# T-F0-005 — Tratamento de Erros HTTP (GlobalExceptionHandler)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-005-ERRO.md`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-005-ERRO.md)  
> **Status:** ✅ Implementado — GlobalExceptionHandler + IamExceptionHandler

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Handler global (4xx/5xx) | [`shared/api/GlobalExceptionHandler.kt`](../../backend/shared/src/main/kotlin/br/ufpr/sept/so2/shared/api/GlobalExceptionHandler.kt) |
| Handler IAM (domínio auth) | [`iam/api/IamExceptionHandler.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/IamExceptionHandler.kt) |

---

## Formato padrão de erro — RFC 7807 Problem Details

Todos os erros do backend seguem o formato RFC 7807 (`application/problem+json`):

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/<tipo>",
  "title": "Descrição curta",
  "status": 4xx,
  "detail": "Mensagem legível para o usuário final.",
  "instance": "/caminho/que/gerou/o/erro"
}
```

---

## Mapeamento de exceções → HTTP

| Exceção de domínio | HTTP Status | `type` |
|--------------------|-------------|--------|
| `InvalidCredentialsException` | `401` | `unauthorized` |
| `AccountBlockedException` | `401` | `unauthorized` (anti-enumeração) |
| `InvalidTokenException` | `401` | `unauthorized` |
| `TokenReuseException` | `401` | `token_reuse_detected` |
| `PasswordReuseException` | `422` | `password-reuse` |
| `WeakPasswordException` | `422` | `weak-password` |
| `MethodArgumentNotValidException` (Jakarta @Valid) | `400` | `validation-error` |
| `NoSuchElementException` | `404` | `not-found` |
| `IllegalArgumentException` | `400` | `bad-request` |
| `AccessDeniedException` (Spring Security) | `403` | `forbidden` |
| `Exception` genérica | `500` | `internal-server-error` |

---

## Checklist de Verificação

- [x] Todos os erros retornam `Content-Type: application/problem+json`
- [x] Campo `status` sempre presente e igual ao HTTP status code
- [x] Erros 4xx nunca expõem stack traces ao cliente
- [x] Erros 401 para login/credenciais são idênticos (anti-enumeração)
