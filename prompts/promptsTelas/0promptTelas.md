@agents/ux-ui-specialist.md

## Missão — Fase F0 (Público / Não autenticado)

Criar no Figma **todas as 7 telas do fluxo F0**, como frames de alta fidelidade, **somente com instâncias** da biblioteca de Design System — sem hex/px soltos, sem recriar componentes que já existem no DS.

**Prioridade:** F0.1 é **P0** (MVP v1); demais telas F0 são **P2**.

## Referências obrigatórias (ler antes de agir)

1. **Design System (biblioteca):** https://www.figma.com/design/gF97YuhDuYr5Sy9wagZLoJ/designSystem-v2?node-id=46-2&t=8uQEkywAyEhF2IL0-1
2. **Arquivo destino das telas:** https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=0-1&t=BN5rYwsMi72Qnvpj-1
3. **Specs desta fase:** arquivos listados abaixo em `telasFigma/telas0/`
4. **Convenções:** `telasFigma/00-CONVENCOES.md`
5. **Contexto de página Figma MCP:** `telasFigma/promptsTelas/00-FIGMA-PAGE-CONTEXT.md` (**obrigatório**)
6. **Inventário DS:** `designSystem/inventario-design-system.md`
7. **Mapa de rotas:** `foundationDocs/analysis/telas.md` §2 (F0)

## Telas desta fase (ordem de criação)

| # | Spec | Rota | Shell | Variantes obrigatórias |
|---|------|------|-------|------------------------|
| 1 | `F0.1-login.md` | `/login` | AuthLayout | Default, Error, Loading |
| 2 | `F0.2-recuperar-senha.md` | `/recuperar-senha` | AuthLayout | Default, Success |
| 3 | `F0.3-nova-senha.md` | `/nova-senha?token=` | AuthLayout | Valid token, Invalid token |
| 4 | `F0.4-contato.md` | `/contato` | PublicLayout | Desktop + Mobile |
| 5 | `F0.5-erro.md` | `/erro/:codigo` | PublicLayout | 401, 403, 404, 500 |
| 6 | `F0.6-verificar-protocolo.md` | `/publico/verificar-protocolo/:id` | PublicLayout | OK, Upload match, Not found |
| 7 | `F0.7-verificar-certificado.md` | `/publico/verificar-certificado/:hash` | PublicLayout | Válido, Inválido |

## Shells e componentes DS esperados

- **Shell/AuthLayout** — F0.1, F0.2, F0.3
- **Shell/PublicLayout** — F0.4, F0.5, F0.6, F0.7
- **DS/Input**, **DS/Button**, **DS/Card**, **DS/AlertBanner**, **DS/EmptyState**, **DS/FileDropzone** (verificadores)
- **DS/PasswordStrengthMeter** — F0.3 (se existir no DS; senão anotar gap)

## Regras imutáveis

- Usar **Figma Variables** do DS (`color/*`, `space/*`, `radius/*`, tipografia) — zero valores hardcoded.
- Telas = composição de `Shell/*` + `DS/*` + `Main/*`.
- **Não implementar código React** nesta tarefa — apenas Figma.
- Antes de cada escrita no Figma: carregar skills `figma-use` e `figma-generate-design`; usar `search_design_system` no arquivo do DS antes de criar qualquer componente novo.
- **Contexto de página:** seguir `00-FIGMA-PAGE-CONTEXT.md` — `setCurrentPageAsync` em todo script; instâncias DS **somente** dentro dos frames de tela; nunca órfãs na `Page 1`.
- Telas públicas **não** usam AppLayout nem sidebar.

## Workflow Figma

1. **Inspecionar DS** (link acima): Variables, `Shell/AuthLayout`, `Shell/PublicLayout`, componentes listados.
2. **Gap analysis:** listar componentes faltantes; não desenhar com retângulos soltos.
3. No arquivo **secretariaOnline2**, criar página **`Telas / F0 — Público`** e usar **somente ela** para instanciar componentes (ver `00-FIGMA-PAGE-CONTEXT.md`).
4. **Por tela**, na ordem da tabela:
   - Ler o `.md` em `telasFigma/telas0/`
   - Frame desktop **1440×900** + mobile **375×812** quando spec indicar “Web + Mobile”
   - Instâncias do DS (library `designSystem-v2` habilitada)
   - Nomear frame: `F0.x — {nome}`
5. Checklist `00-CONVENCOES.md` + screenshot de cada frame.

## Entregáveis

- URLs com **node-id** de cada frame criado
- Componentes DS reutilizados vs. gaps
- Pendências de UX ou DS

## Não fazer

- Não criar `frontend-web/` nem código
- Não usar AppLayout nesta fase
- Não inventar rotas fora de `telas.md`
- Não deixar instâncias `DS/*` / `Shell/*` na `Page 1` ou fora dos frames `F0.x — …`
