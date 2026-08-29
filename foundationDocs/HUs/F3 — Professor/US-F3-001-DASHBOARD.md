# US-F3-001 — Dashboard do Professor

| Campo | Valor |
|-------|-------|
| **ID** | US-F3-001 |
| **Épico** | PROF-DASH |
| **Tela** | F3.1 — `/inicio` |
| **Prioridade** | P2 |
| **Plataforma** | Web + Mobile |
| **Capability** | `dashboard.view_self_professor` |
| **API primária** | `GET /bff/dashboard/professor` |
| **Frames Figma** | [Default/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=243-16994) · [Skeleton/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=243-17311) · [Default/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=243-17324) · [Empty/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=243-17629) |
| **Spec de tela** | `telasFigma/telas3/F3.1-inicio-professor.md` |

---

## 1. História de Usuário

> **Como** professor autenticado,  
> **Quero** ver um painel unificado com todas as minhas filas de trabalho (solicitações para deliberar, eventos do dia, formativas CAAF, estágios e TCCs pendentes),  
> **Para** ter visão imediata do que precisa da minha atenção e acessar rapidamente cada função sem precisar navegar por menus.

---

## 2. Regras de Negócio

| ID | Regra |
|----|-------|
| **RN-F3.1-01** | O dashboard usa a **mesma estrutura DashboardA** do aluno (F1.1), mas com blocos de conteúdo diferentes, servidos por `GET /bff/dashboard/professor`. A UI não usa perfil para decidir o que exibir — usa `_links` e presença de blocos na resposta do BFF. |
| **RN-F3.1-02** | O **KpiRow** exibe: pendências para deliberar, formativas para revisão CAAF, eventos hoje (como organizador), SLA warnings (solicitações prestes a vencer). |
| **RN-F3.1-03** | O bloco **"Formativas CAAF"** (lista de entradas para revisar) é renderizado **somente** se o BFF retornar esse bloco — ou seja, apenas se o professor tem `formative.review` e há itens pendentes. Professores sem vínculo à CAAF nunca veem esse bloco. |
| **RN-F3.1-04** | O bloco **"Fila de solicitações"** exibe solicitações com `canDeliberate=true` para aquele professor, filtradas pelo BFF. |
| **RN-F3.1-05** | O bloco **"Meus eventos"** exibe eventos do professor com `event.manage`. Eventos com janela de presença ativa hoje ficam destacados com badge "Em andamento". |
| **RN-F3.1-06** | Degradação graciosa: se um módulo do BFF falhar, o bloco correspondente exibe `DS/AlertBanner warning` e os demais blocos renderizam normalmente. |

---

## 3. Critérios de Aceitação

### CA-01 — Dashboard carregado com blocos HATEOAS

```gherkin
Dado que o professor está autenticado com capabilities event.manage e request.deliberate
Quando acessa /inicio
Então o BFF retorna blocos para: KpiRow, fila solicitações, meus eventos
  E o bloco "Formativas CAAF" NÃO aparece (professor não tem formative.review)
  E todos os CTAs derivam de _links na resposta

Dado que o mesmo professor tem também formative.review
Então o bloco "Formativas CAAF" aparece com a lista de entradas pendentes
```

### CA-02 — KpiCard SLA warning

```gherkin
Dado que há 2 solicitações com prazo_em < now + 24h
Quando o dashboard carrega
Então o KpiCard de SLA exibe badge warning: "2 solicitações urgentes"
  E ao clicar navega para /solicitacoes?to=me com filtro de urgência aplicado
```

### CA-03 — Evento ativo hoje com destaque

```gherkin
Dado que o professor tem um evento em andamento (estado Em_andamento)
Quando o dashboard carrega
Então o card do evento exibe badge success "Em andamento"
  E o CTA "Operar evento" leva para /professor/eventos/:id/operacao
  E este CTA aparece somente se _links.operar existe na resposta
```

### CA-04 — Degradação graciosa

```gherkin
Dado que o módulo de solicitações está indisponível
Quando o BFF tenta agregar os dados
Então o bloco "Fila de solicitações" exibe DS/AlertBanner warning
  E os demais blocos (eventos, formativas) renderizam normalmente
```

---

## 4. Contrato de API (BFF)

```http
GET /bff/dashboard/professor
Cookie: access_token={jwt}
```

```json
{
  "kpis": {
    "pendentesDeliberar": 3,
    "formativasRevisar": 7,
    "eventosHoje": 1,
    "slaUrgentes": 2
  },
  "filaSolicitacoes": [ { "numero": "2026-0042", "tipo": "...", "prazoEm": "..." } ],
  "meusEventos": [ { "titulo": "...", "estado": "EM_ANDAMENTO", "_links": { "operar": {...} } } ],
  "formativasCaaf": null,
  "_links": {
    "novoEvento": { "href": "/professor/eventos/novo" }
  }
}
```

---

## 5. Referências

| Recurso | Link / Caminho |
|---------|---------------|
| Spec de tela | `telasFigma/telas3/F3.1-inicio-professor.md` |
| Fluxo F3.1 | `foundationDocs/analysis/fluxos_por_perfil.md` §4 F3.1 |
| DashboardA (blueprint) | [US-F1-001](../F1/US-F1-001-DASHBOARD.md) |
| Página Figma F3 | [Telas / F3 — Professor](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=209-339) |
| Frame principal | [F3.1 — Dashboard Professor / Default / Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=243-16994) |
