# Log Fatia 5 — Estágio, TCC, egresso

**Quando:** 2026-08-31 / 01-09 (BRT) — caixa-branca + HTTP/UI contra API real, com correção de bugs  
**Contrato:** `FatiasFrontend/06-fatia-5-estagio-tcc-egresso.md`  
**Oráculos:** `T-F1-007-008-estagio-tcc.md` · `T-F2-001-dashboard-egresso.md`  
**SPA:** `http://localhost:5173` (`frontend-web/`)  
**API:** `http://localhost:8080`  
**Dumps HTTP:** `logs/raw/fatia-5/`

**Resultado (reteste ~00:04 BRT):** aceite §5 **passa**. Front (4 páginas + kernel) + back (`conclude` só em `EM_ANDAMENTO`; 2º conclude **409**). GitNexus: `AttachmentUpload` / `uiPathFromHref` / `DashboardPage` / `EstagioQuery.get` risco **LOW**. SPA untracked — impacto UNKNOWN.

Delegating to: frontend-engineer, ux-ui-specialist, security-engineer, backend-architect.

---

## 0) O que foi entregue

| Rota | Arquivo | Contrato |
|------|---------|----------|
| `/estagios` | `EstagiosPage.tsx` | POST `/internships` + GET `/internships/mine` (nunca `/estagios` no fetch) |
| `/estagios/:id` | `EstagioDetailPage.tsx` | GET detalhe + docs MinIO + HateoasBar (`conclude` só se `_links`) |
| `/tccs` | `TccsPage.tsx` | POST `/tccs` (prof) + GET `/tccs/mine` (aluno) + GET `/tccs` staff |
| `/tccs/:id` | `TccDetailPage.tsx` | membros/banca/approve pelos rels + PDF `submit-final-url` |
| `/dashboard` | `DashboardPage.tsx` | redirect `alumni.view_own` → BFF egresso; diploma-url paste |

Kernel: `queryKeys` internships/tccs, `hateoas.uiPathFromHref` (`/internships`→`/estagios`, `/tccs`→`/tccs`), `AttachmentUpload` `buildPresignBody`/`confirmPath`, `Shell` nav, `router`.

IDs desta sessão:

| Quem | UUID |
|------|------|
| Aluno `ana.aluno@ufpr.br` | `1bafbb82-a473-4170-8433-c13cebc22562` |
| Prof `prof.ana@ufpr.br` | `98fe1066-4c4c-4f20-b911-2941e0c921a0` |
| Egresso `ana.egressa@ufpr.br` | `ba731d06-ad28-4998-9a53-e756ce1b789f` |
| Curso TADS | `01a05940-ec36-75cc-ab53-d999ce0a7fa1` |
| Estágio HTTP | `2df8219f-…` EM_ANDAMENTO (docs MinIO) |
| Estágio conclude HTTP | `75788083-…` CONCLUIDO |
| Estágio UI | `087fbe61-…` CONCLUIDO (admin HateoasBar) |
| TCC HTTP | `7609aab9-…` (aluno membro AUTOR) |
| TCC UI prof | `15466986-…` |

---

## 1) Caixa-preta HTTP

Aluno `ana.aluno@ufpr.br` · Prof `prof.ana@ufpr.br` · Admin `admin@ufpr.br` · Egresso `ana.egressa@ufpr.br`.

| Caso | Status |
|------|--------|
| `POST /internships` aluno | **201** `EM_ANDAMENTO` `2df8219f-…` |
| `GET /internships/mine` | **200**, contém o id |
| `GET /internships/{id}` aluno | **200** `_links` só `self`+`documents` — **sem** `conclude` |
| `POST …/conclude` aluno | **403** |
| `GET /internships/{id}` secretaria | **403** (SECRETARIO sem `internship.review`; COE/coord/admin têm) |
| `GET /internships/{id}` admin | **200** `_links.conclude` + `update` |
| Presign + PUT MinIO + `POST …/documents` | **200** / **200** / **201** CONTRATO |
| `POST /internships` #2 + conclude admin | **201** / **200** `CONCLUIDO` |
| `POST /tccs` aluno | **403** |
| `POST /tccs` prof | **201** `7609aab9-…` `EM_ANDAMENTO` |
| `POST /tccs/{id}/members` AUTOR | **201** |
| `GET /tccs/{id}` aluno | **200** `_links.submit-final-url` |
| `GET /tccs/mine` aluno | **200** (após member) |
| `GET /tccs/mine` prof | **403** (`tcc.view_own` só aluno) |
| `GET /estagios/me` (path errado) | **404** |
| `GET /bff/dashboard/egresso` egresso | **200**, **sem** `novaSolicitacao` |
| Aluno → `/bff/dashboard/egresso` | **403** |
| Egresso → `/bff/dashboard/aluno` | **403** |
| Aluno → `/bff/dashboard/aluno` | **200** com `novaSolicitacao` |
| Egresso `GET /graduations/{fake}/diploma-url` | **403** (authority; colação é fatia 7) |
| Admin diploma-url UUID fake | **404** |

Reteste pós-restart (B1/B2):

| Caso | Status |
|------|--------|
| GET detalhe `087fbe61` CONCLUIDO | **200**, `_links.conclude` **ausente** |
| GET detalhe `75788083` CONCLUIDO | **200**, sem conclude |
| GET detalhe `2df8219f` EM_ANDAMENTO | **200**, conclude **presente** |
| 2º `POST conclude` em CONCLUIDO | **409** `Estágio não está EM_ANDAMENTO.` |

---

## 2) Caixa-preta UI (harness 5173)

| Fluxo | Prova |
|-------|--------|
| Nav Estágios / TCCs | Shell links visíveis após login |
| Aluno POST `/internships` | `087fbe61-…` na lista mine + link “abrir” |
| Aluno detalhe | HateoasBar só `documents` — **sem** `conclude`; estado `EM_ANDAMENTO` |
| Aluno force conclude URL crua | ProblemBanner **403** “Você não tem permissão…” |
| Aluno POST `/tccs` | ProblemBanner **403**; `GET /tccs/mine` lista `7609aab9-…` |
| Aluno detalhe TCC | HateoasBar `submit-final-url` (membro); sem add-member/approve |
| Aluno força BFF egresso | `?perfil=egresso` → **403** |
| Prof POST `/tccs` (idCurso TADS) | **201** `15466986-…` |
| Egresso `/dashboard` | `perfil BFF: egresso`; `novaSolicitacao: (ausente) — egresso não abre solicitação`; rels `certificados`+`comunicados` |
| Egresso força BFF aluno | **403** |
| Admin detalhe estágio | HateoasBar `documents`+`update`+`conclude`; click conclude → `CONCLUIDO` |

---

## 3) Bugs de front encontrados e corrigidos

### B-front-1 — `AttachmentUpload` só falava o body de `/requests`

Estágio exige `{ tipo, nomeOriginal, contentType }`; TCC `{ nomeOriginal }` + confirm `{ storageKey, sha256 }`. O kernel mandava `{ filename, contentType, sha256, … }` e confirmava só `/requests/…/attachments/confirm`.

**Fix:** props opcionais `buildPresignBody`, `confirmPath`, `buildConfirmBody`. Formativas/wizard inalterados. GitNexus impacto **LOW** (3 callers).

### B-front-2 — `POST /tccs` do professor sem `idCurso` nascia disabled

Prof não tem `metadata.idCurso`. Aluno seeda do `/me`; prof escolhe no `<select>` de `GET /academico/cursos`.

### B-front-3 — add-member caía no `id` do `/me` (orientador)

HateoasBar `add-member` sem UUID colado mandaria o professor como aluno.

**Fix:** exigir `idAluno` preenchido; placeholder `uuid do aluno`.

---

## 4) Bugs de backend corrigidos (reteste após restart JVM)

| Bug | Fix | Prova |
|-----|-----|--------|
| `_links.conclude` mesmo em `CONCLUIDO` | `EstagioQuery.get`: conclude só se `canReview && estado == EM_ANDAMENTO` | GET `087fbe61` / `75788083` sem rel; `2df8219f` com rel |
| 2º conclude era **400** (`require` → `IllegalArgumentException`) | `IllegalStateException` → handler **409** conflict | POST conclude em CONCLUIDO **409** |

GitNexus impacto `EstagioQuery.get` / `EncerrarEstagioUseCase.conclude`: **LOW** (0 callers indexados no grafo; controller chama).

---

## 5) Aceite §5 (após correção)

1. POST internship aluno **201** `EM_ANDAMENTO`; GET mine contém o id — **PASS** (`2df8219f-…` HTTP; `087fbe61-…` UI)
2. Aluno detalhe **sem** rel conclude; token admin **com**; URL crua aluno **403** — **PASS**
3. POST tccs como aluno sem `tcc.supervise` → **403** — **PASS** (HTTP + UI)
4. POST tccs como prof **201** — **PASS** (`7609aab9-…` HTTP; `15466986-…` UI)
5. Egresso dashboard sem nova solicitação; aluno nesse path **403** — **PASS** (cruzado também egresso→aluno)

---

## 6) Não-bugs / notas

- **SECRETARIO** não tem `internship.review` (V010: só COE, COORDENADOR, ADMIN). `GET /internships/{id}` secretaria **403** é FGAC, não regressão. Conclude na demo: admin/coord.
- `GET /tccs/mine` **403** no professor: authority `tcc.view_own` é de aluno. TccsPage mostra o 403 no JsonPanel; POST create segue.
- Path HTTP `/estagios` **404** (já visto no HTTPie). Rota React `/estagios`; fetch `/internships`.
- Diploma PDF: BFF egresso **não** emite `_links.download`. `GET /graduations/{id}/diploma-url` exige `diploma.register`/`alumni.list`/`system.admin` — egresso **403**. T-F2-001 diploma fica completo na fatia 7 (colação). Dashboard já tem o paste + HateoasBar `download` se o BFF mandar.
- Pool COE `/commissions/coe/pool`: fatia 6. Banca/modalidade de curso: fatia 7.
- `/alumni/me` não existe no as-built — só BFF egresso.
