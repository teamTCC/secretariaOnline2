# T-F5 — Secretaria: Dashboard, Solicitações, Gestão de Alunos

> **Diagramas de referência:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/`](../../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/)  
> **Status:** ✅ Dashboard implementado | ✅ Solicitações via RequestController | ⏳ Demais módulos stub

---

## F5.1 — Dashboard da Secretaria

> **Ver:** [T-F1-001-DASHBOARD](../F1%20—%20Aluno/T-F1-001-DASHBOARD.md) — seção "Dashboard da Secretaria"

```
GET /bff/dashboard/secretaria
Authorization: Bearer eyJhbGci...  (hasAuthority('dashboard.view_secretary'))
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

---

## F5.2 — Fila de Solicitações da Secretaria

A secretaria usa o mesmo `RequestController` com authority `request.view_curso` (vê todas as solicitações do curso, não apenas as suas):

```
GET /requests?estado=ABERTA&page=0&size=20
Authorization: Bearer eyJhbGci...  (hasAuthority('request.view_curso'))
```

Por ter `request.view_curso` (sem ser apenas `request.view_own`), o filtro de `idSolicitante` é `null` — retorna solicitações de **todos os alunos**:

```kotlin
// RequestController.kt
val idSolicitante = if (
    user.authorities.contains("request.view_own") &&
    !user.authorities.contains("request.view_curso") &&  // secretaria TEM esta authority
    !user.authorities.contains("request.deliberate")
) {
    user.userId
} else {
    null  // secretaria vê tudo
}
```

---

## F5.3 — Gestão de Alunos (IAM)

| Endpoint | Capability | Status |
|----------|-----------|--------|
| `GET /usuarios` | `usuario.view_all` | ⏳ Não implementado |
| `POST /usuarios` | `usuario.create` | ⏳ Não implementado |
| `PATCH /usuarios/{id}/status` | `usuario.manage` | ⏳ Não implementado |

---

## F5.4 — Dados Acadêmicos (Turmas, Disciplinas, Períodos)

| Endpoint | Status |
|----------|--------|
| `GET /academico/cursos` | ✅ `AcademicoController` |
| `GET /academico/disciplinas` | ✅ `AcademicoController` |
| `POST /academico/turmas` | ⏳ Parcial |

---

## F5.9 — Importação de Dados (CSV/Excel)

> **Diagrama:** `US-F5-009-IMPORTACOES.md`  
> **Status:** ⏳ Não implementado — endpoint de upload CSV previsto para P2

---

## F5.11 — Estatísticas e Relatórios

> **Status:** ⏳ Não implementado — provavelmente views SQL + endpoint de export

---

## Checklist de Verificação

- [x] `GET /bff/dashboard/secretaria` → `200` com KPIs
- [x] `GET /requests` com `request.view_curso` → vê todas as solicitações do curso
- [x] `POST /requests/{id}/transitions` com capabilities secretaria → encaminha/encerra
- [x] `GET /academico/cursos` e `/disciplinas` → dados acadêmicos
- [ ] `GET /usuarios` — **não implementado**
- [ ] Importação CSV de alunos — **não implementado**
- [ ] Relatórios e exportações — **não implementado**
