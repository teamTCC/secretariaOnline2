Você tem acesso ao Figma MCP. Seu trabalho é criar um Design System completo diretamente no Figma para o meu projeto.

## INFORMAÇÕES DO PROJETO

- Nome: [nome do projeto]
- O que faz: [descrição curta]
- Pra quem: [público-alvo]
- Sensação visual: [ex: clean e profissional / divertido e jovem / dark e sofisticado]
- Referências visuais: [ex: Linear, Notion, Spotify, Amie, Raycast]

## O QUE CRIAR NO FIGMA

### 1. VARIÁVEIS — Color Primitives

Crie uma collection “Primitives” com 12-16 cores base que refletem a identidade do projeto. Inclua:

- Neutros (4-5): do mais escuro ao branco
- Cinzas (2-3): tons intermediários
- Cor principal da marca (3-4 variações: light, base, dark)
- Cor de acento/destaque (1-2)
- Cor de erro/perigo (1)

### 2. VARIÁVEIS — Semantic Color Tokens

Crie uma collection “Color” com tokens semânticos que apontam para os primitives. Dois modos: Light e Dark. Grupos:

text (5 tokens):
- primary → título e texto importante
- secondary → texto de apoio
- muted → placeholders, texto desabilitado
- on-dark → texto sobre fundos escuros
- brand → texto na cor da marca

surface (5 tokens):
- page → fundo principal da página
- sidebar → fundo da navegação lateral
- card → fundo de cards
- elevated → elementos elevados (modais, popovers)
- canvas → fundo de áreas de conteúdo

action (3 tokens):
- primary → botões principais e CTAs
- primary-hover → hover do primary
- secondary → botões secundários

border (3 tokens):
- default → bordas padrão
- subtle → bordas sutis
- focus → focus ring de inputs e botões

status (3 tokens):
- success → confirmação
- warning → alerta
- error → erro, perigo

brand (4 tokens):
- youtube, substack, linkedin, instagram (ou as plataformas relevantes pro projeto)

### 3. VARIÁVEIS — Spacing

Crie uma collection “Spacing” com escala consistente:

- space-1: 4px
- space-2: 8px
- space-3: 12px
- space-4: 16px
- space-6: 24px
- space-8: 32px
- space-10: 40px
- space-12: 48px
- space-16: 64px
- space-20: 80px
- space-24: 96px
- space-32: 128px
- space-40: 160px

### 4. TEXT STYLES

Crie text styles usando a tipografia que combina com o projeto:

- heading (heading/1 a heading/4): fonte de display, pesos bold e semibold
- body (body/base, body/small): fonte de UI, peso regular
- label, caption: fonte de UI, peso medium, tamanhos menores

### 5. EFFECT STYLES

- shadow (para cards e elementos elevados)

### 6. COMPONENTES

Crie os seguintes componentes usando as variáveis semânticas:

Button — variantes: primary, secondary, outline, ghost, link, destructive. Tamanhos: sm, md, lg. Estados: default, hover, active, disabled.

Badge — variantes: default, success, warning, error, outline.

Avatar — tamanhos: sm, md, lg. Com e sem imagem (fallback com iniciais).

Card — usando surface-card, border-default, shadow. Com padding space-6.

### 7. PÁGINA FOUNDATIONS

Crie uma página chamada “Foundations” no Figma documentando visualmente:

- Seção Colors: swatches de primitives + semantic tokens organizados por grupo
- Seção Typography: exemplos de cada text style com nome e tamanho
- Seção Components: cada componente com todas as variantes visíveis

## REGRAS

- Use APENAS variáveis semânticas nos componentes, nunca hex codes diretos
- Todo componente deve usar auto layout
- Nomeie tudo em inglês, lowercase, com / pra grupos (ex: text/primary, surface/card)
- Comece pelos primitives, depois semantic tokens, depois componentes, depois a página de Foundations