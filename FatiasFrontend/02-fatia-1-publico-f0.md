# Fatia 1 — F0 público (login, senha, contato, verificações)

**Objetivo da demo:** a equipe entra com cada perfil demo e vê cookies. Prova anti-enumeração, CSRF no contato, RFC 7807 na tela.

**Pré-requisito:** Fatia 0 mergeada.  
**Oráculos:** `httpie/F0-publico/T-F0-00*.md` · `logs/log testes httpie - req_resp.md` (bloco F0).

---

## 1. Whitelist

| Arquivo | Rota UI |
|---------|---------|
| `features/publico/LoginPage.tsx` | `/login` |
| `features/publico/ForgotPage.tsx` | `/recuperar-senha` |
| `features/publico/ResetPage.tsx` | `/nova-senha?token=` |
| `features/publico/ContatoPage.tsx` | `/contato` |
| `features/publico/ErroPage.tsx` | `/erro/:incidentId?` |
| `features/publico/ProtocoloPage.tsx` | `/publico/solicitacoes/:ano/:numero` |
| `features/publico/CertificadoPage.tsx` | `/publico/verificar-certificado/:hash` |
| `features/publico/OttPage.tsx` | `/auth/ott?token=` (exchange) |
| `app/router.tsx` | registrar rotas acima; `/` → `/login` |

Hooks colados no mesmo arquivo da página (não criar `useLogin.ts` ainda).

---

## 2. Login (`POST /auth/login`)

**CSRF isento.** Body:

```json
{ "identificador": "ana.aluno@ufpr.br", "senha": "AlunoS3nh@Forte!" }
```

`identificador` aceita **e-mail ou GRR** (`20210001`).

**200 body (sem JWT):**

```json
{ "mustChangePassword": false, "mustAcceptLgpd": false }
```

Cookies: `access_token` Path=/ HttpOnly; `refresh_token` Path=/auth HttpOnly.

UI mínima:

```
<input name=identificador>
<input name=senha type=password>
<button>entrar</button>
<ProblemBanner />
```

Após 200:

- guardar as duas flags em `sessionStorage` (não o token)
- se `mustChangePassword || mustAcceptLgpd` → `/primeiro-acesso` (página nasce na fatia 2; por ora redirecionar `/me` e deixar o guard da fatia 2)
- senão `GET /me` e redirecionar dashboard (fatia 2). **Nesta fatia** redirecionar para `/me-raw` (página 10 linhas: `JsonPanel` de `/me`) para provar cookie.

**401:** mensagem genérica do Problem (`detail`). Nunca “usuário não existe”. UI mostra o JSON do problem.

**429:** `Retry-After` / campo `retryAfterSeconds` — mostrar “tente em Ns”. Rate: 5/min IP+identificador.

Credenciais de fumaça (bootstrap já rodou):

| Perfil | identificador | senha |
|--------|---------------|-------|
| Admin | `admin@ufpr.br` | `Admin@123456` |
| Aluno | `ana.aluno@ufpr.br` | `AlunoS3nh@Forte!` |
| Prof | `prof.ana@ufpr.br` | `ProfS3nh@Forte!` |
| Sec | `secretaria@ufpr.br` | `SecrS3nh@Forte!` |
| Coord | `coord.tads@ufpr.br` | `CoordS3nh@Forte!` |
| Egresso | `ana.egressa@ufpr.br` | `EgressoS3nh@Forte!` |

Um `<select>` de “preencher demo” (só em `import.meta.env.DEV`) poupa a equipe. Não commitar senhas em comentários longos — a tabela acima basta.

---

## 3. Forgot / reset

`POST /auth/forgot-password` `{ email }` → **sempre 202** (mesmo e-mail inexistente). CSRF isento. Rate 3/h e-mail+IP.

UI: um input + “se existir, enviaremos link”. Não dizer se o e-mail existe.

Token: Mailpit/outbox (`iam.password_reset_requested`). Para a demo, campo extra “colar token JWT” na `ResetPage`.

`POST /auth/reset-password` `{ token, novaSenha }`:

- senha fraca → **422** `weak-password`
- reuso → **422** `password-reuse`
- token replay/expirado → **401**

O back valida política de senha no **domínio** (não só `@Size`). A UI só mostra o Problem.

---

## 4. Contato (CSRF **obrigatório**)

`GET /publico/contato` → dados institucionais + `_links.enviar`.

`POST /publico/contato` `{ nome, email, mensagem }` + `X-XSRF-TOKEN`. Rate 10/min IP.

Sem header CSRF → 403. Com CSRF → 202.

Isto é o teste visual de Double Submit para a equipe.

---

## 5. Erro RFC 7807

Rota `/erro/:incidentId?` — se o `ProblemBanner` tiver `incidentId`, link para cá. Página mostra o id. `GET` público de incidente **não** existe; é só UI.

---

## 6. Protocolo e certificado públicos (sem cookie)

Depois que a fatia 3/4 gerar dados:

- `GET /publico/solicitacoes/{ano}/{numero}` — número **sem** zero à esquerda (`/publico/solicitacoes/2026/42`). Rate 10/min.
- `GET /publico/verificar-certificado/{hashSha256}`
- `GET /.well-known/jwks.json` — RSA + OKP Ed25519. Em dev a chave de cert é **efêmera** (reinício da JVM invalida certs antigos → `INVALID`). Mostrar isso no `JsonPanel` (a equipe precisa ver).

`JsonPanel` da resposta é suficiente. Sem PDF viewer.

---

## 7. OTT (deep-link de e-mail)

`POST /auth/ott` `{ token }` — CSRF isento. Audience `request:{uuid}`. Replay → 401. Sucesso = mesmo contrato do login (cookies + flags).

Página `/auth/ott?token=`: no mount, POST e redireciona. Usado quando o outbox manda link de solicitação.

---

## 8. Aceite (equipe no browser)

- [ ] Login aluno: Application → cookies `access_token` + `refresh_token`; body sem JWT
- [ ] `GET /me` na aba Network com cookie, 200
- [ ] Login senha errada: 401 genérico na `ProblemBanner`
- [ ] Forgot: 202 para e-mail fake e real
- [ ] Contato sem CSRF: 403; com CSRF: 202
- [ ] JWKS abre sem login

## 9. Não fazer

- Tela ilustrada, logo UFPR, Tailwind.
- Guard de rotas completo (fatia 2).
- Logout (precisa CSRF + sessão — fatia 2).
