# Inventário, Lacunas e Decisões Abertas — SecretariaOnline2

**Projeto:** SecretariaOnline2 — UFPR SEPT  
**Versão:** 0.1 (Etapa 0 — Skeleton)  
**Data:** 2026-06-23  

> Este documento registra: (a) inventário dos artefatos-fonte confirmados, (b) lacunas identificadas, (c) conflitos entre fontes, (d) decisões tomadas pelos analistas, (e) perguntas abertas ao usuário/orientador. Atualizar a cada etapa da campanha.

---

## 1. Inventário de artefatos-fonte

### 1.1 Artefatos confirmados em disco ✅

| Artefato | Caminho | Situação | Papel |
|----------|---------|----------|-------|
| 51 HUs individuais | `foundationDocs/HUs/<Fase>/US-*.md` | ✅ Verificado (51 arquivos via Glob) | Fonte primária de RF |
| HUs.txt consolidado | `foundationDocs/HUs/HUs.txt` | ✅ Restaurado do git (estava deletado localmente) | Varredura consolidada |
| Análise arquitetural | `foundationDocs/analysis/analise_arquitetural_secretariaonline2.md` | ✅ 1696 linhas | Contexto, ADRs §14, qualidade §17 |
| As-built backend | `foundationDocs/analysis/as-built-backend.md` | ✅ 2026-08-29 | Contrato HTTP/persistência **como está no código**; vence a análise 2026-06 onde divergirem |
| Fluxos por perfil | `foundationDocs/analysis/fluxos_por_perfil.md` | ✅ | Fluxos narrativos F0–F8 + transversais §10 |
| Telas | `foundationDocs/analysis/telas.md` | ✅ | 48 rotas novas |
| Diagramas de caso de uso | `foundationDocs/useCaseDiagrams/casos_de_uso.md` | ✅ 510 linhas | UCs por módulo |
| Legenda siglas UCs | `foundationDocs/useCaseDiagrams/legenda_siglas_casos_de_uso_por_ator.md` | A verificar | Mapeamento UC ↔ ator ↔ sigla |
| Endpoints canônicos presença | `foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md` | A verificar | RF presença v4.1 |
| MVP v1 (walking skeleton) | `foundationDocs/analysis/mvp_v1_walking_skeleton_aluno.md` | A verificar | Prioridade P0 |
| MVP v2 (workflow engine) | `foundationDocs/analysis/mvp_v2_solicitacoes_workflow_engine.md` | A verificar | RF solicitações |
| Entidades JPA | `foundationDocs/analysis/jpaInterfaces_PostgresEntities.md` | A verificar | Apoio a RF de persistência |
| .cursorrules | `.cursorrules` | ✅ | Stack, FGAC, métricas P95, cobertura |
| security-engineer | `agents/security-engineer.md` | A verificar | RNF de segurança |
| backend-architect | `agents/backend-architect.md` | A verificar | RNF arquiteturais |
| database-engineer | `agents/database-engineer.md` | A verificar | RNF de dados |
| devops-engineer | `agents/devops-engineer.md` | A verificar | RNF operacionais |
| ux-ui-specialist | `agents/ux-ui-specialist.md` | A verificar | RNF de UX/a11y |
| workflow-engine-specialist | `agents/workflow-engine-specialist.md` | A verificar | RF motor solicitações |
| Sequence diagrams | `foundationDocs/sequenceDiagrams/<Fase>/US-*.md` | ✅ 51+ arquivos | Conferência de APIs |
| Diagrama Caso de Uso (texto) | `foundationDocs/otherDiagrams/Diagrama de Caso de Uso.md` | A verificar | Descrição narrativa UCs |

### 1.2 Artefatos a criar nesta campanha ❌

| Artefato | Caminho | Criado em |
|----------|---------|-----------|
| Índice geral | `foundationDocs/requisitos/00-indice-requisitos.md` | Etapa 0 ✅ |
| Inventário/decisões | `foundationDocs/requisitos/00-inventario-e-decisoes.md` | Etapa 0 ✅ |
| Fila de execução | `foundationDocs/requisitos/QUEUE.md` | Etapa 0 ✅ |
| RNF transversais | `foundationDocs/requisitos/02-requisitos-nao-funcionais.md` | Etapa 1 |
| RF F0 | `foundationDocs/requisitos/por-fase/RF-F0-publico.md` | Etapa 2 ✅ |
| RF F1 | `foundationDocs/requisitos/por-fase/RF-F1-aluno.md` | Etapa 3 ✅ |
| RF F2 | `foundationDocs/requisitos/por-fase/RF-F2-egresso.md` | Etapa 4 ✅ |
| RF F3 | `foundationDocs/requisitos/por-fase/RF-F3-professor.md` | Etapa 5 ✅ |
| RF F4 | `foundationDocs/requisitos/por-fase/RF-F4-comissoes.md` | Etapa 6 ✅ |
| RF F5 | `foundationDocs/requisitos/por-fase/RF-F5-secretaria.md` | Etapa 7 ✅ |
| RF F6 | `foundationDocs/requisitos/por-fase/RF-F6-coordenacao.md` | Etapa 8 ✅ |
| RF F7 | `foundationDocs/requisitos/por-fase/RF-F7-admin.md` | Etapa 9 ✅ |
| RF F8 | `foundationDocs/requisitos/por-fase/RF-F8-cross-cutting.md` | Etapa 10 ✅ |
| RF transversais | `foundationDocs/requisitos/por-fase/RF-TR-transversais.md` | Etapa 11 ✅ |

---

## 2. Lacunas identificadas

### LAC-001 — HUs.txt deletado localmente

**Detectada em:** Etapa 0  
**Descrição:** O arquivo `foundationDocs/HUs/HUs.txt` estava deletado na árvore de trabalho (git status mostrava `deleted`). A deleção estava não-staged, ou seja, o arquivo existe no index/HEAD mas não no diretório de trabalho.  
**Resolução:** Arquivo restaurado via `git restore foundationDocs/HUs/HUs.txt` antes de iniciar a Etapa 0. ✅  
**Impacto:** Nenhum — arquivo restabelecido para uso nas próximas etapas.

### LAC-002 — Índices de fase (F*-INDICE.md) a verificar

**Detectada em:** Etapa 0  
**Descrição:** O PROMPT referencia `foundationDocs/HUs/<Fase>/*-INDICE.md` ou `00-INDICE.md` como contexto obrigatório por fase. A presença desses arquivos não foi verificada nesta etapa.  
**Resolução pendente:** Verificar existência antes de cada etapa (2–10). Se ausentes, usar as HUs individuais como fonte primária e registrar aqui.  
**Impacto:** Sem impacto na Etapa 0; verificar na Etapa 2.

### LAC-003 — Diagramas de sequência com `[INSERIR DIAGRAMA]`

**Detectada em:** Etapa 0  
**Descrição:** Os arquivos de sequência das HUs (ex.: `US-F0-001-LOGIN.md` dentro de `HUs.txt`) contêm a marcação `[INSERIR DIAGRAMA DE SEQUÊNCIA AQUI]` em vez de diagramas Mermaid reais. Os diagramas estão separados em `foundationDocs/sequenceDiagrams/`.  
**Resolução:** Para derivação de RF, usar o texto da HU (CAs + RNs) como fonte. Os diagramas de sequência servem apenas para conferência de APIs/erros — não expandir escopo além do que as HUs definem.  
**Impacto:** Baixo — os CAs textuais são suficientes para derivação de RF.

### LAC-004 — Mapeamento UC ↔ RF pendente de validação

**Detectada em:** Etapa 0  
**Descrição:** Os IDs de UC (ex.: `UC-AUT-01`, `UC-ALU-01`) na matriz de rastreabilidade foram preenchidos provisoriamente com base nos padrões de nomenclatura observados. Precisam ser conferidos contra `foundationDocs/useCaseDiagrams/legenda_siglas_casos_de_uso_por_ator.md` em cada etapa.  
**Resolução pendente:** Cada etapa deve confirmar os IDs de UC ao gerar os RFs.  
**Impacto:** Médio — corrigir os IDs provisórios ao longo das Etapas 2–11.

---

## 3. Conflitos entre fontes

### CONF-001 — Numeração legada RF vs. nova nomenclatura SO2

**Identificado em:** Etapa 0  
**Descrição:** A análise arquitetural (§1) menciona "54 requisitos funcionais" do sistema legado e cita "RNF05" e "RNF06" em alguns pontos. A nomenclatura legada usa numeração simples (`RF-01` a `RF-54`, `RNF05`, `RNF06`).  
**Regra aplicada:** A numeração legada **não é reutilizada** no SO2. O novo sistema usa `RF-Fx-NNN` e `RNF-{CAT}-NN`. Quando houver correspondência, registrar `Legado: RF-XX` no campo rastreio.  
**Status:** Decisão tomada. ✅

### CONF-002 — "Primeiro acesso" na F0 ou F1?

**Identificado em:** Etapa 0  
**Descrição:** O fluxo de `mustChangePassword` envolve telas em `/login` (US-F0-001) e `/primeiro-acesso` (US-F1-002). A HU US-F0-001 cobre o redirecionamento; a HU US-F1-002 cobre o formulário de troca de senha.  
**Regra aplicada:** RF-F0-001 (login) inclui o CA de redirecionamento. RF-F1-002 (primeiro acesso) inclui o RF de troca de senha. Dependência explicitada.  
**Status:** Decisão tomada. ✅

### CONF-003 — Presença v4.1 aparece em F1, F3 e F5

**Identificado em:** Etapa 0  
**Descrição:** US-F1-009 (aluno confirma presença), US-F3-002 (professor gerencia eventos), US-F5-008 (secretaria gerencia eventos) tratam do mesmo domínio de presença sob perspectivas diferentes.  
**Regra aplicada:** Cada HU gera seu próprio RF com ator específico. RF-TR-008 (transversal) cobre as regras compartilhadas do módulo de presença v4.1. As RNs do endpoints_canonicos_v4.md são referenciadas em todos os três RFs.  
**Status:** Decisão tomada. ✅

### CONF-004 — Rate limit de login: 5/min (HU) vs. 15 min (agents/RNF)

**Identificado em:** Etapa 2  
**Descrição:** US-F0-001 (RN-F0.1-06, CA-04) e `fluxos_por_perfil.md` §1 F0.1 especificam **5 tentativas por minuto** por IP + identificador. `agents/security-engineer.md` e RNF-SEC-04 (Etapa 1) documentam **5 tentativas por 15 minutos**.  
**Regra aplicada:** Precedência HU > agents. RF-F0-001 e critérios de aceitação usam **5/min**. RNF-SEC-04 alinhado na Etapa 12.  
**Status:** Decisão tomada na Etapa 2; correção de RNF-SEC-04 aplicada. ✅

---

## 4. Decisões de derivação tomadas

### DEC-001 — Subdivisão de RF por sub-funcionalidade independente

**Contexto:** Algumas HUs (ex.: US-F1-003 Perfil) possuem sub-funcionalidades claramente independentes (visualizar dados, editar dados, alterar senha).  
**Decisão:** Sub-funcionalidades independentes de uma HU geram RFs distintos com sufixo sequencial: `RF-F1-003-a`, `RF-F1-003-b`. Sub-funcionalidades do mesmo fluxo (ex.: variantes de erro) são CAs do mesmo RF.  
**Impacto:** Pode aumentar o total de RFs além da estimativa base; atualizar contadores ao final de cada fase.

### DEC-002 — Fluxos de erro como critérios, não RFs separados

**Contexto:** Muitas HUs descrevem CAs de erro (401, 403, 404, 429, 500).  
**Decisão:** Respostas de erro de endpoints existentes entram como critérios de aceitação do RF principal. Exceção: verificadores públicos e certificados anti-fraude recebem RF próprio porque são funcionalidades de **negócio independente** (não apenas tratamento de erro).  
**Impacto:** Reduz proliferação de RFs; mantém foco em capacidades observáveis.

### DEC-003 — RNs não viram RNF salvo quando mensuram qualidade

**Contexto:** As HUs contêm regras de negócio (`RN-Fx.y-NN`) como "identificador normalizado antes do SELECT" ou "token JWT 1 uso com JTI".  
**Decisão:** RNs são incorporadas como critérios de aceitação ou detalhamento do RF. Apenas RNs que expressam **limite numérico** ou **atributo de qualidade** (ex.: "taxa de erros < 1%", "bloqueio após 10 falhas", "P95 < 300ms") são candidatas a RNF.  
**Impacto:** Evita explosão de RNFs; mantém distinção ISO 25010 clara.

### DEC-004 — Estrutura `por-fase/` adotada desde o início

**Contexto:** O PROMPT recomenda `por-fase/` quando `01-requisitos-funcionais.md` ultrapassar ~800 linhas. Com 51 HUs e ~75–90 RFs esperados, o arquivo único ficaria ~2.500–3.500 linhas.  
**Decisão:** Adotar estrutura `por-fase/` desde a Etapa 2, sem criar `01-requisitos-funcionais.md` unificado. O índice (`00-indice-requisitos.md`) serve como ponto de entrada centralizado.  
**Impacto:** Cada etapa gera 1 arquivo menor e navegável.

### DEC-005 — RF-F1-003-d criado para cobrir lacuna funcional em RNF-LGL-01

**Contexto:** O item (c) do RNF-LGL-01 ("permitir que o usuário exporte seus dados pessoais via tela de perfil") foi identificado na revisão de classificação de 2026-08-09 como uma capacidade funcional observável (tela, API, fluxo de dados) incorretamente documentada como atributo de qualidade de conformidade legal.  
**Decisão:** O item (c) foi removido do RNF-LGL-01 e substituído por referência a **RF-F1-003-d** (novo RF), que especifica a funcionalidade completa: tela `/perfil` seção Privacidade, `POST /me/data-export`, geração assíncrona de arquivo JSON/CSV, TTL 24h, limite de 1 exportação/dia. Aplica-se a alunos e egressos (US-F1-003, direito LGPD Art. 18, III).  
**Impacto:** Total de RFs aumenta de 71 para **72**; subdivisão DEC-001 de US-F1-003 passa de 3 para 4 itens (a, b, c, d).

---

## 5. Perguntas abertas ao usuário / orientador

| ID | Pergunta | Fase afetada | Prioridade | Status |
|----|----------|:------------:|:----------:|:------:|
| Q-001 | Os UC IDs provisórios na matriz estão corretos? | F0–F6 | Média | ✅ Resolvido para F0–F6 (ver matriz em `00-indice-requisitos.md`) |
| Q-002 | US-F1-011 e US-F5-007 são o mesmo domínio? | F1, F5 | Baixa | ✅ Capacidades distintas: aluno dá ciência (`service_record.view_own`); secretaria registra (`F5`). RFs separados. |
| Q-003 | US-F0-005 deve gerar RF formal? | F0 | Baixa | ✅ Sim — RF-F0-005 gerado na Etapa 2 |
| Q-004 | RF-TR-008 listado também em F1/F3/F5? | F1, F3, F5, TR | Média | ✅ RF-TR-008 é canônico; RF-F1-009, RF-F3-002, RF-F5-008 referenciam sem duplicar regras |

---

## 6. Log de alterações deste documento

| Data | Versão | Alteração | Etapa |
|------|--------|-----------|:-----:|
| 2026-06-23 | 0.1 | Criação inicial (Etapa 0) — inventário, LACs 001-004, CONFs 001-003, DECs 001-004, Qs 001-004 | 0 |
| 2026-06-23 | 0.2 | Etapa 2 — 7 RFs F0 em `por-fase/RF-F0-publico.md`; CONF-004 (rate limit); Q-001/Q-003 resolvidas para F0 | 2 |
| 2026-06-23 | 0.3 | Etapa 3 — 14 RFs F1; US-F1-003 e US-F1-005 subdivididos (DEC-001); Q-002 resolvida | 3 |
| 2026-06-23 | 0.4 | Etapa 4 — 1 RF F2 (dashboard read-only + reemissão); telas reaproveitadas referenciam RF-F1-003/010 | 4 |
| 2026-06-23 | 0.5 | Etapa 5 — 9 RFs F3; US-F3-002 e US-F3-003 subdivididos (DEC-001); UCs corrigidos (UC-DASH-01, UC-PRE-*, UC-SOL-04, etc.) | 5 |
| 2026-06-23 | 0.6 | Etapa 6 — 2 RFs F4 (pool CAAF/COE); UCs UC-FOR-04 e UC-EST-03; rotas `/comissoes/caaf` e `/comissoes/coe` | 6 |
| 2026-06-23 | 0.7 | Etapa 7 — 18 RFs F5; subdivisões US-F5-002/004/005/008 (DEC-001); UCs oficiais (UC-CAD-*, UC-SOL-*, UC-EGR-*, etc.) | 7 |
| 2026-06-23 | 0.8 | Etapa 8 — 2 RFs F6; UCs UC-CAD-05 e UC-ADM-09; F6.3 comissões delegado a F7 | 8 |
| 2026-06-23 | 0.9 | Etapa 9 — 8 RFs F7; US-F7-002 subdividido (DEC-001); matriz F7 corrigida (rotas, UCs UC-COM-03/ADM-04/05); RF-F7-007 P3 extra-MVP | 9 |
| 2026-06-23 | 1.0 | Etapa 10 — 2 RFs F8; UCs corrigidos UC-ADM-10 (busca) e UC-ADM-11 (suporte); rota `/buscar?q=`; **51/51 HUs cobertas** | 10 |
| 2026-06-23 | 1.1 | Etapa 11 — 8 RF-TR consolidados; mapa de consumo DRY; Q-004 resolvida (presença v4.1) | 11 |
| 2026-06-23 | 1.2 | Etapa 12 — Revisão final; CONF-004 resolvido (RNF-SEC-04 → 5/min); checklist §7 completo; campanha encerrada | 12 |
| 2026-08-09 | 1.3 | Revisão de classificação RNF×RF — 6 RNFs corrigidos (SEC-03/04, LGL-01/02, DIS-01/04); 4 RF-CAs corrigidos (F0-001, F0-003, TR-001/002/006); RF-F1-003-d criado (DEC-005); total RF: 71 → 72 | 13 |
| 2026-08-29 | 1.4 | Alinhamento as-built: inventário inclui `as-built-backend.md`; cookies/OTT/HATEOAS strings/V019 | — |

---

*Última atualização: 2026-08-29 — as-built-backend.md no inventário*
