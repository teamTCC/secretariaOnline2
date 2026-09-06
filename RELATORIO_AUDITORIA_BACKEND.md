# Relatório de auditoria — backend SecretariaOnline2

**Data:** 2026-08-29  
**Escopo:** somente `backend/` + `ops/` (sem frontend, sem React).  
**Método:** leitura do código real contra o design do TCC (Clean Architecture por bounded context, motor genérico de solicitações, FGAC + HATEOAS, JWT + Redis, Flyway, Docker Compose).  
**Régua:** projeto de TCC / MVP — não “app de empresa”. O que está *pragmático e suficiente* não é marcado como erro.

---

## 1. Como o projeto está desenhado

O produto é um **monólito modular** Spring Boot 3 + Kotlin: um único processo (`:app`), vários bounded contexts em `backend/modules/*`, kernel em `backend/shared`, schema Flyway centralizado em `backend/app/src/main/resources/db/migration`.

Não há API Gateway. Nginx (evolução) ou Compose direto na porta 8080. Mensageria RabbitMQ está **adiada de propósito**: Outbox + `@Scheduled` a cada 5s.

### 1.1 Estrutura de pastas (Gradle)

```
backend/
  app/                          # entrypoint Spring, SecurityConfig, CacheConfig, Flyway
    src/main/resources/db/migration/   V001…V018
  shared/                       # kernel: RFC 7807, PageResponse, VOs (Email, GRR, CPF),
                                # AuditPublisher, OutboxEventPublisher, AuthenticatedUser
  modules/
    iam/            identidade, JWT, CSRF, roles/authorities, usuários, diplomas, FAQ…
    academico/      cursos, disciplinas, períodos, histórico
    solicitacoes/   motor genérico (RequestType + WorkflowEngine) — coração DRY
    formativas/     horas complementares + CAAF
    estagio/        estágios + COE
    tcc/            trabalhos de conclusão
    presenca/       eventos formativos + certificado ED25519
    comunicacao/    hub + templates
    notificacoes/   outbox dispatcher
    auditoria/      audit_log imutável
    arquivos/       adapter MinIO (sem domain próprio — OK)
    bff/            agregadores de dashboard / busca / relatórios
  Dockerfile                    # multi-stage Temurin 21 → fat jar
ops/
  docker-compose.yml            # postgres, redis, minio, backend
  postgres/init/01-extensions.sql
```

Gradle (`settings.gradle.kts`): `:shared`, `:app`, 12 módulos.

### 1.2 Design de camadas *por módulo* (o que o TCC define)

O template do arquiteto é:

| Camada | Responsabilidade | “O quê” vs “como” |
|--------|------------------|-------------------|
| `domain/` | Entidades, VOs, invariantes, engine | **O quê** sempre é verdade no negócio. Zero Spring/JPA/HTTP. |
| `application/` | Use cases | **Como** executar um comando (orquestra ports). |
| `application/ports/out/` | Interfaces | Persistência, JWT, MinIO, dashboard BFF. |
| `infrastructure/` | JPA entities, repos, adapters, outbox handlers | Implementa ports. |
| `api/` | Controllers finos + DTOs request/response + HATEOAS | HTTP. Sem regra de negócio. |

**O que o TCC *não* exige neste estágio:** um arquivo por entidade, pasta `assembler/`, pasta `ports/in/`, ArchUnit, Kubernetes, Prometheus+Grafana no compose.

Agrupar DTOs em `*Requests.kt` / `*Responses.kt` e entities em `*Entities.kt` é **adequado** para o tamanho do repositório.

### 1.3 Mapa dos 12 módulos

| Módulo | Papel | Domain | Use cases | API |
|--------|-------|--------|-----------|-----|
| **iam** | Auth, FGAC, usuários, suporte | `Usuario`, `Role`, `RefreshToken` | Login, refresh, reset, roles… | `/auth`, `/me`, `/usuarios`, … |
| **academico** | Catálogo | `Curso` | CRUD curso/período/histórico | `/academico`, `/coordenacao` |
| **solicitacoes** | 19 tipos × 1 engine | `WorkflowEngine`, `Request`, `AttachmentPolicy` | Open, draft, transition, anexos | `/requests`, `/request-types` |
| **formativas** | CH complementar | `FormativaActivity` | submit/review/claim | `/formativas` |
| **estagio** | Estágios | `Estagio` | declarar, encerrar, docs | `/internships` |
| **tcc** | TCC | `Tcc` | create, approve, grade | `/tccs` |
| **presenca** | Eventos + certificado | `EventAttendance` | create, confirm, close | `/events`, `/publico/verificar-certificado` |
| **comunicacao** | Comunicados | `Communication` | publish, templates | `/communications` |
| **notificacoes** | Outbox | (infra) | dispatcher 5s | `/admin/outbox` |
| **auditoria** | Trilha | (infra) | — | `/audit` |
| **arquivos** | S3/MinIO | — | serviço | (usado por outros) |
| **bff** | Agregação | — | queries + cache 60s | `/bff/dashboard/*` |

Módulos conversam no processo JVM. O caminho “certo” é **port de saída** (ex.: `SolicitacaoDashboardPort`). O BFF e alguns controllers ainda leem `*JpaRepository` de outros módulos — atalho de monólito, aceitável no TCC, desalinhado do template estrito.

---

## 2. Workflow engine — como funciona e regras de negócio

Três pilares (implementados):

```
RequestType (configuração no Postgres)
  form_schema JSONB     → validação server-side (JSON Schema Draft-07)
  workflow_json JSONB   → estados + transições + authorities + guards
  prazo_dias, code, ativo

Request (instância)
  dados JSONB, estado, prazo_em, id_solicitante
  anexos em request_attachment (SHA-256 + MinIO)

WorkflowEngine (Kotlin puro)
  allowedTransitions(estado, authorities)  → o que vira _links
  applyTransition                          → RequestEvent + Outbox
```

**Invariantes que o domínio realmente garante**

- Estado inicial no **open** vem de `workflow_json.initial` (não hardcoded).
- Transição só existe se `(from, action)` está no JSON.
- Transição exige `requiresAuthority` ∩ authorities do JWT.
- Guards reconhecidos: `actor.id == request.idSolicitante` e `request.allowsReview`.
- `allowsReview()`: só `INDEFERIDA` e `concludedAt` há menos de 5 dias.
- Rascunho **não** valida `form_schema`; open e submit **validam**.
- Anexos: allowlist de content-type, 20 MB, SHA-256 recalculado no servidor, estados `RASCUNHO|ABERTA|EM_AJUSTE`.
- 19 tipos seedados (V011 + V017). Um tipo novo = INSERT JSON, sem classe nova. **Isso está correto.**
- Evento + outbox na mesma `@Transactional` em `TransitionRequestUseCase`.

**Estados canônicos:** `RASCUNHO`, `ABERTA`, `EM_TRIAGEM`, `EM_DELIBERACAO`, `EM_AJUSTE`, `DEFERIDA`, `INDEFERIDA`, `EM_REVISAO`, `ARQUIVADA`. Finais: DEFERIDA, INDEFERIDA, ARQUIVADA.

**Fluxo típico (aluno → secretaria → professor)**  
ABERTA → ASSIGN → EM_TRIAGEM → FORWARD_TO_DELIBERATOR → EM_DELIBERACAO → DEFER|DENY|REQUEST_ADJUSTMENT.  
EM_AJUSTE → RESUBMIT (só o solicitante) → ABERTA.

Admin: `POST /request-types` cria `ativo=false`; `POST /{id}/publish` valida estrutura e publica. Sem tabela de versão (P2 consciente).

---

## 3. JWT, dual cookie e Redis

### 3.1 O que está implementado (e está alinhado ao TCC)

Melhor que o rascunho “access token no JS”: **os dois tokens são cookies HttpOnly**. XSS não lê JWT. CSRF é Double Submit (`GET /auth/csrf` + `X-XSRF-TOKEN`) + SameSite=Lax.

| Cookie | Path | Conteúdo | TTL |
|--------|------|----------|-----|
| `access_token` | `/` | JWT RS256 | 15 min (`SECURITY_JWT_ACCESS_TTL`, default 900s) |
| `refresh_token` | `/auth` | UUID opaco (tabela `refresh_token`) | 7 dias |
| `XSRF-TOKEN` | `/` | CSRF (não HttpOnly) | sessão |

Claims do access: `sub` (userId), `sid`, `authorities[]`, `nome`, `jti`, `iss`, `exp`.  
Filtro aceita cookie **ou** `Authorization: Bearer` (Swagger/httpie).

**Redis (obrigatório para login funcionar)** — não é só cache:

| Chave | Função | TTL |
|-------|--------|-----|
| `auth:session:<sid>` | sessão viva; logout apaga a chave → JWT morre na hora | access TTL + 60s |
| `auth:force-logout:user:<uuid>` | reuse de refresh / reset senha derruba todos os access | access TTL |
| `auth:revoked:jti:<jti>` | blacklist legado / one-shot | resto de vida do JWT |

Refresh: **rotação** + detecção de reuso (revoga todos os refresh do usuário + force-logout Redis). Argon2id nas senhas. Rate limit Bucket4j (login, forgot, transições 20/min).

Política Redis no Compose: `maxmemory-policy noeviction` + AOF — certo para não evictar sessão de auth.

Cache BFF (`bff-dashboard`, 60s) **não** usa as mesmas chaves de auth. Em Compose `CACHE_TYPE=redis`; Gradle local default `simple` (memória) — o **login ainda precisa do Redis** (`StringRedisTemplate`).

### 3.2 HATEOAS

O detalhe de solicitação, tipos, perfil, eventos, estágio e TCC usam `_links` para o front “cego a perfil”. Transições: `action` → `rel` em kebab (`FORWARD_TO_DELIBERATOR` → `forward-to-deliberator`).

Há **dois formatos** no mesmo backend (ver guia frontend):

- HAL Spring (`EntityModel`): `{ "href": "…" }`
- Mapa string nos DTOs de create/lista: `"self": "/requests/…"`

Não há pasta `assembler/` — links montados no controller. Para TCC, suficiente.

---

## 4. Rotas e DTOs (API)

Padrão real (bom para o TCC):

- Controllers com `@RequestMapping` estável (`/requests`, `/auth`, `/events`…).
- DTOs de entrada com Jakarta Validation (`SolicitacoesRequests.kt`, `IamRequests.kt`, …).
- DTOs de saída com `@JsonInclude(NON_NULL)` e `_links` onde o front precisa.
- Erros RFC 7807 (`ProblemDetail`) no handler global + handlers de módulo (schema 422, transição 422, guard 403).
- OpenAPI SpringDoc em `/swagger-ui.html`.
- Sem `context-path`.

Leituras (GET lista/detalhe) em vários módulos ainda falam com JPA no controller. Comandos importantes (login, transition, open, anexos) passam por use case. **Híbrido:** writes mais limpos que reads.

---

## 5. Infra de containers (`ops/`)

### 5.1 Serviços e portas

Rede Docker: `secretaria_net` (bridge). Host ↔ container:

| Serviço | Imagem | Porta host | Porta interna | Uso |
|---------|--------|------------|---------------|-----|
| postgres | postgres:16-alpine | **5432** | 5432 | JDBC `postgres:5432/secretaria_dev` |
| redis | redis:7-alpine | **6379** | 6379 | sessão JWT + cache BFF |
| minio | minio/minio | **9000** API, **9001** console | 9000 / 9001 | anexos S3 |
| backend | fat jar Temurin 21 | **8080** | 8080 | API + `/actuator/health` |

Gradle local (sem container backend): `localhost:5432`, `6379`, `9000` — Compose documenta `up postgres redis minio`.

Backend no Compose usa hostnames **internos** (`postgres`, `redis`, `minio:9000`). `MINIO_ENDPOINT` default `http://minio:9000`. Dev Gradle: `http://localhost:9000`.

Dockerfile: multi-stage, user não-root, `MaxRAMPercentage=75`, fat jar `secretaria-online-2.jar`. Perfil do container: **`prod`** (Flyway on, `ddl-auto: validate`). Correto.

Extensões Postgres no init: `uuid-ossp`, `pgcrypto`, `citext`, `pg_trgm`, função `uuid_generate_v7()`.

### 5.2 O que o design pedia e o compose *não* tem (TCC: opcional)

- Mailpit/Mailhog (SMTP 1025 / UI 8025) — o YAML de e-mail aponta `localhost:1025`, mas **não há serviço de mail** no compose. E-mail no container backend cai no vazio.
- Prometheus :9090, Grafana :3000, Loki — actuator já exporta `prometheus`; stack de observabilidade **adiada**. Adequado a TCC.
- Comentário no topo do compose ainda diz “Three services”; na prática são **quatro** (+ MinIO).

Healthcheck MinIO usa `curl` dentro da imagem oficial (muitas tags **não têm curl**). Risco: `depends_on: minio: service_healthy` trava o `backend`. Vale trocar por `mc ready` / wget se isso aparecer no dia a dia.

---

## 6. O que já está implementado corretamente

- Monólito modular com os 12 bounded contexts previstos.
- Motor genérico de 19 tipos; zero controller por tipo.
- Validação `dados` × `form_schema` no servidor; RFC 7807.
- HATEOAS de transições derivado do engine × JWT authorities.
- Anexos presigned MinIO + SHA-256 server-side + política de domínio.
- Dual cookie HttpOnly + CSRF + refresh rotativo + reuse detection.
- Redis sessão fail-closed (sem Redis, JWT não autentica — de propósito).
- Outbox a cada 5s (sem Rabbit).
- Flyway V001–V018 no perfil `prod`; UUIDv7; TIMESTAMPTZ; JSONB onde o schema varia.
- FGAC `@PreAuthorize` + ownership `view_own`.
- BFF dashboards com cache curto e ports de leitura (`*DashboardPort`).
- Certificados (presença) com chave e rota pública `/publico/verificar-certificado`.
- Testes de domínio do engine + ITs Testcontainers (workflow, anexos).
- Rate limit em login e `POST /requests/{id}/transitions`.

---

## 7. O que está mal implementado ou desalinhado (prioridade TCC)

Ordenado do que mais dói no dia a dia / na banca, sem “refatorar o mundo”.

### P1 — vale corrigir se for mexer nessa área

1. **`application-dev.yml` desliga Flyway e liga `ddl-auto: update`.**  
   Gradle local (`dev`) e Docker (`prod`) **não compartilham a mesma fonte de schema**. Hibernate pode criar colunas que o Flyway não conhece. O design é SQL-first / Flyway only.  
   **Ajuste proporcional:** `flyway.enabled: true` no dev (ou um perfil `local` que ainda rode Flyway) e `ddl-auto: validate`.

2. **`InsufficientAuthorityException` no `domain/` estende `AccessDeniedException` do Spring.**  
   Quebra a regra “domain sem framework”. Funciona (vira 403), mas o domínio fica acoplado.  
   **Ajuste:** exception pura + `@ExceptionHandler` no `SolicitacoesExceptionHandler`.

3. **Use cases de solicitação (e outros) injetam `*JpaRepository` e `OutboxEventJpaRepository` de outro módulo.**  
   Existe `OutboxEventPublisher` em `shared` e não é usado no transition. Acoplamento infra↔infra.  
   **Ajuste só se tocar no arquivo:** passar a enfileirar pelo port shared.

4. **Healthcheck MinIO com `curl`** + `backend.depends_on.minio: service_healthy`.  
   Pode impedir o stack de subir.

5. **Dois shapes de `_links`.** Não é bug, mas o contrato da API não é uniforme (HAL vs `Map<String,String>`). Documentado no guia; unificar só se o front sofrer.

### P2 — desalinhamento consciente / aceitável no TCC

- Controllers “gordos” em GET (lista/detalhe) com JPA direto — comum em walking skeleton.
- Sem `assembler/`, sem `ports/in`, sem ArchUnit.
- `LoginUseCase` chama `JwtTokenService` / Argon2 na application (infra vazando para cima). IAM ainda tem ports para usuário e refresh — meio-termo OK.
- `TransitionGuardFailedException` herda `IllegalStateException` (409 global) mas o handler do módulo mapeia 403 — funciona, herança confusa.
- BFF `ReportsController` / `SearchController` importam JPA de 5+ módulos.
- `arquivos/`, `auditoria/`, `notificacoes/` sem `domain/` rico — adapters. OK.
- Seed `x-ui.endpoint: /academico/disciplinas` vs API real `/academico/cursos/{id}/disciplinas`.
- Deep-link `?ott=` é **emitido**; não há `POST /auth/ott` para virar sessão.
- Versionamento `request_type_version` / snapshot no publish — P2, motor funciona sem isso.
- `JtiBlacklistRepository` (Postgres, reset de senha) **e** `auth:revoked:jti` (Redis, access JWT) — dois mecanismos, nomes parecidos.

### Não é desalinhamento

- Access JWT em cookie em vez de memória JS: **mais seguro**, mantenha.
- Sem Prometheus/Grafana no compose: escopo TCC.
- Sem `request_line_item`: linhas de tabela vivem em `dados` JSONB.

---

## 8. O que pode melhorar (sem escalar)

Coisas de **meia tarde**, não de sprint enterprise:

| Melhoria | Por quê | Esforço |
|----------|---------|---------|
| Ligar Flyway no perfil `dev` | Uma verdade de schema | baixo |
| Mailpit no compose (1025/8025) | Smoke de e-mail/outbox | baixo |
| Healthcheck MinIO sem `curl` | Compose sobe sempre | baixo |
| Handler de `InsufficientAuthorityException` sem Spring no domain | Pureza + banca | baixo |
| `OutboxEventPublisher` no transition | Um port a menos de infra cruzada | baixo |
| Um helper único de `_links` (string \| HAL) no guia já existe; no back, opcional | Contrato | médio — **não priorizar** |
| ArchUnit “domain não importa Spring” | Trava regressão | baixo, 1 teste |

**Não fazer neste TCC:** microserviços, assemblers em todos os recursos, versionamento imutável de RequestType, Kubernetes, Vault, RabbitMQ, cache distribuído sofisticado, CQRS extra.

---

## 9. Gaps — ainda não implementado (backlog honesto)

| Item | Precisa para o MVP da banca? |
|------|------------------------------|
| Frontend-web (só existe guia em `frontend-web/docs/`) | Sim, para demo UI — fora deste relatório |
| Exchange do JWT `ott` → sessão | Demo deep-link “mágica”; workaround: login normal + URL |
| `request_type_version` + FK na instância | Não |
| Enum DRAFT/PUBLISHED (hoje `ativo`) | Não |
| `request_line_item` | Não |
| Stack Prometheus/Grafana/Loki | Não |
| Mailpit no compose | Conveniência de demo de e-mail |
| Lookup `/academico/disciplinas` como no seed | Front usa curso do wizard |
| Firebase FCM em produção (config existe, JSON vazio) | Push opcional |
| Testes de integração de **todos** os módulos (muitos GEs ainda sem IT) | Engine + anexos já cobrem o núcleo DRY |

---

## 10. Fluxo ponta a ponta (auth + engine + infra)

```
Browser
  POST /auth/login  → Argon2 + JWT(sid) cookie + refresh cookie + Redis session
  GET  /auth/csrf   → XSRF-TOKEN
  GET  /requests/types
  POST /requests    → FormSchemaValidator + estado initial + outbox
  GET  /requests/{id} → WorkflowEngine.allowedTransitions → _links
  POST /requests/{id}/transitions → engine + request_event + outbox (mesma TX)
  OutboxDispatcher @ 5s → e-mail / deep-link ?ott=
MinIO 9000 ← PUT presigned ← confirm SHA-256
Postgres 5432 ← Flyway (perfil prod)
Redis 6379 ← sid + cache dashboard
```

---

## 11. Veredito

O backend **está no desenho do TCC**: um motor, N tipos, FGAC, cookies, Redis de sessão, Outbox, Flyway, MinIO, módulos com domain/application/api/infrastructure na maior parte dos contextos de negócio.

O desalinhamento real não é “falta de microserviço”. É **hibridismo de camadas** (GET no controller, alguns ports pulados), **perfil `dev` sem Flyway**, **Spring escorrendo no domain da engine**, e **compose incompleto** (mail + healthcheck MinIO). Nada disso impede defender o MVP se o stack Docker (`prod` + postgres + redis + minio) for o caminho da demo.

**Fonte complementar do motor:** `foundationDocs/analysis/workflow_engine_gap_report.md` (fases 0–6 do workflow). Este relatório cobre o sistema inteiro no backend, não só solicitações.

---

*Auditoria estática de código e compose — serviços não foram levantados neste passe. Se o `backend` não sobe no Compose, o primeiro suspeito é o healthcheck do MinIO.*
