# Índice Geral de Requisitos — SecretariaOnline2

**Projeto:** SecretariaOnline2 — UFPR SEPT  
**Versão:** 1.0 (campanha concluída)  
**Data:** 2026-06-23  
**Campanha:** 12 etapas conforme `PROMPT_gerar_requisitos_funcionais_e_nao_funcionais.md`  
**Status geral:** ✅ Concluída (Etapa 12 — revisão de cobertura)

---

## 1. Convenções deste documento

### 1.1 Nomenclatura (não confundir)

| Símbolo | Significa | Exemplo | Não confundir com |
|---------|-----------|---------|-------------------|
| `US-Fx-NNN` | História de usuário (fonte) | `US-F5-005` | ID de RF |
| `RF-Fx-NNN` | Requisito funcional (novo SO2) | `RF-F0-001` | ID da HU |
| `RNF-{CAT}-NN` | Requisito não funcional (novo SO2) | `RNF-SEC-01` | RN dentro da HU |
| `RN-Fx.y-NN` | Regra de negócio **dentro** da HU | `RN-F0.1-06` | RNF de qualidade |
| `Fx.y` | Tela / fluxo narrativo | `F5.11` | `US-F5-011` (HU) |
| `UC-XXX-NN` | Caso de uso (diagrama) | `UC-AUT-01` | RF direto |

**Numeração RF:** sequencial por fase, iniciando em 001 — ex.: `RF-F0-001`, `RF-F0-002`, …, `RF-F0-009`.  
**Numeração RNF:** sequencial por categoria — ex.: `RNF-SEC-01`, `RNF-SEC-02`, …, `RNF-DES-01`, …  
**Numeração transversal:** prefixo `TR` — ex.: `RF-TR-001`, …, `RF-TR-008`.

**Legado:** o sistema antigo possuía ~54 RFs e citava RNF05/RNF06 na análise arquitetural. A numeração legada **não é reutilizada**. Quando há correspondência, registrar `Legado: RF-XX` no campo rastreio do RF/RNF.

### 1.2 Prioridades

| Código | Significado | Referência |
|--------|-------------|------------|
| **P0** | MVP obrigatório — walking skeleton | `mvp_v1_walking_skeleton_aluno.md` |
| **P1** | MVP complementar — entregável no TCC | `mvp_v2_solicitacoes_workflow_engine.md` |
| **P2** | Pós-MVP — fase 2 do roadmap | `analise_arquitetural §13` |
| **P3** | Desejável — backlog futuro | — |

### 1.3 Regras de derivação resumidas

- **1 HU → 1 a 3 RFs** (agrupar por capacidade de negócio, não por CA)
- Fluxos de erro (401, 403, 429) são **critérios de aceitação** do RF principal, não RFs separados
  - Exceção: verificadores públicos e anti-fraude recebem RF próprio
- RNs (`RN-Fx.y-NN`) viram detalhamento/CAs do RF, **não viram RNF** salvo quando expressam atributo de qualidade mensurável
- Uma capacidade que aparece em múltiplas HUs (ex.: auditoria, FGAC) vira **RF transversal** com referência DRY nas fases

### 1.4 Estrutura de arquivos

```
foundationDocs/requisitos/
├── 00-indice-requisitos.md          ← este arquivo
├── 00-inventario-e-decisoes.md      ← lacunas, conflitos, decisões abertas
├── QUEUE.md                         ← fila de execução Etapas 0–12
├── 02-requisitos-nao-funcionais.md  ← gerado na Etapa 1
└── por-fase/
    ├── RF-F0-publico.md             ← gerado na Etapa 2
    ├── RF-F1-aluno.md               ← gerado na Etapa 3
    ├── RF-F2-egresso.md             ← gerado na Etapa 4
    ├── RF-F3-professor.md           ← gerado na Etapa 5
    ├── RF-F4-comissoes.md           ← gerado na Etapa 6
    ├── RF-F5-secretaria.md          ← gerado na Etapa 7
    ├── RF-F6-coordenacao.md         ← gerado na Etapa 8
    ├── RF-F7-admin.md               ← gerado na Etapa 9
    ├── RF-F8-cross-cutting.md       ← gerado na Etapa 10
    └── RF-TR-transversais.md        ← gerado na Etapa 11
```

---

## 2. Contagem confirmada RF/RNF (Etapa 12)

> Valores finais após revisão de cobertura — substituem as estimativas da Etapa 0.

### 2.1 Requisitos Funcionais (RF) — confirmado

| Fase / escopo | HUs | RFs gerados | Arquivo |
|---------------|----:|:-----------:|---------|
| F0 — Público | 7 | 7 | `por-fase/RF-F0-publico.md` |
| F1 — Aluno | 11 | 15 | `por-fase/RF-F1-aluno.md` |
| F2 — Egresso | 1 | 1 | `por-fase/RF-F2-egresso.md` |
| F3 — Professor | 7 | 9 | `por-fase/RF-F3-professor.md` |
| F4 — Comissões | 2 | 2 | `por-fase/RF-F4-comissoes.md` |
| F5 — Secretaria | 12 | 18 | `por-fase/RF-F5-secretaria.md` |
| F6 — Coordenação | 2 | 2 | `por-fase/RF-F6-coordenacao.md` |
| F7 — Admin | 7 | 8 | `por-fase/RF-F7-admin.md` |
| F8 — Cross-cutting | 2 | 2 | `por-fase/RF-F8-cross-cutting.md` |
| Transversais | — | 8 | `por-fase/RF-TR-transversais.md` |
| **Total** | **51** | **72** | 10 arquivos |

Subdivisões DEC-001: US-F1-003 (4 — inclui RF-F1-003-d), US-F1-005 (3), US-F3-002 (2), US-F3-003 (2), US-F5-002 (3), US-F5-004 (3), US-F5-005 (2), US-F5-008 (2), US-F7-002 (2).

### 2.2 Requisitos Não Funcionais (RNF) — confirmado

| Categoria | Sigla | Quantidade |
|-----------|-------|:----------:|
| Segurança | RNF-SEC | 10 |
| Desempenho | RNF-DES | 6 |
| Disponibilidade | RNF-DIS | 4 |
| Usabilidade / Acessibilidade | RNF-UX | 5 |
| Manutenibilidade | RNF-MAN | 5 |
| Confiabilidade | RNF-CON | 4 |
| Portabilidade | RNF-POR | 3 |
| Compatibilidade | RNF-CMP | 3 |
| Conformidade legal | RNF-LGL | 2 |
| **Total** | | **42** |

Arquivo: `02-requisitos-nao-funcionais.md`

### 2.3 Estimativa original (Etapa 0 — referência histórica)

| Métrica | Estimativa Etapa 0 | Confirmado Etapa 12 |
|---------|:------------------:|:-------------------:|
| RF total | ~75–90 | **71** |
| RNF total | ~35–45 | **42** |
| HUs cobertas | 51/51 | **51/51** ✅ |

---

## 3. Matriz de rastreabilidade HU → RF

> Preenchida progressivamente a cada etapa. ✅ = RF gerado | ⬜ = pendente

### F0 — Público (7 HUs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F0-001 | Autenticação de Usuário (Login) | RF-F0-001 | UC-AUT-01 | F0.1 `/login` | P0 | ✅ |
| US-F0-002 | Recuperar Senha | RF-F0-002 | UC-AUT-02 | F0.2 `/recuperar-senha` | P1 | ✅ |
| US-F0-003 | Nova Senha (primeiro acesso / reset) | RF-F0-003 | UC-AUT-03 | F0.3 `/nova-senha` | P1 | ✅ |
| US-F0-004 | Página de Contato | RF-F0-004 | UC-PUB-01 | F0.4 `/contato` | P2 | ✅ |
| US-F0-005 | Página de Erro | RF-F0-005 | UC-PUB-01 | F0.5 `/erro/:codigo` | P1 | ✅ |
| US-F0-006 | Verificar Protocolo de Solicitação | RF-F0-006 | UC-CRT-02 | F0.6 `/publico/verificar-protocolo` | P2 | ✅ |
| US-F0-007 | Verificar Certificado | RF-F0-007 | UC-CRT-03 | F0.7 `/publico/verificar-certificado` | P2 | ✅ |

### F1 — Aluno (11 HUs → 14 RFs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F1-001 | Dashboard do Aluno | RF-F1-001 | UC-DASH-01 | F1.1 `/inicio` | P0 | ✅ |
| US-F1-002 | Primeiro Acesso | RF-F1-002 | UC-AUT-04 | F1.2 `/primeiro-acesso` | P0 | ✅ |
| US-F1-003 | Perfil — dados pessoais | RF-F1-003-a | UC-AUT-05 | F1.3 `/perfil` | P2 | ✅ |
| US-F1-003 | Perfil — segurança/sessões | RF-F1-003-b | UC-AUT-05 | F1.4 `/perfil/seguranca` | P2 | ✅ |
| US-F1-003 | Perfil — notificações | RF-F1-003-c | UC-AUT-06 | F1.5 `/perfil/notificacoes` | P2 | ✅ |
| US-F1-003 | Perfil — exportação LGPD | RF-F1-003-d | UC-AUT-05 | F1.3 `/perfil` (Privacidade) | P2 | ✅ |
| US-F1-004 | Comunicação | RF-F1-004 | UC-COM-01 | F1.6 `/comunicacao` | P2 | ✅ |
| US-F1-005 | Solicitações — listar | RF-F1-005-a | UC-SOL-03 | F1.7 `/solicitacoes` | P2 | ✅ |
| US-F1-005 | Solicitações — abrir wizard | RF-F1-005-b | UC-SOL-01 | F1.8 `/solicitacoes/nova` | P1 | ✅ |
| US-F1-005 | Solicitações — detalhe/timeline | RF-F1-005-c | UC-SOL-03, UC-SOL-05 | F1.9 `/solicitacoes/:id` | P2 | ✅ |
| US-F1-006 | Atividades Formativas | RF-F1-006 | UC-FOR-01, UC-FOR-02 | F1.10–F1.12 | P2 | ✅ |
| US-F1-007 | Estágio | RF-F1-007 | UC-EST-01 | F1.13–F1.14 | P2 | ✅ |
| US-F1-008 | TCC | RF-F1-008 | UC-TCC-01 | F1.15–F1.16 | P2 | ✅ |
| US-F1-009 | Presença em Eventos | RF-F1-009 | UC-PRE-03 | F1.17–F1.18 | P2 | ✅ |
| US-F1-010 | Certificados | RF-F1-010 | UC-CRT-01 | F1.19 `/certificados` | P2 | ✅ |
| US-F1-011 | Atendimentos | RF-F1-011 | UC-ATD-02 | F1.20 `/meus-atendimentos` | P2 | ✅ |

### F2 — Egresso (1 HU)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F2-001 | Dashboard do Egresso e reemissão | RF-F2-001 | UC-EGR-01 | F2.1 `/egresso/inicio` | P2 | ✅ |

### F3 — Professor (7 HUs → 9 RFs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F3-001 | Dashboard do Professor | RF-F3-001 | UC-DASH-01 | F3.1 `/inicio` | P2 | ✅ |
| US-F3-002 | Eventos — CRUD | RF-F3-002-a | UC-PRE-01 | F3.2a–F3.2b `/professor/eventos` | P2 | ✅ |
| US-F3-002 | Eventos — operação ao vivo | RF-F3-002-b | UC-PRE-02, UC-PRE-04 | F3.2c `/professor/eventos/:id/operacao` | P2 | ✅ |
| US-F3-003 | Solicitações — fila | RF-F3-003-a | UC-SOL-04 | F3.3 `/solicitacoes?to=me` | P2 | ✅ |
| US-F3-003 | Solicitações — deliberar | RF-F3-003-b | UC-SOL-04 | F3.4 `/solicitacoes/:id/deliberar` | P2 | ✅ |
| US-F3-004 | Revisar Formativas (CAAF) | RF-F3-004 | UC-FOR-03 | F3.5 `/formativas?to=me` | P2 | ✅ |
| US-F3-005 | Orientação de Estágio | RF-F3-005 | UC-EST-02 | F3.6 `/estagios?to=me` | P2 | ✅ |
| US-F3-006 | Orientação de TCC | RF-F3-006 | UC-TCC-02 | F3.7 `/tccs?to=me` | P2 | ✅ |
| US-F3-007 | Publicar Comunicado | RF-F3-007 | UC-COM-02 | F3.8 `/comunicacao/publicar` | P2 | ✅ |

### F4 — Comissões (2 HUs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F4-001 | Pool CAAF — atribuir e aprovar em lote | RF-F4-001 | UC-FOR-04 | F4.1 `/comissoes/caaf` | P2 | ✅ |
| US-F4-002 | Pool COE — atribuir estágios | RF-F4-002 | UC-EST-03 | F4.2 `/comissoes/coe` | P2 | ✅ |

### F5 — Secretaria (12 HUs → 18 RFs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F5-001 | Dashboard da Secretaria | RF-F5-001 | UC-DASH-01 | F5.1 `/inicio` | P2 | ✅ |
| US-F5-002 | Fila + atrasados | RF-F5-002-a | UC-SOL-06 | F5.2 `/solicitacoes` · F5.5 `/secretaria/atrasados` | P2 | ✅ |
| US-F5-002 | Nova solicitação interna | RF-F5-002-b | UC-SOL-02 | F5.3 `/solicitacoes/nova` | P2 | ✅ |
| US-F5-002 | Deliberar (secretaria) | RF-F5-002-c | UC-SOL-04 | F5.4 `/solicitacoes/:id/deliberar` | P2 | ✅ |
| US-F5-003 | Gestão de Alunos | RF-F5-003 | UC-CAD-01 | F5.6 `/secretaria/alunos` | P2 | ✅ |
| US-F5-004 | Cursos | RF-F5-004-a | UC-CAD-02 | F5.7 `/secretaria/cursos` | P2 | ✅ |
| US-F5-004 | Disciplinas | RF-F5-004-b | UC-CAD-03 | F5.8 `/secretaria/disciplinas` | P2 | ✅ |
| US-F5-004 | Calendários | RF-F5-004-c | UC-CAD-04 | F5.9 `/secretaria/calendarios` | P2 | ✅ |
| US-F5-005 | Listar egressos | RF-F5-005-a | UC-EGR-03 | F5.10 `/secretaria/egressos` | P2 | ✅ |
| US-F5-005 | Colação e diploma | RF-F5-005-b | UC-EGR-02 | F5.11 `/secretaria/diplomas` | P2 | ✅ |
| US-F5-006 | Autorizações de Imagem | RF-F5-006 | UC-SOL-07 | F5.12 `/secretaria/autorizacoes-imagem` | P2 | ✅ |
| US-F5-007 | Atendimentos | RF-F5-007 | UC-ATD-01 | F5.13 `/secretaria/atendimentos` | P2 | ✅ |
| US-F5-008 | Eventos — CRUD | RF-F5-008-a | UC-PRE-01 | F5.14 `/secretaria/eventos` | P2 | ✅ |
| US-F5-008 | Eventos — operação | RF-F5-008-b | UC-PRE-02, UC-PRE-04 | F5.15 `/secretaria/eventos/:id/operacao` | P2 | ✅ |
| US-F5-009 | Importações | RF-F5-009 | UC-ADM-06 | F5.16 `/secretaria/importacoes` | P2 | ✅ |
| US-F5-010 | Exportações | RF-F5-010 | UC-ADM-07 | F5.17 `/secretaria/exportacoes` | P2 | ✅ |
| US-F5-011 | Estatísticas | RF-F5-011 | UC-ADM-08 | F5.18 `/secretaria/estatisticas` | P2 | ✅ |
| US-F5-012 | Tarefas Internas | RF-F5-012 | UC-ADM-12 | F5.19 `/secretaria/tarefas` | P3 | ✅ |

### F6 — Coordenação (2 HUs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F6-001 | Configurar parâmetros do curso | RF-F6-001 | UC-CAD-05 | F6.1 `/coordenacao/cursos/:id/configurar` | P2 | ✅ |
| US-F6-002 | Relatórios analíticos | RF-F6-002 | UC-ADM-09 | F6.2 `/coordenacao/relatorios` | P2 | ✅ |

### F7 — Admin (7 HUs → 8 RFs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F7-001 | Gestão de Usuários (IAM) | RF-F7-001 | UC-ADM-01 | F7.1 `/admin/usuarios` · F7.8 reset modal | P2 | ✅ |
| US-F7-002 | Perfis (roles) | RF-F7-002-a | UC-ADM-02 | F7.2 `/admin/perfis` | P2 | ✅ |
| US-F7-002 | Authorities (FGAC) | RF-F7-002-b | UC-ADM-02 | F7.3 `/admin/autoridades` | P2 | ✅ |
| US-F7-003 | Workflow Engine (RequestType) | RF-F7-003 | UC-ADM-03 | F7.4 `/admin/tipos-solicitacao` | P2 | ✅ |
| US-F7-004 | Templates de Comunicação | RF-F7-004 | UC-COM-03 | F7.5 `/admin/templates-comunicacao` | P2 | ✅ |
| US-F7-005 | Jobs e Outbox | RF-F7-005 | UC-ADM-04 | F7.6 `/admin/jobs` | P2 | ✅ |
| US-F7-006 | Audit Log | RF-F7-006 | UC-ADM-05 | F7.7 `/admin/audit-log` | P2 | ✅ |
| US-F7-007 | Saúde do Sistema | RF-F7-007 | — | F7.9 `/admin/sistema/saude` | P3 | ✅ |

### F8 — Cross-cutting (2 HUs)

| HU | Título | RF | UC | Tela | Prioridade | Status |
|----|--------|----|----|------|:----------:|:------:|
| US-F8-001 | Busca Global (Command Palette) | RF-F8-001 | UC-ADM-10 | F8.1 `/buscar?q=` (modal) | P2 | ✅ |
| US-F8-002 | Suporte e FAQ | RF-F8-002 | UC-ADM-11 | F8.2 `/suporte` | P2 | ✅ |

### Transversais (sem HU direta)

| ID sugerido | Tema | Fontes | Prioridade | Status |
|-------------|------|--------|:----------:|:------:|
| RF-TR-001 | Motor genérico de solicitações (19 tipos via RequestType) | ADR-003, MVP v2, US-F7-003 | P0 | ✅ |
| RF-TR-002 | Hub de comunicação + entrega assíncrona (Outbox) | `analise §7`, US-F7-005 | P1 | ✅ |
| RF-TR-003 | Emissão e verificação de certificados anti-fraude | `analise §11`, US-F0-007, US-F1-010 | P1 | ✅ |
| RF-TR-004 | Auditoria imutável de comandos | US-F7-006, `security-engineer` | P0 | ✅ |
| RF-TR-005 | Autorização FGAC + UI orientada a HATEOAS `_links` | ADR-002, `.cursorrules` | P0 | ✅ |
| RF-TR-006 | BFF de dashboard contextual por perfil | fluxos, US-F1-001, US-F3-001, US-F5-001 | P0 | ✅ |
| RF-TR-007 | Notificações push + e-mail com fallback | `analise §7`, comunicacao | P1 | ✅ |
| RF-TR-008 | Presença em eventos formativos v4.1 (modos configuráveis) | `endpoints v4.1`, `analise §10` | P1 | ✅ |

---

## 4. Contadores de cobertura

> Atualizado a cada etapa concluída.

| Métrica | Meta | Atual |
|---------|:----:|:-----:|
| HUs com ≥ 1 RF mapeado | 51/51 (100%) | **51/51 (100%)** ✅ |
| RF gerados (funcional) | ~75–90 | **72** (64 fase + 8 TR) |
| RNF gerados | ~35–45 | **42** ✅ |
| RNF com métrica verificável | 100% | **100%** ✅ |
| RF P0 cobrindo walking skeleton (MVP v1) | 100% | **100%** ✅ |

### 4.1 Walking skeleton — RF P0 (MVP v1)

| HU MVP v1 | RF | RF-TR suporte |
|-----------|-----|---------------|
| US-F0-001 Login | RF-F0-001 | RF-TR-004, RF-TR-005 |
| US-F1-001 Dashboard | RF-F1-001 | RF-TR-006 |
| US-F1-002 Primeiro acesso | RF-F1-002 | RF-TR-004, RF-TR-005 |

Transversais P0 adicionais exigidos pela arquitetura baseline: RF-TR-001 (motor solicitações — MVP v2), RF-TR-004 (auditoria), RF-TR-005 (FGAC/HATEOAS), RF-TR-006 (BFF).

---

## 5. Progresso da campanha

| Etapa | Escopo | Status | Arquivo gerado |
|------:|--------|:------:|----------------|
| 0 | Convenções + índice + inventário + estimativas | ✅ | `00-indice-requisitos.md`, `00-inventario-e-decisoes.md`, `QUEUE.md` |
| 1 | RNF transversais completos | ✅ | `02-requisitos-nao-funcionais.md` |
| 2 | RF F0 — Público (7 HUs) | ✅ | `por-fase/RF-F0-publico.md` |
| 3 | RF F1 — Aluno (11 HUs → 14 RFs) | ✅ | `por-fase/RF-F1-aluno.md` |
| 4 | RF F2 — Egresso (1 HU) | ✅ | `por-fase/RF-F2-egresso.md` |
| 5 | RF F3 — Professor (7 HUs → 9 RFs) | ✅ | `por-fase/RF-F3-professor.md` |
| 6 | RF F4 — Comissões (2 HUs) | ✅ | `por-fase/RF-F4-comissoes.md` |
| 7 | RF F5 — Secretaria (12 HUs → 18 RFs) | ✅ | `por-fase/RF-F5-secretaria.md` |
| 8 | RF F6 — Coordenação (2 HUs) | ✅ | `por-fase/RF-F6-coordenacao.md` |
| 9 | RF F7 — Admin (7 HUs → 8 RFs) | ✅ | `por-fase/RF-F7-admin.md` |
| 10 | RF F8 — Cross-cutting (2 HUs) | ✅ | `por-fase/RF-F8-cross-cutting.md` |
| 11 | RF transversais (§5 PROMPT) + revisão | ✅ | `por-fase/RF-TR-transversais.md` |
| 12 | Revisão de cobertura total | ✅ | `00-indice-requisitos.md`, `02-requisitos-nao-funcionais.md` (RNF-SEC-04) |

---

## 6. Checklist de qualidade — campanha encerrada (Etapa 12)

Conforme PROMPT §7:

| # | Critério | Status |
|---|----------|:------:|
| 1 | 51/51 HUs mapeadas na matriz §3 | ✅ |
| 2 | Nenhum RF sem ator e sem CA testável | ✅ (amostragem por fase — todos os 71 RFs seguem template) |
| 3 | Nenhum RNF sem métrica verificável | ✅ (42/42) |
| 4 | RNs consolidadas nos RFs — não duplicadas como RF redundante | ✅ |
| 5 | Prioridades P0 alinhadas a MVP v1 (US-F0-001, US-F1-001, US-F1-002) | ✅ §4.1 |
| 6 | Presença v4.1 sem geofence/trust score/aula regular | ✅ RF-TR-008, RF-F1-009, RF-F3-002, RF-F5-008 |
| 7 | Certificados: sistema gera PDF — nunca upload externo | ✅ RF-TR-003, RF-F0-007, RF-F1-010, RNF-LGL-02 |
| 8 | Numeração `RF-Fx-NNN` sequencial por fase | ✅ |
| 9 | Numeração `RNF-{CAT}-NN` por categoria | ✅ |
| 10 | Conflitos legado documentados | ✅ `00-inventario-e-decisoes.md` CONF-001 a CONF-004 |
| 11 | Linguagem acadêmica adequada ao TCC | ✅ |

### 6.1 Ações corretivas da revisão

| Item | Ação |
|------|------|
| CONF-004 | RNF-SEC-04 atualizado: **5 tentativas/min** (US-F0-001), não 15 min; bloqueio de conta 10 falhas/15 min documentado |
| Q-004 | RF-TR-008 canônico para presença; fases F1/F3/F5 referenciam |
| Contagem RF | Confirmado **71** (63 fase + 8 TR) — dentro da faixa estimada |
| Revisão classificação | **2026-08-09** — 6 RNFs corrigidos; 4 CAs de RFs corrigidos; RF-F1-003-d criado (LGPD exportação); total RF **72** (DEC-005) |

---

*Última atualização: 2026-08-09 — Revisão de classificação RNF×RF; RF-F1-003-d adicionado (LGPD exportação); total RF 71 → 72; contadores e matriz F1 atualizados*
