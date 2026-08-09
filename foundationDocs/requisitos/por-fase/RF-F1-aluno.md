# Requisitos Funcionais — Fase F1 (Aluno)

**Projeto:** SecretariaOnline2 — UFPR SEPT  
**Versão:** 1.0  
**Data:** 2026-06-23  
**Gerado a partir de:** US-F1-001 a US-F1-011; `fluxos_por_perfil.md` §2; `telas.md` §3; `legenda_siglas_casos_de_uso_por_ator.md`; `mvp_v1_walking_skeleton_aluno.md`; `endpoints_canonicos_presenca_eventos_v4.md`  
**Total RF neste arquivo:** 15 (11 HUs → 15 capacidades)

---

## Resumo da fase

| RF | Nome | HU | UC | Tela(s) | Prioridade |
|----|------|----|----|---------|:----------:|
| RF-F1-001 | Visualizar dashboard unificado do aluno | US-F1-001 | UC-DASH-01 | F1.1 `/inicio` | P0 |
| RF-F1-002 | Completar primeiro acesso (senha + LGPD) | US-F1-002 | UC-AUT-04 | F1.2 `/primeiro-acesso` | P0 |
| RF-F1-003-a | Editar dados pessoais do perfil | US-F1-003 | UC-AUT-05 | F1.3 `/perfil` | P2 |
| RF-F1-003-b | Trocar senha e gerenciar sessões ativas | US-F1-003 | UC-AUT-05 | F1.4 `/perfil/seguranca` | P2 |
| RF-F1-003-c | Configurar preferências de notificação | US-F1-003 | UC-AUT-06 | F1.5 `/perfil/notificacoes` | P2 |
| RF-F1-003-d | Exportar dados pessoais (portabilidade LGPD) | US-F1-003 | UC-AUT-05 | F1.3 `/perfil` (seção Privacidade) | P2 |
| RF-F1-004 | Visualizar e gerenciar comunicações recebidas | US-F1-004 | UC-COM-01 | F1.6 `/comunicacao` | P2 |
| RF-F1-005-a | Listar solicitações acadêmicas próprias | US-F1-005 | UC-SOL-03 | F1.7 `/solicitacoes` | P2 |
| RF-F1-005-b | Abrir solicitação via wizard dinâmico | US-F1-005 | UC-SOL-01 | F1.8 `/solicitacoes/nova` | P1 |
| RF-F1-005-c | Acompanhar detalhe e timeline de solicitação | US-F1-005 | UC-SOL-03, UC-SOL-05 | F1.9 `/solicitacoes/:id` | P2 |
| RF-F1-006 | Submeter e acompanhar atividades formativas | US-F1-006 | UC-FOR-01, UC-FOR-02 | F1.10–F1.12 | P2 |
| RF-F1-007 | Acompanhar estágios e enviar documentos | US-F1-007 | UC-EST-01 | F1.13–F1.14 | P2 |
| RF-F1-008 | Acompanhar TCC e enviar versão final | US-F1-008 | UC-TCC-01 | F1.15–F1.16 | P2 |
| RF-F1-009 | Consultar eventos e confirmar presença | US-F1-009 | UC-PRE-03 | F1.17–F1.18 | P2 |
| RF-F1-010 | Visualizar e baixar certificados emitidos | US-F1-010 | UC-CRT-01 | F1.19 `/certificados` | P2 |
| RF-F1-011 | Consultar atendimentos e dar ciência | US-F1-011 | UC-ATD-02 | F1.20 `/meus-atendimentos` | P2 |

---

### RF-F1-001 — Visualizar dashboard unificado do aluno

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-001 |
| **Nome** | Visualizar dashboard unificado do aluno |
| **Prioridade** | P0 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-001 |
| **Rastreio UC** | UC-DASH-01 |
| **Tela** | F1.1 `/inicio` |
| **API** | `GET /bff/dashboard/aluno` |
| **Legado** | T08/T09 `home.jsp` |

**Descrição:** O sistema deve apresentar ao aluno autenticado um painel unificado com KPIs, pendências de ação, últimas solicitações, próximos eventos, prazos e atalhos — agregados pelo BFF em uma única chamada, com renderização condicional orientada a HATEOAS.

**Pré-condições:**
- Aluno autenticado com `mustChangePassword = false`.
- Capability `dashboard.view_own` concedida.

**Pós-condições:**
- Dashboard renderizado com dados agregados ou estados vazios/degradação por seção.
- CTAs e atalhos exibidos somente quando `_links` correspondentes existirem na resposta.

**Critérios de aceitação:**
1. `GET /bff/dashboard/aluno` retorna saudação, KPIs (horas formativas validadas/requeridas, solicitações abertas, eventos hoje, certificados), pendências (máx. 3), últimas solicitações (máx. 5), próximos eventos (máx. 3), prazos, último parecer e `_links` (RN-F1.1-01 a RN-F1.1-06).
2. Durante carregamento: `DS/Skeleton` por bloco; após 200 OK renderiza conteúdo completo (CA-01).
3. KpiCard de horas formativas exibe `validadas / requeridas` com barra de progresso (CA-02).
4. Pendências exibem CTA derivado de `_links` do item; seção vazia com `DS/EmptyState` quando ausente (CA-03, CA-06).
5. Solicitações com `prazo_em < now` exibem data em `status/danger` (CA-04, RN-F1.1-04).
6. Eventos com janela ativa exibem badge "Janela aberta" quando aplicável (RN-F1.1-05).
7. Degradação graciosa: falha em um módulo exibe `DS/AlertBanner` warning na seção afetada sem bloquear demais seções (CA-05).
8. Botão "Nova solicitação", badge do hub e QuickTiles renderizados apenas a partir de `_links` — nunca hardcoded (RN-F1.1-07 a RN-F1.1-09).
9. Mobile: pull-to-refresh invalida cache TanStack Query e rebusca dados (CA-07, RN-F1.1-11).
10. Responsivo: 4 colunas KPI em ≥1280px; 2×2 em tablet; drawer sidebar em <768px (CA-08).
11. FCP < 1,5 s (RNF-DES-04).

**Regras de negócio relacionadas:** RN-F1.1-01 a RN-F1.1-11

**Dependências:** RF-F0-001, RF-F1-002, RF-TR-006, RNF-DES-04, RNF-UX-04, RNF-UX-05

---

### RF-F1-002 — Completar primeiro acesso (senha + LGPD)

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-002 |
| **Nome** | Completar primeiro acesso (senha + LGPD) |
| **Prioridade** | P0 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-002 |
| **Rastreio UC** | UC-AUT-04 |
| **Tela** | F1.2 `/primeiro-acesso` |
| **API** | `POST /auth/first-access` |
| **Legado** | T06 `novaSenhaAluno.jsp` |

**Descrição:** O sistema deve obrigar o aluno com `senha_alterada = false` a definir senha pessoal forte e aceitar a política de privacidade (LGPD) antes de acessar qualquer outra funcionalidade.

**Pré-condições:**
- Login bem-sucedido com `mustChangePassword = true` (RF-F0-001).
- Capability `auth.first_access` concedida.

**Pós-condições:**
- `usuario.senha_alterada = true`; `usuario.metadata.aceite_lgpd_em` registrado com IP e User-Agent.
- Evento `iam.first_access_completed` em `audit_log`.
- Redirecionamento para `/inicio` (RF-F1-001).

**Critérios de aceitação:**
1. Tela exibe formulário de nova senha, confirmação, medidor de força, checkbox LGPD obrigatório e botão "Continuar" desabilitado até requisitos cumpridos (CA-01, CA-02).
2. Senha: mín. 12 caracteres, maiúscula, minúscula, dígito e especial; confirmação idêntica; não pode ser igual à senha temporária (RN-F1.2-03 a RN-F1.2-05, CA-03).
3. Checkbox LGPD obrigatório; link para política abre em modal/nova aba (RN-F1.2-06, RN-F1.2-07).
4. `POST /auth/first-access` com sucesso conclui fluxo e habilita navegação completa (CA-04, RN-F1.2-09).
5. Bloqueio de navegação: qualquer rota protegida redireciona para `/primeiro-acesso` enquanto pendente (RN-F1.2-01, RN-F1.2-02, CA-05).
6. Sidebar sem links de navegação até conclusão (RN-F1.2-02).
7. Acessibilidade: tab order lógico, `aria-live="assertive"` em erros (CA-06).
8. Segunda tentativa após sessão expirada pode exigir CAPTCHA (RN-F1.2-10).

**Regras de negócio relacionadas:** RN-F1.2-01 a RN-F1.2-10

**Dependências:** RF-F0-001, RF-F1-001, RNF-SEC-01, RNF-LGL-01, RNF-UX-03

---

### RF-F1-003-a — Editar dados pessoais do perfil

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-003-a |
| **Nome** | Editar dados pessoais do perfil |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-003 (HU-A) |
| **Rastreio UC** | UC-AUT-05 |
| **Tela** | F1.3 `/perfil` |
| **API** | `GET /me`, `PATCH /me` |
| **Legado** | T05, T119 (parcial) |

**Descrição:** O sistema deve permitir que o aluno visualize e edite dados pessoais não acadêmicos (nome social, telefone, e-mail pessoal, foto), mantendo campos institucionais somente leitura.

**Pré-condições:**
- Aluno autenticado com capability `user.update_own_profile`.

**Pós-condições:**
- Dados atualizados persistidos; foto armazenada em MinIO quando alterada.

**Critérios de aceitação:**
1. Campos editáveis: nome social, telefone, e-mail pessoal, foto. Somente leitura: GRR, e-mail institucional, curso, período (RN-F1.3-01).
2. Dirty state: botão "Salvar" habilitado após alteração; "Cancelar" reverte sem API (RN-F1.3-03, CA-01).
3. `PATCH /me` persiste alterações; toast de sucesso ao concluir (CA-01).
4. Foto: crop circular; JPEG/PNG/WebP; máx. 2 MB; upload via URL pré-assinada MinIO (RN-F1.3-02, CA-02).
5. Arquivo > 2 MB rejeitado com mensagem clara (CA-02).

**Regras de negócio relacionadas:** RN-F1.3-01 a RN-F1.3-03

**Dependências:** RNF-POR-03, RNF-LGL-01, RNF-UX-05

---

### RF-F1-003-b — Trocar senha e gerenciar sessões ativas

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-003-b |
| **Nome** | Trocar senha e gerenciar sessões ativas |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-003 (HU-B) |
| **Rastreio UC** | UC-AUT-05 |
| **Tela** | F1.4 `/perfil/seguranca` |
| **API** | `POST /me/password`, `GET /me/sessions`, `DELETE /me/sessions/:id` |
| **Legado** | T05 (alterar senha) |

**Descrição:** O sistema deve permitir que o aluno troque sua senha (exigindo senha atual) e visualize/encerre sessões ativas em outros dispositivos.

**Pré-condições:**
- Aluno autenticado com capability `user.update_own_password`.

**Pós-condições:**
- Senha alterada com Argon2id ou sessão específica invalidada.

**Critérios de aceitação:**
1. Troca de senha exige senha atual correta; nova senha segue requisitos de US-F0-003 (RN-F1.4-01, RN-F1.4-02, CA-03).
2. Senha atual incorreta → HTTP 401 com mensagem inline (CA-03).
3. Sucesso na troca invalida todas as outras sessões exceto a atual (RN-F1.4-05, CA-03).
4. Lista de sessões exibe dispositivo (User-Agent simplificado), IP e último uso (RN-F1.4-03, CA-04).
5. Encerrar sessão via `DELETE /me/sessions/:id` somente se `_links.encerrar` existir; sessão atual sem botão de encerramento (RN-F1.4-04, CA-04).

**Regras de negócio relacionadas:** RN-F1.4-01 a RN-F1.4-05

**Dependências:** RF-F0-003, RNF-SEC-01, RNF-SEC-03

---

### RF-F1-003-c — Configurar preferências de notificação

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-003-c |
| **Nome** | Configurar preferências de notificação |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-003 (HU-C) |
| **Rastreio UC** | UC-AUT-06 |
| **Tela** | F1.5 `/perfil/notificacoes` |
| **API** | `GET /me/notification-preferences`, `PATCH /me/notifications` |
| **Legado** | — (lacuna L04 corrigida) |

**Descrição:** O sistema deve permitir que o aluno configure canais de notificação por prioridade, horário DND e modo digest.

**Pré-condições:**
- Aluno autenticado com capability `user.update_own_preferences`.

**Pós-condições:**
- Preferências persistidas e aplicadas pelo dispatcher de notificações.

**Critérios de aceitação:**
1. Matriz prioridade × canal (CRITICAL, HIGH, MEDIUM, LOW × push, e-mail, in-app) (RN-F1.5-01).
2. Notificações CRITICAL não podem ser desabilitadas — switches bloqueados (RN-F1.5-02, CA-05).
3. DND bloqueia push no período configurado; HIGH e CRITICAL ainda chegam por e-mail no DND (RN-F1.5-03).
4. Modo digest agrega notificações não-críticas em e-mail único no período configurado (RN-F1.5-04).
5. `PATCH /me/notifications` persiste alterações (CA-05).

**Regras de negócio relacionadas:** RN-F1.5-01 a RN-F1.5-04

**Dependências:** RF-TR-007, RNF-UX-05

---

### RF-F1-003-d — Exportar dados pessoais (portabilidade LGPD)

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-003-d |
| **Nome** | Exportar dados pessoais (portabilidade LGPD) |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno; A3 Egresso (mesma tela) |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-003 (direito de portabilidade — LGPD Art. 18, III) |
| **Rastreio UC** | UC-AUT-05 |
| **Tela** | F1.3 `/perfil` (seção "Privacidade e Dados") |
| **API** | `POST /me/data-export`; `GET /me/data-export/:jobId` |
| **Legado** | — (lacuna funcional identificada na revisão de classificação) |

> **Origem:** Este RF foi criado na revisão de classificação de 2026-08-09, quando o item (c) do RNF-LGL-01 foi identificado como uma capacidade funcional observável (tela + API + fluxo) indevidamente documentada como atributo de qualidade.

**Descrição:** O sistema deve permitir que o titular dos dados pessoais solicite e baixe um arquivo contendo todos os dados pessoais armazenados sobre ele, em cumprimento ao direito de portabilidade da LGPD (Art. 18, III). O arquivo deve ser gerado de forma assíncrona e disponibilizado para download por link temporário.

**Pré-condições:**
- Aluno autenticado com capability `user.export_own_data`.

**Pós-condições:**
- Arquivo JSON ou CSV gerado com dados pessoais do usuário; link de download válido por 24 horas; evento `lgpd.data_export_requested` registrado em `audit_log`.

**Critérios de aceitação:**
1. Botão "Exportar meus dados" visível na seção "Privacidade e Dados" da tela `/perfil`; somente exibido quando `_links.export-data` estiver presente na resposta do BFF.
2. `POST /me/data-export` retorna 202 com `jobId`; banner informativo: "Sua solicitação foi recebida. O arquivo estará disponível em alguns minutos."
3. `GET /me/data-export/:jobId` retorna status `PENDING`, `READY` ou `EXPIRED`; quando `READY`, inclui URL pré-assinada MinIO válida por 24 h.
4. Conteúdo do arquivo inclui: dados cadastrais, e-mails de contato, histórico de solicitações (números e tipos, sem conteúdo interno), registros de presença em eventos, preferências de notificação e data de aceite LGPD.
5. **Não inclui:** dados acadêmicos de terceiros, histórico de auditoria de outros usuários, dados de saúde ou outros dados sensíveis além do necessário.
6. Arquivo no formato JSON estruturado com seções nomeadas por domínio; alternativa CSV disponível via parâmetro `?format=csv`.
7. Máximo 1 exportação por usuário por período de 24 horas; nova solicitação dentro do período retorna HTTP 429 com link para download do arquivo anterior se ainda válido.
8. Arquivo eliminado do armazenamento após expiração de 24 horas (TTL MinIO/S3).
9. Evento `lgpd.data_export_requested` registrado em `audit_log` com `id_ator`, timestamp e IP conforme RNF-LGL-01.

**Regras de negócio relacionadas:** Lei 13.709/2018 Art. 18, III; RN-F1.3-01 (campos exibidos no perfil)

**Dependências:** RNF-LGL-01, RNF-POR-03, RNF-SEC-08, RF-TR-004, RNF-UX-05

---

### RF-F1-004 — Visualizar e gerenciar comunicações recebidas

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-004 |
| **Nome** | Visualizar e gerenciar comunicações recebidas |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-004 |
| **Rastreio UC** | UC-COM-01 |
| **Tela** | F1.6 `/comunicacao` |
| **API** | `GET /communications`, `POST /communications/:id/read` |
| **Legado** | `mensagem` legado, T133 |

**Descrição:** O sistema deve exibir hub unificado de comunicações (institucional, turma, inbox de ações) com filtros, marcação de leitura e CTAs para itens que requerem ação.

**Pré-condições:**
- Aluno autenticado com capability `communication.read`.

**Pós-condições:**
- Comunicação marcada como lida quando aplicável; badge do topbar atualizado.

**Critérios de aceitação:**
1. Tabs: Todos, Institucional, Turma, Inbox — cada uma com badge de não lidos (RN-F1.6-01, CA-01).
2. Tab Inbox exibe apenas itens com CTA pendente; CTA pulsante para ação requerida (RN-F1.6-02, CA-03).
3. Marcar como lido via `POST /communications/:id/read` quando `_links.marcar-lido` existir (RN-F1.6-03, CA-02).
4. Filtros (lido, tipo, curso) aplicados no backend via query params (RN-F1.6-04, CA-04).
5. Entrega assíncrona via Outbox (`comunicacao.published`) (RN-F1.6-05).
6. Badge topbar atualizado por polling 60s ou push (RN-F1.6-06).
7. Mobile: tabs scrolláveis horizontalmente; alvos touch ≥ 44px (CA-05).

**Regras de negócio relacionadas:** RN-F1.6-01 a RN-F1.6-06

**Dependências:** RF-TR-002, RF-TR-007, RNF-UX-05

---

### RF-F1-005-a — Listar solicitações acadêmicas próprias

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-005-a |
| **Nome** | Listar solicitações acadêmicas próprias |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-005 (HU-A) |
| **Rastreio UC** | UC-SOL-03 |
| **Tela** | F1.7 `/solicitacoes` |
| **API** | `GET /requests?solicitante=me` |
| **Legado** | T17–T20 |

**Descrição:** O sistema deve permitir que o aluno liste suas solicitações acadêmicas com filtros, indicador de SLA e acesso à criação de nova solicitação quando autorizado.

**Pré-condições:**
- Aluno autenticado com capability `request.view_own`.

**Pós-condições:**
- Lista paginada exibida; navegação para detalhe ou wizard quando aplicável.

**Critérios de aceitação:**
1. Tabela paginada (20 itens/página) com colunas: Número, Tipo, Estado, Prazo, SLA (RN-F1.7-01, CA-01).
2. Filtros backend: estado, tipo, ano, busca textual (RN-F1.7-01).
3. `prazo_em < now` exibe data em `status/danger` (RN-F1.7-02, CA-01).
4. Botão "Nova solicitação" somente se `_links.novaSolicitacao` existir (CA-01).
5. Mobile: cards empilhados; filtros em Sheet/drawer (RN-F1.7-03).
6. Rascunhos exibidos com badge "Rascunho" (RN-F1.8-05).
7. Estados loading, empty e error por seção (RNF-UX-05).

**Regras de negócio relacionadas:** RN-F1.7-01 a RN-F1.7-03

**Dependências:** RF-F1-005-b, RF-F1-005-c, RF-TR-001, RNF-DES-06, RNF-UX-04

---

### RF-F1-005-b — Abrir solicitação via wizard dinâmico

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-005-b |
| **Nome** | Abrir solicitação via wizard dinâmico |
| **Prioridade** | P1 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-005 (HU-B) |
| **Rastreio UC** | UC-SOL-01 |
| **Tela** | F1.8 `/solicitacoes/nova` |
| **API** | `GET /request-types/{code}`, `POST /requests`, `POST /requests/draft` |
| **Legado** | T15 + 19× `novaSol*` |

**Descrição:** O sistema deve permitir que o aluno abra qualquer tipo de solicitação elegível através de wizard de 3 passos com formulário dinâmico (`form_schema`), anexos e confirmação — sem lógica duplicada por tipo.

**Pré-condições:**
- Aluno autenticado com capability `request.open`.
- Tipos elegíveis filtrados por curso, período e pré-requisitos.

**Pós-condições:**
- Solicitação criada no estado inicial do workflow; `prazo_em` calculado; `numero_anual` gerado; evento Outbox `solicitacoes.opened` enfileirado.
- Redirecionamento para `/solicitacoes/:id`.

**Critérios de aceitação:**
1. Wizard 3 passos: (1) escolha do tipo, (2) formulário dinâmico + anexos, (3) revisão e confirmação (RN-F1.8-01).
2. Passo 1: apenas tipos elegíveis para o aluno exibidos (RN-F1.8-02, CA-02).
3. Passo 2: `DynamicForm` renderiza `form_schema`; validação Zod inline; campos condicionais (RN-F1.8-03, CA-03).
4. Anexos: PDF/JPEG/PNG; máx. 10 MB; upload MinIO via URL pré-assinada; SHA-256 calculado no browser (RN-F1.8-04, CA-04).
5. Rascunho local (PWA/AsyncStorage) + backend `POST /requests/draft` estado `RASCUNHO` (RN-F1.8-05, CA-06).
6. Confirmação: `POST /requests` cria registro, calcula prazo, gera número, enfileira Outbox (RN-F1.8-06, CA-05).
7. Notificação in-app + push após abertura (RN-F1.8-07).
8. Retomada de rascunho ao reabrir wizard (CA-06).

**Regras de negócio relacionadas:** RN-F1.8-01 a RN-F1.8-07

**Dependências:** RF-TR-001, RNF-POR-03, RNF-CON-01, RNF-UX-04

---

### RF-F1-005-c — Acompanhar detalhe e timeline de solicitação

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-005-c |
| **Nome** | Acompanhar detalhe e timeline de solicitação |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-005 (HU-C) |
| **Rastreio UC** | UC-SOL-03, UC-SOL-05 |
| **Tela** | F1.9 `/solicitacoes/:id` |
| **API** | `GET /requests/{id}`, `POST /requests/{id}/protocol` |
| **Legado** | 19× `consultarSol*` + T144–T147 |

**Descrição:** O sistema deve exibir detalhe completo da solicitação, timeline de eventos imutável e ações disponíveis exclusivamente via HATEOAS, incluindo edição em ajuste e geração de protocolo PDF.

**Pré-condições:**
- Aluno autenticado; solicitante = self ou capability deliberativa (escopo aluno: `request.view_own`).

**Pós-condições:**
- Ações executadas conforme transições permitidas pelo workflow; protocolo PDF gerado quando solicitado.

**Critérios de aceitação:**
1. ActionBar derivada exclusivamente de `_links` — UI não conhece workflow interno (RN-F1.9-01, CA-07).
2. Estado `EM_AJUSTE`: link `editar` reabre wizard Passo 2 pré-preenchido (RN-F1.9-02, CA-07).
3. Estado `DELIBERADA`: link `gerar-protocolo` → `POST /requests/{id}/protocol` com QR para verificação pública (RN-F1.9-03, CA-07, RF-F0-006).
4. Timeline `request_event` em ordem reversa (RN-F1.9-04).
5. Anexos listados com download via URL pré-assinada MinIO (15 min) (RN-F1.9-05).
6. HTTP 404 para solicitação inexistente ou sem permissão → tela de erro adequada.

**Regras de negócio relacionadas:** RN-F1.9-01 a RN-F1.9-05

**Dependências:** RF-F1-005-b, RF-TR-001, RF-F0-006, RNF-UX-04, RNF-CON-03

---

### RF-F1-006 — Submeter e acompanhar atividades formativas

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-006 |
| **Nome** | Submeter e acompanhar atividades formativas |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-006 |
| **Rastreio UC** | UC-FOR-01, UC-FOR-02 |
| **Tela** | F1.10 `/formativas` · F1.11 `/formativas/nova` · F1.12 `/formativas/:id` |
| **API** | `GET /formative-entries?aluno=me`, `POST /formative-entries`, `GET /formative-entries/{id}` |
| **Legado** | T96 |

**Descrição:** O sistema deve permitir que o aluno submeta comprovantes de atividades formativas, acompanhe parecer da CAAF e baixe certificados quando aprovado — incluindo confirmação simplificada para eventos com presença validada.

**Pré-condições:**
- Aluno autenticado com `formative.view_own` / `formative.submit`.

**Pós-condições:**
- Entrada formativa criada ou confirmada; notificação à CAAF via Outbox quando submetida.

**Critérios de aceitação:**
1. Lista em `/formativas`: atividade, horas, estado, data; filtro por estado/tipo; botão "Nova" se `_links.nova` (RN-F1.10-01, CA-01).
2. Horas do dashboard contam apenas entradas `APROVADA` (RN-F1.10-02).
3. Submissão manual: selecionar `formative_activity`, declarar horas, anexar comprovante → estado `SUBMETIDA`; Outbox `formativas.submitted` (RN-F1.11-01 a RN-F1.11-03, CA-02).
4. Caminho pré-validado (evento interno): formulário pré-preenchido readonly; confirmação 1 clique sem upload (RN-F1.11-04, CA-03).
5. Detalhe: parecer CAAF, comprovante, horas validadas (RN-F1.12-01).
6. Estado `APROVADA`: botão "Baixar certificado" via `_links.baixar-certificado` (RN-F1.12-02, CA-04).
7. Estado `REJEITADA`: link `resubmeter` se permitido pelo tipo (RN-F1.12-03).

**Regras de negócio relacionadas:** RN-F1.10-01, RN-F1.10-02, RN-F1.11-01 a RN-F1.11-04, RN-F1.12-01 a RN-F1.12-03

**Dependências:** RF-F1-009, RF-F1-010, RF-TR-003, RNF-POR-03, RNF-UX-04

---

### RF-F1-007 — Acompanhar estágios e enviar documentos

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-007 |
| **Nome** | Acompanhar estágios e enviar documentos |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-007 |
| **Rastreio UC** | UC-EST-01 |
| **Tela** | F1.13 `/estagios` · F1.14 `/estagios/:id` |
| **API** | `GET /internships?aluno=me`, `GET /internships/{id}`, `POST /internships/{id}/documents` |
| **Legado** | T88, T86, T87 |

**Descrição:** O sistema deve permitir que o aluno visualize estágios registrados pela secretaria, envie documentos exigidos (TCE, relatórios) e acompanhe pareceres do orientador/COE.

**Pré-condições:**
- Aluno autenticado com `internship.view_own`.
- Estágio previamente cadastrado pela secretaria (F5).

**Pós-condições:**
- Documento enviado e notificação ao orientador/COE via Outbox.

**Critérios de aceitação:**
1. Lista: empresa, supervisor, vigência, situação; filtro por situação; empty state quando vazio (RN-F1.13-01, CA-01).
2. Aluno não abre estágio — apenas acompanha e envia documentos (RN-F1.13-02).
3. Detalhe com tabs Documentos e Pareceres (RN-F1.14-01).
4. Upload habilitado via `_links` por tipo de documento (RN-F1.14-02, CA-02).
5. Upload dispara Outbox `estagios.document_uploaded` (RN-F1.14-03).
6. Pareceres em ordem cronológica reversa na tab Pareceres (RN-F1.14-04, CA-03).

**Regras de negócio relacionadas:** RN-F1.13-01, RN-F1.13-02, RN-F1.14-01 a RN-F1.14-04

**Dependências:** RNF-POR-03, RNF-CON-01, RNF-UX-04

---

### RF-F1-008 — Acompanhar TCC e enviar versão final

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-008 |
| **Nome** | Acompanhar TCC e enviar versão final |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-008 |
| **Rastreio UC** | UC-TCC-01 |
| **Tela** | F1.15 `/tccs` · F1.16 `/tccs/:id` |
| **API** | `GET /tccs?aluno=me`, `GET /tccs/{id}`, `POST /tccs/{id}/upload` |
| **Legado** | T107–T111 |

**Descrição:** O sistema deve permitir que o aluno acompanhe o status do TCC, visualize banca e datas-chave, e faça upload da versão final quando autorizado.

**Pré-condições:**
- Aluno autenticado com `tcc.view_own`.
- TCC cadastrado pela secretaria/coordenação.

**Pós-condições:**
- Arquivo final enviado; estado `SUBMETIDO`; notificação à banca via Outbox.

**Critérios de aceitação:**
1. Lista: título, orientador, situação, data defesa; máx. 1 TCC ativo (RN-F1.15-01, RN-F1.15-02, CA-01).
2. Detalhe: equipe, banca, datas-chave, arquivo atual (RN-F1.16-01, CA-03).
3. Upload via `_links.upload-final` quando estado permite (RN-F1.16-02, CA-02).
4. Upload dispara Outbox `tcc.submitted`; botão desaparece após envio (RN-F1.16-03, CA-02).
5. Resultado da avaliação na timeline do detalhe (RN-F1.16-04).
6. Data limite de entrega com badge danger se prazo próximo (CA-03).

**Regras de negócio relacionadas:** RN-F1.15-01, RN-F1.15-02, RN-F1.16-01 a RN-F1.16-04

**Dependências:** RNF-POR-03, RNF-CON-01, RNF-UX-04

---

### RF-F1-009 — Consultar eventos e confirmar presença

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-009 |
| **Nome** | Consultar eventos e confirmar presença |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-009 |
| **Rastreio UC** | UC-PRE-03 |
| **Tela** | F1.17 `/eventos` · F1.18 `/eventos/:id/presenca` |
| **API** | `GET /events?audience=me`, `GET /events/{id}/attendance/session`, `POST /events/{id}/attendance/confirm` |
| **Legado** | — (novo) |

**Descrição:** O sistema deve permitir que o aluno consulte eventos formativos elegíveis e confirme presença dentro das janelas configuradas, suportando modos SECRET/QR × SINGLE/DUAL conforme presença v4.1 — sem geofence, trust score ou aula regular.

**Pré-condições:**
- Aluno autenticado com `attendance.view_open` / `attendance.check_in`.
- Evento em andamento com janela de validação ativa (para confirmação).

**Pós-condições:**
- `attendance_session` atualizada; presença completa dispara certificado/formativa via Outbox.

**Critérios de aceitação:**
1. Lista: título, período, estado (Agendado/Em andamento/Concluído), organizador, CH, situação presença (RN-F1.17-01, CA-01).
2. `AttendanceWidget` no modal somente se `_links.confirmar-presenca` existir (RN-F1.17-02, CA-02, CA-03).
3. Widget adapta-se a `attendanceMode`: SECRET_SINGLE, SECRET_DUAL, QR_SINGLE, QR_DUAL (RN-F1.17-03, RN-F1.18-01 a RN-F1.18-04).
4. Fora da janela: HTTP 403; UI cega sem revelar política (RN-F1.18-05, CA-03, CA-06).
5. `deviceUuid` enviado; binding `UNIQUE (id_evento, device_uuid)` quando política ativa (RN-F1.18-06).
6. Countdown sincronizado com servidor; bloqueio automático ao zerar (RN-F1.18-07, CA-06).
7. Presença completa → Outbox `presenca.confirmed` → certificado/formativa (RN-F1.18-08).
8. Não logado: redirect login com retorno à URL original (RN-F1.18-09, CA-07).
9. Modos duplos: perda da primeira fase torna inelegível para segunda (RN-F1.18-02, RN-F1.18-04).
10. **Fora de escopo explícito:** geofence, trust score, aula regular SIGA (HU §5).

**Regras de negócio relacionadas:** RN-F1.17-01 a RN-F1.17-03, RN-F1.18-01 a RN-F1.18-09

**Dependências:** RF-TR-008, RF-F1-006, RF-F1-010, `endpoints_canonicos_presenca_eventos_v4.md`, RNF-UX-04

---

### RF-F1-010 — Visualizar e baixar certificados emitidos

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-010 |
| **Nome** | Visualizar e baixar certificados emitidos |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-010 |
| **Rastreio UC** | UC-CRT-01 |
| **Tela** | F1.19 `/certificados` |
| **API** | `GET /certificates?beneficiario=me` |
| **Legado** | — (substitui upload manual) |

**Descrição:** O sistema deve listar certificados emitidos automaticamente pelo sistema (nunca upload externo) e permitir download do PDF assinado com QR de verificação pública.

**Pré-condições:**
- Aluno autenticado com `certificate.view_own`.
- Certificado previamente emitido por formativa aprovada ou evento concluído.

**Pós-condições:**
- PDF baixado via URL pré-assinada; verificação pública disponível em RF-F0-007.

**Critérios de aceitação:**
1. Lista: tipo, evento/atividade, data emissão, botão download; filtros por tipo/ano (RN-F1.19-02, CA-01).
2. Certificados emitidos automaticamente — aluno nunca solicita manualmente (RN-F1.19-01).
3. Download via `_links.download` e URL pré-assinada MinIO (15 min) (RN-F1.19-03, CA-02).
4. PDF contém QR para `/publico/verificar-certificado/:hash` (RN-F1.19-04, RF-F0-007).
5. Hash SHA-256 e assinatura ED25519 imutáveis após emissão (RN-F1.19-05).
6. Novo certificado incrementa KPI no dashboard e dispara notificação (CA-03, RF-F1-001).

**Regras de negócio relacionadas:** RN-F1.19-01 a RN-F1.19-05

**Dependências:** RF-TR-003, RF-F0-007, RNF-LGL-02, RNF-POR-03

---

### RF-F1-011 — Consultar atendimentos e dar ciência

| Campo | Valor |
|-------|-------|
| **ID** | RF-F1-011 |
| **Nome** | Consultar atendimentos e dar ciência |
| **Prioridade** | P2 |
| **Ator(es)** | A2 Aluno |
| **Módulo** | F1 — Aluno |
| **Rastreio HU** | US-F1-011 |
| **Rastreio UC** | UC-ATD-02 |
| **Tela** | F1.20 `/meus-atendimentos` |
| **API** | `GET /service-records?aluno=me`, `POST /service-records/:id/acknowledge` |
| **Legado** | T134/T135 (RF42 parcial) |

**Descrição:** O sistema deve permitir que o aluno visualize atendimentos registrados pela secretaria e confirme ciência dos pendentes, com registro auditável.

**Pré-condições:**
- Aluno autenticado com `service_record.view_own`.
- Atendimento registrado pela secretaria (F5).

**Pós-condições:**
- Estado `CIENCIA_DADA`; `ciencia_em` registrado; pendência removida do dashboard.

**Critérios de aceitação:**
1. Lista: data, assunto, status, ação; badge "Pendente ciência" em warning (RN-F1.20-02, CA-01).
2. Botão "Estou ciente" quando `_links.acknowledge` existir (CA-01).
3. `POST /service-records/:id/acknowledge` → estado `CIENCIA_DADA`; evento `atendimentos.acknowledged` com data/IP (RN-F1.20-03, CA-02).
4. Criação de atendimento pela secretaria dispara notificação via Outbox (RN-F1.20-04).
5. Pendências aparecem no dashboard (RN-F1.20-05, RF-F1-001).
6. Filtro por pendências com empty state adequado (CA-03).
7. Aluno não cria atendimentos — somente consulta e dá ciência.

**Regras de negócio relacionadas:** RN-F1.20-01 a RN-F1.20-05

**Dependências:** RF-F1-001, RF-TR-002, RF-TR-007, RNF-UX-04

---

*Última atualização: 2026-08-09 — RF-F1-003-d adicionado (exportação de dados pessoais LGPD — lacuna funcional identificada na revisão de classificação RNF/RF)*
