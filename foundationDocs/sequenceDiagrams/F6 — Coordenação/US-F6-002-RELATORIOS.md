# US-F6-002 — Relatórios Analíticos de Coordenação

| HU | Tela | Capability | API primária | Fonte |
|----|------|-----------|-------------|-------|
| US-F6-002 | F6.2 — Relatórios Coordenação (`/coordenacao/relatorios`) | `report.view_coordinator` | `GET /reports/coordinator` | `fluxos_por_perfil.md` §7.2 · HU US-F6-002 |

---

## Matriz de cobertura

| ID diagrama | Origem (CA / RN) | Classificação | Status |
|-------------|-----------------|---------------|--------|
| F6.2-D01 | CA-F6-002-01..02 · RN-01,03,04,05,07,08,09,10,13 — GET cache MISS + métricas analíticas | SEQUENCIA | gerado |
| F6.2-D02 | RN-F6-002-12 — cache HIT (staleTime=5 min, padrão F5.18) | DRY → US-F5-011 F5.18-D02 | link |
| F6.2-ERRO | RN-F6-002-01 — 403 `report.view_coordinator` ausente | DRY → US-F5-011 F5.11-ERRO-403 | link |
| — | CA-F6-002-01 — DS/Skeleton Loading (UI interim enquanto GET está em voo) | NAO_APLICAVEL | — |
| — | CA-F6-002-03 — AlertBanner threshold indeferimento (renderização condicional) | NAO_APLICAVEL | — |
| — | CA-F6-002-04 — Pendências clicáveis (navegação React Router, sem API) | NAO_APLICAVEL | — |
| — | CA-F6-002-05 — gráfico Evasão (Recharts renderiza dados do response) | NAO_APLICAVEL | — |
| — | CA-F6-002-06 — QuickTiles HATEOAS (`useActions` lê `_links`, sem API extra) | NAO_APLICAVEL | — |
| — | CA-F6-002-07 — filtros persistem na URL (React Router `useSearchParams`) | NAO_APLICAVEL | — |
| — | CA-F6-002-08 — drill-down deliberador (dados já em `cargaPorDeliberador` no response) | NAO_APLICAVEL | — |
| — | RN-F6-002-06 — AlertBanner threshold (frontend: `taxaIndeferimento > thresholdIndeferimento`) | NAO_APLICAVEL | — |
| — | RN-F6-002-11 — QuickTile condicional via `useActions(resource)` | NAO_APLICAVEL | — |

---

## Referências DRY

| Padrão | Diagrama local | Referência canônica |
|--------|---------------|---------------------|
| Cache HIT (staleTime=5 min) | F6.2-D02 | [`../F5/US-F5-011-ESTATISTICAS.md` — F5.18-D02](../F5/US-F5-011-ESTATISTICAS.md) |
| 403 FGAC capability ausente | F6.2-ERRO | [`../F5/US-F5-011-ESTATISTICAS.md` — F5.11-ERRO-403](../F5/US-F5-011-ESTATISTICAS.md) |

**Diferença em relação ao DRY:** endpoint `/reports/coordinator` (vs `/reports/secretary`) e capability `report.view_coordinator` (vs `report.view_secretary`). O fluxo de rede e o comportamento de cache/FGAC são estruturalmente idênticos — não duplicar Mermaid.

---

## Fora de sequência

| Item | Motivo |
|------|--------|
| CA-F6-002-01 — Loading Skeleton | `isLoading=true` enquanto o GET está em voo — estado TanStack Query; nenhuma chamada de rede adicional. Coberto em Notas de F6.2-D01. |
| CA-F6-002-03 — AlertBanner threshold | Lógica frontend pura: `taxaIndeferimento > thresholdIndeferimento` sobre campos já presentes no response do F6.2-D01. Sem API call extra. |
| CA-F6-002-04 — Pendências clicáveis | `href` de cada `PendenciaItem` vem do response (campo `pendencias[].href`); o clique é `navigate(href)` — React Router. Sem API call. |
| CA-F6-002-05 — Gráfico Evasão (Recharts) | Renderização a partir de `evasaoPorPeriodo[]` já no response. Sem API call. |
| CA-F6-002-06 — QuickTiles HATEOAS | `useActions(resource)` filtra `_links` do response; sem API call extra. |
| CA-F6-002-07 — Filtros na URL | `useSearchParams` sincroniza estado de filtros ↔ URL; quando filtros mudam, dispara novo GET (mesmo fluxo F6.2-D01 com query params diferentes). |
| CA-F6-002-08 — Drill-down carga deliberador | `cargaPorDeliberador[]` já presente no response de F6.2-D01; a tabela de drill-down renderiza esses dados sem nova requisição. |
| RN-F6-002-06 — AlertBanner | Idem CA-03. |
| RN-F6-002-11 — QuickTile condicional | Idem CA-06. |

---

## F6.2-D01 — GET /reports/coordinator (cache MISS, com filtros)

**Escopo:** happy path — coordenador acessa `/coordenacao/relatorios` com filtros aplicados; TanStack Query não tem entrada em cache (primeira carga ou staleTime expirado); backend agrega KPIs, séries históricas e dados operacionais em resposta única.

**Pré-condições:**
- Coordenador autenticado com JWT válido e capability `report.view_coordinator`.
- TanStack Query: cache MISS para a chave `['coordinator-report', filters]`.
- Filtros aplicados via query string: `?periodo=2025-2&curso=TADS`.

```mermaid
sequenceDiagram
    autonumber
    participant Coordenador
    participant WebApp
    participant RC as ReportsController
    participant Query as ReportsQuery
    participant ReqPort as SolicitacaoBffReadPort
    participant IamPort as IamBffReadPort

    Coordenador->>WebApp: acessa /coordenacao/relatorios?periodo=2025-2&curso=TADS
    WebApp->>WebApp: TanStack Query cache MISS (staleTime=5min expirado)
    WebApp->>RC: GET /reports/coordinator?periodo=2025-2&curso=TADS (cookie)
    RC->>Query: coordinator(periodo, curso)
    Query->>ReqPort: avgDeliberation, countByEstado, findSlaAbertas
    Query->>IamPort: countColacoesByAno, findNome
    ReqPort-->>Query: kpis + pendencias SLA
    IamPort-->>Query: séries colação / nomes
    Query-->>RC: CoordinatorReportResponse
    RC-->>WebApp: 200 {kpis, series, pendencias, _links strings}
    WebApp-->>Coordenador: KpiRow + ChartsGrid + Pendências + QuickTiles
```

**Notas:**
- Ports adicionais (omitidos no diagrama): `TccDashboardPort`, `EstagioSummaryPort`, `FormativaBffReadPort`, `PresencaBffReadPort`, `AcademicoReadPort`, `IamDashboardPort`.
- Sem `RelatoriosController`, sem `GetCoordinatorReportUC`, sem SELECT no Postgres a partir do BFF.
- `_links` = `Map<String,String>` (`self`, `curso`). Pendências usam `href` string no DTO de relatório (não HAL).
- Auth: cookie `access_token` (Bearer fallback).

**Lacunas:** nenhuma.

---

## F6.2-D02 — Cache HIT

**DRY → [`../F5/US-F5-011-ESTATISTICAS.md` — F5.18-D02](../F5/US-F5-011-ESTATISTICAS.md)**

Padrão idêntico: TanStack Query retorna dados em memória (staleTime=5 min); nenhuma chamada ao backend. Apenas substitua:
- Endpoint: `/reports/coordinator` (vs `/reports/secretary`)
- Capability: `report.view_coordinator` (vs `report.view_secretary`)

---

## F6.2-ERRO — 403 `report.view_coordinator` ausente

**DRY → [`../F5/US-F5-011-ESTATISTICAS.md` — F5.11-ERRO-403](../F5/US-F5-011-ESTATISTICAS.md)**

Padrão idêntico: JwtFilter valida JWT; Spring Security rejeita por capability ausente antes de chegar ao controller; retorna `403 Problem Details`; WebApp exibe `DS/AlertBanner`. Apenas substitua capability e endpoint conforme indicado acima.
