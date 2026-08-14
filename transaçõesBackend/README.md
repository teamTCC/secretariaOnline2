# transaçõesBackend — Guia de Implementação das Transações

> **Objetivo:** Documentar como cada transação dos diagramas de sequência (`foundationDocs/sequenceDiagrams/`) está implementada no backend, com links diretos aos arquivos de código, exemplos de JSON, DTOs e notas de cobertura.  
> **Use como:** tutorial de aprendizado + checklist de verificação do funcionamento real do app.

---

## Status de Implementação por Módulo

| Módulo | Status | Cobertura |
|--------|--------|-----------|
| IAM — Autenticação (F0.1 a F0.3) | ✅ **Implementado** | Login, Refresh, Forgot/Reset/FirstAccess, Rate Limit, Audit, CSRF Double Submit |
| Perfil do Usuário (F1.3) | ✅ **Implementado** | GET/PATCH /me, avatar MinIO, senha, notificações, FCM token |
| BFF — Dashboard Aluno/Professor/Secretaria/Egresso | ✅ **Implementado** | Agregador de dados, `_links` HATEOAS, graceful degradation |
| Solicitações — Motor de Workflow (F1.5, F3.3, F5.2) | ✅ **Implementado** | Open+Outbox, Transition+Outbox, HATEOAS, WorkflowEngine |
| Horas Formativas (F1.6, F3.4) | ✅ **Implementado** | Submit, comprovante MinIO, Review+certificado PDF, Resumo KPI |
| Presença em Eventos (F1.9, F3.2) | ✅ **Implementado** | Criar evento, Confirmar entrada/saída+Outbox (SECRET e QR) |
| Hub de Comunicação (F1.4, F3.7) | ✅ **Implementado** | Inbox, marcar lido, publicar, contador de não-lidos |
| Admin de Usuários (F7.1 / F5) | ✅ **Implementado** | CRUD usuários, activate/deactivate, reset-password |
| Outbox Dispatcher (Transversal 10.1) | ✅ **Implementado** | Scheduler, SKIP LOCKED, retry/DEAD, 5+ handlers |
| Push FCM (Transversal 10.5) | ✅ **Implementado** | Firebase Admin SDK real; fallback gracioso sem service account |
| Admin Outbox (F7.5) | ✅ **Implementado** | List/detail/retry-DEAD/delete para operadores |
| Estágio (F1.7, F3.5) | ✅ **Implementado** | CRUD + documentos MinIO + Outbox (COE+supervisor) |
| TCC (F1.8, F3.6) | ✅ **Implementado** | CRUD + banca + upload final MinIO + Outbox |
| Comissões CAAF (F4.1) | ✅ **Implementado** | Pool, self-assign, batch-review, stats |
| Comissões COE (F4.2) | ✅ **Implementado** | Pool, assign-supervisor, bulk-assign, stats |
| Coordenação — Config Curso (F6.1/F6.2) | ✅ **Implementado** | `GET/PATCH /courses/:id/config` + CRUD + histórico escolar |
| Atendimentos (F1.11) | ✅ **Implementado** | Lista, ciência, agendamento aluno `POST /me/service-records` |
| Busca Global (F8.1) | ✅ **Implementado** | Fan-out + FGAC + GIN `pg_trgm` + timeout 5s |
| Suporte/FAQ (F8.2) | ✅ **Implementado** | FAQ público + tickets de suporte (abrir, responder, fechar) |
| Consulta pública de protocolo (F0.6) | ✅ **Implementado** | `GET /publico/solicitacoes/{ano}/{numero}` |
| Certificados (Transversal 10.4, F1.10, F0.7) | ✅ **Implementado** | PDF OpenPDF + Ed25519 + MinIO + JWKS (evento e formativa) |
| Auditoria (F7.6) | ✅ **Implementado** | AuditPublisher + audit_log table |
| Egressos / diplomas (F5.5) | ✅ **Implementado** | 5 critérios, PDF diploma, role EGRESSO, entrega |
| Importação CSV (F5.9) | ✅ **Implementado** | Validar + confirmar (`alunos` e `professores`) |
| Exportações CSV (F5.10) | ✅ **Implementado** | Job `PROCESSANDO` + worker 5s + MinIO |
| Estatísticas secretaria (F5.11) | ✅ **Implementado** | `GET /reports/secretary` |
| Kanban secretaria (F5.12) | ✅ **Implementado** | `/tasks` |
| Relatório coordenação (F6.2) | ✅ **Implementado** | `GET /reports/coordinator` |
| Admin roles (F7.2) | ✅ **Implementado** | `/admin/roles`, autoridades, `PUT /admin/usuarios/{id}/roles` |
| Editor RequestType (F7.3) | ✅ **Implementado** | `/request-types` CRUD + publish |
| Templates comunicação (F7.4) | ✅ **Implementado** | Catálogo versionado + TemplateEngine no dispatcher |

---

## Arquitetura de Referência (Fluxo de uma Requisição)

```
HTTP Request
    ↓
RateLimitFilter (Bucket4j — login, forgot-password, consultas públicas, contato)
    ↓
CsrfFilter (Double Submit: cookie XSRF-TOKEN + header X-XSRF-TOKEN)
    ↓
JwtAuthenticationFilter (extrai Bearer → popula SecurityContext)
    ↓
Spring Security (verificação de authority com @PreAuthorize)
    ↓
Controller (valida DTO com @Valid, constrói Command)
    ↓
UseCase (@Transactional, regras de negócio, Argon2, JWT)
    ↓  ↘
Repository  OutboxEventJpaRepository (atomic TX)
(JPA → Postgres)     ↓
                OutboxDispatcher (@Scheduled 5s)
    ↓               ↓
AuditPublisher  OutboxEventHandler (email, FCM, etc.)
    ↓
JSON Response (RFC 7807 para erros, HATEOAS _links onde aplicável)
```

---

## Índice de Tutoriais

### F0 — Público (sem autenticação)
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F0-001-LOGIN.md](F0 — Público/T-F0-001-LOGIN.md) | US-F0-001 | ✅ Implementado |
| [T-F0-002-RECUPERAR-SENHA.md](F0 — Público/T-F0-002-RECUPERAR-SENHA.md) | US-F0-002 | ✅ Outbox + Rate Limit 3/h + retryAfterSeconds |
| [T-F0-003-NOVA-SENHA.md](F0 — Público/T-F0-003-NOVA-SENHA.md) | US-F0-003 | ✅ Implementado |
| [T-F0-004-CONTATO.md](F0 — Público/T-F0-004-CONTATO.md) | US-F0-004 | ✅ GET/POST `/publico/contato` |
| [T-F0-005-ERRO.md](F0 — Público/T-F0-005-ERRO.md) | US-F0-005 | ✅ GlobalExceptionHandler + `incidentId` em 5xx |
| [T-F0-006-007-VERIFICACOES-PUBLICAS.md](F0 — Público/T-F0-006-007-VERIFICACOES-PUBLICAS.md) | US-F0-006/007 | ✅ `/publico/solicitacoes/{ano}/{numero}` + `/publico/verificar-certificado/{hash}` |

### F1 — Aluno
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F1-001-DASHBOARD.md](F1 — Aluno/T-F1-001-DASHBOARD.md) | US-F1-001 | ✅ BFF + graceful degradation |
| [T-F1-002-PRIMEIRO-ACESSO.md](F1 — Aluno/T-F1-002-PRIMEIRO-ACESSO.md) | US-F1-002 | ✅ Implementado |
| [T-F1-003-PERFIL.md](F1 — Aluno/T-F1-003-PERFIL.md) | US-F1-003 | ✅ GET/PATCH/avatar/senha/notificações/FCM |
| [T-F1-004-COMUNICACAO.md](F1 — Aluno/T-F1-004-COMUNICACAO.md) | US-F1-004 | ✅ Inbox, read, publicar |
| [T-F1-005-SOLICITACOES.md](F1 — Aluno/T-F1-005-SOLICITACOES.md) | US-F1-005 | ✅ Open+Outbox, Transition+Outbox, Anexos+Draft+Protocolo |
| [T-F1-006-FORMATIVAS.md](F1 — Aluno/T-F1-006-FORMATIVAS.md) | US-F1-006 | ✅ Submit + comprovante MinIO + certificado na aprovação |
| [T-F1-007-008-ESTAGIO-TCC.md](F1 — Aluno/T-F1-007-008-ESTAGIO-TCC.md) | US-F1-007/008 | ✅ CRUD + MinIO + Banca + Outbox |
| [T-F1-009-PRESENCA.md](F1 — Aluno/T-F1-009-PRESENCA.md) | US-F1-009 | ✅ SECRET+QR dual, janelas, +Outbox |
| [T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md](F1 — Aluno/T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md) | US-F1-010/011 | ✅ Certificados PDF + ciência + agendamento aluno |

### F2 — Egresso
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F2-001-DASHBOARD-EGRESSO.md](F2 — Egresso/T-F2-001-DASHBOARD-EGRESSO.md) | US-F2-001 | ✅ `GET /bff/dashboard/egresso` |

### F3 — Professor
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F3-PROFESSOR.md](F3 — Professor/T-F3-PROFESSOR.md) | US-F3-* | ✅ Dashboard, Eventos, Deliberar, Formativas, Estágio, TCC, Comunicados |

### F4 — Comissões
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F4-001-COMISSAO-CAAF.md](F4 — Comissões/T-F4-001-COMISSAO-CAAF.md) | US-F4-001 | ✅ Pool, self-assign, batch, stats |
| [T-F4-002-COMISSAO-COE.md](F4 — Comissões/T-F4-002-COMISSAO-COE.md) | US-F4-002 | ✅ Pool, assign-supervisor, bulk, stats |

### F5 — Secretaria
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F5-SECRETARIA.md](F5 — Secretaria/T-F5-SECRETARIA.md) | US-F5-* (índice) | ✅ Dashboard, fila, alunos, bulk-deliberate, atendimentos |
| [T-F5-005-EGRESSOS-DIPLOMAS.md](F5 — Secretaria/T-F5-005-EGRESSOS-DIPLOMAS.md) | US-F5-005 | ✅ 5 critérios + PDF diploma |
| [T-F5-009-IMPORTACOES.md](F5 — Secretaria/T-F5-009-IMPORTACOES.md) | US-F5-009 | ✅ CSV alunos e professores |
| [T-F5-010-EXPORTACOES.md](F5 — Secretaria/T-F5-010-EXPORTACOES.md) | US-F5-010 | ✅ Worker `PROCESSANDO` |
| [T-F5-011-ESTATISTICAS.md](F5 — Secretaria/T-F5-011-ESTATISTICAS.md) | US-F5-011 | ✅ `GET /reports/secretary` |
| [T-F5-012-TAREFAS.md](F5 — Secretaria/T-F5-012-TAREFAS.md) | US-F5-012 | ✅ Kanban `/tasks` |

### F6 — Coordenação
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F6-001-CONFIGURAR-CURSO.md](F6 — Coordenação/T-F6-001-CONFIGURAR-CURSO.md) | US-F6-001 | ✅ `GET/PATCH /courses/:id/config` + histórico |
| [T-F6-002-RELATORIOS.md](F6 — Coordenação/T-F6-002-RELATORIOS.md) | US-F6-002 | ✅ `GET /reports/coordinator` + atalho `/academico/relatorios/curso` |

### F7 — Admin
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F7-ADMIN.md](F7 — Admin/T-F7-ADMIN.md) | US-F7-* (índice) | ✅ Audit + FAQ + LGPD + Actuator |
| [T-F7-002-IAM-PERFIS.md](F7 — Admin/T-F7-002-IAM-PERFIS.md) | US-F7-002 | ✅ Roles / authorities |
| [T-F7-003-WORKFLOW-ENGINE.md](F7 — Admin/T-F7-003-WORKFLOW-ENGINE.md) | US-F7-003 | ✅ `/request-types` |
| [T-F7-004-TEMPLATES-COMUNICACAO.md](F7 — Admin/T-F7-004-TEMPLATES-COMUNICACAO.md) | US-F7-004 | ✅ Templates versionados |

### F8 — Cross-cutting
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-F8-001-BUSCA-GLOBAL.md](F8 — Cross-cutting/T-F8-001-BUSCA-GLOBAL.md) | US-F8-001 | ✅ FGAC + `pg_trgm` + timeout 5s |
| [T-F8-002-SUPORTE-FAQ.md](F8 — Cross-cutting/T-F8-002-SUPORTE-FAQ.md) | US-F8-002 | ✅ FAQ público + tickets |

### Transversais
| Arquivo | Diagrama | Status Backend |
|---------|----------|----------------|
| [T-10.1-OUTBOX.md](transversal/T-10.1-OUTBOX.md) | 10.1a/b | ✅ Dispatcher + TemplateEngine + in-app |
| [T-10.4-CERTIFICADO.md](transversal/T-10.4-CERTIFICADO.md) | 10.4a | ✅ PDF + Ed25519 + MinIO + JWKS |
| [T-10.5-PUSH-FCM.md](transversal/T-10.5-PUSH-FCM.md) | FCM | ✅ Firebase Admin SDK real + fallback gracioso |
| [T-10.6-ADMIN-OUTBOX.md](transversal/T-10.6-ADMIN-OUTBOX.md) | Admin | ✅ List/retry-DEAD/delete |
| [T-10.7-REDIS-BFF.md](transversal/T-10.7-REDIS-BFF.md) | Redis | ✅ Cache-aside TTL 60s nos 4 dashboards BFF |

---

---

## 🏁 Status Final (beyond-MVP backend)

- ✅ **Módulos de negócio** — IAM, Solicitações, Formativas, Presença, Estágio, TCC, Comissões, Coordenação (`/courses/:id/config`), Comunicação, Atendimentos (ciência + agendamento), Busca (`pg_trgm` + timeout 5s), Suporte/FAQ, Diplomas (5 critérios + PDF), Import alunos/professores, Export assíncrono, Kanban, Relatórios filtrados
- ✅ **Outbox** — TemplateEngine no catálogo + in-app em ops e `solicitacoes.*` + `contato.recebido`
- ✅ **Segurança** — CSRF Double Submit Cookie; certificados PDF com Ed25519 (chave efêmera em dev)
- ⚪ **Fora deste recorte** — SPA React, Expo/mobile, testes E2E/integração, OpenAPI tipado (`Map<String, Any?>`), Excel (só CSV)

---

## Legenda de Status
- ✅ **Implementado** — controller, use case, DTO existem e cobrem o fluxo do diagrama
- ⏳ **Stub/Parcial** — entidades no banco, controller esqueleto ou parcialmente implementado
- ⚪ **Sem backend** — tela puramente estática, sem chamada HTTP ao backend

