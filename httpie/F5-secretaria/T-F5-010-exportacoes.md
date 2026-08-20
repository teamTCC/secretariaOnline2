# T-F5-010 — Exportações CSV

> **Transação:** [`T-F5-010`](../../transaçõesBackend/F5%20—%20Secretaria/T-F5-010-EXPORTACOES.md)  
> **Diagrama:** [`US-F5-010`](../../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-010-EXPORTACOES.md)  
> **IDs:** `{{exportJobId}}`

Capability: `export.run`. Kinds: `alunos` · `egressos` · `solicitacoes` (máx. 5000 linhas). Worker a cada 5 s.

---

## Passo 1 — Enfileirar

```
POST {{baseUrl}}/exports/alunos
Authorization: Bearer {{accessTokenSecretaria}}
X-XSRF-TOKEN: {{xsrfToken}}
```

Sem body. Repita com `/exports/egressos` e `/exports/solicitacoes` se quiser.

**Esperado 202:**

```json
{
  "jobId": "…",
  "status": "PROCESSANDO",
  "_links": { "self": "/exports/…" }
}
```

Copie `jobId` → `{{exportJobId}}`.

---

## Passo 2 — Polling

```
GET {{baseUrl}}/exports
GET {{baseUrl}}/exports/{{exportJobId}}
```

Estados: `PROCESSANDO` → `PRONTO` | `ERRO` | depois de 7 dias `EXPIRADO`.

Quando `PRONTO`:

```
GET {{baseUrl}}/exports/{{exportJobId}}/download
```

**Esperado:** `{ "downloadUrl": "http://minio…?X-Amz-…" }`. Job de outro ator → **403**.

Outbox `exports.ready`.
