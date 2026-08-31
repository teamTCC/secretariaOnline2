# Log Fatia 1 — F0 público (SPA harness)

**Quando:** 2026-08-31 (auditoria caixa-branca + caixa-preta no browser, pós-correção)  
**Contrato:** `FatiasFrontend/02-fatia-1-publico-f0.md`  
**Oráculos:** `httpie/F0-publico/T-F0-00*.md` · as-built `AuthController` / `UsuarioPersistenceAdapter`  
**SPA:** `http://localhost:5173` (`frontend-web/`)  
**API:** `http://localhost:8080` (Spring Boot, profile `dev`)  
**Dumps crus:** `logs/raw/fatia-1/`

**Resultado:** aceite da Fatia 1 **passou após correção de 5 bugs**. SPA fala login/cookies/CSRF/RFC 7807 contra a API real. Sem JWT em `localStorage`. Sem axios/zod/shadcn.

---

## 0) Escopo da auditoria

| Tipo | O que foi feito |
|------|-----------------|
| Caixa-branca | Leitura de `LoginPage`, `ForgotPage`, `ResetPage`, `ContatoPage`, `OttPage`, `MeRawPage`, `ProtocoloPage`, `CertificadoPage`, `ErroPage`, `session.ts`, `client.ts` (`skipCsrf`), `router.tsx` vs spec + as-built Kotlin |
| Caixa-preta HTTP | `fetch` na origem `5173` → `8080` com `credentials: include` (mesmo CORS da SPA) |
| Caixa-preta UI | Clique/navegação no harness: login demo, GRR, 401, 429, forgot, OTT, `/erro`, certificado 404, troca de perfil |

Não reiniciamos JVM/Vite nesta sessão (já estavam no ar da fatia 0).

---

## 1) Bugs encontrados (caixa-branca) e correção

### B1 — OTT quebrava no React 18 StrictMode / não re- tentava após 401

`OttPage` usava `useMutation` + `Set` módulo. StrictMode: mount → POST → unmount (observer some) → remount com token já no Set → **não POSTA de novo e não redireciona**. Após 401, o Set bloqueava retry pelo querystring.

**Fix:** cache de `Promise` por token (`exchangeOtt`). Erro remove a entrada (retry permitido). Sucesso reusa a mesma promise (StrictMode não duplica POST).

### B2 — Demo “Aluno GRR” `20210001` → 401 no as-built

Spec/HTTPie citam `identificador: "20210001"`. O back só trata GRR se a string **começa com `GRR`**:

```kotlin
// UsuarioPersistenceAdapter.findByIdentificador
if (trimmed.startsWith("GRR", ignoreCase = true)) findByGrr(...)
else findByEmail(...)
```

Prova HTTP: `20210001` → **401** genérico; `GRR20210001` → **200** + `GET /me` de Ana Aluno.

**Fix:** select DEV usa `GRR20210001`. O oráculo HTTPie T-F0-001 passo 2 (variante GRR sem prefixo) está **stale** em relação ao as-built.

### B3 — Cache TanStack de `GET /me` entre perfis

Login admin → `/me-raw` mostra admin. Login aluno em seguida podia **flash/mostrar o JSON do admin** (`queryKey: ['me']` sem invalidar).

**Fix:** `queryClient.removeQueries({ queryKey: queryKeys.me })` no sucesso de login e OTT.

Prova UI: admin → `/me-raw` (`admin@ufpr.br`); depois GRR → `/me` com `ana.aluno@ufpr.br` e **sem** `admin@ufpr.br`.

### B4 — Forgot: texto “se existir…” após erro seguinte

`{m.data ? …}` — no TanStack Query v5 o `data` da última mutação **bem-sucedida permanece** depois de um erro.

**Fix:** `{m.isSuccess ? …}`.

Prova UI: 202 mostra a frase; request seguinte 429 **não** mantém a frase (só `ProblemBanner`).

### B5 — Inputs de reset/protocolo/certificado dessincronizados da URL

`useState(param)` só inicializa no primeiro mount. Navegar `/nova-senha?token=` ou `/publico/verificar-certificado/:hash` na mesma instância deixava o campo velho.

**Fix:** `useEffect` copia `token` / `ano`+`numero` / `hash` da URL.

Prova UI: `/nova-senha?token=abc.jwt.from.mail` preenche o textarea; `/publico/verificar-certificado/deadbeef` preenche o hash.

---

## 2) Caixa-preta HTTP (origem SPA)

Dump: `logs/raw/fatia-1/http-battery.json`.

### 2.1 Login

| Caso | Status | Observação |
|------|--------|------------|
| Body `{}` | **400** `validation-error` | `errors[]` identificador + senha |
| `naoexiste@ufpr.br` | **401** | detail genérico — anti-enumeração |
| GRR `20210001` | **401** | as-built (B2) |
| GRR `GRR20210001` | **200** | flags; `GET /me` Ana Aluno |
| E-mail aluno | **200** | body **sem** `accessToken`/`refreshToken` |
| Admin / Prof / Sec / Coord / Egresso | **200** | `GET /me` com e-mail e `roles` certos |
| 6× login falho `audit.fatia1.429@ufpr.br` | 5×401 + **429** | `retryAfterSeconds: 59` no JSON |

Cookies HttpOnly não aparecem em `document.cookie` (só `XSRF-TOKEN`). Prova de sessão: `GET /me` **200** sem `Authorization`.

### 2.2 Cliente `api()` (módulo Vite)

Monkey-patch de `window.fetch` enquanto se chama `api()`:

| Chamada | credentials | Authorization | X-XSRF-TOKEN |
|---------|-------------|---------------|--------------|
| `POST /auth/login` | **include** | ausente | ausente (isento) |
| `POST /publico/contato` `skipCsrf` | include | ausente | ausente → **403** |
| `POST /publico/contato` | include | ausente | uuid do cookie → **202 ACEITO** |

### 2.3 Forgot / reset / OTT

| Caso | Status |
|------|--------|
| Forgot fake + real | **202** body **idêntico** |
| Forgot e-mail vazio | **400** |
| Reset token lixo | **401** “Token de redefinição… inválido ou expirado” |
| `POST /auth/ott` token lixo | **401** |

422 `weak-password` / `password-reuse` **não** foram disparados: exigem JWT de reset válido no outbox (Mailpit). A UI já mostra `ProblemBanner`+`JsonPanel` para qualquer Problem (provado com 401/429).

### 2.4 Contato / público

| Caso | Status |
|------|--------|
| `GET /publico/contato` | **200** `_links.enviar` |
| POST sem header CSRF (cookie ainda vai) | **403** `forbidden` |
| POST com `X-XSRF-TOKEN` = cookie | **202** `ACEITO` |
| `GET /.well-known/jwks.json` | **200** RSA + OKP Ed25519 |
| `GET /publico/solicitacoes/2026/99999` | **404** |
| `GET /publico/verificar-certificado/deadbeef` | **404** |

Header HTTP `Retry-After` **não** é lido pelo `fetch` do browser (`Access-Control-Expose-Headers` não o lista). O body já traz `retryAfterSeconds` — a `ProblemBanner` usa isso. Não é bug do front.

---

## 3) Caixa-preta UI (browser)

| Fluxo | Resultado |
|-------|-----------|
| `/` → `/login` | PASS |
| Login senha errada | `ProblemBanner` 401 genérico + `JsonPanel` do Problem |
| Demo Admin → entrar | `/me-raw`, flags `{false,false}`, JSON `admin@ufpr.br` |
| Demo Aluno GRR (`GRR20210001`) | `/me` (LGPD true), JSON aluno, **sem** JSON admin (B3) |
| `localStorage` | `{}` |
| `sessionStorage` | só `so2.session.flags` |
| Forgot 202 | “se existir, enviaremos link” |
| Forgot seguinte (bucket 3/h) | **429** “tente em 1883s” — frase de sucesso **sumiu** (B4) |
| `/nova-senha?token=` | textarea preenchido (B5) |
| `/auth/ott?token=not-a-jwt` | POST no mount, 401 na banner; **retry** pelo botão também 401 (B1) |
| `/erro` | `incidentId: (nenhum)` |
| `/erro/INC-2026-ab12` | mostra o id (sessão anterior) |
| Certificado `deadbeef` | 404 na banner; campo hash = param (B5) |
| Contato sem/com CSRF | 403 / 202 (sessão de implementação + reconfirmação HTTP) |

---

## 4) Checklist de aceite (`02-fatia-1` §8)

| Item | Status | Evidência |
|------|--------|-----------|
| Login aluno: cookies access+refresh; body sem JWT | **PASS** | `GET /me` 200; body só flags; `document.cookie` sem JWT |
| `GET /me` com cookie, 200 | **PASS** | HTTP + UI `/me` / `/me-raw` |
| Login senha errada: 401 genérico na banner | **PASS** | UI + HTTP |
| Forgot 202 fake e real | **PASS** | HTTP bodies iguais |
| Contato sem CSRF 403; com CSRF 202 | **PASS** | `api()` captures + HTTP |
| JWKS sem login | **PASS** | RSA + Ed25519 |

**Não fazer (conferido):** sem Tailwind/logo; sem AuthGuard; sem logout.

---

## 5) Fora desta fatia / residual

- Happy-path **422** reset (senha fraca / reuso) e OTT **200** (deep-link real) — token no outbox; fatia 3/transversal.
- Protocolo/certificado **200** válidos — dados nas fatias 3–4.
- Logout + guard `/primeiro-acesso` — fatia 2.
- Oráculo HTTPie GRR `20210001` diverge do as-built (`GRR…`).

---

## 6) Conclusão

Depois dos cinco fixes, a Fatia 1 está **coerente com o Spring as-built** e o aceite da spec. O harness permite à equipe:

1. Entrar com cada perfil demo (GRR com prefixo).
2. Ver 401 genérico, 429 com “tente em Ns”, 403/202 de CSRF, JWKS público.
3. Guardar **só** flags em `sessionStorage`.
4. Trocar de usuário sem JSON velho de `/me`.
5. Abrir OTT/`?token=` sem ficar preso no StrictMode.
