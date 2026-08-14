# Prompt — Documentação do Banco de Dados (TCC 1 — SecretariaOnline2)

**Objetivo:** gerar em `foundationDocs/DB/` a documentação completa do banco PostgreSQL 16 do SO2: inventário, SQL único, modelo conceitual (Mermaid), modelo lógico (DBML) e modelo físico (DBML).

**Idioma de saída:** português do Brasil  
**Não inventar:** tabelas/colunas fora das fontes listadas em §2 — divergências → registrar em `00-inventario-e-decisoes.md` e perguntar ao usuário.

**Prompt mestre:** execute **uma etapa por chat** (§6). Não pule a Etapa 0.

---

## 0. Resultado da análise prévia (baseline — jun/2026)

### 0.1 Fontes canônicas (ordem de precedência)


| Prioridade | Arquivo                                                                      | Papel                                                                                      |
| ---------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| 1          | `foundationDocs/analysis/analise_arquitetural_secretariaonline2.md` §5.1–5.3 | Princípios + ER Mermaid + **DDL esboço**                                                   |
| 2          | `agents/database-engineer.md`                                                | Regras Flyway, tipos Postgres, índices, UUIDv7                                             |
| 3          | `foundationDocs/analysis/jpaInterfaces_PostgresEntities.md`                  | Mapeamento JPA ↔ tabela (29 domínio)                                                       |
| 4          | `foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md`         | Campos/estados presença v4.1                                                               |
| 5          | `foundationDocs/analysis/mvp_v1_walking_skeleton_aluno.md`                   | `refresh_token`, seed, IAM mínimo (implementação)                                          |
| 5b         | `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` §4          | Colunas/nomes do módulo solicitações (implementação) — **reconciliar** com §5.3 na Etapa 0 |
| 6          | `foundationDocs/otherDiagrams/Diagrama de Classes - Secretaria Online 2.md`  | Relações de domínio (composição)                                                           |
| 7          | `foundationDocs/otherDiagrams/Diagrama de Caso de Uso.md`                    | Limites por módulo F0–F8                                                                   |
| 8          | `foundationDocs/sequenceDiagrams/F0 — Público/US-F0-*.md`                    | `refresh_token`, `jti_blacklist`, revogação, reset senha (JWT 1-uso)                       |
| 9          | `agents/workflow-engine-specialist.md`                                       | `form_schema`, `workflow_json`, estados — apoio solicitações (Etapa 0 + M3)                |
| 10         | Imagens de referência TCC                                                    | `foundationDocs/prompts/bd-prompt-context/*.png`                                           |


### 0.2 Inventário de tabelas (~29 domínio + técnicas)


| Módulo        | Tabelas                                                                               | PK composta                      |
| ------------- | ------------------------------------------------------------------------------------- | -------------------------------- |
| IAM           | `usuario`, `role`, `authority`, `role_authority`, `usuario_role`                      | `role_authority`, `usuario_role` |
| Acadêmico     | `curso`, `disciplina`, `periodo_letivo`, `calendario_academico`                       | —                                |
| Solicitações  | `request_type`, `request`, `request_event`, `request_line_item`, `request_attachment` | —                                |
| Formativas    | `formative_activity`, `formative_entry`                                               | —                                |
| Estágio       | `internship`, `internship_document`                                                   | —                                |
| TCC           | `tcc`, `tcc_member`, `tcc_examiner`                                                   | `tcc_member`, `tcc_examiner`     |
| Comunicação   | `communication`, `communication_delivery`, `notification_preference`, `outbox_event`  | —                                |
| Presença v4.1 | `event_attendance`, `attendance_session`                                              | —                                |
| Certificados  | `certificate`                                                                         | —                                |
| Auditoria     | `audit_log`                                                                           | —                                |


**Tabelas técnicas a incluir no SQL final (fora dos 29):**


| Tabela                  | Fonte                                                      | Obrigatória                               |
| ----------------------- | ---------------------------------------------------------- | ----------------------------------------- |
| `refresh_token`         | MVP v1 + diagrama classes IAM + seq. F0-001                | Sim                                       |
| `jti_blacklist`         | seq. F0-003, F3-003, F7-001; HU RN-F0.3-03; `.cursorrules` | Sim                                       |
| `flyway_schema_history` | Flyway runtime                                             | Não no script manual (criada pelo Flyway) |


**Não incluir (decisão I6 — reset de senha):**

- `password_reset_token` — F0-002 gera JWT **em memória** (não persiste antes do uso); consumo em F0-003 grava JTI em `jti_blacklist`. Nos diagramas/HUs o nome lógico `iam_jti_blacklist` mapeia para a tabela física `jti_blacklist`.

**Opcional (documentar decisão na Etapa 0):**

- `attendance_validation_window` — só se normalizar `validation_windows` JSONB (hoje JSONB em `event_attendance` é canônico em §5.3 e endpoints v4.1).

### 0.3 Inconsistências detectadas (resolver na Etapa 0)


| #   | Onde                         | Problema                                                               | Decisão recomendada                                                                                                     |
| --- | ---------------------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| I1  | ER §5.2 vs DDL §5.3          | `ATTENDANCE_CHECKIN` no Mermaid; DDL usa `attendance_session`          | **Físico:** `attendance_session`. **Conceitual:** renomear para “Sessão de Presença” / `ATTENDANCE_SESSION`             |
| I2  | ER §5.2                      | `DELIBERATION` como entidade                                           | **Não** criar tabela; deliberação = `request_event` + `estado` em `request`                                             |
| I3  | ER §5.2                      | `FORM_SCHEMA`, `WORKFLOW_DEFINITION` separados                         | **Não** criar tabelas; colunas JSONB em `request_type`                                                                  |
| I4  | Diagrama classes presença    | `AttendanceCheckin` + `DeviceUuid` vs `attendance_session.device_uuid` | **Físico:** uma tabela `attendance_session`; `device_uuid` é coluna, não entidade                                       |
| I5  | `jpaInterfaces` vs MVP v1    | `refresh_token` ausente na lista principal                             | **Incluir** no SQL e DBML físico                                                                                        |
| I6  | F0-002/003 vs §5.3           | Reset sem tabela no DDL; seq. usa JWT + blacklist                      | **Não** `password_reset_token`; **incluir** `jti_blacklist` (`jti` PK ou UK, `expira_em`, `created_at`)                 |
| I7  | DDL §5.3                     | `usuario.id_curso` → `curso` e `curso.id_coordenador` → `usuario`      | Criar `curso` sem coordenador; `ALTER` ou seed depois; ou FK deferrable                                                 |
| I8  | DDL §5.3                     | `calendario_academico.id_request_type` antes de `request_type` existir | Ordem Flyway: extensões → acadêmico parcial → IAM → `request_type` → `calendario_academico`                             |
| I9  | Diagrama classes comunicação | `CommunicationDelivery` → `NotificationPreference`                     | **Físico:** `communication_delivery.id_destinatario` → `usuario`; preferências em `notification_preference` por usuário |
| I10 | `casos_de_uso.md`            | Nota explícita ER vs JPA presença                                      | Seguir **v4.1** (`attendance_session`)                                                                                  |
| I11 | MVP v2 §4 vs DDL §5.3        | Nomes/colunas divergentes em solicitações (ver §0.3.1)                 | **Base TCC:** §5.3; **incorporar do v2** onde indicado em §0.3.1                                                        |


### 0.4 Estilo visual para o TCC (referência `bd-prompt-context/`)


| Artefato                 | Imagem referência                                                                     | Convenção SO2                                                                                                                      |
| ------------------------ | ------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| **Conceitual**           | `image-b5669317-*.png` (FIGURA 2 — Chen: retângulos + losangos + cardinalidade 1/N/M) | Mermaid `erDiagram` **sem blocos de atributos**; nomes em português ou inglês consistente; legenda “FIGURA X – Modelo conceitual…” |
| **Lógico**               | `image-817df885-*.png` (FIGURA 3 — tabelas com PK/FK, tipos, NOT NULL)                | DBML com `Table`, tipos genéricos (`uuid`, `varchar`, `jsonb`, `timestamptz`); renderizar em [dbdiagram.io](https://dbdiagram.io)  |
| **Físico**               | Mesmo layout do lógico + detalhes Postgres                                            | DBML com tipos Postgres (`citext`, `timestamptz`, `inet`, `numeric(5,2)`), `[note: 'idx parcial…']` para índices relevantes        |
| **Classes (apoio)**      | `image-2fc8313d-*.png`, `image-356faec1-*.png`, etc.                                  | Composição UML → `ON DELETE CASCADE` nas FKs filhas                                                                                |
| **Casos de uso (apoio)** | `image-e9cfb58c-*.png`, `image-97e3f688-*.png`, etc.                                  | Agrupar tabelas por módulo F0–F8 no inventário                                                                                     |


> **Nota:** as imagens de exemplo (estudo biológico) são **referência de formatação acadêmica**, não de domínio. O conteúdo vem exclusivamente dos docs SO2.

### 0.3.1 I11 — Solicitações: MVP v2 §4 vs `analise_arquitetural` §5.3

Documentação **TCC (schema completo)** usa §5.3 como base. MVP v2 é guia de implementação incremental — reconciliar assim:


| Tópico               | MVP v2 §4                                                                 | §5.3 (canônico TCC)                                           | Decisão para SQL/DBML TCC                                                                |
| -------------------- | ------------------------------------------------------------------------- | ------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Número protocolo     | `numero` único (`2026-0042`)                                              | `numero_anual` + `ano` + `UNIQUE(ano, numero_anual)`          | **Manter §5.3**                                                                          |
| `request_type`       | `nome`, `categoria`, `estado_inicial`, `sla_dias`, `authorities_required` | `descricao`, `prazo_dias`, `interna`, `required_auth`, `code` | **Manter §5.3**; `estado_inicial` vive em `workflow_json` (workflow-engine)              |
| `request`            | sem `id_curso` explícito no v2                                            | `id_curso` FK obrigatório                                     | **Manter §5.3** (`id_curso`)                                                             |
| `request_event`      | `de_estado`, `para_estado`, `comentario`                                  | `estado_anterior`, `estado_novo`, `parecer`, `tipo`, `at`     | **Manter nomes §5.3**                                                                    |
| `request_line_item`  | ausente no v2                                                             | presente (APROVEITAMENTO etc.)                                | **Manter** — fora do escopo UI v2, no schema TCC                                         |
| `request_attachment` | `status` PENDING/CONFIRMED, `nome_arquivo`                                | `categoria`, `nome_original`, `uploaded_by`, sem `status`     | **Unificar:** manter §5.3 + **adicionar** `status` VARCHAR (`PENDING`/`CONFIRMED`) do v2 |
| Migrations v2        | `V003`/`V004` só 4 tabelas                                                | 5 tabelas com `request_line_item`                             | Flyway futuro pode ser incremental; **doc TCC** descreve modelo completo                 |


### 0.5 Estrutura de saída (`foundationDocs/DB/`)

```
foundationDocs/DB/
├── README.md                      ← índice TCC + como renderizar diagramas
├── 00-inventario-e-decisoes.md    ← Etapa 0
├── V000__extensions_and_functions.sql  ← extensões + uuid_generate_v7()
├── schema_completo.sql            ← script único (todas as tabelas + índices)
├── modelo-conceitual.mmd          ← Mermaid erDiagram
├── modelo-conceitual.md           ← wrapper: título FIGURA, fonte, bloco mermaid
├── modelo-logico.dbml
├── modelo-fisico.dbml
└ exports/                         ← (opcional, manual) PNG de dbdiagram.io
```

---

## 1. Estratégia de modelos (custo × qualidade)


| Etapa                          | Modelo Cursor                                     | Por quê                                    | Custo relativo |
| ------------------------------ | ------------------------------------------------- | ------------------------------------------ | -------------- |
| **0** Inventário + decisões    | **Claude Opus 4.6** (thinking high)               | Cruzar fontes §0.1, resolver I1–I11        | $$$ (1×)       |
| **1** Conceitual Mermaid       | **Composer 2.5**                                  | Só entidades/relações; baixo risco         | $              |
| **2** Lógico DBML (por módulo) | **Claude Sonnet 4.6** (thinking medium) × 9 chats | Atributos + FK; módulo limita alucinação   | $$             |
| **3** Merge lógico DBML        | **Composer 2.5**                                  | Concatenar arquivos; sem raciocínio pesado | $              |
| **4** Físico DBML              | **GPT-5.3 Codex**                                 | Tipos Postgres, índices, constraints       | $$             |
| **5** SQL completo             | **GPT-5.3 Codex**                                 | DDL executável; ordem de criação           | $$             |
| **6** QA cruzada               | **Claude Opus 4.6** (thinking high)               | Diff SQL ↔ DBML físico ↔ inventário        | $$$ (1×)       |
| **7** README TCC               | **Composer 2.5**                                  | Prosa + instruções de render               | $              |


**Economia:** Opus só 2× (Etapas 0 e 6). Sonnet no trabalho repetitivo. Codex só em SQL/DBML físico. Composer em tarefas mecânicas.

**Alternativa premium (menos etapas, mais caro):** Etapas 2+4+5 em um único chat **Opus 4.6** por módulo (9 chats) — elimina Sonnet/Codex mas aumenta custo ~3×.

---

## 2. Referências obrigatórias (anexar em todo chat)

```
@agents/database-engineer.md
@foundationDocs/analysis/analise_arquitetural_secretariaonline2.md
@foundationDocs/analysis/jpaInterfaces_PostgresEntities.md
@foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md
@foundationDocs/prompts/PROMPT_gerar_documentacao_banco_dados.md
```

**Etapa 0 adicionar:**

```
@foundationDocs/otherDiagrams/Diagrama de Classes - Secretaria Online 2.md
@foundationDocs/otherDiagrams/Diagrama de Caso de  Uso.md
@foundationDocs/analysis/mvp_v1_walking_skeleton_aluno.md
@foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md
@agents/workflow-engine-specialist.md
@foundationDocs/sequenceDiagrams/F0 — Público/US-F0-001-LOGIN.md
@foundationDocs/sequenceDiagrams/F0 — Público/US-F0-002-RECUPERAR-SENHA.md
@foundationDocs/sequenceDiagrams/F0 — Público/US-F0-003-NOVA-SENHA.md
```

**Etapa 2 — M3 Solicitações adicionar:**

```
@foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md
@agents/workflow-engine-specialist.md
```

**Etapas 2+ (demais módulos):** anexar só a seção relevante do §5.3 da análise arquitetural.

**Estilo visual:** consultar imagens em `@foundationDocs/prompts/bd-prompt-context/` (não copiar domínio delas).

---

## 3. Regras transversais (todas as etapas)

1. **PK:** `UUID PRIMARY KEY DEFAULT uuid_generate_v7()` — nunca SERIAL.
2. **Datas/horas:** `TIMESTAMPTZ`; calendário puro: `DATE`.
3. **Email:** `CITEXT` onde aplicável.
4. **JSONB:** `request.dados`, `request_type.form_schema`, `request_type.workflow_json`, `usuario.metadata`, `event_attendance.validation_windows`, etc.
5. **Soft delete:** `deleted_at TIMESTAMPTZ` em `usuario` (e outras se §5.3 indicar).
6. **Nomes físicos:** `snake_case` em português/inglês conforme DDL §5.3 (não misturar).
7. **Não editar** migrations já aplicadas (quando existirem no backend); este pacote é documentação TCC em `foundationDocs/DB/`.
8. **Validação pós-geração:**
  - DBML → colar em [dbdiagram.io](https://dbdiagram.io)
  - Mermaid → [mermaid.live](https://mermaid.live)
  - SQL → `psql -f schema_completo.sql` em Postgres 16 vazio (dev)

---

## 4. Ordem dos módulos (Etapas 2 e dependências FK)

```
1. extensions (V000)
2. academico: curso, disciplina, periodo_letivo
3. iam: usuario, role, authority, role_authority, usuario_role, refresh_token, jti_blacklist
4. academico: calendario_academico (FK request_type — criar request_type antes!)
5. solicitacoes: request_type → request → request_event, request_line_item, request_attachment
6. formativas
7. estagio
8. tcc
9. comunicacao + outbox
10. presenca (event_attendance, attendance_session)
11. certificate
12. audit_log
13. FK tardia: curso.id_coordenador → usuario (se necessário)
```

---

## 5. Fila de módulos (Etapa 2 — um chat cada)


| #   | Módulo                  | Tabelas                                | Arquivo parcial                       |
| --- | ----------------------- | -------------------------------------- | ------------------------------------- |
| M1  | IAM + sessão            | 7 (`+refresh_token`, `+jti_blacklist`) | `_parcial/iam.dbml`                   |
| M2  | Acadêmico               | 4                                      | `_parcial/academico.dbml`             |
| M3  | Solicitações            | 5                                      | `_parcial/solicitacoes.dbml`          |
| M4  | Formativas              | 2                                      | `_parcial/formativas.dbml`            |
| M5  | Estágio                 | 2                                      | `_parcial/estagio.dbml`               |
| M6  | TCC                     | 3                                      | `_parcial/tcc.dbml`                   |
| M7  | Comunicação + Outbox    | 4                                      | `_parcial/comunicacao.dbml`           |
| M8  | Presença v4.1           | 2                                      | `_parcial/presenca.dbml`              |
| M9  | Certificado + Auditoria | 2                                      | `_parcial/certificado_auditoria.dbml` |


Após M1–M9: Etapa 3 merge → `modelo-logico.dbml`.

---

## 6. Prompts por etapa (copiar no Cursor — modo Agent)

### Etapa 0 — Inventário e decisões

**Modelo:** `Claude Opus 4.6` (thinking high)  
**Saída:** `foundationDocs/DB/00-inventario-e-decisoes.md`

```
Você é o database engineer do SecretariaOnline2.

MODELO: Opus 4.6 thinking — leia todas as fontes antes de escrever.

TAREFA: Etapa 0 do PROMPT_gerar_documentacao_banco_dados.md (§0, §6 Etapa 0).

1. Liste TODAS as tabelas (domínio + técnicas) com módulo, PK, FKs principais.
2. Para cada inconsistência I1–I11 do prompt (incl. §0.3.1), registre DECISÃO FINAL (ou PERGUNTA ao usuário se bloqueado).
3. Reconcilie MVP v1 (IAM) e MVP v2 §4 (solicitações) com §5.3 — não duplicar tabelas; unificar colunas conforme §0.3.1.
4. Confirme ordem de criação (§4).
5. Liste tabelas/colunas presentes em diagramas de classe / seq. F0 mas ausentes no DDL §5.3.
6. NÃO gere SQL, DBML ou Mermaid ainda — só inventário.

FORMATO do arquivo:
# Inventário e Decisões — Banco SO2
## 1. Tabelas
## 2. Resolução de inconsistências
## 3. Ordem de criação
## 4. Pendências / perguntas
## 5. Rastreabilidade (tabela → fonte doc)
```

---

### Etapa 1 — Modelo conceitual (Mermaid)

**Modelo:** `Composer 2.5`  
**Entrada:** `00-inventario-e-decisoes.md`  
**Saída:** `modelo-conceitual.mmd` + `modelo-conceitual.md`

```
MODELO: Composer 2.5 — seja conciso.

TAREFA: Etapa 1 — modelo CONCEITUAL do banco SO2.

FONTES: @foundationDocs/DB/00-inventario-e-decisoes.md + §0.4 do prompt mestre.

REGRAS:
- Mermaid erDiagram APENAS entidades e relacionamentos (SEM blocos { atributos }).
- Aplicar decisões I1–I11 (ex.: ATTENDANCE_SESSION; `jti_blacklist`; sem DELIBERATION).
- Agrupar por módulo com comentários %% IAM, %% Solicitações, etc.
- Cardinalidade UML: ||--o{, }o--||, etc.

ARQUIVO modelo-conceitual.md:
# FIGURA X – Modelo conceitual do banco de dados transacional — SecretariaOnline2
FONTE: Os autores (2026).
+ bloco mermaid incluso ou link para .mmd
```

---

### Etapa 2 — Modelo lógico DBML (um módulo)

**Modelo:** `Claude Sonnet 4.6` (thinking medium)  
**Repetir:** uma invocação por linha da fila §5 (M1…M9)

```
MODELO: Sonnet 4.6 thinking medium.

TAREFA: Etapa 2 — DBML LÓGICO do módulo [M1 IAM | M2 Acadêmico | … ].

FONTES: inventário Etapa 0 + §5.3 analise_arquitetural (seção do módulo) + database-engineer.md.
Se M3 Solicitações: + mvp_v2 §4 + workflow-engine-specialist.md; aplicar decisões I11 e §0.3.1.
Se M1 IAM: incluir refresh_token e jti_blacklist conforme inventário (I5, I6).

REGRAS DBML:
- Table nome_tabela { coluna tipo [pk|unique|not null|ref: > outra.col] }
- Tipos lógicos: uuid, varchar, citext, jsonb, timestamptz, date, boolean, text, inet, numeric
- PK compostas: usar syntax DBML composta ou Table com Indexes { (col_a, col_b) [pk] }
- Incluir Ref: ou ref inline para FKs
- NÃO incluir índices parciais ainda (isso é físico)

SAÍDA: foundationDocs/DB/_parcial/<modulo>.dbml

Ao final, liste FKs que apontam para tabelas de outros módulos (para validação no merge).
```

---

### Etapa 3 — Merge DBML lógico

**Modelo:** `Composer 2.5`  
**Saída:** `modelo-logico.dbml`

```
MODELO: Composer 2.5.

TAREFA: Etapa 3 — mesclar foundationDocs/DB/_parcial/*.dbml em modelo-logico.dbml.

REGRAS:
- Remover Table duplicadas
- Unificar Project { database_type: 'PostgreSQL' } no topo
- Ordenar: IAM → Acadêmico → … → Auditoria
- Adicionar cabeçalho comentário com contagem de tabelas
- Não alterar colunas sem conflito — se conflito, citar os dois arquivos e parar
```

---

### Etapa 4 — Modelo físico DBML

**Modelo:** `GPT-5.3 Codex`  
**Entrada:** `modelo-logico.dbml` + `00-inventario-e-decisoes.md` + `database-engineer.md`  
**Saída:** `modelo-fisico.dbml`

```
MODELO: GPT-5.3 Codex.

TAREFA: Etapa 4 — derivar modelo-fisico.dbml a partir do modelo-logico.dbml.

ADICIONAR em relação ao lógico:
- Tipos Postgres exatos (citext, timestamptz, numeric(5,2), char(64))
- Defaults: uuid_generate_v7(), NOW(), '{}'::jsonb
- ON DELETE CASCADE onde composição UML (diagramas de classe)
- Indexes { } para FKs, filtros UI, GIN jsonb, parciais (outbox PENDING, request abertas)
- Table notes para constraints CHECK relevantes
- Tabelas técnicas IAM: refresh_token, jti_blacklist (sem password_reset_token)
- request_attachment.status PENDING/CONFIRMED se inventário I11 confirmar

NÃO remover tabelas do lógico sem justificar no comentário.
```

---

### Etapa 5 — Script SQL completo

**Modelo:** `GPT-5.3 Codex`  
**Saída:** `V000__extensions_and_functions.sql` + `schema_completo.sql`

```
MODELO: GPT-5.3 Codex.

TAREFA: Etapa 5 — SQL PostgreSQL 16 executável.

FONTES: modelo-fisico.dbml + 00-inventario-e-decisoes.md + §5.3 analise_arquitetural + database-engineer.md.

ESTRUTURA schema_completo.sql:
1. Comentário cabeçalho TCC
2. \\i ou incluir inline extensões + uuid_generate_v7() (mesmo conteúdo V000)
3. CREATE TABLE na ordem §4
4. CREATE INDEX após tabelas
5. Comentários ROLLBACK por seção (não executáveis)

REGRAS:
- Um arquivo único autocontido (schema_completo.sql)
- Arquivo separado V000 só se preferir modularidade Flyway futura
- Resolver dependência circular curso ↔ usuario conforme inventário
```

---

### Etapa 6 — QA cruzada (somente leitura + relatório)

**Modelo:** `Claude Opus 4.6` (thinking high)  
**Saída:** append em `00-inventario-e-decisoes.md` seção `## 6. QA` ou `QA-relatorio.md`

```
MODELO: Opus 4.6 thinking — NÃO reescreva arquivos sem listar erros primeiro.

TAREFA: Etapa 6 — validar consistência entre:
- schema_completo.sql
- modelo-fisico.dbml
- modelo-logico.dbml
- modelo-conceitual.mmd
- jpaInterfaces_PostgresEntities.md
- decisões I11 (mvp_v2 §4 vs §5.3) em 00-inventario-e-decisoes.md

CHECKLIST:
[ ] Contagem de tabelas bate
[ ] Toda FK do SQL existe no DBML físico
[ ] Nomes presença v4.1 (attendance_session, não checkin)
[ ] refresh_token + jti_blacklist presentes; sem password_reset_token
[ ] Solicitações: §5.3 + status em attachment (I11); request_line_item presente
[ ] Sem DELIBERATION / FORM_SCHEMA como tabelas
[ ] Tipos citext/timestamptz/jsonb corretos

SAÍDA: tabela PASS/FAIL por item + patches sugeridos (diff textual).
```

---

### Etapa 7 — README documentação TCC

**Modelo:** `Composer 2.5`  
**Saída:** `foundationDocs/DB/README.md`

```
MODELO: Composer 2.5.

TAREFA: Etapa 7 — README para entrega TCC.

INCLUIR:
- Visão dos 3 níveis (conceitual / lógico / físico)
- Mapa arquivo → figura TCC
- Como renderizar: dbdiagram.io (DBML), mermaid.live (conceitual)
- Stack: PostgreSQL 16, Flyway, UUIDv7
- Referências aos docs fonte
- Status da QA (Etapa 6)
```

---

## 7. Checklist de conclusão

- [x] Etapa 0 — inventário sem pendências bloqueantes
- [x] Etapa 1 — conceitual renderiza em mermaid.live
- [x] Etapas 2–3 — lógico renderiza em dbdiagram.io
- [x] Etapa 4 — físico com índices e tipos Postgres
- [x] Etapa 5 — SQL roda em Postgres 16 limpo
- [x] Etapa 6 — QA sem FAIL críticos
- [x] Etapa 7 — README publicado
- [ ] Export PNG opcional em `foundationDocs/DB/exports/` para o PDF do TCC

---

## 8. Automação futura (opcional)

Similar ao §7.5 de `promptParaGerarDiagramasDeSequencia.md`: Cursor Automation / Loop com fila `M1…M9` + Etapas 3–7, **desde que** Etapa 0 tenha sido aprovada manualmente.

---

**Versão:** 1.1 — 2026-06-21  
**Changelog v1.1:** MVP v2 na Etapa 0; I11 solicitações; `jti_blacklist` substitui `password_reset_token`; M3 com workflow-engine.  
**Autor:** planejamento gerado a partir da análise dos docs SO2 e imagens em `bd-prompt-context/`.