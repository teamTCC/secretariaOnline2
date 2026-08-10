# T-F1-010 / T-F1-011 — Certificados e Atendimentos

> **Diagramas:**  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-010-CERTIFICADOS.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-010-CERTIFICADOS.md)  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-011-ATENDIMENTOS.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-011-ATENDIMENTOS.md)  
> **Status:** ⏳ Stub

---

## Certificados (F1.10)

Listar e baixar certificados gerados automaticamente pelo sistema após eventos concluídos ou formativas aprovadas.

### Depende de
- [T-10.4-CERTIFICADO](../transversal/T-10.4-CERTIFICADO.md) — emissão automática
- MinIO (`MinioStorageService`) — download via presigned URL

### Endpoints necessários

```
GET /certificates/me                    → listar meus certificados
GET /certificates/{id}/download-url     → presigned URL MinIO (TTL 15min)
```

---

## Atendimentos (F1.11)

Agendamento e consulta de atendimentos presenciais com a secretaria.

### Endpoints necessários

```
GET  /atendimentos/me        → listar meus agendamentos
POST /atendimentos           → solicitar atendimento
```

---

## Checklist de Verificação

- [ ] `GET /certificates/me` → **não implementado**
- [ ] Download via presigned URL MinIO → **não implementado**
- [ ] `POST /atendimentos` → **não implementado**
