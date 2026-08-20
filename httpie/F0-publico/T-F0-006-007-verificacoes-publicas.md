# T-F0-006 / 007 — Verificar protocolo e certificado (público)

> **Transação:** [`T-F0-006-007`](../../transaçõesBackend/F0%20—%20Público/T-F0-006-007-VERIFICACOES-PUBLICAS.md)  
> **Diagramas:** [`US-F0-006`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-006-VERIFICAR-PROTOCOLO.md) · [`US-F0-007`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-007-VERIFICAR-CERTIFICADO.md)  
> **IDs:** `{{requestAno}}`, `{{requestNumero}}`, `{{certificateHash}}`

Sem JWT. Rate limit: **10 req/min por IP** nos dois GETs.

Você só terá protocolo/hash depois de [T-F1-005](../F1-aluno/T-F1-005-solicitacoes.md) e [T-F1-010](../F1-aluno/T-F1-010-011-certificados-atendimentos.md) (ou SQL do catálogo).

---

## Passo 1 — JWKS (chaves públicas)

```
GET {{baseUrl}}/.well-known/jwks.json
```

Link direto: [http://localhost:8080/.well-known/jwks.json](http://localhost:8080/.well-known/jwks.json)

**Esperado 200:** `keys[]` com:

- `kty: RSA` (JWT de acesso)
- `kty: OKP`, `crv: Ed25519` (certificados), se a chave de cert estiver ativa (em dev o par é efêmero na subida)

---

## Passo 2 — Protocolo público

Primeiro descubra ano/número:

```
GET {{baseUrl}}/requests/{{requestId}}/protocol
Authorization: Bearer {{accessToken}}
```

**Esperado (autenticado):**

```json
{
  "protocolo": "2026/0042",
  "tipo": "DECLARACAO_MATRICULA",
  "estado": "ABERTA",
  "_links": { "public": "/publico/solicitacoes/2026/42" }
}
```

Copie ano e número (sem zero à esquerda no path público) → `{{requestAno}}` / `{{requestNumero}}`.

Agora **sem** Bearer:

```
GET {{baseUrl}}/publico/solicitacoes/{{requestAno}}/{{requestNumero}}
```

Exemplo: [http://localhost:8080/publico/solicitacoes/2026/42](http://localhost:8080/publico/solicitacoes/2026/42)

**Esperado 200:**

```json
{
  "protocolo": "2026/0042",
  "tipo": "DECLARACAO_MATRICULA",
  "estado": "ABERTA",
  "abertaEm": "2026-08-19T17:00:00Z",
  "prazoEm": "2026-08-22T17:00:00Z",
  "_links": { "self": "/publico/solicitacoes/2026/42" }
}
```

**Não** deve aparecer nome do aluno nem `dados` JSONB.

Ano/número inexistente → **404**.

---

## Passo 3 — Certificado público

Liste os seus (aluno):

```
GET {{baseUrl}}/certificates/mine
Authorization: Bearer {{accessToken}}
```

Copie `hashSha256` → `{{certificateHash}}`.

```
GET {{baseUrl}}/publico/verificar-certificado/{{certificateHash}}
```

URL: `http://localhost:8080/publico/verificar-certificado/{{certificateHash}}`

**Esperado 200 (válido):**

```json
{
  "valido": true,
  "hashSha256": "a1b2…",
  "chCreditadas": 4.0,
  "issuedAt": "2026-08-19T20:00:00Z",
  "idEvento": "7c9e6679-…",
  "verificacaoAssinatura": "ED25519_VALID",
  "_links": { "jwks": "/.well-known/jwks.json" }
}
```

Hash inexistente → **404**. Prefixo legado `UNSIGNED_` → `valido: false`.

Emissão: [T-10.4](../transversal/T-10.4-certificado.md).

---

## Checklist

- [ ] JWKS com RSA (+ OKP se cert ativo)
- [ ] Protocolo 200 sem PII
- [ ] Protocolo 404
- [ ] Certificado 200 `valido: true` depois de emitir um
- [ ] 429 se disparar >10/min
