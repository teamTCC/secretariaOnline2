@agents/ux-ui-specialist.md

## Missão — Fase F7 (Admin / Plataforma)

Criar no Figma **todas as 9 telas do fluxo F7**, como frames de alta fidelidade com instâncias do Design System.

**Prioridade:** P2 (F7.9 saúde do sistema = **P3** opcional).

## Referências obrigatórias

1. **Design System:** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Convenções:** `telasFigma/00-CONVENCOES.md`
4. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
5. **Inventário DS:** `designSystem/inventario-design-system.md` §8 (editores admin)
6. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §9 (F7)
7. **ADR-003:** tipos de solicitação / workflow engine (F7.4)
8. **Specs desta fase:** arquivos em `telasFigma/telas7/`

## Telas desta fase (ordem recomendada)

| # | Spec | Rota | Nota |
|---|------|------|------|
| 1 | `F7.1-admin-usuarios.md` | `/admin/usuarios` | CRUD list |
| 2 | `F7.8-admin-reset-senha.md` | `/admin/usuarios/:id/reset-senha` | Modal confirmação |
| 3 | `F7.2-admin-perfis.md` | `/admin/perfis` | CRUD roles |
| 4 | `F7.3-admin-autoridades.md` | `/admin/autoridades` | Matriz role×authority |
| 5 | `F7.4-admin-tipos-solicitacao.md` | `/admin/tipos-solicitacao` | **Crítico ADR-003** |
| 6 | `F7.5-admin-templates-comunicacao.md` | `/admin/templates-comunicacao` | Markdown editor |
| 7 | `F7.6-admin-jobs.md` | `/admin/jobs` | Outbox + scheduled jobs |
| 8 | `F7.7-admin-audit-log.md` | `/admin/audit-log` | Diff drawer |
| 9 | `F7.9-admin-sistema-saude.md` | `/admin/sistema/saude` | P3 opcional |

## Shell

- **Shell/AdminLayout** — AppLayout + seção nav "Administração" + breadcrumb reforçado
- Largura mínima **1440px** para editores (F7.4, F7.5)

## Componentes DS específicos

- **F7.4:** split 3 painéis — lista tipos | CodeEditor JSON Schema | FormPreview + WorkflowCanvas
- **F7.5:** MarkdownEditor + histórico versões
- **F7.6:** Tabs PENDING/SENT/FAILED/DEAD + botão Reentregar
- **F7.7:** DiffViewer JSON side-by-side em Drawer
- **F7.8:** Modal only — sem campo senha visível

## Regras imutáveis

- F7.4 é coração DRY — documentar estados inválidos de schema/workflow.
- Variables + `surface/code` para blocos JSON/diff.
- Skills `figma-use` + `figma-generate-design`.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — instâncias DS **somente** dentro dos frames `F7.x — …` na página `Telas / F7 — Admin`.
- F7.8: operador **nunca** vê senha — só confirma envio de link.

## Workflow Figma

1. Gap analysis (editores, WorkflowCanvas, DiffViewer).
2. Página **`Telas / F7 — Admin`** — **única página** para instanciar componentes desta fase (`00-FIGMA-PAGE-CONTEXT.md`).
3. CRUDs F7.1–F7.3: Pattern CRUD admin compartilhado.
4. F7.4: frame wide dedicado + wireframe workflow graph.
5. F7.9: omitir ou marcar "P3 — extra MVP".

## Entregáveis

- URLs + node-ids
- F7.4 documentado como referência workflow engine
- Lista gaps de componentes editor

## Não fazer

- Não simplificar F7.4 a um textarea JSON sem preview
- Não mostrar senha em F7.8
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames de tela
