@agents/ux-ui-specialist.md

## Missão — Fase F1 (Aluno)

Criar no Figma **todas as 20 telas do fluxo F1**, como frames de alta fidelidade, **somente com instâncias** da biblioteca de Design System.

**Prioridades:** F1.1 + F1.2 = **P0** (MVP v1) · F1.7–F1.9 = **P1** (MVP v2) · demais = **P2**.

> **F1.1 (`telas1/F1.1-inicio-aluno.md`) é o BLUEPRINT DashboardA** de todo o app autenticado. Criar primeiro e usar como referência estrutural para F3.1, F5.1, F2.1.

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Convenções:** `telasFigma/00-CONVENCOES.md`
4. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
5. **Inventário DS:** `designSystem/inventario-design-system.md`
6. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §3 (F1)
7. **Presença v4.1:** `foundationDocs/analysis/endpoints_canonicos_presenca_eventos_v4.md` (F1.17, F1.18)

## Telas desta fase (ordem recomendada)

| # | Spec | Rota | Prioridade |
|---|------|------|------------|
| 1 | `F1.1-inicio-aluno.md` | `/inicio` | P0 — BLUEPRINT |
| 2 | `F1.2-primeiro-acesso.md` | `/primeiro-acesso` | P0 |
| 3 | `F1.7-solicitacoes-lista.md` | `/solicitacoes` | P1 |
| 4 | `F1.8-solicitacoes-nova.md` | `/solicitacoes/nova` | P1 |
| 5 | `F1.9-solicitacoes-detalhe.md` | `/solicitacoes/:id` | P1 |
| 6 | `F1.3-perfil.md` | `/perfil` | P2 |
| 7 | `F1.4-perfil-seguranca.md` | `/perfil/seguranca` | P2 |
| 8 | `F1.5-perfil-notificacoes.md` | `/perfil/notificacoes` | P2 |
| 9 | `F1.6-comunicacao.md` | `/comunicacao` | P2 |
| 10 | `F1.10-formativas-lista.md` | `/formativas` | P2 |
| 11 | `F1.11-formativas-nova.md` | `/formativas/nova` | P2 |
| 12 | `F1.12-formativas-detalhe.md` | `/formativas/:id` | P2 |
| 13 | `F1.13-estagios-lista.md` | `/estagios` | P2 |
| 14 | `F1.14-estagios-detalhe.md` | `/estagios/:id` | P2 |
| 15 | `F1.15-tccs-lista.md` | `/tccs` | P2 |
| 16 | `F1.16-tccs-detalhe.md` | `/tccs/:id` | P2 |
| 17 | `F1.17-eventos-lista.md` | `/eventos` | P2 |
| 18 | `F1.18-eventos-presenca.md` | `/eventos/:id/presenca` | P2 |
| 19 | `F1.19-certificados.md` | `/certificados` | P2 |
| 20 | `F1.20-meus-atendimentos.md` | `/meus-atendimentos` | P2 |

## Shells e componentes DS esperados

- **Shell/AppLayout** — todas exceto F1.2 (layout simplificado sem nav) e F1.18 (pode usar AuthLayout/card central)
- **DS/KpiCard**, **DS/QuickTile**, **DS/PendenciaItem**, **DS/EventoRow**, **DS/DataTable** — dashboard F1.1
- **DS/WizardStepper**, **DS/DynamicForm**, **DS/AttachmentUpload** — F1.8 (P1)
- **DS/TimelineItem**, **DS/AttachmentList** — F1.9
- **DS/AttendanceWidget** (variants QR_SINGLE, QR_DUAL, SECRET_SINGLE, SECRET_DUAL) — F1.17, F1.18
- **DS/Tabs**, **CommunicationRow** — F1.6

## Regras imutáveis

- **DashboardA apenas** — ignorar DashboardB/C e toggle de versão.
- Variables do DS apenas; UI cega a perfis (anotar `_links` nos botões).
- Estados Loading / Empty / Error em listas e cards.
- Skills `figma-use` + `figma-generate-design` antes de escrever no Figma.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro dos frames `F1.x — …` na página `Telas / F1 — Aluno`.
- F1.8: frames separados para passos 1, 2 e 3 do wizard + estados de validação.

## Workflow Figma

1. Inspecionar DS + gap analysis.
2. Página **`Telas / F1 — Aluno`** no arquivo secretariaOnline2 — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. Criar **F1.1 primeiro** (desktop 1440×1024 + mobile 375×812) + variantes Skeleton/Empty.
4. Demais telas na ordem da tabela; ler cada `telasFigma/telas1/F1.*.md`.
5. F1.17: modal detalhe + AttendanceWidget dinâmico.
6. F1.18: component set por `attendanceMode` × fase.

## Entregáveis

- URLs + node-id de cada frame
- Confirmação de que F1.1 está marcado como referência DashboardA
- Gaps de DS e pendências

## Não fazer

- Não criar código React
- Não duplicar variantes B/C de dashboard
- Não fixar presença em “10 min PIN” — usar janelas configuráveis v4.1
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
