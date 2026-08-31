# 00 — Contexto geral (versão de testes do frontend)

**Ler isto em toda sessão** antes de qualquer fatia.  
**Produto desta pasta:** SPA de **inspeção de contrato**, não o frontend de produção.

O backend SecretariaOnline2 (Kotlin / Spring Boot 3 / PostgreSQL / Redis / MinIO) **já está implementado e foi exercitado com HTTPie**. Esta SPA existe para a equipe **ver no browser** que o back manda JSON certo, aplica regra de negócio, dirige o workflow engine e **só libera ações via `_links`**.

---

## 1. O que esta versão é / não é

**Funcionalidade = 100% das transações.** A equipe vai testar **caixa-preta no browser** tudo o que o HTTPie já exercitou (`httpie/` F0–F8 + transversais). Nenhum endpoint autenticado relevante fica “só no HTTPie”.

O que se economiza é **só UX/visual**, não cobertura.

| É (obrigatório) | Não é (proibido gastar token nisto) |
|-----------------|-------------------------------------|
| Harness caixa-preta de **todas** as transações F0–F8 + outbox/cert/FCM/Redis-BFF | App acadêmico polido, Figma, DashboardA |
| **Os 19 tipos** de solicitação abríveis/deliberáveis no browser | **19 arquivos** `SegundaChamadaPage.tsx` / `TrancamentoForm.tsx` |
| Um `DynamicForm` que lê `form_schema` (select, textarea, date, entity-select, tabela, anexos) | Formulário hardcoded por `tipoCode` |
| Draft, submit, anexos MinIO, on-behalf, bulk, todas as actions do seed | “Smoke só com DECLARACAO_MATRICULA” e pular o resto |
| JSON cru + `HateoasBar` + `ProblemBanner` em **cada** fluxo | shadcn, tokens, animação, KPI cards, layout responsivo caprichado |
| Web Vite 5173 | Expo nesta versão (mesmo contrato documentado) |

**Visual:** HTML nativo + um `index.css` curto. Feio de propósito. Se o botão existe e o POST sai, está pronto.

**Produção futura** (não usar agora): `frontend-web/docs/plano-entregas-frontend.md`, DashboardA, shadcn, NativeWind, Figma MCP.

Mapa transação → tela: [`09-cobertura-transacoes.md`](09-cobertura-transacoes.md).

---

## 2. Stack desta versão (mínimo que ainda casa com o back)

O back foi desenhado para React 18 + Vite 5173 + cookies + HATEOAS. Não trocar a stack.

| Peça | Escolha | Por quê (este projeto) | Evitar |
|------|---------|------------------------|--------|
| Runtime | **React 18** + **Vite 6** + **TypeScript 5 strict** | CORS já lista `http://localhost:5173`; TS alinha a OpenAPI | Next.js (o back já é BFF), CRA |
| Rotas | **React Router 6** (`createBrowserRouter`) | poucas rotas; lazy opcional | Next App Router |
| HTTP | **`fetch` nativo** + wrapper `api()` | `credentials: 'include'`; menos deps = menos tokens | axios (o agent de front sugere axios; **nesta versão fetch basta**) |
| Server state | **TanStack Query v5** | cache do BFF, invalidação pós-transição | Redux, Context de servidor |
| Forms | `<form>` nativo + `FormData` / `JSON.stringify` | login e wizard simples | RHF+Zod **nesta versão** (Zod volta no produto) |
| Schema do wizard | JSON Schema + `x-ui` → widgets nativos (inclui tabela e entity-select) | os **19** tipos seedados usam o mesmo motor | `SegundaChamadaPage.tsx`; lib form-engine pesada |
| HATEOAS | `useActions(links)` 15 linhas | lei do produto: UI cega a perfil | `if (role === 'ADMIN')` |
| Erros | `ProblemBanner` lendo RFC 7807 | contrato Spring | `alert(err.message)` genérico |
| Estilo | `frontend-web/src/index.css` ~40 linhas | foco no JSON | Tailwind, shadcn, CSS modules por tela |
| Tipos | interfaces manuais **curtas** nas fatias 0–3; `openapi-typescript` opcional depois | gera milhares de tokens | copiar OpenAPI inteiro no chat |

**Mobile (só documentação, não implementar agora):** React Native + Expo Router no futuro usa o **mesmo** contrato (`Authorization: Bearer` + CSRF + SecureStore). Cookie jar do Chrome não existe no RN. Não abrir pasta `mobile/` nesta versão.

---

## 3. Arquitetura de pastas (screaming / feature-sliced, enxuta)

O back é modular monolith + Clean Architecture. O front **espelha bounded contexts** sem import cruzado de features.

```
frontend-web/
  src/
    app/
      main.tsx
      providers.tsx          # QueryClient only
      router.tsx
    shared/
      api/
        client.ts            # fetch + CSRF + refresh + problem
        hateoas.ts           # hrefOf, useActions, actionFromRel
        problem.ts           # type Problem
        queryKeys.ts
      ui/
        JsonPanel.tsx        # <pre>{JSON.stringify(x,null,2)}</pre>
        ProblemBanner.tsx
        HateoasBar.tsx       # botões = Object.keys(_links) filtrados
        Page.tsx             # h1 + children
        AttachmentUpload.tsx # fatia 3+; MinIO presign/PUT/confirm
      auth/
        session.ts           # flags mustChangePassword / mustAcceptLgpd (NÃO o JWT)
        AuthGuard.tsx
    features/
      publico/               # fatia 1
      dashboard/             # fatia 2
      solicitacoes/          # fatia 3 — DynamicForm cobre os 19 tipos
      academico/             # fatia 4
      vinculos/              # fatia 5 estágio/tcc/egresso
      staff/                 # fatias 6–7
  index.html
  vite.config.ts
  .env.local                 # VITE_API_BASE_URL=http://localhost:8080
```

Regras:

- `features/A` **não importa** `features/B`. Só `shared/`.
- Página não chama `fetch` — só hooks (`useLogin`, `useRequest`).
- **Não** criar um arquivo por widget de DS. Kernel UI: `Page`, `JsonPanel`, `ProblemBanner`, `HateoasBar` + `DynamicForm` + `AttachmentUpload` (MinIO). Isso cobre os 19 tipos e o resto dos uploads.

---

## 4. Arquitetura de páginas (padrão único — copiar)

Toda página autenticada segue o mesmo esqueleto (pouquíssimas linhas):

```
<Page title="…">
  {isPending && <p>carregando</p>}
  <ProblemBanner problem={error} />
  <HateoasBar links={data?._links} onAction={…} />
  <JsonPanel data={data} />
</Page>
```

Isso é intencional: a equipe **vê o JSON real** e os `rel`s que o back mandou. Se um botão não aparece, o back não mandou o `rel` — não é bug de CSS.

Lista paginada: `content` + `page` + `_links` (self/next) como o `PageResponse` Kotlin.

Dashboard: **uma** query `GET /bff/dashboard/{perfil}` — o BFF já agrega. Proibido fan-out de 6 GETs no mount.

---

## 5. Contrato de integração (o que quebra o time se ignorar)

Fonte: as-built + `AuthController` + HTTPie F0.

### 5.1 Cookies (web)

`POST /auth/login` **não** devolve JWT. Body:

```json
{ "mustChangePassword": false, "mustAcceptLgpd": false }
```

| Cookie | Path | HttpOnly | Uso |
|--------|------|----------|-----|
| `access_token` | `/` | sim | 15 min, claim `sid` → Redis `auth:session:<sid>` |
| `refresh_token` | `/auth` | sim | 7 dias; só requests para `/auth/*` |
| `XSRF-TOKEN` | `/` | **não** | Double Submit; ecoar em `X-XSRF-TOKEN` |

Cliente: `credentials: 'include'` em **todas** as chamadas.  
Dev: profile `application-dev.yml` já tem `app.security.cookie.secure: false`. Sem isso o Chrome descarta cookie em `http://localhost`.

**Não** guardar JWT em `localStorage`. **Não** mandar `Authorization: Bearer` no web se o cookie já autenticar (o filtro JWT aceita cookie **ou** Bearer; cookie é o caminho SPA).

### 5.2 CSRF

```
GET /auth/csrf  →  { "token": "<uuid>", "headerName": "X-XSRF-TOKEN" }  + Set-Cookie XSRF-TOKEN
```

Mutações autenticadas (`POST`/`PATCH`/`PUT`/`DELETE`) e `POST /publico/contato` **exigem** o header igual ao cookie.

**Isentos:** login, refresh, ott, forgot-password, reset-password.

Bootstrap: chamar `/auth/csrf` no `main.tsx` e de novo após 403 `forbidden` de CSRF.

### 5.3 Refresh e 401

Access TTL 900 s. Interceptor:

1. 401 em rota autenticada (não login) → `POST /auth/refresh` (o cookie `refresh_token` Path=/auth vai sozinho).
2. 200 → retry **uma** vez.
3. Falha → limpar flags de sessão, `navigate('/login')`.

Logout: `POST /auth/logout` **com CSRF**. Redis apaga `sid` na hora (force-logout).

### 5.4 RFC 7807

`Content-Type: application/problem+json`:

```json
{
  "type": "https://secretariaonline.ufpr.br/errors/…",
  "title": "…",
  "status": 401,
  "detail": "…",
  "instance": "/path",
  "timestamp": "…",
  "incidentId": "INC-2026-xxxx"
}
```

- 401 login: mensagem **genérica** (anti-enumeração). Nunca “usuário não existe”.
- 422 workflow: `invalid-transition` / `weak-password` / `password-reuse`.
- 429: `Retry-After` / `retryAfterSeconds`.
- 5xx: mostrar `incidentId` na UI.

### 5.5 HATEOAS (formato único as-built 2026-08)

```json
{ "_links": { "self": "/requests/{id}", "assign": "/requests/{id}/transitions" } }
```

- Sempre `Map<string,string>`. **Nunca** `{ assign: { href: "…" } }`.
- Dashboard: `_links` no envelope; pendências usam `_link` **singular**.
- Transição: o **valor** do rel é sempre `POST …/transitions`; o **nome** do rel vira `action`:

```ts
const actionFromRel = (rel: string) => rel.replace(/-/g, '_').toUpperCase()
// "forward-to-deliberator" → "FORWARD_TO_DELIBERATOR"
```

Botões de ação: `Object.entries(links).filter(([rel]) => !['self','events','attachments'].includes(rel))`.

**Proibido:** `if (user.roles.includes('SECRETARIO')) showDefer()`. O admin tem quase todas as authorities — 403 de teste usa **aluno** vs **secretaria**, não “esconder no front”.

### 5.6 Workflow engine (o que o front **não** reimplementa)

Tabelas: `request_type` (`form_schema` JSONB + `workflow_json` JSONB) + `request_type_version` (V019, snapshot no publish) + `request` (`dados` JSONB, `estado`, `id_request_type_version`).

O front:

- **renderiza** `formSchema` (do type ou do detalhe da instância — detalhe já traz o snapshot).
- **não** decide transições a partir de `workflow_json.states`.
- manda `dados` no POST; o back valida JSON Schema de novo (422 se errar).
- actions seed: `ASSIGN`, `FORWARD_TO_DELIBERATOR`, `DEFER`, `DENY`, `REQUEST_ADJUSTMENT`, `RESUBMIT`, `REQUEST_REVIEW` — **não** `DEFERIR`.

Os **19** codes seed (V011+V017) têm de aparecer em `GET /requests/types` e abrir no **mesmo** wizard. `DECLARACAO_MATRICULA` é o caminho mais curto para treinar HATEOAS; **não** substitui trancamento (tabela), segunda chamada (anexo obrigatório) nem os demais. Bulk DEFER direto de `ABERTA` em declaração → **422** (esperado: falta `ASSIGN`).

Lista: `ADIANTAMENTO_PERIODO`, `APROVEITAMENTO_DISCIPLINA`, `TRANCAMENTO_DISCIPLINA`, `TRANCAMENTO_PERIODO`, `COLACAO_SEM_SOLENIDADE`, `REVISAO_NOTA`, `SEGUNDA_CHAMADA`, `INCLUSAO_DISCIPLINA`, `EXCLUSAO_DISCIPLINA`, `MATRICULA_DISCIPLINA_ISOLADA`, `MATRICULA_DISCIPLINA_ELETIVA`, `APROVEITAMENTO_ESTAGIO`, `APROVEITAMENTO_ATIVIDADE_COMPLEMENTAR`, `JUSTIFICATIVA_FALTA`, `DECLARACAO_MATRICULA`, `HISTORICO_ESCOLAR`, `DIPLOMA`, `AUTORIZACAO_IMAGEM`, `ATESTADO_FREQUENCIA`.

### 5.7 FGAC

Authorization via **capabilities** no JWT (`authorities`). Exemplos:

- aluno: `dashboard.view_own`, `request.open`, `request.view_own`
- secretaria: `dashboard.view_secretary`, `request.deliberate`, `request.view_curso`, `request.internal_open` (on-behalf)
- professor: `dashboard.view_self_professor`, `event.manage`, `event.host`

`GET /bff/dashboard/aluno` com cookie de professor → **403** `forbidden`.

---

## 6. Regras para o Grok gastar o mínimo de tokens no React

Economia = **não desenhar**. Cobertura funcional **não** se corta para poupar tokens.

1. Recusar DS: `KpiCard`, `SidebarItem`, `Badge`, Storybook, Playwright, MSW, comentários óbvios, README de componente.
2. **Não** copiar OpenAPI inteiro. Tipos curtos por DTO.
3. **Não** extrair utils até a terceira duplicação.
4. Um `index.css` curto. Sem CSS-in-JS / Tailwind / shadcn.
5. Página pode passar de 80 linhas **se** for POST/GET/anexo/transição. Se passar por div/spacing/label bonito, está errado.
6. Não instalar: axios, RHF, Zod, shadcn, radix, framer-motion, styled-components, jotai, zustand.
7. Dependências: `react`, `react-dom`, `react-router-dom`, `@tanstack/react-query`, `typescript`, `vite`.
8. Um `DynamicForm` + um `AttachmentUpload` para **todos** os tipos e uploads (formativa, estágio, TCC, avatar, request).
9. Widget desconhecido no schema → `<textarea>` JSON **rotulado** com o nome do campo (o tipo continua testável). Nunca `if (code === 'SEGUNDA_CHAMADA')`.
10. Diff da sessão = whitelist da fatia; **não** omitir endpoint da fatia porque “o visual já prova”.
11. Fallback feio permitido: colar UUID, colar `storageKey`, `<input type=file>` nativo. Fallback **proibido**: tela sem o POST da transação.

---

## 7. Variáveis de ambiente

```
VITE_API_BASE_URL=http://localhost:8080
```

`vite.config.ts`: `server.port = 5173`.

---

## 8. Referências (não colar no código)

| Doc | Uso |
|-----|-----|
| `foundationDocs/analysis/as-built-backend.md` | contrato que vence HUs antigas |
| `httpie/` | oráculo passo a passo |
| `logs/log testes httpie - req_resp.md` | exemplos reais 200/4xx desta máquina |
| `frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md` | rels, form_schema, V019 (ler na fatia 3) |
| `transaçõesBackend/` + `foundationDocs/sequenceDiagrams/` | por que o back faz o que faz |
| `backend/app/src/main/resources/db/migration/V011` + `V017` | seed dos 19 tipos |

---

## 9. Definição de pronto (todas as fatias)

- [ ] Roda contra API real, sem mock de auth
- [ ] Cookies visíveis no DevTools após login
- [ ] Mutação autenticada falha **sem** CSRF e passa **com**
- [ ] `_links` mandam nos botões
- [ ] Problem+JSON visível na UI
- [ ] Nenhuma tela nova de “design system”
- [ ] Checklist de [`09-cobertura-transacoes.md`](09-cobertura-transacoes.md) verde — **cada** T-F* / transversal tem uma tela que dispara o mesmo HTTP do HTTPie
