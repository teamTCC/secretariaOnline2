# T-F5-011 — Estatísticas da Secretaria

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-011-ESTATISTICAS.md`](../../foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-011-ESTATISTICAS.md)  
> **Status:** ✅ `GET /reports/secretary` via `ReportsController` → `ReportsQuery.secretary` + ports  
> **Capability:** `report.view_secretary`  
> **Não existe** `RelatoriosController`.

---

## Arquivos

| Papel | Classe |
|-------|--------|
| HTTP | [`bff/ReportsController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/ReportsController.kt) |
| Query | [`bff/application/ReportsQuery.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/application/ReportsQuery.kt) |
| Ports | `IamDashboardPort`, `IamBffReadPort`, `SolicitacaoBffReadPort`, `TccDashboardPort`, `EstagioSummaryPort`, `FormativaBffReadPort`, `PresencaBffReadPort`, `AcademicoReadPort` |

Authority `report.view_secretary` seedada em [`V016`](../../backend/app/src/main/resources/db/migration/V016__egresso_and_report_authorities.sql) para SECRETARIO, COORDENADOR, CAAF, COE e ADMIN.

O controller **não** injeta JPA. `ReportsQuery` agrega via ports; cada adapter vive no módulo dono.

```
GET /reports/secretary?periodo=2025-2&curso=TADS
Cookie: access_token=…
```

```json
{
  "filtros": { "periodo": "2025-2", "curso": "TADS" },
  "kpis": {
    "alunosAtivos": 142,
    "egressos": 18,
    "solicitacoesAbertas": 9,
    "eventosAgendados": 3
  },
  "solicitacoesPorTipo": [{ "tipo": "APROVEITAMENTO_DISCIPLINA", "total": 12 }],
  "distribuicaoPorEstado": [{ "estado": "ABERTA", "total": 9 }],
  "evolucaoTemporal": [],
  "rankingCursos": [{ "cursoId": "uuid", "sigla": "TADS", "total": 40 }]
}
```

`periodo` / `curso` recortam solicitações (`id_curso` + janela do período letivo). `evolucaoTemporal` é série `YYYY-MM` via `date_trunc`.

Cache: TanStack Query no cliente (staleTime 5 min). Sem cache Redis específico neste endpoint.

Coordenação: [T-F6-002](../F6 — Coordenação/T-F6-002-RELATORIOS.md) (`ReportsQuery.coordinator`, mesmos ports).

---

## Checklist

- [x] `GET /reports/secretary` → `ReportsQuery` + ports (não JPA no BFF)
- [x] KPIs + por tipo + por estado + ranking de cursos
- [x] 403 sem `report.view_secretary`
- [ ] Série temporal mensal / filtro SQL por período — não no MVP
