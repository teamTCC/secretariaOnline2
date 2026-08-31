# Log Fatia 2 — Shell, `/me`, primeiro acesso, dashboards BFF, FGAC

**Quando:** 2026-08-31 (reset de ambiente + caixa-branca + caixa-preta HTTP/UI, pós-correção)  
**Contrato:** `FatiasFrontend/03-fatia-2-shell-dashboard.md`  
**Oráculos:** `httpie/F1-aluno/T-F1-001` · `T-F1-002` · `T-F1-003` · F2/F3/F5 dashboards  
**SPA:** `http://localhost:5173` (`frontend-web/`)  
**API:** `http://localhost:8080` (Spring Boot, profile `dev`)  
**Dumps crus:** `logs/raw/fatia-2/`

**Resultado:** aceite da Fatia 2 **passou após correção de 3 bugs de front**. Cada perfil vê o BFF dele; FGAC cruzado pinta 403; logout mata a sessão (F5 em `/dashboard` → `/login`). Sem JWT em `localStorage`. Sem axios/zod/shadcn/KpiRow.

Delegating to: frontend-engineer, security-engineer (auth/CSRF/FGAC), devops-engineer (reset). GitNexus: SPA untracked — `MePage` sem símbolo indexado.

---

## 0) Reinício + reseed (ambiente limpo)

Postgres **não** é o container `secretaria_postgres`: a porta `:5432` é o PostgreSQL 18 nativo do Windows (`secretaria` / `localdev`, db `secretaria_dev`). `docker compose up postgres` conflita.

| Serviço | Ação | Prova |
|---------|------|--------|
| Redis / MinIO | `docker compose` em `ops/` — volumes `ops_redis_data` / `ops_minio_data` recriados | `secretaria_redis` e `secretaria_minio` healthy |
| Mailpit | `docker restart secretaria_mailpit` | `:1025` / `:8025` |
| Schema | `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` + uuid-ossp, pgcrypto, citext, pg_trgm | Flyway reaplicou no bootRun |
| JVM `:8080` + Vite `:5173` | processos anteriores mortos; `gradlew :app:bootRun` (JWT de `backend/.env.local`) + `npm run dev` | `GET /actuator/health` **200**; Vite **200** |
| Hash admin | placeholder Flyway **não** autentica; Argon2id válido reaplicado após migrate | login `admin@ufpr.br` 200 |

Hash Argon2id reaplicado (admin):

`$argon2id$v=19$m=16384,t=2,p=1$utK0+pWRSGCQd140l0CBmw$EYnm4LWw4NQBWZm3f9x/BwIrLbZfJxnDCwp1jHmmybo`

**Bootstrap demo (limpo)** — first-access **200** em todos; relogin flags `{false,false}`:

| Papel | Identificador | Senha definitiva |
|-------|---------------|------------------|
| Aluno | `ana.aluno@ufpr.br` / GRR `GRR20210001` | `AlunoS3nh@Forte!` |
| Prof | `prof.ana@ufpr.br` | `ProfS3nh@Forte!` |
| Sec | `secretaria@ufpr.br` | `SecrS3nh@Forte!` |
| Coord | `coord.tads@ufpr.br` | `CoordS3nh@Forte!` |
| Egresso | `ana.egressa@ufpr.br` | `EgressoS3nh@Forte!` |

- Curso TADS `cursoId`: `01a05940-ec36-75cc-ab53-d999ce0a7fa1`
- Aluno id: `1bafbb82-a473-4170-8433-c13cebc22562`
- Senhas temporárias lidas de `outbox_event` (`iam.usuario_criado`) via SQL — `GET /admin/outbox` voltou vazio mesmo com eventos PROCESSED.

**Armadilha de bootstrap:** `PATCH /me` só com `idCurso` **substitui** o mapa `metadata` e apaga `aceite_lgpd_em` (as-built `UpdateProfileUseCase`). Restaurado com PATCH contendo os dois campos. Relogin HTTP → flags `{false,false}`.

---

## 1) Caixa-branca vs spec / as-built

Whitelist conferida: `session.ts`, `AuthGuard.tsx`, `Shell.tsx`, `DashboardPage.tsx`, `PrimeiroAcessoPage.tsx`, `MePage.tsx`, `router.tsx`. Rotas autenticadas wrap `AuthGuard` → `Shell`. `/me-raw` → `/me`. Stub `/solicitacoes` = “fatia 3”.

### As-built (não “corrigir” no front)

| Spec | Spring |
|------|--------|
| `GET /me` com `authorities[]` | **só** `roles[]` — BFF inferido por role: `SECRETARIO` → secretaria, `PROFESSOR` → professor, `EGRESSO` → egresso, `ALUNO` → aluno; admin/coord sem BFF |
| `POST /me/avatar` `{ contentType }` | **sem body** |
| `POST /me/password` | **204** (não JSON) |
| `POST /auth/logout` | **200** (spec cita 204) |
| first-access senha `< 12` | **400** bean validation; **422** só domínio (fraca com ≥12 chars) |
| `PATCH /me` | **substitui** o mapa `metadata` inteiro |

Dashboard: **um** GET `/bff/dashboard/{perfil}`; `?perfil=` força FGAC. Sem fan-out `/requests`+`/events`+`/formativas`.

---

## 2) Bugs encontrados e correção

### B1 — AuthGuard + `mustAcceptLgpd` (prende a SPA)

`FirstAccessUseCase` exige `mustChangePassword()` (`senhaAlterada=false`). Se só `mustAcceptLgpd` (ex.: `PATCH /me` apagou `metadata.aceite_lgpd_em`), `POST /auth/first-access` → **400** “já completou” e o guard **redirecionava em loop** para `/primeiro-acesso`.

**Fix:** AuthGuard só redireciona se `mustChangePassword`. `afterAuthRedirect` ainda manda para `/primeiro-acesso` se qualquer flag (login fresco). `PrimeiroAcessoPage` tem botão “ir ao dashboard (first-access já feito)” se `mustChangePassword === false`.

Prova UI: login aluno com flags stale `mustAcceptLgpd:true` caía em `/primeiro-acesso`; após o fix, `/dashboard` **200** com `novaSolicitacao`.

### B2 — JsonPanel “última mutação” na `/me`

Cadeia `??` mostrava mutação antiga (avatar presign) depois de `POST /me/password` 400.

**Fix:** estado `lastMutation` + `onError` na mutation de senha; efeito no poll de data-export.

Prova UI: senha errada → `ProblemBanner` “Senha atual incorreta.” (400). Após `onError`, o JsonPanel da mutação passa a mostrar o Problem.

### B3 — `JSON.parse` de metadata no PATCH `/me`

Parse inválido virava exceção crua. **Fix:** try/catch → Problem 400 “JSON inválido” na banner.

**Preventivo (mesmo as-built):** MePage **seed** nome + `metadata` do `GET /me` para o PATCH não apagar `aceite_lgpd_em` / `idCurso`.

---

## 3) Caixa-preta HTTP (origem API / cookies de sessão)

Dump: `logs/raw/fatia-2/http-battery.json` + arquivos `.txt` por caso.

| Caso | Status | Observação |
|------|--------|------------|
| `GET /bff/dashboard/aluno` (aluno) | **200** | `_links.novaSolicitacao` = `/requests/types`; kpis 0/120 |
| mesmo path como prof | **403** | FGAC |
| `GET /bff/dashboard/secretaria` como aluno | **403** | FGAC |
| dash prof / sec / egresso | **200** | egresso **sem** `novaSolicitacao` |
| dash aluno como egresso | **403** | FGAC |
| `GET /me` aluno | **200** | `roles: [ALUNO]`; **sem** `authorities` |
| `PATCH /me` | **200** | |
| `POST /me/password` senha atual errada | **400** | |
| `PATCH /me/notifications` | **200** | |
| `POST` / `DELETE /me/fcm-token` | **200** | |
| `POST /me/data-export` | **202** | poll → `READY` + URL MinIO |
| `POST /me/avatar` | **200** | presign MinIO |
| `POST /auth/logout` | **200** | `GET /me` seguinte **401** |
| first-access `aceiteLgpd: false` | **400** | |
| first-access senha `"curta"` | **400** | bean validation (não 422) |

---

## 4) Caixa-preta UI (browser `localhost:5173`)

Dump: `logs/raw/fatia-2/ui-battery.json`.

| Fluxo | Resultado |
|-------|-----------|
| Login demo Aluno → dashboard | **200**, `novaSolicitacao` visível (após B1) |
| `/me`: PATCH, notifications, FCM ±, data-export, avatar | **200**/READY; senha errada **400** na banner |
| `Sair` | `/login`; `sessionStorage` flags **null**; `document.cookie` só `XSRF-TOKEN` |
| F5 / abrir `/dashboard` anônimo | **redirect** `/login` (AuthGuard 401) |
| Login Prof | BFF professor; `_links.novoEvento` = `/events` |
| Forçar GET aluno logado como prof | **403** “Acesso negado” + JSON RFC 7807 |
| Login Admin | “sem BFF” + JSON `/me` `roles: [ADMIN]` |
| Demo Aluno GRR `GRR20210001` | BFF aluno; e-mail `ana.aluno@ufpr.br` |
| Forçar GET secretaria logado como aluno | **403** `instance=/bff/dashboard/secretaria` |
| `localStorage` | vazio (JWT não vaza) |

`npx tsc --noEmit` em `frontend-web/`: **0 erros**.

---

## 5) Checklist de aceite (`03-fatia-2` §6)

| Item | Status | Evidência |
|------|--------|-----------|
| Aluno: dashboard 200, `_links.novaSolicitacao` presente | **PASS** | HTTP `dash-aluno.txt` + UI |
| Professor no dashboard aluno: 403 visível | **PASS** | HTTP + UI `?perfil=aluno` |
| Aluno no dashboard secretaria: 403 | **PASS** | HTTP + UI GRR `?perfil=secretaria` |
| Logout: cookie some; F5 `/dashboard` → login | **PASS** | `me-after-logout` 401; navigate `/dashboard` → `/login` |
| `GET /me` no JsonPanel com `roles` | **PASS** | UI `/me` e admin no dashboard |
| Shell botão Sair + CSRF no POST logout | **PASS** | `api()` + cookie `XSRF-TOKEN`; UI Sair |
| PATCH `/me`, senha, notifications, FCM ±, data-export | **PASS** | HTTP + UI `/me` |
| Avatar: presign 200 | **PASS** | HTTP `avatar.txt` + UI; PUT MinIO **não** exercitado (CORS/arquivo; `storageKey` colável no JSON) |

**Não fazer (conferido):** sem KpiRow/gráficos/DashboardA; sem fan-out no mount; nav fixa do harness (não `if (role === 'ADMIN')` para esconder itens de aluno).

---

## 6) Fora desta fatia / residual

- PUT real no MinIO a partir do browser (CORS do bucket) — presign já prova o contrato.
- Happy-path first-access 200 na UI com usuário **novo** (admin fatia 7); 400/422 cobertos via payload forçado HTTP.
- `GET /me` sem `authorities[]` — inferência por `roles` (as-built).
- `GET /admin/outbox` vazio após wipe — senhas lidas via SQL.
- Wipe futuro: reaplicar hash admin; `PATCH /me` **sempre** reenviar `aceite_lgpd_em` + `idCurso`.
- Postgres nativo: **não** subir o container postgres na `:5432`.

---

## 7) Conclusão

Depois do reset (schema + Redis/MinIO + Flyway + bootstrap + first-access) e dos três fixes de SPA, a Fatia 2 está **coerente com o Spring as-built** e o aceite da spec. O harness permite à equipe:

1. Entrar como aluno/prof/sec/egresso/admin/GRR e ver o BFF certo (ou “sem BFF”).
2. Forçar o dashboard do outro perfil e ler **403** na `ProblemBanner`.
3. Exercitar T-F1-003 inteiro em `/me` (incluindo export READY e presign de avatar).
4. Sair e confirmar que a sessão Redis/cookie morreu (próximo GET 401 / F5 → login).
5. Sobreviver a `mustAcceptLgpd` órfão sem loop no `/primeiro-acesso`.
