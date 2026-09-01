# Log Fatia 6 — Professor, eventos host, CAAF, COE

**Quando:** 2026-09-01 (BRT) — caixa-branca + HTTP/UI contra API real, com correção de HATEOAS  
**Contrato:** `FatiasFrontend/07-fatia-6-professor-comissoes.md`  
**Oráculos:** `T-F3-professor.md` · `T-F4-001-caaf.md` · `T-F4-002-coe.md` · `T-F1-009` passos 1–2  
**SPA:** `http://localhost:5173` (`frontend-web/`)  
**API:** `http://localhost:8080`  
**Dumps HTTP:** `logs/raw/fatia-6/` (`_run.ps1` + `http-battery.json`)

**Resultado (reteste ~01:16 BRT):** aceite §7 **passa**. Front (5 páginas staff + kernel) + back (`abrir-janela-entrada` em AGENDADO; `editar` só AGENDADO; CONCLUIDO só `self`). GitNexus: `EventAttendanceQuery.getById` / `uiPathFromHref` / `Shell` risco **LOW**. SPA untracked — impacto UNKNOWN.

Delegating to: frontend-engineer, ux-ui-specialist, security-engineer, backend-architect.

---

## 0) O que foi entregue

| Rota | Arquivo | Contrato |
|------|---------|----------|
| `/prof/eventos` | `EventosHostPage.tsx` | GET `/events?host=me` + POST `/events` (4 `attendanceMode`) |
| `/prof/eventos/:id` | `EventoHostDetailPage.tsx` | GET detalhe + HateoasBar janelas/close + PIN/QR em `<pre class="secret">` |
| `/comissoes/caaf` | `CaafPoolPage.tsx` | pool/claim/batch-review/stats (paths as-built, nunca `/dashboard`) |
| `/comissoes/coe` | `CoePoolPage.tsx` | pool/assign-supervisor/bulk/stats + lookup `GET /usuarios?email=` |
| `/prof/comunicado` | `PublicarAvisoPage.tsx` | POST `/communications` + checkbox omitir `cursoId` |
| `/dashboard` | `DashboardPage.tsx` | `solicitacoesPendentes` + `meusEventos` (links `/prof/eventos/:id`) |

Kernel: `queryKeys` eventosHost/caaf/coe, `hateoas.uiPathFromHref` (`/events/{uuid}` → `/prof/eventos/:id`, `/events?host=me` → `/prof/eventos`, commissions → `/comissoes/*`), `Shell` nav, `router`. Deliberar request **reusa** `SolicitacaoDetailPage` (fatia 3).

Default TADS (`01a05940-…`) quando `/me` não tem `metadata.idCurso` (prof.ana não tem).

IDs desta sessão:

| Quem | UUID |
|------|------|
| Aluno `ana.aluno@ufpr.br` | `1bafbb82-a473-4170-8433-c13cebc22562` |
| Prof `prof.ana@ufpr.br` | `98fe1066-4c4c-4f20-b911-2941e0c921a0` |
| Curso TADS | `01a05940-ec36-75cc-ab53-d999ce0a7fa1` |
| Evento HTTP SECRET_SINGLE | `17fcf90f-…` CONCLUIDO (PIN `953896`, 1 certificado) |
| Evento HTTP SECRET_DUAL | `9024f314-…` (PIN `711527`) |
| Evento HTTP QR_SINGLE | `086d13d6-…` (`qrToken` `cd1ebd84…`) |
| Evento HTTP QR_DUAL | `0903938f-…` (`qrToken` `652f48c2…`) |
| Evento UI SECRET_SINGLE | `fbb5a5ef-…` (PIN `507564`, aluno `isComplete: true`) |
| Evento reteste HATEOAS | `9edfbbea-…` AGENDADO com `abrir-janela-entrada` |
| Formativa CAAF HTTP | `6d945a60-…` claim + batch APROVADA |
| Formativa CAAF UI | `53c909a6-…` claim (`idRevisor` prof) |
| Estágio COE HTTP | `8c1257e3-…` assign-supervisor 200 |
| Estágio COE pool (fatia 5) | `2df8219f-…` |
| Comunicado HTTP | `06bb0497-…` entregas=1 |
| Comunicado UI | `43c622ac-…` entregas=1 |

---

## 1) Caixa-branca

Impacto GitNexus **antes** das edições: `uiPathFromHref`, `queryKeys`, `Shell`, `EventAttendanceQuery.getById` = **LOW**.

`detect_changes` (após B1): 8 símbolos, 0 processos, risco **low**. SPA pages staff não indexadas (untracked).

Leitura de contrato vs as-built:

- CAAF: `GET /commissions/caaf/pool` · `POST …/{id}/claim` · `POST …/batch-review` · `GET …/stats`. Path do diagrama `/commissions/caaf/dashboard` → **404**.
- COE: `GET /commissions/coe/pool` · `POST …/{id}/assign-supervisor` · `POST …/bulk-assign` · `GET …/stats`. Conclude individual: `POST /internships/{id}/conclude` (não há approve em lote).
- Eventos: POST windows/entry já promovia AGENDADO → EM_ANDAMENTO; `_links` do GET **não** acompanhavam (B1).

FGAC: UI cega a perfil (`HateoasBar` + ProblemBanner). Cruzado aluno nas rotas staff = 403. Prof **sem** `internship.review` → COE 403 (esperado; admin faz o smoke).

---

## 2) Caixa-preta HTTP

Prof `prof.ana@ufpr.br` · Aluno `ana.aluno@ufpr.br` · Admin `admin@ufpr.br`.

| Caso | Status |
|------|--------|
| `GET /bff/dashboard/professor` prof | **200** `novoEvento` + `meusEventos` |
| `GET /bff/dashboard/professor` aluno | **403** |
| `GET /events?host=me` | **200** |
| `POST /events` SECRET_SINGLE | **201** `17fcf90f-…` |
| GET detalhe AGENDADO (pré-fix) | **200**, `_links` só `self`+`editar` — **sem** `abrir-janela-entrada` |
| `POST /events` aluno | **403** |
| `POST …/windows/entry` SECRET_SINGLE | **200** `secret=953896` |
| GET detalhe EM_ANDAMENTO (pré-fix) | **200**, rels entrada+encerrar **e** `editar` (indevido) |
| `GET …/attendance/session` aluno | **200** `confirmar-entrada` |
| `POST …/attendance/entry` aluno PIN | **200** |
| `POST …/close` | **200** `certificadosEmitidos: 1` |
| GET detalhe CONCLUIDO (pré-fix) | **200**, ainda tinha `editar` |
| POST os 4 `attendanceMode` + windows/entry | **201** / **200** (DUAL e QR inclusive) |
| `GET /commissions/caaf/dashboard` | **404** |
| `POST /formativas` aluno | **201** `PENDENTE` `6d945a60-…` |
| `GET /commissions/caaf/pool` prof | **200**, contém o id |
| `GET /commissions/caaf/pool` aluno | **403** |
| `POST …/caaf/{id}/claim` | **200** `idRevisor` prof |
| 2º claim | **409** |
| `POST …/caaf/batch-review` | **200** |
| `GET …/caaf/stats` | **200** |
| `GET /commissions/coe/pool` prof | **403** (sem `internship.review`) |
| `GET /commissions/coe/pool` aluno | **403** |
| `POST /internships` aluno | **201** `8c1257e3-…` |
| `GET /commissions/coe/pool` admin | **200**, contém o id |
| `GET /usuarios?email=prof.ana@…` admin | **200** |
| `GET /usuarios?email=` prof | **403** |
| `POST …/coe/{id}/assign-supervisor` | **200** |
| `GET …/coe/stats` | **200** |
| `POST /communications` prof + `cursoId` | **201** entregas=1 |
| `POST /communications` sem `cursoId` | **400** pré-fix; **422** pós-fix (`comm-publish-nocurso.txt`) |
| `POST /communications` aluno | **403** |
| `GET /communications/me` aluno | **200** |

Reteste pós-fix (B1), dumps `re-evt-*` / `re-win-entry.txt`:

| Caso | Status |
|------|--------|
| POST evento + GET `9edfbbea` AGENDADO | **200**, `_links.abrir-janela-entrada` **presente** + `editar` |
| POST windows/entry `9edfbbea` | **200** `secret=386147` |
| GET `9edfbbea` EM_ANDAMENTO | **200**, entrada+`encerrar-evento` — **sem** `editar` |
| GET `17fcf90f` CONCLUIDO | **200**, `_links` só `self` — **sem** `editar` |

Reteste B2 (~01:15 BRT), dump `comm-publish-nocurso.txt` / `comm-publish-retest.txt`:

| Caso | Status |
|------|--------|
| POST `/communications` prof **sem** `cursoId` | **422** `unprocessable-entity` `cursoId obrigatório para publicação de turma.` |
| POST `/communications` prof **com** TADS | **201** `b04538c7-…` entregas=1 |

Login 500 `INC-2026-2545` no meio do restart = Redis timeout (`Unable to connect to Redis`). Redis ping PONG; retry login **200**. Não é bug da fatia.

---

## 3) Caixa-preta UI (harness 5173)

| Fluxo | Prova |
|-------|--------|
| Login prof · dashboard | `perfil BFF: professor`; HateoasBar `novoEvento`/`meusEventos`; lista `meusEventos` com links `/prof/eventos/:id` |
| Nav Host eventos / Publicar aviso / CAAF / COE | Shell links visíveis |
| `novoEvento` → `/prof/eventos` | POST SECRET_SINGLE UI `fbb5a5ef-…` |
| Detalhe AGENDADO (pré-fix) | HateoasBar só `editar`; **forçar POST windows/entry** → PIN `507564` + estado `EM_ANDAMENTO` + rels `abrir-janela-entrada`/`encerrar-evento` |
| Detalhe `9edfbbea` pós-fix | EM_ANDAMENTO: HateoasBar `abrir-janela-entrada`+`encerrar-evento` **sem** `editar`; click encerrar → CONCLUIDO, `_links` só `self` |
| Aluno outra aba · sessão + `confirmar-entrada` | PIN 200 `isComplete: true` |
| CAAF | pool lista PENDENTE; claim `53c909a6-…` → `idRevisor` prof |
| COE como prof | ProblemBanner **403** |
| COE como admin | pool `2df8219f-…`; lookup email; assign-supervisor **200** |
| Publicar aviso | **201** `43c622ac-…` entregas=1 |
| Publicar omitindo `cursoId` | ProblemBanner **422** (pós-fix) `cursoId obrigatório para publicação de turma.` |
| Aluno `?perfil=professor` | **403** |
| Aluno `/comissoes/caaf` | **403** |

---

## 4) Bugs de front encontrados e corrigidos

### B-front-1 — prof sem `metadata.idCurso`

POST `/events` e `/communications` nasciam sem curso (TADS). Aluno seeda do `/me`; prof escolhe no `<select>` de `GET /academico/cursos`, com default TADS.

### B-front-2 — mapeamento HATEOAS de `/events`

`uiPathFromHref`: detalhe `/events/{uuid}` → `/prof/eventos/:id` (host); `?audience=` → `/eventos` (aluno, fatia 4); `?host=me` e `/events` → `/prof/eventos`. Sem isso o dashboard mandava o prof para a lista de audiência.

Nenhum clone de `SolicitacaoDetailPage`.

---

## 5) Bugs de backend corrigidos (reteste após restart JVM)

| Bug | Fix | Prova |
|-----|-----|--------|
| GET AGENDADO **sem** `abrir-janela-entrada` (POST windows/entry já funcionava) | `EventAttendanceQuery.getById`: host + not CONCLUIDO → rel entrada | GET `9edfbbea` AGENDADO **com** o rel |
| GET CONCLUIDO ainda tinha `editar` | `editar` só se `AGENDADO` | GET `17fcf90f` e UI `9edfbbea` CONCLUIDO só `self` |
| GET EM_ANDAMENTO tinha `editar` | mesmo gate | GET `9edfbbea` EM_ANDAMENTO: entrada+encerrar, **sem** `editar` |
| POST `/communications` sem `cursoId` era **400** (`requireNotNull` → IAE) | `CommunicationBusinessException` + `ComunicacaoExceptionHandler` **422** | HTTP + UI ProblemBanner **422**; com `cursoId` ainda **201** |

GitNexus impacto `EventAttendanceQuery.getById` / `PublishCommunicationUseCase.execute`: **LOW**.

---

## 6) Aceite §7 (após correção)

1. Dashboard prof **200**; aluno nesse path **403** — **PASS** (HTTP + UI `?perfil=professor`)
2. POST event + open window: JSON com `secret`; estado `EM_ANDAMENTO` — **PASS** (`17fcf90f-…` HTTP PIN `953896`; UI `fbb5a5ef-…` PIN `507564`)
3. Aluno (outra aba) confirma PIN → **200** — **PASS** (HTTP entry + UI `isComplete: true`)
4. Close evento → CONCLUIDO; certificado nasce — **PASS** (`certificadosEmitidos: 1`)
5. CAAF pool lista formativa PENDENTE; aluno no pool → **403** — **PASS** (`6d945a60-…` HTTP; `53c909a6-…` UI)
6. COE pool lista internship; assign-supervisor **200** — **PASS** (`8c1257e3-…`; admin, porque prof seed **não** tem `internship.review`)

---

## 7) Não-bugs / notas

- **Professor não entra no COE.** V010: `internship.review` é COE/coord/admin. 403 do prof no pool é FGAC, não regressão. Smoke COE: admin.
- **`GET /usuarios?email=`** exige authority de staff IAM — prof **403**; admin **200**. CoePoolPage mostra o 403 no ProblemBanner; colar UUID do supervisor ainda funciona para quem tem o pool.
- **`POST /communications` sem `cursoId`** (B2): oráculo T-F3 **422**. Corrigido — ver §5. Admin com `communication.publish` continua podendo omitir `cursoId` (201 institucional).
- Path HTTP `/commissions/caaf/dashboard` **404** (já no HTTPie). Rota React `/comissoes/caaf`; fetch `/commissions/caaf/pool`.
- QR nesta versão = texto do `qrToken` em `<pre class="secret">`. Sem canvas.
- Login 500 pontual pós-restart = Redis down/timeout. Infra, não contrato. Retry **200**.
- JVM `bootRun` caiu (exit -1) e um segundo `bootRun` falhou com porta 8080 ocupada; o processo que ficou no ar (health **UP**) já carregava o fix B1 — reteste HTTP+UI em `9edfbbea` passou.
- Close do evento de reteste `9edfbbea` emitiu **0** certificados (ninguém confirmou PIN nesse id). O SECRET_SINGLE HTTP `17fcf90f` emitiu **1**.
- Fatia 7 (colação / diploma / banca) **não** implementada.
