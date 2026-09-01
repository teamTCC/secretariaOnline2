# Log Fatia 7 — Secretaria, coordenação, admin, busca

**Quando:** 2026-09-01 (BRT) — implementação + caixa-branca + HTTP/UI contra API real, com correção de form e 405  
**Contrato:** `FatiasFrontend/08-fatia-7-secretaria-gestao.md`  
**Oráculos:** `T-F5-*.md` · `T-F6-coordenacao.md` · `T-F7-*.md` · `T-F8-001` · `T-F8-002`  
**SPA:** `http://localhost:5173` (`frontend-web/`)  
**API:** `http://localhost:8080`  
**Dumps HTTP:** `logs/raw/fatia-7/` (`_run.ps1` + `http-battery.json` + `*-retest.txt`)

**Resultado (reteste ~01:52 BRT):** aceite §8 **passa** (diploma-url só quando houver registro de colação — lista 200, `content: []`). Front (staff + kernel) + back (`id_coordenador` TADS; `HttpRequestMethodNotSupportedException` → **405**). GitNexus: `GlobalExceptionHandler` **LOW**; `SolicitacoesListPage` **LOW**; `CourseConfigPage` **UNKNOWN** (página nova). `detect_changes`: medium (1 processo `MeRawPage → UseActions` por colisão de nome em `hateoas.ts` — não é fluxo desta fatia).

Delegating to: frontend-engineer, ux-ui-specialist, security-engineer, backend-architect.

---

## 0) O que foi entregue

| Rota | Arquivo | Contrato |
|------|---------|----------|
| `/usuarios` · `/usuarios/:id` | `UsuariosPage.tsx` | GET lista/detalhe · PATCH `/usuarios/{id}/status` (as-built) · POST reset-password |
| `/secretaria/nova-on-behalf` | `OnBehalfPage.tsx` | lookup `GET /usuarios?email=` + POST `/requests` com `idSolicitanteOnBehalf` |
| `/tarefas` | `TarefasPage.tsx` | GET `/tasks` (não `/tarefas`) · POST · PATCH · DELETE |
| `/relatorios` | `ReportsPage.tsx` | GET `/reports/secretary` · `/reports/coordinator` (`periodo`+`curso`) |
| `/cursos/:id/config` | `CourseConfigPage.tsx` | GET/PATCH `/courses/{id\|sigla}/config` + lookup disciplinas |
| `/admin/request-types` | `RequestTypesAdminPage.tsx` | CRUD + publish; textarea JSON |
| `/admin/roles` | `RolesAdminPage.tsx` | `/admin/roles` · `/admin/autoridades` · PUT user roles |
| `/busca` | `SearchPage.tsx` | GET `/search`; flag `tem USUARIO`; click `href` → navigate |
| `/import` · `/export` | `ImportExportPage.tsx` | jobs + poll JsonPanel; template via `fetch` (não alterou `api()`) |
| `/admin/outbox` | `OutboxAdminPage.tsx` | default PENDING; `?status=PROCESSED`; actuator health |
| `/graduacoes` | `GraduationsPage.tsx` | elegibilidade + `bloqueios[]` · POST `/graduations` · diploma-url · confirm-delivery |
| `/admin/templates` | `TemplatesAdminPage.tsx` | GET `/communication-templates` |
| `/admin/audit` | `AuditPage.tsx` | GET `/admin/audit` |
| `/suporte` | `StaffTicketsPage.tsx` | GET `/support/tickets` · PATCH respond `{resposta}` / close |
| `/faq` | `FaqPage.tsx` | admin POST/PATCH/DELETE `/faq` |
| `/atendimentos` | `AtendimentosPage.tsx` | balcão POST `/service-records` |
| `/solicitacoes?estado=ABERTA` | `SolicitacoesListPage` (reuso) | fila + bulk DEFER visível via `_links.bulk_deliberate` |

Kernel: `queryKeys` (usuarios, tasks, reports, courseConfig, request-types, roles, search, import/export, outbox, graduations, templates, audit, ticketsStaff, disciplinasLookup); `hateoas.uiPathFromHref` (`/search`→`/busca`, `/usuarios`, `/tasks`→`/tarefas`, `/reports/*`, `/courses/:id/config`→`/cursos/:id/config`, `/request-types`→`/admin/request-types`, `/admin/outbox|audit|roles`, `/imports`→`/import`, `/exports`→`/export`, `/graduations`→`/graduacoes`, `/communication-templates`→`/admin/templates`, `/support/tickets`→`/suporte`); `Shell` nav; `router`.

Não esconde botões por `roles.includes`. UI cega a perfil: HateoasBar + ProblemBanner.

IDs desta sessão:

| Quem | UUID |
|------|------|
| Aluno `ana.aluno@ufpr.br` | `1bafbb82-a473-4170-8433-c13cebc22562` |
| Secretaria `secretaria@ufpr.br` | `b1540ee7-32ee-474a-9e66-2405d9e81762` |
| Coord `coord.tads@ufpr.br` | `26942516-ad2b-4316-bbb4-c146fa7628a3` |
| Admin `admin@ufpr.br` | `01a05940-ec31-70fa-8350-d42331ef541a` |
| Curso TADS | `01a05940-ec36-75cc-ab53-d999ce0a7fa1` |
| On-behalf HTTP | `d0146a17-…` protocolo `2026/0010` `idSolicitante`=Ana |
| On-behalf UI | `134a18fe-…` protocolo `2026/0011` |
| Bulk 422 UI | `a1f30f2a-…` DECLARACAO_MATRICULA ABERTA |
| RequestType publicado | `95ba4747-…` code `FATIA7_1788236883` |
| Task HTTP | `fac34807-…` Fatia7 kanban |
| Task UI | `4971e4cf-…` Conferir diplomas |
| Export job | `25abd899-…` PRONTO |
| Service-record HTTP | `d873f053-…` |
| Service-record UI | `b4ae3a31-…` PENDENTE_CIENCIA |
| Ticket respond UI | `6793800d-…` RESPONDIDO |
| FAQ admin POST | `685a5052-…` |
| Outbox PROCESSED (on-behalf UI) | `29008891-…` `solicitacoes.aberta` aggregate `134a18fe-…` |

---

## 1) Caixa-branca

Impacto GitNexus **antes** das edições de kernel: `uiPathFromHref` / `Shell` / `FaqPage` / `AtendimentosPage` / `queryKeys` = **LOW**.  
`CourseConfigPage` (form hydrate): **UNKNOWN** (símbolo novo).  
`SolicitacoesListPage` (já tinha bulk): **LOW**.  
`GlobalExceptionHandler`: **LOW** (2 docs, 0 processos).

`detect_changes` (unstaged): 16 símbolos, 1 processo (`MeRawPage → UseActions` via `hateoas.ts`), risco **medium**. Páginas staff novas não indexadas. Não se alterou `api()` (impacto CRITICAL / colisão de nome).

Leitura de contrato vs as-built:

- PATCH usuário: diagrama/fatia `PATCH /usuarios/{id}` `{ativo}` → **as-built** `PATCH /usuarios/{id}/status`. Path sem `/status` era **500** (B2); após handler → **405**.
- Tarefas: `GET /tasks` **200**; `/tarefas` **404**.
- Reports: `/reports/secretary` e `/reports/coordinator` (não `RelatoriosController`). Query `periodo`+`curso`. Sem 42P18.
- Outbox: default PENDING; lista “vazia” sem `?status=PROCESSED` é esperado.
- Audit: `/admin/audit` (não `/audit`).
- Busca: resposta plana `{type,id,title,subtitle,href}`; FGAC aluno sem `USUARIO`.
- `api()` não recebeu overload de blob: template CSV usa `fetch` + `BASE` em `ImportExportPage`.

FGAC: UI não filtra por perfil. Cruzado aluno em dashboard secretaria / course-config / tickets staff = 403. Secretaria em `/reports?kind=coordinator` = 403. Secretaria POST `/faq` = 403 (`system.admin`). Admin POST `/faq` = 201.

---

## 2) Caixa-preta HTTP

Sec `secretaria@ufpr.br` · Coord `coord.tads@ufpr.br` · Admin `admin@ufpr.br` · Aluno `ana.aluno@ufpr.br` · Prof `prof.ana@ufpr.br`.

| Caso | Status |
|------|--------|
| `GET /bff/dashboard/secretaria` sec | **200** `emTriagem` + `_links.solicitacoes`/`usuarios` |
| `GET /bff/dashboard/secretaria` prof | **403** |
| `GET /usuarios?email=ana.aluno@…` sec | **200** |
| `GET /usuarios?email=` aluno | **403** |
| POST on-behalf | **201** `d0146a17-…` |
| GET detalhe on-behalf | **200** `idSolicitante` = Ana |
| Bulk DEFER ABERTA declaração | **422** |
| ASSIGN depois DEFER | **200** / **200** |
| `GET /search?q=ana` aluno | **200** `results: []` sem USUARIO |
| `GET /search?q=ana&types=USUARIO` sec | **200** 3 usuários |
| `GET /tasks` | **200** |
| `GET /tarefas` | **404** |
| POST `/tasks` | **201** |
| `GET /reports/secretary` · `/coordinator` | **200** (sem 42P18) |
| `GET /courses/tads/config` coord (pré-SQL) | **403** |
| `GET /courses/tads/config` admin | **200** |
| `GET /courses/tads/config` aluno | **403** |
| `GET /courses/tads/config` coord (pós-SQL) | **200** (`course-config-coord-retest.txt`) |
| `GET /request-types` admin | **200** |
| `GET /request-types` aluno | **403** |
| POST + publish type | **201** / **200** `FATIA7_1788236883` |
| Aluno `GET /requests/types` após publish | **200** contém `FATIA7_1788236883` |
| `GET /admin/outbox?status=PROCESSED` | **200** (defer/assign/aberta) |
| `GET /admin/roles` · `/admin/autoridades` | **200** |
| `GET /admin/audit` | **200** |
| `GET /communication-templates` | **200** |
| `GET /graduations` · `GET /students?eligibleForGraduation=true` | **200** Ana `eligible:false` + `bloqueios[]` |
| PATCH `/usuarios/{id}` (path errado) pré-fix | **500** |
| PATCH `/usuarios/{id}/status` | **200** |
| POST `/exports/alunos` · POST `/imports/alunos` | **202** |
| POST `/service-records` | **201** |
| `GET /support/tickets` staff | **200** |
| `GET /support/tickets` aluno | **403** |
| `GET /faq` | **200** |
| `GET /academico/disciplinas?search=` | **200** |

Reteste pós-restart JVM (~01:52 BRT):

| Caso | Status |
|------|--------|
| PATCH `/usuarios/{id}` (path errado) | **405** `method-not-allowed` (`usuarios-patch-wrong-retest.txt`) |
| PATCH `/usuarios/{id}/status` | **200** (`usuarios-status-retest.txt`) |
| POST `/faq` admin | **201** `685a5052-…` (`faq-admin-post-retest.txt`) |
| GET `/courses/tads/config` sessão coord antiga | **401** (JWT/sessão morta no restart — re-login UI coord **200**) |

---

## 3) Caixa-preta UI (harness 5173)

| Fluxo | Prova |
|-------|--------|
| Login sec · dashboard | `perfil BFF: secretaria`; HateoasBar `solicitacoes`/`usuarios`; KPI `emTriagem` |
| On-behalf lookup + POST | detalhe `idSolicitante` = Ana; `_links.assign` |
| Fila ABERTA + checkbox + bulk DEFER | ProblemBanner **422** `Transição 'DEFER' não é válida a partir do estado 'ABERTA'` (`a1f30f2a-…`) |
| Busca sec `q=ana` | `tem USUARIO: true` |
| Busca sec `types=USUARIO` + click href | navega `/usuarios/1bafbb82-…` |
| Relatórios secretary | **200** KPIs |
| Relatórios coordinator como sec | ProblemBanner **403** |
| Relatórios coordinator como coord | **200** KPIs (`tempoMedioDeliberacaoSegundos`, pendências) |
| Curso config coord | GET **200** `horasFormativasMinimas: 120`; form hidratado **120** (pós B1; antes default 150) |
| Curso config aluno | ProblemBanner **403** |
| Colação | Ana `eligible: false`; `bloqueios` TCC/HISTORICO/HORAS_FORMATIVAS/SOLICITACOES visíveis no JSON |
| Export | histórico job `25abd899-…` `PRONTO` |
| Import GET template | CSV `nome,email,grr,role` · `{template, bytes:64}` |
| Tarefas GET + POST + PATCH | lista `/tasks`; POST `4971e4cf-…` |
| Usuários GET detalhe Ana | `grr` GRR20210001 `ativo: true` |
| Tickets staff respond | `6793800d-…` `RESPONDIDO` |
| FAQ GET + POST `/faq` como sec | lista 200; POST ProblemBanner **403** |
| Balcão POST `/service-records` | `b4ae3a31-…` `PENDENTE_CIENCIA` |
| Busca aluno `q=ana` | `tem USUARIO: false` `results: []` |
| Aluno `?perfil=secretaria` | **403** `/bff/dashboard/secretaria` |
| Sec `?perfil=professor` | **403** `/bff/dashboard/professor` |
| Admin outbox `PROCESSED` | 52 eventos; `solicitacoes.defer` / `assign` / `aberta` |
| Admin request-types | `FATIA7_1788236883` na lista + no `<select>` da fila |
| Templates | 6 códigos seed (`solicitacoes.transicionada`, …) |
| Audit | `LOGIN_SUCCESS` + `request_type.publish` |

Diploma-url / confirm-delivery: botões existem; `GET /graduations` `content: []` — sem registro colado, aceite #10 fica no list 200 + bloqueios.

---

## 4) Bugs de front encontrados e corrigidos

### B-front-1 — form de config nascia com `150` enquanto GET devolvia `120`

`CourseConfigPage` usava `useState('150')` e nunca sincronizava com `cfg.data`. PATCH cego escreveria 150. `useEffect` hidrata `horasFormativasMinimas` / `duracaoCalendario` / banca / modalidade / regimento. Reteste UI: input **120**.

---

## 5) Bugs de backend / dados corrigidos (reteste após restart JVM)

| Bug | Fix | Prova |
|-----|-----|--------|
| Coord `GET /courses/tads/config` **403** | `curso.id_coordenador` estava NULL (Passo F do bootstrap não aplicado). SQL: `UPDATE curso SET id_coordenador = '26942516-…' WHERE sigla='TADS'` | `course-config-coord-retest.txt` **200**; UI coord 200 |
| PATCH `/usuarios/{id}` (sem mapping) → **500** genérico | `GlobalExceptionHandler.handleMethodNotAllowed` → **405** `method-not-allowed` | `usuarios-patch-wrong-retest.txt` **405**; `/status` ainda **200** |

GitNexus impacto `GlobalExceptionHandler`: **LOW**.

Restart JVM: 1º `bootRun` falhou cache Kotlin Windows; 2º falhou **porta 8080 ocupada** (java zumbi `24672`); 3º `bootRun` (`Tomcat started` 01:50) carregou o handler.

---

## 6) Aceite §8 (após correção)

1. Sec dashboard **200**; prof nesse path **403** — **PASS** (HTTP + UI sec; HTTP prof; UI aluno `?perfil=secretaria` 403)
2. On-behalf **201**; detalhe `idSolicitante` = Ana — **PASS** (HTTP `d0146a17-…`; UI `134a18fe-…`)
3. Bulk DEFER em ABERTA declaração → **422** visível; ASSIGN depois DEFER → **200** — **PASS** (HTTP + UI 422; ASSIGN/DEFER HTTP)
4. Busca aluno `q=ana` sem USUARIO; busca sec com USUARIO — **PASS**
5. Admin publish type → aluno vê em `/requests/types` — **PASS** (`FATIA7_1788236883`; UI select da fila)
6. Outbox `status=PROCESSED` mostra eventos — **PASS** (HTTP + UI admin)
7. Reports secretary/coordinator **200** (não 500 42P18) — **PASS**
8. Coord `GET /courses/tads/config` **200**; aluno **403** — **PASS** (após SQL)
9. `GET /tasks` **200** (não `/tarefas`) — **PASS** (HTTP 200/404 + UI POST)
10. T-F5-005 colação/diploma-url **200** quando houver registro — **PASS** lista/elegibilidade 200 + `bloqueios[]`; diploma-url N/A (`content: []`)
11. Templates listáveis; audit/FAQ admin disparáveis — **PASS** (GET templates/audit 200; POST `/faq` admin **201**)
12. Import + export job criados e polled — **PASS** (HTTP 202; UI export PRONTO + template import)

---

## 7) Não-bugs / notas

- **Busca aluno vazia** para `q=ana` não é regressão FGAC: aluno não recebe `type=USUARIO` e “ana” não casa com requests/eventos/cursos próprios. Sec/admin veem usuários.
- **PATCH `/usuarios/{id}`** do diagrama não existe. Harness documenta `/status`. 405 pós-fix é o status certo (não 500).
- **Coord 403 em course-config** era dado (`id_coordenador` NULL), não FGAC do perfil COORDENADOR. Admin bypass já era 200.
- **403 genérico** (`Você não tem permissão para esta operação.`) esconde “não é coordenador” — `GlobalExceptionHandler.handleAccessDenied`. Contrato de harness: ProblemBanner basta.
- Secretaria em `/reports/coordinator` **403** é FGAC, não bug. Coord **200**.
- Secretaria POST `/faq` **403** (`system.admin`). Botão visível de propósito (não filtrar por role).
- `GET /graduations` vazio: Ana não elegível (`HORAS_FORMATIVAS` 4 < 120, TCC, histórico, solicitações ABERTA). Bloqueios **não** mascarados.
- `GET /courses/tads/config` após restart com sessão httpie coord antiga → **401**. Relogar.
- `detect_changes` medium em `useActions` é ruído de grafo (MeRawPage). `uiPathFromHref` é o símbolo tocado de verdade.
- Fatia é SPA de inspeção (HTML feio + JsonPanel). Sem shadcn/Figma.
