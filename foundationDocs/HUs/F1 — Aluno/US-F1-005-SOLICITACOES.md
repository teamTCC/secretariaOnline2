# US-F1-005 — Abrir, Listar e Acompanhar Solicitações

| Campo | Valor |
|-------|-------|
| **ID** | US-F1-005 |
| **Épico** | ALUNO-SOLICITACOES |
| **Telas** | F1.7 `/solicitacoes` · F1.8 `/solicitacoes/nova` · F1.9 `/solicitacoes/:id` |
| **Prioridade** | P1 (wizard F1.8) / P2 (lista F1.7, detalhe F1.9) |
| **Plataforma** | Web + Mobile |
| **Capability** | `request.view_own`, `request.open` |
| **API primária** | `GET /requests?solicitante=me`, `GET /request-types/{code}`, `POST /requests`, `GET /requests/{id}` |
| **Frames Figma** | **F1.7:** [Loaded/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=60-1452) · [Empty/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=60-1677) · [Skeleton/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=109-7228) · [Error/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=109-7832) · [Loaded/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=106-6352) · [Empty/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=112-10155) · **F1.8:** [Passo 1/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=76-1677) · [Passo 2/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=77-1814) · [Passo 3/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=78-1964) · [Validação erro/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=79-2086) · [Passo 1 Loading/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=147-14040) · [Passo 1/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=86-2403) · [Passo 2/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=86-2542) · [Passo 3/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=86-2719) · [Validação erro/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=86-2857) · **F1.9:** [Default/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=80-2284) · [Skeleton/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=143-13445) · [404/Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=143-15338) · [Default/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=106-6697) · [Skeleton/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=149-30478) · [404/Mobile](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=149-32960) |
| **Specs de tela** | `telasFigma/telas1/F1.7-solicitacoes-lista.md` · `F1.8-solicitacoes-nova.md` · `F1.9-solicitacoes-detalhe.md` |

---

## Histórias

---

### HU-A — Listar minhas solicitações (F1.7)

> **Como** aluno autenticado,  
> **Quero** ver todas as minhas solicitações com filtros por estado, tipo e ano,  
> **Para** acompanhar o andamento de cada pedido e identificar quais precisam de ação minha.

### HU-B — Abrir nova solicitação via wizard dinâmico (F1.8)

> **Como** aluno autenticado,  
> **Quero** abrir qualquer tipo de solicitação seguindo um wizard de 3 passos com formulário gerado dinamicamente a partir do tipo escolhido,  
> **Para** formalizar pedidos à secretaria ou colegiado de forma padronizada, com anexos e sem conhecer a burocracia interna do fluxo.

### HU-C — Visualizar detalhe e acompanhar timeline (F1.9)

> **Como** aluno autenticado,  
> **Quero** ver os detalhes completos de uma solicitação, sua linha do tempo de eventos e as ações disponíveis,  
> **Para** entender exatamente onde está meu pedido e tomar as ações necessárias (corrigir, gerar protocolo).

---

## 2. Regras de Negócio

### Lista de solicitações (F1.7)

| ID | Regra |
|----|-------|
| **RN-F1.7-01** | A lista é paginada (20 itens por página). Os filtros disponíveis são: estado, tipo, ano e busca textual. Todos os filtros são aplicados no backend. |
| **RN-F1.7-02** | Solicitações com `prazo_em < now` exibem a data do prazo em `status/danger` para indicar SLA vencido. |
| **RN-F1.7-03** | Em mobile, a tabela é substituída por cards empilhados e os filtros ficam em um Sheet/drawer. |

### Wizard de nova solicitação (F1.8)

| ID | Regra |
|----|-------|
| **RN-F1.8-01** | O wizard tem **3 passos**: (1) Escolha do tipo, (2) Formulário dinâmico + anexos, (3) Revisão e confirmação. |
| **RN-F1.8-02** | Os tipos de solicitação disponíveis no Passo 1 são filtrados pelo backend com base no curso do aluno, período e `request_type.prerequisitos`. O aluno não vê tipos para os quais não tem elegibilidade. |
| **RN-F1.8-03** | O Passo 2 renderiza o formulário a partir do `form_schema` JSON do `RequestType` selecionado. A validação é feita via **Zod** (frontend) e re-validada no backend ao submeter. Campos condicionais aparecem/somem com animação. |
| **RN-F1.8-04** | Upload de anexos: drag-and-drop ou botão. Validação de tipo (PDF, JPEG, PNG) e tamanho (máx 10 MB por arquivo) no cliente. Upload assíncrono para MinIO via URL pré-assinada. SHA-256 calculado no browser antes do upload e enviado no POST para integridade. |
| **RN-F1.8-05** | O rascunho é salvo localmente (PWA/AsyncStorage) e também no backend como `estado=RASCUNHO` via `POST /requests/draft`. Aparece na lista de solicitações com badge "Rascunho". |
| **RN-F1.8-06** | Ao confirmar no Passo 3, o backend: cria a solicitação no estado inicial do workflow, calcula `prazo_em = now + request_type.prazo_dias`, gera `numero_anual` atômico (`YYYY-NNNN`), enfileira `solicitacoes.opened` no Outbox. |
| **RN-F1.8-07** | Após criação, o aluno é redirecionado para `/solicitacoes/:id` e recebe notificação in-app + push: "Sua solicitação foi aberta com nº YYYY-NNNN." |

### Detalhe e ações HATEOAS (F1.9)

| ID | Regra |
|----|-------|
| **RN-F1.9-01** | Todos os botões da ActionBar são derivados **exclusivamente** de `_links` na resposta de `GET /requests/{id}`. `_links` é `Map<String,String>` (rel → URL). A UI não usa HAL `{ href }` nem `EntityModel`. |
| **RN-F1.9-01a** | GET detalhe carrega `formSchema` da **versão da instância** (`idRequestTypeVersion` / `request.id_request_type_version`, snapshot `V019`). Publicar tipo novo não altera formulário de solicitações já abertas. |
| **RN-F1.9-02** | Estado `EM_AJUSTE`: o link `editar` reabre o wizard no Passo 2 pré-preenchido. |
| **RN-F1.9-03** | Estado `DELIBERADA`: o link `gerar-protocolo` dispara `POST /requests/{id}/protocol` e gera PDF com QR para `/publico/verificar-protocolo/:id`. |
| **RN-F1.9-04** | A timeline de eventos (`request_event`) é exibida em **ordem reversa** (mais recente no topo) com tipo, data/hora e texto do evento. |
| **RN-F1.9-05** | Anexos são listados com nome, tipo e botão de download. O download usa URL pré-assinada do MinIO com expiração curta (15 min). |

---

## 3. Critérios de Aceitação

### CA-01 — Listar solicitações com filtros

```gherkin
Dado que o aluno está em /solicitacoes
Quando a página carrega
Então exibe tabela com colunas: Número, Tipo, Estado (DS/Badge), Prazo, SLA
  E solicitações com prazo vencido têm a data em status/danger
  E o botão "Nova solicitação" aparece apenas se _links.novaSolicitacao existir

Quando aplica filtro estado = "EM_ANALISE"
Então realiza GET /requests?solicitante=me&estado=EM_ANALISE
  E exibe apenas as solicitações filtradas
```

### CA-02 — Passo 1 do wizard: escolha do tipo

```gherkin
Dado que o aluno está em /solicitacoes/nova
Quando o wizard carrega
Então exibe grid de cards com os tipos de solicitação elegíveis para o aluno
  E tipos para os quais o aluno não tem elegibilidade NÃO aparecem na lista
  E ao selecionar um tipo avança para o Passo 2
```

### CA-03 — Passo 2 do wizard: formulário dinâmico

```gherkin
Dado que o aluno selecionou um tipo de solicitação com form_schema
Quando o Passo 2 carrega
Então renderiza os campos definidos no form_schema (texto, select, data, multi-item)
  E campos condicionais aparecem/somem conforme valores preenchidos
  E a zona de upload de anexos está disponível
  E a validação Zod ocorre inline ao perder foco de cada campo
```

### CA-04 — Upload de anexo no wizard

```gherkin
Dado que o aluno está no Passo 2 do wizard
Quando arrasta um arquivo PDF de 5 MB para a zona de upload
Então o cliente calcula o SHA-256 do arquivo localmente
  E inicia o upload para MinIO via URL pré-assinada
  E exibe barra de progresso durante o upload
  E ao concluir exibe o arquivo na lista com nome e botão de remover

Quando tenta fazer upload de arquivo acima de 10 MB
Então exibe erro: "O arquivo excede o tamanho máximo de 10 MB."
```

### CA-05 — Passo 3: revisão e confirmação

```gherkin
Dado que o aluno completou os campos e anexos no Passo 2
Quando avança para o Passo 3
Então exibe resumo legível dos dados preenchidos (sem campos técnicos)
  E lista os anexos com pré-visualização ou ícone de tipo
  E botão "Confirmar" destravado
  E ao clicar em "Confirmar" realiza POST /requests e redireciona para /solicitacoes/:id
```

### CA-06 — Rascunho salvo automaticamente

```gherkin
Dado que o aluno começou a preencher o Passo 2 do wizard
Quando fecha o browser acidentalmente ou sai da tela
Então ao retornar para /solicitacoes/nova o sistema detecta o rascunho salvo localmente
  E oferece opção de "Continuar rascunho" ou "Começar novo"
```

### CA-07 — Detalhe com timeline e ações HATEOAS

```gherkin
Dado que o aluno acessa /solicitacoes/:id
Quando a página carrega com estado EM_AJUSTE
Então exibe ActionBar com botão "Editar" (derivado de _links.editar)
  E a timeline exibe todos os request_events em ordem reversa
  E ao clicar em "Editar" reabre o wizard no Passo 2 com dados pré-preenchidos

Quando o estado é DELIBERADA e _links.gerar-protocolo existe
Então exibe botão "Gerar protocolo" na ActionBar
```

---

## 4. Componentes de UI (Design System)

| Componente | Tela | Uso |
|------------|------|-----|
| `DS/DataTable` | F1.7 | Lista de solicitações |
| `DS/WizardStepper` | F1.8 | Progresso dos 3 passos |
| `DS/DynamicForm` | F1.8 | Formulário gerado por JSON Schema |
| `DS/AttachmentUpload` | F1.8 | Upload de comprovantes |
| `DS/Badge` | F1.7, F1.9 | Estado da solicitação |
| `DS/TimelineItem` | F1.9 | Eventos da solicitação |
| `DS/AttachmentList` | F1.9 | Lista de anexos com download |
| `DS/EmptyState` | F1.7 | Sem solicitações |
| `DS/Skeleton` | todas | Loading |

---

## 5. Contrato de API (detalhe)

```http
GET /requests/{id}
Cookie: access_token={jwt}
```

```json
{
  "id": "...",
  "protocolo": "2026-0042",
  "tipoCode": "ADIANTAMENTO_PERIODO",
  "estado": "EM_AJUSTE",
  "dados": {},
  "formSchema": {},
  "idRequestTypeVersion": "...",
  "_links": {
    "self": "/requests/{id}",
    "events": "/requests/{id}/events",
    "attachments": "/requests/{id}/attachments",
    "update-draft": "/requests/{id}/draft"
  }
}
```

`_links` = objeto de strings (não `{ rel: { href } }`). `formSchema` vem do snapshot da versão da instância (V019).

## 6. Fora de escopo

- Solicitação de revisão de decisão após indeferimento — prevista se `workflow_json` do tipo permitir, implementada junto com o tipo específico
- Cancelamento de solicitação pelo aluno — depende do workflow do tipo

---

## 7. Definição de Pronto (DoD)

- [ ] Frames Figma aprovados: Lista (loaded + empty), Wizard (3 passos), Detalhe
- [ ] DynamicForm renderiza corretamente a partir de form_schema de ao menos 3 tipos diferentes
- [ ] SHA-256 de anexo calculado no browser e validado no backend
- [ ] Rascunho local funcional via PWA storage
- [ ] Todas as ações em F1.9 derivadas exclusivamente de `_links` (strings, não HAL href)
- [ ] Detalhe usa `formSchema` da versão stampada (`idRequestTypeVersion`)
- [ ] Geração de protocolo PDF com QR funcional

---

## 8. Referências

| Recurso | Link / Caminho |
|---------|---------------|
| Specs de tela | `telasFigma/telas1/F1.7-*.md`, `F1.8-*.md`, `F1.9-*.md` |
| Fluxo F1.2, F1.3 | `foundationDocs/analysis/fluxos_por_perfil.md` §2 F1.2, F1.3 |
| As-built backend | `foundationDocs/analysis/as-built-backend.md` |
| Módulo workflow | `agents/workflow-engine-specialist.md` |
| Página Figma F1 | [Telas / F1 — Aluno](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=48-339) |
| Frame F1.7 principal | [F1.7 — Solicitações / Loaded / Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=60-1452) |
| Frame F1.8 principal | [F1.8 — Nova solicitação / Passo 1 / Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=76-1677) |
| Frame F1.9 principal | [F1.9 — Solicitação detalhe / Default / Desktop](https://www.figma.com/design/y1ZC44ThrXH0CIpEWZITh6/secretariaOnline2?node-id=80-2284) |
