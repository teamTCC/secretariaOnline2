# Log Fatia 0 — Fundação HTTP (SPA harness)

**Quando:** 2026-08-31 (reinício + aceite no browser)  
**Contrato:** `FatiasFrontend/01-fatia-0-fundacao-http.md`  
**Oráculo:** `httpie/F0-publico/T-F0-001-login.md` · `GET /auth/csrf`  
**SPA:** `http://localhost:5173` (`frontend-web/`, Vite 6.4.3)  
**API:** `http://localhost:8080` (Spring Boot, profile `dev`)  
**Dumps crus:** `logs/raw/fatia-0/`

**Resultado:** aceite da Fatia 0 **passou**. Kernel HTTP fala com o Spring do mesmo jeito que o HTTPie (cookies + CORS + Problem+JSON). Sem JWT em `localStorage`. Sem axios/zod/shadcn.

---

## 0) Reinício

Parados os processos anteriores em `:8080` (Java/Gradle `bootRun`) e `:5173`/`:5174` (Vite).  
Mantidos: Postgres `:5432`, Redis/MinIO/Mailpit (Docker).

Subidos de novo:

| Serviço | Como | Prova |
|---------|------|--------|
| Spring Boot | `gradlew :app:bootRun` + JWT de `backend/.env.local` | log: `Started SecretariaOnlineApplicationKt` · `GET /actuator/health` → 200 `{"status":"UP"}` |
| Vite | `npm run dev` em `frontend-web/` | `Local: http://localhost:5173/` · porta **5173** (sem proxy `/auth`) |

`vite.config.ts` só define `server.port = 5173`. Sem `proxy` — o cookie `refresh_token` Path=`/auth` não é quebrado.

---

## 1) O que foi enviado / observado

### 1.1 `GET /actuator/health` (sanity)

Ver `logs/raw/fatia-0/health.txt`.

```
GET /actuator/health HTTP/1.1
Host: localhost:8080
```

**Resposta:** `200` · `{"status":"UP","groups":["liveness","readiness"]}`

**Por quê:** distinguir API fora de contrato errado.

### 1.2 `GET /auth/csrf` — HTTPie (oráculo)

Ver `logs/raw/fatia-0/csrf-direct.txt`.

```
GET /auth/csrf HTTP/1.1
Host: localhost:8080
```

**Resposta 200:**

```
Content-Type: application/json
Set-Cookie: XSRF-TOKEN=bc100869-ac3f-4e3b-9c97-dff34c1bea0f; Max-Age=43200; Path=/
```

```json
{
  "token": "bc100869-ac3f-4e3b-9c97-dff34c1bea0f",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf"
}
```

Cookie **não** HttpOnly (SPA lê `document.cookie`). Token do body = valor do cookie.

### 1.3 Bootstrap + fumaça no browser (`/health-front`)

`main.tsx` chama `api('/auth/csrf')` **antes** de montar o router.  
Rota `/health-front`: botão `GET /auth/csrf` + `JsonPanel`.

Após reload em `http://localhost:5173/health-front` e clique no botão:

| Checagem | Resultado |
|----------|-----------|
| Heading | `health-front` |
| `JsonPanel` | `token` + `headerName: "X-XSRF-TOKEN"` + `parameterName: "_csrf"` |
| `document.cookie` | `XSRF-TOKEN=<uuid>` **igual** ao JSON |
| `localStorage` | `{}` |
| `sessionStorage` | `{}` |
| JWT em storage | **não** |

JSON visto na UI (cookie jar do Chrome em `localhost`):

```json
{
  "token": "8f6aeb40-d12d-484c-91b3-16899d32d3ff",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf"
}
```

### 1.4 `fetch` com `credentials: 'include'` (prova de runtime)

Monkey-patch de `window.fetch` na SPA, depois `api('/auth/csrf')`, `api('/me')`, `api('/publico/contato')`. Dump: `logs/raw/fatia-0/browser-harness.json`.

| URL | credentials | Authorization | X-XSRF-TOKEN |
|-----|-------------|---------------|--------------|
| `GET http://localhost:8080/auth/csrf` | **include** | ausente | ausente (GET) |
| `GET http://localhost:8080/me` | **include** | ausente | ausente (GET) |
| `POST http://localhost:8080/publico/contato` | **include** | ausente | uuid do cookie |

**Por quê:** CORS 5173→8080 só grava/lê cookie com `credentials: 'include'`. Sem Bearer — o filtro JWT usa o cookie.

### 1.5 CORS preflight (origem da SPA)

Ver `logs/raw/fatia-0/cors-preflight.txt`.

```
OPTIONS /auth/csrf
Origin: http://localhost:5173
Access-Control-Request-Method: GET
Access-Control-Request-Headers: X-XSRF-TOKEN
```

**Resposta 200:**

```
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Credentials: true
Access-Control-Allow-Headers: X-XSRF-TOKEN
Access-Control-Expose-Headers: Set-Cookie
```

### 1.6 RFC 7807 — `GET /me` anônimo

HTTPie: `logs/raw/fatia-0/me-anon-401.txt`.  
Mesmo path via `api('/me', { skipRefresh: true })` na SPA.

```
GET /me HTTP/1.1
Host: localhost:8080
```

**Resposta 401:**

```
Content-Type: application/problem+json;charset=UTF-8
```

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/unauthorized",
  "title": "Não autenticado",
  "status": 401,
  "detail": "Token JWT inválido ou expirado.",
  "timestamp": "2026-08-31T12:22:16.019803900-03:00"
}
```

O client **throw** desse objeto. `isProblem(meProblem) === true`.  
`isProblem('application/problem+json') === true` (aceite: reconhece o content-type).  
`isProblemContentType('application/problem+json;charset=UTF-8') === true`.

Refresh **não** foi disparado (`skipRefresh: true` no teste; path `/me` com cookie ausente → 401 esperado).

### 1.7 CSRF no header (mutação, extra)

`POST /publico/contato` com `X-XSRF-TOKEN` ecoando o cookie → **400** `validation-error` (`assunto` blank). **Não** 403 CSRF.

Isto prova Double Submit: header = cookie. O 400 é contrato do DTO (campo extra da fatia 1); fora do checklist da fatia 0.

### 1.8 `normalizeLinks` / `useActions` / `actionFromRel`

Executado no browser (módulos Vite `/src/shared/api/hateoas.ts`):

| Entrada | Saída |
|---------|--------|
| mapa as-built `{ self: '/requests/1', assign: '…/transitions' }` | strings |
| HAL objeto `{ self: { href } }` | strings |
| HAL array `[{ rel, href }]` | strings |
| `actionFromRel('forward-to-deliberator')` | `FORWARD_TO_DELIBERATOR` |
| `useActions({ self, events, attachments, public, assign })` | `actionRels = ['assign']` |

### 1.9 Dependências

```
npm ls axios zod shadcn  → (empty)
```

Instalado: `react@18.3.1`, `react-dom@18.3.1`, `react-router-dom@6.30.6`, `@tanstack/react-query@5.102.8`, `vite@6.4.3`, `typescript@5.7.3`.

`grep` em `frontend-web/src`: zero `Authorization` / `Bearer` / `proxy`.

---

## 2) Checklist de aceite (`01-fatia-0` §7)

| Item | Status | Evidência |
|------|--------|-----------|
| Vite em `http://localhost:5173` | **PASS** | terminal Vite + URL da SPA |
| `GET /auth/csrf` 200 + cookie `XSRF-TOKEN` | **PASS** | HTTPie Set-Cookie + `document.cookie` + JsonPanel |
| `fetch` usa `credentials: include` | **PASS** | 3/3 fetches capturados |
| `isProblem` reconhece `application/problem+json` | **PASS** | string CT + objeto 401 `/me` |
| `normalizeLinks` mapa string + HAL | **PASS** | mapa, `{href}`, array `[{rel,href}]` |
| Zero JWT em localStorage | **PASS** | `localStorage {}` |
| `npm ls` sem axios / zod / shadcn | **PASS** | empty |

**Não fazer (também conferido):**

- Sem interceptor Bearer lendo cookie.
- Sem proxy Vite em `/auth`.
- `client.ts` sem comentários longos (uma linha no retry 403).

---

## 3) Conclusão

A Fatia 0 está **bem implementada** contra a API real. O browser:

1. Sobe CSRF no boot.
2. Mostra o JSON do Spring na tela feia.
3. Guarda só o cookie `XSRF-TOKEN` (não HttpOnly).
4. Transforma 401 `problem+json` em `Problem` para o `ProblemBanner`.
5. Normaliza `_links` no formato as-built 2026-08.

**Fora desta fatia (próxima):** login, forgot/reset, contato UI, `/erro`, protocolo, certificado, OTT (`02-fatia-1-publico-f0.md`).
