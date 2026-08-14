# Prompt Cursor Agent — Figma MCP: Variables + Componentes Design System

> **Uso:** colar no **chat do Cursor** (Agent mode) com Figma MCP conectado e autenticado.  
> **Agente:** `@agents/ux-ui-specialist.md`  
> **Inventário:** `@designSystem/inventario-design-system.md` (v2.0)  
> **Versão prompt:** 2.2 — execução sequencial reforçada (1 fase/turno + GATE + checkpoints).

---

## Prompt (copiar abaixo)

Você é o **UX/UI Specialist + Design System Engineer** do SecretariaOnline2. Execute **exclusivamente via Figma MCP** (`use_figma`, `get_design_context`, `get_variable_defs`, `get_metadata`, `FetchMcpResource`, `search_design_system`), seguindo `@agents/ux-ui-specialist.md` e `@designSystem/inventario-design-system.md`.

### Objetivo

No arquivo **Figma Design** destino, construir o Design System em **duas fases estritas**:

1. **FASE 1 — Variables apenas:** criar **todas** as Figma Variables (Primitives + Semantic + Spacing + Typography + …), valores validados contra Make `/ds/00-foundations` + inventário.
2. **FASE 2 — Componentes apenas** (após GATE): criar **todos** os Component Sets `DS/*` espelhando **pixel-a-pixel** as páginas Make `/ds/01`–`/ds/10`, **100% bound às Variables**.

**Não criar telas de produto** (`/inicio`, login completo, wizard, admin). Apenas DS reutilizável.

---

### REGRAS IMUTÁVEIS

1. **Versão A única** — azul `#2563eb`, neutros slate, cards elevados. Ignorar DashboardB/C.
2. **Ignorar** `/inicio` — fonte visual = rotas `/ds/*` apenas.
3. **Gate Fase 1 → 2:** `get_variable_defs` em `cbzU2vijj5r6zIOzi4PKBV` deve retornar ≥95% tokens do § ANEXO A (sessão 00) antes de qualquer componente.
4. **Hierarquia:** Primitives → Semantic → Componentes (zero hex hardcoded).
5. **Naming tokens:** `color/brand/primary`, `space/md`, `radius/lg`. Layers: `DS/Component/Variant/State`.
6. **Auto Layout** 100%. Touch target ≥ 44px.
7. **Light + Dark** modes (mesmos nomes; Dark pode usar oklch do `theme.css` Make).
8. Copy pt-BR. Protocolos: `2026-0042`.
9. **Uma fase por turno** — nunca avançar para a fase seguinte na mesma resposta sem GATE aprovado.
10. **Checkpoint obrigatório** — ao terminar cada fase, emitir o template § CHECKPOINT e **parar** (aguardar confirmação do operador ou nova mensagem explícita).

---

### PROTOCOLO DE EXECUÇÃO SEQUENCIAL

**Regra de ouro:** execute **somente a fase indicada** na mensagem atual. Se o operador não especificar fase, comece pela **primeira incompleta** (0 → 1 → GATE → 2).

| Regra | Comportamento |
|-------|---------------|
| **1 fase / turno** | Cada resposta cobre **no máximo uma fase** (0, 1 ou 2.x). Não misturar Variables + Componentes no mesmo turno. |
| **GATE bloqueante** | Se `get_variable_defs` < 95% ANEXO A → **corrigir Fase 1** e **não** iniciar Fase 2. |
| **Fase 2 incremental** | Cada turno de Fase 2 cobre **exatamente 1 sessão** ANEXO B (ex.: só B.03). Não pular sessões. |
| **Parar ao concluir** | Após checkpoint, **encerrar a resposta**. Não continuar espontaneamente. |
| **Falha = retry mesma fase** | Erro em `use_figma` → corrigir e repetir **a mesma fase/passo**, sem avançar. |

**Mensagens do operador (opcionais):**

- `Execute Fase 0` · `Execute Fase 1` · `Execute GATE` · `Execute Fase 2 — sessão 03` · `Execute validação final`

Se nenhuma fase for especificada, executar **Fase 0** e parar no checkpoint.

---

### ARQUIVOS FIGMA

| Papel | URL | Identificadores |
|-------|-----|-----------------|
| **Fonte (Make)** — ler `/ds/*` | `https://www.figma.com/make/F6XouRSZfqjnW84IKkTUaa/prototipo_inicial` | `makeFileKey`: `F6XouRSZfqjnW84IKkTUaa` |
| **Destino (Design)** — escrever | `https://www.figma.com/design/cbzU2vijj5r6zIOzi4PKBV/designSystem?node-id=0-1` | `fileKey`: `cbzU2vijj5r6zIOzi4PKBV` · raiz `0:1` |

**Estado atual do destino (verificado):** arquivo contém apenas `Page 1` — você **deve criar/renomear** as 11 páginas listadas abaixo.

---

### FASE 0 — Setup + extração obrigatória

#### 0.1 Setup Design

1. `get_metadata` em `cbzU2vijj5r6zIOzi4PKBV` (sem nodeId).
2. Criar/renomear páginas Figma Design (nomes exatos):

| # | Página Figma Design |
|---|---------------------|
| 00 | `00 — Foundations` |
| 01 | `01 — Brand` |
| 02 | `02 — Shells` |
| 03 | `03 — Ações & Superfícies` |
| 04 | `04 — Status & Feedback` |
| 05 | `05 — Formulários` |
| 06 | `06 — Navegação & Overlays` |
| 07 | `07 — Dados & Listas` |
| 08 | `08 — Domínio Acadêmico` |
| 09 | `09 — Estados Globais` |
| 10 | `10 — Icons` |

3. Carregar skill **`figma-use`** antes de **todo** `use_figma`.

#### 0.2 Protocolo de leitura Make (OBRIGATÓRIO antes da Fase 1)

Para **cada** rota, executar:

```
get_design_context(makeFileKey=F6XouRSZfqjnW84IKkTUaa, nodeId=0:1)
FetchMcpResource → src/app/components/DSLibrary/pages/PageXX*.tsx
```

| Rota Make | Arquivo fonte | Uso |
|-----------|---------------|-----|
| `/ds/00-foundations` | `Page00Foundations.tsx` | **Master tokens** — hex + specs |
| `/ds/01-brand` | `Page01Brand.tsx` | Brand assets + confirma tokens |
| `/ds/02-shells` | `Page02Shells.tsx` | Layout tokens + wireframes shell |
| `/ds/03-acoes-superficies` | `Page03AcoesSup.tsx` | Specs componentes ações/superfícies |
| `/ds/04-status-feedback` | `Page04StatusFeedback.tsx` | Badges, alerts, skeleton, avatar |
| `/ds/05-formularios` | `Page05Formularios.tsx` | Inputs, selects, form states |
| `/ds/06-navegacao-overlays` | `Page06NavOverlays.tsx` | Nav, tabs, dialog, sheet, etc. |
| `/ds/07-dados-listas` | `Page07DadosListas.tsx` | Tables, timeline, quicktile, filter |
| `/ds/08-dominio-academico` | `Page08DominioAcad.tsx` | Badges domínio, presença, certificados |
| `/ds/09-estados-globais` | `Page09EstadosGlobais.tsx` | Matriz loading/empty/error |
| `/ds/10-icons` | `Page10Icons.tsx` | 48 ícones Lucide + tamanhos |

Também ler `src/app/components/DS/*.tsx` e `Shell/*.tsx` para medidas implementadas.

**Micro-passos 0.2 (executar em ordem, reportar ao final):**

| Passo | Ação | Critério de done |
|-------|------|------------------|
| 0.2.1 | `FetchMcpResource` Page00–Page04 | 5 arquivos lidos |
| 0.2.2 | `FetchMcpResource` Page05–Page09 | 5 arquivos lidos |
| 0.2.3 | `FetchMcpResource` Page10 + `DS/*.tsx` + `Shell/*.tsx` | leitura completa |
| 0.2.4 | Cruzar ANEXO A/B vs conteúdo lido | listar gaps (se houver) |

**Não usar:** `DashboardB.tsx`, `DashboardC.tsx`.

**CHECKPOINT Fase 0** — emitir e **parar**:

```
✅ CHECKPOINT — Fase 0 concluída
- Páginas Design: [11/11 nomes listados]
- Make lidos: [11/11 Page*.tsx]
- Gaps ANEXO A/B: [nenhum | lista]
- Próximo passo: Fase 1 (aguardando confirmação)
```

---

## ANEXO A — Tokens extraídos (Make Page00 + validação sessões 01–10)

> Valores **canônicos** para Fase 1. Status/domínio confirmados em Page08.

### A.1 Brand & ação

| Token | Hex |
|-------|-----|
| `color/brand/primary` | `#2563eb` |
| `color/brand/primary-hover` | `#1d4ed8` |
| `color/brand/primary-pressed` | `#1e40af` |
| `color/brand/subtle` | `#eff6ff` |
| `color/brand/accent` | `#475569` |
| `color/action/link` | `#2563eb` |
| `color/action/link-hover` | `#1d4ed8` |
| `color/action/destructive` | `#dc2626` |

### A.2 Superfícies

| Token | Hex |
|-------|-----|
| `color/surface/default` | `#f8fafc` |
| `color/surface/elevated` | `#ffffff` |
| `color/surface/overlay` | `rgba(0,0,0,0.4)` |
| `color/surface/subtle` | `#f1f5f9` |
| `color/surface/inverse` | `#1e293b` |
| `color/surface/auth` | `#f8fafc` |
| `color/surface/code` | `#0f172a` |

### A.3 Texto

| Token | Hex |
|-------|-----|
| `color/text/primary` | `#0f172a` |
| `color/text/secondary` | `#475569` |
| `color/text/muted` | `#64748b` |
| `color/text/disabled` | `#cbd5e1` |
| `color/text/inverse` | `#ffffff` |
| `color/text/link` | `#2563eb` |
| `color/text/on-brand` | `#ffffff` |

### A.4 Bordas

| Token | Hex |
|-------|-----|
| `color/border/default` | `#e2e8f0` |
| `color/border/strong` | `#94a3b8` |
| `color/border/subtle` | `#f1f5f9` |
| `color/border/focus` | `#2563eb` |
| `color/border/error` | `#ef4444` |

### A.5 Status (bg / border / text / icon)

| Status | bg | border | text | icon |
|--------|-----|--------|------|------|
| success | `#f0fdf4` | `#a7f3d0` | `#047857` | `#059669` |
| warning | `#fffbeb` | `#fde68a` | `#92400e` | `#d97706` |
| danger | `#fef2f2` | `#fecaca` | `#b91c1c` | `#dc2626` |
| info | `#eff6ff` | `#bfdbfe` | `#1d4ed8` | `#2563eb` |
| neutral | `#f1f5f9` | `#e2e8f0` | `#334155` | `#64748b` |

### A.6 Domínio acadêmico

**SLA:** on-time `#10b981` · at-risk `#f59e0b` · overdue `#dc2626`

**Presença:** pending `#fef3c7` · partial `#bfdbfe` · complete `#a7f3d0` · ineligible `#e2e8f0`

**Evento:** scheduled `#e2e8f0` · in-progress `#3b82f6` · completed `#10b981`

**Solicitação:** draft `#e2e8f0` · submitted `#bfdbfe` · in-review `#3b82f6` · adjustments `#fbbf24` · approved `#10b981` · rejected `#ef4444`

**EventStatusBadge (Page08):** Agendado `slate-100/700` · Em andamento `blue-100/700` · Concluído `emerald-100/700`

**PresenceBadge:** Pendente `amber-100/800` · Parcial `blue-100/700` · Completa `emerald-100/700` · Inelegível `slate-100/500`

### A.7 Tipografia

| Token | Size | Weight | LH |
|-------|-----:|-------:|---:|
| `Heading/H1` | 32 | 700 | 1.25 |
| `Heading/H2` | 24 | 600 | 1.3 |
| `Heading/H3` | 20 | 600 | 1.4 |
| `Heading/H4` | 18 | 600 | 1.4 |
| `Body/Default` | 16 | 400 | 1.5 |
| `Body/Semibold` | 16 | 600 | 1.5 |
| `Body/Small` | 14 | 400 | 1.5 |
| `Caption/Default` | 12 | 400 | 1.4 |
| `Caption/Muted` | 12 | 400 muted | 1.4 |
| `Label/Default` | 14 | 500 | 1.4 |
| `Code/Inline` | 14 mono | 400 | 1.4 |

`font/family/sans` = Inter · `font/family/mono` = ui-monospace

### A.8 Spacing & layout

`space/xs` 4 · `sm` 8 · `md` 16 · `lg` 24 · `xl` 32 · `2xl` 40 · `3xl` 48 · `4xl` 64

`layout/sidebar-width` 256 · `sidebar-collapsed` 72 · `topbar-height` 64 · `page-padding-desktop` 32 · `page-padding-mobile` 16 · `card-padding` 24 · `card-gap` 24 · `section-gap` 40 · `form-gap` 16 · `touch-target-min` 44 · `content-max-width` 480 · `content-max-width-wide` 720

### A.9 Radius, border, shadow, motion, icon, z-index

**Radius:** none 0 · sm 4 · md 8 · lg 12 · xl 16 · full 9999

**Border width:** default 1 · strong 2 · focus 2

**Shadow:** none · sm (cards) · md (dropdown) · lg (modal) — espelhar Tailwind shadow-sm/md/lg

**Motion** (nomes exatos Page00): `duration/fast` 150ms · `duration/normal` 200ms · `duration/slow` 300ms · `duration/pulse` 1500ms · `easing/default` ease-in-out · `easing/enter` ease-out · `easing/exit` ease-in

**Icon** (Page10): `icon/size/xs` 16 · `icon/size/sm` 20 · `icon/size/md` 24 · `icon/size/lg` 32 · stroke-width 2

**Z-index:** base · sticky · dropdown · sidebar-overlay · modal · toast · tooltip

### A.10 Tokens adicionais detectados nas sessões 01–10 (incluir na collection Color)

| Token | Origem | Valor ref. |
|-------|--------|------------|
| `color/notification/dot` | Page02 Topbar | `#ef4444` |
| `color/nav/active-bg` | Page02/06 NavItem | `#eff6ff` (brand subtle) |
| `color/nav/active-text` | NavItem active | `#1d4ed8` |
| `color/highlight-card-bg` | Page03 HighlightCard | `#eff6ff` @ 50% |
| `color/highlight-card-border` | Page03 | `#bfdbfe` |
| `color/tooltip/bg` | Page06 | `#1e293b` |
| `color/certificate/gradient-from` | Page08 | `#2563eb` |
| `color/certificate/gradient-to` | Page08 | `#1e40af` |
| `color/pulse/attention` | Page08 Acknowledgment | `#f59e0b` |

---

## ANEXO B — Componentes por sessão Make (Fase 2 — specs pixel-a-pixel)

> Cada item = **1 Component Set** no Figma Design, página indicada, **bound às Variables** do ANEXO A.

### B.01 — `01 — Brand` (Page01Brand)

| Component Set | Variantes / specs |
|---------------|-------------------|
| `Brand/Logo/Full` | sm/md · logo mark + "SecretariaOnline" + "Portal Acadêmico" |
| `Brand/Logo/Mark-Only` | sm 32 · md 48 · lg 64 · `rounded-xl` · bg brand primary |
| `Brand/Logo/Inverse` | sobre `color/brand/primary` · texto inverse |
| `Brand/Wordmark` | 24px/700 sans |
| `Brand/Favicon` | 16, 32, 48, 64px |
| `Illustration/Empty/*` | 6 contextos: pendência, solicitação, evento, certificado, comunicação, resultado |
| `Illustration/Error/*` | 404, 403, 500, offline |
| `Pattern/AuthBackground` | gradient blue-50 → slate-50 → slate-100 |

### B.02 — `02 — Shells` (Page02Shells)

| Component Set | Medidas |
|---------------|---------|
| `Shell/AppLayout` | Sidebar w256 · Topbar h64 · Main bg surface/default · padding 32 |
| `Shell/Sidebar` | NavItem h44 · Brand h16 area · border-r |
| `Shell/Topbar` | Search max-w480 h40 · Bell + Avatar + UserMenu |
| `Shell/AuthLayout` | Card max-w480 · gradient auth bg · logo + form |
| `Shell/PublicLayout` | Header + main + footer links |
| `Shell/MobileShell` | Header h48 · TabBar h56 · 5 tabs |
| `Main/PageHeader` | título + descrição + ações |
| `Main/AlertStrip` | max 2 banners |
| `Main/KpiRow` | 4 col gap 24 |
| `Main/MainGrid` | ratio 2:1 gap 24 |
| `Main/FilterBar` | sticky |
| `Main/Toolbar` | bulk + export |

### B.03 — `03 — Ações & Superfícies` (Page03AcoesSup)

| Component Set | Variantes / medidas |
|---------------|---------------------|
| `DS/Button` | primary, secondary, ghost, link, danger · h40 / sm h32 · px16/12 · rounded-md · focus ring 2px |
| `DS/Button` estados | default, disabled (opacity 50), loading (spinner) |
| `DS/Button` ícones | leading, trailing, icon-only |
| `DS/IconButton` | ghost 40×40 · outline · danger outline · disabled |
| `DS/Link` | inline underline · standalone + chevron |
| `DS/Card` | default p24 · border + shadow-sm |
| `DS/Card` interactive | hover shadow + border emphasis |
| `DS/KpiCard` | min-h120 · number-only · with-progress (bar h1.5) · with-chip · with-helper |
| `DS/HighlightCard` | p20 gap12 · bg highlight · border highlight |
| `DS/Separator` | horizontal hr · vertical w-px |
| `DS/Panel` | border only p16 · sem shadow [P1] |

### B.04 — `04 — Status & Feedback` (Page04StatusFeedback)

| Component Set | Specs |
|---------------|-------|
| `DS/Badge` | neutral, success, warning, danger, info · h24 px8 rounded-full |
| `DS/AlertBanner` | info, warning, success, error · dismissible · title + optional description + action |
| `DS/Skeleton` | line · block · circle · composições KPI + lista |
| `DS/EmptyState` | generic · with action · with icon |
| `DS/Spinner` | sm · md · inline com label |
| `DS/Progress` | linear 0–100% · label + valor (ex. 72/120h) |
| `DS/InlineAlert` | error abaixo input · ícone + texto xs |
| `DS/Avatar` | sm 32 · md 36 · lg 48 · foto + initials |

### B.05 — `05 — Formulários` (Page05Formularios)

| Component Set | Specs |
|---------------|-------|
| `DS/Input` | h40 · label sm medium · helper · error state · disabled |
| `DS/Input/Search` | h40 · pl36 icon · bg slate-50 → white on focus |
| `DS/Input/password` | toggle eye icon trailing |
| `DS/Textarea` | min 4 rows · resize-y · counter 0/500 |
| `DS/Select` | h40 · chevron native [P1] |
| `DS/Checkbox` | 16×16 · checked/disabled/indeterminate [P1] |
| `DS/RadioGroup` | 16×16 · vertical list [P1] |
| `DS/Switch` | w44 h24 · thumb 20px · focus ring [P1] |
| `DS/FormField` | label + input + error wrapper |
| `DS/FormSection` | grid 2 col + FormActions (Cancel + Submit) |

### B.06 — `06 — Navegação & Overlays` (Page06NavOverlays)

| Component Set | Specs |
|---------------|-------|
| `DS/NavItem` | h44 px12 gap12 · icon 20 · active bg nav · badge count |
| `DS/Tabs` | underline · border-b2 active blue [P1] |
| `DS/Breadcrumb` | chevron separator 14 · truncate middle [P1] |
| `DS/Pagination` | btn h36 w36 · prev/next · current filled primary [P1] |
| `DS/WizardStepper` | 3 steps · circle 32 · check done · line connector [P1] |
| `DS/Dialog` | max-w-md · overlay scrim · rounded-xl · shadow-lg [P1] |
| `DS/Sheet` | w320 right · full height · overlay [P1] |
| `DS/DropdownMenu` | w192 · shadow-md · item h36 · danger item red [P1] |
| `DS/Tooltip` | bg tooltip · text xs white · arrow [P1] |
| `DS/CommandPalette` | frame estático Ctrl+K [P2] |

### B.07 — `07 — Dados & Listas` (Page07DadosListas)

| Component Set | Specs |
|---------------|-------|
| `DS/DataTable/Compact` | row h48 · cols: Número, Tipo, Estado, Prazo, SLA · hover row |
| `DS/DataTable/Full` | + checkbox col · sort ↕ · toolbar filter + export CSV [P1] |
| `DS/FilterBar` | chips rounded-full · select pill · date pill · limpar [P1] |
| `DS/ListItem` | icon circle 40 · title truncate · meta · ghost CTA |
| `DS/Timeline` | dot 12 · connector 1px · date mono xs |
| `DS/QuickTile` | min 88×88 · p12 · icon 24 · label 11px 2 lines · grid 2×3 |
| `DS/FilterBar` | chips + selects + date + limpar (ver acima) |

> **Nota Make Page07:** `PendenciaItem`, `EventoRow`, `DeadlineRow` existem no inventário/DashboardA mas **não** têm tile dedicado na Page07 — derivar de `DS/ListItem` + DashboardA se necessário [P1].

### B.08 — `08 — Domínio Acadêmico` (Page08DominioAcad)

| Component Set | Specs |
|---------------|-------|
| `DS/EventStatusBadge` | Agendado · Em andamento · Concluído |
| `DS/PresenceBadge` | Pendente · Parcial · Completa · Inelegível |
| `DS/SlaIndicator` | dot 8 + label · on-time/at-risk/overdue |
| `DS/ProtocolBadge` | mono sm · shield icon 12 · `2026-NNNN` |
| `DS/PresenceValidator` | PIN 4× input 48×48 · icon key circle 48 |
| `DS/CountdownTimer` | mono 3xl amber · bar progress |
| `DS/QRCodeDisplay` | 128×128 placeholder · protocol label |
| `DS/CertificateCard` | gradient header · PDF download btn |
| `DS/HashDisplay` | bg code surface · mono xs · copy btn |
| `DS/AcknowledgmentBadge` | pulse button amber · state "Ciente" emerald |
| `DS/PasswordStrengthMeter` | 4 bars + checklist regras |
| `DS/SessionList` | row ~72px · device + IP · badge Atual · Encerrar danger ghost |

> **Nota Make Page08:** `NotificationItem`, `InboxItem`, `ParecerCard`, `VerificationResult` estão no inventário mas **não** na Page08 Make — criar só se inventário exigir [P2].

### B.09 — `09 — Estados Globais` (Page09EstadosGlobais)

Frame **`States/Matrix`** na página 09 — grid **3 colunas** (Loading | Empty | Error) × **6 blocos**:

1. KPI Row  
2. Lista Pendências  
3. Tabela Solicitações  
4. Formulário (loading submitting | pristine | validation error)  
5. ErrorPage 404 (full width)  
6. Eventos/Calendário  

Usar `DS/Skeleton`, `DS/EmptyState`, `DS/AlertBanner` já criados na Fase 2 (páginas 03–04).

### B.10 — `10 — Icons` (Page10Icons)

- Grid **48 ícones** Lucide @ 24px + rótulo mono
- 4 tamanhos demo: 16/20/24/32 → tokens `icon/size/xs|sm|md|lg`
- stroke-width 2 · currentColor · aria-hidden em decorativos

**Lista canônica (48):** home, file-text, calendar, bell, search, user, settings, log-out, check-circle, alert-triangle, x-circle, info, clock, download, upload, qr-code, key, mail, chevron-right, plus, filter, more-horizontal, external-link, shield, book-open, briefcase, graduation-cap, award, message-square, headset, life-buoy, chevron-down, menu, x, check, alert-circle, arrow-right, file-plus-2, refresh-cw, trash-2, edit, eye, eye-off, lock, unlock, copy, share-2, star

**Grupos semânticos:** Navegação · Ações · Status · Domínio acadêmico (ver Page10 seção “Mapeamento semântico”)

---

### FASE 1 — VARIABLES (implementação)

Usar `use_figma` Plugin API: `createVariableCollection`, `createVariable`, modes Light/Dark, aliases Primitives → Semantic.

Preencher com valores do **ANEXO A** (não inventar hex). Executar **micro-passos em ordem** — validar cada passo antes do próximo.

| Passo | Collection / ação | Conteúdo ANEXO A |
|-------|-------------------|------------------|
| **1.1** | Criar modes Light + Dark em todas as collections | — |
| **1.2** | `Primitives` | hex base (brand, slate, status, domínio) |
| **1.3** | `Color` | aliases semânticos A.1–A.6 + A.10 → Primitives |
| **1.4** | `Spacing` | `space/*` + `layout/*` (A.8) |
| **1.5** | `Radius` | A.9 radius |
| **1.6** | `Border` | A.9 border width |
| **1.7** | `Shadow` | A.9 shadow |
| **1.8** | `Typography` | A.7 + `font/family/*` |
| **1.9** | `Motion` | `duration/*` + `easing/*` (A.9) |
| **1.10** | `Icon` + `Z-index` | A.9 icon sizes + z-index |
| **1.11** | Frame `Foundations/Swatches` na pág `00 — Foundations` | swatches **bound** (espelhar Page00) |
| **1.12** | **GATE** — `get_variable_defs` | ≥ 95% tokens ANEXO A |

**Após cada passo 1.2–1.10:** retornar contagem de variables criadas na collection. Se erro → corrigir **no mesmo passo**, não avançar.

**GATE FASE 1 (passo 1.12 — bloqueante):**

```
get_variable_defs(fileKey=cbzU2vijj5r6zIOzi4PKBV, nodeId=<qualquer frame da pág 00>)
```

- **PASS:** ≥ 95% ANEXO A → emitir CHECKPOINT Fase 1 e **parar** (não iniciar Fase 2 no mesmo turno).
- **FAIL:** listar tokens faltantes → voltar ao passo da collection afetada → repetir GATE.

**CHECKPOINT Fase 1** — emitir e **parar**:

```
✅ CHECKPOINT — Fase 1 concluída
- Collections: [10/10]
- Variables totais: [N]
- GATE get_variable_defs: PASS ([X]% ANEXO A)
- Swatches pág 00: [sim/não]
- Próximo passo: Fase 2 — sessão 01 Brand (aguardando confirmação)
```

---

### FASE 2 — COMPONENTES (implementação)

**Escopo por turno:** exatamente **1 sessão** ANEXO B (ex.: turno = só B.03). Nunca duas sessões no mesmo turno.

Ordem de construção (dependências — **não pular nem reordenar**):

| Turno | Sessão ANEXO B | Página Figma Design |
|-------|----------------|---------------------|
| 2.1 | B.01 Brand | `01 — Brand` |
| 2.2 | B.03 Ações & Superfícies | `03 — Ações & Superfícies` |
| 2.3 | B.04 Status & Feedback | `04 — Status & Feedback` |
| 2.4 | B.05 Formulários | `05 — Formulários` |
| 2.5 | B.06 Nav & Overlays | `06 — Navegação & Overlays` |
| 2.6 | B.07 Dados & Listas | `07 — Dados & Listas` |
| 2.7 | B.08 Domínio Acadêmico | `08 — Domínio Acadêmico` |
| 2.8 | B.02 Shells | `02 — Shells` |
| 2.9 | B.09 Estados Globais | `09 — Estados Globais` |
| 2.10 | B.10 Icons | `10 — Icons` |

Para **cada** Component Set da sessão atual:

1. Ler Make Page correspondente (`FetchMcpResource`).
2. `use_figma` criar Component Set com variantes + estados.
3. Bind **100%** fills/strokes/text/gaps/padding/radius às Variables.
4. Auto Layout + naming `DS/Component/Variant/State`.
5. `get_screenshot` amostra para validação visual.

**Proibido:** hex literal · criar componente antes do GATE Fase 1 · pular variantes · executar 2 sessões no mesmo turno.

**CHECKPOINT por sessão Fase 2** — emitir e **parar**:

```
✅ CHECKPOINT — Fase 2 sessão [B.0X] concluída
- Component Sets criados: [N] — [lista nomes]
- Screenshots: [ok/pendente]
- Variantes faltantes vs ANEXO B: [nenhuma | lista]
- Próximo passo: Fase 2 sessão [B.0Y] (aguardando confirmação)
```

---

### ORDEM DE EXECUÇÃO (1 fase ou 1 sessão por turno)

```
TURNO 1  → Fase 0.1 Setup páginas Design
         → Fase 0.2 Leitura Make (0.2.1 → 0.2.4)
         → CHECKPOINT Fase 0 → PARAR

TURNO 2  → Fase 1 passos 1.1 → 1.12 (Variables + swatches + GATE)
         → CHECKPOINT Fase 1 → PARAR  ⚠️ só avançar se GATE = PASS

TURNO 3  → Fase 2.1  B.01 Brand           → CHECKPOINT → PARAR
TURNO 4  → Fase 2.2  B.03 Ações           → CHECKPOINT → PARAR
TURNO 5  → Fase 2.3  B.04 Status          → CHECKPOINT → PARAR
TURNO 6  → Fase 2.4  B.05 Forms           → CHECKPOINT → PARAR
TURNO 7  → Fase 2.5  B.06 Nav             → CHECKPOINT → PARAR
TURNO 8  → Fase 2.6  B.07 Dados           → CHECKPOINT → PARAR
TURNO 9  → Fase 2.7  B.08 Domínio         → CHECKPOINT → PARAR
TURNO 10 → Fase 2.8  B.02 Shells          → CHECKPOINT → PARAR
TURNO 11 → Fase 2.9  B.09 Estados         → CHECKPOINT → PARAR
TURNO 12 → Fase 2.10 B.10 Icons           → CHECKPOINT → PARAR

TURNO 13 → Validação final (get_variable_defs + contagem Component Sets + screenshots)
         → CHECKPOINT entrega final → PARAR
```

**Nunca** executar TURNO N+1 na mesma resposta que TURNO N.

---

### PÓS-ENTREGA (TURNO 13 — validação final)

1. URL: `https://www.figma.com/design/cbzU2vijj5r6zIOzi4PKBV/designSystem`
2. Tabela: collections · # variables · # component sets por página
3. Gaps vs `@designSystem/inventario-design-system.md`
4. Próximo: export → `frontend-web/src/shared/tokens/tokens.css` + Code Connect

**CHECKPOINT entrega final** — emitir e **parar**:

```
✅ CHECKPOINT — Entrega final
- Variables GATE: [PASS/FAIL] ([X]%)
- Component Sets: [N total] por página [tabela]
- Critérios de aceite: [N/10 ✓]
- Gaps inventário: [lista | nenhum]
- URL: https://www.figma.com/design/cbzU2vijj5r6zIOzi4PKBV/designSystem
```

---

### CRITÉRIOS DE ACEITE

- [ ] 11 páginas Figma Design criadas (nomes exatos)
- [ ] Variables ANEXO A completas (Light + Dark)
- [ ] Swatches pág 00 bound (não hex solto)
- [ ] GATE Fase 1 passou antes de Fase 2
- [ ] Todos Component Sets ANEXO B existem
- [ ] Page 09 matriz 3×6 estados
- [ ] Page 10 grid 48 ícones
- [ ] Zero hex hardcoded em componentes
- [ ] Visual = Make `/ds/*` (azul #2563eb, slate)
- [ ] Sem Dashboard B/C
- [ ] Cada fase/sessão encerrada com CHECKPOINT (sem avanço automático)

Ao concluir cada fase ou sessão, emitir o **CHECKPOINT** correspondente (§ Fase 0, 1 ou 2) e **encerrar a resposta** — não continuar para a fase seguinte sem confirmação explícita do operador.

---

## Notas para o operador (não colar no agente)

| Item | Valor |
|------|-------|
| Make fileKey | `F6XouRSZfqjnW84IKkTUaa` |
| Design fileKey | `cbzU2vijj5r6zIOzi4PKBV` |
| Destino atual | Apenas `Page 1` — agente cria 11 páginas |
| Extração v2.1 | **11/11** páginas Make lidas e mapeadas nos ANEXOS A/B |
| Inventário | `designSystem/inventario-design-system.md` |
| Estimativa | **13 turnos** (0 + 1 + 10 sessões Fase 2 + validação) · 1 fase/sessão por turno |
| Reforço v2.2 | GATE bloqueante · checkpoints · micro-passos Fase 1 (1.1–1.12) |

### Auditoria de extração (2026-06-08 — revalidada via MCP)

Extração executada: `get_design_context` + `FetchMcpResource` em **11/11** arquivos `Page*.tsx`.

| Sessão | Arquivo Make | Match ANEXO | Gaps detectados |
|--------|--------------|-------------|-----------------|
| 00 | Page00Foundations | ✅ 100% tokens hex | motion = `duration/*` não `motion/*` |
| 01 | Page01Brand | ✅ 8 blocos | — |
| 02 | Page02Shells | ✅ 4 shells + Main/* | wireframe sidebar 200px (canônico 256) |
| 03 | Page03AcoesSup | ✅ 8 componentes | Panel = P1 |
| 04 | Page04StatusFeedback | ✅ 8 componentes | — |
| 05 | Page05Formularios | ✅ 8 blocos | Select/Checkbox/Radio/Switch = P1 |
| 06 | Page06NavOverlays | ✅ 9 componentes | CommandPalette ausente no Make |
| 07 | Page07DadosListas | ✅ 6 blocos | PendenciaItem/EventoRow só no inventário |
| 08 | Page08DominioAcad | ✅ 12 blocos | Notification/Inbox/Parecer só inventário |
| 09 | Page09EstadosGlobais | ✅ 6 blocos | bloco 5 = 404 full-width (não 3 col) |
| 10 | Page10Icons | ✅ 48 ícones | sem tamanho 48px no Make |

**Destino Figma Design (`cbzU2vijj5r6zIOzi4PKBV`):** apenas `Page 1` · zero Variables · zero Component Sets — agente deve popular do zero seguindo este prompt.

**GATE esperado pós-Fase 1:** `get_variable_defs(nodeId=0:1)` retorna mapa de tokens ANEXO A (atualmente falha — arquivo vazio).
