# T-F1-003 — Perfil (`/me`)

> **Transação:** [`T-F1-003`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-003-PERFIL.md)  
> **Diagrama:** [`US-F1-003`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-003-PERFIL.md)  
> **IDs:** `{{dataExportJobId}}` depois do POST export.  

Todas as rotas usam o UUID do JWT — não passe `id` na URL.

Headers padrão autenticados: `Authorization: Bearer {{accessToken}}` + `X-XSRF-TOKEN` nas mutações.

---

## Passo 1 — GET perfil

```
GET {{baseUrl}}/me
```

**Esperado 200:** `id`, `nome`, `email`, `grr`, `roles`, `_links` HAL (`update-profile`, `change-password`, `notifications`, `data-export`). Copie `id` → `{{alunoId}}`.

---

## Passo 2 — PATCH perfil

Cole no Body:

```json
{
  "nome": "Ana Aluno Demo",
  "metadata": {
    "idCurso": "{{cursoId}}",
    "telefone": "41999990000"
  }
}
```

```
PATCH {{baseUrl}}/me
```

**Esperado 200** com nome/metadata atualizados. `{{cursoId}}` precisa ser UUID real.

---

## Passo 3 — Avatar (presign MinIO)

Cole no Body:

```json
{
  "contentType": "image/jpeg"
}
```

```
POST {{baseUrl}}/me/avatar
```

**Esperado 200:** `{ "uploadUrl": "http://…", "storageKey": "avatars/….jpg" }`

Crie um **segundo** request HTTPie:

- Method `PUT`
- URL = cole `uploadUrl` **inteira** (não use `{{baseUrl}}`)
- Body = arquivo binário `foto.jpg`
- Header `Content-Type: image/jpeg`
- Sem Bearer (a query string da URL já autentica o MinIO)

---

## Passo 4 — Trocar senha

Cole no Body:

```json
{
  "senhaAtual": "AlunoS3nh@Forte!",
  "novaSenha": "AlunoS3nh@Nova2026!"
}
```

```
POST {{baseUrl}}/me/password
```

**Esperado 200** `{ "mensagem": "Senha alterada com sucesso." }`. Senha atual errada → **400**. Atualize `{{alunoSenha}}`.

---

## Passo 5 — Preferências de notificação

Cole no Body:

```json
{
  "emailEnabled": true,
  "pushEnabled": false,
  "inAppEnabled": true
}
```

```
PATCH {{baseUrl}}/me/notifications
```

**Esperado 200** ecoando os três booleans.

---

## Passo 6 — FCM (opcional)

Cole no Body:

```json
{
  "fcmToken": "fake-fcm-token-httpie-desktop-001",
  "plataforma": "WEB"
}
```

```
POST {{baseUrl}}/me/fcm-token
DELETE {{baseUrl}}/me/fcm-token
```

Body do DELETE: 

```json
{
  "fcmToken": "fake-fcm-token-httpie-desktop-001"
}
```

`fcmToken` é obrigatório.

---

## Passo 7 — Exportação LGPD

```
POST {{baseUrl}}/me/data-export
```

**Esperado 200/202** com `jobId`. Copie → `{{dataExportJobId}}`.

```
GET {{baseUrl}}/me/data-export/{{dataExportJobId}}
```

Estados: `READY` + URL MinIO 24 h, ou `EXPIRED` se o objeto sumiu.
