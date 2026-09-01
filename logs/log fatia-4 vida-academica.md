# Log Fatia 4 — Vida acadêmica (formativas, presença, certificados, atendimentos, hub)

**Quando:** 2026-08-31 / 01-09 (UTC) — caixa-branca + HTTP/UI contra API real, com correção de bugs de front  
**Contrato:** `FatiasFrontend/05-fatia-4-vida-academica.md`  
**Oráculos:** `T-F1-004`, `T-F1-006`, `T-F1-009`, `T-F1-010-011`  
**SPA:** `http://localhost:5173` (`frontend-web/`)  
**API:** `http://localhost:8080`  
**Dumps HTTP:** `logs/raw/fatia-4/`

**Resultado (reteste ~23:09 BRT):** aceite §7 **passa**. Front (7 páginas + kernel) + back (`_links.read` no inbox; sessão de presença só emite `confirmar-*` com janela ativa). GitNexus: SPA untracked — impacto UNKNOWN. `myInbox` / `attendanceSession` risco **LOW**.

Delegating to: frontend-engineer, backend-architect, security-engineer. GitNexus: SPA não indexada.

---

## 0) O que foi entregue

| Rota | Arquivo | Contrato |
|------|---------|----------|
| `/formativas` | `FormativasPage.tsx` | presign MinIO + POST `/formativas` + GET `/formativas/minhas` + resumo |
| `/eventos` | `EventosAlunoPage.tsx` | GET `/events?audience=me` + paste `eventId` |
| `/eventos/:id/presenca` | `EventoSessaoPage.tsx` | sessão HATEOAS + PIN/qrToken + force entry/exit/qr |
| `/certificados` | `CertificadosPage.tsx` | mine + download + verify + IDOR |
| `/atendimentos` | `AtendimentosPage.tsx` | POST agendar + GET me/alias + `_links.acknowledge` |
| `/comunicados` | `InboxPage.tsx` | GET me + unread-count + PATCH `_links.read` |
| `/faq` e `/suporte` | `FaqPage.tsx` | GET `/faq` (V013) + POST `/support/tickets` + GET mine |

Kernel: `queryKeys`, `hateoas.uiPathFromHref`, `Shell` nav, `router`, `AttachmentUpload.presignPath`.  
`deviceUuid` estável em `localStorage` (`so2.deviceUuid`) — **não** é JWT.

Back (contrato):

- Inbox: `CommunicationDeliveryResponse._links` + `read` só se `readAt == null`.
- Presença: `EventAttendanceQuery.attendanceSession` emite `confirmar-entrada` / `confirmar-saida` **só** com janela JSONB ativa (não basta `EM_ANDAMENTO`).

---

## 1) Caixa-preta HTTP (aluno `ana.aluno@ufpr.br`)

Prof `prof.ana@ufpr.br` cria evento/abre janela; secretaria `secretaria@ufpr.br` registra balcão.

| Caso | Status |
|------|--------|
| `GET /faq` (seed V013) | **200** |
| `POST /support/tickets` `{assunto, descricao}` | **201** `ABERTO` |
| `GET /support/tickets/mine` | **200** |
| Formativa presign + PUT MinIO + `POST /formativas` | **200** / **200** / **201** `PENDENTE` |
| `GET /formativas/minhas` + resumo | **200** |
| `POST /me/service-records` AGENDAMENTO | **201** `AGENDADO` |
| Sec `POST /service-records` balcão | **201** `PENDENTE_CIENCIA` |
| Alias `GET /service-records?aluno=me&status=PENDENTE_CIENCIA` | **200** + `_links.acknowledge` |
| `POST …/acknowledge` | **200** `CIENTE` |
| Inbox `GET /communications/me` | **200** `_links.read` |
| `PATCH` read pelo href | **204**; `readAt` preenchido; unread **14→13** |
| Evento `AGENDADO` + POST entry | **409** “Evento não está em andamento” |
| Sessão sem janela | `_links` só `self` |
| Sessão com janela ENTRY | `confirmar-entrada` |
| `SECRET_SINGLE` / `SECRET_DUAL` / `QR_SINGLE` / `QR_DUAL` entry | **200** |
| DUAL exit (SECRET + QR) | **200** |
| `GET /certificates/mine` + download-url próprio | **200** |
| UUID inexistente download-url | **404** |
| Egresso `ana.egressa` no cert do aluno (`472680e5-…`) | **403** |
| Verify público (mesma JVM) | **200** `ED25519_VALID` |

Os 4 `attendanceMode` fecham o evento após presença completa → certificados emitidos.

---

## 2) Caixa-preta UI (harness 5173, aluno logado)

| Fluxo | Prova |
|-------|--------|
| Formativas POST | `PENDENTE` na lista (HTTP + UI anterior) |
| Eventos sessão AGENDADO + force entry | `ProblemBanner` **409** |
| Certificados download próprio | `downloadUrl` MinIO TTL 900s |
| Certificados UUID fake | **404** no JsonPanel IDOR |
| Verify (botão + rota pública) | `ED25519_VALID` |
| Inbox PATCH `read` | unread **13→12**; `readAt` deixa de ser null |
| FAQ seed | 8 perguntas V013 visíveis |
| POST ticket UI | **201** `Fatia 4 UI ticket` / `ABERTO`; mine totalElements **2** |
| Atendimentos POST agendar | **201** `AGENDADO` `c1f35ee3-…` |
| Acknowledge UI (balcão `a0150c8a-…`) | **200** `CIENTE`; botão `acknowledge` some |

---

## 3) Bugs de front encontrados e corrigidos

### B1 — Inbox inventava path de read

`readHref` caía em `/communications/deliveries/{id}/read` mesmo sem `_links`. Spec: seguir `_links`, não inventar path.

**Fix:** só `hrefOf(links, 'read' | 'marcar-lido')`. Botão `PATCH read` some depois de `readAt`.

**Prova:** item já lido (`bb11a473-…`) só tem GET detalhe; unread com `_links.read` tem HateoasBar `read` + PATCH.

### B2 — `ProblemBanner` de certificados ficava preso no 404 do IDOR

Cadeia `mine → download → tryIdor → verify`: após IDOR 404, verify **200** não limpava o banner.

**Fix:** `lastProblem` — mutação com sucesso zera; erro sobrescreve.

**Prova (reteste):** IDOR → banner “Recurso não encontrado (404)”; click `verify` → banner **null** + `ED25519_VALID`; download próprio não reabre o 404.

### B3 — copy IDOR dizia só “403”

UUID inexistente é **404**; cert de outro titular é **403**.

**Fix:** texto da página distingue os dois. Prova HTTP: fake UUID 404; egresso no cert `472680e5-…` **403**.

---

## 4) Bugs de backend corrigidos (antes do reteste UI)

| Bug | Fix | Prova |
|-----|-----|--------|
| Inbox sem `_links.read` | `CommunicationDeliveryResponse.links` + `myInbox` emite `read` se `readAt == null` | PATCH 204; após read o rel some |
| Sessão `EM_ANDAMENTO` emitia `confirmar-*` sem janela ativa | `hasActiveWindow` no JSONB | sem janela: só `self`; com ENTRY: `confirmar-entrada`; DUAL+exit: `confirmar-saida` |

GitNexus impacto desses símbolos: **LOW**.

---

## 5) Aceite §7 (após correção)

1. Formativa `PENDENTE` na lista aluno — **PASS** (`53c909a6-…`)
2. Sem janela: sessão sem rel confirmar; com janela + PIN/QR: 200 + `entryConfirmedAt` — **PASS**
3. Check-in evento AGENDADO: **409** — **PASS**
4. Certificado próprio download **200**; id alheio **403**; UUID fake **404** — **PASS**
5. Verify público pelo hash — **PASS** `ED25519_VALID`
6. FAQ **200** com seed V013 — **PASS**
7. Inbox **200**; marcar lido altera `readAt` — **PASS** (HTTP 14→13; UI 13→12)
8. POST ticket + GET mine — **PASS** (HTTP + UI `6793800d-…`)
9. Os 4 `attendanceMode` confirmam (entry; DUAL também exit) — **PASS**

---

## 6) Não-bugs / notas

- Ed25519 é **efêmera no restart da JVM** → cert de outra JVM `INVALID`.
- Secretaria/coord com `event.manage` pode baixar cert de aluno (**200** by design); IDOR é titular vs outro perfil sem essa authority.
- Dump `evt-create-SECRET_SINGLE.txt` colidiu de nome (AGENDADO + SECRET_SINGLE); IDs continuam nos JSON `_evt-*.json`.
- Publicar comunicado / abrir janela como prof: fatia 6. Nesta fatia o harness do aluno cola PIN/QR do HTTPie.
- CAAF (review formativa): fatia 6. Balcão secretaria CRUD extra: fatia 7.
