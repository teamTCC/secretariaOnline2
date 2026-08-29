# US-F1-001 — Dashboard do Aluno (Visão Unificada)

| HU | Tela | Capability | API primária | Fonte |
|----|------|------------|--------------|-------|
| US-F1-001 | F1.1 — `/inicio` | `dashboard.view_own` | `GET /bff/dashboard/aluno` | `HUs/F1 — Aluno/US-F1-001-DASHBOARD.md` · `fluxos_por_perfil.md` §2 F1 · `as-built-backend.md` §3 |

---

## Matriz de cobertura

| ID diagrama | Origem (CA / RN / sub-fluxo) | Tipo | Status |
|-------------|------------------------------|------|--------|
| F1.1-D01 | CA-01 · RN-F1.1-01 · RN-F1.1-10 (cache MISS) — carregamento inicial | SEQUENCIA | gerado |
| F1.1-D02 | RN-F1.1-10 (cache HIT) — retorno em < 1,5 s | SEQUENCIA | gerado |
| F1.1-D03 | CA-05 · RN-F1.1-01 — degradação graciosa (módulo indisponível) | SEQUENCIA | gerado |
| F1.1-D04 | CA-07 · RN-F1.1-11 — pull-to-refresh mobile | SEQUENCIA | gerado |
| — | CA-02 (KpiCard horas formativas) | DRY | → F1.1-D01 (resposta BFF inclui `kpis.horasFormativas`) |
| — | CA-03 (pendências com CTA) | DRY | → F1.1-D01 (`pendencias[]._link` string) |
| — | RN-F1.1-02 (cálculo `horas_validadas / horas_requeridas`) | DRY | → F1.1-D01 (calculado no Query antes de retornar) |
| — | RN-F1.1-03 (máx. 3 pendências, `_link` CTA) | DRY | → F1.1-D01 |
| — | RN-F1.1-05 (3 próximos eventos, badge "Janela aberta") | DRY | → F1.1-D01 |
| — | RN-F1.1-06 (último parecer) | DRY | → F1.1-D01 |
| — | RN-F1.1-07 (`_links.novaSolicitacao`) | DRY | → F1.1-D01 |
| — | RN-F1.1-08 (`_links.hub` unreadCount) | DRY | → F1.1-D01 |
| — | RN-F1.1-09 (QuickTiles de `_links`) | DRY | → F1.1-D01 |
| — | CA-04 (SLA breach — célula em `status/danger`) | NAO_APLICAVEL | — |
| — | CA-06 (estado vazio — DS/EmptyState por seção) | NAO_APLICAVEL | — |
| — | CA-08 (responsividade — 375/768/1280px) | NAO_APLICAVEL | — |
| — | RN-F1.1-04 (badge SLA vermelho) | NAO_APLICAVEL | — |

---

## Referências DRY

| Padrão | Arquivo canônico |
|--------|-----------------|
| JWT validation + `dashboard.view_own` FGAC | `F0/US-F0-001-LOGIN.md` F0.1-a (cookie `access_token`; Bearer fallback) |
| Outbox dispatcher (notificação de certificado/formativa) | `transversal/10.1-outbox-notificacao.md` |
| Emissão de certificado (trigger background) | `transversal/10.4-certificado-emissao.md` |

---

## Fora de sequência

| Item | Motivo |
|------|--------|
| CA-04 — SLA breach (célula vermelha) | Lógica exclusivamente frontend: `prazo_em < Date.now()` comparado no cliente após receber a resposta; nenhuma chamada HTTP adicional. |
| CA-06 — Estado vazio (DS/EmptyState) | Mesmo fluxo de CA-01; diferença é só o conteúdo do JSON retornado (arrays vazios). Sem variação de participantes ou mensagens. |
| CA-08 — Responsividade (375/768/1280px) | Requisito de layout CSS/NativeWind; sem troca de mensagens entre camadas. |
| RN-F1.1-04 — Badge SLA | Computação client-side derivada de `prazo_em` já presente na resposta do BFF. |

---

## F1.1-D01 — Carregamento inicial do dashboard (happy path — cache MISS)

**Escopo:** happy path — primeiro acesso ou cache Redis expirado  
**Atores:** Aluno, WebApp, JwtFilter, DashboardAlunoController, DashboardAlunoQuery, Redis  
**Pré-condições:** aluno autenticado (`mustChangePassword = false`), cookie `access_token` válido com `dashboard.view_own`

```mermaid
sequenceDiagram
    autonumber
    box #e8f4fc Cliente
        participant Aluno
        participant WebApp
    end
    box #fff8ee Servidor
        participant JwtFilter
        participant DashCtrl as DashboardAlunoController
        participant Query as DashboardAlunoQuery
        participant Redis
        participant Ports
    end

    Aluno->>WebApp: navega para /inicio
    WebApp->>JwtFilter: GET /bff/dashboard/aluno (cookie access_token)
    JwtFilter->>DashCtrl: JWT ok + dashboard.view_own ✓
    DashCtrl->>Query: execute(alunoId, authorities)
    Query->>Redis: GET bff-dashboard aluno:{id}
    Redis-->>Query: MISS
    Query->>Ports: Solicitacao + Presenca + Formativa + Iam
    Ports-->>Query: pendencias, eventos, horas, atendimentos
    Query->>Redis: PUT bff-dashboard aluno:{id} TTL=60s
    Query-->>DashCtrl: DashboardAlunoResponse
    DashCtrl-->>WebApp: 200 {_links strings, pendencias[]._link}
    WebApp-->>Aluno: dashboard (KpiRow, Pendências, Eventos, QuickTiles)
```

**Notas:**
- Auth: cookie HttpOnly `access_token` (Bearer é fallback do `JwtAuthenticationFilter`).
- Ports (nunca JPA no BFF): `SolicitacaoDashboardPort`, `PresencaDashboardPort`, `FormativaDashboardPort`, `IamDashboardPort`. Adapters JPA ficam nos módulos donos.
- Cache name `bff-dashboard`, chave `aluno:{id}`, TTL **60 s** (`CacheConfig`). Sem Redis → cache `simple` Spring.
- `_links` é objeto de **strings** (`self`, `novaSolicitacao`, `formativas`, `eventos`) — não HAL `{href}`. Itens de pendência usam `_link` (singular, string).
- `novaSolicitacao` só é preenchido se `request.open` estiver nas authorities.

**Lacunas:** nenhuma.

---

## F1.1-D02 — Carregamento do dashboard (cache HIT — FCP < 1,5 s)

**Escopo:** retorno em cache Redis dentro da janela de 60 s (RN-F1.1-10)  
**Atores:** Aluno, WebApp, JwtFilter, DashboardAlunoController, DashboardAlunoQuery, Redis  
**Pré-condições:** cache `bff-dashboard` populado há menos de 60 s para `aluno:{id}`

```mermaid
sequenceDiagram
    autonumber
    box #e8f4fc Cliente
        participant Aluno
        participant WebApp
    end
    box #fff8ee Servidor
        participant JwtFilter
        participant DashCtrl as DashboardAlunoController
        participant Query as DashboardAlunoQuery
        participant Redis
    end

    Aluno->>WebApp: navega para /inicio (ou F5/refresh)
    WebApp->>JwtFilter: GET /bff/dashboard/aluno (cookie access_token)
    JwtFilter->>DashCtrl: JWT ok + dashboard.view_own ✓
    DashCtrl->>Query: execute(alunoId, authorities)
    Query->>Redis: GET bff-dashboard aluno:{id}
    Redis-->>Query: HIT (TTL restante ≤ 60 s)
    Query-->>DashCtrl: DashboardAlunoResponse (cache)
    DashCtrl-->>WebApp: 200 {_links strings, pendencias[]._link}
    WebApp-->>Aluno: dashboard renderizado (FCP < 1,5 s)
```

**Notas:**
- HIT não chama ports. Pull-to-refresh (F1.1-D04) invalida só TanStack Query no cliente — Redis segue o TTL de 60 s.
- Resposta degradada (`_degraded=true`) **não** é gravada no cache (MISS sempre reconsulta ports).

**Lacunas:** nenhuma.

---

## F1.1-D03 — Degradação graciosa (módulo de solicitações indisponível)

**Escopo:** erro parcial de módulo — CA-05, RN-F1.1-01  
**Atores:** Aluno, WebApp, JwtFilter, DashboardAlunoQuery, SolicitacaoDashboardPort, FormativaDashboardPort  
**Pré-condições:** `SolicitacaoDashboardPort` lança timeout; demais ports respondem

```mermaid
sequenceDiagram
    autonumber
    box #e8f4fc Cliente
        participant Aluno
        participant WebApp
    end
    box #fff8ee Servidor
        participant JwtFilter
        participant Query as DashboardAlunoQuery
        participant SolPort as SolicitacaoDashboardPort
        participant FormPort as FormativaDashboardPort
    end

    Aluno->>WebApp: navega para /inicio
    WebApp->>JwtFilter: GET /bff/dashboard/aluno (cookie access_token)
    JwtFilter->>Query: JWT ok + dashboard.view_own ✓
    Query->>SolPort: findPendenciasAluno / findRecentesAluno
    Query->>FormPort: sumHorasAprovadas (paralelo)
    SolPort-->>Query: timeout / erro interno
    FormPort-->>Query: horas OK
    Query-->>WebApp: 200 {pendencias:null, _degraded:true, _links}
    WebApp-->>Aluno: dashboard parcial + DS/AlertBanner na seção Solicitações
```

**Notas:**
- Controller omitido para caber ports: `DashboardAlunoController` só delega a `DashboardAlunoQuery.execute`.
- HTTP continua **200**; `pendencias`/`ultimasSolicitacoes` null + `_degraded=true`. Não há PUT no Redis quando degradado.
- `try/catch` por port — falha de um bloco não cancela os demais.

**Lacunas:** nenhuma.

---

## F1.1-D04 — Pull-to-refresh no mobile (CA-07, RN-F1.1-11)

**Escopo:** pull-to-refresh reinvalida cache TanStack Query e rebusca dados  
**Atores:** Aluno, MobileApp, DashboardAlunoQuery, Redis  
**Pré-condições:** aluno autenticado no app mobile, dashboard já carregado

```mermaid
sequenceDiagram
    autonumber
    box #e8f4fc Cliente
        participant Aluno
        participant MobileApp
    end
    box #fff8ee Servidor
        participant JwtFilter
        participant Query as DashboardAlunoQuery
        participant Redis
        participant Ports
    end

    Aluno->>MobileApp: gesto pull-to-refresh
    MobileApp->>MobileApp: invalidateQueries([dashboard])
    MobileApp->>JwtFilter: GET /bff/dashboard/aluno (cookie access_token)
    JwtFilter->>Query: JWT ok + dashboard.view_own ✓
    Query->>Redis: GET bff-dashboard aluno:{id}
    Redis-->>Query: HIT (TTL restante) ou MISS
    Query->>Ports: ports só se MISS
    Query->>Redis: PUT bff-dashboard aluno:{id} TTL=60s (se MISS)
    Query-->>MobileApp: 200 {_links strings, pendencias[]._link}
    MobileApp-->>Aluno: indicador some, tela atualizada
```

**Notas:**
- Pull-to-refresh **não** apaga Redis. HIT devolve o agregado de até 60 s. Bearer só se o mobile não enviar cookie.
- `DashboardAlunoController` omitido (delega 1:1 ao Query).

**Lacunas:** nenhuma.
