# T-F0-003 — Definir nova senha (token 1 uso)

> **Transação:** [`T-F0-003`](../../transaçõesBackend/F0%20—%20Público/T-F0-003-NOVA-SENHA.md)  
> **Diagrama:** [`US-F0-003`](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-003-NOVA-SENHA.md)  
> **IDs:** `{{resetToken}}` (JWT audience `password-reset`, TTL 24 h).  

Pré-requisito: [T-F0-002](T-F0-002-recuperar-senha.md). Isento de CSRF.

Política de senha: ≥12 chars, maiúscula, minúscula, dígito, especial. Não reutilizar as 3 últimas.

---

## Passo 1 — Happy path

```
POST {{baseUrl}}/auth/reset-password
Content-Type: application/json
```

Cole no Body:

```json
{
  "token": "{{resetToken}}",
  "novaSenha": "NovaS3nh@Forte2026!"
}
```

**Esperado 200:**

```json
{
  "mensagem": "Senha redefinida com sucesso. Faça login novamente."
}
```

Efeitos colaterais (confira se quiser):

- `jti` do token entra na blacklist
- todos os `refresh_token` do usuário revogados
- `audit_log.acao = PASSWORD_CHANGED`

Login a seguir: use `NovaS3nh@Forte2026!` (ou o valor que você colocou no JSON). Atualize `{{alunoSenha}}` no environment.

---

## Passo 2 — Token já usado

Repita o Passo 1 com o **mesmo** `{{resetToken}}`.

**Esperado 401:**

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Token inválido",
  "status": 401,
  "detail": "Token de redefinição de senha inválido ou expirado."
}
```

A API **não** distingue expirado / inválido / já usado.

---

## Passo 3 — Senha fraca (token ainda válido)

Peça um **novo** forgot-password, copie o token novo e cole no Body: 

```json
{
  "token": "{{resetToken}}",
  "novaSenha": "fraca"
}
```

**Esperado 422** (`weak-password`). O JTI **não** é blacklistado — você pode tentar de novo com senha forte no mesmo token.

---

## Passo 4 — Reuso das últimas 3 senhas

Com um token fresco, envie a senha **atual** do usuário.

**Esperado 422:**

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/password-reuse",
  "title": "Senha já utilizada",
  "status": 422,
  "detail": "Esta senha já foi utilizada recentemente."
}
```

Token continua válido.

---

## Checklist

- [ ] 200 no primeiro uso do token
- [ ] 401 no segundo uso
- [ ] 422 senha fraca (token reutilizável)
- [ ] 422 reuso (token reutilizável)
- [ ] Login com a senha nova
