# T-F6-002 — Relatórios Analíticos de Coordenação

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F6 — Coordenação/US-F6-002-RELATORIOS.md`](../../foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-002-RELATORIOS.md)  
> **Status:** ⏳ Não implementado — padrão análogo ao relatório da secretaria; apenas capability e endpoint diferem

---

## Contexto: semelhança com US-F5-011

Este endpoint é estruturalmente idêntico ao `GET /reports/secretary` da secretaria. A diferença é:

| Aspecto | Secretaria (F5-011) | Coordenação (F6-002) |
|---------|---------------------|----------------------|
| Capability | `report.view_secretary` | `report.view_coordinator` |
| Endpoint | `GET /reports/secretary` | `GET /reports/coordinator` |
| Escopo dos dados | Toda a instituição | Apenas o(s) curso(s) do coordenador |
| Extra | — | `thresholdIndeferimento` (alerta configurable) |

---

## O que os diagramas especificam

### F6.2-D01 — `GET /reports/coordinator` (cache MISS, com filtros)

```
GET /reports/coordinator?periodo=2025-2&curso=TADS
Authorization: Bearer eyJhbGci...  (hasAuthority('report.view_coordinator'))
```

Backend agrega em **3 rounds paralelos de queries** (Kotlin coroutines ou CTE único):
1. KPIs: `tempoMedioDeliberacao`, `taxaIndeferimento`, `volumeFormativas`, `taxaPresenca`, `thresholdIndeferimento`
2. Séries históricas: `evasaoPorPeriodo[]`, `seriesFormativas[]`
3. Dados operacionais: `pendencias[]`, `solicitacoesTopSla[]`, `proximosEventos[]`, `cargaPorDeliberador[]`

**JSON de saída (200):**

```json
{
  "kpis": {
    "tempoMedioDeliberacaoDias": 3.7,
    "taxaIndeferimento": 0.18,
    "thresholdIndeferimento": 0.20,
    "volumeFormativasAprovadas": 142,
    "taxaPresencaEventos": 0.83
  },
  "evasaoPorPeriodo": [
    { "periodo": "2024-1", "evasao": 0.08 },
    { "periodo": "2024-2", "evasao": 0.11 },
    { "periodo": "2025-1", "evasao": 0.07 }
  ],
  "seriesFormativas": [
    { "tipo": "PALESTRA", "aprovadas": 45, "rejeitadas": 3 },
    { "tipo": "MINICURSO", "aprovadas": 31, "rejeitadas": 1 }
  ],
  "pendencias": [
    {
      "tipo": "FORMATIVA_SEM_REVISOR",
      "quantidade": 5,
      "href": "/comissoes/caaf"
    }
  ],
  "solicitacoesTopSla": [
    { "protocolo": "SOL-2025-042", "tipo": "APROVEITAMENTO", "diasEmAberto": 18 }
  ],
  "proximosEventos": [
    { "id": "abc-...", "titulo": "Workshop de Python", "data": "2026-08-20" }
  ],
  "cargaPorDeliberador": [
    { "nome": "Prof. Ana Lima", "pendentes": 12, "aprovadas": 38 }
  ],
  "_links": {
    "self": "/reports/coordinator?periodo=2025-2&curso=TADS",
    "requests": "/solicitacoes",
    "events": "/eventos"
  }
}
```

**Regras de negócio importantes:**
- `taxaIndeferimento > thresholdIndeferimento` → frontend exibe `DS/AlertBanner` (lógica client-side sobre dados do response)
- `pendencias[].href` — o clique usa `navigate(href)` via React Router, sem nova chamada à API
- `cargaPorDeliberador[]` — renderizado no drill-down sem requisição extra
- Filtros na URL (`?periodo=&curso=`): quando alterados, TanStack Query invalida cache e faz novo GET

---

### F6.2-D02 — Cache HIT

TanStack Query retorna dados em memória (`staleTime = 5 min`). **Nenhuma chamada ao backend.** Padrão idêntico ao US-F5-011 relatório da secretaria.

---

### F6.2-ERRO — 403 `report.view_coordinator` ausente

```json
HTTP/1.1 403 Forbidden
{
  "type": "access_denied",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Capability report.view_coordinator ausente."
}
```

`JwtFilter` valida JWT → Spring Security rejeita por `@PreAuthorize` antes de qualquer query.

---

## O que precisa ser implementado

| Arquivo a criar | Descrição |
|----------------|-----------|
| `modules/bff/CoordinatorReportController.kt` (ou em `ReportController.kt`) | `GET /reports/coordinator` com filtros |
| `modules/bff/GetCoordinatorReportUseCase.kt` | 3 rounds de agregação paralela, scope por `cursosIds` |
| `modules/academico/infrastructure/CourseConfigEntity.kt` | `threshold_indeferimento` como campo configurável |
| Migração | Adicionar `threshold_indeferimento` a `course_config` |

---

## Checklist de Verificação

- [ ] `GET /reports/coordinator` → `200` com todos os campos esperados
- [ ] Scope: apenas dados dos cursos do coordenador (não de outros cursos)
- [ ] `thresholdIndeferimento` retornado nos kpis (para lógica de alerta no frontend)
- [ ] `pendencias[].href` com caminhos navegáveis
- [ ] `cargaPorDeliberador[]` presente no response (drill-down sem nova requisição)
- [ ] Cache TanStack Query: segunda chamada dentro de 5 min não chega ao servidor
- [ ] 403 sem `report.view_coordinator` → `access_denied`
