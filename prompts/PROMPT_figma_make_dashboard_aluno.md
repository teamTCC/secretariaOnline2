Construa o protótipo da tela SecretariaOnline2 `/inicio` (Dashboard do Aluno).

ESTILO VISUAL (condicional):

**Se eu enviei paste e/ou imagem de referência neste chat** — aplicar esse visual (cores, tipografia, ícones, sombras, raios) nas três versões. As versões A, B e C diferem em **layout e hierarquia**, não em identidade visual.

**Se NÃO enviei referência de terceiros** — criar **três propostas visuais distintas** (A, B e C), cada uma com paleta, tipografia e tratamento de superfícies próprios, todas coerentes com o contexto abaixo. Não copiar templates genéricos de fintech, SaaS agressivo ou redes sociais.

Contexto do produto: portal de **secretaria acadêmica** de instituição universitária (matrícula, solicitações, formativas, eventos, certificados). Público: estudantes e servidores — interface **confiável, clara e humana**.

Diretrizes visuais (obrigatórias quando você definir o visual):
- **Sério e institucional**, porém **não austero** — acolhedor o suficiente para uso frequente pelo aluno.
- **Simples e minimalista** — poucos ornamentos, hierarquia tipográfica clara, bastante respiro.
- **Com cor** — fundos claros + cor de marca/acento + neutros; evitar interface cinza-monocromática.
- **Saturação média** — cores vivas o bastante para orientar e destacar estados; **sem** neon, gradientes chamativos ou contraste exagerado de startup.
- Acessível: contraste legível, estados (sucesso, alerta, erro) distinguíveis por cor **e** texto/ícone.

Sugestão de exploração quando não houver referência (adaptar livremente dentro das diretrizes):
- **A:** azul institucional + neutros frios, cards com borda sutil.
- **B:** base neutra quente + acento verde-azulado, superfícies planas, menos sombra.
- **C:** neutros + acento secundário contido (ex.: âmbar/teal só em CTAs e badges), sidebar levemente tonalizada.

ESTRUTURA: seguir o arquivo anexo `PROMPT_figma_make_dashboard_aluno_estrutura.md` (componentes `DS/*`, copy, shell, navegação, blocos obrigatórios). Em conflito entre referência visual e anexo, **priorizar a estrutura do anexo** e adaptar o visual.

ENTREGÁVEL — 3 versões desktop (`1440×1024`, com dados), páginas `/inicio — A`, `/inicio — B`, `/inicio — C`. Mesmos blocos funcionais em todas; sem omitir conteúdo.

| Versão | Layout (sempre) | Visual (só se sem referência externa) |
|--------|-----------------|--------------------------------------|
| **A — Clássica** | KPI 4 col → grid 2:1 (66% \| 33%) conforme mapa | Proposta visual A |
| **B — Ação primeiro** | Alertas + Pendências em destaque no topo; KPIs compactos; resto em 2:1 | Proposta visual B |
| **C — Densa** | KPI 2×2 ou faixa compacta; coluna larga (pendências/eventos/solicitações); direita estreita (prazos/atalhos) | Proposta visual C |

Sufixo em layers: `-A`, `-B`, `-C`.

Também entregar: mobile `390×844` (versão A), página `DS/*` compartilhada, frame Skeleton (versão A).

Conteúdo obrigatório (todas as versões):
- Shell: sidebar `256px` (9 nav + 2 rodapé), topbar `64px` (busca max `480px`, notificações, avatar+menu)
- `PageHeader`: H1 + subtítulo; "Ver solicitações" (ghost) + "Nova solicitação" (primary)
- `AlertStrip`: até 2 banners (warning ajuste + info presença)
- `KpiRow`: horas formativas (progresso), solicitações `3`, eventos `2`, certificados `1`
- Pendências (3 itens), Eventos (3 linhas, 1 CTA Validar), Solicitações (tabela 5×3)
- Prazos (3), Último parecer, Atalhos (`2×3`)

Auto Layout em todos os containers. Layers: `Shell/*`, `Main/*`, `DS/*`. Textos pt-BR realistas.

Ao finalizar: 1–2 frases por versão (layout +, se aplicável, paleta escolhida) e indique se o visual veio da referência enviada ou foi criado pelo Make.
