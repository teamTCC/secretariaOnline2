@agents/ux-ui-specialist.md

## Missão — Fase F3 (Professor)

Criar no Figma **todas as 10 telas do fluxo F3**, como frames de alta fidelidade com instâncias do Design System.

**Prioridade:** P2 (operação de eventos F3.2c é crítica para presença v4.1).

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Convenções:** `telasFigma/00-CONVENCOES.md`
4. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
5. **Presença v4.1:** `foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md`
6. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §5 (F3)
7. **Specs desta fase:** arquivos em `telasFigma/telas3/`

## Telas desta fase (ordem recomendada)

| # | Spec | Rota |
|---|------|------|
| 1 | `F3.1-inicio-professor.md` | `/inicio` (visão professor) |
| 2 | `F3.2-professor-eventos-lista.md` | `/professor/eventos` |
| 3 | `F3.2-professor-eventos-detalhe.md` | `/professor/eventos/:id` |
| 4 | `F3.2-professor-eventos-operacao.md` | `/professor/eventos/:id/operacao` |
| 5 | `F3.3-solicitacoes-deliberar-fila.md` | `/solicitacoes?to=me` |
| 6 | `F3.4-solicitacoes-deliberar.md` | `/solicitacoes/:id/deliberar` |
| 7 | `F3.5-formativas-revisao.md` | `/formativas?to=me` (somente CAAF) |
| 8 | `F3.6-estagios-revisao.md` | `/estagios?to=me` |
| 9 | `F3.7-tccs-revisao.md` | `/tccs?to=me` |
| 10 | `F3.8-comunicacao-publicar.md` | `/comunicacao/publicar` |

## Componentes DS críticos

- **F3.1:** derivar de DashboardA; card CAAF **condicional** (só se dados no BFF — anotar)
- **F3.2 detalhe:** `WindowBuilder`, radios `attendanceMode`, DateTimePicker
- **F3.2 operação:** `DS/OperationPanel` — QRDisplay, PINDisplay, Countdown; variants por modo × fase
- **F3.4:** DeliberationPanel + Timeline; Deferir/Indeferir/Ajustes via `_links`
- **F3.8:** Markdown editor split + preview (web only)

## Regras imutáveis

- F3.2c será **reutilizado** por F5.15 — criar como componente master reutilizável.
- F3.4 será **reutilizado** por F5.4 — mesmo frame, contexto secretaria depois.
- UI cega a perfis; card formativas CAAF não por role hardcoded.
- Skills `figma-use` + `figma-generate-design`.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro dos frames `F3.x — …` na página `Telas / F3 — Professor`.

## Workflow Figma

1. Gap analysis (OperationPanel, WindowBuilder prioritários).
2. Página **`Telas / F3 — Professor`** — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. F3.1 após F1.1 existir no arquivo telas.
4. F3.2c: component set `mode × phase` (mín. 4 modos presença).
5. Demais telas conforme specs individuais.

## Entregáveis

- URLs + node-ids de todos os frames
- Component set OperationPanel documentado para reuso F5.15

## Não fazer

- Não dar CRUD de aula/chamada SIGA
- Não duplicar F3.4/F3.2c para secretaria nesta fase
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
