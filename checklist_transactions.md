# Checklist geral de transações — SecretariaOnline2

Marque cada caixa **depois da verificação manual** (HTTPie e/ou leitura de código). As caixas dos tutoriais em `transaçõesBackend/` estão como “implementado no código”; **este arquivo é o seu progresso de QA**.

**Como usar**

1. Setup: [httpie/00-setup-httpie-desktop.md](httpie/00-setup-httpie-desktop.md) → [httpie/02-bootstrap-usuarios-demo.md](httpie/02-bootstrap-usuarios-demo.md).
2. Para cada transação: abra o **diagrama**, o **T-** (implementação) e o **HTTPie** (passos + JSON).
3. Marque **HTTPie** quando a API local responder como o tutorial. Marque **Código** quando conferir o arquivo Kotlin/SQL indicado.
4. Índice mestre: [transaçõesBackend/README.md](transaçõesBackend/README.md) · [httpie/README.md](httpie/README.md) · [foundationDocs/sequenceDiagrams/README.md](foundationDocs/sequenceDiagrams/README.md).

**Legenda**

| Prefixo | Significado |
|---------|-------------|
| HTTPie | Teste manual no Desktop (URL, body, status, JSON, cookies) |
| Código | Leitura do controller / use case / filtro / migration |
| Diagrama | Caso de sequência (IDs `F0.1-a`, `F1.7-D01`, …) |

---

## Progresso por módulo

- [ ] Setup HTTPie + bootstrap de usuários
- [ ] F0 — Público
- [ ] F1 — Aluno
- [ ] F2 — Egresso
- [ ] F3 — Professor
- [ ] F4 — Comissões
- [ ] F5 — Secretaria
- [ ] F6 — Coordenação
- [ ] F7 — Admin
- [ ] F8 — Cross-cutting
- [ ] Transversais (10.1, 10.4, 10.5, 10.6, 10.7)

---

## Setup (antes das transações)

**Links:** [httpie/00-setup](httpie/00-setup-httpie-desktop.md) · [httpie/01-ids](httpie/01-ids-credenciais-e-ambiente.md) · [httpie/02-bootstrap](httpie/02-bootstrap-usuarios-demo.md) · [httpie/ambiente/local.json](httpie/ambiente/local.json) · [V011 seed](backend/app/src/main/resources/db/migration/V011__seed_demo_data.sql) · [V010 roles](backend/app/src/main/resources/db/migration/V010__seed_authorities_roles.sql)

- [ ] **HTTPie** — `GET {{baseUrl}}/actuator/health` → `{ "status": "UP" }`.
- [ ] **HTTPie** — Environment `local.json` colado; cookie jar ligado.
- [ ] **HTTPie** — `GET /auth/csrf` → cookie `XSRF-TOKEN` copiado para `{{xsrfToken}}`.
- [ ] **HTTPie** — Login admin; `{{accessToken}}` preenchido.
- [ ] **HTTPie** — Bootstrap: aluno, professor, secretaria, coordenador criados; `{{cursoId}}` TADS no env.
- [ ] **Código** — Confirmar hash Argon2id do admin (se 401, senha seed placeholder). Ver `UsuariosController` + outbox `iam.usuario_criado` (`payload.senhaTemporaria`).

---

# F0 — Público

## T-F0-001 — Autenticação (Login / refresh / logout / CSRF)

**IDs diagrama:** F0.1-a … F0.1-f  
**Links:** [T-F0-001](transaçõesBackend/F0%20—%20Público/T-F0-001-LOGIN.md) · [US-F0-001](foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-001-LOGIN.md) · [HTTPie](httpie/F0-publico/T-F0-001-login.md)
**Código:** [AuthController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AuthController.kt) · [LoginUseCase.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/LoginUseCase.kt) · [RefreshTokenUseCase.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/RefreshTokenUseCase.kt) · [RateLimitFilter.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/security/RateLimitFilter.kt) · [JwtTokenService.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/services/JwtTokenService.kt) · [SecurityConfig.kt](backend/app/src/main/kotlin/br/ufpr/sept/so2/config/SecurityConfig.kt)

- [ ] **Transação completa** (todas as subcaixas abaixo)

### F0.1-a — Login happy path

- [ ] **HTTPie** — `POST /auth/login` com e-mail admin → **200**, `accessToken` RS256, `tokenType: Bearer`, `mustChangePassword`/`mustAcceptLgpd` booleanos; cookie `refresh_token` HttpOnly (`Path=/auth`).
- [ ] **HTTPie** — Login aluno por e-mail e por GRR (bodies no tutorial T-F0-001); identificador aceita lowercase após trim.
- [ ] **Código** — `LoginUseCase` normaliza identificador, verifica Argon2id, emite JWT (`sub`, `authorities`, `nome`, TTL 15 min), grava `refresh_token` + audit `LOGIN_SUCCESS`.

### F0.1-b — `mustChangePassword`

- [ ] **HTTPie** — Login de usuário recém-criado (senha temporária) → `mustChangePassword: true` (e possivelmente `mustAcceptLgpd: true`); o token **ainda** autentica `POST /auth/first-access`.
- [ ] **Código** — Flag vem de `usuario.senha_alterada == false`; aceite LGPD de `metadata.aceite_lgpd_em`.

### F0.1-c — 401 anti-enumeração

- [ ] **HTTPie** — Login inválido (body no tutorial T-F0-001) → **401** `application/problem+json`, detalhe genérico (não “usuário não encontrado”).
- [ ] **Código** — Usuário inexistente, inativo e senha errada lançam a **mesma** `InvalidCredentialsException`.

### F0.1-d — 429 rate limit

- [ ] **HTTPie** — 6 logins falhos no mesmo identificador em <1 min → **429**, `retryAfterSeconds` + header `Retry-After`. Bucket 5/min por IP+identificador.
- [ ] **Código** — `RateLimitFilter` intercepta **antes** do controller; body 429 no formato problem+json.

### F0.1-e — Conta bloqueada

- [ ] **HTTPie** — Após 10 falhas consecutivas, login continua **401 genérico** (não revela bloqueio).
- [ ] **Código** — `handleFailedAttempt`: 10 falhas → `bloqueadoAte` +15 min; audit `ACCOUNT_BLOCKED`.

### F0.1-f — Refresh + reuso

- [ ] **HTTPie** — `POST /auth/refresh` (cookie ou body no tutorial) → **200** novo `accessToken`; cookie rotacionado.
- [ ] **HTTPie** — Reenviar o refresh **antigo** → **401** e sessões revogadas; precisa logar de novo.
- [ ] **Código** — `RefreshTokenUseCase`: used/revoked → `revokeAllForUser` + audit `SUSPICIOUS_TOKEN_REUSE`.

### CSRF + logout (T-F0-001 extra)

- [ ] **HTTPie** — `GET /auth/csrf` → `{ token, headerName: X-XSRF-TOKEN }`; login/refresh/forgot/reset **isentos** do header.
- [ ] **HTTPie** — `POST /auth/logout` com Bearer + `X-XSRF-TOKEN` → refresh inválido na sequência.
- [ ] **Código** — Double Submit em `SecurityConfig` (`CookieCsrfTokenRepository.withHttpOnlyFalse`).

---

## T-F0-002 — Recuperar senha

**IDs:** F0.2-a, F0.2-b, F0.2-c  
**Links:** [T-F0-002](transaçõesBackend/F0%20—%20Público/T-F0-002-RECUPERAR-SENHA.md) · [US-F0-002](foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-002-RECUPERAR-SENHA.md) · [HTTPie](httpie/F0-publico/T-F0-002-recuperar-senha.md)  
**Código:** [ForgotPasswordUseCase.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/ForgotPasswordUseCase.kt) · [PasswordResetOutboxHandler.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/outbox/PasswordResetOutboxHandler.kt)

- [ ] **Transação completa**

### F0.2-a — E-mail cadastrado

- [ ] **HTTPie** — `POST /auth/forgot-password` → **202** `{ mensagem: "Se este email existir…" }`; em ≤5 s Mailhog ou SQL `outbox_event` tipo `iam.password_reset_requested` com `payload.token`. Copiar → `{{resetToken}}`.
- [ ] **Código** — Mesma TX: JWT 1-uso (`audience=password-reset`, 24 h, JTI) + outbox; **não** chama SMTP no use case.

### F0.2-b — E-mail inexistente

- [ ] **HTTPie** — E-mail inexistente (body no tutorial T-F0-002) → **202 idêntico**; sem nova linha de outbox.
- [ ] **Código** — Ramo `else` só `log.debug`; nunca 404.

### F0.2-c — Rate limit 3/h

- [ ] **HTTPie** — 4ª chamada na hora → **429** + `retryAfterSeconds`.
- [ ] **Código** — Bucket separado no `RateLimitFilter` (e-mail+IP); body cacheado para o controller.

---

## T-F0-003 — Nova senha (token 1 uso)

**IDs:** F0.3-a, F0.3-b, F0.3-c  
**Links:** [T-F0-003](transaçõesBackend/F0%20—%20Público/T-F0-003-NOVA-SENHA.md) · [US-F0-003](foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-003-NOVA-SENHA.md) · [HTTPie](httpie/F0-publico/T-F0-003-nova-senha.md)  
**Código:** [ResetPasswordUseCase.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/ResetPasswordUseCase.kt)

- [ ] **Transação completa**

### F0.3-a — Happy path

- [ ] **HTTPie** — `POST /auth/reset-password` com token + senha ≥12 (maiúscula, minúscula, dígito, especial) → **200**; login com a senha nova.
- [ ] **Código** — Verify RS256 + audience; Argon2id **antes** da TX; na TX: senha, histórico, JTI blacklist, `revokeAll` refresh; audit `PASSWORD_CHANGED`.

### F0.3-b — Token inválido/usado

- [ ] **HTTPie** — Segundo uso do mesmo token → **401** genérico (não distingue expirado/usado).
- [ ] **Código** — `jtiBlacklistRepository.exists` antes de alterar senha; insert JTI só no sucesso.

### F0.3-c — Reuso / senha fraca

- [ ] **HTTPie** — → **422** `weak-password`; token **ainda válido**.
- [ ] **HTTPie** — Senha igual a uma das 3 últimas → **422** `password-reuse`; JTI **não** blacklistado.
- [ ] **Código** — `validatePasswordStrength` + `passwordHistoryRepository.findRecentHashes(3)`.

---

## T-F0-004 — Contato público

**IDs:** US-F0-004 (diagrama original estático; backend agora tem API)  
**Links:** [T-F0-004](transaçõesBackend/F0%20—%20Público/T-F0-004-CONTATO.md) · [US-F0-004](foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-004-CONTATO.md) · [HTTPie](httpie/F0-publico/T-F0-004-contato.md)  
**Código:** [ContatoPublicoController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ContatoPublicoController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /publico/contato` → dados `app.contato.*` + `_links.enviar`; emite CSRF.
- [ ] **HTTPie** — `POST /publico/contato` com `X-XSRF-TOKEN` → **202** `{ id, status: ACEITO }`; sem CSRF → **403**.
- [ ] **Código** — Persistência `contact_message` + outbox `contato.recebido`; rate limit 10/min no POST.

---

## T-F0-005 — Erros RFC 7807

**IDs:** F0.5-a (5xx + incidentId), F0.5-b (4xx)  
**Links:** [T-F0-005](transaçõesBackend/F0%20—%20Público/T-F0-005-ERRO.md) · [US-F0-005](foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-005-ERRO.md) · [HTTPie](httpie/F0-publico/T-F0-005-erros.md)  
**Código:** [GlobalExceptionHandler.kt](backend/shared/src/main/kotlin/br/ufpr/sept/so2/shared/api/GlobalExceptionHandler.kt) · [IamExceptionHandler.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/IamExceptionHandler.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /me` sem Bearer → **401** problem+json; `status` no body = HTTP.
- [ ] **HTTPie** — `POST /auth/login` `{}` → **400** `validation-error` com `errors[]` por campo.
- [ ] **HTTPie** — Token válido sem authority → **403** `forbidden`.
- [ ] **Código** — 4xx sem stack; 5xx com `incidentId` (`INC-yyyy-xxxx`); `AccountBlockedException` mapeada como 401 genérico.

---

## T-F0-006 / T-F0-007 — Verificações públicas

**IDs:** F0.6-a…d · F0.7-a…d  
**Links:** [T-F0-006-007](transaçõesBackend/F0%20—%20Público/T-F0-006-007-VERIFICACOES-PUBLICAS.md) · [US-F0-006](foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-006-VERIFICAR-PROTOCOLO.md) · [US-F0-007](foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-007-VERIFICAR-CERTIFICADO.md) · [HTTPie](httpie/F0-publico/T-F0-006-007-verificacoes-publicas.md)  
**Código:** [PublicoSolicitacaoController.kt](backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/PublicoSolicitacaoController.kt) · [PublicoController.kt](backend/modules/presenca/src/main/kotlin/br/ufpr/sept/so2/modules/presenca/api/PublicoController.kt) · [JwksController.kt](backend/app/src/main/kotlin/br/ufpr/sept/so2/config/JwksController.kt)

- [ ] **Transação completa**

### F0.6 — Protocolo

- [ ] **HTTPie** — Autenticado: `GET /requests/{{requestId}}/protocol` → copie ano/número; anônimo: `GET /publico/solicitacoes/{ano}/{numero}` → **200** sem nome do aluno nem `dados`.
- [ ] **HTTPie** — Ano/número inexistente → **404** (F0.6-c).
- [ ] **Código** — Só campos públicos; `permitAll` + `@SecurityRequirements` vazio; rate 10/min.

### F0.7 — Certificado + JWKS

- [ ] **HTTPie** — `GET /.well-known/jwks.json` → chave RSA (+ OKP Ed25519 se cert ativo).
- [ ] **HTTPie** — `GET /publico/verificar-certificado/{{certificateHash}}` → `valido: true` e `verificacaoAssinatura: ED25519_VALID` (depois de emitir em F1.9/F1.6).
- [ ] **HTTPie** — Hash inventado → **404** (F0.7-c).
- [ ] **Código** — Recomputo SHA-256 do PDF no MinIO + verify Ed25519; prefixo `UNSIGNED_` → `valido: false`. Emissão: [T-10.4](#t-104--emissão-de-certificado-anti-fraude).

---

# F1 — Aluno

## T-F1-001 — Dashboard BFF do aluno

**IDs:** F1.1-D01 … D04  
**Links:** [T-F1-001](transaçõesBackend/F1%20—%20Aluno/T-F1-001-DASHBOARD.md) · [US-F1-001](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-001-DASHBOARD.md) · [HTTPie](httpie/F1-aluno/T-F1-001-dashboard.md) · Redis [T-10.7](transaçõesBackend/transversal/T-10.7-REDIS-BFF.md)  
**Código:** [DashboardAlunoController.kt](backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/DashboardAlunoController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Token aluno: `GET /bff/dashboard/aluno` → **200** com `kpis`, `pendencias` (máx. 3, `EM_AJUSTE`), `eventos` (máx. 3, `EM_ANDAMENTO`), `ultimasSolicitacoes`, `_links` (`novaSolicitacao` só se `request.open`).
- [ ] **HTTPie** — Token sem `dashboard.view_own` → **403**.
- [ ] **HTTPie** — Segundo GET <60 s (Redis ligado) mais rápido (F1.1-D02). Resposta com `_degraded: true` **não** deve cachear (F1.1-D03).
- [ ] **Código** — `alunoId` só do JWT; `try/catch` por bloco; cache key `aluno:{uuid}`; `@PreAuthorize("hasAuthority('dashboard.view_own')")`.

---

## T-F1-002 — Primeiro acesso (senha + LGPD)

**IDs:** F1.2-D01, D02, D03  
**Links:** [T-F1-002](transaçõesBackend/F1%20—%20Aluno/T-F1-002-PRIMEIRO-ACESSO.md) · [US-F1-002](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-002-PRIMEIRO-ACESSO.md) · [HTTPie](httpie/F1-aluno/T-F1-002-primeiro-acesso.md)  
**Código:** [FirstAccessUseCase.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/FirstAccessUseCase.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `POST /auth/first-access` com Bearer do login provisório + `aceiteLgpd: true` → **200**; relogin com `mustChangePassword: false`.
- [ ] **HTTPie** — `aceiteLgpd: false` → **400**; senha curta → **422**.
- [ ] **Código** — `currentUserId()` do JWT; `metadata.aceite_lgpd_em`; hash provisório no histórico; audit `FIRST_ACCESS_COMPLETED`; `require(mustChangePassword)`.

---

## T-F1-003 — Perfil `/me`

**IDs:** F1.3-D01, D02 · F1.4-D03…D05 · F1.5-D06 · F1.3-D07a/b/c  
**Links:** [T-F1-003](transaçõesBackend/F1%20—%20Aluno/T-F1-003-PERFIL.md) · [US-F1-003](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-003-PERFIL.md) · [HTTPie](httpie/F1-aluno/T-F1-003-perfil.md)
**Código:** [ProfileController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ProfileController.kt) · [FcmTokenController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/FcmTokenController.kt) · [DataExportUseCase.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/DataExportUseCase.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /me` → `id`, `email`, `roles`, `_links` HAL; copie `{{alunoId}}`.
- [ ] **HTTPie** — `PATCH /me` com `metadata.idCurso` () → **200** (F1.3-D01).
- [ ] **HTTPie** — `POST /me/avatar` → `uploadUrl` + PUT MinIO (F1.3-D02).
- [ ] **HTTPie** — `POST /me/password` senha atual correta → **200**; senha atual errada → **400** (F1.4-D03/D04).
- [ ] **HTTPie** — `PATCH /me/notifications` → eco dos booleans (F1.5-D06).
- [ ] **HTTPie** — `POST /me/fcm-token` body `fcmToken`+`plataforma`; `DELETE` com.
- [ ] **HTTPie** — `POST /me/data-export` + `GET /me/data-export/{{jobId}}` → `READY` + URL 24 h (D07a/b).
- [ ] **Código** — ID só do JWT (sem path de outro usuário); Argon2 na troca de senha; presign MinIO; LGPD JSON no bucket.

---

## T-F1-004 — Comunicação (inbox)

**IDs:** F1.6-D01, D02, D03  
**Links:** [T-F1-004](transaçõesBackend/F1%20—%20Aluno/T-F1-004-COMUNICACAO.md) · [US-F1-004](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-004-COMUNICACAO.md) · [HTTPie](httpie/F1-aluno/T-F1-004-comunicacao.md)  
**Código:** [CommunicationsController.kt](backend/modules/comunicacao/src/main/kotlin/br/ufpr/sept/so2/modules/comunicacao/api/CommunicationsController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Aluno: `GET /communications/me` paginado; `GET /communications/me/unread-count`.
- [ ] **HTTPie** — `PATCH /communications/deliveries/{{deliveryId}}/read` → `readAt` preenchido; count cai. (Diagrama cita POST `/:id/read`; código usa PATCH em **delivery**.)
- [ ] **HTTPie** — Publicar: ver [T-F3-007](#us-f3-007--publicar-comunicado) com token professor/admin.
- [ ] **Código** — `delivery.idUsuario == currentUserId()` no mark-read; fan-out por `audiencia.cursoId` vs todos os ativos.

---

## T-F1-005 — Solicitações (workflow)

**IDs:** F1.7-D01 · F1.8-D02…D05 · F1.9-D06…D08  
**Links:** [T-F1-005](transaçõesBackend/F1%20—%20Aluno/T-F1-005-SOLICITACOES.md) · [US-F1-005](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-005-SOLICITACOES.md) · [HTTPie](httpie/F1-aluno/T-F1-005-solicitacoes.md)
**Código:** [RequestController.kt](backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/RequestController.kt) · [OpenRequestUseCase.kt](backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/OpenRequestUseCase.kt) · [TransitionRequestUseCase.kt](backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/TransitionRequestUseCase.kt) · [WorkflowEngine.kt](backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/domain/WorkflowEngine.kt)

- [ ] **Transação completa**

### F1.7-D01 — Lista

- [ ] **HTTPie** — `GET /requests?estado=ABERTA` como aluno → só as dele (filtro pelo JWT, ignora query de outro solicitante).
- [ ] **Código** — Se só `request.view_own` (sem `view_curso`/`deliberate`) força `idSolicitante = user.userId`.

### F1.8-D02 — Tipos

- [ ] **HTTPie** — `GET /requests/types` → `formSchema` JSON Schema; copie `id` de `DECLARACAO_MATRICULA` → `{{requestTypeId}}`.
- [ ] **Código** — Só tipos `ativo=true`; admin publica em [T-F7-003](#t-f7-003--editor-requesttype--workflow).

### F1.8-D03 / D04 — Anexo + abrir

- [ ] **HTTPie** — Presign `POST /requests/attachments/presigned-url` → PUT MinIO → `POST /requests` com ou sem anexo → **201** + `id`.
- [ ] **Código** — `numeroAnual` por curso/ano; `prazoEm` = agora + `prazoDias`; anexos na **mesma TX**; outbox `solicitacoes.aberta`.

### F1.8-D05 — Rascunho

- [ ] **HTTPie** — `POST /requests/draft` → **201** sem protocolo/outbox; `POST /requests/{id}/submit` → `ABERTA` + `protocolo`.
- [ ] **Código** — Draft não incrementa número; submit calcula prazo e enfileira outbox.

### F1.9-D06…D08 — Detalhe, protocolo, download

- [ ] **HTTPie** — `GET /requests/{{id}}` → `_links` de transições só se o ator tiver authority **e** o estado permitir. Aluno em `ABERTA` ≈ só `self`.
- [ ] **HTTPie** — `GET /requests/{{id}}/protocol` → `"2026/0001"` + `_links.public`. (Diagrama cita PDF; API atual devolve JSON.)
- [ ] **HTTPie** — `GET /requests/{{id}}/events` timeline; `GET …/attachments` + `download-url`; `DELETE` anexo só dono em `ABERTA`/`RASCUNHO` → **204**.
- [ ] **Código** — `WorkflowEngine.allowedTransitions`; actions do **seed**: `ASSIGN`, `DEFER`, `DENY` (não `DEFERIR`).

### Transição (também F3.3 / F5.2)

- [ ] **HTTPie** — Com token secretaria/professor: `POST /requests/{{id}}/transitions` depois → **200**; action inválida no estado → 4xx.
- [ ] **Código** — `TransitionRequestUseCase` + `request_event` + outbox na mesma TX.

---

## T-F1-006 — Horas formativas

**IDs:** F1.10-D01 · F1.11-D02/D03 · F1.12-D04  
**Links:** [T-F1-006](transaçõesBackend/F1%20—%20Aluno/T-F1-006-FORMATIVAS.md) · [US-F1-006](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-006-FORMATIVAS.md) · [HTTPie](httpie/F1-aluno/T-F1-006-formativas.md)  
**Código:** [FormativasController.kt](backend/modules/formativas/src/main/kotlin/br/ufpr/sept/so2/modules/formativas/api/FormativasController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Presign comprovante → `POST /formativas` → **201** `PENDENTE`; copie `{{formativaId}}`.
- [ ] **HTTPie** — `GET /formativas/minhas`, `GET /formativas/resumo` (`horasAprovadas` / 120).
- [ ] **HTTPie** — Revisor: `GET /formativas/pendentes`; `PATCH /formativas/{{id}}/review` → `APROVADA` + certificado origem FORMATIVA.
- [ ] **Código** — `formative.submit` vs `formative.review`; aprovação grava `formative_entry` + `CertificateIssuerService`.

---

## T-F1-007 — Estágio

**IDs:** F1.13-D01 · F1.14-D02/D03  
**Links:** [T-F1-007-008](transaçõesBackend/F1%20—%20Aluno/T-F1-007-008-ESTAGIO-TCC.md) · [US-F1-007](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-007-ESTAGIO.md) · [HTTPie](httpie/F1-aluno/T-F1-007-008-estagio-tcc.md)  
**Código:** [EstagioController.kt](backend/modules/estagio/src/main/kotlin/br/ufpr/sept/so2/modules/estagio/api/EstagioController.kt) · [EstagioDocumentController.kt](backend/modules/estagio/src/main/kotlin/br/ufpr/sept/so2/modules/estagio/api/EstagioDocumentController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `POST /internships` → **201** `EM_ANDAMENTO`; `GET /internships/mine` e `GET /internships/{{id}}`.
- [ ] **HTTPie** — `POST …/documents/upload-url` + confirm; listar documentos.
- [ ] **Código** — Outbox `estagio.declarado`; FGAC `internship.view_own` / `upload_doc_own`. Conclude: [T-F4-002](#t-f4-002--comissão-coe).

---

## T-F1-008 — TCC

**IDs:** F1.15-D01 · F1.16-D02/D03  
**Links:** [T-F1-007-008](transaçõesBackend/F1%20—%20Aluno/T-F1-007-008-ESTAGIO-TCC.md) · [US-F1-008](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-008-TCC.md) · [HTTPie](httpie/F1-aluno/T-F1-007-008-estagio-tcc.md)  
**Código:** [TccController.kt](backend/modules/tcc/src/main/kotlin/br/ufpr/sept/so2/modules/tcc/api/TccController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Professor: `POST /tccs`, `POST …/members`, `POST …/examiners`, `PATCH …/grade`, `PATCH …/approve`.
- [ ] **HTTPie** — Aluno membro: `GET /tccs/mine`, `POST …/submit-final/url` + confirm.
- [ ] **Código** — `tcc.upload_final` checa membership; outbox `tcc.criado` / `tcc.deliberado`; TCC `APROVADO` é critério de colação (F5.5).

---

## T-F1-009 — Presença em eventos (v4.1)

**IDs:** F1.17-D01/D02 · F1.18-D03…D05  
**Links:** [T-F1-009](transaçõesBackend/F1%20—%20Aluno/T-F1-009-PRESENCA.md) · [US-F1-009](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-009-PRESENCA.md) · [HTTPie](httpie/F1-aluno/T-F1-009-presenca.md)
**Código:** [EventAttendanceController.kt](backend/modules/presenca/src/main/kotlin/br/ufpr/sept/so2/modules/presenca/api/EventAttendanceController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Professor: `POST /events` → `{{eventoId}}`; `POST …/attendance/windows/entry` → copiar `secret`/`qrToken`.
- [ ] **HTTPie** — Aluno (`metadata.idCurso`): `GET /events?audience=me`; `GET …/attendance/session` → `_links.confirmar-entrada` só com janela ativa.
- [ ] **HTTPie** — `POST …/attendance/entry` PIN correto → **200**; PIN/janela inválidos → 4xx/403 (F1.18-D05); mesmo `deviceUuid` outro aluno → conflito.
- [ ] **HTTPie** — DUAL: janela `exit` + `POST …/attendance/exit`; QR: `POST …/attendance/qr/validate`.
- [ ] **HTTPie** — `POST /events/{{id}}/close` → certificados emitidos (ver T-10.4).
- [ ] **Código** — Modos `SECRET_*` / `QR_*`; janelas no JSONB `validationWindows`; HATEOAS por `event.host` + estado `EM_ANDAMENTO`; `CONCLUIDO` imutável.

---

## T-F1-010 — Certificados do aluno

**IDs:** F1.19-D01…D03  
**Links:** [T-F1-010-011](transaçõesBackend/F1%20—%20Aluno/T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md) · [US-F1-010](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-010-CERTIFICADOS.md) · [HTTPie](httpie/F1-aluno/T-F1-010-011-certificados-atendimentos.md)  
**Código:** [CertificateController.kt](backend/modules/presenca/src/main/kotlin/br/ufpr/sept/so2/modules/presenca/api/CertificateController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /certificates/mine` → `hashSha256`, `_links.download` e `verify` público; copie hash.
- [ ] **HTTPie** — `GET /certificates/{{id}}/download-url` → MinIO TTL 15 min; certificado de outro aluno → **403**.
- [ ] **Código** — Filtro `idAluno == currentUserId()`; emissão só via close/review (não há POST create).

---

## T-F1-011 — Atendimentos (aluno)

**IDs:** F1.20-D01/D02  
**Links:** [T-F1-010-011](transaçõesBackend/F1%20—%20Aluno/T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md) · [US-F1-011](foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-011-ATENDIMENTOS.md) · [HTTPie](httpie/F1-aluno/T-F1-010-011-certificados-atendimentos.md)  
**Código:** [ServiceRecordController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ServiceRecordController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `POST /me/service-records` → `AGENDADO`; `GET /me/service-records?status=PENDENTE_CIENCIA`.
- [ ] **HTTPie** — Secretaria cria → `PENDENTE_CIENCIA`; aluno `POST …/acknowledge` → `CIENTE`; outro aluno → **403**.
- [ ] **Código** — `_links.acknowledge` só em `PENDENTE_CIENCIA`; audit `SERVICE_RECORD_ACKNOWLEDGED` com IP; KPI dashboard `atendimentosPendentes`.

---

# F2 — Egresso

## T-F2-001 — Dashboard do egresso

**IDs:** F2.1-D01…D04  
**Links:** [T-F2-001](transaçõesBackend/F2%20—%20Egresso/T-F2-001-DASHBOARD-EGRESSO.md) · [US-F2-001](foundationDocs/sequenceDiagrams/F2%20—%20Egresso/US-F2-001-DASHBOARD-EGRESSO.md) · [HTTPie](httpie/F2-egresso/T-F2-001-dashboard-egresso.md)  
**Código:** [DashboardAlunoController.kt](backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/DashboardAlunoController.kt) (`GET /bff/dashboard/egresso`) · [GraduationController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/GraduationController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Token EGRESSO: `GET /bff/dashboard/egresso` → **200**, sem `_links.novaSolicitacao`.
- [ ] **HTTPie** — Egresso em `GET /bff/dashboard/aluno` → **403**; aluno no dashboard egresso → **403** (F2.1-D04).
- [ ] **HTTPie** — Após colação: `GET /graduations/{{id}}/diploma-url` (staff) ou link do BFF; certificados em `GET /certificates/mine` (mesmo hash, sem reemitir artefato novo).
- [ ] **Código** — Diagrama cita `/alumni/me`; implementação canônica é o BFF. Confirmar no Swagger se `/alumni/me` existe. Cache `egresso:{uuid}`.

---

# F3 — Professor

**Índice T:** [T-F3-PROFESSOR.md](transaçõesBackend/F3%20—%20Professor/T-F3-PROFESSOR.md) · **HTTPie:** [T-F3-professor.md](httpie/F3-professor/T-F3-professor.md) · **Login:**

## US-F3-001 — Dashboard professor

**Links:** [US-F3-001](foundationDocs/sequenceDiagrams/F3%20—%20Professor/US-F3-001-DASHBOARD.md) · DRY [T-F1-001](transaçõesBackend/F1%20—%20Aluno/T-F1-001-DASHBOARD.md)

- [ ] **HTTPie** — `GET /bff/dashboard/professor` → `meusEventos`, `solicitacoesPendentes`, `_links.novoEvento`.
- [ ] **HTTPie** — Sem `dashboard.view_self_professor` → **403**.
- [ ] **Código** — Degradação por bloco (F3.1-D02); cache `professor:{uuid}`.

## US-F3-002 — Eventos (host)

**Links:** [US-F3-002](foundationDocs/sequenceDiagrams/F3%20—%20Professor/US-F3-002-EVENTOS.md) · [T-F1-009](transaçõesBackend/F1%20—%20Aluno/T-F1-009-PRESENCA.md) · [HTTPie presença](httpie/F1-aluno/T-F1-009-presenca.md)

- [ ] **HTTPie** — F3.2-D01 criar evento (`event.manage`); D03/D04 abrir janela QR vs PIN; D05 `POST /close`.
- [ ] **HTTPie** — Evento `CONCLUIDO` não aceita nova janela (D02 imutável).
- [ ] **HTTPie** — Sem `event.host` → **403** (F3.2-ERRO).
- [ ] **Código** — `_links` `abrir-janela-entrada/saida` e `encerrar-evento` só host + `EM_ANDAMENTO`.

## US-F3-003 — Deliberar solicitações

**Links:** [US-F3-003](foundationDocs/sequenceDiagrams/F3%20—%20Professor/US-F3-003-DELIBERAR-SOLICITACOES.md) · [HTTPie solicitações](httpie/F1-aluno/T-F1-005-solicitacoes.md)

- [ ] **HTTPie** — `GET /requests?estado=EM_DELIBERACAO`; detalhe com `_links`; `POST …/transitions` action `DEFER` (D02).
- [ ] **Código** — Guard + authorities no `WorkflowEngine`; outbox por transição; deep-link JWT 1-uso (D03/ERRO-a) se o tipo gerar token — conferir `generateOneTimeToken` no `workflow_json` seed.

## US-F3-004 — Revisar formativas

**Links:** [US-F3-004](foundationDocs/sequenceDiagrams/F3%20—%20Professor/US-F3-004-REVISAR-FORMATIVAS.md) · Role CAAF

- [ ] **HTTPie** — `GET /formativas/pendentes`; `PATCH …/review` APROVAR/REJEITAR; lote em [T-F4-001](#t-f4-001--comissão-caaf).
- [ ] **HTTPie** — Sem `formative.review` → **403**.

## US-F3-005 — Orientação de estágio

**Links:** [US-F3-005](foundationDocs/sequenceDiagrams/F3%20—%20Professor/US-F3-005-ESTAGIO-ORIENTACAO.md) · [HTTPie estágio](httpie/F1-aluno/T-F1-007-008-estagio-tcc.md)

- [ ] **HTTPie** — `GET /internships` com `internship.supervise`/`review`; parecer/conclude conforme controller.
- [ ] **Código** — Outbox ao atribuir supervisor / concluir.

## US-F3-006 — Orientação de TCC

**Links:** [US-F3-006](foundationDocs/sequenceDiagrams/F3%20—%20Professor/US-F3-006-TCC-ORIENTACAO.md)

- [ ] **HTTPie** — Lista `canReview`, `PATCH /tccs/{{id}}/approve`, download presigned; sem `tcc.supervise` → **403**.

## US-F3-007 — Publicar comunicado

**Links:** [US-F3-007](foundationDocs/sequenceDiagrams/F3%20—%20Professor/US-F3-007-PUBLICAR-COMUNICADO.md) · ·

- [ ] **HTTPie** — `POST /communications` com `cursoId` (`publish_class`) → **201** `{ id, entregas }`; sem `cursoId` só `publish` (admin).
- [ ] **HTTPie** — Sem authority → **403**; `publish_class` sem curso → **422**.
- [ ] **Código** — Fan-out `communication_delivery` na mesma operação.

---

# F4 — Comissões

## T-F4-001 — Comissão CAAF

**IDs:** F4.1a…f  
**Links:** [T-F4-001](transaçõesBackend/F4%20—%20Comissões/T-F4-001-COMISSAO-CAAF.md) · [US-F4-001](foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-001-COMISSAO-CAAF.md) · [HTTPie](httpie/F4-comissoes/T-F4-001-caaf.md)  
**Código:** [CommissionsCaafController.kt](backend/modules/formativas/src/main/kotlin/br/ufpr/sept/so2/modules/formativas/api/CommissionsCaafController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Paths **reais**: `GET /commissions/caaf/pool` (não `/dashboard`); `POST /commissions/caaf/{{id}}/claim` (não `/assign`); `POST /batch-review`; `GET /stats`.
- [ ] **HTTPie** — Claim em `PENDENTE` sem revisor → **200** `idRevisor`; já reivindicada → 400.
- [ ] **HTTPie** — Batch `APROVAR` → certificados + outbox `formativas.batch_revisada`; `acao` inválida → 400.
- [ ] **HTTPie** — Sem `formative.review` → **403** (F4.1e).
- [ ] **Código** — `@PreAuthorize` na classe; aprovação cria `FormativeEntry` + `issueFormativeCertificate`.

## T-F4-002 — Comissão COE

**IDs:** F4.2a…e  
**Links:** [T-F4-002](transaçõesBackend/F4%20—%20Comissões/T-F4-002-COMISSAO-COE.md) · [US-F4-002](foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-002-COMISSAO-COE.md) · [HTTPie](httpie/F4-comissoes/T-F4-002-coe.md) ·  
**Código:** [CommissionsCoeController.kt](backend/modules/estagio/src/main/kotlin/br/ufpr/sept/so2/modules/estagio/api/CommissionsCoeController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /commissions/coe/pool`; `POST …/{{internshipId}}/assign-supervisor`; `POST /bulk-assign`; `GET /stats`.
- [ ] **HTTPie** — **Não** existe aprovar estágio em lote. Conclude: `POST /internships/{{id}}/conclude`.
- [ ] **HTTPie** — Sem `internship.review` → **403** (F4.2e).
- [ ] **Código** — Outbox `estagio.supervisor_atribuido` (aluno + professor).

---

# F5 — Secretaria

**Índice T:** [T-F5-SECRETARIA.md](transaçõesBackend/F5%20—%20Secretaria/T-F5-SECRETARIA.md) · **HTTPie índice:** [T-F5-secretaria.md](httpie/F5-secretaria/T-F5-secretaria.md) · **Login:**

## US-F5-001 — Dashboard secretaria

**Links:** [US-F5-001](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-001-DASHBOARD.md)

- [ ] **HTTPie** — `GET /bff/dashboard/secretaria` → KPIs `emTriagem`/`emDeliberacao` (contagem real, não página).
- [ ] **HTTPie** — Sem `dashboard.view_secretary` → **403** (F5.1-D03).
- [ ] **Código** — Cache `secretaria:static`; counts `ABERTA` / `EM_DELIBERACAO`.

## US-F5-002 — Fila de solicitações

**Links:** [US-F5-002](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-002-SOLICITACOES.md) ·

- [ ] **HTTPie** — `GET /requests?estado=ABERTA` com `request.view_curso`; `type=` alias de `typeCode`.
- [ ] **HTTPie** — `PATCH /requests/bulk-deliberate` action `DEFER` → **200** ou **409** rollback (all-or-nothing).
- [ ] **Código** — Cada id reusa `TransitionRequestUseCase`; outbox por item.

## US-F5-003 / F7.1 — Gestão de usuários / alunos

**Links:** [US-F5-003](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-003-GESTAO-ALUNOS.md) · [US-F7-001](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-001-IAM-USUARIOS.md)
**Código:** [UsuariosController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/UsuariosController.kt)

- [ ] **HTTPie** — `GET /usuarios?email=…` (path real **não** é `/students`).
- [ ] **HTTPie** — `POST /usuarios` → **201** `{ id, email }` **sem** senha; senha no outbox `iam.usuario_criado`.
- [ ] **HTTPie** — `PATCH /usuarios/{{id}}/status` `{ ativo: false }`; `POST …/reset-password` → Mailhog token 1-uso.
- [ ] **HTTPie** — E-mail/GRR duplicado → **409** (F5.6-ERRO-01). Sem `user.manage_students` → **403**.
- [ ] **Código** — Argon2id na criação; `senha_alterada=false`; desativar deve invalidar sessões (conferir JTI/refresh).

## US-F5-004 — Dados acadêmicos

**Links:** [US-F5-004](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-004-DADOS-ACADEMICOS.md) · [T-F6-001](transaçõesBackend/F6%20—%20Coordenação/T-F6-001-CONFIGURAR-CURSO.md) · [HTTPie F6](httpie/F6-coordenacao/T-F6-coordenacao.md) ·  
**Código:** [AcademicoController.kt](backend/modules/academico/src/main/kotlin/br/ufpr/sept/so2/modules/academico/api/AcademicoController.kt) · [CoordenacaoController.kt](backend/modules/academico/src/main/kotlin/br/ufpr/sept/so2/modules/academico/api/CoordenacaoController.kt)

- [ ] **HTTPie** — `GET /academico/cursos`, `…/disciplinas`, `…/periodos/ativo`.
- [ ] **HTTPie** — `POST /academico/disciplinas` (`cargaHorariaTotal`+`creditos`); `POST /academico/periodos-letivos`.
- [ ] **Código** — Paths do diagrama (`/secretaria/cursos`) vs código (`/academico/*`); sigla duplicada → 409.

## US-F5-005 — Egressos / diplomas

**IDs:** F5.10-D01 · F5.11-D02…D04  
**Links:** [T-F5-005](transaçõesBackend/F5%20—%20Secretaria/T-F5-005-EGRESSOS-DIPLOMAS.md) · [US-F5-005](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-005-EGRESSOS-DIPLOMAS.md) · [HTTPie](httpie/F5-secretaria/T-F5-005-egressos-diplomas.md)  
**Código:** [GraduationController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/GraduationController.kt) · [GraduationEligibilityService.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/GraduationEligibilityService.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /students?eligibleForGraduation=true` → `eligible` + `bloqueios[].razao` (`TCC`, `HISTORICO`, `HORAS_FORMATIVAS`, `FINANCEIRO`, `SOLICITACOES`).
- [ ] **HTTPie** — `POST /graduations` DTO **`idCurso`** (não `cursoId`) → role EGRESSO + PDF; falha de 1 aluno rejeita o lote.
- [ ] **HTTPie** — `GET /secretaria/egressos`, `?format=csv`; `GET /graduations/{{id}}/diploma-url`; `PATCH …/confirm-delivery`.
- [ ] **HTTPie** — Sem `diploma.register` → **403**.
- [ ] **Código** — Revalida 5 critérios no POST; `diplomas/{id}.pdf` MinIO; outbox `graduations.confirmed`.

## US-F5-006 — Autorizações de imagem

**Links:** [US-F5-006](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-006-AUTORIZACOES-IMAGEM.md)

- [ ] **HTTPie** — `GET /requests?type=AUTORIZACAO_IMAGEM`; bulk-deliberate (mesmo JSON F5).
- [ ] **HTTPie** — Concorrência: segundo lote no mesmo id → **409** (F5.12-ERRO-01).
- [ ] **Código** — `SELECT FOR UPDATE` / all-or-nothing; tipo precisa existir no catálogo (T-F7-003).

## US-F5-007 — Atendimentos (secretaria)

**Links:** [US-F5-007](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-007-ATENDIMENTOS.md) ·

- [ ] **HTTPie** — `POST /service-records` `{ idAluno, assunto, tipo: PRESENCIAL }` → **201** `PENDENTE_CIENCIA` + outbox `atendimentos.created`.
- [ ] **Código** — Audit `SERVICE_RECORD_CREATED`; ciência do aluno em T-F1-011.

## US-F5-008 — Eventos (secretaria)

**Links:** [US-F5-008](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-008-EVENTOS.md) · [HTTPie presença](httpie/F1-aluno/T-F1-009-presenca.md)

- [ ] **HTTPie** — Listar eventos no escopo secretaria; encerrar com presença → formativa + certificado.
- [ ] **HTTPie** — Excluir evento com presença → **422** (F5.8-ERRO) se o endpoint de delete existir no Swagger.

## US-F5-009 — Importação CSV

**IDs:** F5.9-D01…D03 · ERRO-D04 · 403  
**Links:** [T-F5-009](transaçõesBackend/F5%20—%20Secretaria/T-F5-009-IMPORTACOES.md) · [US-F5-009](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-009-IMPORTACOES.md) · [HTTPie](httpie/F5-secretaria/T-F5-009-importacoes.md) · CSV  
**Código:** [ImportController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ImportController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /imports/templates/alunos` e `/professores` → CSV (Excel fora de escopo).
- [ ] **HTTPie** — Multipart `POST /imports/alunos` (sem `Content-Type: application/json`) → job `VALIDATED`/`INVALID`; `GET /imports/{{jobId}}` preview.
- [ ] **HTTPie** — `POST /imports/{{jobId}}/confirm` → `COMPLETED`/`PARTIAL`; usuários + Argon2 + outbox `imports.completed`.
- [ ] **HTTPie** — Sem `import.run` → **403**.

## US-F5-010 — Exportações CSV

**Links:** [T-F5-010](transaçõesBackend/F5%20—%20Secretaria/T-F5-010-EXPORTACOES.md) · [US-F5-010](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-010-EXPORTACOES.md) · [HTTPie](httpie/F5-secretaria/T-F5-010-exportacoes.md)  
**Código:** [ExportController.kt](backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/ExportController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `POST /exports/alunos` (também `egressos`, `solicitacoes`) → **202** `PROCESSANDO`.
- [ ] **HTTPie** — Poll `GET /exports/{{jobId}}` até `PRONTO`; `GET …/download` → URL MinIO; job de outro ator → **403**.
- [ ] **Código** — Worker `@Scheduled` 5 s; TTL 7 dias → `EXPIRADO`; outbox `exports.ready`.

## US-F5-011 — Estatísticas

**Links:** [T-F5-011](transaçõesBackend/F5%20—%20Secretaria/T-F5-011-ESTATISTICAS.md) · [US-F5-011](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-011-ESTATISTICAS.md) · [HTTPie](httpie/F5-secretaria/T-F5-011-estatisticas.md)  
**Código:** [ReportsController.kt](backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/ReportsController.kt)

- [ ] **HTTPie** — `GET /reports/secretary?periodo=2026-2&curso=TADS` → kpis + por tipo + por estado + ranking.
- [ ] **HTTPie** — Sem `report.view_secretary` → **403**.

## US-F5-012 — Kanban / tarefas

**IDs:** F5.19-D01…D04 · ERRO-01…04  
**Links:** [T-F5-012](transaçõesBackend/F5%20—%20Secretaria/T-F5-012-TAREFAS.md) · [US-F5-012](foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-012-TAREFAS.md) · [HTTPie](httpie/F5-secretaria/T-F5-012-tarefas.md)  
**Código:** [SecretaryTaskController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/SecretaryTaskController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /tasks`; `POST` → `PENDENTE`; `PATCH` → `EM_ANDAMENTO`; `DELETE` em `PENDENTE` → **204**.
- [ ] **HTTPie** — `DELETE` se não `PENDENTE` → **400** (diagrama cita 409 para `CONCLUIDA` — conferir status real).
- [ ] **HTTPie** — Sem `task.manage` → **403**; POST sem título → **422**.

---

# F6 — Coordenação

## T-F6-001 — Configurar curso

**IDs:** F6.1-D01/D02 · ERRO 403  
**Links:** [T-F6-001](transaçõesBackend/F6%20—%20Coordenação/T-F6-001-CONFIGURAR-CURSO.md) · [US-F6-001](foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-001-CONFIGURAR-CURSO.md) · [HTTPie](httpie/F6-coordenacao/T-F6-coordenacao.md) ·  
**Código:** [CourseConfigController.kt](backend/modules/academico/src/main/kotlin/br/ufpr/sept/so2/modules/academico/api/CourseConfigController.kt) · [HistoricoEscolarController.kt](backend/modules/academico/src/main/kotlin/br/ufpr/sept/so2/modules/academico/api/HistoricoEscolarController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /courses/tads/config` (UUID **ou** sigla); `PATCH` horas/duração/banca.
- [ ] **HTTPie** — Coordenador de outro curso → **403**; admin bypass.
- [ ] **HTTPie** — `GET/PUT /academico/alunos/{{alunoId}}/historico/{{disciplinaId}}` estados `CURSANDO|CONCLUIDA|REPROVADA`.
- [ ] **Código** — Ownership `curso.id_coordenador`; audit `COURSE_CONFIG_UPDATED` com diff; PATCH **não** recalcula colações antigas. SQL dono: [02-bootstrap Passo F](httpie/02-bootstrap-usuarios-demo.md).

## T-F6-002 — Relatórios do coordenador

**IDs:** F6.2-D01/D02 · ERRO 403  
**Links:** [T-F6-002](transaçõesBackend/F6%20—%20Coordenação/T-F6-002-RELATORIOS.md) · [US-F6-002](foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-002-RELATORIOS.md) · [HTTPie F6](httpie/F6-coordenacao/T-F6-coordenacao.md)

- [ ] **HTTPie** — `GET /reports/coordinator?periodo=2026-2&curso=TADS`; atalho `GET /academico/relatorios/curso`.
- [ ] **HTTPie** — Sem `report.view_coordinator` → **403**.
- [ ] **Código** — Recorte SQL por `id_curso` + janela do período; `evasaoPorPeriodo` = proxy de colações (não SIGA).

---

# F7 — Admin

**Índice T:** [T-F7-ADMIN.md](transaçõesBackend/F7%20—%20Admin/T-F7-ADMIN.md) · **HTTPie:** [T-F7-admin.md](httpie/F7-admin/T-F7-admin.md) · **Login:**

## US-F7-001 — IAM usuários

Já coberto em [US-F5-003](#us-f5-003--f71--gestão-de-usuários--alunos) (`/usuarios`, não `/admin/usuarios` no CRUD).

- [ ] **HTTPie** — Confirmar `user.manage_all` vs `user.manage_students` no Swagger.
- [ ] **Código** — Reset admin: JWT 1-uso + outbox (F7.8-D04); admin **nunca** vê a senha no JSON.

## T-F7-002 — Perfis e authorities

**IDs:** F7.2-D01…D05 · ERRO-01/02  
**Links:** [T-F7-002](transaçõesBackend/F7%20—%20Admin/T-F7-002-IAM-PERFIS.md) · [US-F7-002](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-002-IAM-PERFIS-AUTORIDADES.md) · [HTTPie](httpie/F7-admin/T-F7-002-iam-perfis.md) · ·  
**Código:** [AdminRolesController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/AdminRolesController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /admin/roles` (alias `/admin/perfis`) e `GET /admin/autoridades`.
- [ ] **HTTPie** — `POST /admin/roles` MONITOR; `PATCH …/authorities`; `PUT /admin/usuarios/{{id}}/roles` **substitui** o conjunto.
- [ ] **HTTPie** — Relogar depois do PUT (JWT antigo até 15 min).
- [ ] **HTTPie** — `DELETE` role protegida (`ALUNO`/`ADMIN`/…) → **422**; custom sem usuários → **204**.
- [ ] **HTTPie** — Sem `iam.manage_roles` → **403**.

## T-F7-003 — Editor RequestType / workflow

**IDs:** F7.4-D01…D04 · ERRO-01…03  
**Links:** [T-F7-003](transaçõesBackend/F7%20—%20Admin/T-F7-003-WORKFLOW-ENGINE.md) · [US-F7-003](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-003-WORKFLOW-ENGINE.md) · [HTTPie](httpie/F7-admin/T-F7-003-workflow-engine.md)  
**Código:** [AdminRequestTypeController.kt](backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/AdminRequestTypeController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /request-types` (inclui rascunhos); `POST` `ativo=false`; `PATCH`; `POST …/publish` → aparece em `GET /requests/types` do aluno.
- [ ] **HTTPie** — Publish com schema vazio → **422**; `DELETE` com solicitações existentes → **400**.
- [ ] **Código** — ADR-003: um JSON na tabela, sem classe Kotlin nova por tipo.

## T-F7-004 — Templates de comunicação

**IDs:** F7.5-D01…D04  
**Links:** [T-F7-004](transaçõesBackend/F7%20—%20Admin/T-F7-004-TEMPLATES-COMUNICACAO.md) · [US-F7-004](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-004-TEMPLATES-COMUNICACAO.md) · [HTTPie](httpie/F7-admin/T-F7-004-templates.md)  
**Código:** [CommunicationTemplateController.kt](backend/modules/comunicacao/src/main/kotlin/br/ufpr/sept/so2/modules/comunicacao/api/CommunicationTemplateController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /communication-templates`; `POST`; `POST …/revisions`; `GET …/versions` e `…/versions/{rev}` readonly.
- [ ] **Código** — Revisões imutáveis; `TemplateEngine` no dispatcher (T-10.1); placeholders `{{nome}}`, `{{protocolo}}`, etc.

## US-F7-005 / T-10.6 — Jobs / outbox admin

Ver [T-10.6](#t-106--admin-outbox) abaixo. Diagrama: [US-F7-005](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-005-JOBS-OUTBOX.md).

## US-F7-006 — Audit log

**Links:** [T-F7-ADMIN § F7.6](transaçõesBackend/F7%20—%20Admin/T-F7-ADMIN.md) · [US-F7-006](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-006-AUDIT-LOG.md) · [HTTPie T-F7](httpie/F7-admin/T-F7-admin.md)  
**Código:** [AuditController.kt](backend/modules/auditoria/src/main/kotlin/br/ufpr/sept/so2/modules/auditoria/api/AuditController.kt)

- [ ] **HTTPie** — `GET /admin/audit?acao=LOGIN_SUCCESS&de=2026-08-01&ate=2026-08-20`.
- [ ] **HTTPie** — Sem `system.admin` / `audit.read` → **403**.
- [ ] **Código** — Após login, existe linha `LOGIN_SUCCESS` ou `LOGIN_FAILED`; payloads sem senha/token.

## US-F7-007 — Saúde do sistema

**Links:** [US-F7-007](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-007-SAUDE-SISTEMA.md)

- [ ] **HTTPie** — `GET /actuator/health` [link](http://localhost:8080/actuator/health) → `UP`; com admin, detalhes de componentes se `show-details=when-authorized`.
- [ ] **HTTPie** — Swagger [swagger-ui.html](http://localhost:8080/swagger-ui.html); OpenAPI [v3/api-docs](http://localhost:8080/v3/api-docs).
- [ ] **Código** — `management.endpoints.web.exposure` em [application.yml](backend/app/src/main/resources/application.yml).

## F7.A — FAQ admin

**Links:** [HTTPie FAQ](httpie/F7-admin/T-F7-admin.md) · [T-F8-002](#t-f8-002--faq-e-tickets)

- [ ] **HTTPie** — `POST /faq` → `{{faqId}}`; `PATCH`; `DELETE` soft (`ativo=false`); lista pública `GET /faq` some o item.

---

# F8 — Cross-cutting

## T-F8-001 — Busca global

**IDs:** F8.1-D01…D04  
**Links:** [T-F8-001](transaçõesBackend/F8%20—%20Cross-cutting/T-F8-001-BUSCA-GLOBAL.md) · [US-F8-001](foundationDocs/sequenceDiagrams/F8%20—%20Cross-cutting/US-F8-001-BUSCA-GLOBAL.md) · [HTTPie](httpie/F8-cross/T-F8-001-busca.md)  
**Código:** [SearchController.kt](backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/SearchController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Aluno: `GET /search?q=ana` → **sem** `type=USUARIO`; vê requests próprias / eventos / cursos.
- [ ] **HTTPie** — Secretaria: `?types=USUARIO` encontra alunos; `types=REQUEST,CURSO,EVENTO`.
- [ ] **HTTPie** — `q` sem match → lista vazia (D03), **200** (não 404).
- [ ] **Código** — Resposta **plana** `{ type, id, title, subtitle, href }` (diagrama agrupa arrays); timeout 5 s → `timedOut`; `pg_trgm` V015.

## T-F8-002 — FAQ e tickets

**IDs:** F8.2-D01…D03  
**Links:** [T-F8-002](transaçõesBackend/F8%20—%20Cross-cutting/T-F8-002-SUPORTE-FAQ.md) · [US-F8-002](foundationDocs/sequenceDiagrams/F8%20—%20Cross-cutting/US-F8-002-SUPORTE-FAQ.md) · [HTTPie](httpie/F8-cross/T-F8-002-suporte-faq.md) · · [V013 faq seed](backend/app/src/main/resources/db/migration/V013__faq_seed.sql)  
**Código:** [SupportController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/SupportController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /faq` (não `/support/faq` no código) → itens `ativo=true`.
- [ ] **HTTPie** — `POST /support/tickets` body **`descricao`** (não `mensagem`) → **201**; `GET /support/tickets/mine`.
- [ ] **HTTPie** — Staff: `GET /support/tickets`; `PATCH …/respond`; `PATCH …/close`.
- [ ] **HTTPie** — Rate limit tickets → **429** (F8.2-D03) se disparar demais.
- [ ] **Código** — Diagrama DRY workflow `SUPORTE_TECNICO`; conferir se o código usa tabela `support_ticket` própria.

---

# Transversais

## T-10.1 — Outbox (TX + dispatch)

**IDs:** 10.1a (fase TX) · 10.1b (dispatch)  
**Links:** [T-10.1](transaçõesBackend/transversal/T-10.1-OUTBOX.md) · [diagrama 10.1](foundationDocs/sequenceDiagrams/transversal/10.1-outbox-notificacao.md) · [HTTPie](httpie/transversal/T-10.1-outbox.md) · Mailhog [http://localhost:8025](http://localhost:8025)  
**Código:** [OutboxDispatcher.kt](backend/modules/notificacoes/src/main/kotlin/br/ufpr/sept/so2/modules/notificacoes/OutboxDispatcher.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — Dispare forgot-password ou `POST /requests`; em ≤5 s status `PENDING` → `PROCESSED`.
- [ ] **Código** — Insert outbox na **mesma** `@Transactional` do use case (10.1a); dispatcher `SKIP LOCKED`, backoff, 8 tentativas → `DEAD` (10.1b); `TemplateEngine` no handler.

## T-10.4 — Emissão de certificado anti-fraude

**IDs:** 10.4a  
**Links:** [T-10.4](transaçõesBackend/transversal/T-10.4-CERTIFICADO.md) · [diagrama 10.4](foundationDocs/sequenceDiagrams/transversal/10.4-certificado-emissao.md) · [HTTPie](httpie/transversal/T-10.4-certificado.md)  
**Código:** CertificateIssuer no módulo presenca (ver T-10.4)

- [ ] **Transação completa**
- [ ] **HTTPie** — Não existe `POST /certificates`. Emitir via `POST /events/{id}/close` **ou** review APROVAR **ou** CAAF batch.
- [ ] **HTTPie** — PDF no MinIO; `GET /certificates/mine` com `origem`; verificar público F0.7.
- [ ] **Código** — SHA-256 dos **bytes do PDF**; Ed25519; sem prefixo `UNSIGNED_` em emissão nova; chave efêmera em dev se env vazio (certificados não sobrevivem restart).

## T-10.5 — Push FCM

**Links:** [T-10.5](transaçõesBackend/transversal/T-10.5-PUSH-FCM.md) · (sem tutorial HTTPie dedicado — use [T-F1-003 FCM](httpie/F1-aluno/T-F1-003-perfil.md) + T-10.1)  
**Código:** [FcmTokenController.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/FcmTokenController.kt) · [FcmOutboxHandler.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/outbox/FcmOutboxHandler.kt) · [FirebaseConfig.kt](backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/config/FirebaseConfig.kt)

- [ ] **HTTPie** — `POST /me/fcm-token` `{ "fcmToken", "plataforma" }` → `{ registered: true }`; `DELETE` com o mesmo token.
- [ ] **Código** — Sem `APP_FIREBASE_SERVICE_ACCOUNT_JSON`: handler **não** lança, só loga; tabela `device_fcm_token`; eventos `push.fcm.send` no outbox.

## T-10.6 — Admin outbox

**IDs:** F7.6-D01…D03 · ERRO-01  
**Links:** [T-10.6](transaçõesBackend/transversal/T-10.6-ADMIN-OUTBOX.md) · [US-F7-005](foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-005-JOBS-OUTBOX.md) · [HTTPie](httpie/transversal/T-10.6-admin-outbox.md)  
**Código:** [AdminOutboxController.kt](backend/modules/notificacoes/src/main/kotlin/br/ufpr/sept/so2/modules/notificacoes/api/AdminOutboxController.kt)

- [ ] **Transação completa**
- [ ] **HTTPie** — `GET /admin/outbox`, `?status=DEAD`, `/dead`, `GET /{id}` (payload completo — não commitar JWT de reset).
- [ ] **HTTPie** — `POST /{id}/retry` DEAD→PENDING; `DELETE /{id}`.
- [ ] **HTTPie** — Sem admin → **403**.

## T-10.7 — Redis BFF

**Links:** [T-10.7](transaçõesBackend/transversal/T-10.7-REDIS-BFF.md) · [HTTPie dashboard](httpie/F1-aluno/T-F1-001-dashboard.md)  
**Código:** [CacheConfig.kt](backend/app/src/main/kotlin/br/ufpr/sept/so2/config/CacheConfig.kt)

- [ ] **HTTPie** — Com `CACHE_TYPE=redis`: 2× `GET /bff/dashboard/aluno` — segundo mais rápido; `_degraded` não cacheia.
- [ ] **Código** — Chaves `aluno:{uuid}`, `professor:{uuid}`, `egresso:{uuid}`, `secretaria:static`; TTL 60 s; `@ConditionalOnProperty CACHE_TYPE=redis`.

---

# Atalhos de navegação

| Recurso | Link |
|---------|------|
| Índice transações | [transaçõesBackend/README.md](transaçõesBackend/README.md) |
| Índice HTTPie | [httpie/README.md](httpie/README.md) |
| Environment | [httpie/ambiente/local.json](httpie/ambiente/local.json) |
| Fila de diagramas | [foundationDocs/sequenceDiagrams/README.md](foundationDocs/sequenceDiagrams/README.md) |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| JWKS | http://localhost:8080/.well-known/jwks.json |
| Mailhog | http://localhost:8025 |
| Seed admin | [V011__seed_demo_data.sql](backend/app/src/main/resources/db/migration/V011__seed_demo_data.sql) |
| Authorities/roles | [V010__seed_authorities_roles.sql](backend/app/src/main/resources/db/migration/V010__seed_authorities_roles.sql) |
| Security FGAC | [agents/security-engineer.md](agents/security-engineer.md) (autoridades canônicas) |

**Divergências diagrama × código (marcar se conferiu):**

- [ ] CAAF: `/pool` + `/claim` (não `/dashboard` + `/assign`).
- [ ] COE: `/pool` + `/assign-supervisor` (não `/dashboard`).
- [ ] Usuários: `/usuarios` (não `/students` no CRUD; `/students` só elegibilidade de colação).
- [ ] FAQ: `GET /faq` (não `/support/faq`).
- [ ] Tickets: campo `descricao`.
- [ ] Workflow actions seed: `DEFER` / `DENY` / `ASSIGN` (não `DEFERIR`).
- [ ] Colação: `idCurso` no JSON.
- [ ] Busca: lista plana, não arrays agrupados.
- [ ] Dashboard egresso: `/bff/dashboard/egresso` vs `/alumni/me`.
- [ ] FCM: `fcmToken` + `plataforma` (não `token`/`platform`).
- [ ] Disciplina: `cargaHorariaTotal` + `creditos`.
