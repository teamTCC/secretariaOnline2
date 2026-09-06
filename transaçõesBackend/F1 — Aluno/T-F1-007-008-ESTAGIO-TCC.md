# T-F1-007 / T-F1-008 — Estágio e TCC

> **Diagramas de referência:**  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-007-ESTAGIO.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-007-ESTAGIO.md)  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-008-TCC.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-008-TCC.md)  
> **Status:** ✅ Implementado — controllers, use cases, MinIO e Outbox

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller Estágio | [`estagio/api/EstagioController.kt`](../../backend/modules/estagio/src/main/kotlin/br/ufpr/sept/so2/modules/estagio/api/EstagioController.kt) |
| Controller Docs Estágio | [`estagio/api/EstagioDocumentController.kt`](../../backend/modules/estagio/src/main/kotlin/br/ufpr/sept/so2/modules/estagio/api/EstagioDocumentController.kt) |
| Controller TCC | [`tcc/api/TccController.kt`](../../backend/modules/tcc/src/main/kotlin/br/ufpr/sept/so2/modules/tcc/api/TccController.kt) |
| Entidades Estágio | [`estagio/infrastructure/persistence/EstagioEntities.kt`](../../backend/modules/estagio/src/main/kotlin/br/ufpr/sept/so2/modules/estagio/infrastructure/persistence/EstagioEntities.kt) |
| Entidades TCC | [`tcc/infrastructure/persistence/TccEntities.kt`](../../backend/modules/tcc/src/main/kotlin/br/ufpr/sept/so2/modules/tcc/infrastructure/persistence/TccEntities.kt) |
| Migration | [`V005__formativas_estagio_tcc_schema.sql`](../../backend/app/src/main/resources/db/migration/V005__formativas_estagio_tcc_schema.sql) |

---

## Banco de dados

A migration `V005__formativas_estagio_tcc_schema.sql` cria:
- `internship` + `internship_document` (estágio + documentos MinIO)
- `tcc` + `tcc_member` + `tcc_examiner` (TCC + membros + banca)

---

## API Estágio

### POST /internships — Declarar início de estágio (ALUNO)
```json
// Request
{
  "empresa": "Empresa XYZ",
  "cargo": "Dev Backend",
  "cargaHorariaSemanal": 20,
  "inicio": "2025-03-01",
  "observacoes": null
}
// Response 201
{ "id": "uuid", "estado": "EM_ANDAMENTO" }
```

### GET /internships/mine — Meus estágios paginados
```json
// Response 200
{
  "content": [
    { "id": "uuid", "empresa": "Empresa XYZ", "estado": "EM_ANDAMENTO", "inicio": "2025-03-01" }
  ],
  "page": 0, "size": 20, "totalElements": 1
}
```

### POST /internships/{id}/documents/upload-url — URL presignada MinIO
```json
// Request
{ "tipo": "CONTRATO", "nomeOriginal": "contrato.pdf", "contentType": "application/pdf" }
// Response 200
{ "uploadUrl": "https://minio.../internships/uuid/...", "storageKey": "internships/uuid/uuid_contrato.pdf" }
```

### POST /internships/{id}/conclude — Concluir estágio (COE)
```json
// Response 200
{ "estado": "CONCLUIDO" }
```

---

## API TCC

### POST /tccs — Criar TCC (orientador)
```json
// Request
{ "titulo": "Modernização de Sistema Acadêmico", "idCurso": "uuid" }
// Response 201
{ "id": "uuid", "titulo": "...", "estado": "EM_ANDAMENTO" }
```

### POST /tccs/{id}/members — Vincular aluno
```json
{ "idAluno": "uuid", "papel": "AUTOR" }
```

### POST /tccs/{id}/examiners — Adicionar banca
```json
{ "idProfessor": "uuid", "papel": "BANCA" }
```

### PATCH /tccs/{id}/grade — Nota da banca
```json
{ "nota": 9.5 }
```

### POST /tccs/{id}/submit-final/url — URL para enviar PDF final (ALUNO)
```json
// Request
{ "nomeOriginal": "tcc_final.pdf" }
// Response 200
{ "uploadUrl": "https://minio.../tccs/uuid/final_uuid.pdf", "storageKey": "tccs/uuid/final_uuid.pdf" }
```

### PATCH /tccs/{id}/approve — Aprovar/reprovar (orientador)
```json
// Request
{ "aprovado": true, "notaFinal": 9.2 }
// Response 200
{ "estado": "APROVADO", "notaFinal": 9.2 }
```

---

## Outbox Events

| Evento | Tipo | Quando |
|--------|------|--------|
| `estagio.declarado` | Estágio | Aluno declara novo estágio |
| `estagio.concluido` | Estágio | COE marca como concluído |
| `estagio.supervisor_atribuido` | Estágio | COE atribui supervisor |
| `tcc.criado` | TCC | Orientador cria TCC |
| `tcc.deliberado` | TCC | Orientador aprova/reprova após banca |

---

## FGAC — Authorities

| Authority | Quem | Endpoint |
|-----------|------|---------|
| `internship.view_own` | ALUNO | GET /internships/mine |
| `internship.upload_doc_own` | ALUNO | POST /internships/{id}/documents |
| `internship.review` | COE | GET /internships, PATCH conclude |
| `internship.supervise` | PROFESSOR | PATCH /internships/{id} |
| `tcc.view_own` | ALUNO | GET /tccs/mine |
| `tcc.upload_final` | ALUNO membro do TCC | POST /tccs/{id}/submit-final/* (checagem de membership) |
| `tcc.supervise` | PROFESSOR | POST /tccs, POST /members |
| `tcc.examine` | PROFESSOR | PATCH /tccs/{id}/grade |

---

## Checklist de Verificação

- [x] `GET /internships/mine` → estágios do aluno autenticado
- [x] `POST /internships` → declarar início de estágio
- [x] `POST /internships/{id}/documents/upload-url` → URL presignada MinIO
- [x] `POST /internships/{id}/conclude` → COE conclui estágio
- [x] `GET /tccs/mine` → TCCs do aluno
- [x] `POST /tccs` → orientador cria TCC
- [x] `POST /tccs/{id}/members` → vincular aluno
- [x] `POST /tccs/{id}/examiners` → adicionar banca
- [x] `PATCH /tccs/{id}/grade` → banca registra nota
- [x] `POST /tccs/{id}/submit-final/url` → URL para PDF final
- [x] `POST /tccs/{id}/submit-final/confirm` → só membro do TCC
- [x] `GET /internships/{id}/documents` → dono, supervisor ou `internship.review`
- [x] `PATCH /tccs/{id}/approve` → orientador aprova/reprova
