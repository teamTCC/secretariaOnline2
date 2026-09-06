# T-F5-010 — Exportações CSV (Secretaria)

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-010-EXPORTACOES.md`](../../foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-010-EXPORTACOES.md)  
> **Status:** ✅ Job assíncrono `PROCESSANDO` → worker 5s → `PRONTO` + outbox `exports.ready`  
> **Capability:** `export.run`

---

## API

```
POST /exports/{kind}          → 202 { jobId, status: "PROCESSANDO", _links.self }
GET  /exports
GET  /exports/{jobId}
GET  /exports/{jobId}/download → { downloadUrl } só se PRONTO
```

Kinds: `alunos` · `egressos` · `solicitacoes` (até 5000 linhas).

Worker [`ExportController.processPending`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/ExportController.kt) (`@Scheduled` 5s) gera o CSV, sobe no MinIO e dispara outbox. Falha → status `ERRO` + `errorMessage`. TTL 7 dias → `EXPIRADO`.

---

## Checklist

- [x] 202 com `PROCESSANDO`
- [x] Worker em background
- [x] Download MinIO; 403 se outro ator
- [x] Expiração 7 dias
- [x] Outbox `exports.ready`
