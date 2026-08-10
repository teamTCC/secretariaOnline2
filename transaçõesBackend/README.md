# transaçõesBackend — Guia de Implementação das Transações

> **Objetivo:** Documentar como cada transação dos diagramas de sequência (`foundationDocs/sequenceDiagrams/`) está implementada no backend, com links diretos aos arquivos de código, exemplos de JSON, DTOs e notas de cobertura.  
> **Use como:** tutorial de aprendizado + checklist de verificação do funcionamento real do app.

---

## Status de Implementação por Módulo

| Módulo | Status | Cobertura |
|--------|--------|-----------|
| IAM — Autenticação (F0.1 a F0.3) | ✅ **Implementado** | Login, Refresh, Forgot/Reset/FirstAccess, Rate Limit, Audit |
| BFF — Dashboard Aluno/Professor/Secretaria (F1.1, F3.1, F5.1) | ✅ **Implementado** | Agregador de dados, `_links` HATEOAS |
| Solicitações — Motor de Workflow (F1.5, F3.3, F5.2) | ✅ **Implementado** | Open, Transition, HATEOAS transitions, WorkflowEngine |
| Horas Formativas (F1.6, F3.4) | ✅ **Implementado** | Submit, List, Review (CAAF), Resumo KPI |
| Presença em Eventos (F1.9, F3.2) | ✅ **Implementado** | Criar evento, Confirmar entrada/saída (SECRET e QR), Janelas |
| Outbox Dispatcher (Transversal 10.1) | ✅ **Implementado (parcial)** | Scheduler, retry/backoff — sem handlers de canal ainda |
| Certificados (Transversal 10.4, F1.10, F0.7) | ⏳ **Stub** | Entidades no banco; emissão real pendente |
| Egresso (F2) | ⏳ **Não implementado** | `AlumniController`, presigned MinIO, IDOR guard pendentes |
| Comissões CAAF (F4.1) | ⏳ **Não implementado** | Pool, self-assign, batch-decide, scope violation pendentes |
| Comissões COE (F4.2) | ⏳ **Não implementado** | Pool, assign+notif.aluno, bulk assign pendentes |
| Coordenação — Config Curso (F6.1) | ⏳ **Não implementado** | PATCH config + audit_log diff + ownership check pendentes |
| Coordenação — Relatórios (F6.2) | ⏳ **Não implementado** | `GET /reports/coordinator`, análogo ao secretary |
| Busca Global (F8.1) | ⏳ **Não implementado** | `SearchController`, fan-out paralelo, `pg_trgm` pendentes |
| Suporte/FAQ (F8.2) | ⏳ **Não implementado** | `SupportController`, `faq_items`, rate limit tickets pendentes |
| Admin IAM (F7.1/F7.2) | ⏳ **Stub** | Entidades e migrações no banco |
| Workflow Engine Admin (F7.3) | ⏳ **Stub** | CRUD de RequestType via DB; sem controller admin |
| Auditoria (F7.6) | ✅ **Implementado** | AuditPublisher + audit_log table |

---

## Arquitetura de Referência (Fluxo de uma Requisição)

```
HTTP Request
    ↓
RateLimitFilter (Bucket4j — /auth/login e /auth/forgot-password)
    ↓
JwtAuthenticationFilter (extrai Bearer → popula SecurityContext)
    ↓
Spring Security (verificação de authority com @PreAuthorize)
    ↓
Controller (valida DTO com @Valid, constrói Command)
    ↓
UseCase (@Transactional, regras de negócio, Argon2, JWT)
    ↓
Repository (JPA → Postgres)
    ↓
AuditPublisher (INSERT audit_log — toda ação mutante)
    ↓
JSON Response (RFC 7807 para erros, HATEOAS _links onde aplicável)
```

---

## Índice de Tutoriais

### F0 — Público (sem autenticação)
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F0-001-LOGIN.md](F0%20—%20Público/T-F0-001-LOGIN.md) | US-F0-001 (F0.1-a..f) | ✅ Implementado |
| [T-F0-002-RECUPERAR-SENHA.md](F0%20—%20Público/T-F0-002-RECUPERAR-SENHA.md) | US-F0-002 (F0.2-a/b/c) | ✅ Implementado |
| [T-F0-003-NOVA-SENHA.md](F0%20—%20Público/T-F0-003-NOVA-SENHA.md) | US-F0-003 (F0.3-a/b/c) | ✅ Implementado |
| [T-F0-004-CONTATO.md](F0%20—%20Público/T-F0-004-CONTATO.md) | US-F0-004 | ⚪ Sem backend |
| [T-F0-005-ERRO.md](F0%20—%20Público/T-F0-005-ERRO.md) | US-F0-005 | ✅ GlobalExceptionHandler |
| [T-F0-006-VERIFICAR-PROTOCOLO.md](F0%20—%20Público/T-F0-006-VERIFICAR-PROTOCOLO.md) | US-F0-006 | ⏳ Stub |
| [T-F0-007-VERIFICAR-CERTIFICADO.md](F0%20—%20Público/T-F0-007-VERIFICAR-CERTIFICADO.md) | US-F0-007 | ⏳ Stub |

### F1 — Aluno
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F1-001-DASHBOARD.md](F1%20—%20Aluno/T-F1-001-DASHBOARD.md) | US-F1-001 (F1.1-D01..D04) | ✅ Implementado |
| [T-F1-002-PRIMEIRO-ACESSO.md](F1%20—%20Aluno/T-F1-002-PRIMEIRO-ACESSO.md) | US-F1-002 | ✅ Implementado |
| [T-F1-003-PERFIL.md](F1%20—%20Aluno/T-F1-003-PERFIL.md) | US-F1-003 | ⏳ Parcial |
| [T-F1-004-COMUNICACAO.md](F1%20—%20Aluno/T-F1-004-COMUNICACAO.md) | US-F1-004 | ⏳ Stub |
| [T-F1-005-SOLICITACOES.md](F1%20—%20Aluno/T-F1-005-SOLICITACOES.md) | US-F1-005 | ✅ Implementado |
| [T-F1-006-FORMATIVAS.md](F1%20—%20Aluno/T-F1-006-FORMATIVAS.md) | US-F1-006 | ✅ Implementado |
| [T-F1-007-ESTAGIO.md](F1%20—%20Aluno/T-F1-007-ESTAGIO.md) | US-F1-007 | ⏳ Stub |
| [T-F1-008-TCC.md](F1%20—%20Aluno/T-F1-008-TCC.md) | US-F1-008 | ⏳ Stub |
| [T-F1-009-PRESENCA.md](F1%20—%20Aluno/T-F1-009-PRESENCA.md) | US-F1-009 | ✅ Implementado |
| [T-F1-010-CERTIFICADOS.md](F1%20—%20Aluno/T-F1-010-CERTIFICADOS.md) | US-F1-010 | ⏳ Stub |
| [T-F1-011-ATENDIMENTOS.md](F1%20—%20Aluno/T-F1-011-ATENDIMENTOS.md) | US-F1-011 | ⏳ Stub |

### Transversais
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-10.1-OUTBOX.md](transversal/T-10.1-OUTBOX.md) | 10.1a/b | ✅ Scheduler implementado |
| [T-10.4-CERTIFICADO.md](transversal/T-10.4-CERTIFICADO.md) | 10.4a | ⏳ Entidades no banco |

### F3 — Professor
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F3-001-DASHBOARD.md](F3%20—%20Professor/T-F3-001-DASHBOARD.md) | US-F3-001 | ✅ BFF `/bff/dashboard/professor` |
| [T-F3-002-EVENTOS.md](F3%20—%20Professor/T-F3-002-EVENTOS.md) | US-F3-002 | ✅ EventAttendanceController |
| [T-F3-003-DELIBERAR.md](F3%20—%20Professor/T-F3-003-DELIBERAR.md) | US-F3-003 | ✅ TransitionRequestUseCase |
| [T-F3-004-FORMATIVAS.md](F3%20—%20Professor/T-F3-004-FORMATIVAS.md) | US-F3-004 | ✅ FormativasController |
| [T-F3-005-ESTAGIO.md](F3%20—%20Professor/T-F3-005-ESTAGIO.md) | US-F3-005 | ⏳ Stub |
| [T-F3-006-TCC.md](F3%20—%20Professor/T-F3-006-TCC.md) | US-F3-006 | ⏳ Stub |
| [T-F3-007-COMUNICADO.md](F3%20—%20Professor/T-F3-007-COMUNICADO.md) | US-F3-007 | ⏳ Stub |

### F5 — Secretaria
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F5-001-DASHBOARD.md](F5%20—%20Secretaria/T-F5-001-DASHBOARD.md) | US-F5-001 | ✅ BFF `/bff/dashboard/secretaria` |
| [T-F5-002-SOLICITACOES.md](F5%20—%20Secretaria/T-F5-002-SOLICITACOES.md) | US-F5-002 | ✅ RequestController |

### F2 — Egresso
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F2-001-DASHBOARD-EGRESSO.md](F2%20—%20Egresso/T-F2-001-DASHBOARD-EGRESSO.md) | US-F2-001 (F2.1-D01..D04) | ⏳ Não implementado |

### F4 — Comissões
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F4-001-COMISSAO-CAAF.md](F4%20—%20Comissões/T-F4-001-COMISSAO-CAAF.md) | US-F4-001 (F4.1a..f) | ⏳ Não implementado |
| [T-F4-002-COMISSAO-COE.md](F4%20—%20Comissões/T-F4-002-COMISSAO-COE.md) | US-F4-002 (F4.2a..e) | ⏳ Não implementado |

### F6 — Coordenação
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F6-001-CONFIGURAR-CURSO.md](F6%20—%20Coordenação/T-F6-001-CONFIGURAR-CURSO.md) | US-F6-001 (F6.1-D01, D02, ERRO) | ⏳ Não implementado |
| [T-F6-002-RELATORIOS.md](F6%20—%20Coordenação/T-F6-002-RELATORIOS.md) | US-F6-002 (F6.2-D01, D02, ERRO) | ⏳ Não implementado |

### F8 — Cross-cutting
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F8-001-BUSCA-GLOBAL.md](F8%20—%20Cross-cutting/T-F8-001-BUSCA-GLOBAL.md) | US-F8-001 (F8.1-D01..D04) | ⏳ Não implementado |
| [T-F8-002-SUPORTE-FAQ.md](F8%20—%20Cross-cutting/T-F8-002-SUPORTE-FAQ.md) | US-F8-002 (F8.2-D01..D03) | ⏳ Não implementado |

---

## Legenda de Status
- ✅ **Implementado** — controller, use case, DTO, teste existem e cobrem o fluxo do diagrama
- ⏳ **Stub/Parcial** — entidades no banco, controller esqueleto ou parcialmente implementado
- ⚪ **Sem backend** — tela puramente estática, sem chamada HTTP ao backend
