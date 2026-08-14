# Prompt — Requisitos Funcionais e Não Funcionais (TCC — SecretariaOnline2)

**Modelo recomendado:** Claude Sonnet 4.6 **medium** (thinking)  
**Idioma de saída:** português do Brasil  
**Objetivo:** gerar em `foundationDocs/requisitos/` o documento formal de **Requisitos Funcionais (RF)** e **Requisitos Não Funcionais (RNF)** do SecretariaOnline2, rastreáveis às HUs, casos de uso, ADRs e análise arquitetural — pronto para inclusão no TCC.

**Prompt mestre:** execute **uma fase por chat** (§6). Não pule a Etapa 0 (inventário e convenções). A campanha completa cobre **51 HUs** + requisitos transversais.

---

## 0. Resultado da análise prévia (baseline — jun/2026)

### 0.1 O que já existe no repositório

| Artefato | Situação | Papel para RF/RNF |
|----------|----------|-------------------|
| 51 HUs em `foundationDocs/HUs/**/US-*.md` | ✅ Completo | **Fonte primária de RF** — CAs, RNs, APIs, capabilities |
| `foundationDocs/HUs/HUs.txt` | ✅ Consolidado | Visão única para varredura; mesma informação das HUs individuais |
| Índices por fase (`F0-INDICE.md`, `00-INDICE.md`) | ✅ 8 fases | Mapa HU → épico → tela → prioridade MVP |
| `analise_arquitetural_secretariaonline2.md` | ✅ Canônico | Contexto, legado (54 RF antigos), ADRs, checklist qualidade §17 |
| `fluxos_por_perfil.md` | ✅ Canônico | Fluxos narrativos F0–F8 + transversais §10 |
| `telas.md` | ✅ Canônico | 48 rotas novas, redução 158→48, propósito de cada tela |
| Diagramas de caso de uso | ✅ Completo | UC-AUT, UC-ALU, UC-SEC… por módulo F0–F8 |
| `sequenceDiagrams/` | ✅ 51+ HUs | Validação técnica; **não** inventar RF além do que HUs/fluxos definem |
| `endpoints_canonicos_presenca_eventos_v4.md` | ✅ v4.1 | RF de presença — modos QR/PIN, janelas, estados |
| MVP v1 / v2 | ✅ Escopo | Priorização P0/P1; walking skeleton; workflow engine |
| `.cursorrules` + `agents/*.md` | ✅ Stack | **Fonte primária de RNF** — segurança, performance, qualidade |
| `foundationDocs/requisitos/` | ❌ A criar | **Destino** desta campanha |

### 0.2 Fontes canônicas (ordem de precedência)

| Prioridade | Arquivo | Papel |
|:----------:|---------|-------|
| 1 | `foundationDocs/HUs/<Fase>/US-Fx-NNN-*.md` | RF derivados de histórias, RNs e CAs |
| 2 | `foundationDocs/HUs/HUs.txt` | Varredura consolidada quando a fase inteira for processada |
| 3 | `foundationDocs/analysis/fluxos_por_perfil.md` | Comportamento ponta a ponta; fluxos transversais §10 |
| 4 | `foundationDocs/analysis/telas.md` | Escopo de tela/rota; o que está **fora** do app |
| 5 | `foundationDocs/otherDiagrams/Diagrama de Caso de  Uso.md` | Descrição narrativa dos UCs por módulo |
| 6 | `foundationDocs/useCaseDiagrams/legenda_siglas_casos_de_uso_por_ator.md` | Mapeamento UC ↔ ator ↔ sigla |
| 7 | `foundationDocs/analysis/analise_arquitetural_secretariaonline2.md` | §1–3 visão; §10 presença; §11 certificados; §14 ADRs; §17 qualidade |
| 8 | `foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md` | Detalhe RF presença v4.1 |
| 9 | `foundationDocs/analysis/mvp_v1_walking_skeleton_aluno.md` | Prioridade MVP; RF marcados P0 |
| 10 | `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` | RF do motor genérico de solicitações |
| 11 | `foundationDocs/analysis/jpaInterfaces_PostgresEntities.md` | Entidades de domínio (apoio a RF de persistência) |
| 12 | `.cursorrules` | Stack, FGAC, Outbox, métricas P95, cobertura de testes |
| 13 | `agents/security-engineer.md` | RNF de segurança (JWT, Argon2, rate limit, auditoria) |
| 14 | `agents/backend-architect.md` | RNF arquiteturais (Clean Architecture, HATEOAS, RFC 7807) |
| 15 | `agents/workflow-engine-specialist.md` | RF transversal do RequestType / workflow |
| 16 | `agents/database-engineer.md` | RNF de dados (UUIDv7, Flyway, TIMESTAMPTZ, JSONB) |
| 17 | `agents/devops-engineer.md` | RNF operacionais (Docker, CI/CD, observabilidade) |
| 18 | `agents/ux-ui-specialist.md` | RNF de UX/a11y (WCAG 2.1 AA, tokens, responsivo 375px) |
| 19 | `foundationDocs/sequenceDiagrams/<Fase>/US-*.md` | Conferência de API/erros; não expandir escopo |

**Regra de conflito:** HU individual > `HUs.txt` > fluxos > análise arquitetural. Em divergência, registrar em `00-inventario-e-decisoes.md` e perguntar ao usuário.

### 0.3 Nomenclatura — evitar confusão

| Símbolo | Significa | Exemplo | **Não confundir com** |
|---------|-----------|---------|------------------------|
| `US-Fx-NNN` | História de usuário | `US-F5-005` | — |
| `RF-Fx-NNN` | Requisito funcional (novo) | `RF-F0-001` | ID da HU |
| `RNF-NN` | Requisito não funcional (novo) | `RNF-01` | RN-F0.1-01 (regra de negócio da HU) |
| `RN-Fx.y-NN` | Regra de negócio **dentro** da HU | `RN-F0.1-06` | RNF de qualidade |
| `Fx.y` | Tela / fluxo narrativo | `F5.11` | `US-F5-011` (Estatísticas) |
| `UC-XXX-NN` | Caso de uso (diagrama) | `UC-AUT-01` | RF direto (mapear 1:N) |

**Legado:** o sistema antigo tinha ~54 RF e RNF05/RNF06 citados na análise arquitetural. **Não reutilizar numeração legada** — criar numeração SO2 limpa com rastreio `legado: RF-XX` quando aplicável.

### 0.4 Mapa de fases → pastas → quantidade

| Fase | Pasta `HUs/` | Seção `fluxos_por_perfil` | HUs | Épicos principais |
|------|--------------|---------------------------|----:|-------------------|
| F0 | `F0 — Público/` | §1 | 7 | AUTH, PUB-VERIFY |
| F1 | `F1 — Aluno/` | §2 | 11 | Dashboard, solicitações, formativas, presença |
| F2 | `F2 — Egresso/` | §3 | 1 | Portal read-only pós-colacao |
| F3 | `F3 — Professor/` | §4 | 7 | Eventos, deliberar, orientação |
| F4 | `F4 — Comissões/` | §5 | 2 | CAAF, COE |
| F5 | `F5 — Secretaria/` | §6 | 12 | Cadastros, diplomas, import/export |
| F6 | `F6 — Coordenação/` | §7 | 2 | Config curso, relatórios |
| F7 | `F7 — Admin/` | §8 | 7 | IAM, workflow, outbox, auditoria |
| F8 | `F8 — Cross-cutting/` | §9 | 2 | Busca global, FAQ |
| — | Transversal | §10 | — | Outbox, certificados, notificações |

---

## 1. Instrução mestre (cole no chat do Cursor)

```
Você é analista de requisitos de software sênior, especialista em sistemas acadêmicos e documentação de TCC.

MODELO: Claude Sonnet 4.6 medium — seja rigoroso e completo; não invente funcionalidades ausentes das fontes.

ANTES DE ESCREVER:
1. Leia §0 deste prompt (fontes, nomenclatura, mapa de fases).
2. Leia a fase alvo indicada pelo usuário (§6) — todas as HUs da fase + § correspondente em fluxos_por_perfil.
3. Consulte as fontes na ordem de precedência (§0.2).
4. Não duplique RF: uma capacidade de negócio = um RF; CAs da mesma HU alimentam o mesmo RF ou subitens numerados RF-Fx-NNN-a, RF-Fx-NNN-b.
5. RNs (RN-Fx.y-NN) viram detalhamento ou critérios de aceitação do RF — não virem RNF salvo quando expressarem atributo de qualidade mensurável.

CONTEXTO OBRIGATÓRIO (anexar com @):
- Este prompt: @foundationDocs/prompts/PROMPT_gerar_requisitos_funcionais_e_nao_funcionais.md
- Análise arquitetural: @foundationDocs/analysis/analise_arquitetural_secretariaonline2.md
- Fluxos: @foundationDocs/analysis/fluxos_por_perfil.md (§ da fase)
- Telas: @foundationDocs/analysis/telas.md
- Índice da fase: @foundationDocs/HUs/<Fase>/*-INDICE.md ou 00-INDICE.md
- HUs da fase: @foundationDocs/HUs/<Fase>/US-*.md
- Casos de uso: @foundationDocs/useCaseDiagrams/legenda_siglas_casos_de_uso_por_ator.md
- Stack e qualidade: @.cursorrules
- Segurança: @agents/security-engineer.md

CONTEXTO CONDICIONAL (anexar se a fase exigir):
- Presença F1/F3/F5: @foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md
- Solicitações F1/F5/F7: @foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md + @agents/workflow-engine-specialist.md
- MVP priorização: @foundationDocs/analysis/mvp_v1_walking_skeleton_aluno.md
- Certificados F0/F1: analise_arquitetural §11
- DevOps/observabilidade F7: @agents/devops-engineer.md
- UX/a11y: @agents/ux-ui-specialist.md

TAREFA DA FASE ATUAL: [USUÁRIO INFORMA — ex.: "Etapa 2 — F0 Público" ou "Etapa 11 — RNF transversais"]

SAÍDA:
- Gravar em foundationDocs/requisitos/ conforme §4
- Atualizar matriz de rastreabilidade em 00-indice-requisitos.md
- Listar lacunas / conflitos em 00-inventario-e-decisoes.md
```

---

## 2. Definições e regras de derivação

### 2.1 O que é um Requisito Funcional (RF)

Capacidade observável que o sistema **deve oferecer** a um ator, independente de *como* implementar.

**Derivação:**
- 1 HU pode gerar **1 a 3 RFs** (agrupar por capacidade de negócio, não por CA).
- Cada RF deve ter: ID, nome, descrição (voz ativa), ator(es), prioridade (P0/P1/P2/P3), pré-condições, pós-condições, critérios de aceitação resumidos, rastreio.
- Fluxos de erro relevantes (401, 403, 429) entram como critérios do RF, não como RF separado — exceto verificadores públicos e anti-fraude (RF próprio).

**Agrupamentos típicos por HU:**
| Padrão na HU | Tratamento RF |
|--------------|---------------|
| Fluxo principal + variantes de erro | 1 RF com CAs numerados |
| Sub-funcionalidades independentes (ex.: US-F1-003 Perfil) | 1 RF por sub-funcionalidade |
| Regras RN-* repetidas em várias HUs | 1 RF transversal + referência DRY nas HUs |
| Telas estáticas (F0.4 Contato) | 1 RF simples de conteúdo estático |

### 2.2 O que é um Requisito Não Funcional (RNF)

Restrição ou atributo de qualidade **mensurável** que permeia o sistema.

**Categorias obrigatórias (ISO/IEC 25010 adaptado):**

| Cat. | Sigla | Fontes principais |
|------|-------|-------------------|
| Segurança | RNF-SEC | security-engineer, .cursorrules §Security, HUs IAM |
| Desempenho | RNF-DES | .cursorrules Success Metrics, analise §17 |
| Disponibilidade | RNF-DIS | devops-engineer, docker-compose |
| Usabilidade / Acessibilidade | RNF-UX | ux-ui-specialist, CAs a11y nas HUs |
| Manutenibilidade | RNF-MAN | analise §17.1–17.2, ADR-003 DRY |
| Confiabilidade | RNF-CON | Outbox, transações, Flyway |
| Portabilidade | RNF-POR | Stack multi-plataforma (Web + Mobile) |
| Compatibilidade | RNF-CMP | OpenAPI, navegadores, PostgreSQL 16 |
| Conformidade legal | RNF-LGL | LGPD (HUs perfil), auditoria |

**Cada RNF deve ter:** ID, categoria, descrição, métrica/limiar verificável, método de verificação (teste, ferramenta, revisão), fonte, prioridade.

**Exemplos de limiares já definidos no projeto (usar como RNF, não renegociar):**
- API listagem P95 < 300 ms; detalhe < 100 ms
- Login < 800 ms (inclui Argon2)
- Cobertura domínio ≥ 85%, aplicação ≥ 70%
- WCAG 2.1 AA; contraste 4.5:1
- Access token 15 min; refresh 7 dias rotativo
- Rate limit login: 5/min por IP+identificador
- Responsivo desde 375 px

### 2.3 O que NÃO virar RF/RNF

| Item | Motivo | Onde documentar |
|------|--------|-----------------|
| Decisão de biblioteca (ex.: TanStack Query) | Implementação | ADR / analise §4 |
| Nome de classe ou tabela | Implementação | jpaInterfaces / DB |
| Layout pixel-perfect Figma | Design | telasFigma/ |
| Passo a passo de diagrama de sequência | Design técnico | sequenceDiagrams/ |
| Seed de demonstração | Dados de teste | MVP v1 |

---

## 3. Formato de cada requisito

### 3.1 Template RF

```markdown
### RF-Fx-NNN — [Nome curto em verbo no infinitivo]

| Campo | Valor |
|-------|-------|
| **ID** | RF-Fx-NNN |
| **Nome** | [Nome] |
| **Prioridade** | P0 / P1 / P2 / P3 |
| **Ator(es)** | A1 Visitante, A2 Aluno, … |
| **Módulo** | F0 — Público |
| **Rastreio HU** | US-F0-001 |
| **Rastreio UC** | UC-AUT-01 |
| **Tela** | F0.1 `/login` |
| **API** | `POST /auth/login` |
| **Legado** | RF-XX (se mapeável) ou — |

**Descrição:** O sistema deve…

**Pré-condições:**
- …

**Pós-condições:**
- …

**Critérios de aceitação:**
1. …
2. …

**Regras de negócio relacionadas:** RN-F0.1-01, RN-F0.1-06, …

**Dependências:** RF-F0-00N, RNF-SEC-03
```

### 3.2 Template RNF

```markdown
### RNF-SEC-03 — Rate limiting em autenticação

| Campo | Valor |
|-------|-------|
| **ID** | RNF-SEC-03 |
| **Categoria** | Segurança |
| **Prioridade** | P0 |
| **Fonte** | US-F0-001 RN-F0.1-06; security-engineer.md |

**Descrição:** O sistema deve limitar tentativas de autenticação para mitigar brute force.

**Métrica:** Máximo 5 tentativas por minuto por par IP + identificador; HTTP 429 quando excedido.

**Verificação:** Teste de integração com Bucket4j; teste de carga controlado.

**RF relacionados:** RF-F0-001
```

---

## 4. Estrutura de saída em `foundationDocs/requisitos/`

```
foundationDocs/requisitos/
├── 00-indice-requisitos.md          # Índice geral + matriz HU→RF→UC
├── 00-inventario-e-decisoes.md      # Lacunas, conflitos, perguntas abertas
├── 01-requisitos-funcionais.md      # OU dividir por fase (ver abaixo)
├── 02-requisitos-nao-funcionais.md  # Todos os RNF por categoria
└── por-fase/                        # Opcional se o arquivo único ficar grande
    ├── RF-F0-publico.md
    ├── RF-F1-aluno.md
    ├── …
    └── RF-F8-cross-cutting.md
```

**Recomendação:** usar `por-fase/` quando `01-requisitos-funcionais.md` ultrapassar ~800 linhas.

### 4.1 Cabeçalho obrigatório de cada arquivo gerado

```markdown
# Requisitos Funcionais — Fase F0 (Público)

**Projeto:** SecretariaOnline2 — UFPR SEPT  
**Versão:** 1.0  
**Data:** AAAA-MM-DD  
**Gerado a partir de:** [listar HUs e docs-fonte]  
**Total RF neste arquivo:** N
```

### 4.2 Matriz de rastreabilidade (`00-indice-requisitos.md`)

| RF | HU | UC | Tela | Prioridade | Status |
|----|-----|-----|------|------------|--------|
| RF-F0-001 | US-F0-001 | UC-AUT-01 | F0.1 | P0 | ✅ |

Incluir também contagem: **RF total**, **RNF total**, **% cobertura HUs** (meta: 100%).

---

## 5. Requisitos transversais (gerar na Etapa 11)

Estes RFs não pertencem a uma única HU — extrair de `fluxos_por_perfil.md` §10, analise §7–11 e agents:

| ID sugerido | Tema | Fontes |
|-------------|------|--------|
| RF-TR-001 | Motor genérico de solicitações (19 tipos via RequestType) | ADR-003, MVP v2, US-F7-003 |
| RF-TR-002 | Hub de comunicação + entrega assíncrona (Outbox) | analise §7, US-F7-005 |
| RF-TR-003 | Emissão e verificação de certificados anti-fraude | analise §11, US-F0-007, US-F1-010 |
| RF-TR-004 | Auditoria imutável de comandos | US-F7-006, security-engineer |
| RF-TR-005 | Autorização FGAC + UI orientada a HATEOAS `_links` | ADR-002, .cursorrules |
| RF-TR-006 | BFF de dashboard contextual por perfil | fluxos, US-F1-001, US-F3-001, US-F5-001 |
| RF-TR-007 | Notificações push + e-mail com fallback | analise §7, comunicacao |
| RF-TR-008 | Presença em eventos formativos v4.1 (modos configuráveis) | endpoints v4.1, §10 analise |

---

## 6. Plano de execução por etapas

Execute **uma etapa por conversa** para manter qualidade. Ao final de cada etapa, atualize `00-indice-requisitos.md`.

| Etapa | Escopo | Arquivos @ obrigatórios adicionais |
|------:|--------|-------------------------------------|
| **0** | Convenções + `00-indice` + `00-inventario` vazios + estimativa de contagem RF/RNF | Este prompt + analise §1, §14, §17 |
| **1** | **RNF transversais** completos (`02-requisitos-nao-funcionais.md`) | `.cursorrules`, todos `agents/*.md` relevantes |
| **2** | RF **F0** (7 HUs) | `HUs/F0 — Público/*` |
| **3** | RF **F1** (11 HUs) | `HUs/F1 — Aluno/*`, mvp_v1 |
| **4** | RF **F2** (1 HU) | `HUs/F2 — Egresso/*` |
| **5** | RF **F3** (7 HUs) | `HUs/F3 — Professor/*`, endpoints presença |
| **6** | RF **F4** (2 HUs) | `HUs/F4 — Comissões/*` |
| **7** | RF **F5** (12 HUs) | `HUs/F5 — Secretaria/*` |
| **8** | RF **F6** (2 HUs) | `HUs/F6 — Coordenação/*` |
| **9** | RF **F7** (7 HUs) | `HUs/F7 — Admin/*`, mvp_v2, workflow-engine |
| **10** | RF **F8** (2 HUs) | `HUs/F8 — Cross-cutting/*` |
| **11** | RF **transversais** (§5) + revisão final | fluxos §10, analise §7–11 |
| **12** | Revisão de cobertura: toda HU tem ≥1 RF; todo RNF tem métrica; sem duplicata | `00-indice-requisitos.md` |

**Estimativa:** ~60–90 RF + ~35–50 RNF (ordem de grandeza; ajustar na Etapa 0).

---

## 7. Checklist de qualidade (aplicar antes de encerrar a campanha)

- [ ] 51/51 HUs mapeadas na matriz de rastreabilidade
- [ ] Nenhum RF sem ator e sem critério de aceitação testável
- [ ] Nenhum RNF sem métrica numérica ou critério verificável objetivo
- [ ] RNs não foram copiadas como RFs redundantes — foram consolidadas
- [ ] Prioridades P0 alinhadas a `mvp_v1` e US-F0-001, US-F1-001, US-F1-002
- [ ] Presença v4.1 não menciona geofence, trust score nem aula regular (fora de escopo)
- [ ] Certificados: sistema gera PDF — nunca upload externo (RF explícito)
- [ ] Numeração `RF-Fx-NNN` consistente dentro de cada fase (NNN sequencial)
- [ ] Numeração `RNF-{CAT}-{NN}` consistente por categoria
- [ ] Conflitos com legado documentados em `00-inventario-e-decisoes.md`
- [ ] Linguagem acadêmica adequada ao TCC (sem jargão de implementação excessivo)

---

## 8. Exemplo mínimo de saída (referência)

### RF-F0-001 — Autenticar usuário com identificador e senha

| Campo | Valor |
|-------|-------|
| **ID** | RF-F0-001 |
| **Prioridade** | P0 |
| **Ator(es)** | A1 Visitante, A2 Aluno, A4 Professor, A7 Secretaria, A8 Coordenador, A9 Admin |
| **Rastreio HU** | US-F0-001 |
| **Rastreio UC** | UC-AUT-01 |
| **Tela** | F0.1 `/login` |

**Descrição:** O sistema deve permitir que usuários cadastrados autentiquem-se informando identificador (e-mail institucional, e-mail pessoal ou GRR) e senha, recebendo tokens de acesso em caso de sucesso.

**Critérios de aceitação:**
1. Credenciais válidas com senha já alterada → emissão de access token (15 min) e refresh token (7 dias) e redirecionamento para `/inicio`.
2. Conta com `senha_alterada = false` → `mustChangePassword: true` e redirecionamento para `/primeiro-acesso` sem acesso ao dashboard.
3. Credenciais inválidas → HTTP 401 com mensagem genérica anti-enumeração.
4. Rate limit excedido → HTTP 429.
5. Eventos `iam.login_success` / `iam.login_failed` registrados em auditoria.

**Regras de negócio relacionadas:** RN-F0.1-01 a RN-F0.1-12

---

### RNF-SEC-01 — Armazenamento de senha com Argon2id

| Campo | Valor |
|-------|-------|
| **Categoria** | Segurança |
| **Prioridade** | P0 |

**Descrição:** O sistema deve armazenar senhas exclusivamente com Argon2id, sem uso de MD5 ou outros algoritmos legados.

**Métrica:** 100% dos hashes na coluna `senha` prefixados com `$argon2id$`; zero ocorrência de MD5 em código e banco após migração.

**Verificação:** Revisão de código + teste unitário de hash + auditoria de migração.

---

## 9. Modo fila (Cursor Automation / Loop)

Para processar todas as fases sem intervenção manual:

1. Criar fila em `foundationDocs/requisitos/QUEUE.md` com itens `Etapa 0` … `Etapa 12`.
2. Cada execução: pegar primeiro item `⬜`, executar, marcar `✅`, commit opcional.
3. Prompt curto por item: `Execute a [Etapa N] conforme @foundationDocs/prompts/PROMPT_gerar_requisitos_funcionais_e_nao_funcionais.md`

---

## 10. Referência rápida — arquivos para anexar com @

### Sempre (qualquer etapa)
```
@foundationDocs/prompts/PROMPT_gerar_requisitos_funcionais_e_nao_funcionais.md
@foundationDocs/analysis/analise_arquitetural_secretariaonline2.md
@foundationDocs/analysis/fluxos_por_perfil.md
@foundationDocs/analysis/telas.md
@.cursorrules
```

### Por fase (HUs)
```
@foundationDocs/HUs/F0 — Público/F0-INDICE.md
@foundationDocs/HUs/F0 — Público/US-F0-001-LOGIN.md
… (todas as HUs da fase)
```

### RNF e transversais
```
@agents/security-engineer.md
@agents/backend-architect.md
@agents/database-engineer.md
@agents/devops-engineer.md
@agents/ux-ui-specialist.md
@agents/workflow-engine-specialist.md
@foundationDocs/useCaseDiagrams/legenda_siglas_casos_de_uso_por_ator.md
@foundationDocs/otherDiagrams/Diagrama de Caso de  Uso.md
```

### Domínios específicos
```
@foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md    # presença
@foundationDocs/analysis/mvp_v1_walking_skeleton_aluno.md              # MVP P0
@foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md      # solicitações
@foundationDocs/analysis/jpaInterfaces_PostgresEntities.md             # entidades
```

---

*Última atualização: 2026-06-23 — v1.0*
