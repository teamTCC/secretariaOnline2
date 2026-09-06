# As-built backend — SecretariaOnline2

**Data:** 2026-08-29  
**Fonte:** código em `backend/` + Flyway `V001`–`V019` + GitNexus (`secretariaOnline2`, 41 controllers).  
**Papel:** contrato para alinhar `foundationDocs/`, `httpie/`, `transaçõesBackend/` e `checklist_transactions.md` à implementação real.  
**Não substitui** a análise arquitetural de 2026-06 (intenção de design). Onde os dois divergem, **este arquivo vence** para “como está no código”.

Relatórios de origem: `RELATORIO_AUDITORIA_BACKEND.md`, `RELATORIO_AUDITORIA_BACKEND_POS_CORRECOES.md`.

---

## 1. Princípios as-built (camadas)

| Camada | Código real | Não fazer na documentação |
|--------|-------------|---------------------------|
| HTTP | `*Controller` fino: DTO in/out, `@PreAuthorize`, chama Query ou UseCase | Controller com JPA; `RelatoriosController` (apagado) |
| Leitura | `*Query` em `application/` | “Service” genérico para GET |
| Escrita | `*UseCase.execute(Command)` + `@Transactional` | Use case devolvendo `*Entity` ao controller |
| Cross-módulo / BFF | Port em `application/ports/out/` + Adapter em `infrastructure/` | BFF injetando `*JpaRepository` de outro módulo |
| Intra-módulo | UseCase/Query **podem** injetar `*JpaRepository` do próprio módulo (atalho TCC) | Exigir `ports/in` em todo use case |
| HATEOAS | `_links` como `Map<String,String>` ou data class com `String` (`@JsonProperty("_links")`) | HAL `EntityModel` `{ rel: { href } }` nos detalhes REST |
| Domain | Sem Spring/JPA (ArchUnit). Exceções de workflow: `WorkflowException` pura | `InsufficientAuthorityException extends AccessDeniedException` |
| Schema | Flyway **ligado** em `dev`/`test`/`prod`; `ddl-auto: validate` | `ddl-auto: update` ou Flyway off no dev |
| Auth HTTP | Cookies HttpOnly `access_token` (Path=/) + `refresh_token` (Path=/auth). JSON **sem** tokens | Body `{ accessToken, refreshToken }` |
| Sessão | Redis `auth:session:<sid>` obrigatório no login; JWT com claim `sid` | Só JWT sem sessão Redis |
| Outbox | `OutboxEventPublisher.enqueue(...)` (port `shared`) | Use case injetando `OutboxEventJpaRepository` |
| MinIO | Compose: healthcheck `mc ready local`; backend `depends_on: service_started` | Healthcheck `curl` no container MinIO |

**Não implementado (e não deve ser descrito como existente):** pasta `assembler/` em todos os módulos; `ports/in` em todos os use cases; enum `DRAFT`/`PUBLISHED` em `request_type` (usa `ativo`); tabela `request_line_item`; Mailpit/Prometheus/Grafana no compose; RabbitMQ.

---

## 2. Auth — rotas e fluxo real

`AuthController` `@RequestMapping("/auth")`:

| Método | Path | Auth | CSRF ignore | Rate limit | Resultado |
|--------|------|------|-------------|------------|-----------|
| GET | `/auth/csrf` | permitAll | — | — | cookie `XSRF-TOKEN` + JSON token |
| POST | `/auth/login` | permitAll | sim | 5/min IP+identificador | 200 `{ mustChangePassword, mustAcceptLgpd }` + cookies |
| POST | `/auth/refresh` | permitAll | sim | — | 200 vazio + novos cookies |
| POST | `/auth/ott` | permitAll | sim | igual login | 200 mesmo contrato do login + cookies |
| POST | `/auth/forgot-password` | permitAll | sim | por e-mail | 202 sempre |
| POST | `/auth/reset-password` | permitAll | sim | — | 200 |
| POST | `/auth/first-access` | autenticado | não | — | 200 |
| POST | `/auth/logout` | autenticado | não | — | 204, limpa cookies, revoga JTI/sessão |

**OTT:** body `{ "token": "<jwt>" }`. Audience `request:{uuid}`. Consome JTI (Redis), emite sessão. Replay → 401.

**Login (cadeia real):** `RateLimitFilter` → `AuthController` → `LoginUseCase` → `UsuarioRepository` + `PasswordHasherPort` (Argon2id) + `TokenServicePort` (JWT) + `TokenRevocationPort.createSession` (Redis) + `RefreshTokenRepository` + `AuditPublisher`. Tokens **não** voltam no JSON.

**Fallback:** `JwtAuthenticationFilter` aceita cookie `access_token` **ou** `Authorization: Bearer`.

---

## 3. BFF — rotas e ports

Controllers (GitNexus): `DashboardAlunoController`, `DashboardProfessorController`, `DashboardSecretariaController`, `DashboardEgressoController`, `ReportsController`, `SearchController`, `ExportController`, `AcademicoSummaryController`.

| Path | Query / UseCase | Ports (não JPA) |
|------|-----------------|-----------------|
| `GET /bff/dashboard/aluno` | `DashboardAlunoQuery` | `SolicitacaoDashboardPort`, `PresencaDashboardPort`, `FormativaDashboardPort`, `IamDashboardPort` |
| `GET /bff/dashboard/professor` | `DashboardProfessorQuery` | idem + ports de estágio/TCC conforme código |
| `GET /bff/dashboard/secretaria` | `DashboardSecretariaQuery` | ports de dashboard |
| `GET /bff/dashboard/egresso` | `DashboardEgressoQuery` | ports IAM/certificado |
| `GET /reports/secretary` | `ReportsQuery.secretary` | `IamDashboardPort`, `IamBffReadPort`, `SolicitacaoBffReadPort`, `TccDashboardPort`, `EstagioSummaryPort`, `FormativaBffReadPort`, `PresencaBffReadPort`, `AcademicoReadPort` |
| `GET /reports/coordinator` | `ReportsQuery.coordinator` | mesmos ports |
| `GET /search` | `SearchQuery` | `IamBffReadPort`, `SolicitacaoBffReadPort`, `PresencaBffReadPort`, `AcademicoReadPort` |
| `POST/GET /export/...` | `ExportJobsUseCase` | `ExportJobPort`, `IamBffReadPort`, `SolicitacaoBffReadPort` |

**Cache Redis:** cache name `bff-dashboard`, **TTL 60 s** (`CacheConfig`). Chaves: `aluno:{id}`, `professor:{id}`, `secretaria:static`, `egresso:{id}`, `academico:summary`. Sem Redis, Spring `simple` cache.

**JSON dashboard aluno:** `_links` é objeto com **strings** (`self`, `novaSolicitacao`, `formativas`, `eventos`). Itens de pendência usam `_link` (singular, string), **não** `_links.acao.href`.

Auth nas setas dos diagramas: cookie `access_token` (Bearer só como fallback). Prefixo típico: `GET /bff/dashboard/aluno (cookie access_token)`.

---

## 4. Solicitações — Query vs UseCase

`RequestController` `@RequestMapping("/requests")`:

| HTTP | Classe |
|------|--------|
| POST `/requests`, draft, submit, PATCH draft, POST transitions, PATCH bulk-deliberate | `*UseCase` |
| GET list, GET `{id}`, protocol, events, types | `RequestQuery` / `RequestTypeQuery` |

`_links` no detalhe: `Map<String,String>` (`self`, `events`, `attachments`, `submit`, `update-draft`, `upload-url`, ações do workflow em kebab-case). Rel = string URL, **não** `{ href }`.

**Versionamento (V019):** tabela `request_type_version`; `request.id_request_type_version`; publish grava snapshot; GET detalhe usa `form_schema` da versão da instância.

**Tipo:** coluna `ativo` (boolean). Sem status `DRAFT`/`PUBLISHED`.

**Deep-link:** e-mail com `?ott=`; exchange em `POST /auth/ott` (não “login normal” como único caminho).

---

## 5. Banco as-built (Flyway)

Fonte da verdade: `backend/app/src/main/resources/db/migration/`. **Não editar** V001–V019.

### 5.1 Migrations

| Ver | Conteúdo |
|-----|----------|
| V001 | extensions |
| V002 | IAM: authority, role, role_authority, usuario, usuario_role, refresh_token, jti_blacklist, password_history |
| V003 | curso, disciplina, periodo_letivo, calendario_academico |
| V004 | request_type, request, request_event, request_attachment (**sem** `request_line_item`) |
| V005 | formativas, estágio, tcc |
| V006 | presença + certificate |
| V007 | comunicação, outbox_event, audit_log |
| V010–V011 | seed authorities/roles + demo |
| V012 | service_record, faq_item, support_ticket, device_fcm_token |
| V013 | FAQ seed |
| V014 | graduation_record, secretary_task, import_job, communication_template(+revision), notification_log, export_job |
| V015 | historico_escolar, contact_message |
| V016 | authorities egresso/reports |
| V017 | 19 request types |
| V018 | `updated_at` em request_event e request_attachment |
| V019 | `request_type_version` + FK `request.id_request_type_version` |

### 5.2 Contagem

O modelo TCC 2026-06 descrevia **31 tabelas** (incluindo `request_line_item` **não migrada**). O Flyway as-built tem **~45 tabelas de aplicação** (V002–V019), sem `request_line_item`.

`jti_blacklist` (Postgres) = tokens de e-mail / OTT de senha. Revogação de access JWT = Redis `auth:revoked:jti:`. Application: `EmailOneTimeTokenStore` (não “JtiBlacklistRepository” na camada application).

### 5.3 `request` (V004 + V019) — colunas reais

Diferenças vs dbml antigo: `request_type_code`, `parecer`, `deleted_at`, `prazo_em` nullable, unique `(numero_anual, ano, id_curso)` (não só ano+número), `id_request_type_version` (V019). Sem `request_line_item`.

`request_type`: `code VARCHAR(60)`, `prazo_dias` default **10**, **sem** colunas `interna` / `required_auth` no Flyway (authorities nas transições do `workflow_json`).

---

## 6. Outros endpoints que a doc antiga errava

| Doc antiga | Código |
|------------|--------|
| `GET /academico/disciplinas` só via curso | Alias `GET /academico/disciplinas` (`AcademicoController`) |
| Relatórios em `RelatoriosController` | `ReportsController` `/reports/secretary` e `/reports/coordinator` |
| `_links` HAL | strings |
| Cache dashboard 30 s | **60 s** |
| Login devolve tokens no JSON | só cookies |
| OTT sem exchange | `POST /auth/ott` |
| BFF `SELECT` único no Postgres | Query → **ports** → adapters → JPA **por módulo** |
| Outbox no use case via JPA | `OutboxEventPublisher` |
| MinIO healthcheck curl | `mc ready local` |

---

## 7. Como atualizar um diagrama de sequência

Convenções em `.cursor/skills/fullstack-sequence-diagrams` + `sequenceDiagrams/mermaid-live-config.json`:

- Humanos = `participant` (nunca `actor`)
- Sem `%%{init}%%`, sem `<br/>` em labels, sem `Note over`
- Labels ≤ ~58 chars; HTTP com método + path + status
- Login/refresh/ott: **não** colocar tokens no JSON da seta de retorno
- BFF: participantes `DashboardAlunoQuery` (ou `ReportsQuery`) + port, **não** `Postgres` direto do BFF
- Escrita: `UseCase` + `BEGIN/COMMIT` implícito numa seta transacional
- GET de recurso: `*Query`, não UseCase

---

## 8. Controllers (GitNexus — 41)

Academico, AcademicoSummary, AdminOutbox, AdminRequestType, AdminRoles, Audit, Auth, Certificate, CommissionsCaaf, CommissionsCoe, CommunicationTemplate, Communications, ContatoPublico, Coordenacao, CourseConfig, DashboardAluno/Egresso/Professor/Secretaria, Estagio, EstagioDocument, EventAttendance, Export, FcmToken, Formativas, Graduation, HistoricoEscolar, Import, Jwks, Profile, Publico, PublicoSolicitacao, Reports, RequestAttachment, Request, Search, SecretaryTask, ServiceRecord, Support, Tcc, Usuarios.

Não existe `RelatoriosController`.
