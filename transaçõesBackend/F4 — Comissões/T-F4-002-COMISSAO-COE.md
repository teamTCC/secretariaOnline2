# T-F4-002 — Pool COE: Atribuir e Acompanhar Estágios em Lote

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F4 — Comissões/US-F4-002-COMISSAO-COE.md`](../../foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-002-COMISSAO-COE.md)  
> **Status:** ⏳ Não implementado — padrão análogo ao CAAF (T-F4-001), mas para o domínio de estágios

---

## Diferenças fundamentais em relação ao CAAF

| Aspecto | CAAF | COE |
|---------|------|-----|
| Domínio | Atividades formativas | Estágios |
| Capability | `formative.review` | `internship.review` |
| Batch Approve | ✅ Sim (`batch-decide`) | ❌ Não (pareceres individuais, juridicamente sensíveis) |
| Notificação extra | Somente ao professor destinatário | **Também ao aluno** (orientador foi definido) |
| document_due_date | N/A | Sim — prazo do documento mais antigo sem parecer |

---

## O que os diagramas especificam

### F4.2a — `GET /commissions/coe/dashboard`

```
GET /commissions/coe/dashboard
Authorization: Bearer eyJhbGci...  (hasAuthority('internship.review'))
```

Lista estágios ativos (`estado != 'CONCLUIDO'`), no escopo do curso do COE. Cada item inclui `document_due_date` para coloração de alerta no frontend.

**JSON de saída (200):**

```json
{
  "kpis": {
    "poolTotal": 22,
    "assignedToMe": 7,
    "docsOverdue": 3,
    "avgSlaDays": 5.2
  },
  "items": [
    {
      "id": "a1b2c3d4-...",
      "alunoNome": "Lucas Ferreira",
      "tipo": "ESTAGIO_OBRIGATORIO",
      "empresa": "TechCorp Ltda.",
      "assigneeId": null,
      "assigneeNome": null,
      "documentDueDate": "2026-07-30",
      "_links": {
        "assign-member": "/commissions/coe/assign"
      }
    }
  ],
  "_links": {
    "self": "/commissions/coe/dashboard"
  }
}
```

> `document_due_date` = `internship_document.review_due_date WHERE reviewed_at IS NULL ORDER BY review_due_date ASC LIMIT 1`. Se `null`, nenhum documento pendente.

> Coloração `status/danger` quando `document_due_date < now()` é **lógica do frontend** — sem chamada extra ao backend.

---

### F4.2b — Self-assign com notificação ao aluno

```
POST /commissions/coe/assign
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "internshipIds": ["a1b2c3d4-..."],
  "assigneeId": "7c9e6679-..."  // próprio userId
}
```

> Note: o endpoint aceita `internshipIds: UUID[]` para suportar atribuição em lote (F4.2d). Com 1 elemento, comportamento é igual ao self-assign.

**Transação atômica:**

```sql
BEGIN;
UPDATE internship SET assignee_id=:self, assigned_at=now() WHERE id=:internshipId;
INSERT INTO outbox_event(type='estagios.assigned', payload={assigneeId, alunoId, ...});
COMMIT;
```

**JSON de saída (200):**

```json
{
  "id": "a1b2c3d4-...",
  "assigneeId": "7c9e6679-...",
  "assigneeNome": "Prof. Mariana Souza",
  "_links": {
    "review": "/estagios/a1b2c3d4-.../documents"
  }
}
```

**Payload outbox — `estagios.assigned`:**

O dispatcher gera **dois destinatários**:
- `assigneeId = self` → filtrado como ator da ação (sem auto-notificação)
- `alunoId` → push/email: "Seu orientador de estágio foi definido: Prof. Mariana Souza"

> Diferença chave vs CAAF: no COE o **aluno** é notificado no momento da atribuição.

---

### F4.2c — `GET /commissions/coe/members` + Atribuir a orientador

```
GET /commissions/coe/members?cursoId=tads
Authorization: Bearer eyJhbGci...
```

**JSON de saída (200):**

```json
{
  "members": [
    { "id": "abc-...", "nome": "Prof. Mariana Souza", "load": 7 },
    { "id": "def-...", "nome": "Prof. Ricardo Nunes", "load": 2 }
  ]
}
```

`load` = `COUNT(*) WHERE assignee_id = member.id AND estado IN ('EM_ANDAMENTO', 'AGUARDANDO_DOC')`

Atribuição ao colega segue o mesmo `POST /commissions/coe/assign` com `assigneeId = colega.id`. Dispatcher notifica: orientador ("Novo estágio atribuído") + aluno ("Orientador definido").

---

### F4.2d — Atribuição em lote via BulkActionBar

```
POST /commissions/coe/assign
Content-Type: application/json

{
  "internshipIds": ["a1b2c3d4-...", "b2c3d4e5-...", "c3d4e5f6-..."],
  "assigneeId": "def-..."
}
```

**Transação única com N outbox_events individuais** (um por estágio, fan-out correto para cada aluno):

```sql
BEGIN;
UPDATE internship SET assignee_id=:assigneeId WHERE id IN (:ids);
INSERT INTO outbox_event VALUES (...) -- N linhas, uma por estágio
COMMIT;
```

**JSON de saída (200):**

```json
{
  "assigned": 3,
  "_links": { "self": "/commissions/coe/assign" }
}
```

> **`DS/BulkActionBar` do COE não tem "Aprovar selecionados"** — decisão intencional. Pareceres de estágio são juridicamente sensíveis e sempre individuais.

---

### F4.2e — Erro 403 (sem `internship.review` ou violação de scope)

**Cenário A — sem `internship.review`:**

```json
HTTP/1.1 403 Forbidden
{
  "type": "access_denied",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Capability internship.review ausente."
}
```

**Cenário B — estágio de curso fora do escopo do COE:**

```json
HTTP/1.1 403 Forbidden
{
  "type": "course_scope_violation",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Estágio fora do escopo do COE."
}
```

Padrão idêntico ao F4.1e — apenas nomes de capability/domain diferem.

---

## Relação com módulos existentes

| Fluxo | Arquivo existente | Status |
|-------|------------------|--------|
| Revisão individual por documento | Controller de estágio | ⏳ Pendente |
| Dispatch outbox | [`OutboxDispatcher.kt`](../../backend/modules/notificacoes/src/main/kotlin/br/ufpr/sept/so2/modules/notificacoes/OutboxDispatcher.kt) | ✅ Implementado |

---

## O que precisa ser implementado

| Arquivo a criar | Descrição |
|----------------|-----------|
| `modules/estagio/api/COEController.kt` | Pool COE: dashboard, members, assign (individual + bulk) |
| `modules/estagio/application/GetCOEDashboardUseCase.kt` | KPIs + pool com `document_due_date` |
| `modules/estagio/application/AssignInternshipUseCase.kt` | Self-assign, assign-to-colleague, bulk |
| `modules/estagio/application/GetCOEMembersUseCase.kt` | Lista orientadores com carga ativa |
| Migração | `commission_members`, `internship_document(review_due_date)` |

---

## Checklist de Verificação

- [ ] `GET /commissions/coe/dashboard` → `200` com kpis, items e `document_due_date`
- [ ] `POST /commissions/coe/assign` (array com 1 elemento) → TX atômica, 2 outbox_events (orientador + aluno)
- [ ] `POST /commissions/coe/assign` (array com N) → N outbox_events individuais em 1 TX
- [ ] `GET /commissions/coe/members` → load calculado apenas para estados ativos
- [ ] 403 sem `internship.review` → `access_denied`
- [ ] 403 curso fora de escopo → `course_scope_violation`
- [ ] Sem endpoint `batch-decide` (não existe, intencional)
