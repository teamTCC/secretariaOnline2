# Log testes HTTPie — req_resp

Contrato: `transaçõesBackend/` + diagramas `foundationDocs/sequenceDiagrams/` + tutoriais `httpie/`.  
Base: `http://localhost:8080`. Sessão HTTPie: `so2-admin` (cookie jar).  
Tokens JWT **não** são reproduzidos na íntegra neste log (só presença de cookie / prefixo).

---

## T-F0-001 — Login, refresh, logout, CSRF, OTT inválido

**Transação:** `transaçõesBackend/F0 — Público/T-F0-001-LOGIN.md`  
**Diagrama:** `foundationDocs/sequenceDiagrams/F0 — Público/US-F0-001-LOGIN.md`  
**Tutorial:** `httpie/F0-publico/T-F0-001-login.md`  
**Status:** executado e corrigido (sessão anterior). Checklist principal passou.

### 1) O que foi enviado (requests)

#### 1.1 `GET /actuator/health`

```
GET /actuator/health HTTP/1.1
Host: localhost:8080
Accept: application/json, */*;q=0.5
```

**Por quê:** sanity check do walking skeleton. Sem isso os demais 4xx/5xx não distinguem “API fora” de “contrato errado”. Sem body, sem cookie, `permitAll`.

#### 1.2 `GET /auth/csrf`

```
GET /auth/csrf HTTP/1.1
Host: localhost:8080
Accept: application/json, */*;q=0.5
Cookie: (session httpie so2-admin)
```

Sem body.

**Por quê:** Double Submit Cookie. O SPA (e o HTTPie) precisa do cookie `XSRF-TOKEN` (não HttpOnly) e do valor para ecoar em `X-XSRF-TOKEN` nas mutações autenticadas. Login/refresh/forgot/reset/ott são isentos; logout **não**.

#### 1.3 `POST /auth/login`

```
POST /auth/login HTTP/1.1
Host: localhost:8080
Accept: application/json, */*;q=0.5
Content-Type: application/json

{
  "identificador": "admin@ufpr.br",
  "senha": "Admin@123456"
}
```

Sem `Authorization`, sem `X-XSRF-TOKEN` (endpoint isento de CSRF, `permitAll`).

**Por quê:** F0.1-a. `identificador` aceita e-mail UFPR, e-mail pessoal ou GRR. Senha do seed Flyway. O JSON **não** deve carregar JWT — só flags de pós-login.

#### 1.4 `GET /me` (com session)

```
GET /me HTTP/1.1
Host: localhost:8080
Accept: application/json, */*;q=0.5
Cookie: access_token=<JWT RS256 HttpOnly>; XSRF-TOKEN=<uuid>
```

**Por quê:** prova que o cookie `access_token` (Path=/) autentica. Bearer é só fallback. Confirma HATEOAS `_links` e `roles`.

#### 1.5 `GET /me` (sem session)

```
GET /me HTTP/1.1
Host: localhost:8080
Accept: application/json, */*;q=0.5
```

**Por quê:** F0.1-c / entrada 401. Sem cookie o filtro JWT deixa anônimo e o `authenticationEntryPoint` devolve Problem Details.

#### 1.6 `POST /auth/refresh`

```
POST /auth/refresh HTTP/1.1
Host: localhost:8080
Accept: application/json, */*;q=0.5
Cookie: refresh_token=<hex opaco Path=/auth>
```

Sem body.

**Por quê:** F0.1-e. Rotação: o refresh só viaja no cookie `Path=/auth`. O JSON não traz tokens. Redis ganha novo `auth:session:<sid>`.

#### 1.7 `POST /auth/login` (credencial inválida)

```
POST /auth/login HTTP/1.1
Content-Type: application/json

{
  "identificador": "naoexiste@ufpr.br",
  "senha": "SenhaErrada123!"
}
```

**Por quê:** anti-enumeração. E-mail inexistente, senha errada e conta bloqueada devem parecer iguais no HTTP.

#### 1.8 `POST /auth/logout`

```
POST /auth/logout HTTP/1.1
Cookie: access_token=<JWT>; XSRF-TOKEN=<uuid>
X-XSRF-TOKEN: <mesmo uuid do cookie>
```

Sem body.

**Por quê:** mutação autenticada — CSRF obrigatório. Redis `DEL auth:session:<sid>` (logout instantâneo) + revoga refresh no Postgres + `Max-Age=0` nos cookies.

#### 1.9 `POST /auth/ott` (token lixo)

```
POST /auth/ott HTTP/1.1
Content-Type: application/json

{
  "token": "not-a-jwt"
}
```

**Por quê:** F0.1-g/h no recorte negativo. CSRF isento. Token inválido → 401. Happy path (JWT one-time do e-mail de deliberação) depende de solicitação + outbox; fica para T-F1-005 / T-10.1.

### 2) O que aconteceu no backend (resumo)

1. **Health:** Actuator liveness/readiness, sem IAM.
2. **CSRF:** `CookieCsrfTokenRepository` + `SpaCsrfTokenRequestHandler` (token **plano**, igual ao cookie). `SkipBlankCsrfCookieRepository` evita `Set-Cookie: XSRF-TOKEN=; Max-Age=0` antes do cookie real (o HTTPie descartava o segundo). `Max-Age=43200` para o jar persistir session cookie.
3. **Login:** `RateLimitFilter` (5/min IP+identificador) → `LoginUseCase`: busca usuário, Argon2id, `issueAccessToken` com claim `sid`, Redis `SET auth:session:<sid>` (TTL = accessTTL+60s, fail-closed), `INSERT refresh_token`, audit `LOGIN_SUCCESS`. Cookies HttpOnly `access_token` (Path=/) e `refresh_token` (Path=/auth). JSON só flags.
4. **GET /me:** `JwtAuthenticationFilter` lê cookie → verifica RS256 → Redis `sessionExists(sid)` → `ProfileQuery.findByIdWithRoleAssignments` (sem JOIN FETCH de authorities, para não duplicar `ADMIN` no produto cartesiano) → `_links` mapa rel→path.
5. **GET /me anônimo:** entry point RFC 7807 `authentication-required`, UTF-8.
6. **Refresh:** marca o token antigo used, emite par novo, novo `sid` no Redis. Reuso (token já used) → `revokeAllForUser` + Redis `auth:force-logout:user:<uuid>` + 401.
7. **Login inválido:** `InvalidCredentialsException` → `IamExceptionHandler` 401 `unauthorized` / título `Não autorizado` / detail genérico. Conta bloqueada mapeada para o **mesmo** 401 (anti-enumeração).
8. **Logout:** CSRF Double Submit → `DEL auth:session:<sid>` → `@Transactional revokeAllForUser` (sem isso era 500 `TransactionRequiredException`) → limpa cookies.
9. **OTT inválido:** parse JWT falha → 401 `invalid-token`.

**Redis:** session store no login/refresh; force-logout no reuso; delete no logout. Sem Redis o filtro é fail-closed (request não autentica).

**Correções feitas nesta transação:** roles duplicadas; 401 fora do contrato; logout 500; CSRF XOR vs cookie + cookie em branco no HTTPie; charset ISO-8859-1 nos Problem Details; bootRun precisa de `SECURITY_JWT_*` em `.env.local`.

### 3) Responses (e por quê)

#### Health 200

```json
{ "status": "UP", "groups": ["liveness", "readiness"] }
```

Actuator padrão. Prova Postgres/Redis/app no ar o suficiente para o resto.

#### CSRF 200

```json
{
  "token": "<uuid igual ao cookie>",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf"
}
```

```
Set-Cookie: XSRF-TOKEN=<uuid>; Max-Age=43200; Path=/
```

O SPA lê o cookie (ou o JSON) e ecoa no header. Token plano para JSON === cookie (Double Submit). Sem isso o logout/contato autenticado toma 403.

#### Login 200

```
Set-Cookie: access_token=<JWT>; HttpOnly; Path=/; SameSite=Lax; Max-Age=900
Set-Cookie: refresh_token=<hex>; HttpOnly; Path=/auth; SameSite=Lax; Max-Age=604800

{
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

Tokens **só** em cookie (XSS não lê JWT). Flags mandam o SPA para `/inicio` ou placeholder de primeiro acesso. Admin seed já alterou senha e aceitou LGPD.

#### GET /me 200 (cookie)

```json
{
  "id": "01a055d7-4444-7c12-a5e4-cecc20ee6775",
  "nome": "Administrador Sistema",
  "email": "admin@ufpr.br",
  "ativo": true,
  "metadata": { "aceite_lgpd_em": "2026-01-01T00:00:00Z" },
  "roles": ["ADMIN"],
  "_links": {
    "self": "/me",
    "update-profile": "/me",
    "change-password": "/me/password",
    "notifications": "/me/notifications",
    "data-export": "/me/data-export"
  }
}
```

HATEOAS: `_links` é mapa **rel → string** (não HAL `{ href }`). UI cega a papéis: só mostra ação se a chave existir. `roles` único após correção do cartesian product.

#### GET /me 401 (sem cookie)

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/authentication-required",
  "title": "Não autenticado",
  "status": 401,
  "detail": "Token JWT inválido ou expirado."
}
```

RFC 7807. UTF-8 no `Content-Type` para o título não virar `N�o`.

#### Refresh 200

```json
{ "mensagem": "Token renovado com sucesso." }
```

+ novos `Set-Cookie`. Body sem tokens. Rotação reduz janela de roubo.

#### Login inválido 401

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Não autorizado",
  "status": 401,
  "detail": "Credenciais inválidas. Verifique seus dados e tente novamente.",
  "instance": "/auth/login"
}
```

Contrato da transação (não `invalid-credentials`). Detail idêntico para não enumerar contas.

#### Logout 200

```json
{ "mensagem": "Sessão encerrada com sucesso." }
```

```
Set-Cookie: access_token=; Max-Age=0; Path=/; HttpOnly
Set-Cookie: refresh_token=; Max-Age=0; Path=/auth; HttpOnly
```

Sessão Redis sumiu: o JWT antigo falha **na hora** em `GET /me` (401), sem esperar 15 min.

#### OTT inválido 401

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/invalid-token",
  "title": "Token inválido",
  "status": 401,
  "detail": "Token one-time inválido ou expirado.",
  "instance": "/auth/ott"
}
```

Deep-link sem JWT assinado com audience `request:*` não cria sessão.

---

## T-F0-002 — Recuperar senha

**Transação:** `transaçõesBackend/F0 — Público/T-F0-002-RECUPERAR-SENHA.md`  
**Diagrama:** `foundationDocs/sequenceDiagrams/F0 — Público/US-F0-002-RECUPERAR-SENHA.md`  
**Tutorial:** `httpie/F0-publico/T-F0-002-recuperar-senha.md`  
**Status:** executado e correto após restart (2026-08-31).

### 1) O que foi enviado (requests)

#### 1.1 `POST /auth/forgot-password` (e-mail existente)

```
POST /auth/forgot-password HTTP/1.1
Host: localhost:8080
Accept: application/json, */*;q=0.5
Content-Type: application/json

{"email": "admin@ufpr.br"}
```

Sem CSRF (endpoint isento). Sem JWT.

**Por quê:** F0.2-a. O identificador é só e-mail. O body **não** pede senha. A resposta deve ser idêntica para e-mail existente e inexistente (anti-enumeração). O use case, no ramo positivo, emite JWT audience `password-reset` (TTL 24h) e enfileira `outbox_event` `iam.password_reset_requested`.

#### 1.2 `POST /auth/forgot-password` (e-mail inexistente)

```
POST /auth/forgot-password HTTP/1.1
Content-Type: application/json

{"email": "nao.cadastrado@ufpr.br"}
```

**Por quê:** F0.2-b. Sem outbox, sem SMTP. O HTTP precisa ser bit-a-bit o mesmo 202.

#### 1.3 Rate limit — 4× o **mesmo** e-mail+IP

```
POST /auth/forgot-password
{"email": "limite.forgot@ufpr.br"}
```

Disparado 4 vezes em sequência. Chave Bucket4j = `IP:email`, 3 req/hora.

**Por quê:** o tutorial pede 4 disparos do passo 1 (mesmo e-mail). E-mails únicos **não** compartilham o bucket — isso foi um engano na sessão anterior.

### 2) Backend (resumo)

1. `RateLimitFilter` consome 1 token do bucket forgot-password.
2. `ForgotPasswordUseCase`: busca por e-mail; se existir, `issueOneTimeToken(audience=password-reset)`, `OutboxEventPublisher.enqueue(iam.password_reset_requested, payload.email/nome/token)`.
3. `OutboxDispatcher` (~5s) envia SMTP → Mailpit `:8025`. Status `PROCESSED`.
4. Redis **não** entra neste fluxo (JTI só na hora do reset). Cookie `XSRF-TOKEN` é emitido por qualquer GET/POST (CSRF cookie filter), mas o header CSRF **não** é exigido.

**Mailpit:** e-mail “Redefinição de senha” com link `http://localhost:3000/nova-senha?token=<JWT>`.

### 3) Responses

#### 202 (existente e inexistente — idênticos)

```json
{"mensagem":"Se este email existir, enviaremos um link válido por 24h."}
```

UTF-8 após o restart (antes o `á` vinha `v├ílido` em ISO-8859-1). Mensagem genérica de propósito: o atacante não descobre se a conta existe. 202 Accepted porque o e-mail é assíncrono (outbox), não “já enviado”.

#### 429 (4ª tentativa mesmo e-mail)

```
HTTP/1.1 429
Retry-After: 3594
Content-Type: application/problem+json;charset=UTF-8

{
  "type": "https://secretariaonline.ufpr.br/errors/rate-limit",
  "title": "Muitas tentativas",
  "status": 429,
  "detail": "Muitas tentativas. Aguarde antes de tentar novamente.",
  "retryAfterSeconds": 3594
}
```

RFC 7807 + header `Retry-After`. Impede flood de SMTP. `retryAfterSeconds` ≈ 1h (refill intervally 3/hora).

---

## T-F0-003 — Nova senha (token 1 uso)

**Transação:** `T-F0-003-NOVA-SENHA.md`  
**Diagrama:** `US-F0-003-NOVA-SENHA.md`  
**Tutorial:** `httpie/F0-publico/T-F0-003-nova-senha.md`  
**Status:** executado e corrigido. Usuário descartável `reset.teste@ufpr.br` (não alterar a senha do admin).

### 1) Requests

Pré-passos (dados): `POST /usuarios` admin+CSRF criou o aluno; senha temporária no outbox `iam.usuario_criado.payload.senhaTemporaria` (`UUID.take(12)`); `POST /auth/first-access` definiu `TempCurr3nt@2026!` (precisa ser forte para o teste de reuso chegar na checagem Argon2, não em `weak-password`).

#### 1.1 `POST /auth/forgot-password` `{ "email": "reset.teste@ufpr.br" }`

**Por quê:** emitir JWT fresco. Token lido em `GET /admin/outbox/{id}` (`payload.token`), não reproduzido neste log.

#### 1.2 Senha fraca (mesmo token)

```
POST /auth/reset-password
Content-Type: application/json

{"token":"<JWT password-reset>", "novaSenha":"fraca"}
```

CSRF isento.

**Por quê:** tutorial F0.3 / checklist “senha < 12 → 422 WeakPassword”. O JTI **não** pode ser consumido.

#### 1.3 Reuso da senha atual

```
{"token":"<mesmo JWT>", "novaSenha":"TempCurr3nt@2026!"}
```

**Por quê:** política “não reutilizar as 3 últimas **nem a atual**”. Correção: `hashesToReject = recentHashes + usuario.senhaHash`. Sem isso o primeiro reset da senha corrente passava 200.

#### 1.4 Happy path

```
{"token":"<mesmo JWT>", "novaSenha":"NovaS3nh@Forte2026!"}
```

#### 1.5 Replay do mesmo token

Mesmo JSON do 1.4.

#### 1.6 Login com a senha nova

```
{"identificador":"reset.teste@ufpr.br","senha":"NovaS3nh@Forte2026!"}
```

### 2) Backend

1. `ResetPasswordUseCase`: parse RS256, audience `password-reset`, JTI ainda não está no store de one-time (Postgres).
2. `validatePasswordStrength` → `WeakPasswordException` → `IamExceptionHandler` 422 `weak-password` (**depois** de remover `@Size(min=12)` do DTO; senão Jakarta devolvia `validation-error` e o tutorial falhava).
3. Argon2id.verify contra histórico + hash atual → `PasswordReuseException` 422 `password-reuse`. JTI intacto.
4. Sucesso (TX): novo hash, histórico, `emailOneTimeTokenStore.add(jti)`, `revokeAllForUser`, Redis `forceLogoutUser`, audit `PASSWORD_CHANGED`.
5. Replay: JTI já existe → `InvalidTokenException` com mensagem **genérica** (anti-enumeração). Type HTTP `unauthorized` (antes era `invalid-token`).
6. Login: cookies novos + Redis `auth:session:<sid>`. Flags `mustChangePassword/mustAcceptLgpd` false.

### 3) Responses

#### 422 weak-password

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/weak-password",
  "title": "Senha fraca",
  "status": 422,
  "detail": "Senha não atende os requisitos: mínimo 12 caracteres"
}
```

Domain, não Bean Validation. Token reutilizável.

#### 422 password-reuse

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/password-reuse",
  "title": "Senha já utilizada",
  "status": 422,
  "detail": "Esta senha já foi utilizada recentemente."
}
```

Título/detail alinhados à transação (não a mensagem crua da exception).

#### 200

```json
{"mensagem":"Senha redefinida com sucesso. Faça login novamente."}
```

Sem tokens no body. Sessões antigas mortas (refresh revogado + force-logout Redis).

#### 401 replay

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Token inválido",
  "status": 401,
  "detail": "Token de redefinição de senha inválido ou expirado."
}
```

Não distingue usado / expirado / lixo.

#### Login 200

```json
{"mustChangePassword":false,"mustAcceptLgpd":false}
```

+ `Set-Cookie: access_token` HttpOnly Path=/ ; `refresh_token` Path=/auth.

---

## T-F0-004 — Contato público

**Transação / diagrama / tutorial:** T-F0-004.  
**Status:** GET+POST+CSRF OK. Body vazio era 500 (`HttpMessageNotReadableException`) — handler 400 `validation-error` adicionado; reteste após restart.

### 1) Requests

#### 1.1 `GET /publico/contato`

Sem JWT. `permitAll`. Emite `Set-Cookie: XSRF-TOKEN`.

**Por quê:** F0.4-a dados institucionais + bootstrap CSRF para o POST. HATEOAS `_links.enviar`.

#### 1.2 `POST /publico/contato` com Double Submit

```
POST /publico/contato
Cookie: XSRF-TOKEN=<uuid>
X-XSRF-TOKEN: <mesmo uuid>
Content-Type: application/json

{
  "nome": "Ana Silva",
  "email": "ana.aluno@ufpr.br",
  "assunto": "Horário de atendimento",
  "mensagem": "A secretaria atende no sábado? Preciso protocolar uma declaração de matrícula."
}
```

**Por quê:** mutação pública. CSRF obrigatório (não está no `ignoringRequestMatchers`). Persistência `contact_message` + outbox `contato.recebido`.

#### 1.3 POST sem header CSRF

Mesmo JSON, sem cookie de sessão e sem `X-XSRF-TOKEN`.

**Por quê:** prova 403. Cookie sozinho ou header sozinho também deve falhar (Double Submit).

### 2) Backend

`ContatoPublicoQuery` devolve DTO estático. `SubmitContactUseCase` grava + outbox. Dispatcher encaminha ao e-mail institucional (`app.contato.email`). Sem CSRF: `CsrfFilter` → access denied handler (`forbidden`).

### 3) Responses

#### GET 200

```json
{
  "nome": "Secretaria SEPT — UFPR",
  "endereco": "Rua Dr. Alcides Vieira Arcoverde, 1225 — Jardim das Américas, Curitiba/PR",
  "telefone": "(41) 3360-4900",
  "email": "secretaria.sept@ufpr.br",
  "horario": "Segunda a sexta, 8h–17h",
  "_links": { "enviar": "/publico/contato" }
}
```

`_links` rel→string. O SPA só mostra o formulário se `enviar` existir.

#### POST 202

```json
{
  "id": "9be870fd-5c65-4697-a578-8c81a62612d5",
  "status": "ACEITO",
  "mensagem": "Mensagem recebida. Retornaremos por e-mail."
}
```

202: aceito para processamento assíncrono. `id` UUIDv7-ish da `contact_message`. `status` de fila, não “já respondido”.

#### POST sem CSRF 403

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/forbidden",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Você não tem permissão para esta operação."
}
```

Spring trata CSRF inválido como acesso negado (não 401). Type alinhado a `forbidden` (antes `access-denied`).

---

## T-F0-005 — Erros RFC 7807

**Status:** 401/403/404/429 OK. Login `{}` e body ausente eram 500 — corrigido (`LoginRequest` defaults + `HttpMessageNotReadableException` → 400).

### 1) Requests (por quê)

| Request | Por quê |
|---|---|
| `GET /me` sem cookie | entry point anônimo — 401 |
| `GET /bff/dashboard/secretaria` com JWT de aluno | FGAC `@PreAuthorize(dashboard.view_secretary)` — 403 (admin tem todas as authorities, então o 403 real usa um aluno) |
| `POST /auth/login` `{}` | Jakarta `@Valid` — 400 `validation-error` |
| `GET /requests/00000000-0000-0000-0000-000000000000` autenticado | `NoSuchElementException` — 404 |
| 6× `POST /auth/login` mesmo identificador senha errada | Bucket4j 5/min — 6º = 429 |

### 2) Backend

`SecurityConfig` authenticationEntryPoint / accessDeniedHandler escrevem Problem JSON UTF-8. `@RestControllerAdvice` IAM + global. Rate limit no filtro **antes** do controller.

### 3) Responses observadas

#### 401 GET /me

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Não autenticado",
  "status": 401,
  "detail": "Token JWT inválido ou expirado."
}
```

Type mudou de `authentication-required` para `unauthorized` (tabela T-F0-005 / tutorial).

#### 403

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/forbidden",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Você não tem permissão para esta operação.",
  "instance": "/bff/dashboard/secretaria"
}
```

Autenticado mas sem capability. UI cega a papéis: o botão nem existiria (`_links`).

#### 404 request

`type=not-found`, `Content-Type: application/problem+json`. UUID nulo não vaza se o recurso existe.

#### 429 login

Mesmo envelope `rate-limit` + `Retry-After`. Anti brute-force no identificador+IP.

---

## T-F0-006 / 007 — JWKS, protocolo e certificado públicos

**Status:** JWKS 200; protocolo e certificado **404** até existir solicitação (T-F1-005) e emissão (T-F1-010 / T-10.4). Rate limit 10/min não exercido.

### 1) Requests

```
GET /.well-known/jwks.json
GET /publico/solicitacoes/2026/99999
GET /publico/verificar-certificado/deadbeef… (64 hex)
```

Sem JWT. **Por quê:** verificação pública (QR do PDF / link de protocolo). JWKS para checagem offline Ed25519 + RS256.

### 2) Backend

JWKS monta a chave RSA de acesso (`.env.local`) + par Ed25519 efêmero de certificado em dev. Protocolo/cert: query por ano/número ou hash; 404 se não achar. Sem PII no 200.

### 3) Responses

#### JWKS 200

```json
{
  "keys": [
    {"kty":"RSA","use":"sig","alg":"RS256","kid":"jwt-signing-key-1","n":"…","e":"AQAB"},
    {"kty":"OKP","crv":"Ed25519","use":"sig","kid":"cert-signing-key-1","x":"…"}
  ]
}
```

RSA = JWT de sessão. OKP Ed25519 = assinatura do certificado anti-fraude.

#### Protocolo 404

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/not-found",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Protocolo não encontrado: 2026/99999"
}
```

Não revela se o número “quase existe”. Happy path 200 (sem nome do aluno, sem `dados` JSONB) depois de T-F1-005.

#### Certificado 404

Mesmo envelope `not-found` para hash inexistente.

#### Reexecução após T-F1-005 / T-F1-009 (2026-08-31)

`GET /publico/solicitacoes/2026/1` → **200** `{ protocolo: "2026/0001", tipo: "DECLARACAO_MATRICULA", estado: "DEFERIDA", _links.self }` — sem PII, sem `dados`.  
`GET /publico/verificar-certificado/{hash EVENTO}` → **200** `{ valido: true, verificacaoAssinatura: "ED25519_VALID", integridadePdf: true, ephemeralKey: true }` (par Ed25519 gerado nesta subida). Hash de formativa emitido antes do restart ficou `INVALID` (chave efêmera mudou) — esperado em dev.

---

## Correções de backend feitas nesta passagem (F1–F8)

1. **Presença:** `openWindow` fazia `UPDATE estado=EM_ANDAMENTO` e em seguida `save()` da entidade ainda `AGENDADO`, revertendo o estado. Check-in dava 409. Corrigido mutando `event.estado` no managed entity.
2. **Relatórios:** JPQL/native `(:fromTs IS NULL OR …)` gerava `? IS NULL` sem tipo (PostgreSQL 42P18). Native queries agora usam `CAST(:fromTs AS timestamptz) IS NULL`.
3. **`GET /academico/disciplinas`:** `CAST(:cursoId AS uuid)` com `null` vinha como `bytea`. Alias sem `idCurso` passou a `searchActiveAll` (só texto); com curso usa `searchByCurso`.
4. **On-behalf:** seed usa `request.internal_open`; o controller só aceitava `request.open_on_behalf` → secretaria 403. Aceita os dois códigos.
5. **Busca:** `SecurityContext` copiado para `CompletableFuture` (já no working tree; retestado 200).
6. **Evento ABERTURA** no `OpenRequestUseCase` (já no working tree; retestado no protocolo 2026/0002).

---

## T-F1-001 — Dashboard do aluno (BFF)

**Transação / diagrama / tutorial:** T-F1-001 · US-F1-001 · `httpie/F1-aluno/T-F1-001-dashboard.md`

### 1) Request

```
GET /bff/dashboard/aluno HTTP/1.1
Host: localhost:8080
Cookie: access_token=<JWT aluno Path=/>; XSRF-TOKEN=<uuid>
Accept: application/json
```

Sem body. **Por quê:** o BFF agrega KPIs + pendências + eventos + últimas solicitações numa round-trip. Cookie HttpOnly autentica (não Bearer). Cache Redis/Spring `bff-dashboard` chave `aluno:{uuid}` TTL ~60s — o HTTP `Cache-Control: no-store` impede cache de browser, não o cache de servidor.

Professor no mesmo path: mesmo GET, cookie `so2-prof`.

### 2) Backend

`DashboardAlunoController` → `DashboardAlunoQuery`. Ports dos bounded contexts (formativas, presença, solicitações, atendimentos). FGAC `dashboard.view_own`. Cache hit na 2ª chamada não bate nos ports.

### 3) Response

**200 aluno** (após formativa + solicitações):

```json
{
  "kpis": { "horasFormativas": { "atual": 4.0, "requerido": 120.0, "percentual": 3.33 }, "atendimentosPendentes": 1 },
  "pendencias": [],
  "eventos": [],
  "ultimasSolicitacoes": [ { "id": "…", "tipo": "DECLARACAO_MATRICULA", "estado": "ABERTA" } ],
  "_links": { "self": "/bff/dashboard/aluno", "novaSolicitacao": "/requests/types", "formativas": "/formativas/minhas", "eventos": "/events?audience=me" }
}
```

HATEOAS `rel → string`: UI cega a perfil. **403 professor:** `type=forbidden` — JWT sem `dashboard.view_own`.

---

## T-F1-002 — Primeiro acesso

Coberto no bootstrap (`02-bootstrap-usuarios-demo.md`) e revalidado em T-F2 (egresso).  
`POST /auth/first-access` com cookie da sessão do login temporário + CSRF + `{ novaSenha, aceiteLgpd: true }` → **200** `{ mensagem }`. Sem isso `mustChangePassword` / `mustAcceptLgpd` permanecem true no login.

---

## T-F1-003 — Perfil `/me`

### 1) Requests

```
GET /me
PATCH /me   X-XSRF-TOKEN + { "metadata": { "idCurso": "<TADS uuid>" } }
```

**Por quê:** GET prova cookie; PATCH grava JSONB `usuario.metadata` (curso do aluno, necessário para eventos `audience=me`). CSRF porque mutação autenticada.

### 2) Backend

`ProfileQuery` com `findByIdWithRoleAssignments` + distinct (evita `roles: ["ADMIN"×N]`). PATCH merge metadata, audit.

### 3) Response

**200** perfil com `roles` únicos, `_links` (`self`, `update`, `password`, `avatar`, `data-export`). Avatar MinIO PUT não exercido nesta passagem.

---

## T-F1-004 — Comunicação

### 1) Requests

```
GET /communications/me
GET /communications/me/unread-count
POST /communications   (admin, CSRF)  { titulo, corpo, audience }
```

**Por quê:** inbox HATEOAS; unread para badge; publish dispara fan-out + outbox.

### 2) Backend

`CommunicationsController`. Deliveries por usuário. Unread = `{ unread: N }` (código; tutorial às vezes diz `count`).

### 3) Response

Inbox **200** com entregas. Unread `{ "unread": 3 }`. Publish **200** `{ id, entregas: 6 }`.

---

## T-F1-005 — Solicitações (workflow engine)

**IDs:** tipo `DECLARACAO_MATRICULA` `01a055d7-4451-7c68-b714-b1d49ba351c3`; curso TADS `01a055d7-4449-74b3-828a-a4b44cef7dfd`; request deferida `d72e7178-b51f-4a11-88c9-ad73ed92467a` protocolo `2026/0001`; 2ª abertura `9144d3da-…` `2026/0002`; draft submetido `dd8fdb23-…` `2026/0003`.

### 1) Requests

```
GET /requests/types
POST /requests   CSRF + {
  "idRequestType": "01a055d7-4451-7c68-b714-b1d49ba351c3",
  "idCurso": "01a055d7-4449-74b3-828a-a4b44cef7dfd",
  "dados": { "finalidade": "BOLSA", "observacoes": "…" }
}
GET /requests/{id}
GET /requests/{id}/events
GET /requests/{id}/protocol
POST /requests/{id}/transitions   { "action": "ASSIGN"|"DEFER", "parecer": "…" }
POST /requests/draft …  POST /requests/{id}/submit
```

**Por quê:** `form_schema` (JSON Schema + `x-ui`) e `workflow_json` (estados/transições) vêm do catálogo + snapshot `request_type_version` (V019). `dados` tem de satisfazer o schema (`finalidade` enum). Actions seed: `ASSIGN`, `DEFER` — não `DEFERIR`. CSRF nas mutações. Aluno **não** manda `action=ASSIGN` (403 `insufficient-authority`).

### 2) Backend

`OpenRequestUseCase`: valida schema, lê `workflow.initial` (`ABERTA`), número anual, persiste `request` + `request_event` tipo `ABERTURA` + outbox `solicitacoes.aberta`. `TransitionRequestUseCase` consulta transições da versão, FGAC, grava evento + outbox `solicitacoes.assign` / `solicitacoes.defer`. Detalhe devolve `formSchema` da versão, `dados` JSONB, `_links` só das actions legais no estado atual.

### 3) Responses

**GET types 200:** array com `formSchema`, `_links.open` / `save-draft`.  
**POST 201:** `{ id, _links.self }`.  
**GET detail 200:** `protocolo`, `estado`, `dados`, `formSchema`, `idRequestTypeVersion`, `_links` mapa.  
**GET events (após fix):** `[{ "tipo":"ABERTURA", "estadoAnterior":"-", "estadoNovo":"ABERTA" }]`. Instância antiga (pré-fix) só tem ASSIGN/DEFER.  
**Aluno ASSIGN 403.** Secretaria ASSIGN → `EM_TRIAGEM`; DEFER → `DEFERIDA`.  
**Submit draft 200:** `{ estado:"ABERTA", protocolo:"2026/0003" }`.

---

## T-F1-006 — Formativas

### 1) Request

```
POST /formativas  CSRF  {
  "titulo": "Palestra: Machine Learning Aplicado",
  "categoria": "PALESTRA", "cargaHoraria": 4.0,
  "dataRealizacao": "2026-06-15",
  "storageKeyComprovante": "httpie/comprovante-demo.pdf"
}
GET /formativas/minhas
GET /formativas/resumo
```

**Por quê:** submissão cria linha `PENDENTE` para a fila CAAF. Presign MinIO opcional (storageKey aceito no MVP sem objeto).

### 2) Backend

`SubmitFormativaUseCase` persiste atividade. Resumo lê horas aprovadas vs `curso.horas_formativas_minimas`.

### 3) Response

**201** `{ id: "d1a0ed95-…", estado: "PENDENTE" }`. Minhas: página HATEOAS. Resumo `{ horasAprovadas: 0, horasRequeridas: 120, percentual: 0 }` até a aprovação CAAF.

---

## T-F1-007 / 008 — Estágio e TCC

### 1) Requests

```
POST /internships  { empresa, cargo, cargaHorariaSemanal, inicio }
GET /internships/mine
POST /tccs  { titulo, idCurso }
POST /tccs/{id}/members  { idAluno, papel: "AUTOR" }
POST /tccs/{id}/examiners  { idProfessor, papel: "BANCA" }
PATCH /tccs/{id}/grade  { nota: 9.5 }
PATCH /tccs/{id}/approve  { aprovado: true, notaFinal: 9.2 }
GET /tccs/mine
```

Paths reais: `/internships/mine`, `/tccs/mine` (não `/estagios/me` nem `/tcc/me` — esses 404 RFC 7807).

### 2) Backend

Estágio nasce `EM_ANDAMENTO`; COE atribui supervisor; `conclude` → `CONCLUIDO` + outbox. TCC: orientador cria, membro AUTOR habilita `tccs/mine` do aluno, banca+nota, approve → `APROVADO` (critério de colação).

### 3) Responses

Estágio **201** `4df831f8-…` `EM_ANDAMENTO`; após COE+conclude `{ estado: "CONCLUIDO", fim: "2026-08-31" }`.  
TCC **201** `be5ab905-…`; mine aluno 200 com o título; approve `{ estado: "APROVADO", notaFinal: 9.2 }`. Upload PDF MinIO do TCC não exercido.

---

## T-F1-009 — Presença

**Evento válido:** `907818cf-00b9-4a55-8974-083d380304a8` (datas cobrindo “agora”). PIN janela: `303155`.

### 1) Requests

```
POST /events  CSRF  { titulo, idCurso, attendanceMode: "SECRET_SINGLE", chCreditadas, inicioEm, fimEm }
POST /events/{id}/attendance/windows/entry  { durationSeconds: 3600 }
GET /events/{id}/attendance/session
POST /events/{id}/attendance/entry  { pin, deviceUuid }
POST /events/{id}/close
```

**Por quê:** janela grava JSONB `validation_windows` (phase, openAt, closeAt, secret). PIN não vai no JWT. `deviceUuid` anti-sharing. Close emite PDF + hash + Ed25519.

### 2) Backend

Create → `AGENDADO`. Open entry window **deve** promover `EM_ANDAMENTO` (bug do save revertido, corrigido). Confirm valida estado + janela ativa + PIN. Close → `CONCLUIDO` + `CertificateIssuerService` + MinIO + outbox `certificate.issued`.

### 3) Responses

Window **200** `{ mensagem, closeAt, secret: "303155" }`.  
Session **200** `estado: EM_ANDAMENTO`, `_links.confirmar-entrada`.  
Confirm **200** `{ mensagem: "Entry confirmada com sucesso." }`.  
Close **200** `{ mensagem: "Evento encerrado. 1 certificados emitidos.", certificadosEmitidos: 1 }`.

Tentativa anterior no evento `da124ef3-…`: 409 `Evento não está em andamento` por causa do overwrite — documentado acima.

---

## T-F1-010 / 011 — Certificados e atendimentos

### 1) Requests

```
GET /certificates/mine
GET /certificates/{id}/download-url
GET /publico/verificar-certificado/{hashSha256}
POST /me/service-records  { assunto, descricao, tipo: "AGENDAMENTO" }
POST /service-records  (secretaria) { idAluno, assunto, tipo: "PRESENCIAL" }
POST /service-records/{id}/acknowledge
```

### 2) Backend

Lista por `idAluno` (IDOR). Download = presign MinIO 15 min. Verify recomputa SHA-256 do objeto + Ed25519. Atendimento secretaria nasce `PENDENTE_CIENCIA`; aluno acknowledge → `CIENTE` + outbox.

### 3) Responses

Mine: formativa `c531c4cc-…` + evento `c508fce3-…` com `_links.download` e `verify`.  
Verify EVENTO (mesma JVM): `valido: true`, `ED25519_VALID`.  
Service record secretaria **201** `PENDENTE_CIENCIA`; ack **200** `CIENTE`.

---

## T-F2-001 — Dashboard egresso

### 1) Requests

```
POST /usuarios  { nome, email: "ana.egressa@ufpr.br", roleCode: "EGRESSO" }
POST /auth/login  { identificador, senhaTemporaria do outbox }
POST /auth/first-access  { novaSenha: "EgressoS3nh@Forte!", aceiteLgpd: true }
GET /bff/dashboard/egresso
GET /bff/dashboard/egresso   (cookie aluno)
```

**Por quê:** colação completa (5 critérios) não foi o caminho — Ana ainda tem horas < mínimo e solicitações ABERTA. Criar usuário EGRESSO testa o BFF + FGAC `alumni.view_own`. Senha só no payload outbox `iam.usuario_criado` (`senhaTemporaria`, 12 chars).

### 2) Backend

`DashboardEgressoQuery` cache `egresso:{uuid}`. Sem `_links.novaSolicitacao`.

### 3) Responses

**200** `{ tccsDefendidos: 0, _links: { self, certificados, comunicados } }`. Aluno **403** `forbidden`.

---

## T-F3 — Professor

```
GET /bff/dashboard/professor
```

**200** `{ meusEventos: [… CONCLUIDO …], solicitacoesPendentes: [], _links: { self, novoEvento: "/events", meusEventos: "/events?host=me" } }`. Authority `dashboard.view_self_professor`. Eventos/deliberar/formativas reutilizam T-F1-005/006/009.

---

## T-F4-001 — CAAF

Paths reais: `/commissions/caaf/pool|/{id}/claim|batch-review|stats` (não `/queue`).

### 1) Requests

```
GET /commissions/caaf/pool
POST /commissions/caaf/{formativaId}/claim
POST /commissions/caaf/batch-review  { ids, acao: "APROVAR", parecer }
GET /commissions/caaf/stats
```

Admin (tem `formative.review`). CSRF nos POSTs.

### 2) Backend

Pool = `PENDENTE` sem revisor. Claim seta `idRevisor`. Batch emite certificado origem FORMATIVA + outbox.

### 3) Responses

Pool 200 com a palestra. Claim `{ idRevisor: admin }`. Batch `{ processadas: 1, estado: "APROVADA" }`. Stats `{ totalPendente: 0, aprovadasHoje: 1 }`.

---

## T-F4-002 — COE

```
GET /commissions/coe/pool
POST /commissions/coe/{internshipId}/assign-supervisor  { idSupervisor }
GET /commissions/coe/stats
POST /internships/{id}/conclude
```

**200** pool com estágio XYZ; assign `{ idSupervisor: prof.ana }`; stats `{ semSupervisor: 0 }` após assign; conclude `{ estado: "CONCLUIDO" }`. Authority `internship.review`.

---

## T-F5 — Secretaria

### Dashboard

`GET /bff/dashboard/secretaria` → **200** KPIs + `_links.solicitacoes` / `usuarios`. Cache `secretaria:static`.

### Fila / on-behalf / bulk

- `GET /requests?estado=ABERTA` 200 (secretaria vê o curso).
- `POST /requests` com `idSolicitanteOnBehalf` → **201** `7d0e78e2-…`, detalhe `idSolicitante` = aluno (não a secretaria). Precisa `request.internal_open` (seed) — corrigido no `@PreAuthorize`.
- `PATCH /requests/bulk-deliberate` `{ ids, action: "DEFER" }` a partir de **ABERTA** → **422** `invalid-transition` (workflow DECLARACAO exige ASSIGN antes). Correto para o motor.

### Usuários / acadêmico

`GET /usuarios`, `/academico/cursos`, `/academico/periodos/ativo` **200**.

### T-F5-005 egressos

`GET /students?eligibleForGraduation=true` **200** com `eligible: false` e `bloqueios` (TCC/HISTORICO/HORAS/SOLICITACOES). Ana: horas 4 < 150 (config patch) + solicitações ABERTA. `GET /secretaria/egressos` lista vazia (colação não fechada). Contrato de bloqueio está correto.

### T-F5-009 import

```
GET /imports/templates/alunos   → 200 text/csv
POST /imports/alunos  multipart file=import-alunos.csv
POST /imports/{jobId}/confirm
```

Upload **202/200** `VALIDATED` 2 linhas 0 erros. Confirm **200** `COMPLETED` successCount=2 (Carlos, Beatriz). Outbox `imports.completed`.

### T-F5-010 export

```
POST /exports/alunos  → 202 { jobId, status: PROCESSANDO, _links }
GET /exports/{jobId}  → PRONTO
GET /exports/{jobId}/download → { downloadUrl: MinIO presign }
```

Worker ~5s. Outbox `exports.ready`.

### T-F5-011 stats

`GET /reports/secretary?periodo=2026-2&curso=TADS` **200**:

```json
{
  "filtros": { "periodo": "2026-2", "curso": "TADS", "cursoId": "…" },
  "kpis": { "alunosAtivos": 3, "egressos": 1, "solicitacoesAbertas": 3, "eventosAgendados": 0 },
  "solicitacoesPorTipo": [{ "tipo": "DECLARACAO_MATRICULA", "total": 4 }],
  "distribuicaoPorEstado": [{ "estado": "ABERTA", "total": 3 }, { "estado": "DEFERIDA", "total": 1 }],
  "evolucaoTemporal": [{ "mes": "2026-08", "total": 4 }],
  "rankingCursos": [{ "sigla": "TADS", "total": 4 }]
}
```

`curso=TADS` resolve UUID; `periodo` usa janela do período letivo. 500 inicial = parâmetro JDBC sem tipo (corrigido).

### T-F5-012 tarefas

```
POST /tasks  { titulo, prioridade: ALTA, prazoEm }
PATCH /tasks/{id}  { estado: EM_ANDAMENTO, idAssignee }
```

**201** `PENDENTE` `c911e5a2-…`; patch **200** `EM_ANDAMENTO`. Delete só PENDENTE (não exercido após mover coluna).

---

## T-F6 — Coordenação

```
GET /courses/tads/config
PATCH /courses/tads/config  { horasFormativasMinimas: 150, … }
GET /reports/coordinator?periodo=2026-2&curso=TADS
GET /academico/relatorios/curso
POST /academico/disciplinas  { codigo: TADS-CAL1, cargaHorariaTotal, creditos }
PUT /academico/alunos/{alunoId}/historico/{disciplinaId}  { estado: CONCLUIDA }
GET /academico/disciplinas
```

Ownership: `curso.id_coordenador` (SQL no bootstrap). PATCH audit `COURSE_CONFIG_UPDATED`. Relatório coordenador **200** (tempo médio deliberação, formativas, carga por deliberador, SLA, `_links`). Alias disciplinas **200** após o split de queries. Histórico **200** `CONCLUIDA`.

---

## T-F7 — Admin

### Roles / workflow / templates / audit / FAQ / health

```
GET /admin/roles
GET /request-types
POST /request-types  { code: ATESTADO_MATRICULA_TESTE, formSchema, workflowJson }
POST /request-types/{id}/publish
GET /requests/types   (aluno — tipo publicado aparece)
GET /communication-templates
GET /admin/audit?page=0&size=3
POST /faq  { pergunta, resposta, categoria, ordem }
GET /actuator/health
```

Publish grava `request_type_version` (V019) e `ativo=true`. Aluno passa a ver `ATESTADO_MATRICULA_TESTE` com `_links.open`. Audit paginado HATEOAS, ações `LOGIN_SUCCESS`. Health `{ status: UP, groups: [liveness, readiness] }` (Redis UP no componente quando autorizado). Path `/admin/communication-templates` **não existe** → 404 `not-found`; o real é `/communication-templates`.

---

## T-F8-001 — Busca

```
GET /search?q=ana&page=0&size=10          (aluno)
GET /search?q=ana&types=USUARIO           (admin)
GET /search?q=TADS&types=CURSO
```

Aluno **200** `{ results: [], totalResults: 0 }` — FGAC omite `USUARIO`; não há request/evento titulado “ana”. Admin USUARIO **200** Ana Professora + Ana Aluno com `href`. CURSO **200** TADS. 500 anterior = SecurityContext perdido no `supplyAsync` (corrigido).

---

## T-F8-002 — FAQ e suporte

```
GET /faq
POST /support/tickets  { assunto, descricao }
GET /support/tickets/mine
GET /support/tickets
PATCH /support/tickets/{id}/respond  { resposta }
PATCH /support/tickets/{id}/close
```

FAQ seed V013 **200**. Ticket **201** `9d267956-…` `ABERTO`; respond `RESPONDIDO`; close `FECHADO`. Campo é `descricao`, não `mensagem`.

---

## Transversais T-10.1 / T-10.4 / T-10.6 — Outbox e certificado

```
GET /admin/outbox?status=PROCESSED&page=0&size=5
GET /admin/outbox/{id}     (payload completo — senhas/JWT não copiados na íntegra)
GET /admin/outbox/dead
```

Dispatcher `@Scheduled` ~5s: `PENDING` → `PROCESSED` (Mailpit :8025). Tipos observados: `iam.usuario_criado`, `solicitacoes.aberta`, `solicitacoes.assign`, `solicitacoes.defer`, `atendimentos.created`, `estagio.declarado`, `certificate.issued`, `presenca.confirmada`, `exports.ready`, `imports.completed`, `tcc.deliberado`. Dead vazio. Retry/delete não exercidos (sem DEAD).

Certificado: emissão só como efeito colateral (close evento / APROVAR formativa). Público + JWKS em T-F0-006/007.

---

## Redis (sessão / force-logout / cache BFF)

Chaves vistas: `auth:session:<sid>` (uma por login). Logout apaga a sessão → access token inválido mesmo antes do `exp` 15 min. Reset de senha dispara force-logout (revoke refresh + drop sessions). Cache BFF usa `CacheManager` cache `bff-dashboard` (chaves lógicas `aluno:{uuid}`, `secretaria:static`, `egresso:{uuid}`) — TTL curto; headers HTTP continuam `no-store`.

---

## Cookies de autenticação (relembrando F0)

| Cookie | Path | HttpOnly | Uso |
|--------|------|----------|-----|
| `access_token` | `/` | sim | JWT RS256 15 min, `sid` → Redis |
| `refresh_token` | `/auth` | sim | rotação 7d |
| `XSRF-TOKEN` | `/` | não | Double Submit; ecoar em `X-XSRF-TOKEN` |

Login/refresh/forgot/reset/ott: CSRF-exempt. Logout e mutações autenticadas: CSRF obrigatório.

---

## IDs demo desta execução

| Recurso | UUID |
|---------|------|
| Admin / Aluno / Prof / Sec / Coord | `01a055d7-4444-…` / `1e29dbd7-…` / `7edddaf0-…` / `b94c1881-…` / `db88f8a2-…` |
| Curso TADS | `01a055d7-4449-74b3-828a-a4b44cef7dfd` |
| RequestType DECLARACAO | `01a055d7-4451-7c68-b714-b1d49ba351c3` |
| RequestType teste publicado | `f3610227-6958-4f9a-aaa2-8084b13b88e2` |
| Evento presença OK | `907818cf-00b9-4a55-8974-083d380304a8` |
| Cert EVENTO hash | `8738bcca55f6907f05d0bb835c4e53ba1d88463aa254dc6d72a6d7e7ecb07e32` |
| Egresso | `df018f3e-3b91-4caf-887a-7a902bc8c2c0` |

Capturas brutas (`--print=HhBb`): `logs/raw/`. JWTs omitidos neste markdown.

