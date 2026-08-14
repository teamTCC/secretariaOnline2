# T-F1-001 — Dashboard do Aluno (BFF)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-001-DASHBOARD.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-001-DASHBOARD.md)  
> **Status:** ✅ Implementado — BFF agrega dados em chamada única (cache Redis TTL 60s — ver T-10.7)

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| BFF Controller | [`bff/DashboardAlunoController.kt`](../../backend/modules/bff/src/main/kotlin/br/ufpr/sept/so2/modules/bff/DashboardAlunoController.kt) |
| Repository de solicitações | [`solicitacoes/persistence/SolicitacoesJpaRepositories.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/infrastructure/persistence/SolicitacoesJpaRepositories.kt) |
| Repository de eventos | `presenca/persistence/EventAttendanceJpaRepository` |
| Repository de formativas | `formativas/persistence/FormativeEntryJpaRepository` |

---

## O que o BFF faz

O endpoint `GET /bff/dashboard/aluno` agrega **4 blocos + KPIs** em uma única chamada HTTP:

1. **KPIs** — horas formativas (aprovadas vs. 120h) e `atendimentosPendentes` (`PENDENTE_CIENCIA`)
2. **Pendências** (solicitações `EM_AJUSTE`, máx. 3)
3. **Eventos abertos** (`EM_ANDAMENTO`, máx. 3)
4. **Últimas solicitações** (5 mais recentes)

---

## Chamada e JSON de resposta

```
GET /bff/dashboard/aluno
Authorization: Bearer eyJhbGci...
```

### JSON de saída — 200

```json
{
  "kpis": {
    "horasFormativas": {
      "atual": 47.5,
      "requerido": 120.0,
      "percentual": 39.58
    },
    "atendimentosPendentes": 2
  },
  "pendencias": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "tipo": "APROVEITAMENTO_DISCIPLINA",
      "estado": "EM_AJUSTE",
      "prazoEm": "2026-08-20T23:59:59Z",
      "acao": "REENVIAR",
      "_link": "/requests/550e8400-e29b-41d4-a716-446655440000"
    }
  ],
  "eventos": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "titulo": "Palestra: Inteligência Artificial na Engenharia",
      "chCreditadas": 4.0,
      "fimEm": "2026-08-10T18:00:00Z",
      "_link": "/events/7c9e6679-7425-40de-944b-e07fc1f90ae7/attendance/session"
    }
  ],
  "ultimasSolicitacoes": [
    {
      "id": "a3bb189e-8bf9-3888-9912-3e6bad1d8f7e",
      "tipo": "CANCELAMENTO_MATRICULA",
      "estado": "DELIBERADA",
      "createdAt": "2026-07-15T10:30:00Z"
    }
  ],
  "_links": {
    "self": "/bff/dashboard/aluno",
    "novaSolicitacao": "/requests/types",
    "formativas": "/formativas/minhas",
    "eventos": "/events?audience=me"
  }
}
```

---

## Como os `_links` HATEOAS funcionam

O frontend usa `useActions(_links)` para renderizar condicionalmente botões e tiles:

```kotlin
// DashboardAlunoController.kt
return mapOf(
    "kpis" to ...,
    "pendencias" to pendencias,
    "eventos" to eventos,
    "ultimasSolicitacoes" to ultimasSolicitacoes,
    "_links" to mapOf(
        "self" to "/bff/dashboard/aluno",
        "novaSolicitacao" to "/requests/types",  // botão só aparece se presente
        "formativas" to "/formativas/minhas",
        "eventos" to "/events?audience=me",
    ),
)
```

> Um aluno sem `request.open` não recebe `_links.novaSolicitacao` — o botão "Nova solicitação" desaparece do frontend **sem código condicional** no React.

---

## FGAC — Proteção do endpoint

```kotlin
// DashboardAlunoController.kt
@PreAuthorize("hasAuthority('dashboard.view_own')")
fun dashboardAluno(): Map<String, Any?> {
    val user = currentUser()
    val alunoId = user.userId  // ID vem do JWT, NÃO de query param
    ...
}
```

O `alunoId` é extraído do JWT no `SecurityContext` — não é possível para um aluno ver o dashboard de outro aluno passando um UUID diferente na query.

---

## F1.1-D01 vs. F1.1-D02: Cache Redis

Implementado com cache-aside TTL 60s — ver [T-10.7-REDIS-BFF](../transversal/T-10.7-REDIS-BFF.md). Chave `aluno:{alunoId}` no cache `bff-dashboard`. Respostas com `_degraded=true` **não** são cacheadas.

---

## F1.1-D03: Degradação graciosa

Cada bloco está em `try/catch`. Falha isolada → campo `null` + `_degraded: true`, HTTP **200**.

---

## Dashboards do Professor e da Secretaria

O mesmo controller tem endpoints para outros perfis:

```
GET /bff/dashboard/professor → hasAuthority('dashboard.view_self_professor')
GET /bff/dashboard/secretaria → hasAuthority('dashboard.view_secretary')
```

**Dashboard do Professor** retorna:
- `meusEventos`: lista de eventos do professor (todos os estados)
- `solicitacoesPendentes`: solicitações em `EM_DELIBERACAO`
- `_links`: `novoEvento`, `meuEventos`

**Dashboard da Secretaria** retorna:
- `kpis.emTriagem`: contagem de solicitações `ABERTA`
- `kpis.emDeliberacao`: contagem de solicitações `EM_DELIBERACAO`
- `_links`: `solicitacoes`, `usuarios`

---

## Checklist de Verificação

- [x] `GET /bff/dashboard/aluno` com Bearer válido → `200` com os 4 blocos
- [x] `alunoId` extraído do JWT (não de query param)
- [x] `_links` HATEOAS presente na resposta
- [x] `dashboard.view_own` obrigatório (403 sem authority)
- [x] KPI: `horasAprovadas / 120.0 * 100` calculado no servidor
- [x] Pendências: apenas estado `EM_AJUSTE` do aluno, máx. 3
- [x] Eventos: apenas estado `EM_ANDAMENTO`, máx. 3
- [x] `kpis.atendimentosPendentes` (count `PENDENTE_CIENCIA`)
- [x] Cache Redis TTL=60s — ver T-10.7
- [x] Degradação graciosa por bloco
