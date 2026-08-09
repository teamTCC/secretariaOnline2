# Requisitos Não Funcionais — SecretariaOnline2

**Projeto:** SecretariaOnline2 — UFPR SEPT  
**Versão:** 1.0  
**Data:** 2026-06-23  
**Gerado a partir de:** `.cursorrules`, `agents/security-engineer.md`, `agents/backend-architect.md`, `agents/database-engineer.md`, `agents/devops-engineer.md`, `agents/ux-ui-specialist.md`, `agents/workflow-engine-specialist.md`, `analise_arquitetural §17`  
**Total RNF neste arquivo:** 42  
**Classificação:** ISO/IEC 25010 adaptado

---

## Índice por categoria

| Categoria | Sigla | Quantidade | Prioridade máxima |
|-----------|-------|:----------:|:-----------------:|
| [Segurança](#1-segurança-rnf-sec) | RNF-SEC | 10 | P0 |
| [Desempenho](#2-desempenho-rnf-des) | RNF-DES | 6 | P0 |
| [Disponibilidade](#3-disponibilidade-rnf-dis) | RNF-DIS | 4 | P0 |
| [Usabilidade / Acessibilidade](#4-usabilidade--acessibilidade-rnf-ux) | RNF-UX | 5 | P0 |
| [Manutenibilidade](#5-manutenibilidade-rnf-man) | RNF-MAN | 5 | P0 |
| [Confiabilidade](#6-confiabilidade-rnf-con) | RNF-CON | 4 | P0 |
| [Portabilidade](#7-portabilidade-rnf-por) | RNF-POR | 3 | P1 |
| [Compatibilidade](#8-compatibilidade-rnf-cmp) | RNF-CMP | 3 | P1 |
| [Conformidade Legal](#9-conformidade-legal-rnf-lgl) | RNF-LGL | 2 | P0 |

---

## 1. Segurança (RNF-SEC)

---

### RNF-SEC-01 — Armazenamento de senha com Argon2id

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-01 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/security-engineer.md §Password Security`; `analise_arquitetural §17.4`; US-F0-001 (RN-F0.1-02); ADR-004 |

**Descrição:** O sistema deve armazenar todas as senhas de usuário exclusivamente com o algoritmo Argon2id, com os parâmetros mínimos recomendados pela OWASP: memória ≥ 47 MB, iterações ≥ 1, paralelismo = 1. É proibido o uso de MD5, SHA-1, SHA-256 ou bcrypt para hashing de senhas.

**Métrica:** 100% dos hashes na coluna `usuario.senha_hash` prefixados com `$argon2id$`; zero ocorrência de MD5 em código e banco após migração; detekt falha em qualquer uso de `MessageDigest.getInstance("MD5")`.

**Verificação:** Revisão de código (detekt rule custom); teste unitário de `Argon2PasswordService.hash()`; auditoria pós-migração ETL com query `SELECT count(*) FROM usuario WHERE senha_hash NOT LIKE '$argon2id$%'`.

**RF relacionados:** RF-F0-001, RF-F0-003

---

### RNF-SEC-02 — Autenticação JWT stateless com RS256

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-02 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/security-engineer.md §JWT Token Lifecycle`; ADR-005; `.cursorrules §Security Baseline` |

**Descrição:** O sistema deve emitir tokens de acesso JWT assinados com RS256 (chave privada RSA), com TTL de 15 minutos. O refresh token deve ser opaco (UUID armazenado em banco), com TTL de 7 dias, armazenado no cliente como cookie `httpOnly; Secure; SameSite=Lax`. O access token deve ser armazenado apenas em memória JavaScript (nunca em `localStorage`).

**Métrica:** `exp(accessToken) - iat(accessToken) = 900s`; `expires_at(refreshToken) - created_at = 604800s`; presença de `Set-Cookie: refreshToken=…; HttpOnly; Secure; SameSite=Lax` na resposta de login/refresh.

**Verificação:** Teste de integração verificando os campos `exp` e `iat` do payload JWT; teste de integração verificando os atributos do cookie no response header; pentest de extração de token da memória.

**RF relacionados:** RF-F0-001, RF-F0-002

---

### RNF-SEC-03 — Refresh token rotativo com detecção de reutilização

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-03 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/security-engineer.md §Refresh Token Strategy` |

**Descrição:** O mecanismo de refresh token deve implementar rotação obrigatória: a cada uso válido, o token consumido é marcado como `isUsed = true` e um novo token opaco é emitido. O sistema deve detectar apresentações de tokens já consumidos como indicador de comprometimento (token reuse detection). O comportamento observável de resposta a essa detecção (invalidação de todas as sessões e registro de auditoria) está especificado em **RF-F0-001 CA-10** como requisito funcional.

> **Nota de classificação:** A estratégia de rotação e a detecção de reutilização são atributos de segurança (RNF). As ações de resposta do sistema (o que fazer quando detectada a reutilização) são comportamento funcional especificado em RF-F0-001.

**Métrica:** 100% dos refresh tokens com campo `is_used` atualizado após uso; zero tokens consumidos (`is_used = true`) aceitos em nova requisição; sistema retorna HTTP 401 para qualquer token já consumido.

**Verificação:** Teste de integração: usar refresh token → verificar novo token emitido + token anterior marcado `is_used = true`; tentar reutilizar o token consumido → verificar HTTP 401; cenário de reuso detectado verificado em RF-F0-001 CA-10.

**RF relacionados:** RF-F0-001

---

### RNF-SEC-04 — Rate limiting em autenticação

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-04 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | US-F0-001 RN-F0.1-06, RN-F0.1-09; `fluxos_por_perfil.md` §1 F0.1; RF-F0-001; CONF-004 (precedência HU > agents) |

**Descrição:** O sistema deve limitar tentativas de autenticação para mitigar ataques de força bruta, implementado via Bucket4j. Limites: `POST /auth/login` — **5 tentativas por minuto** por par `IP + identificador`; `POST /auth/recuperar-senha` — **3 tentativas por hora** por IP. Quando o limite é excedido, o sistema retorna HTTP 429 com corpo RFC 7807 e header `Retry-After`. O bloqueio temporário de conta após falhas consecutivas é um comportamento funcional especificado em **RF-F0-001 CA-7** (não um atributo de qualidade mensurável neste RNF).

> **Nota de classificação:** A limitação de taxa de requisições (rate limiting) é um mecanismo de segurança (RNF). O bloqueio de conta como transição de estado (ativo → bloqueado por 15 min após 10 falhas) é comportamento funcional com especificação em RF-F0-001.

**Métrica:** HTTP 429 retornado na 6ª tentativa de login dentro de 1 minuto para o mesmo par `IP + identificador`; header `Retry-After` presente; HTTP 429 na 4ª tentativa de recuperação dentro de 1 hora para o mesmo par e-mail + IP.

**Verificação:** Teste de integração com Bucket4j (6 tentativas em < 1 min → 429); teste de recuperação de senha (4 tentativas/hora → 429); alinhado com RF-F0-001 CA-6 e RF-F0-002 CA-7.

**RF relacionados:** RF-F0-001, RF-F0-002

---

### RNF-SEC-05 — Tokens de uso único para deep-links (JWT + JTI blacklist)

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-05 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/security-engineer.md §One-Time Use Token`; `analise_arquitetural §1 achado #3`; ADR-005 |

**Descrição:** Todo link enviado por email (recuperação de senha, ação em solicitação por deep-link de professor) deve ser protegido por JWT RS256 de uso único, com JTI único armazenado em blacklist após consumo. O TTL máximo é 24 horas para recuperação de senha e 72 horas para ações de deliberação. O sistema deve rejeitar qualquer tentativa de reutilização com HTTP 401.

**Métrica:** 100% dos tokens de deep-link com campo `jti`; 0 reutilizações aceitas após consumo; TTL verificado no servidor (nunca apenas no cliente).

**Verificação:** Teste de integração: consumir token → tentar reutilizar → esperar HTTP 401; auditoria de query na tabela `jti_blacklist` após consumo.

**RF relacionados:** RF-F0-002, RF-F0-003, RF-TR-001

---

### RNF-SEC-06 — Cabeçalhos de segurança HTTP obrigatórios

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-06 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/security-engineer.md §Security Headers`; OWASP Security Headers Project |

**Descrição:** Todas as respostas HTTP do backend devem incluir os seguintes cabeçalhos de segurança: `Strict-Transport-Security: max-age=31536000; includeSubDomains`; `X-Content-Type-Options: nosniff`; `X-Frame-Options: DENY`; `Referrer-Policy: strict-origin-when-cross-origin`; `Permissions-Policy: geolocation=(), camera=(), microphone=()`; Content-Security-Policy com nonce para scripts inline.

**Métrica:** 100% das respostas com os 6 cabeçalhos listados presentes; ausência de `X-Powered-By`; CSP sem `unsafe-eval` ou `unsafe-inline` sem nonce.

**Verificação:** Teste automatizado com Mozilla Observatory (score ≥ A); scan com OWASP ZAP no pipeline de CI.

**RF relacionados:** Todos os RF (transversal)

---

### RNF-SEC-07 — FGAC por capabilities — proibido autorização por role

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-07 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/security-engineer.md §FGAC`; ADR-002; `.cursorrules §FGAC` |

**Descrição:** Toda verificação de autorização deve ser feita por **capability** (authority) no formato `dominio.acao` (ex.: `request.deliberate`, `event.manage`), nunca por role (ex.: `ROLE_SECRETARIO`). O backend deve usar exclusivamente `@PreAuthorize("hasAuthority('dominio.acao')")`. O frontend deve renderizar botões condicionalmente apenas a partir de `_links` na resposta HATEOAS, sem verificar `user.role`.

**Métrica:** Zero ocorrências de `hasRole(` em código backend; zero ocorrências de `user.role ===` em lógica de renderização condicional no frontend; ArchUnit detecta e falha na build se `hasRole` for usado.

**Verificação:** Regra ArchUnit `noClassesShouldUseHasRole()`; ESLint rule customizada que falha em `user.role ===`; revisão de código em PR.

**RF relacionados:** RF-TR-005, todos os RF com ação restrita

---

### RNF-SEC-08 — Segredos via variáveis de ambiente — zero hardcoded

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-08 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/devops-engineer.md §Secret Rules`; `agents/security-engineer.md §Anti-Patterns`; `analise_arquitetural §1 achado #2` |

**Descrição:** Nenhuma credencial, chave JWT, senha de banco, API key ou token de serviço externo pode ser hardcoded em código-fonte, `application.yml`, `docker-compose.yml` ou qualquer arquivo versionado. Todos os segredos são injetados via variáveis de ambiente. O arquivo `.env` deve estar no `.gitignore`; o `.env.example` (sem valores reais) deve estar versionado.

**Métrica:** 0 segredos detectados pelo gitleaks em qualquer commit; `.env` ausente do repositório; `.env.example` presente e documentado.

**Verificação:** `gitleaks detect` no pre-commit hook e no pipeline CI (step `Check for secrets`); revisão de `.gitignore` e `.env.example` em PR.

**RF relacionados:** Todos (transversal de infraestrutura)

---

### RNF-SEC-09 — Anti-enumeração em endpoints de autenticação

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-09 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | US-F0-001 CA-03; US-F0-002; `agents/security-engineer.md §Anti-Patterns` |

**Descrição:** Os endpoints de autenticação e recuperação de senha devem retornar respostas idênticas independentemente de o identificador existir ou não no banco, evitando enumeração de usuários. O tempo de resposta para "usuário não encontrado" deve ser equiparado ao de "senha incorreta" com uso de hash dummy (Argon2id dummy verify).

**Métrica:** Diferença de tempo de resposta entre "usuário inexistente" e "senha incorreta" < 10ms (medida em P95 por 100 requisições); mensagem de erro idêntica nos dois casos.

**Verificação:** Teste de tempo de resposta comparativo com JMeter; revisão de código do `LoginUseCase` e `RecuperarSenhaUseCase`.

**RF relacionados:** RF-F0-001, RF-F0-002

---

### RNF-SEC-10 — CORS com origens explícitas

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-10 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | `agents/security-engineer.md §Spring Security Configuration`; OWASP |

**Descrição:** A configuração CORS do backend deve listar explicitamente as origens permitidas a partir de variável de ambiente (`CORS_ALLOWED_ORIGINS`). É proibido o uso de `allowedOrigins = listOf("*")`. As origens permitidas incluem o domínio do frontend web e o scheme `capacitor://` para o app mobile.

**Métrica:** Zero ocorrências de `*` na configuração de CORS; header `Access-Control-Allow-Origin` presente e com valor específico (não `*`) em todas as respostas a requisições cross-origin.

**Verificação:** Teste de integração com `Origin: https://evil.example.com` esperando resposta sem `Access-Control-Allow-Origin`; revisão de `SecurityConfig.corsConfigSource()`.

**RF relacionados:** Todos os RF de API

---

## 2. Desempenho (RNF-DES)

---

### RNF-DES-01 — Latência P95 de endpoints de listagem

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DES-01 |
| **Categoria** | Desempenho |
| **Prioridade** | P0 |
| **Fonte** | `.cursorrules §Success Metrics`; `analise_arquitetural §17.3` |

**Descrição:** O tempo de resposta P95 de todos os endpoints de listagem paginada (ex.: `GET /solicitacoes`, `GET /eventos`, `GET /usuarios`) deve ser inferior a 300 ms em condições normais de carga (≤ 50 usuários simultâneos).

**Métrica:** P95 latency < 300ms medido pelo Prometheus/Grafana para `http_server_requests_seconds{uri=~".*/list.*|.*/page.*"}` em janela de 5 minutos.

**Verificação:** Teste de carga com k6 (50 VUs, 2 minutos) em ambiente de staging; dashboard Grafana com alerta configurado para P99 > 2s.

**RF relacionados:** RF-F1-001, RF-F5-001, RF-F5-002, RF-TR-001

---

### RNF-DES-02 — Latência P95 de endpoints de detalhe

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DES-02 |
| **Categoria** | Desempenho |
| **Prioridade** | P0 |
| **Fonte** | `.cursorrules §Success Metrics`; `analise_arquitetural §17.3` |

**Descrição:** O tempo de resposta P95 de todos os endpoints de detalhe (ex.: `GET /solicitacoes/{id}`, `GET /usuarios/{id}`) deve ser inferior a 100 ms em condições normais de carga.

**Métrica:** P95 latency < 100ms para endpoints `GET /{resource}/{id}` medido via Micrometer no Prometheus.

**Verificação:** Teste de carga com k6; métrica Prometheus `http_server_requests_seconds_bucket{method="GET"}` com P95 calculado via `histogram_quantile(0.95, ...)`.

**RF relacionados:** Todos os RF de consulta por ID

---

### RNF-DES-03 — Latência total de login

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DES-03 |
| **Categoria** | Desempenho |
| **Prioridade** | P0 |
| **Fonte** | `.cursorrules §Success Metrics`; `agents/security-engineer.md §Password Security` |

**Descrição:** O tempo de resposta P95 do endpoint `POST /auth/login` — incluindo a verificação Argon2id (operação intencional e custosa) — deve ser inferior a 800 ms.

**Métrica:** P95 de `http_server_requests_seconds{uri="/auth/login", method="POST"}` < 800ms.

**Verificação:** Teste de integração com 10 chamadas consecutivas medindo tempo de resposta; teste de carga com k6 (10 VUs em login simultâneo).

**RF relacionados:** RF-F0-001

---

### RNF-DES-04 — Tempo de carregamento do dashboard (FCP e LCP)

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DES-04 |
| **Categoria** | Desempenho |
| **Prioridade** | P1 |
| **Fonte** | `.cursorrules §Success Metrics`; Web Vitals |

**Descrição:** O carregamento inicial do dashboard (`/inicio`) deve atingir First Contentful Paint (FCP) inferior a 1,5 s e Largest Contentful Paint (LCP) inferior a 3 s em conexão 4G simulada (10 Mbps, 40ms RTT).

**Métrica:** FCP < 1500ms; LCP < 3000ms; medidos via Lighthouse CI em pipeline ou Playwright com throttling.

**Verificação:** Step `npm run lighthouse` no CI com budget de performance; Playwright com perfil de rede `Fast 4G` medindo `page.evaluate(() => performance.getEntriesByType('paint'))`.

**RF relacionados:** RF-F1-001, RF-F3-001, RF-F5-001

---

### RNF-DES-05 — Latência de despacho do Outbox

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DES-05 |
| **Categoria** | Desempenho |
| **Prioridade** | P1 |
| **Fonte** | `.cursorrules §Outbox Pattern`; `agents/devops-engineer.md §Prometheus Alerts`; `analise_arquitetural §17.6` |

**Descrição:** O tempo entre a inserção de um evento na tabela `outbox_event` (status `PENDING`) e seu despacho efetivo (status `SENT`) deve ser inferior a 30 segundos em condições normais. O dispatcher é executado a cada 5 segundos pelo `@Scheduled`.

**Métrica:** Métrica personalizada `secretaria_outbox_pending_total` < 200 em janela de 2 minutos; alerta Grafana disparado quando > 200 por mais de 2 minutos.

**Verificação:** Teste de integração que insere 50 eventos no outbox e aguarda 30s para verificar status `SENT`; alerta Prometheus `OutboxQueueBacklog` configurado conforme `devops-engineer.md`.

**RF relacionados:** RF-TR-002, RF-TR-007

---

### RNF-DES-06 — Toda coleção retornada deve ser paginada

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DES-06 |
| **Categoria** | Desempenho |
| **Prioridade** | P0 |
| **Fonte** | `agents/database-engineer.md §Anti-Patterns`; `agents/backend-architect.md` |

**Descrição:** Nenhum endpoint de listagem pode retornar coleções sem paginação. Todo `GET /{resource}` deve aceitar parâmetros `page`, `size` (padrão 20, máximo 100) e `sort`. Queries sem `Pageable` devem falhar no ArchUnit.

**Métrica:** Zero endpoints de listagem sem `Pageable` no assinatura do repositório; tamanho máximo de página = 100 (validado com `@Max(100)` no parâmetro `size`).

**Verificação:** Regra ArchUnit `repositoriesMustUsePagination()`; teste de integração que verifica retorno de `Page<T>` com campo `totalElements`.

**RF relacionados:** Todos os RF de listagem

---

## 3. Disponibilidade (RNF-DIS)

---

### RNF-DIS-01 — Backup diário automático do PostgreSQL

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DIS-01 |
| **Categoria** | Disponibilidade / Recuperação de Desastres |
| **Prioridade** | P0 |
| **Fonte** | `agents/devops-engineer.md §PostgreSQL Backup Strategy`; `analise_arquitetural §17.6` |

**Descrição:** O sistema deve garantir que o banco de dados PostgreSQL seja recuperável em caso de falha ou desastre. Para tanto, backups automáticos devem ser executados diariamente no formato `pg_dump --format=custom`, armazenados em armazenamento de objetos (bucket `backups` no MinIO/S3), com retenção mínima de 14 dias. O procedimento operacional de backup (scheduling, ferramentas, paths) é detalhado em `agents/devops-engineer.md §PostgreSQL Backup Strategy`.

> **Nota de classificação:** A capacidade de recuperação de dados é um atributo de disponibilidade (RNF). Os procedimentos operacionais específicos (cron job, comandos pg_dump, configuração do MinIO) são responsabilidade da documentação de DevOps e do agente `devops-engineer`, não do documento de requisitos do sistema.

**Métrica:** Arquivo de backup gerado com intervalo ≤ 26 horas; 14 arquivos mínimos disponíveis no bucket `backups`; tamanho do arquivo de backup > 0 bytes.

**Verificação:** Job de CI semanal que verifica data do arquivo mais recente no bucket; alerta Prometheus se backup mais recente > 26h; restauração mensal conforme RNF-DIS-02.

**RF relacionados:** Todos (transversal de operação)

---

### RNF-DIS-02 — Restore testado mensalmente

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DIS-02 |
| **Categoria** | Disponibilidade |
| **Prioridade** | P1 |
| **Fonte** | `agents/devops-engineer.md §PostgreSQL Backup Strategy`; `analise_arquitetural §17.6` |

**Descrição:** O procedimento de restore do banco de dados deve ser executado e validado em ambiente de staging pelo menos uma vez por mês, garantindo que os backups são efetivamente utilizáveis em caso de desastre.

**Métrica:** Restore completado com sucesso em < 30 minutos; contagem de tabelas e registros do restore ≥ 95% da contagem de produção.

**Verificação:** GitHub Actions scheduled workflow mensal que executa `pg_restore` em banco temporário e roda queries de reconciliação; resultado documentado em `ops/restore-test-log.md`.

**RF relacionados:** Todos (transversal de operação)

---

### RNF-DIS-03 — Health checks e probes de readiness/liveness

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DIS-03 |
| **Categoria** | Disponibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/devops-engineer.md §Spring Actuator Configuration`; `analise_arquitetural §17.6` |

**Descrição:** O backend deve expor os endpoints de saúde via Spring Boot Actuator: `/actuator/health` (público, retorna `UP`/`DOWN`), `/actuator/health/liveness` e `/actuator/health/readiness` (para eventual deploy em Kubernetes). Os detalhes completos do health check devem ser acessíveis apenas para usuários autenticados com `system.observe`.

**Métrica:** `/actuator/health` retorna HTTP 200 com `{"status":"UP"}` em condições normais; retorna HTTP 503 se PostgreSQL ou MinIO estiver indisponível.

**Verificação:** Teste de integração que simula falha de conexão com Postgres (Testcontainers stop) e verifica resposta 503; verificação em pipeline CI de que o endpoint responde antes de marcar o deploy como concluído.

**RF relacionados:** RF-F7-007

---

### RNF-DIS-04 — Observabilidade: alertas críticos de operação

| Campo | Valor |
|-------|-------|
| **ID** | RNF-DIS-04 |
| **Categoria** | Disponibilidade |
| **Prioridade** | P1 |
| **Fonte** | `agents/devops-engineer.md §Prometheus Configuration`; `analise_arquitetural §17.6` |

**Descrição:** O sistema deve possuir monitoramento ativo com alertas configurados para condições operacionais críticas, de forma que a equipe de operação seja notificada proativamente antes que os usuários sejam impactados. As condições mínimas que devem gerar alerta são: (a) degradação de disponibilidade (taxa de erros HTTP 5xx); (b) degradação de desempenho (latência elevada); (c) acúmulo anormal na fila de comunicação assíncrona (Outbox); (d) esgotamento de recursos de banco de dados (pool de conexões). Os limiares específicos e a configuração dos alertas são responsabilidade de `agents/devops-engineer.md §Prometheus Configuration` e do arquivo `ops/prometheus/alerts.yml`.

> **Nota de classificação:** A obrigação de possuir alertas operacionais é um atributo de disponibilidade/observabilidade (RNF). As configurações específicas de cada alerta (expressões PromQL, thresholds exatos, destinatários) são procedimentos operacionais mantidos em `ops/prometheus/alerts.yml`, não no documento de requisitos.

**Métrica:** Pelo menos 4 regras de alerta ativas e funcionais cobrindo as condições (a)–(d); alertas disparando notificação em canal configurado quando condições são atingidas.

**Verificação:** Revisão dos arquivos em `ops/prometheus/`; teste manual de trigger de cada categoria de alerta em ambiente de staging.

**RF relacionados:** RF-F7-007

---

## 4. Usabilidade / Acessibilidade (RNF-UX)

---

### RNF-UX-01 — WCAG 2.1 Nível AA

| Campo | Valor |
|-------|-------|
| **ID** | RNF-UX-01 |
| **Categoria** | Usabilidade / Acessibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/ux-ui-specialist.md §Accessibility Standards`; `.cursorrules §Success Metrics` |

**Descrição:** Todas as telas do sistema devem conformar com WCAG 2.1 Nível AA. Requisitos mínimos: contraste ≥ 4,5:1 para texto normal, ≥ 3:1 para texto grande (≥ 18pt); todos os elementos interativos acessíveis via teclado; `aria-live="polite"` em conteúdo dinâmico (erros de formulário, atualizações de status); `role="dialog"` + `aria-labelledby` + focus trap em modais.

**Métrica:** Score de acessibilidade Lighthouse ≥ 90 em todas as páginas auditadas; zero violações de nível `critical` ou `serious` no axe-core; contraste validado em todas as combinações de texto × fundo nos tokens do design system.

**Verificação:** `axe-playwright` em testes E2E de todas as rotas P0; Lighthouse CI com `--accessibility` em pipeline; revisão manual com VoiceOver/NVDA nos fluxos de login e nova solicitação.

**RF relacionados:** Todos os RF (transversal de UX)

---

### RNF-UX-02 — Responsividade a partir de 375px

| Campo | Valor |
|-------|-------|
| **ID** | RNF-UX-02 |
| **Categoria** | Usabilidade / Acessibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/ux-ui-specialist.md §Responsive Breakpoints`; `.cursorrules §Success Metrics` |

**Descrição:** Todas as telas do sistema web devem ser funcionais e legíveis em dispositivos com largura mínima de 375px (iPhone SE). Nenhum conteúdo pode ser truncado ou sobrepostos em 375px. A navegação lateral deve colapsar para overlay em telas menores que `lg` (1024px).

**Métrica:** Zero overflow horizontal detectado em viewport de 375px; todos os alvos touch com mínimo de 44×44px; teclado virtual não encobre o botão de ação principal em mobile.

**Verificação:** Testes Playwright com `viewport: { width: 375, height: 812 }`; snapshot visual comparado a referência aprovada para cada rota P0.

**RF relacionados:** Todos os RF de UI (transversal)

---

### RNF-UX-03 — Navegação completa por teclado

| Campo | Valor |
|-------|-------|
| **ID** | RNF-UX-03 |
| **Categoria** | Usabilidade / Acessibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/ux-ui-specialist.md §Accessibility Standards`; WCAG 2.1 SC 2.1.1 |

**Descrição:** Todos os fluxos críticos (login, nova solicitação, deliberação) devem ser completáveis exclusivamente com teclado. A ordem de tabulação deve ser lógica e seguir a ordem visual dos elementos. O anel de foco (`focus ring`) deve ser sempre visível (`ring-2 ring-brand-primary`) quando o elemento tem foco via teclado.

**Métrica:** Fluxo login → dashboard completado em ≤ 12 pressões de Tab sem uso de mouse; anel de foco visível em 100% dos elementos interativos focados via teclado; zero elementos com `tabindex="-1"` sem alternativa de acionamento.

**Verificação:** Teste manual com teclado nos fluxos P0 documentado em checklist; `axe-playwright` verificando ausência de violação `keyboard-navigation`; revisão de PR com foco em `tabIndex` e `onKeyDown`.

**RF relacionados:** RF-F0-001, RF-F1-005, RF-F3-003

---

### RNF-UX-04 — UI orientada a capacidades via HATEOAS

| Campo | Valor |
|-------|-------|
| **ID** | RNF-UX-04 |
| **Categoria** | Usabilidade / Acessibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/ux-ui-specialist.md §HATEOAS-Aware UI Patterns`; ADR-002; `.cursorrules §FGAC` |

**Descrição:** O frontend nunca deve renderizar botões de ação ou elementos de navegação com base no perfil (`user.role`) do usuário. Toda renderização condicional de ações deve ser baseada na presença do link correspondente em `_links` da resposta da API (padrão HATEOAS), consumido via o hook `useActions(resource)`.

**Métrica:** Zero ocorrências de `user.role ===`, `user.profile`, ou `user.tipo` em lógica de renderização condicional no código frontend; 100% dos botões de ação condicional usando `useActions()`.

**Verificação:** ESLint rule customizada; revisão de PR; teste de integração que simula API retornando `_links` sem a ação e verifica ausência do botão correspondente no DOM.

**RF relacionados:** RF-TR-005, todos os RF com ações restritas por capability

---

### RNF-UX-05 — Estados de componente obrigatórios (loading, empty, error)

| Campo | Valor |
|-------|-------|
| **ID** | RNF-UX-05 |
| **Categoria** | Usabilidade / Acessibilidade |
| **Prioridade** | P1 |
| **Fonte** | `agents/ux-ui-specialist.md §Component Rules` |

**Descrição:** Todo componente que exibe dados carregados de API deve implementar obrigatoriamente três estados: (a) **loading** — exibir `DS/Skeleton` com a mesma estrutura visual do estado preenchido; (b) **empty** — exibir `DS/EmptyState` com mensagem descritiva e, quando pertinente, ação de criação; (c) **error** — exibir `DS/AlertBanner variante "danger"` com opção de retry.

**Métrica:** 100% das listas, tabelas e cards de dados com os três estados implementados; Storybook com stories para cada estado; zero janelas brancas ou spinners infinitos em caso de falha de API.

**Verificação:** Revisão de PR verificando estados; stories no Storybook para cada componente de dados; teste Playwright que mocka API com erro 500 e verifica exibição do AlertBanner.

**RF relacionados:** Todos os RF de listagem e detalhe

---

## 5. Manutenibilidade (RNF-MAN)

---

### RNF-MAN-01 — Cobertura de testes mínima

| Campo | Valor |
|-------|-------|
| **ID** | RNF-MAN-01 |
| **Categoria** | Manutenibilidade |
| **Prioridade** | P0 |
| **Fonte** | `.cursorrules §Success Metrics`; `analise_arquitetural §17.3` |

**Descrição:** O projeto deve manter cobertura mínima de testes automatizados: camada de domínio ≥ 85% (JUnit 5 + Kotest, puro Kotlin sem Spring), camada de aplicação (casos de uso) ≥ 70% (MockK), total do backend ≥ 75%. O frontend deve ter cobertura de hooks e utilitários ≥ 70% (Vitest).

**Métrica:** Relatório Jacoco: `domain` package ≥ 85%; `application` package ≥ 70%; overall ≥ 75%; Vitest coverage report `src/shared/` ≥ 70%.

**Verificação:** Step `./gradlew jacocoTestCoverageVerification` no CI (falha o build se abaixo do limiar); `npm run test -- --coverage --coverageThreshold` no CI frontend.

**RF relacionados:** Todos (qualidade transversal)

---

### RNF-MAN-02 — Zero violações de lint e format

| Campo | Valor |
|-------|-------|
| **ID** | RNF-MAN-02 |
| **Categoria** | Manutenibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/devops-engineer.md §GitHub Actions`; `.cursorrules §Linter & Quality Gates` |

**Descrição:** O código-fonte deve passar sem erros nas ferramentas de lint e formatação: backend com `ktlint` (formatação) e `detekt` (análise estática); frontend com `eslint` e `prettier`. Violações do tipo `error` bloqueiam o merge. Violações `warning` são toleradas se justificadas em comentário.

**Métrica:** `./gradlew ktlintCheck detekt` retorna exit code 0; `npm run lint` retorna exit code 0; zero erros de lint no CI em todas as PRs.

**Verificação:** Steps obrigatórios no GitHub Actions CI para ambos backend e frontend; pre-commit hook opcional para feedback local.

**RF relacionados:** Todos (qualidade transversal)

---

### RNF-MAN-03 — Complexidade ciclomática ≤ 10

| Campo | Valor |
|-------|-------|
| **ID** | RNF-MAN-03 |
| **Categoria** | Manutenibilidade |
| **Prioridade** | P1 |
| **Fonte** | `.cursorrules §Success Metrics`; `analise_arquitetural §17.1` |

**Descrição:** Nenhuma função ou método deve ter complexidade ciclomática de McCabe superior a 10. Funções com complexidade > 10 devem ser refatoradas antes do merge.

**Métrica:** Detekt rule `CyclomaticComplexMethod` configurada com `threshold: 10`; zero violações desta regra no CI.

**Verificação:** Detekt report no CI; revisão em PR de funções com muitos `if/when/for` aninhados.

**RF relacionados:** Todos (qualidade transversal)

---

### RNF-MAN-04 — DRY: blocos duplicados < 3%

| Campo | Valor |
|-------|-------|
| **ID** | RNF-MAN-04 |
| **Categoria** | Manutenibilidade |
| **Prioridade** | P1 |
| **Fonte** | `analise_arquitetural §17.1`; ADR-003; `.cursorrules §DRY` |

**Descrição:** A porcentagem de blocos de código duplicados no projeto deve ser inferior a 3%, medida por ferramenta de análise estática. A estratégia DRY é central para o TCC: o motor genérico de workflow substitui 57 telas quase idênticas do legado; o `DynamicForm` renderiza 19 tipos; value objects evitam validação duplicada de CPF, email e GRR.

**Métrica:** Duplicated blocks < 3% (SonarQube ou detekt `CodeSmell` rules); zero classes `RequestType`-específicas além das configurações JSON.

**Verificação:** SonarQube scan opcional em CI; detekt com regras de duplicação; revisão arquitetural em PR para novos tipos de solicitação.

**RF relacionados:** RF-TR-001 (motor de workflow), todos (qualidade transversal)

---

### RNF-MAN-05 — Clean Architecture enforced por ArchUnit

| Campo | Valor |
|-------|-------|
| **ID** | RNF-MAN-05 |
| **Categoria** | Manutenibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/backend-architect.md §Architectural Invariants`; `analise_arquitetural §17.2` |

**Descrição:** As regras de dependência da Clean Architecture devem ser verificadas automaticamente por ArchUnit nos testes: (a) o pacote `domain/` não pode importar nenhuma classe de `org.springframework`, `jakarta.persistence`, ou `org.hibernate`; (b) o pacote `infrastructure/` de um módulo não pode ser importado pelo `infrastructure/` de outro módulo; (c) módulos comunicam-se apenas por interfaces em `application/ports/`.

**Métrica:** Todos os testes ArchUnit passam (zero violações) em cada PR; build falha se alguma regra for violada.

**Verificação:** Testes ArchUnit no módulo `shared/` aplicados a todos os módulos; executados no step `./gradlew test` do CI.

**RF relacionados:** Todos (qualidade arquitetural transversal)

---

## 6. Confiabilidade (RNF-CON)

---

### RNF-CON-01 — Padrão Outbox: atomicidade garantida para notificações

| Campo | Valor |
|-------|-------|
| **ID** | RNF-CON-01 |
| **Categoria** | Confiabilidade |
| **Prioridade** | P0 |
| **Fonte** | `.cursorrules §Outbox Pattern`; `agents/backend-architect.md §Transaction Boundaries`; ADR-006 |

**Descrição:** Toda mudança de estado que deve gerar notificação (email, push) deve persistir o evento na tabela `outbox_event` na **mesma transação** da mudança de estado da entidade principal. É proibido enviar email/push de forma síncrona ou em transação separada. O dispatcher `@Scheduled` processa a fila de forma assíncrona e independente.

**Métrica:** 100% das transições de solicitação com correspondente `outbox_event` no banco; 0 emails enviados sem `outbox_event` correspondente; teste de rollback: se a transição falhar, nenhum outbox_event deve ser inserido.

**Verificação:** Teste de integração que força rollback após inserção do outbox e verifica ausência do evento no banco; revisão de código de todos os `UseCase` com notificação.

**RF relacionados:** RF-TR-002, RF-TR-007, RF-F1-005, RF-F5-002

---

### RNF-CON-02 — Migrations Flyway imutáveis e versionadas

| Campo | Valor |
|-------|-------|
| **ID** | RNF-CON-02 |
| **Categoria** | Confiabilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/database-engineer.md §Flyway Migration Conventions`; ADR-009; `analise_arquitetural §17.3` |

**Descrição:** Toda alteração de schema de banco de dados deve ser feita por arquivo Flyway `V###__descricao.sql` com número de versão incrementado. É proibido editar qualquer arquivo de migration já aplicado a qualquer ambiente. Correções devem ser novas migrations (ex.: `V012__fix_index_request.sql`). A validação Flyway (`spring.flyway.validate-on-migrate=true`) deve estar sempre ativa.

**Métrica:** Zero modificações em arquivos `V###__*.sql` após primeiro commit (verificado por hash Flyway); `spring.flyway.validate-on-migrate=true` em todos os profiles; migrations aplicadas em ordem estrita em todos os ambientes.

**Verificação:** Flyway falha a aplicação se checksum do arquivo mudar; CI valida que nenhum arquivo de migration existente foi alterado via `git diff --name-only HEAD~1 | grep -E 'V[0-9]+'`.

**RF relacionados:** Todos (persistência transversal)

---

### RNF-CON-03 — Respostas de erro no formato RFC 7807 Problem Details

| Campo | Valor |
|-------|-------|
| **ID** | RNF-CON-03 |
| **Categoria** | Confiabilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/backend-architect.md §Error Handling`; RFC 7807 |

**Descrição:** Todas as respostas de erro HTTP 4xx e 5xx devem ter Content-Type `application/problem+json` e body conformando com RFC 7807, contendo no mínimo os campos: `type` (URI de documentação do erro), `title` (descrição curta), `status` (código HTTP), `detail` (mensagem humanizada), `instance` (URI da requisição). O campo `errors` pode ser adicionado para erros de validação (HTTP 422).

**Métrica:** 100% das respostas de erro com `Content-Type: application/problem+json`; zero respostas com stacktrace exposto em produção; teste de integração para cada tipo de erro da aplicação.

**Verificação:** `GlobalExceptionHandler` cobre todas as exceções de domínio; teste de integração que verifica formato RFC 7807 para HTTP 401, 403, 404, 422, 429, 500.

**RF relacionados:** Todos os RF com fluxos de erro

---

### RNF-CON-04 — Eliminação de queries N+1

| Campo | Valor |
|-------|-------|
| **ID** | RNF-CON-04 |
| **Categoria** | Confiabilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/database-engineer.md §N+1 Prevention`; `analise_arquitetural §17` |

**Descrição:** Nenhum endpoint de listagem ou detalhe pode resultar em problema de N+1 queries (uma query adicional por item da lista). Todos os relacionamentos necessários para resposta devem ser carregados em um único `JOIN FETCH` ou via `@EntityGraph`. Queries de leitura devem usar projeções (`interface Projection`) para listas.

**Métrica:** Número de queries SQL por requisição em endpoints de listagem ≤ 3 (medido com Hibernate Statistics ou datasource-proxy em testes de integração).

**Verificação:** Testes de integração com `hibernate.statistics` habilitado verificando `getQueryExecutionCount()`; revisão de PR obrigatória para qualquer método de repositório que retorne coleções.

**RF relacionados:** RF-F5-002, RF-F1-005, RF-F7-001, todos os RF de listagem

---

## 7. Portabilidade (RNF-POR)

---

### RNF-POR-01 — Plataformas-alvo: Web e Mobile nativo

| Campo | Valor |
|-------|-------|
| **ID** | RNF-POR-01 |
| **Categoria** | Portabilidade |
| **Prioridade** | P1 |
| **Fonte** | `.cursorrules §Technology Stack §Mobile`; `analise_arquitetural §4` |

**Descrição:** O sistema deve funcionar em duas plataformas: (a) **Web**: React 18 + Vite, compatível com os navegadores listados em RNF-CMP-03; (b) **Mobile**: React Native + Expo SDK 50+, compatível com Android 10+ e iOS 15+. Funcionalidades P0 (login, dashboard, nova solicitação, confirmar presença) devem estar disponíveis em ambas as plataformas.

**Métrica:** App mobile publicável via Expo Go sem erros nativos; build web sem erros em todos os navegadores-alvo; fluxos P0 funcionais em ambas as plataformas.

**Verificação:** Build de produção do Expo (`eas build --platform all`) sem erros; testes Playwright na web + testes Detox/Expo e2e no mobile para fluxos P0.

**RF relacionados:** RF-F0-001, RF-F1-001, RF-F1-005, RF-F1-009

---

### RNF-POR-02 — Ambiente de desenvolvimento reproducível (Docker Compose)

| Campo | Valor |
|-------|-------|
| **ID** | RNF-POR-02 |
| **Categoria** | Portabilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/devops-engineer.md §Docker Compose`; `.cursorrules §DevEx` |

**Descrição:** O ambiente de desenvolvimento completo (PostgreSQL, MinIO, Mailpit, backend, Prometheus, Grafana, Loki) deve ser inicializável com um único comando `docker compose up -d` a partir do diretório raiz. O README deve documentar os 5 comandos necessários para subir tudo do zero.

**Métrica:** `docker compose up -d --wait` conclui sem erros em até 120 segundos em máquina com 8GB RAM; backend responde em `/actuator/health` com `{"status":"UP"}` após o compose concluir.

**Verificação:** CI step `docker compose up -d --wait && curl -f http://localhost:8080/actuator/health`; teste em máquina limpa por pelo menos um membro do time.

**RF relacionados:** Todos (transversal de infra)

---

### RNF-POR-03 — Armazenamento de arquivos via API S3-compatível

| Campo | Valor |
|-------|-------|
| **ID** | RNF-POR-03 |
| **Categoria** | Portabilidade |
| **Prioridade** | P1 |
| **Fonte** | `agents/devops-engineer.md`; ADR-011 |

**Descrição:** Todo armazenamento de arquivos (anexos de solicitação, comprovantes de formativas, certificados PDF gerados) deve usar a API S3-compatível via MinIO (desenvolvimento/TCC) ou AWS S3 (produção), configurável por variável de ambiente `STORAGE_ENDPOINT`. O código de aplicação não deve referenciar o sistema de arquivos local.

**Métrica:** Zero ocorrências de `File()`, `FileInputStream`, ou caminhos hardcoded no código da aplicação; upload e download funcionando com MinIO local e compatível com AWS S3 (mesmas chamadas de API).

**Verificação:** Teste de integração com Testcontainers MinIO; revisão de código no `ArquivosAdapter`; tentativa de troca para endpoint S3 real sem alteração de código.

**RF relacionados:** RF-F1-006, RF-F1-007, RF-TR-003

---

## 8. Compatibilidade (RNF-CMP)

---

### RNF-CMP-01 — API documentada em OpenAPI 3.x

| Campo | Valor |
|-------|-------|
| **ID** | RNF-CMP-01 |
| **Categoria** | Compatibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/backend-architect.md §OpenAPI Documentation Standards`; `.cursorrules §API-First Discipline` |

**Descrição:** Todos os endpoints da API REST devem ser documentados no padrão OpenAPI 3.x, gerados automaticamente pelo SpringDoc 2.x a partir das anotações `@Operation`, `@ApiResponse` e `@Tag`. O Swagger UI deve estar acessível em `/swagger-ui` em todos os ambientes não-produção. Os tipos TypeScript do frontend devem ser gerados a partir da spec OpenAPI via `openapi-typescript`.

**Métrica:** 100% dos endpoints com pelo menos 1 `@ApiResponse` documentado; spec OpenAPI acessível em `/v3/api-docs`; zero divergências entre spec e comportamento real (verificado por testes de contrato).

**Verificação:** Step de CI que executa `openapi-diff` comparando spec atual com a última versão aprovada; revisão de PR para novos endpoints sem documentação.

**RF relacionados:** Todos os RF de API (transversal)

---

### RNF-CMP-02 — PostgreSQL 16 com extensões obrigatórias

| Campo | Valor |
|-------|-------|
| **ID** | RNF-CMP-02 |
| **Categoria** | Compatibilidade |
| **Prioridade** | P0 |
| **Fonte** | `agents/database-engineer.md §Flyway Migration Conventions`; ADR-008 |

**Descrição:** O banco de dados deve ser PostgreSQL 16 (mínimo 14) com as extensões: `uuid-ossp` (função `uuid_generate_v7()`), `pgcrypto` (geração segura de dados aleatórios), `citext` (comparação case-insensitive para email), `pg_trgm` (busca por similaridade textual). A migration `V000__extensions.sql` deve ser a primeira a ser aplicada.

**Métrica:** Todas as 4 extensões instaladas verificadas por query `SELECT extname FROM pg_extension`; PKs geradas com `uuid_generate_v7()` no schema.

**Verificação:** Teste de integração com Testcontainers `postgres:16` que verifica extensões após migrations; revisão de migration V000 em todo PR que adiciona nova tabela.

**RF relacionados:** Todos (banco de dados transversal)

---

### RNF-CMP-03 — Compatibilidade com navegadores modernos

| Campo | Valor |
|-------|-------|
| **ID** | RNF-CMP-03 |
| **Categoria** | Compatibilidade |
| **Prioridade** | P1 |
| **Fonte** | `.cursorrules §Technology Stack §Frontend`; UFPR contexto institucional |

**Descrição:** O frontend web deve ser compatível com as últimas 2 versões dos navegadores: Chrome, Firefox, Safari e Edge. O transpile via Vite deve garantir compatibilidade sem polyfills manuais. É aceita degradação graceful (não quebra, mas pode omitir recursos visuais avançados) em IE e versões muito antigas.

**Métrica:** Build Vite com `target: ['es2020']` sem erros; testes Playwright executados no mínimo em Chromium e Firefox; zero erros de console em Firefox e Chrome nas rotas P0.

**Verificação:** Pipeline CI com `npx playwright test --project=chromium --project=firefox`; `browserslist` configurado no `package.json` para `last 2 versions`.

**RF relacionados:** Todos os RF de UI web

---

## 9. Conformidade Legal (RNF-LGL)

---

### RNF-LGL-01 — LGPD: tratamento mínimo de dados pessoais

| Campo | Valor |
|-------|-------|
| **ID** | RNF-LGL-01 |
| **Categoria** | Conformidade Legal |
| **Prioridade** | P0 |
| **Fonte** | Lei 13.709/2018 (LGPD); US-F1-003 (perfil + consentimento); `analise_arquitetural §17` |

**Descrição:** O sistema deve: (a) coletar apenas os dados pessoais necessários para a finalidade específica de cada funcionalidade (princípio da minimização); (b) registrar o momento do aceite de termos/LGPD do usuário no campo `usuario.metadata.aceite_lgpd_em` com timestamp, IP e User-Agent; (c) não logar CPF, GRR completo, senha ou dados bancários em logs de aplicação. A capacidade funcional de exportação dos dados pessoais pelo próprio usuário está especificada em **RF-F1-003-d** (funcionalidade com tela, API e fluxo dedicados).

> **Nota de classificação:** Os itens (a), (b) e (c) são restrições de conformidade legal (RNF). A exportação de dados pessoais pelo usuário [(c) anterior] é uma **funcionalidade observável** e foi movida para RF-F1-003-d para manter a distinção ISO/IEC 25010 entre atributos de qualidade do sistema e capacidades funcionais.

**Métrica:** Campo `aceite_lgpd_em` preenchido para 100% dos usuários que aceitaram os termos; zero ocorrências de CPF ou senha em logs de produção (validado por query no Loki); exportação de dados pessoais disponível conforme RF-F1-003-d.

**Verificação:** Revisão de código nos `UseCase` que manipulam dados pessoais; query de auditoria no Loki buscando padrões de CPF (`\d{3}\.\d{3}\.\d{3}-\d{2}`); teste de aceitação de US-F1-003 verificando aceite LGPD (RF-F1-002 CA-3) e exportação (RF-F1-003-d).

**RF relacionados:** RF-F1-002, RF-F1-003-a, RF-F1-003-d, RF-F7-001

---

### RNF-LGL-02 — Certificados com trilha criptográfica de autoria

| Campo | Valor |
|-------|-------|
| **ID** | RNF-LGL-02 |
| **Categoria** | Conformidade Legal |
| **Prioridade** | P1 |
| **Fonte** | `analise_arquitetural §11`; US-F0-007; US-F1-010; ADR-012 |

**Descrição:** Todo certificado de participação emitido pelo sistema deve possuir trilha criptográfica que permita verificar sua autenticidade e autoria de forma objetiva e independente do sistema: (a) o certificado deve ser gerado exclusivamente pelo sistema a partir de eventos internos validados — nunca por upload de arquivo externo; (b) deve possuir hash SHA-256 do documento que permita detectar adulteração posterior; (c) deve ser assinado com algoritmo assimétrico (ED25519) cuja chave pública seja acessível publicamente, possibilitando verificação offline. A **pipeline funcional** de emissão de certificados (geração de PDF, embedding do QR Code, armazenamento em MinIO, publicação da chave JWKS) está especificada em **RF-TR-003**.

> **Nota de classificação:** Os requisitos de autoria criptográfica comprovável (itens a–c) são atributos de conformidade legal/anti-fraude (RNF). O fluxo técnico de como o sistema gera, armazena e publica os certificados é comportamento funcional especificado em RF-TR-003, evitando duplicação.

**Métrica:** 100% dos certificados com `hash_sha256` não-nulo; 100% com assinatura ED25519 válida verificável via `/.well-known/jwks.json`; zero registros de certificados originados por upload externo.

**Verificação:** Teste de integração que gera certificado, recalcula SHA-256 do PDF e compara com o campo persistido; teste de verificação pública conforme RF-F0-007; revisão de código do `CertificateIssuanceUseCase`.

**RF relacionados:** RF-TR-003, RF-F0-007, RF-F1-010

---

## Apêndice A — Limiares de referência rápida

| Atributo | Limiar | Fonte |
|----------|--------|-------|
| Argon2id memory | ≥ 47 MB | OWASP PHC |
| Argon2id iterations | ≥ 1 | OWASP PHC |
| Access token TTL | 15 min | ADR-005 |
| Refresh token TTL | 7 dias | ADR-005 |
| Rate limit login | 5 tentativas / 15 min / IP+id | Bucket4j |
| Bloqueio de conta | 15 min após 10 falhas consecutivas | US-F0-001 CA-05 |
| Deep-link TTL máx. | 24–72 h por tipo | security-engineer |
| Latência P95 listagem | < 300 ms | .cursorrules |
| Latência P95 detalhe | < 100 ms | .cursorrules |
| Latência P95 login | < 800 ms | .cursorrules |
| FCP dashboard | < 1,5 s | .cursorrules |
| LCP dashboard | < 3 s | .cursorrules |
| Outbox lag | < 30 s | .cursorrules |
| Backup retenção | 14 dias | devops-engineer |
| Contraste texto normal | ≥ 4,5:1 | WCAG 2.1 AA |
| Contraste texto grande | ≥ 3:1 | WCAG 2.1 AA |
| Largura mínima responsiva | 375 px | ux-ui-specialist |
| Cobertura domínio | ≥ 85% | .cursorrules |
| Cobertura aplicação | ≥ 70% | .cursorrules |
| Cobertura total backend | ≥ 75% | .cursorrules |
| Complexidade ciclomática | ≤ 10 | .cursorrules |
| Blocos duplicados | < 3% | .cursorrules |
| Alvo touch mínimo | 44 × 44 px | WCAG 2.5.5 |
| Score Lighthouse a11y | ≥ 90 | axe-core |

---

## Apêndice B — Matriz RNF × Fase

> Indica quais fases possuem RFs que dependem diretamente de cada RNF.

| RNF | F0 | F1 | F2 | F3 | F4 | F5 | F6 | F7 | F8 | TR |
|-----|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| RNF-SEC-01 | ✓ | ✓ | | ✓ | | ✓ | | ✓ | | |
| RNF-SEC-02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-SEC-03 | ✓ | | | | | | | | | |
| RNF-SEC-04 | ✓ | | | | | | | | | |
| RNF-SEC-05 | ✓ | | | ✓ | | | | | | ✓ |
| RNF-SEC-06 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-SEC-07 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-SEC-08 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-SEC-09 | ✓ | | | | | | | | | |
| RNF-SEC-10 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-DES-01 | | ✓ | | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-DES-02 | | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-DES-03 | ✓ | | | | | | | | | |
| RNF-DES-04 | | ✓ | ✓ | ✓ | | ✓ | ✓ | ✓ | | |
| RNF-DES-05 | | ✓ | | ✓ | ✓ | ✓ | | ✓ | | ✓ |
| RNF-DES-06 | | ✓ | | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-DIS-01 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-DIS-02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-DIS-03 | | | | | | | | ✓ | | |
| RNF-DIS-04 | | | | | | | | ✓ | | |
| RNF-UX-01 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-UX-02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | |
| RNF-UX-03 | ✓ | ✓ | | ✓ | | ✓ | | ✓ | | |
| RNF-UX-04 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-UX-05 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | |
| RNF-MAN-01 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-MAN-02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-MAN-03 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-MAN-04 | | | | | | | | ✓ | | ✓ |
| RNF-MAN-05 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-CON-01 | | ✓ | | ✓ | ✓ | ✓ | | ✓ | | ✓ |
| RNF-CON-02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-CON-03 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-CON-04 | | ✓ | | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-POR-01 | ✓ | ✓ | ✓ | ✓ | | ✓ | | | | |
| RNF-POR-02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-POR-03 | | ✓ | | ✓ | | ✓ | | | | ✓ |
| RNF-CMP-01 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-CMP-02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RNF-CMP-03 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | |
| RNF-LGL-01 | | ✓ | ✓ | | | ✓ | | ✓ | | |
| RNF-LGL-02 | ✓ | ✓ | | ✓ | | ✓ | | | | ✓ |

---

*Última atualização: 2026-08-09 — Revisão de classificação: RNF-SEC-03, RNF-SEC-04 (separação mecanismo NF × comportamento funcional); RNF-LGL-01 (exportação de dados movida para RF-F1-003-d); RNF-LGL-02 (pipeline funcional delegada a RF-TR-003); RNF-DIS-01 e RNF-DIS-04 (procedimentos operacionais específicos delegados a agents/devops-engineer.md)*
