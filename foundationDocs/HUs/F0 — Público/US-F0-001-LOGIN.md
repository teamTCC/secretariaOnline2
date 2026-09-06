# US-F0-001 — Autenticação de Usuário (Login)

| Campo | Valor |
|-------|-------|
| **ID** | US-F0-001 |
| **Épico** | AUTH-LOGIN |
| **Tela** | F0.1 — `/login` |
| **Prioridade** | **P0 — MVP v1** |
| **Plataforma** | Web + Mobile |
| **API primária** | `POST /auth/login` (relacionado: `POST /auth/ott`) |
| **Frames Figma** | [Default/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=26-152) · [Error/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=26-188) · [Loading/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=26-255) · [Default/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=26-295) |
| **Spec de tela** | `telasFigma/telas0/F0.1-login.md` |
| **Substitui (legado)** | `web/logarUsuario.jsp` [T01] |

---

## 1. História de Usuário

> **Como** um usuário do sistema (Aluno, Egresso, Professor, Secretária, Coordenador ou Administrador),  
> **Quero** me autenticar informando meu identificador (e-mail institucional, e-mail pessoal ou GRR) e minha senha,  
> **Para** acessar o painel com as funcionalidades correspondentes ao meu perfil de forma segura.

---

## 2. Regras de Negócio

### Identificação e autenticação

| ID | Regra |
|----|-------|
| **RN-F0.1-01** | O campo identificador aceita três formatos: e-mail institucional (`@ufpr.br`), e-mail pessoal cadastrado ou GRR (formato numérico). O backend normaliza o identificador antes da busca. |
| **RN-F0.1-02** | A verificação de senha usa exclusivamente o algoritmo **Argon2id** (substituição ao MD5 do legado). Nenhum hash MD5 deve ser gerado ou comparado pelo novo sistema. |
| **RN-F0.1-03** | Em caso de sucesso, o backend emite dois tokens **apenas em cookies HttpOnly** (nunca no JSON): `access_token` (JWT, Path=/, 15 min) e `refresh_token` (rotativo, Path=/auth, 7 dias). O JSON de 200 contém somente `mustChangePassword` e `mustAcceptLgpd`. JWT inclui claim `sid`; sessão Redis obrigatória em `auth:session:<sid>`. Fallback: `Authorization: Bearer`. Mobile: cookies/Keychain conforme o cliente. |
| **RN-F0.1-04** | Se o campo `usuario.senha_alterada = false` (primeiro acesso ou reset administrativo), a resposta deve conter `mustChangePassword: true`. O frontend **bloqueia** o acesso ao dashboard e redireciona para `/primeiro-acesso`. |
| **RN-F0.1-05** | Após autenticação bem-sucedida com `mustChangePassword = false`, o frontend redireciona para `/inicio` (F1.1). |

### Segurança e prevenção de abusos

| ID | Regra |
|----|-------|
| **RN-F0.1-06** | O endpoint `POST /auth/login` é protegido por **rate limiting** via Bucket4j: máximo de **5 tentativas por minuto** por combinação de IP + identificador. |
| **RN-F0.1-07** | Após **10 falhas consecutivas** para o mesmo identificador, a conta é bloqueada temporariamente por **15 minutos**. O evento `iam.account_blocked` é emitido e registrado em auditoria. |
| **RN-F0.1-08** | Todos os erros de autenticação (credencial incorreta, usuário inexistente, conta inativa, conta bloqueada) retornam a **mesma mensagem genérica** — **anti-enumeração**: `"Credenciais inválidas. Verifique seus dados e tente novamente."` com status HTTP `401`. |
| **RN-F0.1-09** | Se o rate limit for atingido, a resposta deve ser `429 Too Many Requests` e o frontend exibe `DS/AlertBanner` variante `warning` com mensagem: `"Muitas tentativas. Aguarde antes de tentar novamente."` |
| **RN-F0.1-10** | Se um **refresh token já rotacionado** for re-apresentado, todas as sessões ativas do usuário devem ser **invalidadas imediatamente** (defesa contra roubo de token). |
| **RN-F0.1-11** | Cada tentativa de login (sucesso ou falha) emite um evento auditável com: `actor_id`, IP de origem, User-Agent e resultado (`SUCCESS` / `FAILURE`). |

### CSRF e transporte

| ID | Regra |
|----|-------|
| **RN-F0.1-12** | A proteção CSRF é implementada via **Double Submit Cookie** + `SameSite=Lax`. Não há sessão servlet (JSESSIONID). Há sessão Redis `auth:session:<sid>` obrigatória no login (claim JWT `sid`). |
| **RN-F0.1-13** | Deep-link de e-mail usa `?ott=`. O cliente troca o JWT de uso único em `POST /auth/ott` (`ExchangeOttUseCase`): body `{ "token": "<jwt>" }`, audience `request:{uuid}`. Sucesso: mesmo contrato do login (cookies + JSON de flags). Replay do JTI → 401. Relacionado: UC-AUT-07. |

---

## 3. Critérios de Aceitação

### CA-01 — Login com sucesso (fluxo principal)

```gherkin
Dado que o usuário está na tela de login (/login)
  E possui conta ativa com senha já definida (senha_alterada = true)
Quando preenche o campo "Email ou GRR" com identificador válido
  E preenche o campo "Senha" com a senha correta
  E clica no botão "Entrar"
Então o componente DS/Button exibe estado "loading" (spinner + label "Entrando...")
  E o sistema realiza POST /auth/login com os campos como JSON
  E ao receber 200 OK o JSON contém apenas mustChangePassword e mustAcceptLgpd
  E o browser recebe cookies HttpOnly access_token e refresh_token
  E o backend cria sessão Redis auth:session:<sid>
  E redireciona para /inicio (F1.1)
  E o evento iam.login_success é registrado na tabela audit_log
```

### CA-02 — Primeiro acesso (redirecionamento obrigatório)

```gherkin
Dado que o usuário está em /login
  E a conta tem senha_alterada = false
Quando informa credenciais corretas e clica em "Entrar"
Então o sistema recebe mustChangePassword = true (e mustAcceptLgpd conforme o aceite) no JSON
  E os tokens vão só nos cookies (não no body)
  E o frontend bloqueia o acesso ao dashboard
  E redireciona para /primeiro-acesso (F1.2)
  E não é possível navegar para /inicio até que a senha seja alterada
```

### CA-03 — Credenciais inválidas (anti-enumeração)

```gherkin
Dado que o usuário está em /login
Quando informa identificador válido com senha incorreta
  OU informa identificador inexistente na base de dados
  E clica em "Entrar"
Então o sistema retorna 401 Unauthorized (RFC 7807)
  E exibe DS/AlertBanner variante "danger" com mensagem genérica:
    "Credenciais inválidas. Verifique seus dados e tente novamente."
  E o campo "Email ou GRR" permanece preenchido
  E o campo "Senha" é limpo automaticamente
  E a mensagem não revela se o identificador existe ou não
  E o evento iam.login_failed é registrado na tabela audit_log
```

### CA-04 — Rate limit atingido

```gherkin
Dado que o mesmo IP + identificador realizou 5 tentativas de login em menos de 1 minuto
Quando uma nova tentativa é realizada
Então o sistema retorna 429 Too Many Requests
  E exibe DS/AlertBanner variante "warning":
    "Muitas tentativas. Aguarde antes de tentar novamente."
  E o botão "Entrar" permanece desabilitado enquanto o bloqueio temporário estiver ativo
```

### CA-05 — Bloqueio de conta por tentativas excessivas

```gherkin
Dado que o identificador acumulou 10 falhas consecutivas de autenticação
Quando uma nova tentativa de login é feita para esse identificador
Então o sistema retorna 401 com mensagem genérica (mesma de CA-03, sem revelar bloqueio)
  E internamente registra o evento iam.account_blocked
  E a conta permanece bloqueada por 15 minutos
```

### CA-06 — Validação de campos vazios (frontend)

```gherkin
Dado que o usuário está em /login com o formulário vazio
Quando clica no botão "Entrar" sem preencher os campos
Então o campo vazio exibe borda "border/error" e mensagem de validação inline
  E o sistema NÃO realiza chamada à API
  E o foco é direcionado ao primeiro campo inválido
```

### CA-07 — Links de navegação da tela de login

```gherkin
Dado que o usuário está em /login
Quando clica no link "Esqueci minha senha"
Então é redirecionado para /recuperar-senha (F0.2)

Quando clica no link "Contato"
Então é redirecionado para /contato (F0.4)

Quando clica no link "Verificar protocolo ou certificado"
Então é redirecionado para /publico/verificar-protocolo (F0.6)
```

### CA-08 — Acessibilidade

```gherkin
Dado que o usuário navega pela tela de login usando apenas o teclado
Então a ordem de tabulação segue: identificador → senha → "Esqueci minha senha" → "Entrar" → links
  E o toggle de visibilidade da senha possui aria-label descritivo
  E o DS/AlertBanner de erro possui aria-live="polite"
  E todos os textos visíveis têm contraste mínimo 4.5:1 (WCAG 2.1 AA)
```

### CA-09 — Responsividade mobile

```gherkin
Dado que o usuário acessa /login em dispositivo com largura 375px
Então o card de autenticação ocupa toda a largura com margem horizontal de 16px (space/md)
  E o botão "Entrar" não é encoberto pelo teclado virtual (safe area)
  E todos os elementos são interativos com touch (target mínimo 44px)
```

### CA-10 — Exchange OTT (relacionado — deep-link)

```gherkin
Dado que o usuário abriu um e-mail com URL contendo ?ott=<jwt>
Quando o cliente envia POST /auth/ott com { "token": "<jwt>" }
Então o sistema consome o JTI (Redis), emite sessão Redis e cookies access_token + refresh_token
  E retorna 200 com o mesmo JSON do login (mustChangePassword, mustAcceptLgpd)
  E uma segunda apresentação do mesmo token retorna 401
```

---

## 4. Componentes de UI (Design System)

| Componente | Variante | Uso |
|------------|---------|-----|
| `Shell/AuthLayout` | `state=default` | Shell da tela |
| `DS/Input` | `state=default` / `state=error` | Campo identificador |
| `DS/Input/password` | `state=default` | Campo senha |
| `DS/Button` | `variant=primary, state=default` | Botão "Entrar" |
| `DS/Button` | `variant=primary, state=loading` | Botão durante submit |
| `DS/Button` | `variant=ghost` | Link "Esqueci minha senha" |
| `DS/AlertBanner` | `danger` | Erro de autenticação |
| `DS/AlertBanner` | `warning` | Rate limit |

---

## 5. Contrato de API

**Request:**
```http
POST /auth/login
Content-Type: application/json

{
  "identificador": "ana.silva@ufpr.br | grr20201234 | ana@gmail.com",
  "senha": "string"
}
```

**Response (200 OK):** JSON **sem** tokens. Cookies HttpOnly: `access_token` (Path=/) e `refresh_token` (Path=/auth). Sessão Redis `auth:session:<sid>`.
```json
{
  "mustChangePassword": false,
  "mustAcceptLgpd": false
}
```

```http
Set-Cookie: access_token=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/
Set-Cookie: refresh_token=<opaque>; HttpOnly; Secure; SameSite=Lax; Path=/auth
```

**Relacionado — `POST /auth/ott`:** body `{ "token": "<jwt>" }`. 200 = mesmo JSON + mesmos cookies. Replay → 401.

**Response (401 — erro genérico):**
```json
{
  "type": "https://api.secretaria.ufpr.br/errors/unauthorized",
  "title": "Credenciais inválidas",
  "status": 401,
  "detail": "Credenciais inválidas. Verifique seus dados e tente novamente."
}
```

**Response (429 — rate limit):**
```json
{
  "type": "https://api.secretaria.ufpr.br/errors/too-many-requests",
  "title": "Muitas tentativas",
  "status": 429,
  "detail": "Aguarde antes de tentar novamente.",
  "retryAfterSeconds": 60
}
```

---

## 6. Fora de escopo desta história

- SSO institucional UFPR (SAML/OIDC) — futuro, via feature flag
- Login biométrico no mobile — fase posterior
- Lembrar dispositivo / "manter conectado" — não previsto no MVP
- Fluxo de primeiro acesso (`/primeiro-acesso`) — coberto em **US-F1-002**

---

## 7. Definição de Pronto (DoD)

- [ ] Frames Figma aprovados: Default, Error, Loading (desktop + mobile)
- [ ] `LoginUseCase` implementado com cobertura de testes ≥ 85% (módulo `iam`)
- [ ] Teste de integração: login sucesso, login falha, rate limit, mustChangePassword
- [ ] Contrato OpenAPI de `POST /auth/login` publicado antes do desenvolvimento frontend
- [ ] Mensagens de erro validadas como RFC 7807 Problem Details
- [ ] Auditoria: evento `iam.login_success` / `iam.login_failed` gravado em `audit_log`
- [ ] Acessibilidade: tab order, aria-live, contraste 4.5:1 verificados
- [ ] Nenhum hardcode de cor/espaçamento (apenas tokens `color/*`, `space/*`)

---

## 8. Referências

| Recurso | Link / Caminho |
|---------|---------------|
| Spec de tela | `telasFigma/telas0/F0.1-login.md` |
| Fluxo de autenticação | `foundationDocs/analysis/fluxos_por_perfil.md` §1 F0.1 |
| Mapa de rotas | `foundationDocs/analysis/telas.md` §2 F0.1 |
| Análise arquitetural (segurança) | `foundationDocs/analysis/analise_arquitetural_secretariaonline2.md` §8 |
| As-built backend | `foundationDocs/analysis/as-built-backend.md` |
| Página Figma F0 | [Telas / F0 — Público](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=18-152) |
