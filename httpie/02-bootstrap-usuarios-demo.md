# 02 — Bootstrap de usuários demo

O seed só cria `admin@ufpr.br`. Para testar F1–F8 você precisa de um usuário por perfil. Este tutorial usa **HTTPie + admin**.

Pré-requisito: [00-setup](00-setup-httpie-desktop.md) (health, CSRF, login admin).

Transações relacionadas: [T-F0-001](F0-publico/T-F0-001-login.md) · [T-F5 usuários](F5-secretaria/T-F5-secretaria.md) · [T-F7-002 roles](F7-admin/T-F7-002-iam-perfis.md).

---

## Passo A — Login admin

Cole no Body:

```json
{
  "identificador": "admin@ufpr.br",
  "senha": "Admin@123456"
}
```

```
POST {{baseUrl}}/auth/login
```

Copie `accessToken` → `{{accessToken}}`.

Se `401`: o hash do seed não confere. Opções:

1. Subir o app com um job que regrava a senha Argon2id de `Admin@123456`, ou
2. Atualizar `usuario.senha_hash` no Postgres com um hash gerado pela própria API (criar um usuário novo via SQL não ajuda — precisa Argon2id do `Argon2PasswordService`).

Confirme o admin:

```
GET {{baseUrl}}/me
Authorization: Bearer {{accessToken}}
```

Esperado: `"email": "admin@ufpr.br"`, `roles` contendo `ADMIN`.

---

## Passo B — Descobrir `cursoId`

```
GET {{baseUrl}}/academico/cursos
Authorization: Bearer {{accessToken}}
```

Procure `"sigla": "TADS"`. Copie `id` → `{{cursoId}}`.

Seed: *Tecnologia em Análise e Desenvolvimento de Sistemas* / `TADS` e *Engenharia de Software* / `ES`.

---

## Passo C — Criar usuários

Cada create: `POST {{baseUrl}}/usuarios` + header Bearer + `X-XSRF-TOKEN`.

Aluno (`roleCode: ALUNO`):

```json
{
  "nome": "Ana Aluno Demo",
  "email": "ana.aluno@ufpr.br",
  "grr": "20210001",
  "roleCode": "ALUNO"
}
```

Professor (`roleCode: PROFESSOR`):

```json
{
  "nome": "Ana Professora Demo",
  "email": "prof.ana@ufpr.br",
  "grr": null,
  "roleCode": "PROFESSOR"
}
```

Secretaria (`roleCode: SECRETARIO`):

```json
{
  "nome": "Secretaria Demo",
  "email": "secretaria@ufpr.br",
  "grr": null,
  "roleCode": "SECRETARIO"
}
```

Coordenador (`roleCode: COORDENADOR`):

```json
{
  "nome": "Coordenador TADS Demo",
  "email": "coord.tads@ufpr.br",
  "grr": null,
  "roleCode": "COORDENADOR"
}
```

**Esperado 201** (campos podem variar):

```json
{
  "id": "0193a0c0-…",
  "email": "ana.aluno@ufpr.br",
  "mensagem": "…"
}
```

Copie cada `id` para `{{alunoId}}`, `{{professorId}}`, etc.

A senha temporária **não volta no JSON** (`201 { id, email }`). Ela vai no payload do outbox `iam.usuario_criado` (`senhaTemporaria`) e, depois do dispatcher, para o Mailhog.

```sql
SELECT payload->>'email' AS email, payload->>'senhaTemporaria' AS senha, status
FROM outbox_event
WHERE event_type = 'iam.usuario_criado'
ORDER BY created_at DESC
LIMIT 10;
```

### Pegar a senha / link no Mailhog

1. Abra [http://localhost:8025](http://localhost:8025).
2. Abra o e-mail `iam.usuario_criado` (ou similar).
3. **Ou** SQL:

```sql
SELECT event_type, status, payload
FROM outbox_event
WHERE event_type LIKE 'iam%'
ORDER BY created_at DESC
LIMIT 10;
```

Se o dispatcher ainda não rodou (`status=PENDING`), espere ~5 s (`OutboxDispatcher`).

---

## Passo D — Primeiro acesso de cada usuário novo

1. `POST /auth/login` com e-mail + senha temporária.
2. Resposta: `mustChangePassword: true` (e talvez `mustAcceptLgpd: true`).
3. Com **esse** accessToken: `POST /auth/first-access` — body 

```json
{
  "novaSenha": "AlunoS3nh@Forte!",
  "aceiteLgpd": true
}
```

4. Login de novo com a senha definitiva (`AlunoS3nh@Forte!`, etc.).

Tutorial completo: [T-F1-002](F1-aluno/T-F1-002-primeiro-acesso.md).

---

## Passo E — Vincular aluno ao curso (metadata)

Vários endpoints (eventos `audience=me`, colação) leem `usuario.metadata.idCurso`.

```
PATCH {{baseUrl}}/me
Authorization: Bearer {{accessTokenDoAluno}}
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "metadata": {
    "idCurso": "{{cursoId}}"
  }
}
```

Admin também pode PATCH em fluxos de gestão — o contrato canônico do aluno é `/me`.

---

## Passo F — Coordenador dono do curso (F6)

`GET /courses/tads/config` só passa se `curso.id_coordenador == currentUserId()` (admin bypass).

No Postgres, depois de criar o coordenador:

```sql
UPDATE curso
SET id_coordenador = '{{coordenadorId}}'::uuid
WHERE sigla = 'TADS';
```

(Cole o UUID real, sem aspas de placeholder.)

---

## Passo G — Conferir papéis

```
GET {{baseUrl}}/usuarios?email=ana.aluno@ufpr.br
Authorization: Bearer {{accessTokenAdmin}}
```

Ou:

```
GET {{baseUrl}}/admin/roles
Authorization: Bearer {{accessTokenAdmin}}
```

Atribuir papéis extra (o JWT só atualiza no **próximo login**):

CAAF:

```json
{
  "roleCodes": ["PROFESSOR", "CAAF"]
}
```

COE:

```json
{
  "roleCodes": ["PROFESSOR", "COE"]
}
```

Detalhe: [T-F7-002](F7-admin/T-F7-002-iam-perfis.md).

---

## Checklist

- [ ] Login admin 200
- [ ] `cursoId` TADS no environment
- [ ] 4 usuários criados + IDs copiados
- [ ] Senha definitiva via first-access
- [ ] Aluno com `metadata.idCurso`
- [ ] Login aluno → `GET /bff/dashboard/aluno` 200
- [ ] Login professor → `GET /bff/dashboard/professor` 200
- [ ] Login secretaria → `GET /bff/dashboard/secretaria` 200
