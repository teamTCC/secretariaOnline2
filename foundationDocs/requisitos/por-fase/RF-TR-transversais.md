# Requisitos Funcionais — Transversais (RF-TR)

**Projeto:** SecretariaOnline2 — UFPR SEPT  
**Versão:** 1.0  
**Data:** 2026-06-23  
**Gerado a partir de:** `fluxos_por_perfil.md` §10; `analise_arquitetural_secretariaonline2.md` §6–11, §14 (ADR-002/003); `mvp_v2_solicitacoes_workflow_engine.md`; `endpoints_canonicos_presenca_eventos_v4.md`; US-F7-003, US-F7-004, US-F7-005, US-F7-006; US-F0-007, US-F1-001, US-F1-010; `.cursorrules`  
**Total RF neste arquivo:** 8

> **Princípio DRY:** capacidades que aparecem em múltiplas HUs/fases são consolidadas aqui. Os RFs por fase (`RF-Fx-NNN`) descrevem o comportamento **do ator na tela**; os RF-TR descrevem o **motor compartilhado** que esses RFs consomem.

---

## Resumo transversal

| RF | Nome | Módulo(s) | Prioridade | HU(s) âncora | ADR |
|----|------|-----------|:----------:|--------------|-----|
| RF-TR-001 | Motor genérico de solicitações | `solicitacoes/` | P0 | US-F7-003 | ADR-003 |
| RF-TR-002 | Hub de comunicação + Outbox | `comunicacao/`, `notificacoes/` | P1 | US-F7-005 | — |
| RF-TR-003 | Certificados anti-fraude | `arquivos/`, presença, formativas | P1 | US-F0-007, US-F1-010 | — |
| RF-TR-004 | Auditoria imutável | `auditoria/` | P0 | US-F7-006 | — |
| RF-TR-005 | FGAC + HATEOAS `_links` | `iam/`, todos os módulos API | P0 | — | ADR-002 |
| RF-TR-006 | BFF dashboard contextual | `bff/` | P0 | US-F1-001, US-F3-001, US-F5-001 | — |
| RF-TR-007 | Notificações push + e-mail | `comunicacao/`, `notificacoes/` | P1 | US-F7-004 | — |
| RF-TR-008 | Presença em eventos v4.1 | `presenca/` | P1 | US-F1-009, US-F3-002, US-F5-008 | — |

---

## Mapa de consumo (RF por fase → RF-TR)

| RF-TR | Principais consumidores |
|-------|-------------------------|
| RF-TR-001 | RF-F1-005-a/b/c, RF-F3-003-a/b, RF-F5-002-a/b/c, RF-F7-003, RF-F8-002 |
| RF-TR-002 | RF-F0-002, RF-F1-004/005/011, RF-F3-007, RF-F5-007, RF-F7-005, RF-F8-002 |
| RF-TR-003 | RF-F0-007, RF-F1-006/010, RF-F2-001, RF-F3-002/004, RF-F4-001, RF-F5-008 |
| RF-TR-004 | RF-F7-001–006, RF-F5-003–012, RF-F3-003-b, RF-F6-001 |
| RF-TR-005 | Todos os RF com ações condicionais (F0–F8) |
| RF-TR-006 | RF-F1-001, RF-F3-001, RF-F5-001, RF-F6-002 |
| RF-TR-007 | RF-F1-003-c/004, RF-F3-007, RF-F7-004 |
| RF-TR-008 | RF-F1-009, RF-F3-002-a/b, RF-F5-008-a/b |

---

### RF-TR-001 — Motor genérico de solicitações (RequestType)

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-001 |
| **Nome** | Motor genérico de solicitações (RequestType) |
| **Prioridade** | P0 |
| **Ator(es)** | Todos (abertura, acompanhamento, deliberação conforme workflow) |
| **Módulo** | `solicitacoes/` (domain + application) |
| **Rastreio HU** | US-F7-003 (config); US-F1-005, US-F3-003, US-F5-002 (uso) |
| **Rastreio UC** | UC-SOL-01 a UC-SOL-07 (conforme tipo) |
| **Tela** | F1.7–F1.9, F3.3–F3.4, F5.2–F5.4, F7.4 |
| **API** | `/request-types`, `/requests`, `/requests/{id}/transitions`, anexos |
| **Legado** | 19× `novaSol*` / `consultarSol*` → 1 engine |

**Descrição:** O sistema deve executar o ciclo de vida de **todos os tipos de solicitação** (meta: 19 tipos) a partir de configuração `RequestType` (`form_schema` JSON Schema + `workflow_json` state machine), sem duplicar código por tipo — núcleo ADR-003.

**Pré-condições:** `RequestType` publicado (`PUBLISHED`); solicitante/deliberador com authority exigida pela transição.

**Pós-condições:** Estado atualizado; `request_event` append-only; `outbox_event` enfileirado quando workflow define notificação.

**Critérios de aceitação:**

*Modelo e configuração*
1. Entidades: `RequestType`, `Request`, `RequestEvent`, `RequestAttachment` (UUIDv7, `dados` JSONB, `numero` protocolo único).
2. Admin configura via RF-F7-003; tipos `DRAFT` ocultos do wizard; versão imutável por publicação — solicitações abertas mantêm versão original.
3. Seed MVP: ≥ 2–3 tipos reais (`ADIANTAMENTO_PERIODO`, `APROVEITAMENTO_DISCIPLINA`, `TRANCAMENTO_DISCIPLINA`).

*Engine de workflow*
4. `WorkflowEngine` pura (sem dependência de Spring/JPA — conforme RNF-MAN-05 §Clean Architecture): valida `from → to`, authority, guards simples; rejeita transição inválida com RFC 7807.
5. Toda transição gera `RequestEvent` (`CREATED`, `TRANSITION`, `COMMENT`) — histórico imutável.
6. Payload validado contra `form_schema` no backend (Konform/Jakarta) além do Zod no front.

*API e UI genéricas*
7. Três rotas front: `/solicitacoes`, `/solicitacoes/nova` (wizard 3 passos + `DynamicForm`), `/solicitacoes/:id` (timeline + `useActions`).
8. Anexos: URL pré-assinada MinIO/S3 → PUT → `confirm` com SHA-256.
9. Deliberação: transições expõem `_links` condicionais (`deliberar`, `solicitar-ajuste`, etc.).

*Escopo MVP v2*
10. Aluno: abrir, listar próprias, detalhe, reenviar após ajuste.
11. Deliberação UI dedicada (secretaria/professor) pode ser pós-MVP; backend suporta transições via API.

**Regras de negócio relacionadas:** RN-F7-003-*; RN-F1.5-*; fluxos §10.2

**Dependências:** RF-F7-003, RF-TR-002, RF-TR-004, RF-TR-005, RNF-MAN-01, RNF-CON-01

---

### RF-TR-002 — Hub de comunicação + entrega assíncrona (Outbox)

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-002 |
| **Nome** | Hub de comunicação + entrega assíncrona (Outbox) |
| **Prioridade** | P1 |
| **Ator(es)** | Autores (secretaria, coordenação, professor); destinatários (todos logados) |
| **Módulo** | `comunicacao/`, `notificacoes/` |
| **Rastreio HU** | US-F7-005 (observabilidade); US-F1-004, US-F3-007 |
| **Rastreio UC** | UC-COM-01, UC-COM-02 |
| **Tela** | F1.6, F3.8, F7.6 |
| **API** | `/communications`, `outbox_event`, dispatcher `@Scheduled` |
| **Legado** | `mensagem` → Hub |

**Descrição:** O sistema deve publicar comunicações institucionais e enfileirar eventos de domínio na tabela `outbox_event` para entrega assíncrona confiável (in-app, push, e-mail) sem RabbitMQ no MVP.

**Critérios de aceitação:**

*Hub de comunicação*
1. Tipos: Notícia, Aviso institucional, Comunicado de professor, Oportunidade (analise §7.1).
2. Publicação grava `communication` + `outbox_event` na mesma transação do comando de negócio.
3. `communication_delivery` registra `sent_at`, `read_at`, tentativas, `last_error`.

*Outbox dispatcher*
4. `@Scheduled(fixedDelay=5000)`: processa até 50 eventos `PENDING` com `SELECT FOR UPDATE SKIP LOCKED`.
5. Retry com backoff exponencial; após 5 tentativas → `DEAD` (reentrega manual via RF-F7-005).
6. Latência PENDING→SENT conforme RNF-DES-05 (< 30 s em fila vazia); ver também RNF-CON-01 para garantia de atomicidade.
7. Eventos `SENT` retidos 7 dias antes de arquivamento.

*Integração*
8. Use cases de deliberação, colação, atendimento enfileiram eventos tipados (`solicitacoes.deliberated`, etc.).
9. Templates referenciados por nome no `workflow_json` (RF-F7-004).

**Dependências:** RF-TR-007, RF-F7-005, RNF-CON-01, RNF-DES-05

---

### RF-TR-003 — Emissão e verificação de certificados anti-fraude

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-003 |
| **Nome** | Emissão e verificação de certificados anti-fraude |
| **Prioridade** | P1 |
| **Ator(es)** | Sistema (emissor); A1 Visitante (verificador público); A2 Aluno (download) |
| **Módulo** | certificados (cross-module) |
| **Rastreio HU** | US-F0-007, US-F1-010 |
| **Rastreio UC** | UC-CRT-01, UC-CRT-03 |
| **Tela** | F0.7 `/publico/verificar-certificado/:hash`, F1.19 `/certificados` |
| **API** | `CertificateIssuerUseCase`; `GET /publico/verificar-certificado/{hash}`; `/.well-known/jwks.json` |
| **Legado** | Upload manual de PDF — **eliminado** |

**Descrição:** O sistema deve emitir certificados **somente** a partir de eventos internos validados (presença completa ou formativa aprovada), gerar PDF canônico com hash SHA-256 assinado ED25519 e QR de verificação pública — **nunca** aceitar upload externo.

**Critérios de aceitação:**

*Gatilhos de emissão*
1. `attendance_session` com presença completa + evento `CONCLUÍDO` → `CertificateIssuerUseCase`.
2. `formative_entry` aprovada pela CAAF → mesma pipeline.
3. Certificado externo de outra instituição → fluxo de **aproveitamento** (RequestType), não upload.

*Pipeline de emissão*
4. Template Markdown → HTML → PDF (Gotenberg/Playwright); metadados fixos; PDF/A quando possível.
5. `SHA-256` dos bytes do PDF persistido em `certificate`.
6. Assinatura ED25519 do digest; chave privada em env/Vault; chave pública em `/.well-known/jwks.json`.
7. QR embutido apontando para `/publico/verificar-certificado/{hash}`.
8. PDF armazenado em MinIO/S3; registro referencia `event_attendance.id` ou `formative_entry.id`.

*Verificação pública (RF-F0-007)*
9. Visitante informa hash/ID → sistema recalcula hash + valida assinatura → exibe Válido/Adulterado sem autenticação.
10. Resposta não expõe PII além do necessário para verificação.

*Regras negócio*
11. Reemissão gera nova versão com auditoria; hash anterior permanece verificável.
12. Secretaria registra horas por referência `certificate_id` — sem conferência manual de PDF.

**Dependências:** RF-TR-008, RF-TR-004, RF-F0-007, RNF-LGL-02, RNF-SEC-08

---

### RF-TR-004 — Auditoria imutável de comandos

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-004 |
| **Nome** | Auditoria imutável de comandos |
| **Prioridade** | P0 |
| **Ator(es)** | Sistema (escrita); A9 Admin (leitura via RF-F7-006) |
| **Módulo** | `auditoria/` |
| **Rastreio HU** | US-F7-006 |
| **Rastreio UC** | UC-ADM-05 |
| **Tela** | F7.7 `/admin/audit-log` |
| **API** | `audit_log` (INSERT only); `GET /audit-log` |
| **Legado** | — |

**Descrição:** O sistema deve registrar **toda mutação de estado** em `audit_log` append-only, com ator, ação, entidade alvo, payload antes/depois, IP e timestamp — sem endpoints DELETE/PATCH.

**Critérios de aceitação:**
1. Use cases emitem `AuditEvent` na mesma transação da mutação de negócio.
2. Campos: `id_ator`, `acao` (enum estável), `alvo_tipo`, `alvo_id`, `payload_antes`/`payload_depois` JSONB, `ip`, `created_at`.
3. Índices: `(id_ator, created_at)`, `(alvo_tipo, alvo_id)`.
4. Retenção **5 anos**; busca admin com diff JSON (RF-F7-006).
5. Cobertura mínima: IAM, solicitações, deliberações, diplomas, import/export, workflow publish, templates.
6. Falha de auditoria **aborta** a transação (fail-closed).

**Dependências:** RF-F7-006, RNF-LGL-01, RNF-SEC-06

---

### RF-TR-005 — Autorização FGAC + UI orientada a HATEOAS `_links`

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-005 |
| **Nome** | Autorização FGAC + UI orientada a HATEOAS `_links` |
| **Prioridade** | P0 |
| **Ator(es)** | Todos |
| **Módulo** | `iam/` + assemblers em cada módulo API |
| **Rastreio HU** | — (transversal; ADR-002) |
| **Rastreio UC** | — |
| **Tela** | Todas as telas autenticadas |
| **API** | `_links` em respostas HAL/JSON; `@PreAuthorize` |
| **Legado** | Matriz 10 perfis hardcoded — **substituída** |

**Descrição:** O sistema deve autorizar por **capabilities** granulares (authorities dot-notation) e expor ações disponíveis via `_links` HATEOAS — a UI é **cega a perfis** e renderiza botões somente quando o link existe.

**Critérios de aceitação:**

*Backend*
1. JWT contém authorities derivadas de roles×matriz FGAC (RF-F7-002-b).
2. `@PreAuthorize("hasAuthority('request.deliberate')")` em endpoints mutáveis.
3. Assemblers adicionam `_links` condicionais por capability + estado do recurso (ex.: `retry`, `deliberar`, `reset-password`).
4. HTTP 403 retorna RFC 7807 sem vazar existência de recurso quando política exigir.

*Frontend*
5. Hook `useActions(resource)` lê `_links` e filtra ações disponíveis.
6. Proibido hardcode `if (role === 'SECRETARIA')` para exibir botões.
7. Busca global (RF-F8-001) filtra índices por capability — mesmo princípio.

*Invalidação*
8. Alteração de roles/authorities invalida cache de capabilities do usuário (RF-F7-002).

**Dependências:** RF-F7-002-a/b, RNF-SEC-02, RNF-MAN-02

---

### RF-TR-006 — BFF de dashboard contextual por perfil

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-006 |
| **Nome** | BFF de dashboard contextual por perfil |
| **Prioridade** | P0 |
| **Ator(es)** | A2 Aluno, A4 Professor, A7 Secretaria, A8 Coordenador (extensível) |
| **Módulo** | `bff/` |
| **Rastreio HU** | US-F1-001, US-F3-001, US-F5-001 |
| **Rastreio UC** | UC-DASH-01 |
| **Tela** | F1.1, F3.1, F5.1 `/inicio` |
| **API** | `GET /bff/dashboard/{perfil}` (`aluno`, `professor`, `secretaria`, …) |
| **Legado** | — |

**Descrição:** O sistema deve agregar em **uma chamada HTTP** os KPIs, pendências, listas recentes e `_links` de atalhos do dashboard conforme o perfil ativo, com degradação graciosa se um submódulo falhar.

**Critérios de aceitação:**
1. Endpoints dedicados por perfil principal; contrato estável documentado em OpenAPI.
2. Agregação paralela interna; falha parcial retorna blocos disponíveis + indicador de erro no bloco falho (RN-F1.1-01).
3. KPIs, pendências (máx. 3), últimas solicitações (5), próximos eventos (3) conforme HU do perfil.
4. QuickTiles e CTAs derivados exclusivamente de `_links` do BFF (RN-F1.1-07 a RN-F1.1-09).
5. Cache Redis 30 s para dados não real-time (RN-F1.1-10); invalidação em mutações relevantes.
6. FCP conforme RNF-DES-04 (< 1,5 s); mobile pull-to-refresh revalida TanStack Query.
7. MVP v2: cards de solicitações leem tabelas reais de `solicitacoes/` (não mock).

**Dependências:** RF-TR-001, RF-TR-005, RF-TR-008, RNF-DES-04, RNF-UX-04

---

### RF-TR-007 — Notificações push + e-mail com fallback

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-007 |
| **Nome** | Notificações push + e-mail com fallback |
| **Prioridade** | P1 |
| **Ator(es)** | Sistema (dispatcher); destinatários |
| **Módulo** | `comunicacao/`, `notificacoes/` |
| **Rastreio HU** | US-F7-004 (templates); US-F1-003-c (preferências) |
| **Rastreio UC** | UC-AUT-06, UC-COM-03 |
| **Tela** | F1.5 `/perfil/notificacoes` |
| **API** | FCM adapter; Mailgun/SendGrid + SMTP UFPR fallback |
| **Legado** | E-mail SMTP porta 25 legado — **corrigido** |

**Descrição:** O sistema deve entregar notificações por push (FCM/Expo) e e-mail com política de prioridade, agregação (digest), Do Not Disturb e fallback SMTP institucional.

**Critérios de aceitação:**

*Canais e provedores*
1. Push via Firebase Cloud Messaging (`expo-notifications` no mobile).
2. E-mail primário: Mailgun ou SendGrid; fallback SMTP UFPR com STARTTLS.
3. SPF/DKIM/DMARC configurados; webhooks de bounce → `email_valido=false`.

*Política de entrega (analise §7.3)*
4. Crítica (4): push + e-mail imediato; Alta (3): push + digest diário; Média (2): in-app + push se online; Baixa (1): in-app only.
5. `notification_preference`: DND 22h–7h bloqueia push; digest diário/semanal configurável (RF-F1-003-c).

*Templates*
6. Renderização Markdown + placeholders de RF-F7-004; versão `CURRENT` usada no dispatch.
7. Workflow referencia template por nome em transição (`notificacoes.DELIBERADA`).

*Confiabilidade*
8. Mesma transação Outbox de RF-TR-002; entrega rastreada em `communication_delivery`.
9. Preferências do usuário respeitadas antes do envio.

**Dependências:** RF-TR-002, RF-F7-004, RF-F1-003-c, RNF-CON-01, RNF-SEC-09

---

### RF-TR-008 — Presença em eventos formativos v4.1 (modos configuráveis)

| Campo | Valor |
|-------|-------|
| **ID** | RF-TR-008 |
| **Nome** | Presença em eventos formativos v4.1 (modos configuráveis) |
| **Prioridade** | P1 |
| **Ator(es)** | A2 Aluno (confirmar); A4 Professor / A7 Secretaria (gerir/host) |
| **Módulo** | `presenca/` |
| **Rastreio HU** | US-F1-009, US-F3-002, US-F5-008 |
| **Rastreio UC** | UC-PRE-01 a UC-PRE-04 |
| **Tela** | F1.17–F1.18, F3.2, F5.14–F5.15 |
| **API** | `endpoints_canonicos_presenca_eventos_v4.md` |
| **Legado** | — |

**Descrição:** O sistema deve suportar confirmação de presença em **eventos formativos internos** com modos `QR_SINGLE`, `QR_DUAL`, `SECRET_SINGLE`, `SECRET_DUAL`, janelas temporais configuráveis e device binding — **fora do escopo**: chamada de aula regular (SIGA).

**Critérios de aceitação:**

*Modos e estados*
1. Enum `attendanceMode` canônico; estados evento: `AGENDADO` → `EM_ANDAMENTO` → `CONCLUÍDO`.
2. `ch_creditadas` pode divergir da duração civil do evento.
3. Fora da janela ativa: HTTP 403; `_links` ausentes na UI.

*Permissões*
4. `event.manage`: CRUD e listagens (professor, secretaria, admin).
5. `event.host`: operação ao vivo (abrir janelas, exibir QR/PIN).
6. Mutação de evento de terceiro: somente organizador (ou política institucional).

*Endpoints canônicos*
7. CRUD `/events`; sessão aluno `GET .../attendance/session` + HATEOAS.
8. Janelas: `POST .../attendance/windows/entry|exit`.
9. Confirmação: `POST .../attendance/entry|exit` (PIN) ou `.../qr/validate` (QR).
10. Encerramento: `POST .../events/{id}/close` → certificados via RF-TR-003.

*Anti-fraude*
11. `deviceUuid` obrigatório quando política exige; `UNIQUE (id_evento, device_uuid)`.
12. Token QR: TTL curto + uso único ou assinatura server-side.
13. **Sem** geofence, trust score ou BLE.

*Observabilidade*
14. Métricas: `presenca.confirm.duration`, `403.window_violation`, taxa conclusão por modo.

**Regras de negócio relacionadas:** RN-F1.9-*, RN-F3.2-*, RN-F5.008-*; analise §10

**Dependências:** RF-TR-003, RF-TR-005, RF-F1-009, RF-F3-002-a/b, RF-F5-008-a/b, RNF-DES-03

---

## Fora de escopo (transversais)

- RabbitMQ/Kafka no MVP (Outbox suficiente; ver analise §9)
- API Gateway Node/Fastify
- Geofence / trust score em presença
- Upload manual de certificados
- ICP-Brasil (evolução pós-MVP)
- Chat ao vivo de suporte

---

## Rastreio ADR

| ADR | RF-TR |
|-----|-------|
| ADR-002 FGAC + HATEOAS | RF-TR-005 |
| ADR-003 Workflow Engine DRY | RF-TR-001 |
| Outbox (decisão arquitetural §9) | RF-TR-002, RF-TR-007 |

---

*Última atualização: 2026-08-09 — Revisão de classificação: RF-TR-001 CA-4 (restrição arquitetural referenciada via RNF-MAN-05); RF-TR-002 CA-6 (métrica de latência referenciada via RNF-DES-05); RF-TR-006 CA-6 (métrica FCP referenciada via RNF-DES-04)*
