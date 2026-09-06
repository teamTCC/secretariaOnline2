@agents/ux-ui-specialist.md

## Missão — Fase F2 (Egresso)

Criar no Figma a **tela do fluxo F2** (dashboard read-only pós-graduação), como frame de alta fidelidade com instâncias do Design System.

**Prioridade:** P2.

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Spec:** `telasFigma/telas2/F2.1-egresso-inicio.md`
4. **Blueprint:** derivar estrutura de `telasFigma/telas1/F1.1-inicio-aluno.md` (DashboardA) em modo **read-only**
5. **Convenções:** `telasFigma/00-CONVENCOES.md`
6. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
7. **Fluxos:** `foundationDocs/analysis/fluxos_por_perfil.md` §3 (F2)

## Tela desta fase

| Spec | Rota | Shell |
|------|------|-------|
| `F2.1-egresso-inicio.md` | `/egresso/inicio` | AppLayout |

## Diretrizes específicas F2

- **Duplicar/adaptar** frame F1.1 — Dashboard Aluno; remover CTAs de criação (nova solicitação, etc.).
- Manter: histórico, certificados, diploma, dados de colação.
- Botão **Reemitir PDF** (secondary) onde `_links.reemitir` aplicável.
- Badges **Concluído** / read-only; sem QuickTiles de ações de aluno ativo.
- Egresso reutiliza rotas de perfil/certificados — não recriar essas telas aqui.

## Regras imutáveis

- Variables do DS; skills `figma-use` + `figma-generate-design`.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro do frame `F2.1 — …` na página `Telas / F2 — Egresso`.
- Anotar dependência HATEOAS nos CTAs restantes.
- Desktop 1440×1024 + mobile 375×812.

## Workflow Figma

1. Gap analysis (mesmos componentes do dashboard F1.1).
2. Página **`Telas / F2 — Egresso`** — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. Criar frame `F2.1 — Dashboard Egresso` + estados Loading/Empty.
4. Screenshot + checklist `00-CONVENCOES.md`.

## Entregáveis

- URL + node-id do frame
- Nota de diferenças vs. F1.1

## Não fazer

- Não criar telas de perfil/certificados (já em F1)
- Não adicionar fluxos de aluno ativo
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
