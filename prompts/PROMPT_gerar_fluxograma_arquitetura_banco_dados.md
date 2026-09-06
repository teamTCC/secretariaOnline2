# Prompt — Fluxograma da Arquitetura Completa do Banco de Dados (TCC — SecretariaOnline2)

**Objetivo:** gerar **uma imagem** (fluxograma horizontal, estilo acadêmico/TCC) que explique como funcionam, em conjunto, o banco transacional, o armazenamento de arquivos, o processamento assíncrono, a auditoria e as consultas analíticas do **SecretariaOnline2**.

**Idioma do diagrama:** português do Brasil  
**Formato de saída desejado:** PNG ou SVG em alta resolução, orientação **paisagem (landscape)**, proporção aproximada **16:9** ou **A4 horizontal**  
**Público-alvo da figura:** banca de TCC e leitores técnicos não especialistas em banco de dados

---

## Como usar este prompt

1. **Gemini (imagem):** copie só a seção **§ PROMPT PARA COLAR NA IA** (já inclui layout em grade, mapa de setas e regras anti-erro). Anexe imagem de referência de estilo, se tiver. Veja também **§ Erros comuns** e **§ Uso no Gemini**.
2. **Outras IAs de imagem:** mesmo bloco final; se ilegível, peça segunda iteração com o checklist do rodapé do prompt.
3. **Chat (refinar):** pode colar o documento inteiro, mas **não** cole o prompt final duas vezes.
4. Salve o resultado em `foundationDocs/DB/figuras/FIGURA-1-arquitetura-completa-banco-dados.png`.

**Ferramentas sugeridas:** DALL·E, Midjourney, Ideogram, Gemini (imagem), Napkin AI, Lucidchart (com prompt), ou Figma Make com modo diagrama.

---

## Contexto do sistema (não inventar além disso)

**SecretariaOnline2** é a modernização do sistema acadêmico *Secretaria Online* (UFPR SEPT). Arquitetura: **monólito modular** (Kotlin + Spring Boot 3) com **PostgreSQL 16** como banco transacional único no MVP, **MinIO** para blobs, **padrão Outbox** para notificações assíncronas (sem RabbitMQ no MVP), clientes **React Web** e **React Native (Expo)**.

> **Importante:** o sistema **não** possui data warehouse OLAP separado no MVP. Estatísticas consultam o OLTP com cache de 5 minutos. A evolução futura (réplica read-only) deve aparecer apenas como caixa tracejada opcional — **não** como fluxo principal.

---

## Referência visual (imitar o estilo, não o domínio)

Reproduzir o **layout e a linguagem visual** do exemplo anexo (fluxograma "Arquitetura completa do Banco de Dados do Sistema"):

| Elemento do exemplo | Adaptação para SecretariaOnline2 |
|---------------------|----------------------------------|
| Fundo bege/amarelo claro | Manter |
| Título grande amarelo/laranja no canto superior esquerdo | **"Arquitetura completa do Banco de Dados — SecretariaOnline2"** |
| Fluxo principal da esquerda para a direita | Manter |
| Cilindro vermelho = OLTP | **PostgreSQL 16 (OLTP)** |
| Caixa azul escura = processamento batch | **Processamento Assíncrono (Outbox + Jobs)** |
| Cilindro azul claro = camada analítica | **Consultas Analíticas (MVP)** — *não* chamar de OLAP |
| Ícone verde = visualização | **Dashboards Recharts** (secretaria/coordenação) |
| Balões/caixas arredondadas com lista numerada | Manter para cada componente |
| Setas com rótulos de ação | Manter (Persiste, Enfileira, Consulta, Envia, etc.) |
| Figura de usuário no final | **Usuários do sistema acadêmico** |
| Loop de retorno do usuário | **Filtros de período/curso** nas estatísticas; **download** de exportações |

---

## Mapa do fluxo (conteúdo obrigatório)

### Bloco 1 — Entrada de dados (extrema esquerda)

**Ícones:** computador (portal web) + smartphone (app mobile)

**Rótulo principal:** `Usuários do Sistema Acadêmico`

**Sub-rótulos (pequenos):** Aluno · Professor · Secretaria · Coordenador · Admin · Público (verificação de certificado)

**Seta saindo para a direita:** `Requisições REST + JWT (HATEOAS)`

**Caixa explicativa (bege):**
1. Web: React 18 + Vite + TanStack Query  
2. Mobile: React Native + Expo  
3. API stateless; autorização por capabilities (FGAC)  
4. Upload/download de arquivos via URL pré-assinada (não passa pelo banco relacional)

---

### Bloco 2 — Camada de aplicação (opcional, entre usuários e banco)

**Retângulo azul médio:** `Backend Spring Boot 3 (Monólito Modular)`

**Caixa explicativa (azul escuro):**
1. 9 bounded contexts: iam, academico, solicitacoes, formativas, estagio, tcc, presenca, comunicacao, auditoria/arquivos  
2. Clean Architecture: domain → application → infrastructure → api  
3. Flyway aplica migrations SQL versionadas (`V###__*.sql`)  
4. Toda mutação de estado relevante grava `audit_log`

**Seta para a direita:** `Persiste / Consulta`

---

### Bloco 3 — Núcleo transacional (centro-esquerda) — **destaque visual**

**Cilindro vermelho grande:** `Banco OLTP (PostgreSQL 16)`

**Caixa explicativa (vermelho claro/rosa):**
1. Registro operacional de todo o domínio acadêmico (solicitações, presença, formativas, estágio, TCC, comunicações)  
2. **31 tabelas** em estrutura **normalizada (3NF)**  
3. Chaves **UUIDv7**; datas **TIMESTAMPTZ**; schemas variáveis em **JSONB** (`request_type.form_schema`, `request_type.workflow_json`, `request.dados`)  
4. Extensões: `pgcrypto`, `uuid-ossp`, `citext`  
5. Versionamento de schema via **Flyway** (migrations imutáveis, forward-only)

**Lista compacta dos módulos (dentro ou ao lado do cilindro):**

| Módulo | Tabelas principais |
|--------|-------------------|
| M1 IAM | `usuario`, `role`, `authority`, `role_authority`, `usuario_role`, `refresh_token`, `jti_blacklist` |
| M2 Acadêmico | `curso`, `disciplina`, `periodo_letivo`, `calendario_academico` |
| M3 Solicitações | `request_type`, `request`, `request_event`, `request_line_item`, `request_attachment` |
| M4 Formativas | `formative_activity`, `formative_entry` |
| M5 Estágio | `internship`, `internship_document` |
| M6 TCC | `tcc`, `tcc_member`, `tcc_examiner` |
| M7 Comunicação | `communication`, `communication_delivery`, `notification_preference`, `outbox_event` |
| M8 Presença v4.1 | `event_attendance`, `attendance_session` |
| M9 Certificados/Auditoria | `certificate`, `audit_log` |

**Setas saindo do cilindro OLTP (múltiplas, para baixo e para a direita):**

| Seta | Destino | Rótulo |
|------|---------|--------|
| 1 | Processamento Assíncrono | `Enfileira eventos` |
| 2 | MinIO | `Metadados de arquivo` |
| 3 | Trilha de Auditoria | `Registra imutável` |
| 4 | Consultas Analíticas | `Agrega (SELECT)` |
| 5 | Jobs Export/Import | `Dispara job` |

---

### Bloco 4 — Processamento assíncrono (centro) — equivalente visual ao "ETL" do exemplo

**Retângulo azul escuro horizontal:** `Processamento Assíncrono (Outbox + Scheduled Jobs)`

**Caixa explicativa (azul escuro):**
1. **Padrão Outbox:** `INSERT outbox_event` na mesma transação da mutação de negócio (consistência ACID)  
2. **OutboxDispatcher:** `@Scheduled(fixedDelay=5000)` — lê até 50 eventos `PENDING` com `FOR UPDATE SKIP LOCKED`  
3. Tipos de evento: `solicitacoes.deliberated`, `exports.ready`, `imports.completed`, `estagios.document_reviewed`, etc.  
4. **Export Jobs:** gera CSV volumoso em background → grava no MinIO → notifica por e-mail  
5. **Import Jobs:** valida CSV em lotes de 1.000 linhas → persiste no OLTP  
6. **Certificados:** ao concluir evento/aprovar formativa, gera PDF + hash SHA-256 + assinatura ED25519

**Setas saindo:**
- `Envia` → Canais de Notificação (e-mail Mailgun/SMTP + push FCM)  
- `Grava arquivo` → MinIO  
- `Atualiza status` → OLTP (retorno pontilhado)

> **Não** rotular este bloco como "ETL Python" — o MVP usa **Kotlin + Spring @Scheduled**, não pipeline Python/Pandas.

---

### Bloco 5 — Armazenamento de objetos (centro-direita, paralelo ao OLTP)

**Cilindro roxo ou teal:** `Object Storage (MinIO / S3-compatible)`

**Caixa explicativa (roxo/teal claro):**
1. Blobs **fora** do PostgreSQL: PDFs, imagens, CSVs de exportação  
2. Tabelas relacionais guardam apenas `storage_key`, `sha256`, `mime_type`, `tamanho_bytes`  
3. Upload: backend emite URL pré-assinada → cliente faz PUT direto no MinIO  
4. Download: URL pré-assinada temporária (15 min download; exportações expiram em 7 dias)  
5. Buckets: anexos de solicitação, documentos de estágio, fotos de perfil, certificados PDF, exports CSV

**Seta de retorno para usuário:** `Download via link assinado`

---

### Bloco 6 — Trilha de auditoria (faixa inferior, paralela ao fluxo principal)

**Retângulo cinza com ícone de escudo ou livro:** `Auditoria Imutável (audit_log)`

**Caixa explicativa (cinza):**
1. Todo comando que altera estado emite registro append-only  
2. Campos: `ator`, `acao`, `alvo_tipo`, `alvo_id`, `dados_antes/depois` (JSONB), `resultado`, `ip`, `timestamp`  
3. Consultável por admin em `/admin/audit-log`  
4. Importações e exportações registram checksum SHA-256 para rastreabilidade

**Sem seta de escrita de volta ao OLTP** (somente leitura para conformidade)

---

### Bloco 7 — Consultas analíticas (centro-direita)

**Cilindro azul claro:** `Camada Analítica (MVP — consulta OLTP)`

**Caixa explicativa (azul claro):**
1. Dashboards de estatísticas operacionais (`/secretaria/estatisticas`)  
2. API: `GET /reports/secretary` com filtros período + curso  
3. Gráficos Recharts: solicitações por tipo, evolução temporal, distribuição por estado, horas formativas  
4. Cache de **5 minutos** no backend; drill-down tabular paginado  
5. **Não há** schema estrela nem banco OLAP separado no MVP

**Caixa tracejada (evolução futura, menor, canto inferior):**  
`Evolução (6–12 meses): réplica PostgreSQL read-only para relatórios pesados`

**Seta para visualização:** `Envia dados agregados`

---

### Bloco 8 — Visualização e notificação (extrema direita)

**Ícone verde (gráfico de barras/linhas):** `Visualização (Secretaria / Coordenação)`

**Ícone azul (envelope + sino):** `Notificações Multicanal`

**Caixa explicativa do robô/IA do exemplo → substituir por Notificações:**
1. E-mail HTML renderizado por TemplateEngine  
2. Push via Firebase Cloud Messaging (app mobile)  
3. Hub de avisos in-app (`communication` + `communication_delivery`)  
4. Preferências por usuário em `notification_preference`

**Figura de usuário:** `Usuário`

**Setas finais:**
- `Visualização` → Usuário (dashboards, tabelas, gráficos)  
- `Notificação` → Usuário (e-mail, push, hub)  
- `Seleciona filtros` ← Usuário → Consultas Analíticas (loop de retorno, como no exemplo)  
- `Baixa exportação` ← Usuário → MinIO (loop opcional)

---

### Bloco 9 — Verificação pública (canto inferior direito, opcional)

**Ícone de QR code + cadeado:** `Verificador Público de Certificados`

**Rota:** `/publico/verificar-certificado/:hash`  
**Sem autenticação** — consulta hash SHA-256 e chave pública em `/.well-known/jwks.json`  
**Seta tracejada** desde `certificate` (OLTP) e PDF (MinIO)

---

## Paleta de cores sugerida

| Componente | Cor principal | Cor da caixa explicativa |
|------------|---------------|--------------------------|
| Fundo | `#F5F0E1` bege claro | — |
| Título | `#E8A317` amarelo-laranja | — |
| PostgreSQL OLTP | `#C0392B` vermelho | `#FADBD8` rosa claro |
| Spring Boot / API | `#2C3E50` azul escuro | `#D6EAF8` azul muito claro |
| Outbox + Jobs | `#1A5276` azul petróleo | `#AED6F1` |
| MinIO | `#6C3483` roxo | `#E8DAEF` |
| Analítico (MVP) | `#2980B9` azul | `#D4E6F1` |
| Visualização | `#27AE60` verde | `#D5F5E3` |
| Auditoria | `#7F8C8D` cinza | `#EAECEE` |
| Setas | `#2C3E50` ou preto | — |

---

## Regras de qualidade da figura

1. **Legibilidade:** texto das caixas explicativas em fonte sans-serif (Arial, Helvetica ou similar), tamanho mínimo equivalente a 9–10 pt impresso.  
2. **Hierarquia:** o cilindro PostgreSQL OLTP deve ser o **maior** elemento visual (núcleo do diagrama).  
3. **Fidelidade técnica:** não mostrar MongoDB, RabbitMQ, Redis ou data warehouse como componentes do MVP.  
4. **Coerência:** todas as setas com sentido lógico (esquerda→direita no fluxo principal; retornos pontilhados para downloads e filtros).  
5. **Legenda da figura (rodapé):**  
   *Figura X – Arquitetura completa do banco de dados do SecretariaOnline2: camada transacional PostgreSQL 16, armazenamento de objetos MinIO, processamento assíncrono via Outbox, trilha de auditoria imutável e consultas analíticas com cache sobre o OLTP (MVP).*  
6. **Fonte (canto inferior):** *Fonte: Os autores (2026), com base em `foundationDocs/DB/` e `analise_arquitetural_secretariaonline2.md`.*

---

## O que NÃO incluir (erros comuns a evitar)

- ❌ Pipeline ETL Python / Pandas / SQLAlchemy (é do exemplo de referência, não do SO2)  
- ❌ Banco OLAP / Data Warehouse / Star Schema como componente principal do MVP  
- ❌ Camada de IA / Machine Learning analisando dados  
- ❌ RabbitMQ, Kafka ou Redis como fila no MVP  
- ❌ MD5, sessões server-side, filesystem local `C:\Users\...` (legado)  
- ❌ Entidades removidas do modelo: `DELIBERATION`, `FORM_SCHEMA`, `WORKFLOW_DEFINITION` como tabelas separadas  
- ❌ `ATTENDANCE_CHECKIN` — o nome correto é `attendance_session`  
- ❌ **Duplicar** qualquer bloco (especialmente Backend / BLOCO 2)  
- ❌ **Reutilizar** círculos numerados 1/2/3 em caixas de blocos diferentes  
- ❌ Seta **MinIO → Analítico** (analítico consulta só o OLTP)  
- ❌ Pular **BLOCO 6** (Auditoria) — numerar 1 a 8 sem saltos  
- ❌ Geofence, BLE ou trust score no módulo de presença v4.1

---

## Variações opcionais (pedir em iteração separada se necessário)

| Variação | Quando usar |
|----------|-------------|
| **Simplificada** | Slide de apresentação oral — omitir tabela de 31 tabelas; manter só 5 blocos principais |
| **Detalhada** | Apêndice do TCC — incluir mini-ER com hub `USUARIO` e os 4 módulos de maior volume |
| **Com migração legado** | Adicionar caixa tracejada à esquerda: "Sistema Legado (Java EE + MD5)" → seta "ETL de migração (Big Bang)" → OLTP |

---

## Erros comuns em gerações anteriores (corrigir no prompt)

| Erro observado | Correção obrigatória no novo desenho |
|----------------|--------------------------------------|
| Círculo **"1"** repetido em Blocos 1, 2, 3 e 4 | Cada bloco tem **um único selo** "BLOCO N" (N de 1 a 8); listas internas usam **bullets (•)**, não círculos 1/2/3 |
| **Bloco 2 (Backend) duplicado** (duas caixas idênticas) | Backend aparece **uma única vez**, entre Entrada e OLTP |
| Pulou o **Bloco 6** (foi direto de 5 para 7) | Auditoria é **BLOCO 6**, faixa inferior central |
| Seta **MinIO → Analítico** | **Proibido.** Analítico lê **somente** do OLTP (SELECT agregado) |
| Setas cruzadas no centro | Seguir o **mapa de setas** da grade abaixo; rotas paralelas, sem cruzar |
| OLAP / ETL Python | Não incluir |

---

## Uso no Gemini (imagem)

1. Anexe a imagem de **referência de estilo** (fluxograma bege com cilindros), se tiver.
2. Cole **somente** o bloco **§ PROMPT PARA COLAR NA IA** abaixo (já contém layout + conteúdo + anti-erros).
3. **Não** cole o prompt duas vezes — o documento inteiro é opcional só para iteração em chat.
4. Se a imagem sair errada, peça refação citando: *"corrija: Bloco 2 duplicado, círculos 1 repetidos, seta MinIO→Analítico"*.

---

## PROMPT PARA COLAR NA IA

Copie o bloco abaixo **na íntegra** (recomendado para Gemini com geração de imagem):

---

```
TAREFA: Gere UMA única imagem PNG/SVG — fluxograma horizontal de arquitetura de dados, estilo acadêmico/TCC, em português do Brasil. Não descreva em texto: produza a figura completa.

TÍTULO (canto superior esquerdo, amarelo-laranja #E8A317, fonte grande):
"Arquitetura completa do Banco de Dados — SecretariaOnline2"

FUNDO: bege claro #F5F0E1 | Orientação: paisagem 16:9 | Alta resolução | Texto legível (sans-serif)

DOMÍNIO: portal de secretaria acadêmica universitária (UFPR SEPT). NÃO é biologia, ciência de dados com Python, nem data warehouse corporativo genérico.

═══════════════════════════════════════════════════════════════
REGRAS ANTI-ERRO (OBRIGATÓRIAS — violar invalida a figura)
═══════════════════════════════════════════════════════════════

R1. Existem EXATAMENTE 8 blocos numerados (BLOCO 1 … BLOCO 8). Cada bloco aparece UMA ÚNICA VEZ na imagem. NUNCA duplicar o Backend nem qualquer outro bloco.

R2. Identificação dos blocos: use selo retangular ou faixa com texto "BLOCO 1", "BLOCO 2", … "BLOCO 8" — um selo por bloco, cores distintas por camada.

R3. PROIBIDO usar círculos numerados ① ② ③ como identificador de bloco nas caixas explicativas — isso causou repetição do número "1" em blocos diferentes na geração anterior. Dentro das caixas explicativas, use bullets (•) ou travessões (—), nunca reutilizar 1/2/3 em blocos distintos.

R4. O cilindro PostgreSQL OLTP (BLOCO 3) é o MAIOR elemento visual — núcleo central do diagrama.

R5. PROIBIDO: OLAP, Data Warehouse, ETL Python, Pandas, RabbitMQ, Kafka, Redis, IA/robô analisando dados, MongoDB.

R6. A camada Analítica (BLOCO 7) consulta APENAS o PostgreSQL OLTP. NÃO desenhar seta de MinIO para o Analítico.

R7. Fluxo principal da esquerda para a direita: Entrada → Backend → OLTP → ramificações → Saída.

═══════════════════════════════════════════════════════════════
GRADE ESPACIAL (posicionar os 8 blocos nestas células — não sobrepor)
═══════════════════════════════════════════════════════════════

Linha SUPERIOR (fluxo principal, esquerda → direita):
  [BLOCO 1 Entrada] → [BLOCO 2 Backend] → [BLOCO 3 OLTP] → [BLOCO 4 Async] → [BLOCO 7 Analítico] → [BLOCO 8 Saída]

Linha MÉDIA (paralelo ao OLTP, à direita do centro):
  [BLOCO 5 MinIO] — abaixo ou à direita do BLOCO 4, SEM sobrepor setas do OLTP

Linha INFERIOR (faixa horizontal contínua, abaixo do OLTP):
  [BLOCO 6 Auditoria] — centralizada sob o BLOCO 3

Canto inferior direito (pequeno, opcional):
  [Verificador Público de Certificados] — ícone QR, fora da grade principal

═══════════════════════════════════════════════════════════════
MAPA DE SETAS (desenhar TODAS; evitar cruzamentos no centro)
═══════════════════════════════════════════════════════════════

FLUXO PRINCIPAL (setas sólidas, esquerda→direita):
  B1 ─"Requisições REST + JWT"→ B2 ─"Persiste / Consulta"→ B3

SAÍDAS DO OLTP (B3) — rotas separadas, sem cruzar:
  B3 ─"Enfileira eventos (outbox_event)"→ B4
  B3 ─"Metadados (storage_key, sha256)"→ B5
  B3 ─"Registra imutável"→ B6  (seta para baixo)
  B3 ─"Agrega (SELECT)"→ B7

PROCESSAMENTO ASSÍNCRONO (B4):
  B4 ─"Grava arquivo (PDF/CSV)"→ B5
  B4 ─"Envia notificação"→ B8 (componente Notificações dentro do B8)
  B4 - - -"Atualiza status" - - -→ B3  (seta PONTILHADA de retorno)

MINIO (B5):
  B5 - - -"Download (URL pré-assinada)" - - -→ Usuário no B8  (seta pontilhada)

ANALÍTICO (B7):
  B7 ─"Dados agregados"→ B8 (componente Visualização)
  NÃO conectar B5 → B7

RETORNO DO USUÁRIO (setas pontilhadas):
  Usuário (B8) - - -"Seleciona filtros (período, curso)" - - -→ B7

AUDITORIA (B6):
  Somente entrada vinda de B3. Sem seta de saída para OLTP (append-only, consulta admin).

═══════════════════════════════════════════════════════════════
CONTEÚDO DE CADA BLOCO (texto exato ou muito próximo)
═══════════════════════════════════════════════════════════════

─── BLOCO 1 — ENTRADA (faixa bege/amarelo claro, extrema esquerda) ───
Selo: "BLOCO 1 — ENTRADA"
Ícones: monitor (web) + smartphone (mobile)
Título: "Usuários do Sistema Acadêmico"
Subtítulo em linha: Aluno · Professor · Secretaria · Coordenador · Admin · Público
Caixa explicativa (bullets, fundo bege):
  • Web: React 18 + Vite + TanStack Query
  • Mobile: React Native + Expo
  • API REST stateless; JWT + HATEOAS (FGAC — UI cega a perfis)
  • Arquivos: upload/download via URL pré-assinada (bytes NÃO vão ao Postgres)

─── BLOCO 2 — BACKEND (retângulo azul escuro #2C3E50, UMA vez só) ───
Selo: "BLOCO 2 — BACKEND"
Título no retângulo: "Backend Spring Boot 3 (Monólito Modular)"
Linguagem: Kotlin
Caixa explicativa (fundo azul claro #D6EAF8, bullets):
  • 9 módulos: iam · academico · solicitacoes · formativas · estagio · tcc · presenca · comunicacao · auditoria/arquivos
  • Clean Architecture por módulo (domain → application → infrastructure → api)
  • Flyway: migrations SQL versionadas (V###__*.sql)
  • Toda mutação de estado grava audit_log (via BLOCO 6)

─── BLOCO 3 — OLTP (cilindro vermelho #C0392B, MAIOR do diagrama) ───
Selo: "BLOCO 3 — NÚCLEO TRANSACIONAL"
Título no cilindro: "Banco OLTP (PostgreSQL 16)"
Caixa explicativa (fundo rosa claro #FADBD8, bullets):
  • Registro operacional: solicitações, presença, formativas, estágio, TCC, comunicações
  • 31 tabelas · 3NF · 9 módulos de domínio
  • UUIDv7 · TIMESTAMPTZ · JSONB (form_schema, workflow_json, request.dados)
  • Extensões: pgcrypto, citext · Schema via Flyway
Lista compacta (uma linha ou duas, fonte menor):
  M1 IAM · M2 Acadêmico · M3 Solicitações (19 tipos, engine genérica) · M4 Formativas · M5 Estágio · M6 TCC · M7 Comunicação/Outbox · M8 Presença v4.1 · M9 Certificados

─── BLOCO 4 — PROCESSAMENTO ASSÍNCRONO (retângulo azul petróleo #1A5276) ───
Selo: "BLOCO 4 — PROCESSAMENTO ASSÍNCRONO"
Título: "Outbox + Scheduled Jobs (Kotlin)"
NÃO escrever "ETL Python"
Caixa explicativa (fundo #AED6F1, bullets):
  • Outbox: INSERT outbox_event na mesma transação ACID da regra de negócio
  • OutboxDispatcher: @Scheduled a cada 5 s (até 50 eventos PENDING)
  • Export Jobs: gera CSV → MinIO → e-mail (exports.ready)
  • Import Jobs: valida CSV em lotes de 1.000 → persiste no OLTP
  • Certificados: PDF gerado pelo sistema + hash SHA-256 + assinatura ED25519

─── BLOCO 5 — OBJECT STORAGE (cilindro roxo #6C3483) ───
Selo: "BLOCO 5 — ARMAZENAMENTO DE OBJETOS"
Título no cilindro: "MinIO (S3-compatible)"
Caixa explicativa (fundo #E8DAEF, bullets):
  • Blobs fora do Postgres: PDFs, imagens, CSVs de exportação
  • Postgres guarda só: storage_key, sha256, mime_type
  • Upload: URL pré-assinada → PUT direto no MinIO
  • Download: URL pré-assinada (15 min); exports expiram em 7 dias

─── BLOCO 6 — AUDITORIA (faixa cinza #7F8C8D, linha INFERIOR, sob o OLTP) ───
Selo: "BLOCO 6 — AUDITORIA"
Título: "audit_log (imutável, append-only)"
Caixa explicativa (fundo #EAECEE, bullets):
  • Cada comando de mutação: ator + ação + alvo + JSON antes/depois
  • Consulta admin: /admin/audit-log
  • Importações/exportações: checksum SHA-256 registrado
  • Sem UPDATE/DELETE — trilha de conformidade (LGPD)

─── BLOCO 7 — ANALÍTICO MVP (cilindro azul claro #2980B9) ───
Selo: "BLOCO 7 — CONSULTAS ANALÍTICAS"
Título no cilindro: "Estatísticas (MVP — sobre OLTP)"
NÃO rotular "OLAP" nem "Data Warehouse"
Caixa explicativa (fundo #D4E6F1, bullets):
  • Tela: /secretaria/estatisticas · API: GET /reports/secretary
  • Gráficos Recharts: solicitações por tipo, evolução temporal, estados, horas formativas
  • Cache backend: 5 min · drill-down tabular paginado
Caixa TRACEJADA menor (evolução futura, fonte pequena):
  "Evolução: réplica PostgreSQL read-only (6–12 meses)"

─── BLOCO 8 — SAÍDA (extrema direita, ícones + usuário) ───
Selo: "BLOCO 8 — SAÍDA"
Dois componentes lado a lado:
  [Ícone gráfico verde #27AE60] "Visualização — Secretaria / Coordenação"
  [Ícone envelope+sino azul] "Notificações — E-mail · Push FCM · Hub in-app"
Figura humana simplificada: "Usuário"
Caixa explicativa verde claro (bullets):
  • Dashboards e tabelas (Recharts)
  • E-mail HTML (TemplateEngine) + push mobile
  • Hub: communication + communication_delivery
Setas saindo para o Usuário: "Visualização" e "Notificação"

─── EXTRA (canto inferior direito, pequeno) ───
Ícone QR + texto: "Verificador Público de Certificados"
Subtexto: /publico/verificar-certificado/:hash (sem login)
Setas tracejadas desde B3 (hash) e B5 (PDF) — opcional, não poluir o centro

═══════════════════════════════════════════════════════════════
PALETA E HIERARQUIA VISUAL
═══════════════════════════════════════════════════════════════

• Vermelho = OLTP (maior cilindro, centro-esquerda)
• Azul escuro = Backend | Azul petróleo = Async | Azul claro = Analítico
• Roxo = MinIO | Verde = Visualização | Cinza = Auditoria | Bege = Entrada
• Setas sólidas = fluxo principal | Setas pontilhadas = retorno/download/filtros
• Mínimo de cruzamento de setas no centro — rotear por cima ou por baixo do cilindro OLTP

═══════════════════════════════════════════════════════════════
RODAPÉ (barra inferior, fonte menor, centralizado)
═══════════════════════════════════════════════════════════════

"Figura X – Arquitetura completa do banco de dados do SecretariaOnline2: PostgreSQL 16 (OLTP), MinIO, Outbox assíncrono, auditoria imutável e estatísticas com cache sobre o OLTP (MVP)."
"Fonte: Os autores (2026)."

CHECKLIST FINAL antes de finalizar a imagem:
☐ 8 blocos, cada um uma vez
☐ Selos "BLOCO 1"…"BLOCO 8" únicos (sem círculos 1 repetidos)
☐ Backend não duplicado
☐ Sem seta MinIO → Analítico
☐ OLTP é o maior elemento
☐ Sem OLAP / Python / RabbitMQ
☐ Português em todos os rótulos
```

---

## Checklist pós-geração

- [ ] Exatamente **8 blocos** (BLOCO 1 … BLOCO 8), **cada um uma única vez**
- [ ] **Sem** Backend (BLOCO 2) duplicado
- [ ] Selos "BLOCO N" únicos — **sem** círculos numerados 1/2/3 repetidos entre blocos
- [ ] BLOCO 6 (Auditoria) presente na faixa inferior
- [ ] PostgreSQL OLTP é o elemento central e **maior**
- [ ] MinIO separado do relacional; **sem** seta MinIO → Analítico
- [ ] Analítico lê **somente** do OLTP (SELECT agregado + cache 5 min)
- [ ] Outbox aparece (não RabbitMQ)
- [ ] Sem OLAP / ETL Python / IA como componentes principais
- [ ] Loop de filtros do usuário → BLOCO 7; download exportação → MinIO
- [ ] Título e rodapé em português
- [ ] Texto legível em zoom 100% em A4 horizontal

---

**Arquivo:** `foundationDocs/prompts/PROMPT_gerar_fluxograma_arquitetura_banco_dados.md`  
**Versão:** 1.1 — 2026-06-23 (grade espacial, anti-duplicação, mapa de setas, uso Gemini)  
**Referências:** `foundationDocs/DB/modelo-conceitual.md`, `foundationDocs/DB/00-inventario-e-decisoes.md`, `foundationDocs/analysis/analise_arquitetural_secretariaonline2.md` §3–5 e §13.3, `foundationDocs/sequenceDiagrams/transversal/10.1-outbox-notificacao.md`, RF-F5-010 e RF-F5-011
