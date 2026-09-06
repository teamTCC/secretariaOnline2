@agents/ux-ui-specialist.md

## Missão — Fase F4 (Comissões: CAAF e COE)

Criar no Figma **as 2 telas do fluxo F4** (dashboards de comissão), como frames de alta fidelidade com instâncias do Design System.

**Prioridade:** P2 · **Plataforma:** web (desktop-first).

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Specs:** `telasFigma/telas4/F4.1-comissoes-caaf.md`, `telasFigma/telas4/F4.2-comissoes-coe.md`
4. **Convenções:** `telasFigma/00-CONVENCOES.md`
5. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
6. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §6 (F4)

## Telas desta fase

| # | Spec | Rota |
|---|------|------|
| 1 | `F4.1-comissoes-caaf.md` | `/comissoes/caaf` |
| 2 | `F4.2-comissoes-coe.md` | `/comissoes/coe` |

## Padrão visual (Pattern CommissionDashboard)

Ambas seguem o mesmo pattern:

- **Shell/AppLayout**
- **KpiRow** — indicadores da fila
- **DataTable** com checkbox seleção múltipla
- **Toolbar** ações em lote (atribuir membro, revisar)
- Links abrem detalhe em `/formativas/:id` (CAAF) ou estágio (COE)

**F4.2** = duplicar estrutura F4.1 com labels e ícones de estágio/COE.

## Regras imutáveis

- Variables do DS; seleção em lote acessível (checkbox + select-all).
- Ações via `_links` (atribuir, revisar).
- Skills `figma-use` + `figma-generate-design`.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro dos frames `F4.x — …` na página `Telas / F4 — Comissões`.
- Frame desktop **1440×1024**; mobile opcional (web preferencial).

## Workflow Figma

1. Gap analysis: DataTable com checkbox, KpiCard.
2. Página **`Telas / F4 — Comissões`** — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. Criar F4.1 completo; F4.2 como variante/duplicata adaptada.
4. Estados: Loading, Empty, seleção múltipla ativa.

## Entregáveis

- URLs + node-ids F4.1 e F4.2
- Relação F4.1 → F3.5 (professor CAAF) documentada

## Não fazer

- Não recriar detalhe formativa/estágio (já em F1/F3)
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
