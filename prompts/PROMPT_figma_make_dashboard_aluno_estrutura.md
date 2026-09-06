# Mapa estrutural Figma — `/inicio` Dashboard do Aluno

| Campo | Valor |
|-------|--------|
| Rota | `/inicio` |
| Capability | `dashboard.view_own` |
| API | `GET /bff/dashboard/aluno` |
| Idioma UI | pt-BR |

Estilo visual (cores, tipografia, ícones, sombras, raios): aplicar a partir da referência fornecida separadamente. Este mapa define apenas estrutura, medidas, componentes e conteúdo.

---

## 1. Páginas e frames do arquivo Figma

| Página | Frame(s) | Dimensão |
|--------|----------|----------|
| `01 — Desktop /inicio` | com dados | `1440 × 1024` |
| `02 — Desktop estados` | skeleton; empty pendências; erro parcial | `1440 × 1024` |
| `03 — Mobile /inicio` | com dados | `390 × 844` |
| `04 — Componentes` | biblioteca `DS/*` | — |

Largura útil `Main` (desktop): `1440 − 256 − 64` = `1120` (`paddingMain 32` × 2 já descontados na área scroll interna).

---

## 2. Tokens de layout

Escala base: `8px` (4, 8, 12, 16, 24, 32, 40, 48).

| Token | px | Aplicação |
|-------|---:|-----------|
| `space-xs` | 4 | ícone–texto, badge |
| `space-sm` | 8 | gap compacto |
| `space-md` | 16 | lista, padding interno |
| `space-lg` | 24 | entre cards |
| `space-xl` | 32 | padding página, gap seções |
| `space-2xl` | 40 | margin `PageHeader` |
| `sidebar-width` | 256 | sidebar desktop |
| `sidebar-collapsed` | 72 | breakpoint médio |
| `topbar-height` | 64 | topbar |
| `card-padding` | 24 | interior de card |
| `card-gap` | 24 | entre cards irmãos |

### Grid desktop (`Main`)

| Zona | Layout |
|------|--------|
| `KpiRow` | 4 colunas iguais, gap 24, `min-height` célula 120 |
| `MainGrid` | 2 colunas ratio **2 : 1**, gap 24, `align-items: start` |
| Coluna primária | 3 cards empilhados, gap 24 |
| Coluna secundária | 3 cards empilhados, gap 24 |

### Grid mobile

| Zona | Layout |
|------|--------|
| Sidebar | off-canvas |
| Topbar | largura total |
| `KpiRow` | 2×2 ou carrossel horizontal, gap 12, card ≥ 100 |
| Conteúdo | 1 coluna: AlertStrip → KPIs → Pendências → Eventos → Solicitações → Prazos → Parecer → Atalhos |

---

## 3. Árvore de componentes

```
AppLayout
├── Shell/Sidebar                    width 256 | drawer mobile
│   ├── Brand                        logo 32 + label
│   ├── Nav/Primary                  9 × NavItem
│   └── Nav/Secondary                2 × NavItem
├── Shell/Topbar                     height 64
│   ├── GlobalSearch                 max-width 480, height 40
│   ├── NotificationBell
│   └── UserMenu
└── Main/ScrollArea                  padding 32 | 16 mobile, scroll Y
    ├── PageHeader
    ├── AlertStrip                   max 2 × AlertBanner
    ├── KpiRow                       4 × KpiCard
    └── MainGrid
        ├── Column/Primary           ~66%
        │   ├── Card/Pendencias
        │   ├── Card/EventosPresenca
        │   └── Card/UltimasSolicitacoes
        └── Column/Secondary         ~33%
            ├── Card/PrazosCalendario
            ├── Card/UltimoParecer
            └── Card/AtalhosRapidos
```

---

## 4. App Shell

### 4.1 `AppLayout`

```
Auto Layout horizontal, fill, clip
├── Sidebar
└── Column fill
    ├── Topbar      h 64
    └── Main        fill, scroll vertical
```

### 4.2 `Shell/Sidebar`

| Node | Auto Layout | Medidas |
|------|-------------|---------|
| Container | vertical, space-between | w 256, h 100vh, py 16 |
| `Brand` | horizontal | logo 32, gap 12, pb 24 |
| `Nav/Primary` | vertical | gap 4, flex 1 |
| `Nav/Secondary` | vertical | gap 4, border-top 1px, pt 16 |

**`DS/NavItem`**

| Propriedade | Valor |
|-------------|-------|
| Layout | horizontal, align center |
| Size | h 44, px 12, gap 12 |
| Filhos | Icon 20 + Label + Badge? (ml auto) |
| Variantes | `default`, `active` |

| # | Label | Rota | Nota |
|---|-------|------|------|
| 1 | Início | `/inicio` | `active` |
| 2 | Solicitações | `/solicitacoes` | |
| 3 | Formativas | `/formativas` | |
| 4 | Estágios | `/estagios` | |
| 5 | TCC | `/tccs` | |
| 6 | Eventos | `/eventos` | |
| 7 | Certificados | `/certificados` | |
| 8 | Comunicação | `/comunicacao` | Badge count opcional |
| 9 | Meus atendimentos | `/meus-atendimentos` | |
| 10 | Perfil | `/perfil` | secondary |
| 11 | Suporte | `/suporte` | secondary |

### 4.3 `Shell/Topbar`

| Node | Layout | Medidas |
|------|--------|---------|
| Container | horizontal, space-between | h 64, px 24 |
| `GlobalSearch` | horizontal, icon + field | max-w 480, h 40, px 16 |
| `Actions` | horizontal | gap 16 |

**`NotificationBell`:** icon 24 + badge 8×8 ou pill overlay.

**`UserMenu`:** Avatar 36 + coluna (nome + curso caption) + chevron 16, gap 12.

Placeholder busca: `Buscar protocolo, GRR, evento… (Ctrl+K)`.

### 4.4 `Main/ScrollArea`

- Scroll independente (sidebar e topbar fixas).
- Gap vertical entre filhos: **32** (`PageHeader` → `AlertStrip` → `KpiRow` → `MainGrid`).

---

## 5. `PageHeader`

Auto Layout vertical, gap 8, width fill.

| Row | Layout | Conteúdo |
|-----|--------|----------|
| 1 | horizontal, space-between, align end | Title H1 + grupo CTAs |
| 2 | — | Subtitle caption |

**Row 1**

| Posição | Elemento | Copy exemplo | Papel tipo |
|---------|----------|--------------|------------|
| Esquerda | Title | Olá, Ana Silva | Heading/H1 |
| Direita | Button/ghost | Ver solicitações | h 40 |
| Direita | Button/primary | Nova solicitação | h 40, px 20, gap 12 |

**Row 2:** Subtitle — `Resumo do seu período · TADS · 2026/1` — Caption/Muted.

---

## 6. `AlertStrip`

Auto Layout vertical, gap 12, max **2** instâncias.

### `DS/AlertBanner`

| Parte | Layout | Size |
|-------|--------|------|
| Container | horizontal, center | p 16, gap 12 |
| Icon | — | 20×20 |
| Content | vertical, flex 1 | title 1 linha + description 1–2 linhas |
| Action | Button/link ou small | — |
| Dismiss | hit area | 32×32, icon 20 |

| Variante | Title | Action |
|----------|-------|--------|
| warning | Solicitação 2026-0042 aguarda seu ajuste | Corrigir solicitação |
| info | Janela de presença aberta: Workshop React | Validar presença |

---

## 7. `KpiRow`

Grid 4 col (desktop) / 2×2 (mobile). `min-height` card: **120**.

### `DS/KpiCard`

```
Auto Layout vertical, p 24, gap 8, width fill
├── Row: label caption + icon 20?
├── Value                    → Heading/H2
├── Helper?                  → caption
└── Progress?                → ring 48×48 OU bar h 6
```

| ID | Label | Value | Extra |
|----|-------|-------|-------|
| KPI-1 | Horas formativas | 72h | progresso 72/120 |
| KPI-2 | Solicitações em andamento | 3 | — |
| KPI-3 | Eventos hoje | 2 | chip "1 janela aberta" |
| KPI-4 | Certificados | 1 | "últimos 30 dias" |

---

## 8. `MainGrid` — coluna primária

### 8.1 `Card/Pendencias`

`DS/Card`, p 24.

| Zona | Layout |
|------|--------|
| Header | horizontal, space-between — Title H3 + Link "Ver tudo" |
| Body | vertical, divisores 1px entre itens |

**`DS/PendenciaItem` × 3**

```
horizontal, py 16, gap 16, align center
├── IconContainer 40×40, icon 20
├── Content flex 1, gap 4
│   ├── Title semibold, 1 linha truncate
│   └── Meta caption + Badge?
└── Button/link "Resolver" | chevron 20
```

| Icon | Title | Meta |
|------|-------|------|
| file | Ajuste na solicitação Trancamento | Prazo 12/06/2026 |
| award | Formativa rejeitada — Workshop IA | Ver parecer |
| headset | Atendimento aguardando ciência | Registrado 10/05 |

**Empty:** icon 48 + "Nenhuma pendência no momento", py 48, centralizado.

### 8.2 `Card/EventosPresenca`

Header: `Eventos de hoje` + link `Ver todos`.

**`DS/EventoRow` × 3**

```
vertical, py 12, gap 8
├── Row1: title semibold | Badge estado evento
├── Row2: Badge presença + caption horas formativas
└── Row3?: Button/small "Validar presença"
```

| Evento | Estado | Presença | CTA |
|--------|--------|----------|-----|
| Palestra Carreira | Em andamento | Pendente | sim |
| Workshop Git | Agendado | Pendente | não |
| Mesa redonda TCC | Concluído | Completa | não |

Estados evento: `Agendado` | `Em andamento` | `Concluído`.  
Presença: `Pendente` | `Parcial` | `Completa` | `Inelegível`.

### 8.3 `Card/UltimasSolicitacoes`

Header: `Últimas solicitações` + link `Ver todas`.

**`DS/DataTable/Compact`**

| Coluna | Width | Align |
|--------|------:|-------|
| Número | 100 | left |
| Tipo | flex 1 | left |
| Estado | 120 | left |
| Prazo | 80 | left |
| SLA | 80 | left |

Header row: h 36, px 16, caption semibold.  
Data row: h 48, px 16, divider.

| Número | Tipo | Estado | Prazo | SLA |
|--------|------|--------|-------|-----|
| 2026-0042 | Trancamento | Em análise | 12/06 | OK |
| 2026-0031 | Aproveitamento | Em ajuste | 05/06 | Atrasado |
| 2026-0028 | Matrícula especial | Deferida | — | — |

SLA: texto `OK` ou ícone + `Atrasado` (não só cor).

---

## 9. `MainGrid` — coluna secundária

### 9.1 `Card/PrazosCalendario`

**`DS/TimelineItem` × 3**, gap 16.

```
horizontal, gap 12
├── DateBlock w 48 — dia + mês empilhados
└── Content flex 1 — label + chip curso?
```

Itens: entrega relatório estágio; fim prazo solicitação; evento calendário acadêmico.

### 9.2 `Card/UltimoParecer`

**`DS/HighlightCard`**, p 20, gap 12.

```
vertical
├── Badge estado + caption data
├── Title semibold, max 2 linhas
├── Excerpt 2 linhas truncate
└── Link "Ver detalhes" + arrow 16
```

Exemplo: Oficina de Metodologia Científica — Aprovada — "Aprovada com 8h…".

### 9.3 `Card/AtalhosRapidos`

Grid **2 × 3**, gap 12.

**`DS/QuickTile` × 6** — vertical center, min-h 88, p 12, gap 8, icon 24, label caption 2 linhas max.

| # | Label | Rota |
|---|-------|------|
| 1 | Nova formativa | `/formativas/nova` |
| 2 | Meus estágios | `/estagios` |
| 3 | Meu TCC | `/tccs` |
| 4 | Comunicação | `/comunicacao` |
| 5 | Certificados | `/certificados` |
| 6 | Eventos | `/eventos` |

---

## 10. Biblioteca `DS/*`

| Componente | Variantes | Dimensões |
|------------|-----------|-----------|
| `DS/Button` | primary, secondary, ghost, link | h 40 / h 32 small |
| `DS/Card` | default, interactive | p 24 |
| `DS/Badge` | neutral, success, warning, danger, info | h 24, px 8 |
| `DS/KpiCard` | with-progress, number-only | min-h 120 |
| `DS/NavItem` | default, active | h 44 |
| `DS/AlertBanner` | info, warning, success, error | full width |
| `DS/PendenciaItem` | default | min-h 72 |
| `DS/EventoRow` | with-cta, without-cta | — |
| `DS/DataTable/Compact` | with-header | row 48 |
| `DS/TimelineItem` | default | — |
| `DS/QuickTile` | default | min 88×88 |
| `DS/Avatar` | sm, md, lg | 32 / 36 / 48 |
| `DS/Input/Search` | with-icon | h 40 |
| `DS/Skeleton` | line, block, circle | — |

### Papéis tipográficos

| Papel | Uso |
|-------|-----|
| Heading/H1 | Saudação |
| Heading/H2 | Valores KPI |
| Heading/H3 | Títulos de card |
| Body/Default | listas, tabela |
| Body/Semibold | títulos de item |
| Caption/Muted | metas, subtítulos |

---

## 11. Frames de estado

### Skeleton (`02 — Desktop estados`)

Substituir dados por `DS/Skeleton` mantendo posições §2–§9:

- KPI: bloco 60% + linha caption
- Pendências: 3× (círculo 40 + linhas)
- Tabela: 4 linhas full width

### Empty — pendências

`Card/Pendencias` empty; demais cards com dados.

### Erro parcial

`AlertBanner` error no topo `Main`: `Não foi possível atualizar eventos. Tentar novamente.` — restante inalterado.

---

## 12. Regras de construção

1. Auto Layout em 100% dos containers.
2. Layers: `Shell/...`, `Main/...`, `DS/...`.
3. Protocolos: formato `AAAA-NNNN`.
4. Truncate: títulos lista 1 linha; parecer 2 linhas.
5. Touch mobile: alvo mínimo 44×44 em nav, tiles, botões.
6. Excluir: modais, PIN/QR, wizard solicitação, telas admin.
7. Ordem de leitura = ordem visual; região alertas: `aria-live`.

---

## 13. Critérios de entrega

- [ ] Sidebar: 11 itens §4.2, `Início` active
- [ ] Topbar: busca, notificação, user menu
- [ ] `KpiRow`: 4 cards desktop
- [ ] `MainGrid` 2:1 — 3 + 3 cards
- [ ] Pendências: 3 itens, divisores, ação
- [ ] Eventos: 3 linhas, 1 CTA Validar
- [ ] Tabela: 5 colunas, SLA textual
- [ ] Atalhos: grid 2×3
- [ ] Mobile: mesmos blocos empilhados
- [ ] Página `04 — Componentes` com `DS/*`
- [ ] Frame Skeleton
- [ ] Estilo alinhado à referência visual fornecida
