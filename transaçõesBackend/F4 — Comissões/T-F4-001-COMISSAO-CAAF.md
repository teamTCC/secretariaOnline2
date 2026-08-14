# T-F4-001 — Pool CAAF: Atribuir e Aprovar Atividades Formativas em Lote

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F4 — Comissões/US-F4-001-COMISSAO-CAAF.md`](../../foundationDocs/sequenceDiagrams/F4 — Comissões/US-F4-001-COMISSAO-CAAF.md)  
> **Status:** ✅ Implementado — pool, self-assign, batch-review e stats

---

## O que os diagramas especificam

### F4.1a — `GET /commissions/caaf/dashboard` (Carregar pool)

```
GET /commissions/caaf/dashboard
Authorization: Bearer eyJhbGci...  (hasAuthority('formative.review'))
```

Retorna KPIs do pool coletivo e lista de atividades formativas filtrando: `assignee IS NULL` OU `assignee = userId`, scopo por `commission_member.curso_id`, `estado NOT IN ('APROVADA', 'REJEITADA')`.

**JSON de saída (200):**

```json
{
  "kpis": {
    "poolTotal": 47,
    "assignedToMe": 12,
    "avgDeadlineDays": 4.3,
    "approvedToday": 5
  },
  "items": [
    {
      "id": "3d9f1b2a-...",
      "alunoNome": "Carlos Mendes",
      "tipo": "EVENTO_INTERNO_PRESENCA_VALIDADA",
      "titulo": "Workshop de React",
      "chSolicitada": 4.0,
      "assigneeId": null,
      "assigneeNome": null,
      "_links": {
        "assign-member": "/commissions/caaf/assign"
      }
    }
  ],
  "_links": {
    "self": "/commissions/caaf/dashboard"
  }
}
```

> **Regra FGAC:** `_links.assign-member` só está presente quando `assigneeId IS NULL` ou `assigneeId = userId` do requester (HATEOAS cego).

---

### F4.1b — `POST /commissions/caaf/assign` (Self-assign)

```
POST /commissions/caaf/assign
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "itemId": "3d9f1b2a-...",
  "assigneeId": "7c9e6679-..."  // próprio userId
}
```

Transação atômica: `UPDATE formative_entry SET assignee_id=self, estado='EM_REVISAO'` + `INSERT outbox_event(type='formativas.assigned')`.

**JSON de saída (200):**

```json
{
  "id": "3d9f1b2a-...",
  "estado": "EM_REVISAO",
  "assigneeId": "7c9e6679-...",
  "assigneeNome": "Prof. Ana Lima",
  "_links": {
    "review": "/formativas/3d9f1b2a-.../review"
  }
}
```

---

### F4.1c — `GET /commissions/caaf/members` + Atribuir a colega

```
GET /commissions/caaf/members?cursoId=tads
Authorization: Bearer eyJhbGci...
```

**JSON de saída (200):**

```json
{
  "members": [
    { "id": "abc-...", "nome": "Prof. Ana Lima", "load": 12 },
    { "id": "def-...", "nome": "Prof. Carlos Braga", "load": 3 }
  ]
}
```

`load` = `COUNT(*) WHERE assignee_id = member.id AND estado = 'EM_REVISAO'`

Depois, mesmo `POST /commissions/caaf/assign` com `assigneeId = colega.id`.

---

### F4.1d — `POST /commissions/caaf/batch-decide` (Aprovação em lote)

```
POST /commissions/caaf/batch-decide
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "ids": ["3d9f1b2a-...", "5f7e9c1b-...", "1a2b3c4d-..."],
  "decisao": "APROVADA"
}
```

**Pré-condição:** todos os itens devem ser do tipo `EVENTO_INTERNO_PRESENCA_VALIDADA` (validado no use case, antes de qualquer TX).

Transação única: atualiza estado das atividades pendentes; se `APROVAR`, grava `formative_entry` por atividade (horas no KPI do aluno) + 1 `outbox_event(type='formativas.batch_revisada')`.

**JSON de saída (200):**

```json
{
  "approved": 3,
  "certsPending": 3,
  "_links": {
    "self": "/commissions/caaf/batch-decide"
  }
}
```

O `outbox_event` dispara `CertificateIssuerUseCase` por aluno (→ [T-10.4-CERTIFICADO](../transversal/T-10.4-CERTIFICADO.md)).

---

### F4.1e — Erro 403 (authority ausente ou scope violation)

**Cenário A — sem `formative.review`:**

```json
HTTP/1.1 403 Forbidden
{
  "type": "access_denied",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Capability formative.review ausente."
}
```

**Cenário B — violação de escopo de curso:**

```json
HTTP/1.1 403 Forbidden
{
  "type": "course_scope_violation",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Item fora do escopo da sua comissão."
}
```

O scope check ocorre na camada do use case (após query ao banco) — defesa em profundidade além do `@PreAuthorize`.

---

### F4.1f — Erro 422 (batch com tipos mistos)

```json
HTTP/1.1 422 Unprocessable Entity
{
  "type": "incompatible_activity_type",
  "title": "Tipos incompatíveis",
  "status": 422,
  "detail": "Batch só suporta EVENTO_INTERNO_PRESENCA_VALIDADA.",
  "invalidIds": ["1a2b3c4d-..."]
}
```

Validação antes de qualquer `BEGIN TX` — sem efeito no banco.

---

## Relação com módulos existentes

| Fluxo | Módulo existente | Status |
|-------|-----------------|--------|
| Revisão individual (`/formativas?to=me`) | [`FormativasController.kt`](../../backend/modules/formativas/src/main/kotlin/br/ufpr/sept/so2/modules/formativas/api/FormativasController.kt) | ✅ Implementado |
| Dispatch outbox | [`OutboxDispatcher.kt`](../../backend/modules/notificacoes/src/main/kotlin/br/ufpr/sept/so2/modules/notificacoes/OutboxDispatcher.kt) | ✅ Implementado |
| Emissão certificado pós-batch | `CertificateIssuerUseCase` | ⏳ Pendente |

---

## Arquivo implementado

| Papel | Arquivo |
|-------|---------|
| Controller CAAF | [`formativas/api/CommissionsCaafController.kt`](../../backend/modules/formativas/src/main/kotlin/br/ufpr/sept/so2/modules/formativas/api/CommissionsCaafController.kt) |

---

## Endpoints implementados

| Endpoint | Função |
|----------|--------|
| `GET /commissions/caaf/pool` | Pool pendentes sem revisor |
| `POST /commissions/caaf/{id}/claim` | Self-assign |
| `POST /commissions/caaf/batch-review` | Batch aprovar/rejeitar |
| `GET /commissions/caaf/stats` | KPIs do pool |

---

## Checklist de Verificação

- [x] `GET /commissions/caaf/pool` → lista atividades PENDENTE sem revisor
- [x] `POST /commissions/caaf/{id}/claim` → TX atômica: set `idRevisor = me`
- [x] `POST /commissions/caaf/batch-review` → N updates em 1 TX + `formative_entry` se APROVAR + certificado PDF + outbox `formativas.batch_revisada`
- [x] `GET /commissions/caaf/stats` → `{ totalPendente, aprovadasHoje, rejeitadasHoje }`
- [x] 403 sem `formative.review` → `access_denied`
