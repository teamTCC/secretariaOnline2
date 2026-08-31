# Entrega E0 — Público (F0)

**Público:** equipe de frontend (web + recorte mobile)  
**Data-alvo:** 4 de setembro de 2026 (sprint curta)  
**Plano-mãe:** [`plano-entregas-frontend.md`](./plano-entregas-frontend.md) §9  
**Contrato API:** [`foundationDocs/analysis/as-built-backend.md`](../../foundationDocs/analysis/as-built-backend.md) — em conflito com HU, RF ou `telas.md` antigos, o as-built vence.

Este arquivo descreve **somente a E0**: o que cada tela é, para quem, e a quais IDs de transação, diagrama de sequência, caso de uso e requisito ela se refere. **Não** descreve implementação (sem JSX, hooks, snippets).

---

## 1. Objetivo da entrega

Provar que o SPA (e, se der, o Expo) **fala com o IAM e com os endpoints públicos**: cookies HttpOnly, CORS, CSRF Double Submit, RFC 7807, 401 genérico e 429 com espera.

F0 é o único perfil 100% público. A demo do dia 4 não depende de dashboard de aluno.

**Atores:** **A1** Visitante (todas as telas must). **S6** Verificador externo entra no verificador de protocolo (must) e no de certificado (stretch). Após login 200 o visitante deixa de ser A1, mas a E0 **não** entrega as telas autenticadas — só um placeholder.

---

## 2. O que entra e o que fica de fora

| Prioridade | Telas (ID `telas.md`) | Rotas |
|------------|------------------------|--------|
| **Must** | F0.1, F0.2, F0.4, F0.5, F0.6 | `/login`, `/recuperar-senha`, `/contato`, `/erro/:codigo`, `/publico/verificar-protocolo` |
| **Stretch** | F0.3, F0.7 | `/nova-senha?token=`, `/publico/verificar-certificado/:hash` |
| **Fora da E0** | F1+ (dashboard, primeiro acesso real, wizard, presença, secretaria) | `/inicio` polido, `/primeiro-acesso`, DynamicForm, DS completo, Figma pixel-perfect, push, QR |

**Não é tela, mas é fundação obrigatória da E0:** layout público (`PublicLayout`: logo + links Login | Contato | Verificar protocolo), cliente HTTP com `credentials: 'include'`, bootstrap CSRF (`GET /auth/csrf`), parser Problem+JSON, `VITE_API_BASE_URL=http://localhost:8080`, tokens CSS mínimos.

**Mobile (mínimo, não paridade):** só login na mesma API; se o cookie nativo travar, documentar o bloqueio e manter a demo no web.

---

## 3. Mapa de rastreio (todas as telas E0)

Contrato de path: as-built. IDs de HU/RF/UC continuam válidos mesmo quando o texto antigo da HU cita outro path.

| Tela | Rota | HU | RF | UC | Transação | Diagrama de sequência | Must / stretch |
|------|------|----|----|----|-----------|------------------------|----------------|
| F0.1 Login | `/login` | [US-F0-001](../../foundationDocs/HUs/F0%20—%20Público/US-F0-001-LOGIN.md) | [RF-F0-001](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md) | **UC-AUT-01** Autenticar-se | [T-F0-001](../../transaçõesBackend/F0%20—%20Público/T-F0-001-LOGIN.md) | [US-F0-001-LOGIN.md](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-001-LOGIN.md) · **F0.1-a, F0.1-c, F0.1-d, F0.1-e** | Must |
| F0.2 Recuperar senha | `/recuperar-senha` | [US-F0-002](../../foundationDocs/HUs/F0%20—%20Público/US-F0-002-RECUPERAR-SENHA.md) | [RF-F0-002](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md) | **UC-AUT-02** Recuperar senha | [T-F0-002](../../transaçõesBackend/F0%20—%20Público/T-F0-002-RECUPERAR-SENHA.md) | [US-F0-002-RECUPERAR-SENHA.md](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-002-RECUPERAR-SENHA.md) · **F0.2-a, F0.2-b, F0.2-c** | Must |
| F0.3 Nova senha | `/nova-senha?token=` | [US-F0-003](../../foundationDocs/HUs/F0%20—%20Público/US-F0-003-NOVA-SENHA.md) | [RF-F0-003](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md) | **UC-AUT-03** Redefinir senha | [T-F0-003](../../transaçõesBackend/F0%20—%20Público/T-F0-003-NOVA-SENHA.md) | [US-F0-003-NOVA-SENHA.md](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-003-NOVA-SENHA.md) · **F0.3-a, F0.3-b, F0.3-c** | Stretch |
| F0.4 Contato | `/contato` | [US-F0-004](../../foundationDocs/HUs/F0%20—%20Público/US-F0-004-CONTATO.md) | [RF-F0-004](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md) | **UC-PUB-01** Página institucional / erro | [T-F0-004](../../transaçõesBackend/F0%20—%20Público/T-F0-004-CONTATO.md) | [US-F0-004-CONTATO.md](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-004-CONTATO.md) *(texto ainda marca página estática — ignorar; usar T-F0-004)* | Must |
| F0.5 Erro | `/erro/:codigo` | [US-F0-005](../../foundationDocs/HUs/F0%20—%20Público/US-F0-005-ERRO.md) | [RF-F0-005](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md) | **UC-PUB-01** Página institucional / erro | [T-F0-005](../../transaçõesBackend/F0%20—%20Público/T-F0-005-ERRO.md) | [US-F0-005-ERRO.md](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-005-ERRO.md) · **F0.5-a, F0.5-b** | Must |
| F0.6 Protocolo | `/publico/verificar-protocolo` | [US-F0-006](../../foundationDocs/HUs/F0%20—%20Público/US-F0-006-VERIFICAR-PROTOCOLO.md) | [RF-F0-006](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md) | **UC-CRT-02** Verificar protocolo (público) | [T-F0-006-007](../../transaçõesBackend/F0%20—%20Público/T-F0-006-007-VERIFICACOES-PUBLICAS.md) | [US-F0-006-VERIFICAR-PROTOCOLO.md](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-006-VERIFICAR-PROTOCOLO.md) · **F0.6-a, F0.6-c, F0.6-d** | Must |
| F0.7 Certificado | `/publico/verificar-certificado/:hash` | [US-F0-007](../../foundationDocs/HUs/F0%20—%20Público/US-F0-007-VERIFICAR-CERTIFICADO.md) | [RF-F0-007](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md) | **UC-CRT-03** Verificar certificado (público) | [T-F0-006-007](../../transaçõesBackend/F0%20—%20Público/T-F0-006-007-VERIFICACOES-PUBLICAS.md) | [US-F0-007-VERIFICAR-CERTIFICADO.md](../../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-007-VERIFICAR-CERTIFICADO.md) · **F0.7-a, F0.7-c** | Stretch |

**Casos de uso (legendas):** [`legenda_siglas_casos_de_uso_por_ator.md`](../../foundationDocs/useCaseDiagrams/legenda_siglas_casos_de_uso_por_ator.md) · [`legenda_siglas_casos_de_uso_por_nome_e_tipo.md`](../../foundationDocs/useCaseDiagrams/legenda_siglas_casos_de_uso_por_nome_e_tipo.md) · PlantUML [`diagrama_casos_de_uso_secretariaonline2.puml`](../../foundationDocs/useCaseDiagrams/diagrama_casos_de_uso_secretariaonline2.puml)

**Fora da E0, mas vizinhos do login (não implementar tela):**

| ID | Por que aparece no login | Quando |
|----|--------------------------|--------|
| **UC-AUT-04** / RF-F1-002 / US-F1-002 / diagrama **F0.1-b** | JSON `mustChangePassword: true` → ir para placeholder “primeiro acesso na E1”, não para o formulário real | E1 |
| **UC-AUT-07** / `POST /auth/ott` / diagramas **F0.1-g, F0.1-h** | Deep-link de deliberação; CSRF isento | E5 |
| **UC-DASH-01** / RF-TR-006 | Destino `/inicio` de verdade | E1 |

---

## 4. Telas must — o que cada uma é

### 4.1 F0.1 — Login (`/login`)

**O que é.** Porta de entrada. O visitante informa **identificador** (e-mail institucional `@ufpr.br`, e-mail pessoal cadastrado **ou** GRR) e senha. Não há “criar conta” nesta entrega.

**O que a tela faz.**

- Formulário com dois campos; validação no cliente impede chamada vazia (foco no primeiro inválido).
- Sucesso (200): o JSON **não** traz JWT — só `{ mustChangePassword, mustAcceptLgpd }`. Os tokens vêm em cookies HttpOnly (`access_token`, `refresh_token`).
  - `mustChangePassword: true` → placeholder “primeiro acesso (E1)”.
  - senão → placeholder `/inicio` (“E1”). O importante é o cookie existir (CSRF autenticado ou `GET /me` 200/403).
- Falha de credencial ou conta bloqueada: **401** com a **mesma** mensagem genérica (anti-enumeração). Identificador permanece; senha limpa. Nunca “usuário não existe”.
- Rate limit: **429** com espera (`Retry-After` / `retryAfterSeconds`).
- Links: “Esqueci minha senha” → F0.2; “Contato” → F0.4; verificador de protocolo → F0.6.

**HTTP (as-built).** `POST /auth/login` — `permitAll`, CSRF **isento**, rate limit 5/min por IP+identificador. Login em si **não** usa `X-XSRF-TOKEN`.

**Diagramas a cobrir na E0.** F0.1-a (happy path), F0.1-c (401), F0.1-d (429), F0.1-e (bloqueio — mesma UX do 401). F0.1-f (reuso de refresh) é comportamento do interceptor, não da tela. F0.1-g/h ficam para E5.

**Requisitos.** RF-F0-001 · RN-F0.1-01 a RN-F0.1-11 (RN-F0.1-13 OTT fora) · RNF-SEC-01, RNF-SEC-02, RNF-SEC-03, RNF-SEC-04, RNF-SEC-09, RNF-DES-03, RNF-CON-03, RNF-UX-01, RNF-UX-02, RNF-UX-03 · RF-TR-004 (audit no back, a tela não mostra).

**Arquivos-chave.**

| Camada | Arquivo |
|--------|---------|
| Transação / httpie | `transaçõesBackend/F0 — Público/T-F0-001-LOGIN.md` · `httpie/F0-publico/T-F0-001-login.md` |
| Sequência | `foundationDocs/sequenceDiagrams/F0 — Público/US-F0-001-LOGIN.md` |
| Controller / use case | `backend/modules/iam/api/AuthController.kt` · `LoginUseCase.kt` · `AuthDtos.kt` |
| Senha / JWT / sessão | `Argon2PasswordService.kt` · `JwtTokenService.kt` · `RedisTokenRevocationAdapter.kt` · `JwtAuthenticationFilter.kt` |
| Rate limit | `RateLimitFilter.kt` |
| Front (a criar) | `features/auth/LoginPage.tsx` · `shared/api/client.ts` · `PublicLayout` |
| Tela / HU | `foundationDocs/analysis/telas.md` §F0.1 · `foundationDocs/HUs/F0 — Público/US-F0-001-LOGIN.md` |

---

### 4.2 F0.2 — Recuperar senha (`/recuperar-senha`)

**O que é.** Pedido de link por e-mail. O visitante **não** descobre se o endereço existe na base.

**O que a tela faz.**

- Um campo e-mail; formato inválido não chama a API.
- Submit: botão em loading, campo desabilitado.
- Sempre **202** com copy **neutra** (igual à do back: se o e-mail existir, envia link de 24 h). Esconde o formulário; não permite reenvio na mesma visita sem voltar.
- Rede falhou: alerta e nova tentativa, formulário visível.
- 429: rate limit 3/hora por e-mail+IP.
- “Voltar” → `/login`.

O e-mail sai pelo **Outbox** (não no request HTTP). Na E0 não há Mailhog no compose: o token, se precisar do stretch F0.3, sai de `outbox_event` (SQL) ou do SMTP configurado.

**HTTP.** `POST /auth/forgot-password` — CSRF isento.

**Diagramas.** F0.2-a (e-mail cadastrado), F0.2-b (e-mail inexistente — mesma UX), F0.2-c (429).

**Requisitos.** RF-F0-002 · RN-F0.2-01 a RN-F0.2-08 · RF-TR-002 (Outbox) · RNF-SEC-05, RNF-SEC-09, RNF-CON-01, RNF-CON-03, RNF-UX-05.

**Arquivos-chave.**

| Camada | Arquivo |
|--------|---------|
| Transação / httpie | `T-F0-002-RECUPERAR-SENHA.md` · `httpie/F0-publico/T-F0-002-recuperar-senha.md` |
| Sequência | `US-F0-002-RECUPERAR-SENHA.md` |
| Outbox transversal | `transaçõesBackend/transversal/T-10.1-OUTBOX.md` · `foundationDocs/sequenceDiagrams/transversal/10.1-outbox-notificacao.md` |
| Use case / handler | `ForgotPasswordUseCase.kt` · `PasswordResetOutboxHandler.kt` · `OutboxDispatcher.kt` · `MailService.kt` |
| Front (a criar) | `features/auth/RecuperarSenhaPage.tsx` |

---

### 4.3 F0.4 — Contato (`/contato`)

**O que é.** Página institucional **e** o melhor teste de CSRF da sprint: o POST público **obriga** `X-XSRF-TOKEN`.

**O que a tela faz.**

1. **GET** ` /publico/contato` — preenche nome, endereço, telefone (`tel:`), e-mail (`mailto:`), horário. `_links.enviar` aponta para o POST. Sem autenticação; o GET também emite cookie CSRF.
2. **POST** `/publico/contato` — formulário (nome, e-mail, assunto, mensagem) com header CSRF. Sucesso **202**. Rate limit 10/min por IP.
3. Link “Voltar ao login”.

A HU/RF ainda descrevem “página estática / conteúdo via env”. O **as-built** é GET+POST + outbox `contato.recebido`. Seguir T-F0-004.

**Diagramas.** O arquivo US-F0-004 existe, mas a matriz marca os CAs como não aplicáveis (página estática). Comportamento temporal: T-F0-004.

**Requisitos.** RF-F0-004 (exibir dados) · RNF-UX-01, RNF-UX-02 · RF-TR-002 (e-mail assíncrono do POST) · RNF-CON-03 (erros do POST). CSRF: ver §6.

**Arquivos-chave.**

| Camada | Arquivo |
|--------|---------|
| Transação / httpie | `T-F0-004-CONTATO.md` · `httpie/F0-publico/T-F0-004-contato.md` |
| Controller / config | `ContatoPublicoController.kt` · `application.yml` (`app.contato.*`) · `ContatoOutboxHandler.kt` · Flyway `V015` (`contact_message`) |
| CSRF | `GET /auth/csrf` em `AuthController.kt` · `SecurityConfig.kt` · `SpaCsrfTokenRequestHandler.kt` · `CsrfCookieFilter.kt` |
| Front (a criar) | página de contato no `PublicLayout` |

---

### 4.4 F0.5 — Erro (`/erro/:codigo`)

**O que é.** Única tela de erro da aplicação na E0; as sprints seguintes reusam. Códigos: **401, 403, 404, 500**. Linguagem natural; **sem** stack, SQL ou nome de classe.

**O que a tela faz.**

| Código | Mensagem (RF-F0-005) | Ações |
|--------|----------------------|--------|
| 401 | Precisa fazer login | Fazer login → `/login`; ir ao início público |
| 403 | Sem permissão | Início (placeholder na E0) / suporte (pode ser stub) |
| 404 | Página não encontrada | Voltar / login |
| 500 | Erro inesperado; equipe notificada | Mostrar **`incidentId`** `INC-yyyy-xxxx` quando o Problem+JSON trouxer |

O interceptor / error boundary **navega** para esta rota; 4xx de login **não** devem vazar enumeração (permanecem no formulário F0.1).

**Diagramas.** F0.5-a (5xx + incidentId), F0.5-b (4xx da API).

**Requisitos.** RF-F0-005 · RN-F0.5-01 a RN-F0.5-09 · **RNF-CON-03** · RNF-UX-01, RNF-UX-05.

**Arquivos-chave.**

| Camada | Arquivo |
|--------|---------|
| Transação / httpie | `T-F0-005-ERRO.md` · `httpie/F0-publico/T-F0-005-erros.md` |
| Sequência | `US-F0-005-ERRO.md` |
| Handlers | `shared/api/GlobalExceptionHandler.kt` · `iam/api/IamExceptionHandler.kt` |
| Front (a criar) | rota `/erro/:codigo` alimentada pelo parser 7807 do `client.ts` |

---

### 4.5 F0.6 — Verificar protocolo (`/publico/verificar-protocolo`)

**O que é.** Consulta pública institucional: um terceiro confere se um protocolo existe e em que estado está, **sem** dados do aluno.

**O que a tela faz.**

- Campos **ano** e **número** (não um `:id` opaco). Exemplo de API: `GET /publico/solicitacoes/2025/42`.
- 200: protocolo, tipo, estado, datas; `_links.self`. Sem nome do solicitante.
- 404: empty state “não encontrado”.
- 429: rate limit 10/min por IP.
- Loading enquanto busca.

**Divergência a ignorar na E0.** HU/RF/diagrama ainda falam `GET /publico/protocolos/{id}/verificacao`, hash SHA-256 e dropzone de PDF (**F0.6-b**). O as-built **não** é esse contrato. Na E0: consulta ano/número (F0.6-a / F0.6-c / F0.6-d). Upload de PDF fica para evolução, não para o dia 4.

**Requisitos.** RF-F0-006 (intenção: verificação pública) · RNF-LGL-01 (mínimo de PII) · RNF-CON-03 · RNF-UX-05. RF-TR-001 só no sentido de que o protocolo nasceu do motor — a tela E0 só lê o endpoint público.

**Arquivos-chave.**

| Camada | Arquivo |
|--------|---------|
| Transação / httpie | `T-F0-006-007-VERIFICACOES-PUBLICAS.md` · `httpie/F0-publico/T-F0-006-007-verificacoes-publicas.md` |
| Sequência | `US-F0-006-VERIFICAR-PROTOCOLO.md` (ler com o filtro as-built acima) |
| API | `PublicoSolicitacaoController.kt` · `PublicoSolicitacaoQuery.kt` |
| Rate limit | `RateLimitFilter.kt` |
| Front (a criar) | formulário ano/número no `PublicLayout` |

---

## 5. Telas stretch (só com must verde)

### 5.1 F0.3 — Nova senha (`/nova-senha?token=`)

**O que é.** O visitante chegou pelo link do e-mail (query `token`). Define senha nova (mín. 12, maiúscula, minúscula, dígito, especial) + confirmação. Token inválido/expirado/já usado: empty state genérico + “solicitar novo link” → F0.2 (não distinguir “já usado”). Sucesso: `/login` com banner. 422 reuso das 3 últimas senhas.

**HTTP.** `POST /auth/reset-password` `{ token, novaSenha }` — CSRF isento.

**Diagramas.** F0.3-a, F0.3-b, F0.3-c.

**Requisitos.** RF-F0-003 · RN-F0.3-01 a RN-F0.3-11 · RNF-SEC-01, RNF-SEC-05, RNF-CON-03.

**Arquivos-chave.** `T-F0-003-NOVA-SENHA.md` · `ResetPasswordUseCase.kt` · `PasswordHistoryEntity` · `US-F0-003-NOVA-SENHA.md` · front: `NovaSenhaPage` (a criar). Token: `outbox_event` SQL se não houver SMTP.

### 5.2 F0.7 — Verificar certificado (`/publico/verificar-certificado/:hash`)

**O que é.** Terceiro cola/abre o hash do QR do PDF. A API já valida Ed25519 no servidor (`valido`, `verificacaoAssinatura`). Stretch E0: GET + selo válido/inválido + link JWKS. Não precisa reimplementar Web Crypto no browser no dia 4.

**HTTP.** `GET /publico/verificar-certificado/{hash}` · `GET /.well-known/jwks.json`.

**Diagramas.** F0.7-a (válido), F0.7-c (404). F0.7-d (assinatura no cliente) é opcional.

**Requisitos.** RF-F0-007 · RF-TR-003 · RNF-LGL-02 · RNF-CON-03.

**Arquivos-chave.** `T-F0-006-007` (parte 007) · `presenca/api/PublicoController.kt` · `PublicoCertificateQuery.kt` · `app/config/JwksController.kt` · `US-F0-007-VERIFICAR-CERTIFICADO.md` · transversal certificado se existir `T-10.4`.

---

## 6. Sem tela própria — obrigatório na E0

Estas transações sustentam todas as telas must. Não viram rotas de UI.

| Preocupação | IDs | HTTP / comportamento | Arquivos-chave |
|-------------|-----|----------------------|----------------|
| CSRF Double Submit | RN-F0.1-12 · RNF-SEC (transporte) | `GET /auth/csrf` → cookie `XSRF-TOKEN` + JSON; header `X-XSRF-TOKEN` em mutações. **Isentos:** login, refresh, ott, forgot, reset. **Obrigatório:** `POST /publico/contato` | `AuthController.kt` (`/csrf`) · `SecurityConfig.kt` · `SpaCsrfTokenRequestHandler.kt` · `CsrfCookieFilter.kt` |
| Refresh / 401 | RNF-SEC-03 · F0.1-f | `POST /auth/refresh` (cookie `refresh_token`, Path=`/auth`); interceptor 401→refresh | `RefreshTokenUseCase.kt` · `AuthController.kt` |
| Problem+JSON | **RNF-CON-03** · T-F0-005 | `type`, `title`, `status`, `detail`, `instance`; 5xx + `incidentId` | `GlobalExceptionHandler.kt` |
| CORS + cookie local | RNF-SEC-10 · RNF-POR-02 | Origem `http://localhost:5173`, credentials; `COOKIE_SECURE=false` em HTTP | `SecurityConfig` / YAML · `ops/docker-compose.yml` |
| Audit | RF-TR-004 | Login sucesso/falha no back; a UI não lista audit | `AuditPublisher` (IAM) |

**Cliente HTTP (front, a criar):** `frontend-web/src/shared/api/client.ts` — nascer **antes** das páginas (plano §9).

**Infra para demo:** `ops/docker-compose.yml` (API + Postgres + Redis + MinIO). Swagger `/v3/api-docs`. OpenAPI: RNF-CMP-01.

---

## 7. Mobile nesta sprint

Não é paridade de F0. Só **UC-AUT-01** / F0.1 na mesma API.

- Expo: tela de login no grupo público.
- React Native não persiste HttpOnly como o Chrome: Bearer + SecureStore **ou** cookie manager; CSRF também no mobile (`GET /auth/csrf`).
- Se o cookie jar travar: demo do dia 4 = **web**. Não gastar a sprint nisso.

Plataforma: RNF-POR-01.

---

## 8. Demo de 5 minutos (dia 4)

1. Abrir `/login` → credencial errada → **401** genérico (F0.1-c).
2. Credencial seed certa → cookie gravado (F0.1-a); placeholder E1.
3. `/contato` → GET preenche dados → POST com CSRF → **202**.
4. `/publico/verificar-protocolo` → protocolo seed **ou** 404 tratado (F0.6-a / F0.6-c).

Playwright stretch: 401 + 200 + GET contato 200.

---

## 9. Índice de IDs (consulta rápida)

**Casos de uso E0:** UC-AUT-01, UC-AUT-02, UC-AUT-03 (stretch), UC-PUB-01, UC-CRT-02, UC-CRT-03 (stretch). Ator **A1**; **S6** nos verificadores.

**RFs de fase:** RF-F0-001 … RF-F0-007 ([`RF-F0-publico.md`](../../foundationDocs/requisitos/por-fase/RF-F0-publico.md)).

**RFs transversais tocados:** RF-TR-002 (Outbox no forgot/contato), RF-TR-003 (certificado stretch), RF-TR-004 (audit login).

**RNFs que a E0 tem de sentir na UI:** RNF-SEC-01, RNF-SEC-02, RNF-SEC-03, RNF-SEC-04, RNF-SEC-09, RNF-SEC-10, RNF-DES-03, RNF-CON-01, RNF-CON-03, RNF-UX-01, RNF-UX-02, RNF-UX-03, RNF-UX-05, RNF-POR-01, RNF-POR-02, RNF-CMP-01 ([`02-requisitos-nao-funcionais.md`](../../foundationDocs/requisitos/02-requisitos-nao-funcionais.md)).

**Inventário F0 de HUs:** [`F0-INDICE.md`](../../foundationDocs/HUs/F0%20—%20Público/F0-INDICE.md).

---

*E0 · 2026-08-29 · alinhado a as-built (cookies, CSRF, `GET /publico/solicitacoes/{ano}/{numero}`, contato GET+POST).*
