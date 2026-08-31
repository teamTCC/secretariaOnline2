# Fatia 6 — Professor, eventos host, CAAF, COE

**Objetivo da demo:** o mesmo `DynamicForm`/detalhe de solicitações da fatia 3, agora com `_links` de `request.deliberate`. Host de presença mostra PIN/QR crus. Comissões usam paths **as-built**, não os do diagrama.

**Pré-requisito:** Fatias 0–4 (aluno já abre request e formativa).  
**Oráculos:** `T-F3-professor.md` · `T-F4-001-caaf.md` · `T-F4-002-coe.md` · `T-F1-009` passos 1–2.

Login: `prof.ana@ufpr.br` / `ProfS3nh@Forte!`.  
Admin também tem quase todas as authorities — para 403 cruzado use **aluno**.

---

## 1. Whitelist

```
features/staff/
  EventosHostPage.tsx      # /prof/eventos
  EventoHostDetailPage.tsx # /prof/eventos/:id
  CaafPoolPage.tsx         # /comissoes/caaf
  CoePoolPage.tsx          # /comissoes/coe
  PublicarAvisoPage.tsx    # /prof/comunicado
```

Dashboard professor = fatia 2 (`GET /bff/dashboard/professor`).  
Deliberar request = **reusar** `SolicitacaoDetailPage` (fatia 3). Não clonar.

---

## 2. Eventos (CRUD + janelas)

Authorities: `event.manage` (CRUD), `event.host` (janela ao vivo).

```
GET  /events?host=me
POST /events
{
  "titulo": "Palestra: IA na Engenharia",
  "descricao": "…",
  "idCurso": "<TADS uuid>",
  "attendanceMode": "SECRET_SINGLE",
  "chCreditadas": 4.0,
  "inicioEm": "<ISO agora-1h>",
  "fimEm": "<ISO agora+4h>"
}
```

Ajustar `inicioEm`/`fimEm` para **agora** senão a janela abre mas regras de calendário falham.

**201** `{ id, _links.self }`. Estado inicial típico `AGENDADO`.

Abrir janela (passa a `EM_ANDAMENTO` no entity — regressão já corrigida):

```
POST /events/{id}/attendance/windows/entry
{ "durationSeconds": 900 }
```

**200** inclui `secret` (PIN 6 dígitos) e/ou `qrToken`. **Mostrar em `<pre>` gigante** — é a prova para a equipe. DUAL: também `POST .../windows/exit`.

Encerrar:

```
POST /events/{id}/close
```

Lista HATEOAS no detalhe: `abrir-janela-entrada`, `encerrar-evento` quando host + estado certo.

QR_DUAL / SECRET_DUAL: dois POSTs de janela; aluno confirma entry **e** exit (fatia 4).

Não geolocalizar. QR = texto do `qrToken` (canvas opcional de 10 linhas). Criar **pelo menos um evento de cada** `attendanceMode` (quatro POSTs) para a fatia 4 fechar T-F1-009.

---

## 3. Deliberar solicitações (reuso)

`GET /bff/dashboard/professor` → `solicitacoesPendentes` com `_link`.

Abrir `SolicitacaoDetailPage`. Rel `assign` → `DEFER`/`DENY`. Mesmo `POST /transitions`.

Professor **sem** `request.deliberate` não vê os rels. Se o seed do prof não deliberar declaração, usar admin/secretaria para o smoke do engine e o prof para eventos.

---

## 4. CAAF (`formative.review`)

Diagrama antigo vs código:

| Diagrama | Usar |
|----------|------|
| `GET /commissions/caaf/dashboard` | `GET /commissions/caaf/pool?page=&size=` |
| `POST /commissions/caaf/assign` | `POST /commissions/caaf/{activityId}/claim` |
| batch | `POST /commissions/caaf/batch-review` |
| stats | `GET /commissions/caaf/stats` |

Pré: formativa `PENDENTE` sem revisor (fatia 4).

Pool 200 `content[]`: `id`, `idAluno`, `titulo`, `categoria`, `cargaHoraria`.  
Claim + review: bodies no HTTPie T-F4-001 (aprovar/rejeitar). Sem authority → 403.

UI: tabela nativa + botão claim se `_links` / se o tutorial manda POST claim (se o pool não mandar rel, nesta versão de testes um botão “claim id colado” + ProblemBanner ainda prova o FGAC). Preferir `_links` se existirem.

---

## 5. COE (`internship.review`)

| Uso | Path |
|-----|------|
| Pool | `GET /commissions/coe/pool` |
| Supervisor | `POST /commissions/coe/{internshipId}/assign-supervisor` `{ idSupervisor }` |
| Lote | `POST /commissions/coe/bulk-assign` |
| Stats | `GET /commissions/coe/stats` |

Não existe “aprovar estágio em lote”. Conclude individual: `POST /internships/{id}/conclude`.

`idSupervisor` = UUID do professor (`GET /usuarios?email=prof.ana@ufpr.br`).

---

## 6. Comunicado

`communication.publish_class` + `cursoId`, ou admin `communication.publish`.

Path de publish: conferir SpringDoc (`POST /communications`). Body mínimo no T-F3 / T-F1-004 passo 3. Aluno vê em `GET /communications/me` (fatia 4).

---

## 7. Aceite

- [ ] Dashboard prof 200; aluno nesse path 403
- [ ] POST event + open window: JSON com `secret`; estado evento `EM_ANDAMENTO`
- [ ] Aluno (outra aba) confirma PIN → 200
- [ ] Close evento → CONCLUÍDO; certificado pode nascer (fatia 4)
- [ ] CAAF pool lista a formativa PENDENTE; aluno no pool → 403
- [ ] COE pool lista internship; assign-supervisor 200

## 8. Não fazer

- Segunda cópia de `SolicitacaoDetailPage`.
- Paths `/commissions/caaf/dashboard`.
- UI de QR animada.
