@agents/ux-ui-specialist.md

## Missão — Fase F5 (Secretaria)

Criar no Figma **todas as 19 telas do fluxo F5**, como frames de alta fidelidade com instâncias do Design System.

**Prioridade:** P2 · **Plataforma:** web (desktop-first).

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Convenções:** `telasFigma/00-CONVENCOES.md`
4. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
5. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §7 (F5)
6. **Presença:** `endpoints_canonicos_presenca_eventos_v4.md` (F5.14, F5.15)
7. **Specs desta fase:** arquivos em `telasFigma/telas5/`

## Telas desta fase (ordem recomendada)

| # | Spec | Rota | Nota |
|---|------|------|------|
| 1 | `F5.1-inicio-secretaria.md` | `/inicio` | Variante DashboardA + SLA |
| 2 | `F5.2-solicitacoes-fila.md` | `/solicitacoes` | Fila central + export |
| 3 | `F5.5-secretaria-atrasados.md` | `/secretaria/atrasados` | Variante F5.2 SLA breach |
| 4 | `F5.3-solicitacoes-nova-interna.md` | `/solicitacoes/nova` | Variante F1.8 + campo aluno |
| 5 | `F5.4-solicitacoes-deliberar-secretaria.md` | `/solicitacoes/:id/deliberar` | **Reutilizar F3.4** |
| 6 | `F5.6-secretaria-alunos.md` | `/secretaria/alunos` | CRUD + Drawer |
| 7 | `F5.7-secretaria-cursos.md` | `/secretaria/cursos` | CRUD |
| 8 | `F5.8-secretaria-disciplinas.md` | `/secretaria/disciplinas` | CRUD + reorder |
| 9 | `F5.9-secretaria-calendarios.md` | `/secretaria/calendarios` | Calendar + tabs |
| 10 | `F5.10-secretaria-egressos.md` | `/secretaria/egressos` | Lista + export |
| 11 | `F5.11-secretaria-diplomas.md` | `/secretaria/diplomas` | Wizard 2 passos |
| 12 | `F5.12-secretaria-autorizacoes-imagem.md` | `/secretaria/autorizacoes-imagem` | Lista densa + batch |
| 13 | `F5.13-secretaria-atendimentos.md` | `/secretaria/atendimentos` | Form registro |
| 14 | `F5.14-secretaria-eventos-lista.md` | `/secretaria/eventos` | Igual F3.2 lista |
| 15 | `F5.15-secretaria-eventos-operacao.md` | `/secretaria/eventos/:id/operacao` | **Reutilizar F3.2c** |
| 16 | `F5.16-secretaria-importacoes.md` | `/secretaria/importacoes` | Wizard 4 passos |
| 17 | `F5.17-secretaria-exportacoes.md` | `/secretaria/exportacoes` | Card grid async |
| 18 | `F5.18-secretaria-estatisticas.md` | `/secretaria/estatisticas` | Charts 2×2 |
| 19 | `F5.19-secretaria-tarefas.md` | `/secretaria/tarefas` | Kanban opcional P3 |

## Reutilização obrigatória (DRY)

| Tela F5 | Reutilizar frame |
|---------|------------------|
| F5.3 | F1.8 wizard + Combobox "Em nome de" |
| F5.4 | F3.4 deliberar (instância; breadcrumb Secretaria) |
| F5.5 | F5.2 com filtro SLA |
| F5.14 | F3.2 lista eventos |
| F5.15 | F3.2c operação evento |

## Componentes DS adicionais

- **DS/Drawer**, **DS/Combobox** — CRUDs
- **DS/Calendar** — F5.9
- **DS/Chart** wrappers — F5.18
- **DS/WizardStepper** — F5.11, F5.16
- **FilterBar** persistente — F5.2

## Regras imutáveis

- Desktop 1280px+; skills `figma-use` + `figma-generate-design`.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro dos frames `F5.x — …` na página `Telas / F5 — Secretaria`.
- SLA breach: destaque `status/danger` consistente com F5.1.
- F5.19 opcional — criar só se flag produto ativa (anotar no frame).

## Workflow Figma

1. Gap analysis extensa (CRUD + charts + wizards).
2. Página **`Telas / F5 — Secretaria`** (sub-seções por grupo: filas, CRUD, eventos, ops) — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. Criar F5.1 após F1.1; reutilizar antes de duplicar.
4. Batch: CRUDs F5.6–F5.8 compartilham Pattern CRUD admin.

## Entregáveis

- URLs + node-ids das 19 telas (ou 18 se F5.19 omitida)
- Mapa de reutilização vs. frames novos

## Não fazer

- Não duplicar F3.4 / F3.2c como designs independentes
- Não criar segunda wizard do zero para F5.3
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
