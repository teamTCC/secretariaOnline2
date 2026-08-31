# Fatia 0 — Fundação HTTP (client, CSRF, Problem, HATEOAS)

**Objetivo da demo:** o browser fala com o Spring **do mesmo jeito** que o HTTPie. Sem esta fatia, nenhuma outra funciona.

**Pré-requisito:** pasta vazia `frontend-web/src/`. Ler `FatiasFrontend/00-contexto-geral.md`.  
**Não implementar:** telas de login, dashboard, wizard. Só o kernel.

**Oráculo:** `httpie/F0-publico/T-F0-001-login.md` · `foundationDocs/analysis/as-built-backend.md` §2.

---

## 1. Scaffold Vite (uma vez)

```
frontend-web/
  package.json          # react, react-dom, react-router-dom, @tanstack/react-query
  vite.config.ts        # server.port = 5173
  tsconfig.json         # strict
  index.html
  .env.local            # VITE_API_BASE_URL=http://localhost:8080
  src/index.css         # ~40 linhas: body, pre.json, button, .danger, .row
  src/app/main.tsx
  src/app/providers.tsx
  src/app/router.tsx    # só rota /health ou placeholder
```

`vite.config.ts`: **não** proxy `/auth` a menos que CORS falhe. CORS já inclui `http://localhost:5173`. Preferir chamadas absolutas a `VITE_API_BASE_URL`.

---

## 2. Whitelist de arquivos (só estes)

| Arquivo | Responsabilidade |
|---------|------------------|
| `src/shared/api/problem.ts` | `type Problem = { type, title, status, detail, instance, timestamp, incidentId }` + `isProblem(x)` |
| `src/shared/api/client.ts` | `api<T>(path, init)` — cookies, CSRF, refresh 1×, parse problem |
| `src/shared/api/hateoas.ts` | `hrefOf`, `actionFromRel`, `useActions`, `normalizeLinks` |
| `src/shared/api/queryKeys.ts` | factory mínima: `me`, `csrf`, `dashboard(perfil)` |
| `src/shared/ui/JsonPanel.tsx` | `<pre className="json">` |
| `src/shared/ui/ProblemBanner.tsx` | mostra `title` + `detail` + `incidentId` |
| `src/shared/ui/HateoasBar.tsx` | botões = rels filtrados |
| `src/shared/ui/Page.tsx` | `h1` + children |
| `src/app/main.tsx` | bootstrap CSRF + `QueryClientProvider` + `RouterProvider` |

**Proibido nesta fatia:** axios, Zod, RHF, shadcn, `tokenStorage.ts` com JWT, `AuthProvider` gordo.

---

## 3. `client.ts` — contrato as-built

```
BASE = import.meta.env.VITE_API_BASE_URL  // http://localhost:8080

api(path, { method, body, headers, skipRefresh? }):
  1. credentials: 'include' SEMPRE
  2. Content-Type application/json se body object
  3. Se método não é GET/HEAD e path NÃO está na lista isenta:
       header X-XSRF-TOKEN = cookie XSRF-TOKEN  (document.cookie)
     Se cookie vazio: GET /auth/csrf primeiro, depois retry
  4. fetch
  5. Se 401 e !skipRefresh e path !== /auth/login e path !== /auth/refresh:
       POST /auth/refresh (skipRefresh=true, CSRF isento)
       se 200 → retry original uma vez
       senão → throw + caller navega /login
  6. Se Content-Type contém problem+json → throw Problem
  7. 204 → return undefined
  8. senão JSON
```

**Isentos de CSRF** (as-built `SecurityConfig`):

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/ott`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`

**NÃO isentos:** `POST /auth/logout`, `POST /auth/first-access`, `POST /publico/contato`, qualquer `/requests*`.

Ler cookie `XSRF-TOKEN`: o cookie **não** é HttpOnly — `document.cookie` funciona. Não usar `decodeURIComponent` errado no UUID.

Após 403 cujo `type` contém `forbidden` e detalhe CSRF: `GET /auth/csrf` e retry **uma** vez.

---

## 4. `hateoas.ts` — o back é strings; normalize o resto

As-built 2026-08: `_links: Record<string, string>`.

Alguns DTOs antigos na doc HTTPie ainda mostram HAL (`[{rel,href}]` ou `{ rel: { href } }`). **Normalizar uma vez:**

```ts
export function normalizeLinks(raw: unknown): Record<string, string> {
  if (!raw) return {}
  if (Array.isArray(raw)) {
    return Object.fromEntries(raw.map((x: { rel: string; href: string }) => [x.rel, x.href]))
  }
  const out: Record<string, string> = {}
  for (const [k, v] of Object.entries(raw as object)) {
    out[k] = typeof v === 'string' ? v : (v as { href?: string })?.href ?? ''
  }
  return out
}

export const actionFromRel = (rel: string) => rel.replace(/-/g, '_').toUpperCase()
// "forward-to-deliberator" → "FORWARD_TO_DELIBERATOR"

export function hrefOf(links: Record<string, string> | undefined, rel: string) {
  return links?.[rel]
}

export function useActions(links: Record<string, string> | undefined) {
  const n = links ?? {}
  const has = (rel: string) => Boolean(n[rel])
  const href = (rel: string) => n[rel]
  const actionRels = Object.keys(n).filter(
    (r) => !['self', 'events', 'attachments', 'public'].includes(r),
  )
  return { has, href, actionRels, all: n }
}
```

Pendências do BFF: campo **`_link`** (singular, string). Não é `_links`.

`HateoasBar`: para cada `actionRels`, botão `{rel}` que chama `onAction(rel, href)`. Não traduzir labels nesta versão (o time precisa ver o `rel` cru).

---

## 5. Bootstrap CSRF

Em `main.tsx`, **antes** de renderizar rotas autenticadas:

```
await api('/auth/csrf')  // 200 { token, headerName: "X-XSRF-TOKEN" } + Set-Cookie
```

Não guardar o JWT. Guardar no máximo `sessionStorage` flags `mustChangePassword` / `mustAcceptLgpd` **depois** do login (fatia 1).

---

## 6. Página de fumaça (opcional, 20 linhas)

Rota `/health-front`: botão “GET /auth/csrf” + `JsonPanel` da resposta. Serve para a equipe ver CORS/cookie sem login.

---

## 7. Aceite

- [ ] Vite em `http://localhost:5173`
- [ ] `GET /auth/csrf` no Network: 200, cookie `XSRF-TOKEN` no Application
- [ ] `fetch` usa `credentials: include`
- [ ] Função `isProblem` reconhece `application/problem+json`
- [ ] `normalizeLinks` cobre string map (caso real) e HAL (defensivo)
- [ ] Zero JWT em localStorage
- [ ] `npm ls` não tem axios / zod / shadcn

## 8. Não fazer

- Interceptor que manda `Authorization: Bearer` lendo cookie (o browser já manda o cookie).
- Proxy Vite que quebra `Path=/auth` do `refresh_token` (se usar proxy, `/auth` tem que ir inteiro).
- Comentários longos no `client.ts`.
