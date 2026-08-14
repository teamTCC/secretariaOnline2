# Prompt Figma Make — Biblioteca visual Design System SecretariaOnline2

> **Uso:** colar este prompt no chat do Figma Make **depois** que o dashboard `/inicio` Versão A já existir no mesmo projeto/chat.  
> **Anexos recomendados:** `designSystem/inventario-design-system.md` (v2.0) — inventário completo de tokens e componentes.  
> **Referência de produto:** `foundationDocs/analysis/telas.md` (~48 rotas, fluxos F0–F8).

---

## Prompt (copiar abaixo)

Construa a **biblioteca visual completa do Design System** do **SecretariaOnline2** — portal de secretaria acadêmica universitária (solicitações, formativas, eventos, certificados, comunicação).

---

### REGRA IMUTÁVEL — Versão A única

O dashboard **`/inicio — Versão A`** (ou `DashboardA`) **já criado neste chat** é a **única fonte de verdade visual e estrutural**.

1. **Extrair** deste dashboard todas as decisões visuais: paleta, tipografia, raios, sombras, bordas, ícones, densidade, tratamento de cards/sidebar/topbar.
2. **Replicar** exatamente esse idioma visual em todos os componentes e swatches desta biblioteca.
3. **Ignorar e excluir** completamente Versão B, Versão C, frames `-B`/`-C` e qualquer **botão toggle de versão** no layout.
4. **Não inventar** uma segunda identidade visual — coerência total com Versão A.

Se houver conflito entre este prompt e o dashboard Versão A, **priorizar Versão A** para cores/tipografia/superfícies; **priorizar este prompt** para nomenclatura de tokens, cobertura de componentes e organização de páginas.

---

### Contexto do produto

- **Público:** estudantes, professores, secretaria, comissões, admin.
- **Tom visual:** institucional, confiável, claro, acolhedor — **não** austero cinza-monocromático, **não** startup neon/agressivo.
- **Stack alvo:** Figma Variables → `tokens.css` → Tailwind → shadcn/ui → `DS/*` React.
- **Ícones:** Lucide (stroke 2) — mesmo estilo já usado no dashboard A.
- **Idioma UI:** pt-BR nos exemplos de copy.
- **A11y:** contraste ≥ 4.5:1 texto normal; estados distinguíveis por **cor + ícone/texto**; focus ring visível.

---

### ENTREGÁVEL — Estrutura de páginas no arquivo

Criar **uma página por seção** (nomenclatura exata):

| Página | Conteúdo |
|--------|----------|
| `00 — Foundations` | Swatches + tipografia + spacing + radius + shadow + motion (com **rótulos de token**) |
| `01 — Brand` | Logo, wordmark, favicon preview, ilustrações Empty/Error base |
| `02 — Shells` | PublicLayout, AuthLayout, AppLayout (extraído de A), MobileShell esquemático |
| `03 — DS Ações & Superfícies` | Button, IconButton, Link, Card, KpiCard, Separator, Panel |
| `04 — DS Status & Feedback` | Badge, AlertBanner, Skeleton, EmptyState, Spinner, Progress, Toast |
| `05 — DS Formulários` | Input, Select, Checkbox, Radio, Switch, Textarea, FileUpload, FormField |
| `06 — DS Navegação & Overlays` | NavItem, Tabs, Breadcrumb, Pagination, Dialog, Sheet, Dropdown, Tooltip |
| `07 — DS Dados & Listas` | DataTable, FilterBar, ListItem, Timeline, QuickTile, Pagination |
| `08 — DS Domínio acadêmico` | Badges SLA/presença/solicitação, PendenciaItem, EventoRow, PresenceValidator, etc. |
| `09 — DS Estados globais` | Matriz loading / empty / error por tipo de bloco |
| `10 — Icons` | Grid Lucide nos tamanhos 16/20/24/32 |

**Frame base da biblioteca:** largura `1200px`, fundo `surface/default`, padding `32`, Auto Layout vertical gap `48` entre seções.

**Naming layers:** prefixo `DS/` para componentes, `Token/` para swatches, `Shell/` para layouts.

---

### 1. FOUNDATIONS — Tokens visuais (página `00`)

Extrair valores **do dashboard Versão A** e exibir cada amostra com **rótulo de token** (texto legível ao lado — estes nomes serão Variables no Figma Design depois).

#### 1.1 Cores — Brand & ação

Swatches rotulados:

- `color/brand/primary`, `primary-hover`, `primary-pressed`, `accent`, `subtle`
- `color/action/link`, `link-hover`, `destructive`

#### 1.2 Cores — Superfícies

- `color/surface/default`, `elevated`, `overlay`, `subtle`, `inverse` (se existir em A), `auth`, `code`

#### 1.3 Cores — Texto

- `color/text/primary`, `secondary`, `muted`, `disabled`, `inverse`, `link`, `on-brand`

#### 1.4 Cores — Bordas

- `color/border/default`, `strong`, `subtle`, `focus`, `error`

#### 1.5 Cores — Status (cada grupo: bg + border + text — 4 swatches)

- `color/status/success/*`, `warning/*`, `danger/*`, `info/*`, `neutral/*`

#### 1.6 Cores — Domínio (amostras visuais)

- SLA: `color/sla/on-time`, `at-risk`, `overdue`
- Presença: `color/presence/pending`, `partial`, `complete`, `ineligible`
- Evento: `color/event/scheduled`, `in-progress`, `completed`
- Solicitação (amostrar 4–6 estados): `draft`, `in-review`, `approved`, `rejected`, etc.

#### 1.7 Tipografia

Tabela visual com amostra + rótulo token + spec (size/weight/line-height):

| Token | Spec ref. |
|-------|-----------|
| `Heading/H1` | 32/700 — saudação |
| `Heading/H2` | 24/600 — KPI |
| `Heading/H3` | 20/600 — título card |
| `Heading/H4` | 18/600 |
| `Body/Default` | 16/400 |
| `Body/Semibold` | 16/600 |
| `Body/Small` | 14/400 |
| `Caption/Default` | 12/400 |
| `Caption/Muted` | 12 muted |
| `Label/Default` | 14/500 |
| `Code/Inline` | mono 14 — protocolo `2026-0042` |

Famílias: `font/family/sans`, `font/family/mono`.

#### 1.8 Espaçamento

Barras visuais rotuladas (extrair do dashboard A; usar escala 8px):

`space/xs` 4 · `space/sm` 8 · `space/md` 16 · `space/lg` 24 · `space/xl` 32 · `space/2xl` 40 · `space/3xl` 48

Layout: `sidebar-width` 256 · `topbar-height` 64 · `card-padding` 24 · `card-gap` 24 · `touch-target-min` 44

#### 1.9 Radius, bordas, sombras

- Radius: `sm` 4 · `md` 8 · `lg` 12 · `full`
- Border: 1px default · 2px strong/focus
- Shadow: `sm` (cards A) · `md` · `lg` — mostrar retângulos comparativos

#### 1.10 Motion (documentação visual)

Chips textuais: `duration/fast` 150ms · `normal` 200ms · `slow` 300ms · easing default · `pulse` para CTA pendente

---

### 2. BRAND (página `01`)

- `Brand/Logo` — full + mark-only (derivar do sidebar A)
- `Brand/Wordmark` — "SecretariaOnline" ou nome institucional genérico
- `Illustration/Empty/Generic` — estilo flat/minimal coerente com A
- `Illustration/Error/404` — uma peça base reutilizável

---

### 3. SHELLS (página `02`)

#### `Shell/AppLayout` — **copiar estrutura visual do dashboard A**

- Sidebar w256, Topbar h64, Main padding 32
- NavItem active state como no A
- **Sem** toggle B/C

#### `Shell/AuthLayout` — `[P0]`

Card central max-w 480, logo top, slot formulário, fundo `surface/auth`

#### `Shell/PublicLayout` — `[P0]`

Header público + main + footer links (Contato, Verificar protocolo)

#### `Shell/MobileShell` — esquemático `[P1]`

Header + scroll + TabBar 5 itens (Início, Solicitações, Comunicação, Buscar, Mais)

---

### 4. COMPONENTES `DS/*` — construir TODOS abaixo

Usar **somente** tokens/superfícies extraídos de Versão A. Cada componente: variantes + estados (default, hover, focus, disabled, loading quando aplicável). Auto Layout 100%.

#### `[P0]` MVP v1 — obrigatório detalhado

| Componente | Variantes | Dimensões |
|------------|-----------|-----------|
| `DS/Button` | primary, secondary, ghost, link, danger | h40 / sm h32; icon leading/trailing/only |
| `DS/IconButton` | ghost, outline | 40×40 min |
| `DS/Link` | inline, standalone | — |
| `DS/Card` | default, interactive | p24; Header/Title/Content/Footer |
| `DS/KpiCard` | with-progress, number-only | min-h120 |
| `DS/HighlightCard` | parecer | p20 gap12 |
| `DS/Separator` | horizontal, vertical | — |
| `DS/Badge` | neutral, success, warning, danger, info | h24 px8 |
| `DS/AlertBanner` | info, warning, success, error | dismissible, full width |
| `DS/Skeleton` | line, block, circle | — |
| `DS/EmptyState` | illustration + msg + CTA | — |
| `DS/Spinner` | sm, md | — |
| `DS/InlineAlert` | error under field | — |
| `DS/Avatar` | sm32, md36, lg48 | foto + initials |
| `DS/Logo` | full, mark | — |
| `DS/Input` | text, password, error state | label + helper + error |
| `DS/Input/Search` | with icon, clear | h40 max-w480 |
| `DS/Label` | required optional | — |
| `DS/FormField` | composto label+input+error | — |
| `DS/NavItem` | default, active | h44 px12 |
| `DS/PendenciaItem` | default | min-h72 |
| `DS/EventoRow` | with-cta, without-cta | — |
| `DS/DataTable/Compact` | header + 3 rows exemplo | row h48 |
| `DS/TimelineItem` | default | conector vertical |
| `DS/QuickTile` | default | min 88×88 |
| Componentes já no dashboard A | reutilizar visual | KpiRow, AlertStrip, PageHeader |

#### `[P1]` MVP v2 — mostrar 1 exemplo de cada na biblioteca

`DS/Textarea` · `DS/Select` · `DS/Combobox` · `DS/Checkbox` · `DS/RadioGroup` · `DS/Switch` · `DS/DatePicker` · `DS/FileUpload` · `DS/AttachmentUpload` · `DS/WizardStepper` (3 passos) · `DS/Tabs` · `DS/Breadcrumb` · `DS/Pagination` · `DS/Dialog` · `DS/AlertDialog` · `DS/Sheet` · `DS/DropdownMenu` · `DS/Tooltip` · `DS/Toast` · `DS/Progress` · `DS/FilterBar` · `DS/DataTable/Full` · `DS/ListItem` · `DS/Timeline` · `DS/ActionBar` · `DS/DeliberationPanel` · `DS/ProtocolBadge` · `DS/RequestStatusBadge` · `DS/SlaIndicator` · `DS/ExpandableCard`

#### `[P1]` Domínio acadêmico — página `08`

| Componente | Notas |
|------------|-------|
| `DS/EventStatusBadge` | Agendado, Em andamento, Concluído |
| `DS/PresenceBadge` | pending, partial, complete, ineligible |
| `DS/PresenceValidator` | layout PIN (campo + submit) — amostra estática |
| `DS/CountdownTimer` | janela presença — amostra |
| `DS/QRCodeDisplay` | placeholder QR estilizado |
| `DS/VerificationResult` | valid / invalid |
| `DS/HashDisplay` | mono + copy |
| `DS/CertificateCard` | download PDF |
| `DS/NotificationItem` | hub feed |
| `DS/InboxItem` | atendimento ciência |
| `DS/AcknowledgmentBadge` + botão pulse "Estou ciente" | F1.20 |
| `DS/SessionList` | perfil segurança |
| `DS/PasswordStrengthMeter` | primeiro acesso |

#### `[P2]` Admin & dados densos — tile cards com mini-preview (1 frame cada)

`DS/MarkdownEditor` · `DS/JsonSchemaEditor` · `DS/WorkflowStateMachineEditor` · `DS/ImportPreviewTable` · `DS/Chart/Bar` · `DS/StatCard` · `DS/BulkActionBar` · `DS/RoleAuthorityMatrix` · `DS/EventOperationPanel` · `DS/LiveAttendeeCounter`

---

### 5. ESTADOS GLOBAIS (página `09`)

Três colunas por bloco: **Loading (Skeleton)** | **Empty (EmptyState)** | **Error (AlertBanner)**

Blocos obrigatórios:

1. KPI row  
2. Lista pendências  
3. Tabela solicitações  
4. Formulário (campo inválido)  
5. Página inteira (ErrorPage 404)

---

### 6. ÍCONES (página `10`)

Grid mínimo 24 ícones usados no dashboard A + domínio:

`home`, `file-text`, `calendar`, `bell`, `search`, `user`, `settings`, `log-out`, `check-circle`, `alert-triangle`, `x-circle`, `info`, `clock`, `download`, `upload`, `qr-code`, `key`, `mail`, `chevron-right`, `plus`, `filter`, `more-horizontal`, `external-link`, `shield`

Tamanhos: 16, 20, 24, 32 px com rótulos `icon/size/*`

---

### 7. Regras de construção

1. **Auto Layout** em 100% dos containers e componentes.
2. **Zero valores soltos** — cada cor/texto/spacing deve mapear a um rótulo `Token/...` ou ser claramente derivado de A.
3. Layers: `DS/ComponentName/Variant/State`.
4. Copy pt-BR realista: protocolos `AAAA-NNNN`, nomes brasileiros, datas `07/06/2026`.
5. Truncate: título lista 1 linha; parecer 2 linhas.
6. Touch target ≥ 44px em botões, NavItem, QuickTile.
7. Focus state: anel 2px `color/border/focus` em inputs e botões (frame separado "Focus").
8. **Não** construir telas completas F0–F8 neste entregável — apenas biblioteca + shells.
9. **Remover** do arquivo qualquer resquício de Versão B/C ou toggle.

---

### 8. Critérios de aceite (checklist)

- [ ] Paleta completa rotulada com nomes semânticos (§1 Foundations)
- [ ] Tipografia 12+ papéis com specs
- [ ] Spacing, radius, shadow visuais
- [ ] AppLayout idêntico ao dashboard Versão A (sem B/C)
- [ ] AuthLayout + PublicLayout esboçados
- [ ] Todos componentes `[P0]` com variantes e estados
- [ ] Amostras `[P1]` formulários, overlays, domínio
- [ ] Tiles `[P2]` admin/dados
- [ ] Matriz estados loading/empty/error
- [ ] Grid ícones Lucide
- [ ] Visual 100% coerente com Versão A — uma única identidade
- [ ] Nenhum toggle ou frame B/C restante

---

### 9. Ao finalizar

Responder com:

1. **Tokens extraídos de A** — resumo: primary hex (ou descritivo), fonte, radius card, shadow card (3–5 linhas).
2. **Contagem** — quantos componentes `DS/*` criados por página.
3. **Gaps** — o que não existia em A e foi inferido coerentemente (ex.: AuthLayout).
4. **Próximo passo sugerido** — migrar swatches rotulados para Figma Design Variables usando estes nomes.

---

## Notas para o time (não colar no Make)

| Artefato | Caminho |
|----------|---------|
| Inventário completo DS v2.0 | `designSystem/inventario-design-system.md` |
| Mapa 48 telas F0–F8 | `foundationDocs/analysis/telas.md` |
| Estrutura dashboard A | `foundationDocs/prompts/PROMPT_figma_make_dashboard_aluno_estrutura.md` |
| Agente UX/UI | `agents/ux-ui-specialist.md` |
| Pipeline tokens → código | Figma Variables → `tokens.css` → `tailwind.config.ts` → `shared/ui/*` |

**Prioridade de implementação código:** `[P0]` → `[P1]` → `[P2]` conforme inventário §14.
