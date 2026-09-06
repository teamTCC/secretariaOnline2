# T-F3 — Professor: Dashboards, Eventos, Deliberação, Formativas

> **Diagramas de referência:** [`foundationDocs/sequenceDiagrams/F3 — Professor/`](../../foundationDocs/sequenceDiagrams/F3 — Professor/)  
> **Status:** ✅ Implementado (Dashboard, Eventos, Deliberação, Formativas, Estágio, TCC, Comunicados)

---

## F3.1 — Dashboard do Professor

> **Arquivos:** [`DashboardProfessorController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/DashboardProfessorController.kt) + [`DashboardProfessorQuery.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/application/DashboardProfessorQuery.kt)  
> **Ver também:** [T-F1-001-DASHBOARD](../F1 — Aluno/T-F1-001-DASHBOARD.md) (mesmo padrão slim controller + query)

```
GET /bff/dashboard/professor
Cookie: access_token=…   (hasAuthority('dashboard.view_self_professor'))
```

```json
{
  "meusEventos": [
    {
      "id": "7c9e6679-...",
      "titulo": "Palestra: IA",
      "estado": "EM_ANDAMENTO",
      "inicioEm": "2026-08-10T14:00:00Z",
      "fimEm": "2026-08-10T18:00:00Z"
    }
  ],
  "solicitacoesPendentes": [
    {
      "id": "550e8400-...",
      "tipo": "APROVEITAMENTO_DISCIPLINA",
      "prazoEm": "2026-08-25T23:59:59Z",
      "_link": "/requests/550e8400-..."
    }
  ],
  "_links": {
    "self": "/bff/dashboard/professor",
    "novoEvento": "/events",
    "meusEventos": "/events?host=me"
  }
}
```

---

## F3.2 — Gestão de Eventos (CRUD + Janelas + Encerramento)

> **Ver:** [T-F1-009-PRESENCA](../F1 — Aluno/T-F1-009-PRESENCA.md) — o professor usa os mesmos endpoints do `EventAttendanceController`

### Principais capabilities do professor

| Authority | Endpoints |
|-----------|-----------|
| `event.manage` | `POST /events`, `PATCH /events/{id}`, `GET /events` |
| `event.host` | `POST /events/{id}/attendance/windows/entry`, `/exit`, `POST /events/{id}/close` |

---

## F3.3 — Deliberar Solicitações

O professor delibera solicitações usando o mesmo `RequestController` do aluno, mas com authority `request.deliberate`:

```json
POST /requests/{id}/transitions
Authorization: Bearer eyJhbGci...  (hasAuthority('request.deliberate'))

{
  "action": "DEFERIR",
  "parecer": "Aprovado conforme PPC 2022."
}
```

> **Ver:** [T-F1-005-SOLICITACOES](../F1 — Aluno/T-F1-005-SOLICITACOES.md) — seção "Aplicar Transição"

---

## F3.4 — Revisar Horas Formativas (CAAF)

O professor membro da CAAF usa o mesmo `FormativasController` com `formative.review`:

```json
PATCH /formativas/{id}/review
Authorization: Bearer eyJhbGci...  (hasAuthority('formative.review'))

{
  "acao": "APROVAR",
  "parecer": "Comprovante válido."
}
```

> **Ver:** [T-F1-006-FORMATIVAS](../F1 — Aluno/T-F1-006-FORMATIVAS.md) — seção "Revisar Atividade"

---

## F3.5, F3.6, F3.7 — Estágio, TCC e Publicar Comunicado

| HU | Status | Próximo passo |
|----|--------|---------------|
| F3.5 — Orientação de Estágio | ⏳ Stub | Implementar `EstagioController` |
| F3.6 — Orientação de TCC | ⏳ Stub | Implementar `TccController` |
| F3.7 — Publicar Comunicado | ⏳ Stub | Implementar `ComunicacaoController` |

---

## Checklist de Verificação

- [x] `GET /bff/dashboard/professor` → `200` com eventos e pendências
- [x] `POST /events` com `event.manage` → criar evento
- [x] `POST /events/{id}/attendance/windows/entry` com `event.host` → abrir janela
- [x] `POST /requests/{id}/transitions` com `request.deliberate` → deliberar
- [x] `PATCH /formativas/{id}/review` com `formative.review` → revisar formativa
- [ ] `POST /communications` com `communication.publish` — **não implementado**
- [ ] Orientação de estágio/TCC — **não implementado**
