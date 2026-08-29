# T-F6-002 — Relatórios Analíticos de Coordenação

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F6 — Coordenação/US-F6-002-RELATORIOS.md`](../../foundationDocs/sequenceDiagrams/F6 — Coordenação/US-F6-002-RELATORIOS.md)  
> **Status:** ✅ `GET /reports/coordinator` com recorte curso/período + séries reais

Estatísticas da secretaria: [T-F5-011-ESTATISTICAS.md](../F5 — Secretaria/T-F5-011-ESTATISTICAS.md).

---

## Arquivo

[`bff/ReportsController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/ReportsController.kt) → [`ReportsQuery.coordinator`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/application/ReportsQuery.kt). Mesmos ports da secretaria (`IamDashboardPort`, `IamBffReadPort`, `SolicitacaoBffReadPort`, `TccDashboardPort`, `EstagioSummaryPort`, `FormativaBffReadPort`, `PresencaBffReadPort`, `AcademicoReadPort`). **Não** há `RelatoriosController`.

Authorities seedadas em V016 (`report.view_coordinator`).

```
GET /reports/coordinator?periodo=2025-2&curso=TADS
Authorization: Bearer …  (report.view_coordinator)
```

`curso` aceita UUID ou sigla. `periodo` no formato `AAAA-N` resolve `periodo_letivo` (senão jan–jun / jul–dez).

| Campo | Fonte |
|-------|--------|
| KPIs filtrados | `request` por `id_curso` + janela `created_at` |
| `evolucaoTemporal` (secretaria) | `date_trunc('month', created_at)` |
| `evasaoPorPeriodo` | colações por ano em `graduation_record` (proxy de formatura; não é evasão SIGA) |
| `cargaPorDeliberador` | `request_event` com `estado_novo` DEFERIDA/INDEFERIDA/DELIBERADA |

Atalho `GET /academico/relatorios/curso` continua institucional (totais simples) em [`AcademicoSummaryController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/AcademicoSummaryController.kt) (tag `BFF — Sumário Acadêmico`, distinto dos relatórios analíticos).

---

## Checklist

- [x] Recorte SQL por curso/período nas solicitações (`ReportsQuery` + ports)
- [x] Série mensal, carga por deliberador, colações por ano
- [x] 403 sem `report.view_coordinator`
