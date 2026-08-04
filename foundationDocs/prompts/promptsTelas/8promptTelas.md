@agents/ux-ui-specialist.md

## Missão — Fase F8 (Cross-cutting)

Criar no Figma **as 2 telas do fluxo F8** (busca global e suporte), como frames de alta fidelidade com instâncias do Design System.

**Prioridade:** P2 · Disponível a **todo usuário logado** (F8.2); busca com escopo por capability (F8.1).

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Specs:** `telasFigma/telas8/F8.1-buscar.md`, `telasFigma/telas8/F8.2-suporte.md`
4. **Convenções:** `telasFigma/00-CONVENCOES.md`
5. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
6. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §10 (F8)

## Telas desta fase

| # | Spec | Rota | Apresentação |
|---|------|------|--------------|
| 1 | `F8.1-buscar.md` | `/buscar?q=` | Modal Command Palette (Ctrl+K) |
| 2 | `F8.2-suporte.md` | `/suporte` | Página AppLayout |

## Diretrizes específicas

### F8.1 — Busca global
- **Overlay modal** max-w 640px; disparado pela SearchBar do Topbar (AppLayout)
- Input grande + resultados agrupados por tipo (ícone + label): aluno, protocolo, evento…
- Estados: Empty, Loading, Results
- Atalhos teclado ↑↓ Enter (anotar no frame)
- Mobile: **fullscreen**

### F8.2 — Suporte
- **Shell/AppLayout**
- Accordion FAQ categorizado + form novo ticket (Textarea + assunto)
- Pattern SupportPage

## Componentes DS

- **DS/CommandPalette** — F8.1 (`componentKey` `f8bb6966942465200dbe82db51a4eca9d34f8687` · `empty|loading|results`)
- **DS/SearchResultGroup** — F8.1 Results (`92bb58bf39cc1967b16d47a68ca46a00f0392168`)
- **DS/SearchResultItem** — F8.1 typed (`59eda2a45ccc429efd6b9366d882b6dfed7af3bf` · `aluno|protocolo|evento`)
- **DS/Accordion** — F8.2 (`56826fc223f08346da02bda76b5a86d13d62dc95` · `open|closed`)
- **DS/Input/Search** — trigger no Topbar (`7149fcd5ff6d507f1534d54889f915c9383deeba`)

**Library:** `designSystem v2` · `search_design_system` com `fileKey=gF97YuhDuYr5Sy9wagZLoJ` (ou `includeLibraryKeys` no arquivo telas).

**Scripts pós-publish:** `telasFigma/scripts/sync-f8-ds-instances.js` → `populate-f8-results.js` → `setup-f8-prototype.js`

## Regras imutáveis

- F8.1 não é página full — é **modal sobre** qualquer tela autenticada; criar frame com scrim `surface/overlay`.
- Variables do DS; combobox pattern a11y.
- Skills `figma-use` + `figma-generate-design`.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro dos frames `F8.x — …` na página `Telas / F8 — Cross-cutting`.

## Workflow Figma

1. Gap analysis CommandPalette + Accordion.
2. Página **`Telas / F8 — Cross-cutting`** — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. F8.1: frame exemplo com AppLayout desfocado ao fundo + modal em foco.
4. F8.2: página completa + estado submit.

## Entregáveis

- URLs + node-ids
- Protótipo F8.1 abrindo a partir de Topbar mock

## Não fazer

- Não criar busca como página lista separada (é command palette)
- Não hardcodar resultados por perfil — anotar escopo API
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
