@agents/ux-ui-specialist.md

## Missão — Fase F6 (Coordenação)

Criar no Figma **as 2 telas do fluxo F6**, como frames de alta fidelidade com instâncias do Design System.

**Prioridade:** P2 · Coordenação **reaproveita** telas de Secretaria (F5) + estas duas rotas aditivas.

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Specs:** `telasFigma/telas6/F6.1-coordenacao-configurar-curso.md`, `telasFigma/telas6/F6.2-coordenacao-relatorios.md`
4. **Convenções:** `telasFigma/00-CONVENCOES.md`
5. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
6. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §8 (F6)

## Telas desta fase

| # | Spec | Rota |
|---|------|------|
| 1 | `F6.1-coordenacao-configurar-curso.md` | `/coordenacao/cursos/:id/configurar` |
| 2 | `F6.2-coordenacao-relatorios.md` | `/coordenacao/relatorios` |

## Diretrizes específicas

### F6.1 — Configurar curso
- **Shell/AppLayout** + breadcrumb Curso > Configurar
- Form em **Cards empilhados**: horas formativas mínimas, calendário 15/18 semanas, perfis banca TCC, regimento
- Pattern **SettingsPage**; footer Salvar/Cancelar sticky

### F6.2 — Relatórios coordenação
- **Derivar de F5.18** (estatísticas secretaria) com charts comparativos e séries históricas
- Filtros: curso, período, evasão, taxa aprovação formativas
- Desktop 1440×1024

## Regras imutáveis

- Variables do DS; skills `figma-use` + `figma-generate-design`.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro dos frames `F6.x — …` na página `Telas / F6 — Coordenação`.
- Não recriar CRUDs de secretaria — só estas duas telas.
- Estados Loading nos charts (skeleton).

## Workflow Figma

1. Gap analysis (Settings form + charts).
2. Página **`Telas / F6 — Coordenação`** — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. F6.2 após F5.18 existir — duplicar e adaptar.
4. Checklist `00-CONVENCOES.md`.

## Entregáveis

- URLs + node-ids F6.1 e F6.2
- Nota de derivação F6.2 ← F5.18

## Não fazer

- Não recriar fila secretaria ou admin de cursos (já F5)
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
