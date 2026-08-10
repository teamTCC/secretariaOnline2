# T-F0-006/007 — Verificar Protocolo / Verificar Certificado (Públicos)

> **Diagramas de referência:**  
> - [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-006-VERIFICAR-PROTOCOLO.md`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-006-VERIFICAR-PROTOCOLO.md)  
> - [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-007-VERIFICAR-CERTIFICADO.md`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-007-VERIFICAR-CERTIFICADO.md)  
> **Status:** ⏳ Parcialmente implementado — endpoints públicos mapeados, lógica de verificação pendente

---

## Arquivos existentes

| Papel | Arquivo |
|-------|---------|
| Controller público | [`presenca/api/PublicoController.kt`](../../backend/modules/presenca/src/main/kotlin/br/ufpr/sept/so2/modules/presenca/api/PublicoController.kt) |
| JWKS endpoint | [`app/config/JwksController.kt`](../../backend/app/src/main/kotlin/br/ufpr/sept/so2/config/JwksController.kt) |

---

## US-F0-007: Verificar Certificado

### O que o diagrama especifica

```
GET /publico/verificar-certificado/{hash}
  → DB: SELECT certificate WHERE hash = :hash
  → Verificar assinatura ED25519 com chave pública
  → Retornar dados do certificado (aluno, evento, data, horas)

GET /.well-known/jwks.json
  → Retornar chave pública RSA para verificação offline
```

### O que está implementado

O endpoint `GET /.well-known/jwks.json` está implementado no `JwksController` e retorna a chave pública RSA (não ED25519 — o diagrama especifica ED25519 para certificados, mas a implementação atual usa RSA para JWT).

> **Gap:** A verificação de certificado via hash SHA-256 + assinatura ED25519 **não está implementada** ainda. A entidade `Certificate` e a tabela `certificate` existem no schema Flyway, mas o `PublicoController` não tem a lógica de verificação.

---

## US-F0-006: Verificar Protocolo

> **Gap:** O endpoint de verificação de protocolo de solicitação (consulta pública sem login via número de protocolo) **não está implementado**. O `RequestController` tem os endpoints autenticados, mas não há endpoint público.

---

## Checklist de Verificação

- [ ] `GET /publico/verificar-certificado/{hash}` → **não implementado**
- [ ] Verificação de assinatura ED25519 → **não implementado**
- [x] `GET /.well-known/jwks.json` → implementado (RSA public key para JWT)
- [ ] `GET /publico/verificar-protocolo/{numero}` → **não implementado**
