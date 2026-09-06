# Prompt — Gerar diagramas de sequência (Mermaid)

**Modelo recomendado:** Claude Sonnet 4.6 **medium** (thinking)  
**Idioma de saída:** português do Brasil  
**Formato:** blocos ` ```mermaid ` com `sequenceDiagram` — **não** PlantUML para sequência  
**Objetivo:** cobertura **completa** dos 51 casos de uso (HUs) + 2 fluxos transversais, alinhados a UML 2.x, Clean Architecture e convenções do SecretariaOnline2.

**Prompt mestre:** invoque **uma HU por chat** (ou batch explícito por fase). Cada invocação gera **todos** os diagramas aplicáveis daquela HU. **Automação (Loop / Cursor Automation):** use **§7.5 Modo fila** — 1 execução = 1 item da fila no `sequenceDiagrams/README.md`.

---

## 0. Nomenclatura — evitar confusão de referências

| Símbolo | Significa | Exemplo | **Não confundir com** |
|---------|-----------|---------|------------------------|
| `US-Fx-NNN` | História de usuário (arquivo em `HUs/`) | `US-F5-005` | — |
| `Fx.y` | Tela / fluxo narrativo em `fluxos_por_perfil` e `telas.md` | `F2.1`, `F5.11` | ID da HU |
| `F5.11` | **Tela** Diplomas/Colação (`/secretaria/diplomas`) | HU **US-F5-005** | `US-F5-011` (Estatísticas) |
| `US-F5-011` | HU **Estatísticas** (tela F5.18) | leitura agregada | `F5.11` (diplomas) |
| `§10.1` / `§10.4` | Fluxos transversais (sequence) em `fluxos_por_perfil.md` | Após gerar: `sequenceDiagrams/transversal/` |
| `§12` em `fluxos` | **Flowcharts** por perfil | não é sequência | Diagramas de sequência |

**Trigger egresso (F2):** transição `ALUNO → EGRESSO` ocorre na **US-F5-005** (tela **F5.11**), não na US-F5-011.

### Mapa HU → fluxo narrativo → pasta

| Pasta `sequenceDiagrams/` | Seção `fluxos_por_perfil.md` | Qtd HUs |
|---------------------------|------------------------------|--------:|
| `F0/` | §1 F0 | 7 |
| `F1/` | §2 F1 | 11 |
| `F2/` | §3 F2 | 1 |
| `F3/` | §4 F3 | 7 |
| `F4/` | §5 F4 | 2 |
| `F5/` | §6 F5 | 12 |
| `F6/` | §7 F6 | 2 |
| `F7/` | §8 F7 | 7 |
| `F8/` | §9 F8 | 2 |
| `transversal/` | §10.1, §10.4 — **gerados** na campanha (§5.2 passo 0), fonte `fluxos` §10 |

§10.2 e §10.3 em `fluxos` são **stateDiagram** — não gerar como `sequenceDiagram`.

---

## 1. Instrução mestre (cole no chat do Cursor)

```
Você é especialista em diagramas de sequência fullstack para o SecretariaOnline2.

MODELO: Sonnet 4.6 medium — seja conciso; não explore código além do escopo indicado.

ANTES DE DESENHAR:
1. Leia §0 deste prompt (nomenclatura Fx.y vs US-Fx-NNN).
2. Leia §2 (camada fixa) + §3 (camada da HU alvo).
3. Siga `.cursor/skills/fullstack-sequence-diagrams/` (SKILL.md → reference.md → examples.md).
4. Um diagrama = um resultado. Máx. ~7 participantes e ~15 mensagens.
5. Layout: `participant`, sem `Note over`, sem `%%{init}%%`, sem `<br/>`; labels **completos** (wrap via `mermaid-live-config.json`); self-call espaçador se 1ª seta WebApp→API (SKILL.md §Wrap).

CONTEXTO:
- HU alvo: @foundationDocs/HUs/<Fase>/US-Fx-NNN-*.md
- Fluxo narrativo: @foundationDocs/analysis/fluxos_por_perfil.md (§ da fase, §0)
- Modo: cobertura completa da HU (§5.1)

COBERTURA (§5.1 — obrigatório):
1. Liste TODOS os CAs/RNs/sub-fluxos da HU.
2. Classifique cada um: SEQUENCIA | ERRO | DRY | NAO_APLICAVEL.
3. Monte matriz no cabeçalho do arquivo (planejado vs gerado).
4. Gere todos SEQUENCIA + ERRO; DRY → link; NAO_APLICAVEL → seção dedicada.
5. Tags OUTBOX/CERT: na HU desenhe só a fase TX; para dispatch/emissão completa, **link** para `transversal/10.x` **depois** que a campanha os gerar (§5.2 passo 0), ou cite `fluxos` §10 como fonte até existirem.

PADRÃO INDÚSTRIA (§6):
- UML 2.x: lifelines, mensagens síncronas `->>` / retorno `-->>`, `autonumber`.
- Camadas: participant → WebApp → Controller → UseCase → Postgres/MinIO → adapters.
- REST + HATEOAS; erros RFC 7807 Problem Details; FGAC em diagrama 403 separado.

CABEÇALHO DO ARQUIVO (obrigatório):
# US-Fx-NNN — [título]
| HU | Tela Fx.y | Capability | API primária | Fonte |
## Matriz de cobertura
| ID diagrama | Origem (CA/RN) | Status |
## Referências DRY
## Fora de sequência

ENTREGÁVEL (por diagrama):
## [Fx.y id] — [Título] (happy path | erro 403 | fase TX)

**Escopo:** …
**Pré-condições:** …

```mermaid
sequenceDiagram
  autonumber
  box #e8f4fc Cliente
  ...
  box #fff8ee Servidor
```

(Config: `sequenceDiagrams/mermaid-live-config.json` na aba Config do mermaid.live)

**Notas:** 2–3 bullets
**Lacunas:** … ou nenhuma

SALVAR (§9):
- `foundationDocs/sequenceDiagrams/<Fase>/US-Fx-NNN-<SLUG>.md`
- Atualizar `foundationDocs/sequenceDiagrams/README.md`
```

---

## 2. Camada fixa — ler SEMPRE

| # | Arquivo | Para quê |
|:-:|---------|----------|
| 1 | `.cursor/skills/fullstack-sequence-diagrams/SKILL.md` | Workflow + layout |
| 2 | `.cursor/skills/fullstack-sequence-diagrams/reference.md` | Templates P1–P7 + layout |
| 3 | `.cursor/skills/fullstack-sequence-diagrams/examples.md` | Deliberação, 403 FGAC |
| 4 | `foundationDocs/analysis/fluxos_por_perfil.md` **§0** + **§10** (transversais) + **§13** (DRY) | Outbox, certificado, reaproveitamento |
| 5 | `jpaInterfaces_PostgresEntities.md` | Tabelas e repositórios |
| 6 | `sequenceDiagrams/README.md` | Índice e progresso |

Se HU tem tag `OUTBOX` ou `CERT`: leia **`fluxos` §10.1 ou §10.4** como fonte narrativa. Se `sequenceDiagrams/transversal/10.x` já existir (campanha passo 0), use-o para link DRY.

---

## 3. Camada do fluxo — por HU

### 3.1 Obrigatório

| Arquivo | Quando |
|---------|--------|
| `HUs/<Fase>/US-Fx-NNN-*.md` | **Sempre** — CAs, RNs, API §5 |
| `foundationDocs/analysis/fluxos_por_perfil.md` — seção do perfil (§0 mapa) | Sub-fluxos e triggers |
| `<Fase>-INDICE.md` ou `00-INDICE.md` | Contexto épico / telas |

Uma HU por invocação. Catálogo completo: **§5**.

### 3.2 Suplementos por tag (§5 coluna Suplementos)

| Tag | Ler adicional |
|-----|---------------|
| `AUTH` | `fluxos` §1 F0.1–F0.3 · `reference.md` §P1 |
| `BFF` | `reference.md` §P7 |
| `HUB` | `analise` §7 |
| `OUTBOX` | `fluxos` §10.1 · `analise` §7+§9.3 · HU = TX local + link `transversal/10.1` quando existir |
| `WORKFLOW` | `fluxos` §10.2 (state — referência) · `analise` §3–5 |
| `MINIO` | `reference.md` §P5 |
| `PRESENCA` | `endpoints_canonicos_presenca_eventos_v4.md` · `analise` §10 · `reference.md` §P6 |
| `CERT` | `fluxos` §10.4 · `analise` §11 · HU = reemissão local; emissão → link `transversal/10.4` quando existir |
| `PUBLIC` | Sem JWT — verificadores `/publico/*` |
| `DRY` | `fluxos` §13 + arquivo da HU base |

### 3.3 Backend (opcional, pós-MVP)

Só se OpenAPI/código existir e HU não tiver método+path explícito.

---

## 4. O que NÃO ler

| Evitar | Motivo |
|--------|--------|
| `telas.md` inteiro | Trecho `### Fx.y` só se HU sem API |
| `analise_arquitetural` inteiro | §3, §7, §9.3, §10, §11 conforme tag |
| `useCaseDiagrams/*.puml` | UML caso de uso ≠ sequência |
| 51 HUs num único chat | **1 HU = 1 invocação**; “batch por fase” = repetir 7–12 chats, não gerar a fase inteira de uma vez |
| Redesenhar 10.1/10.4 em cada HU | Link DRY para `transversal/` |

---

## 5. Catálogo — 51 HUs

**Legenda suplementos:** §3.2.

**Índices:** `F0-INDICE.md` · `F1-INDICE.md` · `F2-INDICE.md` · `F3–F8/00-INDICE.md`

### F0 — Público (7) · `fluxos` §1

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F0-001 | `HUs/F0 — Público/US-F0-001-LOGIN.md` | `AUTH` |
| US-F0-002 | `HUs/F0 — Público/US-F0-002-RECUPERAR-SENHA.md` | `AUTH` · `OUTBOX` |
| US-F0-003 | `HUs/F0 — Público/US-F0-003-NOVA-SENHA.md` | `AUTH` |
| US-F0-004 | `HUs/F0 — Público/US-F0-004-CONTATO.md` | — (estático; 0 diagrama OK) |
| US-F0-005 | `HUs/F0 — Público/US-F0-005-ERRO.md` | — (UI; 0–1 se Problem Details API) |
| US-F0-006 | `HUs/F0 — Público/US-F0-006-VERIFICAR-PROTOCOLO.md` | `PUBLIC` |
| US-F0-007 | `HUs/F0 — Público/US-F0-007-VERIFICAR-CERTIFICADO.md` | `PUBLIC` · `CERT` |

### F1 — Aluno (11) · `fluxos` §2

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F1-001 | `HUs/F1 — Aluno/US-F1-001-DASHBOARD.md` | `BFF` |
| US-F1-002 | `HUs/F1 — Aluno/US-F1-002-PRIMEIRO-ACESSO.md` | `AUTH` |
| US-F1-003 | `HUs/F1 — Aluno/US-F1-003-PERFIL.md` | `MINIO` · `HUB` |
| US-F1-004 | `HUs/F1 — Aluno/US-F1-004-COMUNICACAO.md` | `HUB` |
| US-F1-005 | `HUs/F1 — Aluno/US-F1-005-SOLICITACOES.md` | `WORKFLOW` · `MINIO` |
| US-F1-006 | `HUs/F1 — Aluno/US-F1-006-FORMATIVAS.md` | `MINIO` · `CERT` |
| US-F1-007 | `HUs/F1 — Aluno/US-F1-007-ESTAGIO.md` | `MINIO` · `WORKFLOW` |
| US-F1-008 | `HUs/F1 — Aluno/US-F1-008-TCC.md` | `MINIO` · `WORKFLOW` |
| US-F1-009 | `HUs/F1 — Aluno/US-F1-009-PRESENCA.md` | `PRESENCA` · `CERT` |
| US-F1-010 | `HUs/F1 — Aluno/US-F1-010-CERTIFICADOS.md` | `CERT` · `MINIO` |
| US-F1-011 | `HUs/F1 — Aluno/US-F1-011-ATENDIMENTOS.md` | `HUB` |

### F2 — Egresso (1) · `fluxos` §3

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F2-001 | `HUs/F2 — Egresso/US-F2-001-DASHBOARD-EGRESSO.md` | `MINIO` · `DRY` → US-F1-003, US-F1-010 · trigger **US-F5-005** (tela F5.11) |

### F3 — Professor (7) · `fluxos` §4

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F3-001 | `HUs/F3 — Professor/US-F3-001-DASHBOARD.md` | `BFF` |
| US-F3-002 | `HUs/F3 — Professor/US-F3-002-EVENTOS.md` | `PRESENCA` |
| US-F3-003 | `HUs/F3 — Professor/US-F3-003-DELIBERAR-SOLICITACOES.md` | `WORKFLOW` · `OUTBOX` |
| US-F3-004 | `HUs/F3 — Professor/US-F3-004-REVISAR-FORMATIVAS.md` | `OUTBOX` · `CERT` |
| US-F3-005 | `HUs/F3 — Professor/US-F3-005-ESTAGIO-ORIENTACAO.md` | `WORKFLOW` |
| US-F3-006 | `HUs/F3 — Professor/US-F3-006-TCC-ORIENTACAO.md` | `WORKFLOW` |
| US-F3-007 | `HUs/F3 — Professor/US-F3-007-PUBLICAR-COMUNICADO.md` | `HUB` · `OUTBOX` |

### F4 — Comissões (2) · `fluxos` §5

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F4-001 | `HUs/F4 — Comissões/US-F4-001-COMISSAO-CAAF.md` | `OUTBOX` · `DRY` → US-F3-004 |
| US-F4-002 | `HUs/F4 — Comissões/US-F4-002-COMISSAO-COE.md` | `OUTBOX` · `DRY` → US-F3-005 |

### F5 — Secretaria (12) · `fluxos` §6

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F5-001 | `HUs/F5 — Secretaria/US-F5-001-DASHBOARD.md` | `BFF` |
| US-F5-002 | `HUs/F5 — Secretaria/US-F5-002-SOLICITACOES.md` | `WORKFLOW` · `DRY` → US-F3-003 |
| US-F5-003 | `HUs/F5 — Secretaria/US-F5-003-GESTAO-ALUNOS.md` | `AUTH` |
| US-F5-004 | `HUs/F5 — Secretaria/US-F5-004-DADOS-ACADEMICOS.md` | — |
| US-F5-005 | `HUs/F5 — Secretaria/US-F5-005-EGRESSOS-DIPLOMAS.md` | `OUTBOX` · telas **F5.10, F5.11** · dispara **US-F2-001** |
| US-F5-006 | `HUs/F5 — Secretaria/US-F5-006-AUTORIZACOES-IMAGEM.md` | `WORKFLOW` |
| US-F5-007 | `HUs/F5 — Secretaria/US-F5-007-ATENDIMENTOS.md` | `MINIO` |
| US-F5-008 | `HUs/F5 — Secretaria/US-F5-008-EVENTOS.md` | `PRESENCA` · `DRY` → US-F3-002 |
| US-F5-009 | `HUs/F5 — Secretaria/US-F5-009-IMPORTACOES.md` | `MINIO` |
| US-F5-010 | `HUs/F5 — Secretaria/US-F5-010-EXPORTACOES.md` | `OUTBOX` |
| US-F5-011 | `HUs/F5 — Secretaria/US-F5-011-ESTATISTICAS.md` | — (tela **F5.18**, não F5.11) |
| US-F5-012 | `HUs/F5 — Secretaria/US-F5-012-TAREFAS.md` | — |

### F6 — Coordenação (2) · `fluxos` §7

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F6-001 | `HUs/F6 — Coordenação/US-F6-001-CONFIGURAR-CURSO.md` | — |
| US-F6-002 | `HUs/F6 — Coordenação/US-F6-002-RELATORIOS.md` | `DRY` → US-F5-011 |

### F7 — Admin (7) · `fluxos` §8

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F7-001 | `HUs/F7 — Admin/US-F7-001-IAM-USUARIOS.md` | `AUTH` |
| US-F7-002 | `HUs/F7 — Admin/US-F7-002-IAM-PERFIS-AUTORIDADES.md` | FGAC |
| US-F7-003 | `HUs/F7 — Admin/US-F7-003-WORKFLOW-ENGINE.md` | `WORKFLOW` |
| US-F7-004 | `HUs/F7 — Admin/US-F7-004-TEMPLATES-COMUNICACAO.md` | `HUB` |
| US-F7-005 | `HUs/F7 — Admin/US-F7-005-JOBS-OUTBOX.md` | `OUTBOX` |
| US-F7-006 | `HUs/F7 — Admin/US-F7-006-AUDIT-LOG.md` | `audit_log` |
| US-F7-007 | `HUs/F7 — Admin/US-F7-007-SAUDE-SISTEMA.md` | observabilidade |

### F8 — Cross-cutting (2) · `fluxos` §9

| ID | Arquivo | Suplementos |
|----|---------|-------------|
| US-F8-001 | `HUs/F8 — Cross-cutting/US-F8-001-BUSCA-GLOBAL.md` | fan-out por capability |
| US-F8-002 | `HUs/F8 — Cross-cutting/US-F8-002-SUPORTE-FAQ.md` | `WORKFLOW` |

**Base:** `foundationDocs/HUs/`

---

### 5.1 Cobertura por HU (todos os diagramas aplicáveis)

#### Classificação de CAs/RNs

| Classe | Critério | Ação |
|--------|----------|------|
| **SEQUENCIA** | HTTP, TX, MinIO, outbox, FGAC no backend | 1 diagrama (happy ou fase TX) |
| **ERRO** | 401, 403, 409, 422, 429 com lógica backend | diagrama **separado** |
| **DRY** | Igual a outra HU ou `fluxos` §13 | Link — **não** duplicar Mermaid |
| **NAO_APLICAVEL** | Skeleton, EmptyState, a11y, responsividade | `## Fora de sequência` |

#### Mínimos por tag

| Tag | Diagramas na HU | Transversal |
|-----|-----------------|-------------|
| `AUTH` | login · refresh · 401 · 429 · bloqueio | — |
| `OUTBOX` | mutação + INSERT outbox na TX | **link → 10.1** |
| `CERT` emissão | trigger local se CA | **link → 10.4** |
| `CERT` reemissão | presigned download na HU | não é 10.4 |
| `WORKFLOW` | transições com API por CA | state §10.2 só referência |
| `PRESENCA` | confirmar · 403 janela · dual se CA | — |
| `PUBLIC` | GET verificador · 404 | — |
| Estático | 0 diagrama | — |

#### Calibragem US-F2-001

| ID | Origem | Ação |
|----|--------|------|
| F2.1 | CA-02 `GET /alumni/me` | SEQUENCIA |
| F2.1a | RN-F2.1-10 `_links.download` diploma | SEQUENCIA |
| F2.1b | CA-03 reemitir certificado | SEQUENCIA |
| F2.1c | CA-04 403 rota aluno | ERRO |
| — | CA-05 perfil/certificados | DRY → US-F1-003, US-F1-010 |
| — | Trigger egresso | DRY → US-F5-005 (tela F5.11) |
| — | CA-06, CA-07 | NAO_APLICAVEL |

#### Calibragem US-F5-005 (dispara F2)

| ID | Origem | Ação |
|----|--------|------|
| F5.11 | Registrar colação/diploma · `diploma.register` | SEQUENCIA |
| F5.11b | Revogar caps aluno + `alumni.view_own` | SEQUENCIA (mesmo ou após 11) |
| F5.11c | `outbox_event` egresso graduado | TX + **link 10.1** |

---

### 5.2 Campanha — cobertura total do sistema

| Etapa | Ação | Meta |
|-------|------|-----|
| **0** | **Gerar** transversais a partir de `fluxos` §10.1 e §10.4 → salvar em `sequenceDiagrams/transversal/` (skill layout) | 2 arquivos |
| 1 | Uma invocação por HU (F0→F8) | 51 arquivos |
| 2 | Atualizar `sequenceDiagrams/README.md` | 51 + 2 transversais |
| 3 | Link nas HUs (opcional) | `## Diagramas de sequência` |

**Modo fila (Loop / Automation):** §7.5 — README como fonte da verdade; **não** pular passo 0; **não** promover `parcial` → `feito` sem matriz §5.1 completa.

**Estimativa:** ~180–220 diagramas `sequenceDiagram` (média 3–5 por HU; F0-004/F0-005 podem ter 0).

**Comando por fase** (11 invocações separadas — **não** juntar HUs num único chat):

```
@foundationDocs/prompts/promptParaGerarDiagramasDeSequencia.md
@.cursor/skills/fullstack-sequence-diagrams/

Repita para cada HU da fase F1 (§5 catálogo): US-F1-001 … US-F1-011.
Cada chat: cobertura completa §5.1 de **uma** HU → salvar em sequenceDiagrams/F1/ → atualizar README ao final da fase.
```

---

## 6. Modelagem e padrão indústria

### Camadas (Clean Architecture / UML 2.x)

`participant` → `WebApp` / `MobileApp` → `*Controller` / `BFF` → `*UseCase` → `Postgres` / `MinIO` → `OutboxDispatcher` / adapters

### Mensagens

- HTTP: `METHOD /path` → `STATUS {campos}` (paths da HU; não prefixar `/api/v1` se a HU não usar)
- TX: `BEGIN` … `COMMIT` na mesma lifeline UC→Postgres
- FGAC: `(capability ✓)` inline **ou** diagrama 403 separado
- HATEOAS: `_links` quando UI usa `useActions`
- Erros: RFC 7807 — tipo shorthand na seta; corpo em **Notas**

### Referências de padrão

| Tópico | Padrão |
|--------|--------|
| Diagrama | UML 2.5 Sequence Diagram |
| API | REST Level 3 (HATEOAS), OpenAPI 3.x |
| Erros | RFC 7807 Problem Details |
| Auth | JWT access 15min + refresh rotativo (OWASP) |
| Async | Transactional Outbox (microservices.io) |
| Arquivos | S3-compatible presigned (MinIO) |
| Acesso | FGAC + `@PreAuthorize` |

### Anti-padrões

- `alt` com 5+ ramos → dividir diagramas
- `actor` humano → `participant`
- `Note over` em docs → **Notas** markdown
- `<br/>` ou `%%{init:%%` em mensagens → overlap / Syntax Error no Cursor; usar single-line + Notas
- Redesenhar 10.1/10.4 em cada HU OUTBOX/CERT
- Confundir **F5.11** (tela) com **US-F5-011** (HU)

---

## 7. Exemplos de invocação

### 7.1 HU completa

```
@foundationDocs/prompts/promptParaGerarDiagramasDeSequencia.md
@.cursor/skills/fullstack-sequence-diagrams/
@foundationDocs/HUs/F2 — Egresso/US-F2-001-DASHBOARD-EGRESSO.md
@foundationDocs/analysis/fluxos_por_perfil.md (§3 F2)

Cobertura completa US-F2-001 (§5.1). Matriz + DRY + salvar em sequenceDiagrams/F2/.
```

### 7.2 Campanha — passo 0 (transversais)

```
@foundationDocs/prompts/promptParaGerarDiagramasDeSequencia.md
@foundationDocs/analysis/fluxos_por_perfil.md (§10.1 e §10.4 apenas)
@.cursor/skills/fullstack-sequence-diagrams/

Gere e salve os 2 diagramas transversais (layout skill) em sequenceDiagrams/transversal/.
Fonte: fluxos §10 — não inventar participantes além do narrado.
```

### 7.3 OUTBOX (TX na HU + link após passo 0)

```
@foundationDocs/HUs/F3 — Professor/US-F3-003-DELIBERAR-SOLICITACOES.md
@foundationDocs/analysis/fluxos_por_perfil.md (§10.1)

Diagramas na HU: (A) deliberar TX+outbox; (B) 403 FGAC.
Dispatch async: link transversal/10.1 se existir; senão cite fluxos §10.1 em Referências DRY.
```

### 7.4 Transição egresso

```
@foundationDocs/HUs/F5 — Secretaria/US-F5-005-EGRESSOS-DIPLOMAS.md
@foundationDocs/HUs/F2 — Egresso/US-F2-001-DASHBOARD-EGRESSO.md (só trigger)

Cobertura US-F5-005 tela F5.11: colação, role EGRESSO, outbox.
Citar US-F2-001 como efeito downstream (DRY).
```

### 7.5 Modo fila — 1 execução = 1 item (Loop / Automation)

**Objetivo:** mesma qualidade que §7.1 manual, sem colar 52 prompts. A fila vive em `foundationDocs/sequenceDiagrams/README.md` (seção **Fila da campanha**).

**Cole em cada tick (Loop) ou em cada run (Automation):**

```
@foundationDocs/prompts/promptParaGerarDiagramasDeSequencia.md
@.cursor/skills/fullstack-sequence-diagrams/
@foundationDocs/sequenceDiagrams/README.md
@foundationDocs/analysis/fluxos_por_perfil.md

Execute §7.5 Modo fila — **uma** execução, **um** item da fila. Pare ao terminar; não inicie o próximo item.
```

#### Algoritmo (obrigatório — ordem fixa)

1. **Ler** `sequenceDiagrams/README.md` → tabela **Fila da campanha**.
2. **Passo 0 (gate):** se **qualquer** transversal (10.1 ou 10.4) estiver `pendente` ou `parcial` → executar **somente** §7.2 (gerar/completar os dois arquivos em `transversal/`). Atualizar README. **Parar.** Não processar HUs.
3. **HU:** escolher a **primeira** linha (ordem crescente) com status `parcial` ou `pendente`. Se só existir `feito` → responder "Campanha completa" e parar.
4. **Carregar** a HU: caminho em `foundationDocs/<arquivo HU>` da fila + `fluxos_por_perfil.md` (§ da fase).
5. **Executar** cobertura §5.1 (igual §7.1): matriz, diagramas, DRY, salvar em `sequenceDiagrams/<Fase>/`.
6. **Validar** antes de atualizar status (ver critérios abaixo).
7. **Atualizar README:** status da linha + métricas **Cobertura** + `Observação` se ainda faltar algo.
8. **Parar.** Na resposta final, informar: item processado, novo status, **próximo** item pendente (ou "nenhum").

#### Atomicidade (não negociável)

| Regra | Motivo |
|-------|--------|
| **1 item por execução** | Evita megachat e truncamento |
| **Não** processar HU se passo 0 incompleto | DRY 10.1/10.4 |
| **Não** iniciar segunda HU na mesma resposta | Paridade com manual |
| `parcial` tem **prioridade** sobre `pendente` | Completar F2-001 antes de F0-001 |
| **Não** alterar linhas já `feito` | Idempotência |

#### Status no README

| Status | Significado | Pode avançar fila? |
|--------|-------------|-------------------|
| `pendente` | Nada gerado ou arquivo inexistente | Não — processar esta linha |
| `parcial` | Arquivo existe; matriz §5.1 **incompleta** (ex.: falta sub-fluxo) | Não — **completar** antes da próxima HU |
| `feito` | Matriz §5.1 satisfeita (todos SEQUENCIA/ERRO gerados; DRY linkados; NAO_APLICAVEL listado) | Sim |

**Promover para `feito` somente se:** no arquivo gerado, coluna **Status** da matriz = `gerado` (ou `DRY` com link) para **todos** os itens SEQUENCIA e ERRO. Caso contrário → manter `parcial` e registrar lacuna em **Observação**.

**Promover `parcial` → `feito`:** só após run que **fecha** as lacunas da Observação (revalidar matriz).

#### Equivalência com manual

Cada execução do modo fila deve produzir o **mesmo artefato** que §7.1: mesmo cabeçalho, matriz, layout skill, persistência. Diferença aceitável: redação do modelo; **não** aceitável: pular CAs, omitir matriz, marcar `feito` com lacunas.

#### Loop vs Automation

| Superfície | Recomendação |
|------------|--------------|
| **Cursor Automation** (1 run = 1 disparo) | **Preferido** — contexto fresco ≈ chat novo por HU |
| **Loop** (`/loop 5m …`) | OK com **mesmo** prompt §7.5; **reinicie o chat** a cada ~5 HUs ou ao notar respostas mais curtas |
| **Mesmo chat, "continue fila"** | Aceitável se você repetir o bloco §7.5; trate como Loop |

#### Saída esperada ao fim de cada execução

```markdown
## Execução fila
- **Item:** US-Fx-NNN (ou 10.1+10.4)
- **Status:** pendente → feito | parcial → feito | parcial (lacuna: …)
- **Arquivo:** sequenceDiagrams/…
- **Próximo:** US-Fy-NNN | passo 0 | campanha completa
```

---

## 8. Checklist

**Cobertura:** matriz completa · CAs SEQUENCIA/ERRO cobertos · DRY linkados · NAO_APLICAVEL listado · sub-fluxos `fluxos` §fase

**Qualidade:** camadas · setas rotuladas · tabelas JPA · ≤15 msgs · erros separados · `autonumber` · lacunas · um bloco mermaid (`box #hex`, config em mermaid-live-config.json) · layout (`participant`, labels completos, sem padding invisível nos labels, sem `<br/>`/`%%{init}%%`, sem `Note over`)

**Persistência:** arquivo em `sequenceDiagrams/<Fase>/` · README atualizado · transversais não duplicados

**Modo fila (§7.5):** 1 item/execução · passo 0 antes das HUs · `parcial` não vira `feito` sem matriz completa · bloco **Execução fila** na resposta

---

## 9. Onde salvar

| Destino | Regra |
|---------|-------|
| `sequenceDiagrams/F0/` … `F8/` | 1 arquivo por HU |
| `sequenceDiagrams/transversal/` | 10.1, 10.4 — **saída** campanha passo 0 (gerados pelo prompt, não à mão) |
| `foundationDocs/analysis/fluxos_por_perfil.md` §10 | Fonte narrativa; Mermaid permanece até passo 0 copiar para `sequenceDiagrams/` |
| `fluxos` §12 | Flowchart — não migrar |
| HU `## Diagramas de sequência` | Link relativo para `sequenceDiagrams/` |

---

**Versão:** 1.5.3 — 2026-06-20  
**Changelog:** v1.5.3 — §7.5 Modo fila (README, atomicidade, status) · README fila completa  
v1.5.2 — caminhos `fluxos` unificados · DRY `fluxos` §13 · batch por fase = N invocações (1 HU/chat)
