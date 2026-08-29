# Inventário e Decisões — Banco SO2

**Projeto:** SecretariaOnline2 (TCC — UFPR SEPT)  
**Etapa:** 0 — Inventário, reconciliação e decisões  
**Data (histórico I1–I11):** 2026-06-22  
**As-built Flyway:** 2026-08-29 — `foundationDocs/analysis/as-built-backend.md` §5  
**Modelo utilizado:** Claude Opus 4.6 (thinking high)  
**Fontes consultadas:** §0.1 do `PROMPT_gerar_documentacao_banco_dados.md` (10 fontes em ordem de precedência)

> **Override físico:** as decisões I1–I11 abaixo permanecem como histórico da trilha acadêmica 2026-06 (`schema_completo.sql`). O **schema físico em execução** é Flyway V001–V019. Onde I11 / §1.1 divergem do Flyway, **o as-built vence**.

---

## 0. As-built Flyway 2026-08

Fonte: `backend/app/src/main/resources/db/migration/` (V001–V019). Próxima migration = **V020**. Nunca editar V001–V019. Hibernate `ddl-auto: validate`; Flyway ligado em `dev`.

### 0.1 Contagem

| Trilha | Tabelas | Notas |
|--------|:-------:|-------|
| Modelo acadêmico 2026-06 (§1 abaixo) | **31** | Inclui `request_line_item` **não migrada** |
| Flyway V002–V019 | **~45** | Sem `request_line_item`; com `request_type_version` + V012–V015 |

### 0.2 `request_line_item` — NÃO existe no Flyway

A tabela **não foi criada** em V004 (nem depois). Decisões por disciplina (aproveitamento, inclusão, exclusão) ficam em **`request.dados` JSONB**, não em linhas relacionais. Não há `RequestLineItemJpaRepository`.

No inventário §1.1, a linha 13 (`request_line_item`) é **somente trilha 2026-06**.

### 0.3 `request_type_version` — domínio as-built (V019)

| # | Tabela | Módulo | PK | FKs principais |
|---|--------|--------|-----|----------------|
| A1 | `request_type_version` | Solicitações | `id` UUIDv7 | `id_request_type` → `request_type.id`; UNIQUE (`id_request_type`, `version`) |

Colunas: `version` INT, `form_schema` JSONB, `workflow_json` JSONB, `published_at`, `created_at`, `updated_at`.

`request.id_request_type_version` UUID NULL → `request_type_version.id` (índice `idx_request_type_version_fk`). Publish grava snapshot; GET detalhe usa o `form_schema` da versão da instância.

### 0.4 Tabelas extras vs §1.1 (V012, V014, V015, V019)

Não inventar outras — só o que o Flyway cria.

**V012** — service / FAQ / suporte / FCM:

| Tabela | FKs principais |
|--------|----------------|
| `service_record` | `id_secretario` → `usuario` (nullable após V015), `id_aluno` → `usuario` |
| `faq_item` | — |
| `support_ticket` | `id_usuario` → `usuario`, `id_atendente` → `usuario` |
| `device_fcm_token` | `id_usuario` → `usuario`; UNIQUE (`id_usuario`, `fcm_token`) |

**V014** — colação, kanban, import/export, templates, log de notificação:

| Tabela | FKs principais |
|--------|----------------|
| `graduation_record` | `id_aluno` → `usuario`, `id_curso` → `curso`, `delivered_by` → `usuario`; V015: `id_periodo` → `periodo_letivo` |
| `secretary_task` | `id_assignee` → `usuario` |
| `import_job` | `id_ator` → `usuario` |
| `communication_template` | — (`codigo` UNIQUE) |
| `communication_template_revision` | `id_template` → `communication_template`, `id_autor` → `usuario` |
| `notification_log` | `id_usuario` (sem FK obrigatória) |
| `export_job` | `id_ator` → `usuario` |

**V015** — histórico e contato:

| Tabela | FKs principais |
|--------|----------------|
| `historico_escolar` | `id_aluno` → `usuario`, `id_disciplina` → `disciplina`; UNIQUE (`id_aluno`, `id_disciplina`) |
| `contact_message` | — (público) |

**V019:** `request_type_version` (§0.3).

**Também no Flyway e não no §1.2 como tabela:** `password_history` (V002) — tabela própria, não JSONB em `usuario`.

### 0.5 Overlay de colunas em solicitações (V004 + V018 + V019)

Diferenças vs §1.1 / I11 (detalhe em `as-built-backend.md` §5.3):

- `request_type`: `code VARCHAR(60)`, `prazo_dias` default **10**, **sem** `interna` / `required_auth` (authorities no `workflow_json`); `created_at` / `updated_at`.
- `request`: `request_type_code`, `parecer`, `deleted_at`, `prazo_em` nullable, UNIQUE (`numero_anual`, `ano`, `id_curso`), `id_request_type_version`.
- `request_event`: `created_at` + `updated_at` (V018); `estado_anterior` **NOT NULL**; sem coluna `at` / `metadata`.
- `request_attachment`: `content_type` (não `mime_type`); **sem** `uploaded_by` / **sem** `status` (I11-a não materializada); `created_at` + `updated_at` (V018).

---

## 1. Tabelas

### 1.1 Tabelas de domínio (29) — trilha 2026-06

Inventário original. **Override físico:** §0. `request_line_item` (linha 13) não está no Flyway; `request_type_version` (13b) está (V019).

| # | Tabela | Módulo | PK | FKs principais | PK composta |
|---|--------|--------|-----|----------------|:-----------:|
| 1 | `usuario` | IAM | `id` UUIDv7 | `id_curso` → `curso` | — |
| 2 | `role` | IAM | `id` UUIDv7 | — | — |
| 3 | `authority` | IAM | `id` UUIDv7 | — | — |
| 4 | `role_authority` | IAM | (`id_role`, `id_authority`) | `id_role` → `role`, `id_authority` → `authority` | ✓ |
| 5 | `usuario_role` | IAM | (`id_usuario`, `id_role`) | `id_usuario` → `usuario`, `id_role` → `role` | ✓ |
| 6 | `curso` | Acadêmico | `id` UUIDv7 | `id_coordenador` → `usuario` (deferrable) | — |
| 7 | `disciplina` | Acadêmico | `id` UUIDv7 | `id_curso` → `curso` | — |
| 8 | `periodo_letivo` | Acadêmico | `id` UUIDv7 | — | — |
| 9 | `calendario_academico` | Acadêmico | `id` UUIDv7 | `id_periodo` → `periodo_letivo`, `id_request_type` → `request_type` | — |
| 10 | `request_type` | Solicitações | `id` UUIDv7 | — | — |
| 11 | `request` | Solicitações | `id` UUIDv7 | `id_solicitante` → `usuario`, `id_request_type` → `request_type`, `id_curso` → `curso`; **as-built:** `id_request_type_version` → `request_type_version` | — |
| 12 | `request_event` | Solicitações | `id` UUIDv7 | `id_request` → `request`, `id_ator` → `usuario` | — |
| 13 | `request_line_item` | Solicitações | — | **NÃO existe no Flyway** (linhas em `request.dados` JSONB). Presente só no modelo 2026-06 / `schema_completo.sql` | — |
| 13b | `request_type_version` | Solicitações | `id` UUIDv7 | `id_request_type` → `request_type` | — |
| 14 | `request_attachment` | Solicitações | `id` UUIDv7 | `id_request` → `request`; **2026-06:** `uploaded_by` → `usuario` (**Flyway V004: sem essa coluna**) | — |
| 15 | `formative_activity` | Formativas | `id` UUIDv7 | `id_curso` → `curso` | — |
| 16 | `formative_entry` | Formativas | `id` UUIDv7 | `id_aluno` → `usuario`, `id_activity` → `formative_activity`, `reviewed_by` → `usuario` | — |
| 17 | `internship` | Estágio | `id` UUIDv7 | `id_aluno` → `usuario`, `id_orientador` → `usuario`, `id_coe` → `usuario` | — |
| 18 | `internship_document` | Estágio | `id` UUIDv7 | `id_internship` → `internship` | — |
| 19 | `tcc` | TCC | `id` UUIDv7 | `id_curso` → `curso` | — |
| 20 | `tcc_member` | TCC | (`id_tcc`, `id_aluno`) | `id_tcc` → `tcc`, `id_aluno` → `usuario` | ✓ |
| 21 | `tcc_examiner` | TCC | (`id_tcc`, `id_professor`) | `id_tcc` → `tcc`, `id_professor` → `usuario` | ✓ |
| 22 | `communication` | Comunicação | `id` UUIDv7 | `id_curso_alvo` → `curso`, `id_autor` → `usuario` | — |
| 23 | `communication_delivery` | Comunicação | `id` UUIDv7 | `id_comm` → `communication`, `id_destinatario` → `usuario` | — |
| 24 | `notification_preference` | Comunicação | `id_usuario` (UUID FK) | `id_usuario` → `usuario` (PK = FK) | — |
| 25 | `outbox_event` | Comunicação/Outbox | `id` UUIDv7 | — (aggregate_id referencia qualquer tabela) | — |
| 26 | `event_attendance` | Presença v4.1 | `id` UUIDv7 | `id_curso` → `curso`, `organizador` → `usuario` | — |
| 27 | `attendance_session` | Presença v4.1 | `id` UUIDv7 | `id_evento` → `event_attendance`, `id_aluno` → `usuario` | — |
| 28 | `certificate` | Certificados | `id` UUIDv7 | `id_beneficiario` → `usuario`, `id_evento` → `event_attendance`, `id_formativa` → `formative_entry` | — |
| 29 | `audit_log` | Auditoria | `id` UUIDv7 | `id_ator` → `usuario` (nullable) | — |

### 1.2 Tabelas técnicas (obrigatórias no SQL)

| # | Tabela | Módulo | PK | Colunas-chave | Fonte |
|---|--------|--------|-----|---------------|-------|
| T1 | `refresh_token` | IAM/Sessão | `id` UUIDv7 | `id_usuario` FK → `usuario`, `token_hash`, `expira_em`, `usado_em`, `created_at` | MVP v1 §5.1, Diagrama Classes IAM, seq. F0-001 |
| T2 | `jti_blacklist` | IAM/Segurança | `jti` UUID (PK natural) | `expira_em` TIMESTAMPTZ, `created_at` TIMESTAMPTZ | seq. F0-003, F3-003, HU RN-F0.3-03 |

### 1.3 Tabelas NÃO incluídas (decisão explícita)

| Tabela | Motivo de exclusão |
|--------|-------------------|
| `password_reset_token` | F0-002 gera JWT em memória (não persiste); consumo em F0-003 grava JTI em `jti_blacklist`. Não há tabela de tokens de reset. |
| `attendance_validation_window` | JSONB em `event_attendance.validation_windows` é canônico em §5.3 e endpoints v4.1. Normalização opcional decidida como **NÃO** (manter JSONB). |
| `flyway_schema_history` | Criada automaticamente pelo Flyway runtime; não incluir no script manual. |
| `DELIBERATION` | Não é entidade física — deliberação é representada por `request_event` + `estado` em `request`. |
| `FORM_SCHEMA` | Não é tabela — é coluna JSONB em `request_type.form_schema`. |
| `WORKFLOW_DEFINITION` | Não é tabela — é coluna JSONB em `request_type.workflow_json`. |
| `request_line_item` | **As-built:** não migrada. Modelo 2026-06 a previa; Flyway V004 omite. Payload em `request.dados`. |

### 1.4 Resumo quantitativo

| Categoria | Quantidade |
|-----------|:----------:|
| Tabelas de domínio (trilha 2026-06) | 29 (inclui `request_line_item` não migrada) |
| Tabelas técnicas (no SQL 2026-06) | 2 |
| **Total no `schema_completo.sql`** | **31** |
| **Total Flyway as-built (V002–V019)** | **~45** (sem `request_line_item`; + `request_type_version` + V012–V015 + `password_history`) |
| Tabelas com PK composta | 4 (`role_authority`, `usuario_role`, `tcc_member`, `tcc_examiner`) — Flyway: `usuario_role` tem PK UUID + UNIQUE (`id_usuario`, `id_role`) |

---

## 2. Resolução de inconsistências

> **Nota as-built (2026-08):** I1–I11 abaixo são o registro histórico da trilha 2026-06. O schema físico **não** segue I11 para `request_line_item`, `request_attachment.status` / `mime_type` / `uploaded_by`, nem `request_type.interna` / `required_auth`. Ver §0.

### I1 — ER §5.2 vs DDL §5.3: `ATTENDANCE_CHECKIN` vs `attendance_session`

| Aspecto | Decisão |
|---------|---------|
| **Nome físico (DDL)** | `attendance_session` — conforme §5.3 e endpoints canônicos v4.1 |
| **Nome conceitual (ER Mermaid)** | Renomear de `ATTENDANCE_CHECKIN` para `ATTENDANCE_SESSION` (ou label "Sessão de Presença") |
| **Justificativa** | O DDL §5.3 é a fonte de maior precedência (nível 1); o Mermaid §5.2 usava nome provisório. endpoints_canonicos v4.1 confirmam `attendance_session`. |
| **Status** | ✅ DECISÃO FINAL |

### I2 — ER §5.2: `DELIBERATION` como entidade

| Aspecto | Decisão |
|---------|---------|
| **Criar tabela `deliberation`?** | **NÃO** |
| **Representação** | A deliberação é um `request_event` com `tipo=DEFERRED|DENIED|REQUEST_ADJUSTMENT` + campo `parecer`. O `request.estado` reflete o estado atualizado. |
| **Justificativa** | workflow-engine-specialist §"Core Domain Model": deliberação é transição + evento. Não há entidade separada. |
| **Status** | ✅ DECISÃO FINAL |

### I3 — ER §5.2: `FORM_SCHEMA` e `WORKFLOW_DEFINITION` separados

| Aspecto | Decisão |
|---------|---------|
| **Criar tabelas separadas?** | **NÃO** |
| **Representação** | Colunas JSONB em `request_type`: `form_schema` (JSON Schema Draft-07) e `workflow_json` (state machine JSON) |
| **Justificativa** | ADR-003 (DRY aggressive): configuração é dado, não código. workflow-engine-specialist confirma: "Adding a new request type = inserting 1 JSON row." |
| **Status** | ✅ DECISÃO FINAL |

### I4 — Diagrama Classes: `AttendanceCheckin` + `DeviceUuid` vs `attendance_session.device_uuid`

| Aspecto | Decisão |
|---------|---------|
| **Tabela física** | Uma única tabela `attendance_session` |
| **`device_uuid`** | É uma **coluna** (`UUID NOT NULL`) em `attendance_session`, não uma entidade separada |
| **`DeviceUuid` (diagrama classes)** | Value Object no domínio Kotlin (wrapper de UUID) — não se materializa como tabela |
| **Justificativa** | §5.3 DDL + endpoints v4.1 §7.1 (`deviceUuid` como chave no corpo JSON). UNIQUE (id_evento, device_uuid) garante device binding. |
| **Status** | ✅ DECISÃO FINAL |

### I5 — `jpaInterfaces` vs MVP v1: `refresh_token` ausente na lista principal

| Aspecto | Decisão |
|---------|---------|
| **Incluir no SQL?** | **SIM** |
| **Estrutura** | `id` UUIDv7, `id_usuario` FK → `usuario`, `token_hash` VARCHAR(200) NOT NULL, `expira_em` TIMESTAMPTZ NOT NULL, `usado_em` TIMESTAMPTZ, `revogado` BOOLEAN DEFAULT false, `created_at` TIMESTAMPTZ |
| **Justificativa** | MVP v1 §5.1 define explicitamente; seq. F0-001 (passo 7: INSERT refresh_token); diagrama classes IAM lista `RefreshToken` como composição de `User`. |
| **Status** | ✅ DECISÃO FINAL |

### I6 — F0-002/003 vs §5.3: Reset de senha sem tabela no DDL

| Aspecto | Decisão |
|---------|---------|
| **Criar `password_reset_token`?** | **NÃO** |
| **Incluir `jti_blacklist`?** | **SIM** |
| **Estrutura `jti_blacklist`** | `jti` UUID PRIMARY KEY (natural key = JTI do JWT), `expira_em` TIMESTAMPTZ NOT NULL, `created_at` TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| **Mecanismo** | F0-002 gera JWT em memória (JTI UUIDv7, audience=password-reset, exp=24h). F0-003 consome: verifica JTI não está em blacklist → executa reset → INSERT jti_blacklist. |
| **Uso estendido** | F3-003 e F7-001 (deep-links de deliberação) também usam JWT de uso único com JTI → blacklist. |
| **Justificativa** | HU RN-F0.3-03 ("JTI imediatamente inserido na blacklist"); seq. F0-003 passo 9 ("INSERT iam_jti_blacklist"). Nome físico: `jti_blacklist` (sem prefixo `iam_`). |
| **Status** | ✅ DECISÃO FINAL |

### I7 — DDL §5.3: Dependência circular `usuario.id_curso` ↔ `curso.id_coordenador`

| Aspecto | Decisão |
|---------|---------|
| **Estratégia** | Criar `curso` **sem** `id_coordenador`; criar `usuario` com `id_curso` FK → `curso`; depois `ALTER TABLE curso ADD COLUMN id_coordenador UUID REFERENCES usuario(id)` |
| **Alternativa descartada** | FK DEFERRABLE — adiciona complexidade operacional desnecessária para TCC |
| **Ordem Flyway** | (1) `curso` (sem coordenador), (2) `usuario`, (3) ALTER curso ADD id_coordenador |
| **Status** | ✅ DECISÃO FINAL |

### I8 — DDL §5.3: `calendario_academico.id_request_type` antes de `request_type` existir

| Aspecto | Decisão |
|---------|---------|
| **Estratégia** | Criar `request_type` no módulo Solicitações **antes** de `calendario_academico` |
| **Ordem corrigida** | extensões → curso (parcial) → usuario → IAM tables → request_type → calendario_academico → demais solicitações |
| **Justificativa** | FK `calendario_academico.id_request_type → request_type(id)` exige que `request_type` exista primeiro. Calendário acadêmico vincula tipos de solicitação a períodos de vigência. |
| **Status** | ✅ DECISÃO FINAL |

### I9 — Diagrama Classes Comunicação: `CommunicationDelivery` → `NotificationPreference`

| Aspecto | Decisão |
|---------|---------|
| **Relação física** | `communication_delivery.id_destinatario` → `usuario` (não diretamente para `notification_preference`) |
| **`notification_preference`** | Tabela separada, PK = `id_usuario` FK → `usuario`. Consultada pelo dispatcher para decidir canal/DND, não referenciada por FK em `communication_delivery` |
| **Justificativa** | §5.3 DDL confirma esta estrutura. Preferência é configuração do usuário; delivery é evento transacional. Relação é indireta via `id_destinatario`. |
| **Status** | ✅ DECISÃO FINAL |

### I10 — `casos_de_uso.md`: Nota explícita ER vs JPA presença

| Aspecto | Decisão |
|---------|---------|
| **Seguir qual versão?** | endpoints canônicos v4.1 + DDL §5.3 |
| **Nome físico** | `attendance_session` (não `checkin`, não `AttendanceCheckin`) |
| **Campos** | Conforme §5.3: `id_evento`, `id_aluno`, `device_uuid`, `entrada_em`, `saida_em`, `estado`, `created_at`, UNIQUEs |
| **Status** | ✅ DECISÃO FINAL (coerente com I1 e I4) |

### I11 — MVP v2 §4 vs DDL §5.3: Solicitações (reconciliação §0.3.1)

| Tópico | MVP v2 §4 | §5.3 (canônico TCC) | **Decisão para SQL/DBML TCC** |
|--------|-----------|---------------------|-------------------------------|
| Número protocolo | `numero` único (`2026-0042`) | `numero_anual` INT + `ano` SMALLINT + UNIQUE(ano, numero_anual) | **§5.3** — separação ano/número permite reset anual |
| `request_type` colunas | `nome`, `categoria`, `estado_inicial`, `sla_dias`, `authorities_required` | `code`, `descricao`, `prazo_dias`, `interna`, `form_schema`, `workflow_json`, `required_auth`, `ativo` | **§5.3** — `estado_inicial` vive dentro de `workflow_json.initial` (workflow-engine) |
| `request` FK `id_curso` | Ausente no v2 | Presente (NOT NULL) | **§5.3** — manter `id_curso` |
| `request_event` nomes | `de_estado`, `para_estado`, `comentario` | `estado_anterior`, `estado_novo`, `parecer`, `tipo`, `at` | **§5.3** — nomes mais expressivos |
| `request_line_item` | Ausente no v2 | Presente (para APROVEITAMENTO, multi-disciplina) | **2026-06: manter no schema TCC.** **As-built Flyway: NÃO migrada** — linhas em `request.dados` JSONB |
| `request_attachment` | `status` PENDING/CONFIRMED, `nome_arquivo` | `categoria`, `nome_original`, `uploaded_by`, `mime_type`, `tamanho_bytes`, `sha256`, `storage_key`, `uploaded_at` — **sem** `status` | **2026-06:** unificar §5.3 + `status`. **Flyway V004:** `content_type` (não `mime_type`); **sem** `uploaded_by` / **sem** `status` |
| Migrations v2 | `V003`/`V004` com 4 tabelas | 5 tabelas (`+request_line_item`) | **As-built:** V004 tem **4** tabelas (`request_type`, `request`, `request_event`, `request_attachment`); V019 adiciona `request_type_version` |

**Status:** ✅ DECISÃO FINAL (trilha 2026-06). **Override físico:** Flyway V004+V019 (§0).

### Decisão adicional I11-a: Coluna `status` em `request_attachment`

A coluna `status` (`PENDING`/`CONFIRMED`) do MVP v2 §4 foi **incorporada ao schema TCC 2026-06** (`schema_completo.sql`) porque:
1. O padrão de upload por URL pré-assinada exige rastrear se o arquivo foi efetivamente enviado ao storage
2. Sem `status`, attachments "fantasma" (URL gerada mas upload nunca feito) ficam indistinguíveis
3. DEFAULT = `'CONFIRMED'` mantém retrocompatibilidade com uploads diretos futuros

**As-built Flyway V004:** a coluna **`status` não existe**. I11-a permanece como decisão de modelo acadêmico, não como DDL runtime.

---

## 3. Ordem de criação

Ordem definitiva para o `schema_completo.sql` (resolve dependências circulares e FKs cross-module):

```
 #  Passo                               Tabela(s)                               Dependências satisfeitas
───────────────────────────────────────────────────────────────────────────────────────────────────────────
 0  Extensões + funções                  —                                       uuid-ossp, pgcrypto, citext, pg_trgm, uuid_generate_v7()
 1  Acadêmico (parcial)                  curso (SEM id_coordenador)              —
 2  Acadêmico                            disciplina                              curso
 3  Acadêmico                            periodo_letivo                          —
 4  IAM                                  usuario                                 curso (id_curso FK)
 5  IAM                                  role                                    —
 6  IAM                                  authority                               —
 7  IAM                                  role_authority                          role, authority
 8  IAM                                  usuario_role                            usuario, role
 9  IAM/Sessão                           refresh_token                           usuario
10  IAM/Segurança                        jti_blacklist                           — (PK natural)
11  Solicitações (config)                request_type                            —
12  Acadêmico (FK tardia)                calendario_academico                    periodo_letivo, request_type
13  Solicitações                         request                                 usuario, request_type, curso
14  Solicitações                         request_event                           request, usuario
15  Solicitações                         request_line_item                       request, disciplina  **[NÃO no Flyway — omitir]**
15b Solicitações                         request_type_version                    request_type (V019)
16  Solicitações                         request_attachment                      request, usuario
17  Formativas                           formative_activity                      curso
18  Formativas                           formative_entry                         usuario, formative_activity
19  Estágio                              internship                              usuario (aluno, orientador, coe)
20  Estágio                              internship_document                     internship
21  TCC                                  tcc                                     curso
22  TCC                                  tcc_member                              tcc, usuario
23  TCC                                  tcc_examiner                            tcc, usuario
24  Comunicação                          communication                           curso, usuario
25  Comunicação                          communication_delivery                  communication, usuario
26  Comunicação                          notification_preference                 usuario
27  Outbox                               outbox_event                            — (aggregate_id genérico)
28  Presença v4.1                        event_attendance                        curso, usuario
29  Presença v4.1                        attendance_session                      event_attendance, usuario
30  Certificados                         certificate                             usuario, event_attendance, formative_entry
31  Auditoria                            audit_log                               usuario (nullable)
32  FK tardia                            ALTER curso ADD id_coordenador          usuario
33  Índices                              CREATE INDEX (todos)                    tabelas já existem
```

**Notas sobre a ordem:**
- Passo 0: extensões devem ser as primeiras (funções dependem de `pgcrypto`/`uuid-ossp`)
- Passo 1: `curso` criado sem `id_coordenador` para quebrar circularidade com `usuario`
- Passo 11: `request_type` antes de `calendario_academico` (resolve I8)
- Passo 32: ALTER adiciona FK `id_coordenador` após `usuario` existir (resolve I7)
- Passo 33: índices agrupados ao final para clareza; podem estar inline se preferido

---

## 4. Pendências / perguntas ao usuário

### 4.1 Decisões tomadas sem bloqueio (nenhuma pergunta pendente)

Todas as inconsistências I1–I11 foram resolvidas com base nas fontes canônicas e precedência documentada em §0.1. Nenhuma ambiguidade remanescente bloqueia as próximas etapas.

### 4.2 Decisões opcionais confirmadas (para registro)

| Item | Decisão | Justificativa |
|------|---------|---------------|
| Normalizar `validation_windows` em tabela filha? | **NÃO** — manter JSONB | endpoints v4.1 + §5.3 usam JSONB; janelas variam por evento; query patterns não exigem JOIN nas janelas |
| Incluir `attendance_validation_window` no DDL? | **NÃO** | Consequência da decisão acima |
| Prefixo `iam_` em `jti_blacklist`? | **NÃO** — nome físico: `jti_blacklist` | HU usa `iam_jti_blacklist` como nome lógico; fisicamente, fica sem prefixo (consistente com demais tabelas IAM sem prefixo) |
| Incluir `audit_log` desde v1? | **SIM** — no schema completo TCC | seq. F0-001 já faz INSERT audit_log no login; MVP v1 §5.1 lista como opcional mas os diagramas a usam |

### 4.3 Colunas reveladas por diagramas de sequência F0 (ausentes no DDL §5.3)

Estas colunas são usadas nos diagramas mas **não estão explicitamente no DDL §5.3**. Decisão de inclusão:

| Coluna | Tabela | Origem | Decisão |
|--------|--------|--------|---------|
| `falhas_consecutivas` | `usuario` | F0.1-e (passo 5: "falhas_consecutivas=10") | **INCLUIR** — INT NOT NULL DEFAULT 0. Suporte ao bloqueio por tentativas (RN-F0.1-07). |
| `bloqueado_ate` | `usuario` | F0.1-e (passo 6: "UPDATE usuario SET bloqueado_ate") | **INCLUIR** — TIMESTAMPTZ nullable. Desbloqueio automático quando NOW() > bloqueado_ate. |
| `password_history` | `usuario` (ou tabela separada) | F0.3-a/c (passwordHistory[3]) | **IMPLEMENTAR como JSONB** dentro de `usuario.metadata` OU coluna dedicada `password_history JSONB DEFAULT '[]'`. Armazena últimos 3 hashes. **Decisão: coluna dedicada** na `usuario` para clareza. |
| `token_hash` em `refresh_token` | `refresh_token` | F0.1-a implícito; MVP v1 §5.1 explícito | **INCLUIR** — já previsto em T1 acima. |
| `revogado` / `status` em `refresh_token` | `refresh_token` | F0.1-f (passo 5: "REVOKE all refresh_tokens") | **INCLUIR** — `revogado BOOLEAN DEFAULT false` ou `usado_em TIMESTAMPTZ` (se usado_em NOT NULL → revogado). MVP v1 usa `usado_em`. **Manter `usado_em`** como indicador de rotação. Adicionar `revogado_em TIMESTAMPTZ` para revogação explícita em massa (reuse detection). |

### 4.4 Perguntas não-bloqueantes (para considerar nas etapas 2+)

| # | Pergunta | Impacto | Decisão provisória |
|---|----------|---------|-------------------|
| P1 | `refresh_token` deve ter `device_info` (user-agent, IP) para listagem de sessões ativas? | UX: tela "Dispositivos conectados" | Incluir `ip INET`, `user_agent TEXT` — custo baixo, benefício futuro. **Não-bloqueante.** |
| P2 | `audit_log.payload` deve armazenar o delta (antes/depois) ou apenas o contexto? | Volume de dados vs utilidade forense | Apenas contexto (userId, requestId, etc.) — delta via eventos dedicados (`request_event`). **Não-bloqueante.** |

---

## 5. Rastreabilidade (tabela → fonte doc)

| Tabela | Fonte primária (DDL) | Confirmação secundária |
|--------|---------------------|------------------------|
| `usuario` | §5.3 analise_arquitetural | MVP v1 §5.1, database-engineer.md, diagrama classes IAM |
| `role` | §5.3 | database-engineer.md, jpaInterfaces |
| `authority` | §5.3 | database-engineer.md, jpaInterfaces |
| `role_authority` | §5.3 | database-engineer.md (PK composta), jpaInterfaces |
| `usuario_role` | §5.3 | database-engineer.md (PK composta + escopo JSONB), jpaInterfaces |
| `refresh_token` | MVP v1 §5.1 | diagrama classes IAM (Composição User→RefreshToken), seq. F0-001, F0.1-f |
| `jti_blacklist` | seq. F0-003, HU RN-F0.3-03 | seq. F3-003, F7-001; PROMPT §0.2 |
| `curso` | §5.3 | MVP v1 §5.1, jpaInterfaces, diagrama classes Acadêmico |
| `disciplina` | §5.3 | jpaInterfaces, diagrama classes Acadêmico |
| `periodo_letivo` | §5.3 | jpaInterfaces, diagrama classes Acadêmico |
| `calendario_academico` | §5.3 | jpaInterfaces |
| `request_type` | §5.3 | workflow-engine-specialist.md, MVP v2 §4 (reconciliado I11); **as-built:** V004 sem `interna`/`required_auth` |
| `request` | §5.3 | workflow-engine-specialist.md, MVP v2 §4 (reconciliado I11); **as-built:** V004+V019 (`request_type_code`, `parecer`, `deleted_at`, `id_request_type_version`) |
| `request_event` | §5.3 | workflow-engine-specialist.md, MVP v2 §4; **as-built:** `created_at`+`updated_at`, `estado_anterior` NOT NULL |
| `request_line_item` | §5.3 | jpaInterfaces 2026-06; **Flyway: NÃO existe** |
| `request_type_version` | V019 Flyway | `as-built-backend.md` §5; `RequestTypeVersionJpaRepository` |
| `request_attachment` | §5.3 + MVP v2 §4 (unificado 2026-06) | **Flyway V004:** `content_type`, sem `status`/`uploaded_by` |
| `formative_activity` | §5.3 | jpaInterfaces, diagrama classes |
| `formative_entry` | §5.3 | jpaInterfaces, diagrama classes |
| `internship` | §5.3 | jpaInterfaces |
| `internship_document` | §5.3 | jpaInterfaces |
| `tcc` | §5.3 | jpaInterfaces |
| `tcc_member` | §5.3 | jpaInterfaces (PK composta) |
| `tcc_examiner` | §5.3 | jpaInterfaces (PK composta) |
| `communication` | §5.3 | jpaInterfaces, diagrama classes Comunicação |
| `communication_delivery` | §5.3 | jpaInterfaces, diagrama classes Comunicação |
| `notification_preference` | §5.3 | jpaInterfaces, diagrama classes Comunicação |
| `outbox_event` | §5.3 | diagrama classes Certificados/Outbox, PROMPT §0.2 |
| `event_attendance` | §5.3 | endpoints v4.1, jpaInterfaces, diagrama classes Presença |
| `attendance_session` | §5.3 | endpoints v4.1, jpaInterfaces, diagrama classes Presença (como AttendanceCheckin → renomeado I1) |
| `certificate` | §5.3 | jpaInterfaces, diagrama classes Certificados, §11 analise_arquitetural |
| `audit_log` | §5.3 | seq. F0-001/002/003, MVP v1 §5.1 (opcional→confirmado) |

---

## 6. Coluna completa de `request_attachment` (unificação I11)

Resultado da reconciliação §5.3 + MVP v2 §4 (**trilha 2026-06**). **Flyway V004 não tem** `mime_type`, `uploaded_by` nem `status`; usa `content_type` + `created_at`/`updated_at`.

| Coluna | Tipo | NOT NULL | Default | Fonte |
|--------|------|:--------:|---------|-------|
| `id` | UUID | ✓ | uuid_generate_v7() | §5.3 |
| `id_request` | UUID FK → request | ✓ | — | §5.3 |
| `categoria` | VARCHAR(50) | ✓ | — | §5.3 |
| `nome_original` | VARCHAR(255) | ✓ | — | §5.3 |
| `storage_key` | VARCHAR(500) | ✓ | — | §5.3 |
| `mime_type` | VARCHAR(100) | ✓ | — | §5.3 |
| `tamanho_bytes` | BIGINT | ✓ | — | §5.3 |
| `sha256` | CHAR(64) | ✓ | — | §5.3 |
| `uploaded_by` | UUID FK → usuario | ✓ | — | §5.3 |
| `uploaded_at` | TIMESTAMPTZ | ✓ | NOW() | §5.3 |
| **`status`** | **VARCHAR(20)** | **✓** | **'CONFIRMED'** | **MVP v2 §4** (adição) |

---

## 7. Colunas adicionais em `usuario` (reveladas por F0)

| Coluna | Tipo | NOT NULL | Default | Justificativa |
|--------|------|:--------:|---------|---------------|
| `falhas_consecutivas` | INTEGER | ✓ | 0 | F0.1-e: bloqueio após 10 falhas consecutivas |
| `bloqueado_ate` | TIMESTAMPTZ | — | NULL | F0.1-e: desbloqueio automático; NULL = não bloqueado |
| `password_history` | JSONB | ✓ | '[]' | F0.3-a/c: rejeitar reutilização das últimas 3 senhas |

---

## 8. Estrutura de `refresh_token` (consolidada)

| Coluna | Tipo | NOT NULL | Default | Fonte |
|--------|------|:--------:|---------|-------|
| `id` | UUID | ✓ | uuid_generate_v7() | MVP v1 §5.1 |
| `id_usuario` | UUID FK → usuario | ✓ | — | MVP v1 §5.1 |
| `token_hash` | VARCHAR(200) | ✓ | — | MVP v1 §5.1 |
| `expira_em` | TIMESTAMPTZ | ✓ | — | MVP v1 §5.1 |
| `usado_em` | TIMESTAMPTZ | — | NULL | MVP v1: indica rotação (token foi trocado por novo) |
| `revogado_em` | TIMESTAMPTZ | — | NULL | F0.1-f: revogação em massa (reuse detection) |
| `ip` | INET | — | NULL | Registro de origem (P1) |
| `user_agent` | TEXT | — | NULL | Registro de dispositivo (P1) |
| `created_at` | TIMESTAMPTZ | ✓ | NOW() | MVP v1 §5.1 |

ON DELETE CASCADE de `usuario` (composição — diagrama classes IAM).

---

## 9. Próximos passos

- [x] Etapa 0 — Inventário e decisões (este documento)
- [ ] Etapa 1 — Modelo conceitual (Mermaid erDiagram)
- [ ] Etapas 2–3 — Modelo lógico DBML (por módulo → merge)
- [ ] Etapa 4 — Modelo físico DBML
- [ ] Etapa 5 — Script SQL completo (`schema_completo.sql`)
- [ ] Etapa 6 — QA cruzada
- [ ] Etapa 7 — README TCC

---

**Versão:** 1.1 (as-built Flyway 2026-08-29 no §0; I1–I11 históricas preservadas)  
**Aprovação:** trilha 2026-06 concluída; overlay físico = Flyway V001–V019.
