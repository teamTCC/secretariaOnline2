# T-F7 — Admin: IAM, Workflow Engine, Auditoria, Saúde

> **Diagramas de referência:** [`foundationDocs/sequenceDiagrams/F7 — Admin/`](../../foundationDocs/sequenceDiagrams/F7%20—%20Admin/)  
> **Status:** ✅ Auditoria implementada | ⏳ Demais módulos stub

---

## F7.6 — Audit Log ✅

Todo evento mutante do sistema é registrado via `AuditPublisher`:

```kotlin
// shared/audit/AuditPublisher.kt
auditPublisher.publish(AuditPayload(
    acao = "LOGIN_SUCCESS",
    idAtor = usuario.id,
    alvoTipo = "usuario",
    alvoId = usuario.id,
    ip = command.ip,
    userAgent = command.userAgent,
    resultado = "OK",
))
```

A tabela `audit_log` (migration V007) registra todas as ações. O endpoint de consulta de auditoria é um stub.

### Eventos de auditoria já emitidos

| Use Case | Evento Auditado |
|----------|----------------|
| `LoginUseCase` | `LOGIN_SUCCESS`, `LOGIN_FAILED`, `ACCOUNT_BLOCKED` |
| `RefreshTokenUseCase` | `SUSPICIOUS_TOKEN_REUSE` |
| `ForgotPasswordUseCase` | `PASSWORD_RESET_REQUESTED` |
| `ResetPasswordUseCase` | `PASSWORD_CHANGED` |
| `FirstAccessUseCase` | `FIRST_ACCESS_COMPLETED` |

---

## F7.1/F7.2 — IAM: Usuários e Perfis ⏳

| Endpoint Admin | Status |
|----------------|--------|
| `GET /admin/usuarios` | ⏳ Não implementado |
| `POST /admin/usuarios` | ⏳ Não implementado |
| `GET /admin/roles` | ⏳ Não implementado |
| `POST /admin/roles/{id}/authorities` | ⏳ Não implementado |

---

## F7.3 — Workflow Engine Admin ⏳

A tabela `request_type` contém `form_schema` (JSON Schema) e `workflow_json` (máquina de estados). O seed `V010__seed_authorities_roles.sql` e `V011__seed_demo_data.sql` pré-populam tipos de solicitação.

Para gerenciar via API (criar/editar tipos de solicitação), um `AdminRequestTypeController` precisará ser implementado.

---

## F7.5 — Jobs / Outbox ⏳

O `OutboxDispatcher` roda mas sem visualização admin. Implementar:
```
GET /admin/outbox/events      → listar eventos pendentes/falhados
POST /admin/outbox/{id}/retry → forçar retry manual
```

---

## F7.9 — Saúde do Sistema (Actuator) ✅

Já configurado via Spring Boot Actuator:
```
GET /actuator/health  → UP/DOWN + health dos dependências (DB, Redis)
GET /actuator/info    → versão da aplicação
GET /actuator/metrics → métricas Micrometer
```

---

## Checklist de Verificação

- [x] Audit log registrado em todos os use cases críticos de IAM
- [x] `GET /actuator/health` → `{"status": "UP"}`
- [ ] `GET /admin/usuarios` → gestão de usuários
- [ ] `GET /admin/roles` → gestão de perfis e authorities
- [ ] `POST /admin/request-types` → CRUD de tipos de solicitação
- [ ] `GET /admin/outbox/events` → monitoramento da fila
