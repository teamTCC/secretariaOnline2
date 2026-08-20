# T-F1-007 / 008 — Estágio e TCC

> **Transação:** [`T-F1-007-008`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-007-008-ESTAGIO-TCC.md)  
> **Diagramas:** [`US-F1-007`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-007-ESTAGIO.md) · [`US-F1-008`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-008-TCC.md)  
> **IDs:** `{{internshipId}}`, `{{tccId}}`, `{{alunoId}}`, `{{professorId}}`, `{{cursoId}}`

---

## Estágio (aluno)

Cole no Body:

```json
{
  "empresa": "Empresa XYZ Ltda.",
  "cargo": "Dev Backend",
  "cargaHorariaSemanal": 20,
  "inicio": "2026-03-01",
  "observacoes": "Estágio obrigatório TADS — teste HTTPie"
}
```

```
POST {{baseUrl}}/internships
Authorization: Bearer {{accessTokenAluno}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 201:** `{ "id": "…", "estado": "EM_ANDAMENTO" }` → `{{internshipId}}`.

```
GET {{baseUrl}}/internships/mine
GET {{baseUrl}}/internships/{{internshipId}}
```

Presign documento: 

```json
{
  "tipo": "CONTRATO",
  "nomeOriginal": "contrato.pdf",
  "contentType": "application/pdf"
}
```

```
POST {{baseUrl}}/internships/{{internshipId}}/documents/upload-url
POST {{baseUrl}}/internships/{{internshipId}}/documents
GET  {{baseUrl}}/internships/{{internshipId}}/documents
```

COE conclui (`internship.review`):

```
POST {{baseUrl}}/internships/{{internshipId}}/conclude
Authorization: Bearer {{accessTokenCoe}}
```

**Esperado 200:** `{ "estado": "CONCLUIDO" }`. Outbox `estagio.concluido`.

Pool/supervisor: [T-F4-002](../F4-comissoes/T-F4-002-coe.md).

---

## TCC (orientador cria, aluno envia PDF)

Token professor `tcc.supervise`:

Cole no Body:

```json
{
  "titulo": "Modernização de Sistema Acadêmico — SecretariaOnline2",
  "idCurso": "{{cursoId}}"
}
```

```
POST {{baseUrl}}/tccs
```

**Esperado 201** → `{{tccId}}`.

Vincular aluno: 

```json
{
  "idAluno": "{{alunoId}}",
  "papel": "AUTOR"
}
```

```
POST {{baseUrl}}/tccs/{{tccId}}/members
```

Banca: 

```json
{
  "idProfessor": "{{professorId}}",
  "papel": "BANCA"
}
```

```
POST {{baseUrl}}/tccs/{{tccId}}/examiners
```

Nota banca (`tcc.examine`): 

```json
{
  "nota": 9.5
}
```

```
PATCH {{baseUrl}}/tccs/{{tccId}}/grade
```

Aluno (`tcc.upload_final`, precisa ser membro):

```
POST {{baseUrl}}/tccs/{{tccId}}/submit-final/url
```

Cole no Body:

```json
{
  "nomeOriginal": "tcc_final.pdf"
}
```

 → PUT MinIO → `POST …/submit-final/confirm`.

Aprovar (orientador): 

```json
{
  "aprovado": true,
  "notaFinal": 9.2
}
```

```
PATCH {{baseUrl}}/tccs/{{tccId}}/approve
```

**Esperado 200:** `{ "estado": "APROVADO", "notaFinal": 9.2 }`. Necessário para colação ([T-F5-005](../F5-secretaria/T-F5-005-egressos-diplomas.md) critério TCC).

```
GET {{baseUrl}}/tccs/mine
```

com token do aluno.
