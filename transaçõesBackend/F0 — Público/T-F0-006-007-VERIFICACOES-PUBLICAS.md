# T-F0-006/007 — Verificar Protocolo / Verificar Certificado (Públicos)

> **Diagramas de referência:**  
> - [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-006-VERIFICAR-PROTOCOLO.md`](../../foundationDocs/sequenceDiagrams/F0 — Público/US-F0-006-VERIFICAR-PROTOCOLO.md)  
> - [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-007-VERIFICAR-CERTIFICADO.md`](../../foundationDocs/sequenceDiagrams/F0 — Público/US-F0-007-VERIFICAR-CERTIFICADO.md)  
> **Status:** ✅ Implementado — protocolo público, verificação ED25519, JWKS RSA+Ed25519, rate limit 10/min

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller protocolo | [`solicitacoes/api/PublicoSolicitacaoController.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/PublicoSolicitacaoController.kt) |
| Controller certificado | [`presenca/api/PublicoController.kt`](../../backend/modules/presenca/src/main/kotlin/br/ufpr/sept/so2/modules/presenca/api/PublicoController.kt) |
| JWKS | [`app/config/JwksController.kt`](../../backend/app/src/main/kotlin/br/ufpr/sept/so2/config/JwksController.kt) |
| Rate limit | [`iam/security/RateLimitFilter.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/RateLimitFilter.kt) |

Não exigem JWT (`permitAll(/publico/**)` e `/.well-known/**`). No OpenAPI, as operações usam `@SecurityRequirements` vazio.

---

## US-F0-006: Verificar Protocolo

```
GET /publico/solicitacoes/{ano}/{numero}
```

Exemplo: `GET /publico/solicitacoes/2025/42`

```json
{
  "protocolo": "2025/0042",
  "tipo": "REAPROVEITAMENTO_DISCIPLINA",
  "estado": "DELIBERADA",
  "abertaEm": "2025-03-15T10:00:00Z",
  "prazoEm": "2025-04-15T10:00:00Z",
  "_links": { "self": "/publico/solicitacoes/2025/42" }
}
```

Retorna só dados não-sigilosos (tipo, estado, datas). Sem nome do aluno.

---

## US-F0-007: Verificar Certificado

```
GET /publico/verificar-certificado/{hash}
```

```json
{
  "valido": true,
  "hashSha256": "a1b2…",
  "chCreditadas": 4.0,
  "issuedAt": "2026-08-13T20:00:00Z",
  "idEvento": "7c9e6679-…",
  "verificacaoAssinatura": "ED25519_VALID",
  "_links": { "jwks": "/.well-known/jwks.json" }
}
```

Regras de `valido`:

| Situação | `valido` |
|----------|----------|
| Assinatura ED25519 confere com `CERT_PUBLIC_KEY` | `true` |
| Hash inexistente | `404` |
| Prefixo `UNSIGNED_` ou `SIGN_ERROR_` (legado) | `false` |
| Chave pública vazia | não ocorre mais: par efêmero na subida |

A verificação agora recomputa SHA-256 do **PDF** no MinIO e valida Ed25519 sobre esse hash. Ver [T-10.4](../transversal/T-10.4-CERTIFICADO.md).

---

## JWKS

```
GET /.well-known/jwks.json
```

Retorna a chave RSA do JWT (`kty: RSA`) e, se `CERT_PUBLIC_KEY` estiver configurado, a chave Ed25519 dos certificados (`kty: OKP`, `crv: Ed25519`).

---

## Rate limit

`GET /publico/solicitacoes/**` e `GET /publico/verificar-certificado/**`: **10 req/min por IP**. Sem wrap do body (são GET). 429 com `retryAfterSeconds` + header `Retry-After`.

---

## Checklist de Verificação

- [x] `GET /publico/solicitacoes/{ano}/{numero}` — dados públicos do protocolo
- [x] `GET /publico/verificar-certificado/{hash}` — verificação ED25519 real
- [x] `UNSIGNED_*` legado → `valido: false`; chaves efêmeras se env vazio
- [x] Integridade do PDF no MinIO + Ed25519
- [x] `GET /.well-known/jwks.json` — RSA (JWT) + Ed25519 (certificados)
- [x] Rate limit 10/min por IP nas consultas públicas
