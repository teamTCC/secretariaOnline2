# T-F5-009 — Importação CSV

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-009-IMPORTACOES.md`](../../foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-009-IMPORTACOES.md)  
> **Status:** ✅ Duas fases (validar → confirmar) — kinds `alunos` e `professores`  
> **Capability:** `import.run`

Excel **não** é suportado (CSV UTF-8). Cabeçalho: `nome,email[,grr,role]`.

---

## API

```
GET  /imports/templates/alunos
GET  /imports/templates/professores
POST /imports/alunos          (multipart file)
POST /imports/professores
GET  /imports/{jobId}
POST /imports/{jobId}/confirm
```

Kind `professores` usa role padrão `PROFESSOR` (parser [`CsvUsuarioParser`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/CsvUsuarioParser.kt)). GRR opcional.

Limite 20 MB. Job `VALIDATED` / `INVALID` / `COMPLETED` / `PARTIAL`. Outbox `imports.completed`.

---

## Checklist

- [x] Template CSV alunos e professores
- [x] Validação sem persistir
- [x] Confirm cria usuários + Argon2
- [ ] Excel — fora de escopo (CSV only)
