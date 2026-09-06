# Documentação do Banco de Dados — SecretariaOnline2 (TCC)

**Projeto:** SecretariaOnline2 — modernização do sistema acadêmico (UFPR SEPT)  
**Autores:** Os autores (2026)  
**Última atualização:** 2026-08-29  
**Pipeline:** Etapas 0–7 de `foundationDocs/prompts/PROMPT_gerar_documentacao_banco_dados.md`

**Contrato as-built (código real):** `foundationDocs/analysis/as-built-backend.md` — especialmente **§5 Banco as-built (Flyway)**. Onde o modelo acadêmico 2026-06 e o Flyway divergem, **o Flyway vence** para o schema físico.

Este diretório reúne o modelo de dados transacional do SO2 em três níveis (conceitual, lógico e físico), o DDL acadêmico (`schema_completo.sql`) e o inventário de decisões. A **fonte de verdade do Postgres em execução** é o Flyway, não o `schema_completo.sql`.

---

## 0. Trilha dupla (2026-06 vs Flyway 2026-08)

Há **dois artefatos de schema** que não devem ser misturados:

| Trilha | O que é | Tabelas | `request_line_item` | `request_type_version` |
|--------|---------|---------|---------------------|------------------------|
| **Modelo acadêmico 2026-06** | `schema_completo.sql` + DBML gerados nas Etapas 0–5; decisões I1–I11 | **31** (29 domínio + `refresh_token` + `jti_blacklist`) | **Incluída** no modelo TCC — **não migrada** | Ausente |
| **Flyway as-built V001–V019** | `backend/app/src/main/resources/db/migration/` | **~45** tabelas de aplicação | **Não existe.** Linhas por disciplina vivem em `request.dados` JSONB | **Existe** (V019) + FK `request.id_request_type_version` |

**Extras Flyway (além das 31, sem `request_line_item`):** `password_history` (tabela V002, não JSONB em `usuario`); V012 `service_record`, `faq_item`, `support_ticket`, `device_fcm_token`; V014 `graduation_record`, `secretary_task`, `import_job`, `communication_template`, `communication_template_revision`, `notification_log`, `export_job`; V015 `historico_escolar`, `contact_message`; V019 `request_type_version`.

### Flyway — regras operacionais

| Item | Valor |
|------|--------|
| Path | `backend/app/src/main/resources/db/migration/` |
| Versões aplicadas | **V001–V019** (não há V008/V009) |
| Próxima migration | **V020** — nunca editar V001–V019 |
| Hibernate `ddl-auto` | `validate` (`application-dev.yml`, test, prod) |
| Flyway em `dev` | **ligado** (`spring.flyway.enabled: true`) |

Detalhe de colunas de `request` / `request_type`: `as-built-backend.md` §5.3.

---

## 1. Visão dos três níveis

| Nível | Artefato | O que representa | Detalhe |
|-------|----------|------------------|---------|
| **Conceitual** | `modelo-conceitual.mmd` / `.md` | Entidades e relacionamentos (Chen) | Sem atributos; decisões I1–I11. `REQUEST_LINE_ITEM` é **conceitual-only** (não existe no Flyway) |
| **Lógico** | `modelo-logico.dbml` | Tabelas, colunas, tipos lógicos, FKs | Base 2026-06 (31) **com overlay as-built** em solicitações (V004+V018+V019); sem `request_line_item`; com `request_type_version` |
| **Físico** | `modelo-fisico.dbml` + `schema_completo.sql` | PostgreSQL 16 | `schema_completo.sql` = trilha 2026-06 (ainda inclui `request_line_item`). **Runtime = Flyway V001–V019** |

**Fluxo de derivação (histórico TCC):**

```
Fontes (§5.3, MVP v1/v2, F0, workflow-engine)
        ↓
00-inventario-e-decisoes.md  (Etapa 0 — decisões I1–I11; as-built no topo)
        ↓
modelo-conceitual.mmd        (Etapa 1 — line_item conceitual-only)
        ↓
_parcial/*.dbml              (Etapa 2 — solicitacoes.dbml alinhado ao Flyway)
        ↓
modelo-logico.dbml           (Etapa 3 — merge + overlay as-built M3)
        ↓
modelo-fisico.dbml           (Etapa 4 — overlay as-built M3)
        ↓
schema_completo.sql          (Etapa 5 — trilha 2026-06; NÃO aplicar em runtime)
        ↓
Flyway V001–V019             (fonte de verdade do banco)
```

---

## 2. Mapa arquivo → figura TCC

| Arquivo | Figura / papel no TCC | Uso recomendado no PDF |
|---------|----------------------|------------------------|
| `modelo-conceitual.md` / `.mmd` | **FIGURA 2** — Modelo conceitual | Diagrama ER de alto nível; nota: line_item conceitual-only |
| `modelo-logico.dbml` | **FIGURA 3** — Modelo lógico | Overlay as-built em solicitações; extras V012–V015 não estão neste diagrama |
| `modelo-fisico.dbml` | **FIGURA 4** — Modelo físico | Idem; tipos Postgres do overlay M3 = Flyway |
| `schema_completo.sql` | **ARTEFATO DDL 2026-06** | Anexo histórico; **não** é o schema do compose |
| Flyway `V001`–`V019` | **Schema as-built** | Única fonte para Postgres de `dev`/`test`/`prod` |
| `00-inventario-e-decisoes.md` | Tabela de decisões | I1–I11 históricas; as-built no topo sobrepõe o físico |
| `foreignKey_crossModulo.md` | Apoio à validação | FKs live = Flyway (sem `request_line_item`) |
| `exports/modelo-conceitual-validacao.svg` | Pré-visualização (opcional) | PNG/SVG exportado do conceitual |
| `_parcial/*.dbml` | Artefatos intermediários | `solicitacoes.dbml` alinhado a V004+V018+V019 |

**Resumo quantitativo:** trilha 2026-06 = **31 tabelas** (inclui `request_line_item` não migrada). Flyway as-built = **~45 tabelas**, sem `request_line_item`, com `request_type_version` e tabelas V012–V015. Ver `as-built-backend.md` §5.2.

---

## 3. Como renderizar os diagramas

### 3.1 Modelo conceitual — [mermaid.live](https://mermaid.live)

1. Abra `modelo-conceitual.md` ou `modelo-conceitual.mmd`.
2. Copie o bloco `erDiagram` (sem os cercas ` ```mermaid `).
3. Cole em [https://mermaid.live](https://mermaid.live).
4. Exporte PNG/SVG (**Actions → Export**) para inserir como **FIGURA 2** no PDF.

Alternativa: o arquivo `exports/modelo-conceitual-validacao.svg` já contém uma renderização validada.

### 3.2 Modelos lógico e físico — [dbdiagram.io](https://dbdiagram.io)

1. Acesse [https://dbdiagram.io/d](https://dbdiagram.io/d).
2. **FIGURA 3:** importe ou cole o conteúdo de `modelo-logico.dbml`.
3. **FIGURA 4:** importe ou cole o conteúdo de `modelo-fisico.dbml`.
4. Ajuste o zoom e exporte PNG (**Export → PNG**) para o PDF do TCC.

> Dica: o modelo físico da trilha 2026-06 é extenso. Para o schema **completo as-built** (~45 tabelas), use as migrations Flyway ou `as-built-backend.md` §5 — os DBML desta pasta não listam V012–V015.

### 3.3 DDL — não usar `schema_completo.sql` no runtime

```bash
# Runtime (dev): Flyway aplica V001–V019 via Spring Boot
# Path: backend/app/src/main/resources/db/migration/

# O script abaixo é o artefato TCC 2026-06 (inclui request_line_item NÃO migrada).
# NÃO aplicar em banco que já rodou Flyway.
# psql -d secretariaonline2 -f foundationDocs/DB/schema_completo.sql
```

---

## 4. Stack tecnológica

| Componente | Versão / escolha | Observação |
|------------|------------------|------------|
| **SGBD** | PostgreSQL 16 | `TIMESTAMPTZ`, `JSONB`, `CITEXT`, `pg_trgm` |
| **Migrations** | Flyway **V001–V019** em `backend/app/src/main/resources/db/migration/` | Próxima = **V020**. Nunca editar V001–V019. `schema_completo.sql` e `V000__*.sql` nesta pasta são trilha 2026-06 |
| **JPA** | `ddl-auto: validate` | Flyway **ligado** em `dev` / `test` / `prod` |
| **PKs** | UUIDv7 | Função `uuid_generate_v7()` em **V001** |
| **Senhas** | Argon2id | Coluna `usuario.senha_hash` (aplicação) |
| **Sessão** | `refresh_token` + JWT + Redis `sid` | Rotação com detecção de reuso |
| **Tokens one-shot** | `jti_blacklist` (Postgres) | E-mail / OTT de senha. Revogação de access JWT = Redis |
| **Workflow** | JSONB em `request_type` + snapshot em `request_type_version` | Authorities nas transições do `workflow_json` (sem colunas `interna` / `required_auth`) |
| **Assíncrono** | Outbox via `OutboxEventPublisher` | Tabela `outbox_event` (sem RabbitMQ no MVP) |

**Extensões PostgreSQL** (criadas em **V001**, não V000):

- `uuid-ossp`, `pgcrypto`, `citext`, `pg_trgm`

---

## 5. Referências aos documentos fonte

| Prioridade | Documento | Contribuição |
|:----------:|-----------|--------------|
| **0** | `foundationDocs/analysis/as-built-backend.md` **§5** | **Vence** para schema físico, colunas de `request`, ausência de `request_line_item` |
| 1 | `foundationDocs/analysis/analise_arquitetural_secretariaonline2.md` (§5.2 ER, §5.3 DDL) | Modelo canônico TCC 2026-06 (intenção) |
| 2 | `foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md` | Presença v4.1 (`attendance_session`, modos QR/SECRET) |
| 3 | `foundationDocs/analysis/mvp_v1_walking_skeleton_aluno.md` | IAM: `refresh_token`, colunas de `usuario` |
| 4 | `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` | Solicitações (I11 histórica: `status` em attachment — **não** está no Flyway V004) |
| 5 | `agents/workflow-engine-specialist.md` | `form_schema`, `workflow_json`, sem tabela `DELIBERATION` |
| 6 | `agents/database-engineer.md` | Convenções Postgres, índices, Flyway |
| 7 | `foundationDocs/analysis/jpaInterfaces_PostgresEntities.md` | Mapeamento repositório ↔ tabela (as-built no topo) |
| 8 | `foundationDocs/sequenceDiagrams/F0 — Público/US-F0-00*.md` | `jti_blacklist`, bloqueio de login, `password_history` |
| 9 | `foundationDocs/otherDiagrams/Diagrama de Classes - Secretaria Online 2.md` | Composições UML |
| 10 | `foundationDocs/prompts/PROMPT_gerar_documentacao_banco_dados.md` | Pipeline Etapas 0–7 |

Decisões consolidadas e rastreabilidade: `00-inventario-e-decisoes.md` (as-built no topo; I1–I11 históricas).

---

## 6. Status da QA (Etapa 6) — trilha 2026-06

**Resultado histórico: PASS (9/9 checks)** sobre `schema_completo.sql` / DBML de 2026-06. **Não** revalida o Flyway as-built.

| # | Item | Status |
|---|------|:------:|
| 1 | Contagem de tabelas (31 em todos os artefatos 2026-06) | PASS (trilha acadêmica) |
| 2 | FKs do SQL presentes no DBML físico | PASS (trilha acadêmica) |
| 3 | Nomes presença v4.1 (`attendance_session`) | PASS |
| 4 | `refresh_token` + `jti_blacklist`; sem `password_reset_token` | PASS |
| 5 | Solicitações §5.3 + `request_attachment.status` (I11) + `request_line_item` | PASS **somente 2026-06**. Flyway V004: **sem** `status`, **sem** `request_line_item` |
| 6 | Sem `DELIBERATION` / `FORM_SCHEMA` como tabelas | PASS |
| 7 | Tipos `citext` / `timestamptz` / `jsonb` corretos | PASS |
| 8 | JPA doc ↔ SQL (29 domínio + 2 técnicas) | PASS 2026-06; as-built ≈ 45 tabelas, ver `jpaInterfaces_PostgresEntities.md` |
| 9 | Conceitual ↔ SQL (cobertura 1:1) | PASS 2026-06; line_item conceitual-only vs Flyway |

**As-built (2026-08-29):** overlay de solicitações nos DBML alinhado a V004+V018+V019; inventário e FKs atualizados. `schema_completo.sql` permanece trilha 2026-06.

**Pendência opcional:** exportar PNGs de `dbdiagram.io` em `exports/` para inserção direta no PDF do TCC.

---

## 7. Estrutura do diretório

```
foundationDocs/DB/
├── README.md                          ← este arquivo (Etapa 7 + as-built 2026-08)
├── 00-inventario-e-decisoes.md        ← Etapa 0 + seção as-built no topo
├── V000__extensions_and_functions.sql ← trilha 2026-06 (runtime usa Flyway V001)
├── schema_completo.sql                ← DDL 2026-06 (não é o schema do compose)
├── modelo-conceitual.mmd              ← FIGURA 2 (line_item conceitual-only)
├── modelo-conceitual.md               ← FIGURA 2 (wrapper TCC)
├── modelo-logico.dbml                 ← FIGURA 3 (overlay M3 as-built)
├── modelo-fisico.dbml                 ← FIGURA 4 (overlay M3 as-built)
├── foreignKey_crossModulo.md          ← FKs; live = Flyway
├── _parcial/                          ← DBML por módulo (Etapa 2)
│   ├── iam.dbml
│   ├── academico.dbml
│   ├── solicitacoes.dbml              ← alinhado a V004+V018+V019
│   ├── formativas.dbml
│   ├── estagio.dbml
│   ├── tcc.dbml
│   ├── comunicacao.dbml
│   ├── presenca.dbml
│   └── certificado_auditoria.dbml
└── exports/
    └── modelo-conceitual-validacao.svg

backend/app/src/main/resources/db/migration/   ← Flyway V001–V019 (fonte de verdade)
```

---

## 8. Módulos (bounded contexts)

| Módulo | Tabelas (trilha 2026-06) | As-built Flyway | Arquivo parcial |
|--------|:------------------------:|-----------------|-----------------|
| M1 IAM + Sessão | 7 | + `password_history`; V012/V014/V015 extras em IAM/ops | `_parcial/iam.dbml` |
| M2 Acadêmico | 4 | + `historico_escolar` (V015) | `_parcial/academico.dbml` |
| M3 Solicitações | 5 (com line_item) | **4 + `request_type_version`**; sem line_item | `_parcial/solicitacoes.dbml` |
| M4 Formativas | 2 | 2 | `_parcial/formativas.dbml` |
| M5 Estágio | 2 | 2 | `_parcial/estagio.dbml` |
| M6 TCC | 3 | 3 | `_parcial/tcc.dbml` |
| M7 Comunicação/Outbox | 4 | + templates, `notification_log` (V014) | `_parcial/comunicacao.dbml` |
| M8 Presença v4.1 | 2 | 2 | `_parcial/presenca.dbml` |
| M9 Certificado/Auditoria | 2 | 2 | `_parcial/certificado_auditoria.dbml` |

Lista canônica de migrations: `as-built-backend.md` §5.1.

---

*Documentação gerada conforme pipeline PROMPT_gerar_documentacao_banco_dados.md v1.1; alinhada ao as-built Flyway em 2026-08-29.*
