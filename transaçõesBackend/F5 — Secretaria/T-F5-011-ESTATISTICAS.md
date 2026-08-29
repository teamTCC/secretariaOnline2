# T-F5-011 — Estatísticas da Secretaria

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-011-ESTATISTICAS.md`](../../foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-011-ESTATISTICAS.md)  
> **Status:** ✅ `GET /reports/secretary` com KPIs, tipos, estados, ranking e série mensal  
> **Capability:** `report.view_secretary`

---

## Arquivo

[`bff/ReportsController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/ReportsController.kt) — tag OpenAPI `BFF — Relatórios Analíticos`. Authority `report.view_secretary` seedada em [`V016`](../../backend/app/src/main/resources/db/migration/V016__egresso_and_report_authorities.sql) para SECRETARIO, COORDENADOR, CAAF, COE e ADMIN.

```
GET /reports/secretary?periodo=2025-2&curso=TADS
Authorization: Bearer …
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

`periodo` / `curso` recortam solicitações no SQL (`id_curso` + janela do período letivo). `evolucaoTemporal` é série `YYYY-MM` via `date_trunc`.

Cache: TanStack Query no cliente (staleTime 5 min). Sem cache Redis específico neste endpoint.

---

## Checklist

- [x] `GET /reports/secretary` → KPIs + por tipo + por estado + ranking de cursos
- [x] 403 sem `report.view_secretary`
- [ ] Série temporal mensal / filtro SQL por período — não no MVP
