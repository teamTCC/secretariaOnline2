# T-F7-002 — Perfis e authorities

> **Transação:** [`T-F7-002`](../../transaçõesBackend/F7%20—%20Admin/T-F7-002-IAM-PERFIS.md)  
> **Diagrama:** [`US-F7-002`](../../foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-002-IAM-PERFIS-AUTORIDADES.md)  
> **IDs:** `{{roleId}}`, `{{alunoId}}` / `{{professorId}}`  
> **Authority:** `iam.manage_roles`

Alias: `/admin/roles` ≡ `/admin/perfis`.

---

## Passo 1 — Listar

```
GET {{baseUrl}}/admin/roles
GET {{baseUrl}}/admin/autoridades
Authorization: Bearer {{accessTokenAdmin}}
```

**Esperado 200:** roles seed (`ALUNO`, `PROFESSOR`, `SECRETARIO`, `ADMIN`, …) com authorities. Copie um id customizado depois do create.

---

## Passo 2 — Criar perfil

Cole no Body:

```json
{
  "code": "MONITOR",
  "descricao": "Monitor de disciplina — perfil de teste HTTPie"
}
```

```
POST {{baseUrl}}/admin/roles
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 201.** Copie `id` → `{{roleId}}`.

```
PATCH {{baseUrl}}/admin/roles/{{roleId}}
```

---

## Passo 3 — Matriz de authorities

Cole no Body:

```json
{
  "authorityCodes": [
    "dashboard.view_own",
    "communication.read"
  ]
}
```

```
PATCH {{baseUrl}}/admin/roles/{{roleId}}/authorities
```

Substitui o conjunto daquele perfil.

---

## Passo 4 — Papéis de um usuário

Cole no Body:

```json
{
  "roleCodes": ["PROFESSOR", "CAAF"]
}
```

Exemplo CAAF. Ajuste `roleCodes` para o teste (`["ALUNO"]`, `["PROFESSOR","COE"]`, …).

```
PUT {{baseUrl}}/admin/usuarios/{{professorId}}/roles
```

**Substitui** o conjunto (`orphanRemoval`). O JWT antigo ainda tem as claims até expirar (15 min) — faça **login de novo** para o HTTPie ver as authorities novas.

---

## Passo 5 — Delete protegido

```
DELETE {{baseUrl}}/admin/roles/{{roleId}}
```

Custom `MONITOR` → **204**. Roles `ALUNO` / `ADMIN` / `SECRETARIO` / `PROFESSOR` → **422** (`role_in_use` / protegido).
