# T-F5-009 — Importação CSV

> **Transação:** [`T-F5-009`](../../transaçõesBackend/F5%20—%20Secretaria/T-F5-009-IMPORTACOES.md)  
> **Diagrama:** [`US-F5-009`](../../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-009-IMPORTACOES.md)  
> **IDs:** `{{importJobId}}`  

Capability: `import.run`. Só CSV UTF-8 (não Excel). Cabeçalho: `nome,email[,grr,role]`. Limite 20 MB.

---

## Passo 1 — Baixar modelo

```
GET {{baseUrl}}/imports/templates/alunos
GET {{baseUrl}}/imports/templates/professores
Authorization: Bearer {{accessTokenSecretaria}}
```

**Esperado 200** `text/csv`. Compare com os demos abaixo (UTF-8, cabeçalho obrigatório).

Alunos:

```csv
nome,email,grr,role
Carlos Mendes,carlos.import@ufpr.br,20218888,ALUNO
Beatriz Lima,beatriz.import@ufpr.br,20218889,ALUNO
```

Professores:

```csv
nome,email,grr,role
Paulo Docente,paulo.docente@ufpr.br,,PROFESSOR
```

---

## Passo 2 — Validar (não persiste usuários)

No HTTPie: Body → **Multipart** → campo `file` = o CSV. **Remova** `Content-Type: application/json` deste request.

```
POST {{baseUrl}}/imports/alunos
```

(professores: `POST /imports/professores`)

**Esperado 200/202** com `jobId` e status `VALIDATED` ou `INVALID`. Copie → `{{importJobId}}`.

```
GET {{baseUrl}}/imports/{{importJobId}}
```

Leia o preview de linhas ok/erro **antes** de confirmar.

---

## Passo 3 — Confirmar

```
POST {{baseUrl}}/imports/{{importJobId}}/confirm
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200:** `COMPLETED` ou `PARTIAL`. Cria usuários + Argon2 + outbox `imports.completed`. Senhas temporárias no Mailhog.

403 sem `import.run`. E-mail duplicado aparece no preview, não no confirm cego.
