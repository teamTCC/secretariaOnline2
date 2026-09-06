# Requisitos Funcionais — Fase F0 (Público)

**Projeto:** SecretariaOnline2 — UFPR SEPT  
**Versão:** 1.0  
**Data:** 2026-06-23  
**Gerado a partir de:** US-F0-001 a US-F0-007; `fluxos_por_perfil.md` §1; `telas.md` §2; `legenda_siglas_casos_de_uso_por_ator.md`; `mvp_v1_walking_skeleton_aluno.md`  
**Total RF neste arquivo:** 7

---

## Resumo da fase

| RF | Nome | HU | UC | Tela | Prioridade |
|----|------|----|----|------|:----------:|
| RF-F0-001 | Autenticar usuário com identificador e senha | US-F0-001 | UC-AUT-01 | F0.1 | P0 |
| RF-F0-002 | Solicitar link de recuperação de senha | US-F0-002 | UC-AUT-02 | F0.2 | P1 |
| RF-F0-003 | Redefinir senha via token de uso único | US-F0-003 | UC-AUT-03 | F0.3 | P1 |
| RF-F0-004 | Exibir informações de contato da secretaria | US-F0-004 | UC-PUB-01 | F0.4 | P2 |
| RF-F0-005 | Exibir página de erro amigável por código HTTP | US-F0-005 | UC-PUB-01 | F0.5 | P1 |
| RF-F0-006 | Verificar autenticidade de protocolo PDF | US-F0-006 | UC-CRT-02 | F0.6 | P2 |
| RF-F0-007 | Verificar autenticidade de certificado digital | US-F0-007 | UC-CRT-03 | F0.7 | P2 |

---

### RF-F0-001 — Autenticar usuário com identificador e senha

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-001 |
| **Nome** | Autenticar usuário com identificador e senha |
| **Prioridade** | P0 |
| **Ator(es)** | A1 Visitante; A2 Aluno; A3 Egresso; A4 Professor; A7 Secretaria; A8 Coordenador; A9 Admin |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-001 |
| **Rastreio UC** | UC-AUT-01 (relacionado: UC-AUT-07) |
| **Tela** | F0.1 `/login` |
| **API** | `POST /auth/login` (relacionado: `POST /auth/ott`) |
| **Legado** | RF-01 (T01 `logarUsuario.jsp`) |

**Descrição:** O sistema deve permitir que usuários cadastrados autentiquem-se informando identificador (e-mail institucional `@ufpr.br`, e-mail pessoal cadastrado ou GRR) e senha. Tokens **não** retornam no JSON: cookies HttpOnly `access_token` (Path=/) e `refresh_token` (Path=/auth) + sessão Redis `auth:session:<sid>`. JSON 200: apenas `mustChangePassword` e `mustAcceptLgpd`.

**Pré-condições:**
- O visitante acessa `/login` sem JWT válido (cookie `access_token` ausente ou expirado).
- A conta do usuário existe e está ativa no sistema (quando as credenciais forem válidas).

**Pós-condições:**
- Credenciais válidas com `senha_alterada = true` → cookies de sessão + JSON `{ mustChangePassword, mustAcceptLgpd }` + redirecionamento para `/inicio`.
- Credenciais válidas com `senha_alterada = false` → resposta com `mustChangePassword: true` + redirecionamento para `/primeiro-acesso` (RF-F1-002).
- Credenciais inválidas ou conta bloqueada → HTTP 401 com mensagem genérica anti-enumeração.
- Toda tentativa registrada em `audit_log` (`iam.login_success` ou `iam.login_failed`).

**Critérios de aceitação:**
1. Identificador aceita e-mail `@ufpr.br`, e-mail pessoal ou GRR numérico; backend normaliza antes da busca (RN-F0.1-01).
2. Verificação de senha conforme RNF-SEC-01 (Argon2id obrigatório); qualquer uso de MD5, SHA-1 ou bcrypt para senhas é proibido e detectado por regra detekt.
3. Sucesso com senha já alterada: `POST /auth/login` retorna HTTP 200 com JSON `{ mustChangePassword, mustAcceptLgpd }` (sem `accessToken`/`refreshToken` no body). Cookies HttpOnly `access_token` e `refresh_token`; sessão Redis `auth:session:<sid>` (claim JWT `sid`). Fallback Bearer apenas no filtro JWT.
4. Sucesso com primeiro acesso pendente: resposta com `mustChangePassword: true`; frontend bloqueia dashboard e redireciona para `/primeiro-acesso`; navegação para `/inicio` impossível até troca de senha (RN-F0.1-04).
5. Credenciais inválidas (senha errada, identificador inexistente, conta inativa ou bloqueada): HTTP 401 RFC 7807 com mensagem genérica `"Credenciais inválidas. Verifique seus dados e tente novamente."` sem revelar qual caso ocorreu (RN-F0.1-08); campo identificador mantido, senha limpa.
6. Rate limit: 6ª tentativa em menos de 1 minuto para o mesmo par IP + identificador → HTTP 429 com mensagem `"Muitas tentativas. Aguarde antes de tentar novamente."` (RN-F0.1-06, RN-F0.1-09).
7. Após 10 falhas consecutivas para o mesmo identificador: conta bloqueada por 15 minutos; resposta externa idêntica à de credencial inválida; evento interno `iam.account_blocked` em auditoria (RN-F0.1-07).
8. Campos vazios: validação frontend impede chamada à API; exibe erro inline e foco no primeiro campo inválido.
9. Links de navegação: "Esqueci minha senha" → `/recuperar-senha`; "Contato" → `/contato`; "Verificar protocolo ou certificado" → `/publico/verificar-protocolo`.
10. Reuso de refresh token rotacionado detectado como sinal de comprometimento: todas as sessões do usuário são invalidadas e evento `iam.suspicious_token_reuse` é registrado em auditoria (conforme mecanismo RNF-SEC-03).
11. Acessibilidade e responsividade conforme RNF-UX-01 (WCAG 2.1 AA), RNF-UX-02 (≥ 375px) e RNF-UX-03 (navegação por teclado).
12. Relacionado — exchange OTT: `POST /auth/ott` (`ExchangeOttUseCase`, UC-AUT-07) troca JWT de deep-link (`?ott=`) por sessão + cookies; mesmo JSON do login; replay → 401 (RN-F0.1-13).

**Regras de negócio relacionadas:** RN-F0.1-01 a RN-F0.1-13

**Dependências:** RF-F1-002 (primeiro acesso), RNF-SEC-01, RNF-SEC-02, RNF-SEC-03, RNF-SEC-04, RNF-SEC-09, RNF-DES-03, RNF-CON-03, RNF-UX-01, RNF-UX-02, RNF-UX-03

---

### RF-F0-002 — Solicitar link de recuperação de senha

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-002 |
| **Nome** | Solicitar link de recuperação de senha |
| **Prioridade** | P1 |
| **Ator(es)** | A1 Visitante; A2 Aluno; A3 Egresso; A4 Professor; A7 Secretaria; A8 Coordenador; A9 Admin |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-002 |
| **Rastreio UC** | UC-AUT-02 |
| **Tela** | F0.2 `/recuperar-senha` |
| **API** | `POST /auth/forgot-password` |
| **Legado** | RF-02 (T02 `enviarEmailSenha.jsp`) |

**Descrição:** O sistema deve permitir que um usuário que esqueceu sua senha informe seu e-mail e receba — quando o e-mail estiver cadastrado — um link de redefinição válido por 24 horas, sem revelar se o endereço existe na base de dados.

**Pré-condições:**
- O visitante acessa `/recuperar-senha` a partir de `/login` ou URL direta.

**Pós-condições:**
- Sempre retorna HTTP 202 com mensagem neutra, independentemente da existência do e-mail.
- Se e-mail cadastrado e usuário ativo: JWT de uso único gerado (`audience=password-reset`, TTL 24h, JTI UUID v7) e evento `iam.password_reset_requested` enfileirado no Outbox para envio de e-mail.
- Se e-mail inexistente: nenhum e-mail enviado; resposta visual idêntica à do fluxo principal.
- Solicitação registrada em `audit_log`.

**Critérios de aceitação:**
1. E-mail válido e cadastrado: `POST /auth/forgot-password` retorna 202; formulário oculto; exibe mensagem `"Se este e-mail estiver cadastrado, você receberá um link válido por 24 horas."`; token enfileirado via Outbox com template `PASSWORD_RESET` (RN-F0.2-01 a RN-F0.2-04).
2. E-mail não cadastrado: mesma resposta 202 e mesma mensagem visual; nenhum e-mail disparado (anti-enumeração).
3. Formato de e-mail inválido no frontend: sem chamada à API; mensagem inline `"Informe um e-mail válido"` e borda de erro (RN-F0.2-05).
4. Durante submit: botão em estado loading; campo desabilitado.
5. Falha de rede: `DS/AlertBanner` danger com opção de nova tentativa; formulário permanece acessível.
6. Após sucesso, reenvio na mesma tela bloqueado até navegar de volta (RN-F0.2-06).
7. Rate limit: máximo 3 solicitações por hora por par e-mail + IP; HTTP 429 quando excedido (RN-F0.2-08).
8. Botão "Voltar" redireciona para `/login`.

**Regras de negócio relacionadas:** RN-F0.2-01 a RN-F0.2-08

**Dependências:** RF-F0-003, RF-TR-002, RNF-SEC-05, RNF-SEC-09, RNF-CON-01, RNF-CON-03, RNF-UX-05

---

### RF-F0-003 — Redefinir senha via token de uso único

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-003 |
| **Nome** | Redefinir senha via token de uso único |
| **Prioridade** | P1 |
| **Ator(es)** | A1 Visitante; A2 Aluno; A3 Egresso; A4 Professor; A7 Secretaria; A8 Coordenador; A9 Admin |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-003 |
| **Rastreio UC** | UC-AUT-03 |
| **Tela** | F0.3 `/nova-senha?token=` |
| **API** | `POST /auth/reset-password` |
| **Legado** | RF-03/04 (T03/T04 `alterarSenha.jsp`) |

**Descrição:** O sistema deve permitir que um usuário que acessou o link de recuperação de senha defina uma nova credencial que atenda aos requisitos de segurança, invalidando o token após o uso e encerrando todas as sessões anteriores.

**Pré-condições:**
- O usuário possui link válido gerado por RF-F0-002 com JWT não expirado e JTI não consumido.
- Parâmetro `?token=` presente na URL `/nova-senha`.

**Pós-condições:**
- Senha atualizada com hash Argon2id; `usuario.senha_alterada = true`.
- JTI inserido na blacklist; token não reutilizável.
- Todas as sessões ativas (access + refresh) invalidadas.
- Evento `iam.password_reset_completed` em `audit_log`.
- Usuário redirecionado para `/login` com mensagem de sucesso.

**Critérios de aceitação:**
1. Token válido: exibe formulário com campos "Nova senha" e "Confirmar senha", medidor de força em tempo real e lista de requisitos (RN-F0.3-01).
2. Token inválido, expirado ou já consumido: exibe `DS/EmptyState` `"Link inválido ou expirado."` com botão "Solicitar novo link" → `/recuperar-senha`; formulário não exibido; mensagem não distingue "já usado" de "inválido" (RN-F0.3-02, RN-F0.3-03, CA-07).
3. Nova senha: mínimo 12 caracteres; pelo menos 1 maiúscula, 1 minúscula, 1 dígito e 1 caractere especial (`!@#$%^&*`) (RN-F0.3-04, RN-F0.3-05).
4. Senha igual a uma das 3 anteriores: HTTP 422 RFC 7807 com mensagem `"Esta senha já foi utilizada recentemente. Escolha uma senha diferente."` (RN-F0.3-06).
5. Confirmação divergente: validação frontend impede submit; mensagem `"As senhas não coincidem"`.
6. Reset bem-sucedido: `POST /auth/reset-password` retorna 200; JTI na blacklist; sessões invalidadas; redirecionamento para `/login` com banner success (RN-F0.3-08 a RN-F0.3-11).
7. Medidor de força atualiza em 4 níveis conforme critérios cumpridos (CA-02).
8. Validação de todos os requisitos de senha obrigatória no backend, não apenas no frontend — princípio de defesa em profundidade (RNF-MAN-05 §Clean Architecture; segurança nunca depende exclusivamente do cliente).

**Regras de negócio relacionadas:** RN-F0.3-01 a RN-F0.3-11

**Dependências:** RF-F0-002, RNF-SEC-01, RNF-SEC-05, RNF-CON-03

---

### RF-F0-004 — Exibir informações de contato da secretaria

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-004 |
| **Nome** | Exibir informações de contato da secretaria |
| **Prioridade** | P2 |
| **Ator(es)** | A1 Visitante; qualquer usuário autenticado |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-004 |
| **Rastreio UC** | UC-PUB-01 |
| **Tela** | F0.4 `/contato` |
| **API** | — (conteúdo estático parametrizado) |
| **Legado** | RF-53 (T14 `contato.jsp`) |

**Descrição:** O sistema deve exibir uma página pública com endereço, telefones, e-mail institucional e horário de atendimento da secretaria, acessível sem autenticação.

**Pré-condições:**
- Nenhuma autenticação necessária.

**Pós-condições:**
- Visitante visualiza dados de contato institucionais.
- Link "Voltar ao login" disponível no rodapé.

**Critérios de aceitação:**
1. Exibe card com endereço (ícone MapPin), telefone(s) como links `tel:`, e-mail como link `mailto:` e horário de atendimento (ícone Clock) (CA-01).
2. Conteúdo parametrizável via variável de ambiente ou arquivo de configuração — sem hardcode no código-fonte (RN-F0.4-01).
3. Layout responsivo: 2 colunas (informações | mapa) em viewport ≥ 1024px; 1 coluna empilhada abaixo de 1024px (CA-02).
4. Mapa com texto alternativo descritivo da localização (RN-F0.4-04); telefones com `aria-label` legível (CA-04).
5. "Voltar ao login" redireciona para `/login` (RN-F0.4-03).
6. Em mobile nativo, link abre em browser externo em vez de renderizar in-app (RN-F0.4-05).

**Regras de negócio relacionadas:** RN-F0.4-01 a RN-F0.4-05

**Dependências:** RNF-UX-01, RNF-UX-02

---

### RF-F0-005 — Exibir página de erro amigável por código HTTP

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-005 |
| **Nome** | Exibir página de erro amigável por código HTTP |
| **Prioridade** | P1 |
| **Ator(es)** | A1 Visitante; qualquer usuário autenticado |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-005 |
| **Rastreio UC** | UC-PUB-01 |
| **Tela** | F0.5 `/erro/:codigo` |
| **API** | — (renderização client-side; incidentId do RFC 7807 em erros 5xx) |
| **Legado** | T11 `menuErrado.jsp`, T155 `exibirErro.jsp` |

**Descrição:** O sistema deve apresentar telas de erro amigáveis e contextualizadas para os códigos HTTP 401, 403, 404 e 500, com mensagens em linguagem natural, ações de recuperação e ID de incidente quando aplicável, sem expor detalhes técnicos internos.

**Pré-condições:**
- Ocorre erro de navegação, autorização, recurso não encontrado ou falha interna capturada pelo roteador ou interceptor de API.

**Pós-condições:**
- Usuário visualiza mensagem compreensível e sabe como prosseguir (login, início ou suporte).
- Nenhum stack trace, query SQL ou nome de classe exposto.

**Critérios de aceitação:**
1. **401** — mensagem `"Você precisa fazer login para acessar esta página."`; ícone cadeado; paleta neutra; botões "Fazer login" (→ `/login`) e "Ir ao início" (→ `/`) para usuário não autenticado (RN-F0.5-01 a RN-F0.5-06).
2. **403** — mensagem `"Você não tem permissão para acessar este recurso."`; paleta danger subtle; botões "Ir ao início" (→ `/inicio`) e "Ir ao suporte" (→ `/suporte`) (RN-F0.5-02, RN-F0.5-07).
3. **404** — mensagem `"Página não encontrada. O link pode estar desatualizado."`; ícone arquivo riscado; roteador captura rotas inexistentes e redireciona automaticamente (RN-F0.5-08).
4. **500** — mensagem `"Erro inesperado no servidor. Nossa equipe foi notificada."`; ícone raio; paleta warning; exibe ID de incidente no formato `INC-YYYY-XXXX` derivado do campo `instance` do RFC 7807 quando disponível (RN-F0.5-05, RN-F0.5-09).
5. Interceptor TanStack Query redireciona erros 5xx da API para `/erro/500`; Error Boundary React captura erros de renderização (DoD US-F0-005).
6. Nenhum detalhe técnico visível ao usuário (CA-06).
7. Ilustrações com `alt` descritivo; código e mensagem anunciados em sequência lógica para leitores de tela (CA-07).

**Regras de negócio relacionadas:** RN-F0.5-01 a RN-F0.5-09

**Dependências:** RNF-CON-03, RNF-UX-01, RNF-UX-05

---

### RF-F0-006 — Verificar autenticidade de protocolo PDF

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-006 |
| **Nome** | Verificar autenticidade de protocolo PDF |
| **Prioridade** | P2 |
| **Ator(es)** | A1 Visitante; S6 Verificador externo |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-006 |
| **Rastreio UC** | UC-CRT-02 |
| **Tela** | F0.6 `/publico/verificar-protocolo/:id` |
| **API** | `GET /publico/protocolos/{id}/verificacao` |
| **Legado** | T144–T147 `protocolo.jsp` |

**Descrição:** O sistema deve permitir que terceiros verifiquem publicamente a autenticidade de um protocolo de solicitação emitido pelo sistema, consultando metadados sanitizados e comparando localmente o hash SHA-256 de um PDF recebido, sem enviar o arquivo ao servidor.

**Pré-condições:**
- O verificador possui número de protocolo ou acessa URL via QR Code do PDF.
- Endpoint público acessível sem autenticação.

**Pós-condições:**
- Protocolo existente: metadados exibidos com hash SHA-256 de referência.
- Upload opcional de PDF: resultado "confere" ou "não confere" calculado no browser.
- Protocolo inexistente: mensagem de não encontrado sem zona de upload.

**Critérios de aceitação:**
1. `GET /publico/protocolos/{id}/verificacao` retorna 200 com: número, tipo, status, datas de criação/conclusão, hash SHA-256 (truncado visualmente, completo em tooltip/área expansível) e nome do solicitante parcialmente mascarado (RN-F0.6-01 a RN-F0.6-03).
2. Zona drag-and-drop aceita apenas PDF; cálculo SHA-256 no browser via Web Crypto API; arquivo **não** enviado ao servidor (RN-F0.6-04, RN-F0.6-06).
3. Hash do PDF local coincide com o do backend → `DS/AlertBanner` success `"Hash confere. O documento é autêntico e não foi modificado."` (RN-F0.6-05).
4. Hash diverge → `DS/AlertBanner` danger alertando possível adulteração; nenhum dado adicional enviado ao servidor (CA-03).
5. Protocolo não encontrado: HTTP 404; `DS/EmptyState` `"Protocolo não encontrado"`; sem zona de upload (CA-04).
6. Durante carregamento: `DS/Skeleton` na área do card; upload oculto (CA-05).
7. Rate limit: 10 requisições/minuto por IP; HTTP 429 com banner warning (RN-F0.6-07).
8. Dropzone acessível via teclado com botão "Selecionar arquivo"; resultado anunciado via `aria-live="assertive"` (CA-07).
9. Tentativas de bruteforce geram alerta configurável no Grafana (RN-F0.6-08).

**Regras de negócio relacionadas:** RN-F0.6-01 a RN-F0.6-09

**Dependências:** RF-TR-001 (geração de protocolo), RNF-LGL-01, RNF-CON-03

---

### RF-F0-007 — Verificar autenticidade de certificado digital

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-007 |
| **Nome** | Verificar autenticidade de certificado digital |
| **Prioridade** | P2 |
| **Ator(es)** | A1 Visitante; S6 Verificador externo |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-007 |
| **Rastreio UC** | UC-CRT-03 |
| **Tela** | F0.7 `/publico/verificar-certificado/:hash` |
| **API** | `GET /publico/certificados/{hash}/verificacao`; `GET /.well-known/jwks.json` |
| **Legado** | — (novo no SO2) |

**Descrição:** O sistema deve permitir que terceiros verifiquem publicamente a autenticidade de certificados emitidos exclusivamente pelo sistema, validando hash SHA-256 e assinatura digital ED25519 com a chave pública publicada em JWKS, sem aceitar upload de certificados externos como documentos oficiais.

**Pré-condições:**
- O certificado foi gerado pelo sistema (nunca upload externo — RN-F0.7-01).
- O verificador acessa URL `/publico/verificar-certificado/{hash}` via QR Code do PDF ou hash informado.

**Pós-condições:**
- Certificado válido: status "Certificado válido" com metadados sanitizados e confirmação criptográfica.
- Certificado inválido ou hash inexistente: status "Certificado inválido" sem dados do beneficiário.
- Certificado revogado: status "Certificado revogado" com dados históricos mínimos.

**Critérios de aceitação:**
1. Endpoint público retorna metadados sanitizados, hash SHA-256, assinatura ED25519 (base64url) e URL JWKS (RN-F0.7-04).
2. Frontend busca chave pública em `/.well-known/jwks.json` e valida assinatura ED25519 no browser via Web Crypto API (RN-F0.7-05, RN-F0.7-06).
3. Assinatura válida: `DS/VerificationSeal` success + badge "Certificado válido"; exibe evento/atividade, carga horária, data de emissão e link "Ver chave pública" (CA-01, RN-F0.7-09).
4. Hash inexistente ou assinatura inválida: selo danger + badge "Certificado inválido"; mensagem de possível falsificação; sem dados do beneficiário (CA-02).
5. Status `REVOGADO`: badge warning "Certificado revogado" com explicação; dados básicos para identificação histórica mantidos (CA-03).
6. Upload opcional de PDF: SHA-256 calculado localmente; comparação com hash registrado; resultado success/danger (CA-04).
7. Chave pública JWKS no formato JWK com algoritmo Ed25519 (`kty: OKP`, `crv: Ed25519`) (RN-F0.7-03, CA-05).
8. Rate limit: 10 requisições/minuto por IP; HTTP 429 com banner warning (RN-F0.7-10).
9. Resposta não expõe GRR completo nem e-mail do beneficiário (RN-F0.7-11).
10. Status anunciado via `aria-live="assertive"`; selo com `alt` descritivo (CA-08).

**Regras de negócio relacionadas:** RN-F0.7-01 a RN-F0.7-11

**Dependências:** RF-TR-003, RNF-LGL-02, RNF-CON-03, RNF-UX-01

---

*Última atualização: 2026-08-29 — as-built: cookies HttpOnly (sem tokens no JSON), sessão Redis sid, POST /auth/ott*
