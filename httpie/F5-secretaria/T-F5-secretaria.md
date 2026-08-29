# T-F5 — Secretaria (dashboard, fila, alunos, atendimentos)

> **Transação índice:** [`T-F5-SECRETARIA.md`](../../transaçõesBackend/F5%20—%20Secretaria/T-F5-SECRETARIA.md)  
> **Diagramas:** [`F5 — Secretaria/`](../../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/)  
> **Login:** `POST /auth/login` com o body abaixo. 

```json
{
  "identificador": "secretaria@ufpr.br",
  "senha": "SecrS3nh@Forte!"
}
```

Tutoriais irmãos: [005 diplomas](T-F5-005-egressos-diplomas.md) · [009 import](T-F5-009-importacoes.md) · [010 export](T-F5-010-exportacoes.md) · [011 stats](T-F5-011-estatisticas.md) · [012 kanban](T-F5-012-tarefas.md).

---

## F5.1 — Dashboard

Cookie da session **ou** Bearer. Código: `DashboardSecretariaController` + `DashboardSecretariaQuery`. Cache key `secretaria:static`.

```
GET {{baseUrl}}/bff/dashboard/secretaria
Authorization: Bearer {{accessTokenSecretaria}}
```

**Esperado 200:**

```json
{
  "kpis": { "emTriagem": 0, "emDeliberacao": 0 },
  "_links": {
    "self": "/bff/dashboard/secretaria",
    "solicitacoes": "/requests",
    "usuarios": "/usuarios"
  }
}
```

Authority `dashboard.view_secretary`.

---

## F5.2 — Fila e bulk

```
GET {{baseUrl}}/requests?estado=ABERTA&page=0&size=20
GET {{baseUrl}}/requests?type=AUTORIZACAO_IMAGEM
```

(`type` = alias de `typeCode`.) Copie um `id` → `{{requestId}}`.

Cole no Body:

```json
{
  "ids": ["{{requestId}}"],
  "action": "DEFER",
  "parecer": "Autorização deferida em lote (HTTPie)."
}
```

```
PATCH {{baseUrl}}/requests/bulk-deliberate
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200** ou **409** (rollback all-or-nothing). Action seed: `DEFER`.

Transição unitária: [T-F1-005](../F1-aluno/T-F1-005-solicitacoes.md) Passo 7.

---

## F5.2b — Abrir em nome do aluno (`onBehalfOf`)

Exige `request.open_on_behalf`. Sem essa authority → **400** (`IllegalArgumentException`).

Copie o `id` do aluno (`GET /usuarios?email=ana.aluno@ufpr.br`) → `{{alunoId}}`. Use um tipo publicado (`GET /requests/types`).

Cole no Body:

```json
{
  "idRequestType": "{{requestTypeId}}",
  "idCurso": "{{cursoId}}",
  "idSolicitanteOnBehalf": "{{alunoId}}",
  "dados": {
    "finalidade": "BOLSA",
    "observacoes": "Aberta pelo balcão da secretaria (HTTPie)."
  }
}
```

```
POST {{baseUrl}}/requests
Authorization: Bearer {{accessTokenSecretaria}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 201.** `GET /requests/{id}` deve ter `idSolicitante` = `{{alunoId}}` (não o da secretaria).

---

## F5.3 — Usuários

```
GET {{baseUrl}}/usuarios?page=0&size=20
GET {{baseUrl}}/usuarios?email=ana.aluno@ufpr.br
GET {{baseUrl}}/usuarios/{{alunoId}}
```

Criar usuários: [02 bootstrap](../02-bootstrap-usuarios-demo.md).

Cole no Body:

```json
{
  "ativo": false
}
```

```
PATCH {{baseUrl}}/usuarios/{{alunoId}}/status
```

Reset senha (link 1-uso, admin nunca vê a senha):

```
POST {{baseUrl}}/usuarios/{{alunoId}}/reset-password
```

Pegue o token no Mailhog / outbox → [T-F0-003](../F0-publico/T-F0-003-nova-senha.md).

---

## F5.4 — Acadêmico (leitura)

```
GET {{baseUrl}}/academico/cursos
GET {{baseUrl}}/academico/cursos/{{cursoId}}/disciplinas
GET {{baseUrl}}/academico/periodos/ativo
```

CRUD coordenação: [T-F6](../F6-coordenacao/T-F6-coordenacao.md).

---

## F5.13 — Registrar atendimento

Cole no Body:

```json
{
  "idAluno": "{{alunoId}}",
  "assunto": "Revisão de matrícula",
  "tipo": "PRESENCIAL",
  "descricao": "Atendimento de balcão registrado via HTTPie."
}
```

```
POST {{baseUrl}}/service-records
```

**Esperado 201** `PENDENTE_CIENCIA`. Ciência do aluno: [T-F1-011](../F1-aluno/T-F1-010-011-certificados-atendimentos.md).
