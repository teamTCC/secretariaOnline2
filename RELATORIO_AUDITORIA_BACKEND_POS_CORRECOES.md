# Relatório de fechamento — correções da auditoria backend (§7)

**Data:** 2026-08-29  
**Origem:** `RELATORIO_AUDITORIA_BACKEND.md` §7 (P1 + P2)  
**Escopo:** `backend/` + `ops/` (sem frontend).  
**Régua:** ajustes proporcionais ao TCC — sem “refatorar o mundo”.

Este documento fecha o ciclo da auditoria: o que foi corrigido, o que permanece de propósito, e como verificar.

---

## Veredito

Os itens **P1** da §7 foram corrigidos. Os itens **P2** que cabiam em um passe proporcional também. O backend continua no desenho do TCC (um motor, N tipos, FGAC, cookies, Redis, Outbox, Flyway SQL-first).

**Camadas de persistência (passe extra):** JPA (`*Entity` / `*JpaRepository`) fica em `infrastructure/persistence`. Controllers **não** persistem. BFF **não** injeta JPA de outros módulos — só ports de leitura. Use cases do próprio bounded context ainda podem usar JPA do módulo (atalho TCC); o comando devolve UUID/DTO, não a entity, para o HTTP não vazar persistência.

**Build verificado neste passe:**

- `:app:compileKotlin` — OK  
- `:app:test --tests br.ufpr.sept.so2.architecture.DomainLayerArchTest` — OK (domain puro + controllers sem JPA + BFF sem JPA)  
- `:modules:iam:test` / `:modules:solicitacoes:test` — OK  
- ktlint nos módulos tocados — OK 

---

## 1. P1 — o que mais doía (corrigido)

| # | Achado da auditoria | O que foi feito | Onde |
|---|---------------------|-----------------|------|
| 1 | `application-dev.yml` desligava Flyway e usava `ddl-auto: update` | Perfil `dev` agora é **Flyway only**: `flyway.enabled: true` + `ddl-auto: validate` (igual `prod`/`test`) | `backend/app/src/main/resources/application-dev.yml` |
| 2 | `InsufficientAuthorityException` estendia `AccessDeniedException` (Spring no `domain/`) | Hierarquia pura: `WorkflowException : RuntimeException`. Handler do módulo mapeia 403/422 | `WorkflowEngine.kt`, `SolicitacoesExceptionHandler.kt` |
| 3 | Use cases injetavam `OutboxEventJpaRepository` | Comandos passam a `OutboxEventPublisher.enqueue` (port em `shared`). JPA do outbox fica no dispatcher/admin + `OutboxEventPublisherImpl` | solicitações, IAM, presença, TCC, estágio, formativas, BFF export |
| 4 | Healthcheck MinIO com `curl` + `depends_on: service_healthy` | Healthcheck `mc ready local`; `backend.depends_on.minio: service_started` (healthcheck ruim **não** trava o stack) | `ops/docker-compose.yml` |
| 5 | Dois shapes de `_links` (HAL `EntityModel` vs `Map<String,String>`) | Detalhes de Request / Profile / Event / Estágio / TCC usam `Map<String,String>` + `@JsonProperty("_links")`. ITs já esperavam string | DTOs de resposta + controllers de detalhe |

### 1.1 Schema: uma fonte da verdade

Gradle local (`dev`) e Docker (`prod`) agora compartilham Flyway. Hibernate **valida** o mapping contra o schema migrado; não cria colunas por conta própria.

Próxima migration nova: **V019** (já aplicada neste passe — ver §2). Não editar V001–V018.

### 1.2 Domain sem Spring

```
WorkflowException
  ├── InvalidTransitionException     → 422 (handler do módulo)
  ├── InsufficientAuthorityException → 403
  └── TransitionGuardFailedException → 403
```

`TransitionGuardFailedException` **não** herda mais `IllegalStateException` (que o handler global trata como 409). O 403 fica explícito no `SolicitacoesExceptionHandler`.

ArchUnit trava regressão: classes em `..domain..` não podem depender de `org.springframework..`, `jakarta.persistence..`, `org.hibernate..`.

### 1.3 Outbox pelo port shared

Assinatura: `OutboxEventPublisher.enqueue(eventType, aggregateType, aggregateId, payload)`.

O único uso restante de `OutboxEventJpaRepository` em application é o admin/dispatcher de `notificacoes` (leitura da fila) — adequado.

### 1.4 Compose MinIO

Comentário do compose atualizado para **quatro** serviços (postgres, redis, minio, backend). Postgres e Redis continuam `service_healthy`; MinIO é `service_started`.

---

## 2. P2 — desalinhamentos conscientes (tratados ou deixados)

| Achado | Status | Notas |
|--------|--------|--------|
| Controllers GET gordos (lista/detalhe JPA) | **Corrigido** | GETs em `*Query`; writes em `*UseCase`. Controller só HTTP |
| Sem `assembler/`, sem `ports/in`, sem ArchUnit | **ArchUnit feito** | Sem pasta `assembler/` nem `ports/in` em todos os módulos — **não exigido neste TCC** |
| `LoginUseCase` chamava `JwtTokenService` / Argon2 | **Corrigido** | `TokenServicePort` + `PasswordHasherPort`; `ParsedToken` evita jjwt na application |
| `TransitionGuardFailedException` × 409 global | **Corrigido** | Ver P1.2 |
| BFF `ReportsController` / `SearchController` importavam JPA de 5+ módulos | **Corrigido** | `ReportsQuery` / `SearchQuery` / `ExportJobsUseCase` usam ports (`IamBffReadPort`, `SolicitacaoBffReadPort`, `AcademicoReadPort`, `PresencaBffReadPort`, `FormativaBffReadPort`, `EstagioSummaryPort`, `TccDashboardPort`, `ExportJobPort`) |
| `arquivos/`, `auditoria/`, `notificacoes/` sem domain rico | **Mantido** | Adapters. OK |
| Seed `x-ui.endpoint: /academico/disciplinas` | **Corrigido** | Alias `GET /academico/disciplinas` (filtro opcional `idCurso`/`search`; `enrolled`/`tipo` ignorados para não 400) |
| Deep-link `?ott=` sem exchange | **Corrigido** | `POST /auth/ott` (`ExchangeOttUseCase`); `permitAll` + CSRF ignore; rate limit como login |
| `request_type_version` / snapshot no publish | **Corrigido** | Flyway `V019__request_type_versioning.sql` + stamp na abertura/draft + schema da versão no GET detalhe |
| `JtiBlacklistRepository` (Postgres) vs `auth:revoked:jti` (Redis) | **Nomes na application** | Port de e-mail: `EmailOneTimeTokenStore`. Redis continua `TokenRevocationPort` / `auth:revoked:jti:`. Tabela Postgres ainda `jti_blacklist` (sem migration de rename) |

### 2.1 `POST /auth/ott`

Consome o JWT one-time (audience `request:{uuid}`), revoga o JTI no Redis e emite cookies de sessão (`access_token` + `refresh_token`), no mesmo contrato do login.

Workaround antigo (login normal + URL) continua válido.

### 2.2 Versionamento de tipo (V019)

- Tabela `request_type_version` (schema + workflow imutáveis por versão).  
- Backfill versão `1` para tipos `ativo = true`.  
- FK `request.id_request_type_version`.  
- `ManageRequestTypeUseCase.publish` grava snapshot.  
- Open/SaveDraft carimbam a versão vigente.  
- GET detalhe prefere o `form_schema` da versão da instância.

---

## 3. O que **não** foi feito (de propósito)

Não entra neste TCC / neste passe:

- Pasta `assembler/` HATEOAS em todos os recursos  
- `ports/in` em todos os use cases  
- Trocar JPA intra-módulo dos use cases por ports (só **cross-module** / BFF foi portado)  
- Domain rico em `arquivos` / `auditoria` / `notificacoes`  
- Rename da tabela `jti_blacklist` (só o port de application mudou de nome)  
- Rename da classe adapter `JtiBlacklistPersistenceAdapter` (infra; tabela homônima)  
- Mailpit no compose (era §5/§8 da auditoria, **não** §7)  
- Prometheus / Grafana / Loki / RabbitMQ / Kubernetes  
- Frontend (incluindo consumo de `POST /auth/ott`)  
- Enum DRAFT/PUBLISHED no lugar de `ativo`  
- Tabela `request_line_item` (linhas continuam em `dados` JSONB)

`AccessDeniedException` do Spring ainda aparece em **application** (ownership de anexo/draft/protocolo) — isso não viola a regra de domain puro.

---

## 4. Como verificar

| Item | Como |
|------|------|
| Flyway no `dev` | Subir com `SPRING_PROFILES_ACTIVE=dev`. Log deve mostrar migrate/validate. Se o mapping divergir do SQL → falha no boot (`ddl-auto: validate`), não silêncio com colunas extra |
| Domain puro | `./gradlew :app:test --tests br.ufpr.sept.so2.architecture.DomainLayerArchTest` |
| Controllers / BFF sem JPA | O mesmo teste: `controllersMustNotDependOnJpa` + `bffMustNotDependOnJpa` |
| 403 de autoridade/guard | Transição sem capability / guard falho → RFC 7807 **403**, não 409 |
| Outbox | Após `POST /requests/{id}/transitions`, linha em `outbox_event` na mesma transação (dispatcher @ 5s) |
| MinIO / Compose | `docker compose -f ops/docker-compose.yml up postgres redis minio` — backend **não** espera healthcheck MinIO |
| `_links` | GET detalhe: `$._links.self` (e demais rels) são **strings**, não objetos HAL `{href}` |
| Alias disciplinas | `GET /academico/disciplinas` e `GET /academico/cursos/{id}/disciplinas` |
| OTT | `POST /auth/ott` body `{ "token": "<jwt>" }` → 200 + cookies; replay → 401 |
| Versionamento | Publish de tipo incrementa `request_type_version`; request nova aponta a versão; GET detalhe usa o snapshot |

Comando útil de regressão rápida:

```bash
cd backend
./gradlew :app:compileKotlin :app:test --tests br.ufpr.sept.so2.architecture.DomainLayerArchTest :modules:iam:test :modules:solicitacoes:test
```

---

## 5. Mapa de arquivos (principais)

| Área | Arquivos |
|------|----------|
| Flyway dev | `backend/app/src/main/resources/application-dev.yml` |
| V019 | `backend/app/src/main/resources/db/migration/V019__request_type_versioning.sql` |
| Domain exceptions | `backend/modules/solicitacoes/.../domain/WorkflowEngine.kt` |
| HTTP mapping | `backend/modules/solicitacoes/.../api/SolicitacoesExceptionHandler.kt` |
| Outbox port | `backend/shared/.../outbox/OutboxEventPublisher.kt` |
| Compose | `ops/docker-compose.yml` |
| ArchUnit | `backend/app/src/test/kotlin/.../architecture/DomainLayerArchTest.kt` |
| Login ports | `TokenServicePort`, `PasswordHasherPort`, `LoginUseCase` |
| OTT | `ExchangeOttUseCase`, `AuthController` `POST /auth/ott`, `SecurityConfig` |
| JTI e-mail | `EmailOneTimeTokenStore` (Postgres); Redis permanece `auth:revoked:jti:` |
| BFF | `ReportsQuery`, `SearchQuery`, `ExportJobsUseCase` + ports nos módulos donos |
| Solicitações GET | `RequestQuery` |
| Disciplinas | `AcademicoController` `GET /disciplinas` |

---

## 6. Relação com a auditoria original

- **§6** (o que já estava certo) permanece válido.  
- **§7 P1** — fechado.  
- **§7 P2** — fechado no que era código/contrato; hibridismo de camadas e adapters “pobres” documentados como aceitáveis.  
- **§8** Mailpit / observabilidade — **não** deste passe.  
- **§9** Frontend, FCM produção, ITs de todos os módulos — backlog. O gap “exchange OTT”, “lookup disciplinas” e “request_type_version” saíram do backlog.  
- **§10** fluxo ponta a ponta agora inclui `POST /auth/ott` após o deep-link `?ott=` do outbox; Flyway também no perfil `dev`.

---

*Correções aplicadas no código; stack Docker completo não foi levantado neste passe. Se o backend não subir no Compose, o suspeito MinIO+curl da auditoria original não se aplica mais — olhar Postgres/Redis healthy, JWT no `ops/.env` e Flyway validate.*
