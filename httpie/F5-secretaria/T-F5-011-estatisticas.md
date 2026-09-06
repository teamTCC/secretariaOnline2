# T-F5-011 — Estatísticas da secretaria

> **Transação:** [`T-F5-011`](../../transaçõesBackend/F5%20—%20Secretaria/T-F5-011-ESTATISTICAS.md)  
> **Diagrama:** [`US-F5-011`](../../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-011-ESTATISTICAS.md)

Capability: `report.view_secretary`. `ReportsController` → `ReportsQuery.secretary` + ports (não JPA no BFF; **não** existe `RelatoriosController`). Sem cache Redis específico.

---

```
GET {{baseUrl}}/reports/secretary?periodo=2026-2&curso=TADS
Authorization: Bearer {{accessTokenSecretaria}}
```

`curso` aceita sigla ou UUID. `periodo` formato `AAAA-N`.

**Esperado 200:**

```json
{
  "filtros": { "periodo": "2026-2", "curso": "TADS" },
  "kpis": {
    "alunosAtivos": 0,
    "egressos": 0,
    "solicitacoesAbertas": 0,
    "eventosAgendados": 0
  },
  "solicitacoesPorTipo": [],
  "distribuicaoPorEstado": [],
  "evolucaoTemporal": [],
  "rankingCursos": []
}
```

Sem authority → **403**. Relatório de coordenação: [T-F6](../F6-coordenacao/T-F6-coordenacao.md).
