# Segurança XSS e CSRF — lacunas e plano de endurecimento

**Status:** backlog / evolução pós-MVP harness  
**Data:** 2026-08-31  
**Público:** equipe backend + frontend + TCC  
**Régua:** `foundationDocs/requisitos/02-requisitos-nao-funcionais.md` (RNF-SEC-06), `RELATORIO_AUDITORIA_BACKEND.md` §3.1, `FatiasFrontend/00-contexto-geral.md` §5

Este documento descreve **o que já está correto**, **o que falta** e **o que implementar** para fortalecer defesas contra **XSS** (Cross-Site Scripting) e **CSRF** (Cross-Site Request Forgery). Não substitui o as-built: em conflito, código + HTTPie vencem.

---

## 1. Resumo executivo

| Ameaça | Situação atual | Prioridade de melhoria |
|--------|----------------|------------------------|
| **CSRF** | **Bem coberto** — Double Submit Cookie + CORS + SameSite=Lax + lista de isentos documentada | Baixa/média (hardening e observabilidade) |
| **XSS** | **Parcial** — HttpOnly nos JWTs, React sem `innerHTML`, validação Jakarta; **sem CSP** nem política formal de sanitização em conteúdo rico | **Alta** antes do frontend de produção |

**Mensagem-chave:** CSRF e XSS são ameaças **diferentes**. O `XSRF-TOKEN` legível **não** é falha de XSS — é requisito do Double Submit. XSS na mesma origem **contorna** CSRF; a defesa XSS é outra camada (escape, CSP, higiene de código).

---

## 2. Estado atual (baseline 2026-08)

### 2.1 CSRF — implementado

| Peça | Onde | Notas |
|------|------|-------|
| Double Submit | `SecurityConfig.kt`, `SpaCsrfTokenRequestHandler.kt`, `CsrfCookieFilter.kt` | Cookie `XSRF-TOKEN` (não HttpOnly) + header `X-XSRF-TOKEN` |
| Bootstrap SPA | `GET /auth/csrf`, `frontend-web/src/shared/api/client.ts` | `credentials: 'include'`; retry após 403 em mutação |
| Isentos | login, refresh, ott, forgot-password, reset-password | As-built; **não** isentar logout, first-access, `/publico/contato`, `/requests*` |
| CORS | `corsConfigSource()` | Origens explícitas; `allowCredentials: true`; header `X-XSRF-TOKEN` permitido |
| Prova caixa-preta | `logs/log fatia-0 fundacao-http.md`, `logs/log fatia-1 publico-f0.md` | Contato sem CSRF → 403; com CSRF → 202/4xx de negócio |

### 2.2 XSS — implementado (parcial)

| Peça | Onde | Efeito |
|------|------|--------|
| JWT em cookie HttpOnly | `AuthController`, cookies `access_token` / `refresh_token` | XSS **não lê** JWT via `document.cookie` |
| Sem JWT em `localStorage` | regra em `00-contexto-geral.md`, harness atual | Reduz exfiltração persistente do token |
| React escape padrão | `ProblemBanner`, formulários fatia 0–1 | `{variável}` não vira HTML executável |
| Sem `dangerouslySetInnerHTML` | `frontend-web/src/**` (harness) | Superfície de reflexão HTML mínima |
| `JsonPanel` com `JSON.stringify` | `shared/ui/JsonPanel.tsx` | JSON malicioso exibido como texto |
| Validação de entrada | `@Valid` em DTOs | Reduz lixo; **não** substitui anti-XSS em HTML |
| `escapeHtml()` em e-mails | outbox (ex.: `ContatoOutboxHandler`) | Protege **corpo de e-mail**, não a SPA |
| Headers parciais | `SecurityConfig` headers | `X-Frame-Options`, `X-Content-Type-Options`, HSTS |

### 2.3 Lacunas conhecidas

| Lacuna | RNF / doc | Impacto |
|--------|-----------|---------|
| **Content-Security-Policy (CSP)** | RNF-SEC-06 P0 | Principal gap anti-XSS no browser |
| **Referrer-Policy**, **Permissions-Policy** | RNF-SEC-06 | Headers documentados, não no `SecurityConfig` |
| Política de sanitização HTML em conteúdo de usuário | implícito em comunicados/FAQ | Stored XSS quando UI renderizar HTML |
| Scan OWASP ZAP / Observatory no CI | RNF-SEC-06 | Sem gate automatizado |
| ESLint `no-dangerously-set-inner-html` | boa prática frontend prod | Prevenção em PR |
| Problem 403 CSRF indistinguível de FGAC 403 | UX + testes | `detail` genérico no `accessDeniedHandler` |

---

## 3. CSRF — o que melhorar (hardening)

O núcleo CSRF **não precisa ser redesenhado**. Itens abaixo são endurecimento, observabilidade e evolução.

### 3.1 P1 — Manter lista de isentos auditável

**Problema:** qualquer novo `POST` público ou webhook adicionado sem revisão de segurança pode ficar isento por engano.

**Ação:**

- Checklist em PR: “mutação nova está na whitelist CSRF ou é isenta documentada?”
- Teste de integração (Spring MockMvc ou Testcontainers): `POST /publico/contato` sem header → **403**; com header+cookie → não 403 por CSRF.
- Tabela viva em `httpie/F0-publico/T-F0-001-login.md` espelhando `ignoringRequestMatchers` do `SecurityConfig`.

**Aceite:** diff em `SecurityConfig` que altere isentos exige atualização do tutorial HTTPie + um teste.

### 3.2 P2 — Resposta RFC 7807 específica para falha CSRF

**Problema:** hoje `accessDeniedHandler` devolve 403 genérico (“Acesso negado”). Falha CSRF e falha FGAC parecem iguais — dificulta debug e testes E2E.

**Ação:**

- Custom `AccessDeniedHandler` ou filtro pós-`CsrfFilter` que detecte rejeição CSRF e devolva:
  - `type`: `https://secretariaonline.ufpr.br/errors/csrf`
  - `status`: 403
  - `detail`: mensagem neutra (sem vazar estado interno)
- Front: `client.ts` já re-bootstrap `GET /auth/csrf` em 403 de mutação; opcionalmente reconhecer `type` `csrf` para mensagem na UI.

**Aceite:** HTTPie documenta body Problem distinto para POST sem `X-XSRF-TOKEN`.

### 3.3 P2 — Validação `Origin` / `Referer` (defesa em profundidade)

**Problema:** Double Submit cobre o caso clássico; alguns proxies antigos ou bugs de configuração CORS raros beneficiam-se de checagem extra.

**Ação:**

- Filtro ou `CsrfTokenRequestHandler` estendido: em mutações, exigir `Origin` ou `Referer` ∈ lista `app.cors.allowed-origins` (mesma config do CORS).
- **Não** substituir Double Submit — apenas camada adicional.

**Aceite:** request de `Origin: https://evil.com` com CSRF válido → 403; SPA em `localhost:5173` → OK.

### 3.4 P3 — Rotação de token CSRF no login

**Problema:** token CSRF pode ser emitido antes do login e reutilizado após — geralmente aceitável, mas alguns padrões rotacionam no login.

**Ação:**

- Após `POST /auth/login` 200, invalidar cookie CSRF anterior e forçar novo `GET /auth/csrf` (ou Set-Cookie novo no response do login).
- SPA: após login, chamar `api('/auth/csrf')` antes da primeira mutação.

**Aceite:** cookie `XSRF-TOKEN` após login ≠ valor pré-login.

### 3.5 P3 — Mobile (Expo) e CSRF

**Problema:** app nativo não usa cookie jar do Chrome; Bearer + CSRF precisam de contrato explícito (`00-contexto-geral.md` já alerta).

**Ação:**

- Documentar fluxo: `GET /auth/csrf` → guardar token em memória/SecureStore → enviar `X-XSRF-TOKEN` + `Authorization: Bearer` em mutações.
- **Não** desabilitar CSRF para mobile; não criar bypass por `User-Agent`.

**Aceite:** tutorial `mobile/` com mesmo oráculo do contato público (403 sem header).

### 3.6 O que **não** fazer em CSRF

- Tornar `XSRF-TOKEN` HttpOnly (quebra SPA).
- Isentar `POST /auth/logout`, `POST /auth/first-access`, `POST /publico/contato`.
- Proxy Vite em `/auth` (quebra `Path=/auth` do `refresh_token`).
- Confundir CSRF com proteção XSS.

---

## 4. XSS — o que implementar (prioridade alta)

### 4.1 P0 — Content-Security-Policy na SPA (produção)

**Problema:** RNF-SEC-06 exige CSP; API hoje não envia CSP (e **não deve** ser o único lugar — a SPA é servida pelo Vite/nginx).

**Ação:**

1. **Servidor estático** (nginx / Vercel / `index.html` meta apenas em dev):
   - Política inicial sugerida (ajustar após build):
     ```
     default-src 'self';
     script-src 'self';
     style-src 'self' 'unsafe-inline';
     img-src 'self' data: https:;
     connect-src 'self' https://api.secretariaonline.ufpr.br;
     frame-ancestors 'none';
     base-uri 'self';
     form-action 'self';
     ```
   - Evitar `unsafe-eval`. `unsafe-inline` em `style-src` pode ser necessário com Tailwind/shadcn — preferir nonce/hash quando migrar para produto visual.
2. **Vite prod:** injetar nonce por request via middleware nginx ou plugin Vite CSP.
3. **Report-Only** em staging antes de enforce.

**Aceite:** Mozilla Observatory ≥ A no host da SPA; zero violações em smoke login → dashboard.

**Arquivos prováveis:** `ops/nginx/` (novo), `frontend-web/index.html`, pipeline deploy.

### 4.2 P0 — Completar headers HTTP (API + SPA)

**Problema:** faltam `Referrer-Policy` e `Permissions-Policy` no `SecurityConfig`.

**Ação em `SecurityConfig.kt` (ou filtro global):

```kotlin
headers.referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
headers.permissionsPolicy { it.policy("geolocation=(), camera=(), microphone=()") }
```

Para a **SPA** (nginx): repetir headers + CSP.

**Aceite:** teste automatizado asserta presença dos 6 cabeçalhos do RNF-SEC-06 nas respostas da API; SPA com CSP no deploy.

### 4.3 P0 — Política de renderização de conteúdo de usuário

**Problema:** superfícies futuras armazenam texto livre que pode virar stored XSS se renderizado como HTML:

| Superfície | Módulo | Risco |
|------------|--------|-------|
| Comunicados / hub | `comunicacao` | corpo HTML ou markdown |
| FAQ (`pergunta`/`resposta`) | `iam` | texto administrativo |
| `request.dados` (JSONB) | `solicitacoes` | campos textarea no `DynamicForm` |
| Tickets de suporte | `support` | descrição livre |
| Mensagens de contato | `publico/contato` | já vai para e-mail com `escapeHtml` |

**Ação:**

1. **Contrato API:** campos de texto livre são **plain text** ou **markdown sanitizado no servidor** — nunca HTML cru confiável do cliente.
2. **Frontend produto:**
   - Padrão: renderizar como texto (`{value}`) ou markdown com pipeline seguro.
   - Se HTML for inevitável: **DOMPurify** (allowlist mínima) + teste com payload OWASP XSS.
   - **Proibir** `dangerouslySetInnerHTML` exceto atrás de wrapper `SafeHtml` auditado.
3. **Backend:** opcional sanitizar na gravação (defesa em profundidade) com biblioteca HTML allowlist — não só na saída.

**Aceite:** payload `<script>alert(1)</script>` em comunicado salvo → exibido escapado ou removido; nunca executado.

### 4.4 P1 — ESLint + revisão de PR (frontend)

**Ação:**

- `eslint-plugin-react` regra `react/no-danger` (já cobre `dangerouslySetInnerHTML`).
- Regra custom ou `no-restricted-syntax` bloqueando `innerHTML`, `document.write`, `eval`.
- CI: `npm run lint` obrigatório em `frontend-web/`.

**Aceite:** PR com `dangerouslySetInnerHTML` falha no CI sem exceção documentada.

### 4.5 P1 — Dependências e supply chain

**Ação:**

- Dependabot ou Renovate no repositório.
- OWASP Dependency-Check ou `npm audit` / Gradle dependency check no CI.
- Pin de versões em lockfiles (`package-lock.json`).

**Aceite:** CVE critical → bloqueio de merge até patch ou ADR de risco aceito.

### 4.6 P1 — Testes de segurança no CI

**Ação:**

- Job CI: OWASP ZAP baseline contra API + SPA em docker-compose.
- Mozilla Observatory ou `securityheaders.com` API no deploy preview.
- Casos manuais HTTPie viram testes Playwright mínimos: contato sem CSRF → 403.

**Aceite:** pipeline falha se CSP ausente em staging.

### 4.7 P2 — Cookies em produção

**Ação:**

- `COOKIE_SECURE=true` + HTTPS obrigatório.
- Manter `SameSite=Lax` (ou `Strict` se não houver fluxo cross-site legítimo).
- Documentar que `application-dev.yml` com `secure: false` é **somente localhost**.

**Aceite:** cookies de auth sem flag `Secure` em prod → falha de checklist de deploy.

### 4.8 P2 — Subresource Integrity (SRI)

**Ação:** se algum script externo for carregado (analytics, fonts CDN), usar `integrity=` + `crossorigin`.

**Aceite:** zero `<script src="https://...">` sem SRI no `index.html` de produção.

### 4.9 P3 — Trusted Types (opcional)

**Ação:** CSP `require-trusted-types-for 'script'` em fase avançada — exige adapters em libs que escrevem no DOM.

**Aceite:** só após CSP estável; não bloquear MVP.

---

## 5. Matriz de responsabilidade

| Item | Camada | Dono |
|------|--------|------|
| Double Submit CSRF | Backend + `client.ts` | iam + frontend |
| CORS / Origin | Backend | app/config |
| CSP | nginx / host SPA | devops + frontend |
| Referrer / Permissions | Backend (+ nginx SPA) | app/config |
| Sanitização conteúdo | Backend domain + frontend render | comunicacao, solicitacoes, frontend |
| HttpOnly JWT | Backend | iam |
| Lint anti-innerHTML | Frontend CI | frontend |
| ZAP / Observatory | CI | devops |

---

## 6. Ordem de implementação sugerida

```
Fase A (pré-produto visual) — 1 sprint
  ├─ CSP Report-Only na SPA (nginx)
  ├─ Referrer-Policy + Permissions-Policy no SecurityConfig
  ├─ ESLint no frontend-web
  └─ Teste integração CSRF contato + logout

Fase B (produto com comunicados / DynamicForm rico) — 1–2 sprints
  ├─ Política SafeHtml / DOMPurify
  ├─ CSP enforce (sem unsafe-eval)
  └─ ZAP baseline no CI

Fase C (endurecimento)
  ├─ Problem type csrf
  ├─ Origin check opcional
  ├─ Rotação CSRF no login
  └─ Mobile CSRF + Bearer doc
```

---

## 7. Checklist de “pronto para produção” (XSS + CSRF)

### CSRF

- [ ] Toda mutação não isenta falha sem `X-XSRF-TOKEN` (teste automatizado)
- [ ] Lista de isentos = exatamente login, refresh, ott, forgot, reset (+ swagger/actuator/jwks conforme ambiente)
- [ ] `POST /auth/logout` e `POST /publico/contato` exigem CSRF
- [ ] CORS: origens explícitas, sem `*` com credentials
- [ ] Documentação mobile alinhada

### XSS

- [ ] CSP enforce no host da SPA (sem `unsafe-eval`)
- [ ] Seis headers do RNF-SEC-06 nas respostas relevantes
- [ ] Zero `dangerouslySetInnerHTML` sem wrapper auditado
- [ ] Conteúdo de usuário (comunicados, FAQ, `dados`) com política plain-text ou sanitizada
- [ ] JWT só HttpOnly; zero token em `localStorage`
- [ ] Scan ZAP/Observatory no CI sem finding critical/high de XSS refletido

---

## 8. Referências internas

| Documento | Uso |
|---------|-----|
| `FatiasFrontend/00-contexto-geral.md` §5.1–5.2 | cookies, CSRF, o que não fazer |
| `FatiasFrontend/01-fatia-0-fundacao-http.md` | contrato `client.ts` |
| `logs/log fatia-0 fundacao-http.md` | evidência CSRF/cookies |
| `logs/log fatia-1 publico-f0.md` | contato com/sem CSRF |
| `foundationDocs/requisitos/02-requisitos-nao-funcionais.md` RNF-SEC-06 | headers obrigatórios |
| `RELATORIO_AUDITORIA_BACKEND.md` §3.1 | baseline auth/CSRF |
| `agents/security-engineer.md` | headers, FGAC, anti-patterns |
| OWASP [CSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html) | Double Submit |
| OWASP [XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html) | escape + CSP |

---

## 9. Nota para o TCC (texto sugerido)

> O sistema adota **Double Submit Cookie** para CSRF em SPA (cookie `XSRF-TOKEN` + header `X-XSRF-TOKEN`), complementado por CORS restritivo e cookies de autenticação HttpOnly. A proteção contra XSS baseia-se em **defesa em profundidade**: tokens de sessão inacessíveis a JavaScript, renderização escapada no React e validação de entrada no backend; a evolução para produção prevê **Content-Security-Policy**, sanitização de conteúdo rico e verificação automatizada (OWASP ZAP), conforme backlog em `future/seguranca-xss-csrf.md`.
