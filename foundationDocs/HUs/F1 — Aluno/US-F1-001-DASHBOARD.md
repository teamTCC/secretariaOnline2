# US-F1-001 — Dashboard do Aluno (Visão Unificada)

| Campo | Valor |
|-------|-------|
| **ID** | US-F1-001 |
| **Épico** | ALUNO-DASH |
| **Tela** | F1.1 — `/inicio` |
| **Prioridade** | **P0 — MVP v1 — BLUEPRINT** |
| **Plataforma** | Web + Mobile |
| **Capability** | `dashboard.view_own` |
| **API primária** | `GET /bff/dashboard/aluno` |
| **Frames Figma** | [Default/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=52-480) · [Skeleton/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=56-818) · [Default/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=56-974) · [Empty/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=141-12709) |
| **Spec de tela** | `telasFigma/telas1/F1.1-inicio-aluno.md` |
| **Obs.** | TELA REFERÊNCIA DO PROJETO (DashboardA). Toda tela autenticada herda estrutura desta. |

---

## 1. História de Usuário

> **Como** um aluno autenticado,  
> **Quero** ver um painel unificado ao entrar no sistema,  
> **Para** ter visão imediata de minhas pendências, horas formativas, solicitações em aberto, próximos eventos e atalhos para as principais funcionalidades.

---

## 2. Regras de Negócio

### Dados agregados pelo BFF

| ID | Regra |
|----|-------|
| **RN-F1.1-01** | O endpoint `GET /bff/dashboard/aluno` agrega dados via `DashboardAlunoQuery` e **ports** (`SolicitacaoDashboardPort`, `PresencaDashboardPort`, `FormativaDashboardPort`, `IamDashboardPort`) — **não** SQL cru no BFF nem JPA de outro módulo. Falha parcial em um módulo não deve impedir a renderização dos demais (degradação graciosa). |
| **RN-F1.1-02** | O KpiCard de **horas formativas** exibe horas já validadas pela CAAF versus o total requerido pelo curso. O progresso é calculado no backend como `horas_validadas / horas_requeridas`. |
| **RN-F1.1-03** | A seção "Pendências" exibe no máximo **3 itens** com ação requerida. Cada item expõe CTA derivado de `PendenciaItem._link` (string singular), **não** `_links.acao.href` HAL. |
| **RN-F1.1-04** | A tabela "Últimas solicitações" exibe as **5 solicitações mais recentes**. O badge de SLA fica vermelho (`status/danger`) quando `prazo_em < now`. |
| **RN-F1.1-05** | A seção "Próximos eventos" exibe os **3 eventos seguintes** com janela de presença ainda não confirmada. Badge "Janela aberta" (`success`) aparece quando o evento tem janela de validação ativa para o aluno. |
| **RN-F1.1-06** | O card "Último parecer" exibe o parecer mais recente recebido em qualquer módulo (solicitação, formativa, estágio, TCC). |

### HATEOAS e ações condicionais

| ID | Regra |
|----|-------|
| **RN-F1.1-07** | O botão "Nova solicitação" é renderizado **somente** se `_links.novaSolicitacao` (string URL) estiver presente na resposta do BFF. |
| **RN-F1.1-08** | O badge de comunicações não lidas no topbar é renderizado somente se o atalho correspondente existir (quando o BFF o expuser). `_links` do dashboard aluno: `self`, `novaSolicitacao`, `formativas`, `eventos` — valores string, não `{ href }`. |
| **RN-F1.1-09** | Os QuickTiles (atalhos) são renderizados a partir de `_links` (strings) retornada pelo BFF. A UI não hardcoda os atalhos disponíveis. |

### Performance

| ID | Regra |
|----|-------|
| **RN-F1.1-10** | O FCP (First Contentful Paint) do dashboard deve ser < 1,5s. O BFF usa **cache Redis de 60s** (`bff-dashboard`, chaves `aluno:{id}`). Sem Redis, Spring cache `simple`. |
| **RN-F1.1-11** | Em mobile, o pull-to-refresh deve reinvalidar o cache do TanStack Query e rebuscar todos os dados do dashboard. |

---

## 3. Critérios de Aceitação

### CA-01 — Carregamento inicial com dados (fluxo principal)

```gherkin
Dado que o aluno está autenticado com mustChangePassword = false
Quando navega para /inicio
Então o sistema realiza GET /bff/dashboard/aluno com cookie access_token (Bearer só como fallback)
  E enquanto a resposta chega exibe DS/Skeleton em cada bloco
    (4 retângulos para KpiRow, 3 linhas para cada lista)
  E ao receber resposta 200 OK renderiza:
    - Saudação: "Olá, {nome}" + Caption com curso e período
    - KpiRow: 4 DS/KpiCards (horas formativas, solicitações, eventos hoje, certificados)
    - Seção Pendências com até 3 itens e CTAs HATEOAS
    - Tabela "Últimas solicitações" com 5 linhas
    - Seção "Próximos eventos" com 3 cards
    - Coluna direita: Prazos, Último parecer, QuickTiles
```

### CA-02 — KpiCard de horas formativas

```gherkin
Dado que o aluno tem 72 horas validadas de um total de 120 requeridas
Quando o dashboard carrega
Então exibe DS/KpiCard com valor "72 / 120 h" e barra de progresso a 60%
  E a cor da barra usa token brand/primary
```

### CA-03 — Pendências com CTA HATEOAS

```gherkin
Dado que o aluno tem uma solicitação no estado EM_AJUSTE
  E o BFF retorna pendencias[0]._link com a URL de edição (string)
Quando o dashboard renderiza
Então exibe o item de pendência com DS/Badge estado "EM AJUSTE"
  E botão CTA vinculado a _link do item
  E se pendencias estiver ausente ou vazio, exibe DS/EmptyState na seção
```

### CA-04 — SLA breach na tabela de solicitações

```gherkin
Dado que uma solicitação tem prazo_em anterior a now
Quando a tabela "Últimas solicitações" é renderizada
Então a célula de prazo exibe a data em cor status/danger
  E sem tooltip explicativo de "SLA vencido"
```

### CA-05 — Degradação graciosa (erro parcial de módulo)

```gherkin
Dado que o serviço de solicitações está indisponível
  Mas os demais módulos (formativas, eventos, certificados) respondem normalmente
Quando o BFF agrega os dados
Então a seção "Últimas solicitações" exibe DS/AlertBanner warning:
  "Não foi possível carregar as solicitações no momento."
  E as demais seções renderizam normalmente
  E o aluno não perde acesso ao resto do dashboard
```

### CA-06 — Estado vazio (aluno recém-cadastrado)

```gherkin
Dado que o aluno não tem solicitações, pendências ou eventos
Quando o dashboard carrega
Então cada seção vazia exibe DS/EmptyState com mensagem contextual:
  - Pendências: "Nenhuma pendência no momento."
  - Solicitações: "Você ainda não abriu solicitações."
  - Eventos: "Nenhum evento próximo."
```

### CA-07 — Pull-to-refresh mobile

```gherkin
Dado que o aluno está no dashboard em dispositivo mobile
Quando puxa a tela para baixo (pull-to-refresh)
Então o indicador nativo de refresh aparece no topo
  E o TanStack Query invalida o cache de /bff/dashboard/aluno
  E os dados são rebuscados e atualizados na tela
```

### CA-08 — Responsividade

```gherkin
Dado que o aluno acessa o dashboard
Quando a viewport é ≥ 1280px
Então KpiRow exibe 4 colunas e o layout principal usa proporção 2:1

Quando a viewport é 768–1023px
Então KpiRow exibe 2x2 e o layout empilha (coluna esquerda acima da direita)

Quando a viewport é < 768px
Então KpiRow exibe 2x2 ou com scroll horizontal
  E a sidebar fica em overlay (drawer)
```

---

## 4. Componentes de UI (Design System)

| Componente | Uso |
|------------|-----|
| `DS/KpiCard` | 4 indicadores (horas, solicitações, eventos, certificados) |
| `DS/AlertBanner` | Degradação parcial de módulo |
| `DS/Badge` | Estado de solicitações e eventos |
| `DS/DataTable` (compact) | Tabela de últimas solicitações |
| `DS/Skeleton` | Loading por bloco |
| `DS/EmptyState` | Seções sem dados |
| `QuickTile` | Atalhos de navegação |
| `EventoRow` | Card de próximos eventos |

---

## 5. Contrato de API (BFF)

```http
GET /bff/dashboard/aluno
Cookie: access_token={jwt}
```

```json
{
  "kpis": {
    "horasFormativas": { "atual": 72, "requerido": 120, "percentual": 60 },
    "atendimentosPendentes": 1
  },
  "pendencias": [
    { "id": "...", "tipo": "...", "estado": "EM_AJUSTE", "prazoEm": "2026-07-01T12:00:00Z", "acao": "editar", "_link": "/solicitacoes/..." }
  ],
  "eventos": [ { "id": "...", "titulo": "...", "chCreditadas": 4, "fimEm": "...", "_link": "/eventos/..." } ],
  "ultimasSolicitacoes": [ { "id": "...", "tipo": "...", "estado": "EM_ANALISE", "createdAt": "..." } ],
  "_links": {
    "self": "/bff/dashboard/aluno",
    "novaSolicitacao": "/solicitacoes/nova",
    "formativas": "/formativas",
    "eventos": "/eventos"
  }
}
```

`_links` e `_link` são **strings**. Não usar HAL `{ href }`. Auth: cookie `access_token` (Bearer só como fallback). BFF: Query → ports → adapters (não SQL no BFF).
---

## 6. Fora de escopo

- Dashboard de outros perfis (Professor, Secretaria) — cobertos em F3/F5
- Versões B e C de dashboard — **NUNCA implementar** (DashboardA é o único blueprint)
- Widgets configuráveis / drag-and-drop de blocos — não previsto no MVP

---

## 7. Definição de Pronto (DoD)

- [ ] Frame Figma master `F1.1 — Dashboard Aluno` 1440×1024 aprovado
- [ ] BFF endpoint `GET /bff/dashboard/aluno` implementado com degradação graciosa
- [ ] Skeleton rendering para todos os blocos antes de dados chegarem
- [ ] HATEOAS: QuickTiles e CTAs apenas de `_links` / `_link` (strings)
- [ ] Cache Redis 60s no BFF (`bff-dashboard`) para dados não-tempo-real
- [ ] BFF via ports (não SQL cru / não JPA cross-módulo)
- [ ] FCP < 1,5s medido com Lighthouse em condições de rede 3G
- [ ] Responsividade validada em 375px, 768px, 1280px e 1440px

---

## 8. Referências

| Recurso | Link / Caminho |
|---------|---------------|
| Spec de tela | `telasFigma/telas1/F1.1-inicio-aluno.md` |
| Fluxo aluno | `foundationDocs/analysis/fluxos_por_perfil.md` §2 |
| As-built backend | `foundationDocs/analysis/as-built-backend.md` |
| MVP Walking Skeleton | `foundationDocs/analysis/mvp_walking_skeleton_aluno.md` |
| Página Figma F1 | [Telas / F1 — Aluno](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=48-339) |
| Frame principal | [F1.1 — Dashboard Aluno / Default / Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=52-480) |
