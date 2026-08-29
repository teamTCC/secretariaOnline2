# T-F5 — Secretaria: índice e fluxos transversais

> **Diagramas de referência:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/`](../../foundationDocs/sequenceDiagrams/F5 — Secretaria/)  
> **Status:** ✅ Dashboard, fila, alunos, atendimentos, diplomas, import/export CSV, kanban e estatísticas

Tutoriais específicos:

| HU | Tutorial |
|----|----------|
| F5.5 Egressos / diplomas | [T-F5-005-EGRESSOS-DIPLOMAS.md](T-F5-005-EGRESSOS-DIPLOMAS.md) |
| F5.9 Importação CSV | [T-F5-009-IMPORTACOES.md](T-F5-009-IMPORTACOES.md) |
| F5.10 Exportações | [T-F5-010-EXPORTACOES.md](T-F5-010-EXPORTACOES.md) |
| F5.11 Estatísticas | [T-F5-011-ESTATISTICAS.md](T-F5-011-ESTATISTICAS.md) |
| F5.12 Tarefas (kanban) | [T-F5-012-TAREFAS.md](T-F5-012-TAREFAS.md) |
| F1.11 Atendimentos | [T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md](../F1 — Aluno/T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md) |

---

## F5.1 — Dashboard da Secretaria

> **Arquivos:** [`DashboardSecretariaController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/DashboardSecretariaController.kt) + [`DashboardSecretariaQuery.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/application/DashboardSecretariaQuery.kt)  
> **Ver também:** [T-F1-001-DASHBOARD](../F1 — Aluno/T-F1-001-DASHBOARD.md)

```
GET /bff/dashboard/secretaria
Cookie: access_token=…   (hasAuthority('dashboard.view_secretary'))
```

```json
{
  "kpis": {
    "emTriagem": 12,
    "emDeliberacao": 7
  },
  "_links": {
    "self": "/bff/dashboard/secretaria",
    "solicitacoes": "/requests",
    "usuarios": "/usuarios"
  }
}
```

KPIs usam `requestRepo.countByEstado("ABERTA"|"EM_DELIBERACAO")` — total real, não a primeira página.

---

## F5.2 — Fila de Solicitações

A secretaria usa o mesmo `RequestController` com `request.view_curso` (vê todas as solicitações do curso):

```
GET /requests?estado=ABERTA&page=0&size=20
GET /requests?type=AUTORIZACAO_IMAGEM
Authorization: Bearer …  (hasAuthority('request.view_curso'))
```

`type` é alias de `typeCode`. Itens `ABERTA` recebem `_links.bulk_deliberate` quando o ator tem `request.deliberate` ou `image_authorization.review`.

### Deliberação em lote (F5.6 — autorização de imagem)

```
PATCH /requests/bulk-deliberate
Authorization: Bearer …  (request.deliberate | image_authorization.review)
Content-Type: application/json

{
  "ids": ["uuid-1", "uuid-2"],
  "action": "DEFER",
  "parecer": "Autorização deferida em lote"
}
```

- All-or-nothing (`@Transactional`): se uma transição falhar, **409 Conflict** e rollback.
- Cada item reusa `TransitionRequestUseCase` (outbox `solicitacoes.{action}` por solicitação).

---

## F5.3 — Gestão de Alunos (IAM)

[`iam/api/UsuariosController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/UsuariosController.kt) — capability `user.manage_students` / `user.manage_all`.

| Endpoint | Capability | Status |
|----------|-----------|--------|
| `GET /usuarios` | `user.manage_students` | ✅ |
| `POST /usuarios` | `user.manage_students` | ✅ (outbox `iam.usuario_criado`) |
| `GET /usuarios/{id}` | `user.manage_students` | ✅ |
| `PATCH /usuarios/{id}/status` | `user.manage_students` | ✅ |
| `POST /usuarios/{id}/reset-password` | `user.reset_password` | ✅ |

---

## F5.4 — Dados Acadêmicos

| Endpoint | Status |
|----------|--------|
| `GET /academico/cursos` | ✅ `AcademicoController` |
| `GET /academico/disciplinas` | ✅ `AcademicoController` |
| CRUD coordenação | ✅ ver [T-F6-001](../F6 — Coordenação/T-F6-001-CONFIGURAR-CURSO.md) |

---

## F5.13 — Atendimentos (secretaria registra)

```
POST /service-records
Authorization: Bearer …  (user.manage_students)

{ "idAluno": "uuid", "assunto": "Revisão de matrícula", "tipo": "PRESENCIAL" }
```

- Estado inicial: `PENDENTE_CIENCIA`
- Mesma TX: `outbox_event` `atendimentos.created` + `audit_log` `SERVICE_RECORD_CREATED`
- O aluno dá ciência em [T-F1-010-011](../F1 — Aluno/T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md)

---

## Checklist de Verificação

- [x] `GET /bff/dashboard/secretaria` → `200` com KPIs reais
- [x] `GET /requests` com `request.view_curso` → vê todas as solicitações
- [x] `PATCH /requests/bulk-deliberate` → 200 ou 409 com rollback
- [x] `GET/POST /usuarios` + `PATCH /usuarios/{id}/status`
- [x] `POST /service-records` → 201 + outbox
- [x] Diplomas, import, export, estatísticas, kanban — tutoriais F5.005–012
