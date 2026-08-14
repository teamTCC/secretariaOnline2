# T-F1-003 — Perfil do Aluno

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-003-PERFIL.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-003-PERFIL.md)  
> **Status:** ✅ Implementado — GET/PATCH /me, avatar, senha, notificações

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller (exportação LGPD) | [`iam/api/ProfileController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ProfileController.kt) |
| Use Case de exportação | [`iam/application/DataExportUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/DataExportUseCase.kt) |

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller de perfil | [`iam/api/ProfileController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ProfileController.kt) |
| Entidade preferências | [`iam/infrastructure/persistence/NotificationPrefEntity.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/persistence/NotificationPrefEntity.kt) |
| Use Case exportação | [`iam/application/DataExportUseCase.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/DataExportUseCase.kt) |

---

## O que está implementado

| Endpoint | Capability | Status |
|----------|-----------|--------|
| `GET /me` | `user.update_own_profile` | ✅ |
| `PATCH /me` | `user.update_own_profile` | ✅ |
| `POST /me/avatar` | `user.update_own_profile` | ✅ URL presignada MinIO |
| `POST /me/password` | `user.update_own_password` | ✅ Argon2 verify + update |
| `PATCH /me/notifications` | `user.update_own_profile` | ✅ |
| `POST /me/data-export` | `user.export_own_data` | ✅ |
| `GET /me/data-export/{jobId}` | `user.export_own_data` | ✅ |
| `POST /me/fcm-token` | `isAuthenticated()` | ✅ Push mobile |
| `DELETE /me/fcm-token` | `isAuthenticated()` | ✅ Logout dispositivo |

---

## JSON Responses

### GET /me
```json
{
  "id": "uuid",
  "nome": "João Silva",
  "email": "joao@ufpr.br",
  "grr": "GRR20201234",
  "ativo": true,
  "metadata": {},
  "roles": ["ALUNO"],
  "_links": {
    "self": {"href": "/me"},
    "update-profile": {"href": "/me", "type": "PATCH"},
    "change-password": {"href": "/me/password", "type": "POST"},
    "notifications": {"href": "/me/notifications", "type": "PATCH"},
    "data-export": {"href": "/me/data-export", "type": "POST"}
  }
}
```

### POST /me/avatar
```json
// Request
{ "contentType": "image/jpeg" }
// Response 200
{ "uploadUrl": "https://minio.../avatars/uuid.jpg?...", "storageKey": "avatars/uuid.jpg" }
```

### POST /me/password
```json
// Request
{ "senhaAtual": "senha123", "novaSenha": "novaSenha456!" }
// Response 200
{ "mensagem": "Senha alterada com sucesso." }
// Response 400 — senha atual incorreta
```

### PATCH /me/notifications
```json
// Request
{ "emailEnabled": true, "pushEnabled": false, "inAppEnabled": true }
// Response 200
{ "emailEnabled": true, "pushEnabled": false, "inAppEnabled": true }
```

---

## Checklist de Verificação

- [x] `GET /me` → dados pessoais do usuário autenticado com `_links`
- [x] `PATCH /me` → atualiza nome e metadata
- [x] `POST /me/avatar` → URL presignada MinIO para upload de foto
- [x] `POST /me/password` com `senhaAtual` + `novaSenha` → `200`
- [x] `PATCH /me/notifications` → preferências email/push/in-app
- [x] `POST /me/fcm-token` → registrar token para push notifications
- [x] `POST /me/data-export` → JSON no MinIO + URL pré-assinada 24h
- [x] `GET /me/data-export/{jobId}` → `READY` + nova URL se o objeto existe; `EXPIRED` se não existe no MinIO
