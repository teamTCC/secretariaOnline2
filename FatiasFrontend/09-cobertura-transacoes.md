# 09 — Cobertura caixa-preta (transação → tela)

Oráculo HTTP: pasta `httpie/` (mesmo body/status). Este arquivo diz **em qual tela feia** a equipe dispara o mesmo request.

Se uma linha não tiver botão/form que chame o path, a fatia **não** está pronta — mesmo que o JSON do dashboard “já mostre” o dado.

Visual: irrelevante. Método + path + CSRF + `_links` + Problem: obrigatórios.

---

## F0 — Público

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F0-001 | login, refresh (interceptor), logout, CSRF, OTT | Login, Shell, `/auth/ott?token=` | 1–2 |
| T-F0-002 | forgot-password 202 | `/recuperar-senha` | 1 |
| T-F0-003 | reset-password 422 reuse/weak | `/nova-senha` | 1 |
| T-F0-004 | GET/POST `/publico/contato` + CSRF | `/contato` | 1 |
| T-F0-005 | Problem+JSON na UI | `ProblemBanner` + `/erro/:incidentId` | 0–1 |
| T-F0-006 | protocolo público | `/publico/solicitacoes/:ano/:numero` | 1 (dados na 3) |
| T-F0-007 | verificar certificado + JWKS | `/publico/verificar-certificado/:hash` | 1 (hash na 4) |

## F1 — Aluno

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F1-001 | `GET /bff/dashboard/aluno` + 403 cruzado | `/dashboard` | 2 |
| T-F1-002 | `POST /auth/first-access` | `/primeiro-acesso` | 2 |
| T-F1-003 | GET/PATCH `/me`, avatar MinIO, senha, notifications, FCM, data-export | `/me` | 2 |
| T-F1-004 | inbox + marcar lido | `/comunicados` | 4 |
| T-F1-005 | **19 tipos**, draft, anexos, transitions, protocol, events | `/solicitacoes*` | 3 |
| T-F1-006 | presign + POST formativa + lista | `/formativas` | 4 |
| T-F1-007 | internships mine/create/docs | `/estagios*` | 5 |
| T-F1-008 | tccs create (prof) + PDF aluno | `/tccs*` | 5–6 |
| T-F1-009 | session + entry/exit 4 modos | `/eventos/:id/presenca` | 4 (host na 6) |
| T-F1-010 | certificates mine + download IDOR | `/certificados` | 4 |
| T-F1-011 | service-records aluno + acknowledge | `/atendimentos` | 4 |

## F2 — Egresso

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F2-001 | `GET /bff/dashboard/egresso` sem novaSolicitacao; 403 no dashboard aluno | `/dashboard` | 2 + 5 |

## F3 — Professor

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F3 | dashboard prof, CRUD evento, janelas PIN/QR, close, deliberar (reuse detalhe), publicar comunicado | `/dashboard`, `/prof/eventos*`, `/solicitacoes/:id`, `/prof/comunicado` | 2, 3, 6 |

## F4 — Comissões

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F4-001 | pool/claim/batch-review/stats CAAF (`/commissions/caaf/*`) | `/comissoes/caaf` | 6 |
| T-F4-002 | pool/assign-supervisor/bulk/stats COE + conclude internship | `/comissoes/coe`, detalhe estágio | 5–6 |

## F5 — Secretaria

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F5 | dashboard sec, fila, bulk-deliberate, on-behalf, usuarios PATCH, service-records balcão | `/dashboard`, `/solicitacoes`, `/secretaria/nova-on-behalf`, `/usuarios`, `/atendimentos` staff | 2, 3, 7 |
| T-F5-005 | colação / diploma URLs | tela graduações (staff) | 7 |
| T-F5-009 | import CSV job | `/import` | 7 |
| T-F5-010 | export CSV job | `/export` | 7 |
| T-F5-011 | `GET /reports/secretary` | `/relatorios` | 7 |
| T-F5-012 | `GET /tasks` (não `/tarefas`) | `/tarefas` | 7 |

## F6 — Coordenação

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F6-001 | GET/PATCH `/courses/{id\|sigla}/config` + 403 ownership | `/cursos/:id/config` | 7 |
| T-F6-002 | `GET /reports/coordinator` | `/relatorios` | 7 |

## F7 — Admin

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F7 | audit, FAQ CRUD, saúde/actuator se exposto | `/admin/*`, `/faq` | 7 |
| T-F7-002 | `/admin/roles`, autoridades, assign | `/admin/roles` | 7 |
| T-F7-003 | CRUD + publish `request-types` (V019) | `/admin/request-types` | 7 |
| T-F7-004 | `communication-templates` | `/admin/templates` | 7 |

## F8 — Cross

| ID | Transação | Tela | Fatia |
|----|-----------|------|-------|
| T-F8-001 | `GET /search` FGAC (aluno sem USUARIO) | `/busca` | 7 |
| T-F8-002 | `GET /faq`, tickets mine/fila/respond/close | `/faq`, `/suporte` | 4 + 7 |

## Transversais (efeito visível no harness)

| ID | O que testar no browser | Tela |
|----|-------------------------|------|
| T-10.1 | outbox enfileira após open/login/reset | `/admin/outbox?status=PENDING` e `PROCESSED` |
| T-10.4 | verify cert Ed25519 VALID vs INVALID pós-restart JVM | página pública cert |
| T-10.5 | `POST/DELETE /me/fcm-token` | `/me` |
| T-10.6 | admin outbox filtros | `/admin/outbox` |
| T-10.7 | 2º GET dashboard <60s (cache Redis) | `/dashboard` Network |

---

## 19 tipos — mesma tela `/solicitacoes/nova`

Não há 19 rotas. Aceite: cada `code` abaixo selecionável no `<select>` e capaz de gerar `POST /requests` (ou 422 de schema/anexo, que também é teste).

`ADIANTAMENTO_PERIODO` · `APROVEITAMENTO_DISCIPLINA` · `TRANCAMENTO_DISCIPLINA` · `TRANCAMENTO_PERIODO` · `COLACAO_SEM_SOLENIDADE` · `REVISAO_NOTA` · `SEGUNDA_CHAMADA` · `INCLUSAO_DISCIPLINA` · `EXCLUSAO_DISCIPLINA` · `MATRICULA_DISCIPLINA_ISOLADA` · `MATRICULA_DISCIPLINA_ELETIVA` · `APROVEITAMENTO_ESTAGIO` · `APROVEITAMENTO_ATIVIDADE_COMPLEMENTAR` · `JUSTIFICATIVA_FALTA` · `DECLARACAO_MATRICULA` · `HISTORICO_ESCOLAR` · `DIPLOMA` · `AUTORIZACAO_IMAGEM` · `ATESTADO_FREQUENCIA`

Detalhes de widget: `04-fatia-3-workflow-engine.md` §3.
