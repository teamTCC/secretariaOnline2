# T-F1-003 — Perfil do Aluno

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-003-PERFIL.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-003-PERFIL.md)  
> **Status:** ⏳ Parcialmente implementado

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller (exportação LGPD) | [`iam/api/ProfileController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ProfileController.kt) |
| Use Case de exportação | [`iam/application/DataExportUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/DataExportUseCase.kt) |

---

## O que está implementado

- `GET /me/data-export` — exportação LGPD dos dados pessoais (stub P2 — retorna esqueleto)
- `POST /me/change-password` — troca de senha (ChangePasswordRequest existe nos DTOs)

---

## O que precisa ser implementado

| Endpoint | Diagrama | Status |
|----------|----------|--------|
| `GET /me/profile` | F1.3-D01 — dados do perfil | ⏳ Não existe controller |
| `PATCH /me/profile` | F1.4-D03 — editar dados | ⏳ Não implementado |
| `POST /me/avatar` | F1.5-D06 — upload foto (MinIO presigned PUT) | ⏳ Não implementado |
| `GET /me/data-export` | F1.3-D07a — exportação LGPD | ⏳ Stub |
| `DELETE /me/account` | F1.3-D07c — exclusão de conta | ⏳ Não implementado |

---

## Checklist de Verificação

- [ ] `GET /me/profile` → dados pessoais do aluno autenticado
- [ ] `PATCH /me/profile` → atualiza nome, foto, dados de contato
- [ ] `POST /me/change-password` com `senhaAtual` + `novaSenha` → `200`
- [x] `GET /me/data-export` → exportação LGPD (stub)
