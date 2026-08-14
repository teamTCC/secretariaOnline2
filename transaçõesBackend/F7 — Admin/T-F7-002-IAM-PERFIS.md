# T-F7-002 — IAM: Perfis e Authorities

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-002-IAM-PERFIS-AUTORIDADES.md`](../../foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-002-IAM-PERFIS-AUTORIDADES.md)  
> **Status:** ✅ `AdminRolesController`  
> **Capability:** `iam.manage_roles` (ADMIN também tem `system.admin`)

Usuários (CRUD) continuam em `GET/POST /usuarios` — ver [T-F5-SECRETARIA](../F5 — Secretaria/T-F5-SECRETARIA.md) F5.3 e [T-F7-ADMIN](T-F7-ADMIN.md).

---

## Arquivo

[`iam/api/AdminRolesController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AdminRolesController.kt)

Aliases: `/admin/roles` ≡ `/admin/perfis`.

```
GET    /admin/roles
POST   /admin/roles                    { "code": "MONITOR", "descricao": "..." }
PATCH  /admin/roles/{id}               { "descricao": "..." }
DELETE /admin/roles/{id}               → 204 (bloqueado para ALUNO/ADMIN/SECRETARIO/PROFESSOR)
GET    /admin/autoridades
PATCH  /admin/roles/{id}/authorities   { "authorityCodes": ["request.deliberate", "..."] }
PUT    /admin/usuarios/{id}/roles      { "roleCodes": ["ALUNO", "EGRESSO"] }
```

`PUT .../roles` substitui o conjunto (`orphanRemoval` em `usuario_role`).

---

## Checklist

- [x] Listar roles com authorities
- [x] Criar / editar / excluir perfil (exceto protegidos)
- [x] Substituir matriz de authorities
- [x] Atribuir papéis a um usuário
