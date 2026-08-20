# T-F1-002 — Primeiro acesso (senha + LGPD)

> **Transação:** [`T-F1-002`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-002-PRIMEIRO-ACESSO.md)  
> **Diagrama:** [`US-F1-002`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-002-PRIMEIRO-ACESSO.md)  

Pré-requisito: usuário criado no [bootstrap](../02-bootstrap-usuarios-demo.md) ainda com senha temporária.

---

## Passo 1 — Login com senha provisória

Cole no Body:

```json
{
  "identificador": "ana.aluno@ufpr.br",
  "senha": "AlunoS3nh@Forte!"
}
```

Troque a senha pela temporária do Mailhog.

**Esperado:** `mustChangePassword: true`. Copie esse `accessToken` (é o único que autoriza o next).

---

## Passo 2 — Completar primeiro acesso

```
POST {{baseUrl}}/auth/first-access
Authorization: Bearer {{accessToken}}
Content-Type: application/json
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "novaSenha": "AlunoS3nh@Forte!",
  "aceiteLgpd": true
}
```

**Esperado 200:**

```json
{
  "mensagem": "Primeiro acesso concluído com sucesso."
}
```

`aceiteLgpd: false` → **400**. Senha curta → **422**.

---

## Passo 3 — Relogar

Login com `AlunoS3nh@Forte!`. Esperado: `mustChangePassword: false`, `mustAcceptLgpd: false`.

Confira metadata:

```
GET {{baseUrl}}/me
Authorization: Bearer {{accessToken}}
```

`metadata.aceite_lgpd_em` preenchido (ISO-8601).
