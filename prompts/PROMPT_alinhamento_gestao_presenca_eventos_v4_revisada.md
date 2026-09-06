# Prompt para alinhamento da documentação — Gestão de presença em eventos (revisão pós‑v4.0)

**Status (maio/2026):** alinhamento **v4.1** aplicado em `telas.md`, `endpoints_canonicos_presenca_eventos_v4.md`, `fluxos_por_perfil.md`, `casos_de_uso.md`, `analise_arquitetural_secretariaonline2.md`, `jpaInterfaces_PostgresEntities.md` e `Diagrama de Classes - Secretaria Online 2.md`. Este ficheiro permanece como **histórico da instrução**; os critérios da §5 foram verificados na pasta.

**Idioma de trabalho:** português do Brasil.
**Objetivo:** usar este documento como **instrução mestre** para revisar e atualizar os arquivos em `a_new_app_design/`, que hoje descrevem a **v4.0** centrada em **PIN fixo em duas etapas**, **janelas rígidas de 10 minutos** e **ausência de QR** na validação de presença do aluno. A **nova proposta** abaixo substitui/estende esse recorte: **múltiplos modos de confirmação**, **janelas de validação configuráveis pelo professor**, **CRUD de eventos compartilhado com visibilidade e permissões explícitas**, **ciclo de vida guiado por agenda (início/fim automáticos)**, **experiência do aluno unificada na tela de eventos** (tabela + modal + componente dinâmico de validação + fluxo de login), **estados de evento visíveis** e **horas formativas creditadas configuráveis**; além de **regras de permissão** para telas de eventos e de formativas.

---

## 1. Contexto

O projeto **SecretariaOnline2** prevê eventos acadêmicos que geram **horas formativas**, com registro auditável de presença. A documentação atual em `a_new_app_design/` (v4.0) converge para um protocolo único (**Proof of Stay** com PIN, duas fases, 10 minutos, *device binding*, sem QR de presença). A **nova versão da gestão de presença** redefine o produto para dar **flexibilidade ao professor** no método de confirmação e nas **janelas temporais** de validação, mantendo rastreabilidade e integração com formativas/certificados conforme a arquitetura geral já descrita nos outros módulos.

*(Os ficheiros indicados no **Status** no topo deste documento já incorporam a **v4.1**; os parágrafos acima descrevem o *delta* em relação ao desenho v4.0 legado, para leitores que ainda migram mentalmente do modelo antigo.)*

---

## 2. Regras de negócio e UX — nova proposta (fonte de verdade para atualizar os artefatos)

### 2.1 Modos de confirmação de presença (escolha do professor na criação/edição do evento)

O professor **configura um modo** (enum ou equivalente) entre, no mínimo:

| Modo | Comportamento esperado (nível produto) |
|------|----------------------------------------|
| **QR Code único** | Um único QR (ou payload equivalente) para validar presença no período permitido; fluxo web com componente de presença. |
| **QR Code duplo** | Dois QRs ou duas fases distintas (ex.: **início** e **fim**), alinhado a eventos longos ou conferência de permanência. |
| **Senha ou PIN único** | Um segredo informado pelo aluno (campo numérico ou alfanumérico conforme política) numa ou mais janelas definidas em §2.2. |
| **Senha ou PIN duplo** | Dois segredos distintos (entrada/saída ou início/fim), análogo conceitual ao “duas etapas” mas **sem impor** duração fixa de 10 minutos — a duração é a **janela** configurada. |

**Nota para documentação:** descrever como o backend expõe ações ao cliente (HATEOAS, *feature flags* por modo, contratos REST) sem acoplar a UI a perfis fixos; detalhar *payloads* e erros (`403` fora da janela, `409` conflitos, etc.) de forma **única** em `endpoints_canonicos_presenca_eventos_v4.md` (renomear ou versionar o arquivo se necessário, ex. `..._v4_1.md`).

### 2.2 Janelas de tempo para validação (preferência do professor)

- O professor define **quando** o aluno pode validar presença, **independentemente** da duração física do evento.
- Exemplos de política (todos devem constar como opções de configuração na especificação):
  - **Janela única longa** (ex.: dia inteiro ou faixa contínua alinhada ao evento).
  - **Duas janelas curtas** (ex.: uma no **início** da palestra e outra no **final**), ou outras combinações que o produto permitir documentar.
- As janelas são **dados do agregado evento** (metadados + eventual tabela filha `attendance_validation_window` ou similar); a documentação JPA/ER deve refletir isso.

### 2.3 Evento amarrado a agenda (cronograma)

- O evento **ocorre em *schedule***: possui **início e fim planejados** (data/hora).
- O sistema deve refletir **início e fim automáticos** conforme essas marcações (transição de estados, liberação do componente de validação, encerramento operacional).
- Harmonizar com a ideia de “evento em andamento” **por tempo** e não apenas por ação manual do professor, salvo exceções documentadas (ex.: encerramento antecipado, cancelamento).

### 2.4 Professor — telas de edição/gestão de eventos (CRUD + leitura de terceiros)

Nova/renovada **área de gestão de eventos** para quem tem permissão adequada (ver §2.7):

- **Criar, editar, remover e visualizar** eventos.
- Listagem com **filtro** para ver **somente os meus** ou **todos** (conforme política de visibilidade institucional).
- Eventos de **outros professores**: o usuário logado pode **apenas visualizar** (sem editar nem excluir).
- Os documentos de telas devem nomear rotas, capabilities e fontes de dados de forma consistente (evitar drift entre `telas.md` e `endpoints_*.md`).

### 2.5 Aluno — experiência na tela de eventos (único lugar principal)

- O aluno **prioritariamente** interage com **uma tela de eventos** (lista).
- **Tabela somente leitura** dos eventos registrados pelos professores (e demais criadores autorizados), com colunas acordadas (título, período, estado, horas formativas creditadas, organizador, etc.).
- **Modal** (ou painel lateral) com **nome, detalhes e informações** do evento.
- Quando o evento **entra em vigor** (segundo agenda + regras de estado), **abaixo da descrição** surge **dinamicamente** o **componente de validação de presença**, cujo conteúdo **depende do modo** escolhido pelo professor:
  - **QR Code:** redirecionar o aluno para o **fluxo web** na tela do evento com o componente de presença (deep link / rota canônica documentada).
  - **Não autenticado:** exibir **notificação** solicitando login, com **botão** que leva ao fluxo de **login**; após login bem-sucedido, **retorno automático** à **mesma tela/rota de evento** para concluir a validação.
- Manter princípios de **UI cega** onde fizer sentido (mensagens genéricas, sem vazar políticas internas), alinhado ao restante da arquitetura.

### 2.6 Estados do evento e horas formativas (visibilidade professor e aluno)

Em **telas de eventos** (professor e aluno), exibir opções/labels de estado coerentes com o domínio, no mínimo:

- **Agendado**
- **Em andamento**
- **Concluído**

Para o **aluno**, exibir também **horas formativas aceitas** / **creditadas** com a **quantidade** configurada pelo professor (o valor **pode divergir** da duração real do evento; ex.: evento de 2h15 com **3h** formativas creditadas).

### 2.7 Permissões (capabilities) e quem acessa o quê

| Recurso / tela | Regra |
|----------------|--------|
| **CRUD e listagens de gestão de eventos** (criação, edição, exclusão, filtros) | Exige **`event.manage`**. **Sem** essa *authority*, a entrada de menu/rota **não aparece**. |
| **Quem deve poder receber/usar `event.manage`** (conforme política a documentar na matriz FGAC) | **Todos os professores**, **secretaria** e **administradores** da plataforma (ajustar `analise_arquitetural_secretariaonline2.md` §6.1 e `telas.md` para não restringir o CRUD apenas à secretaria se a nova regra for global). |
| **Operação de presença no dia** (abrir janelas, exibir QR/PIN conforme modo, acompanhamento) | Separar na documentação **`event.host`** (anfitrião do evento) vs **`event.manage`** (gestão institucional), evitando ambiguidade com a v4.0 atual. |
| **Tela(s) de formativas no perfil professor** (revisão CAAF) | **Somente professores que integram a CAAF** devem acessar a funcionalidade de **revisão/gestão formativa** atribuída à comissão; professores **fora** da CAAF **não** veem essa tela (ajustar `telas.md` F3.5, fluxos **F3.5** e diagrama de casos de uso onde A4 aparece em UC-FOR-03). |

---

## 3. Instrução explícita para o agente ou revisor humano

1. **Ler** integralmente este prompt e, em seguida, **cada arquivo** listado na §4.  
2. **Atualizar** os documentos para remover contradições com a **§2** (substituir “PIN + 10 min + sem QR” como **único** desenho; passar a descrever **modos configuráveis**, **janelas configuráveis**, **QR quando aplicável**, **estados**, **horas creditadas**, **CRUD professor/secretaria/admin**, **regra CAAF para formativas**).  
3. **Preservar** o que permanece válido da arquitetura transversal: FGAC, HATEOAS onde couber, Outbox, auditoria, certificados, BFF, princípios de segurança — apenas **recalibrando** o subdomínio de presença/eventos.  
4. **Garantir rastreabilidade cruzada:** mesmos nomes de rotas, *query params*, *rels* e tabelas entre `telas.md`, `fluxos_por_perfil.md`, `endpoints_canonicos_*.md`, `casos_de_uso.md`, `jpaInterfaces_PostgresEntities.md`, `Diagrama de Classes - Secretaria Online 2.md` e `analise_arquitetural_secretariaonline2.md`.  
5. **Não criar** novos arquivos de documentação além dos necessários; se for inevitável versionar (ex. manter snapshot v4.0), renomear com sufixo claro (`_v4_0_historico`) e deixar o **canônico** atualizado.

---

## 4. Checklist de arquivos em `a_new_app_design/` a revisar

| Arquivo | Principais ajustes esperados |
|---------|------------------------------|
| `telas.md` | F1.17/F1.18 (aluno: lista + modal + componente dinâmico; login interrompido); rotas de professor para **CRUD de eventos** com `event.manage`; F3.2 vs nova tela de edição; F5.14/F5.15 coerentes com professor também CRUD; estados **Agendado / Em andamento / Concluído**; coluna horas formativas; remover ou condicionar texto “sem QR / só PIN / 10 min” como padrão único; **F3.5 formativas só CAAF**. |
| `fluxos_por_perfil.md` | F1.7, F3.2, F5.8: fluxos por modo (QR, PIN simples/duplo); janelas configuráveis; retorno pós-login; diagramas de estado do evento; operação automática por *schedule*. |
| `endpoints_canonicos_presenca_eventos_v4.md` | Novos ou alterados: confirmação por modo, geração/exibição de QR se existir recurso dedicado, janelas (não só `windows/entry` com corpo vazio e 10 min fixos), `GET session` com `_links` por modo; alinhar paths com `telas.md`; decidir versionamento do nome do arquivo. |
| `casos_de_uso.md` | UC-PRE-01 a UC-PRE-04: atores (A4 com CRUD onde couber), novos fluxos/extends por modo; UC-FOR-03: restringir ator professor genérico onde a regra for só CAAF; diagrama Mermaid `M_PRES`. |
| `analise_arquitetural_secretariaonline2.md` | §10 presença, ER `event_attendance` / sessões: campos de modo, janelas, horas creditadas, *schedule*; premissas “sem QR” se deixarem de valer; versão do documento. |
| `jpaInterfaces_PostgresEntities.md` | Modo de validação, janelas (`validation_windows` JSONB ou tabela filha `attendance_validation_window`), repositórios `event_attendance` / `attendance_session` / `certificate`; esqueleto Kotlin alinhado. |
| `Diagrama de Classes - Secretaria Online 2.md` | `EventAttendance` (modo, janelas, estados), `AttendanceSession` (vínculo aluno–evento, `deviceUuid` quando aplicável), `Certificate`; sem *trust score* / geofence como núcleo. |

---

## 5. Critérios de aceite da documentação alinhada

- [x] Não existe afirmação canônica de que **apenas** PIN em duas janelas de **10 minutos** é o modelo, **sem** qualificar que isso pode ser **um** dos modos ou política legada.  
- [x] Estão descritos os **quatro modos** da §2.1 (ou lista equivalente atualizada por decisão de produto).  
- [x] Estão descritas **janelas configuráveis** (ex.: dia inteiro vs duas janelas).  
- [x] **Professor** tem fluxo documentado de **CRUD + visualização read-only de eventos alheios + filtro “somente os meus”**.  
- [x] **Aluno** tem **uma** experiência principal de **lista + detalhe + validação contextual + pós-login**.  
- [x] **Estados** Agendado / Em andamento / Concluído e **horas formativas creditadas** aparecem nas especificações de UI e de API/contrato.  
- [x] **`event.manage`** controla visibilidade do menu/rotas de gestão; professores, secretaria e admin documentados como elegíveis.  
- [x] **Formativas (revisão CAAF)** documentada como **acessível somente a professores no CAAF**, não a todo professor.  
- [x] Referências cruzadas entre os sete arquivos estão **consistentes** após a edição.

---

## 6. Observação final

Esta revisão é **incremental** sobre a base v4.0: reaproveita identidade, comunicação, solicitações, certificados e demais bounded contexts, mas **redefine o núcleo** do módulo **Presença em eventos formativos**. Qualquer implementação futura (Spring, OpenAPI, front) deve tratar a documentação **pós-alinhamento** como contrato de produto.

---

*Documento gerado para orientar atualização coerente da pasta `a_new_app_design/`.*
