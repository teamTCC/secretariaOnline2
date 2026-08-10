# T-F4-001 — Pool CAAF: Atribuir e Aprovar Atividades Formativas em Lote

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F4 — Comissões/US-F4-001-COMISSAO-CAAF.md`](../../foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-001-COMISSAO-CAAF.md)  
> **Status:** ⏳ Não implementado — controllers CAAF específicos não existem; `FormativasController` cobre revisão individual, mas não o pool coletivo

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

Transação única: `UPDATE N registros + INSERT N formative_entry_event_logs + INSERT 1 outbox_event(type='formativas.batch_approved')`.

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

## O que precisa ser implementado

| Arquivo a criar | Descrição |
|----------------|-----------|
| `modules/formativas/api/CAAFController.kt` | Pool CAAF: dashboard, members, assign, batch-decide |
| `modules/formativas/application/GetCAAFDashboardUseCase.kt` | KPIs + pool filtering por scope |
| `modules/formativas/application/AssignFormativeUseCase.kt` | Self-assign e assign-to-colleague |
| `modules/formativas/application/GetCAAFMembersUseCase.kt` | Lista membros com carga |
| `modules/formativas/application/BatchDecideFormativesUseCase.kt` | Aprovação em lote com guard de tipo |
| Migração | `commission_members(user_id, commission_id, curso_id)` |

---

## Checklist de Verificação

- [ ] `GET /commissions/caaf/dashboard` → `200` com kpis e items filtrados por scope
- [ ] `POST /commissions/caaf/assign` → `200`, TX atômica update + outbox_event
- [ ] `GET /commissions/caaf/members` → `200` com load calculado
- [ ] `POST /commissions/caaf/batch-decide` → `200`, todos os N updates em 1 TX
- [ ] 403 Cenário A: sem `formative.review` → `access_denied`
- [ ] 403 Cenário B: item fora de escopo de curso → `course_scope_violation`
- [ ] 422: tipos mistos no batch → `incompatible_activity_type` com `invalidIds`
- [ ] `_links.assign-member` ausente quando item já está com outro assignee
