# Fluxo informacional: onde cada peça entra numa requisição

Este texto explica, no SecretariaOnline2, o papel de **controller**, **BFF**, **service**, **use case**, **query**, **port**, **adapter**, **DTO**, **entidade JPA** e **repositório JPA** — e como eles se encadeiam do HTTP até o Postgres (e de volta).

Não é um tutorial de Spring. É um mapa mental para ler o código.

---

## 1. Ideia central

Uma requisição HTTP atravessa **camadas**. Cada camada traduz a informação para um vocabulário diferente:

```
JSON da tela  →  DTO HTTP  →  Command / Query  →  domínio / persistência  →  tabela
tabela        →  Entity    →  Query / UseCase  →  DTO HTTP               →  JSON da tela
```

A regra: **quem fala HTTP não fala SQL**. Quem fala SQL não decide o JSON da tela.

O projeto é um **monólito modular**. Tudo roda no mesmo processo Spring Boot. “Módulo” (iam, solicitacoes, bff…) é **fronteira de código**, não um microserviço.

---

## 2. Mapa das camadas (onde mora cada coisa)

```
┌─────────────────────────────────────────────────────────────────┐
│  FRONTEND (React)                                               │
│  chama HTTP; lê _links para saber quais botões mostrar          │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP JSON
┌───────────────────────────────▼─────────────────────────────────┐
│  api/          Controller + DTO                                 │
│                (HTTP, auth, validação de entrada)               │
├─────────────────────────────────────────────────────────────────┤
│  application/  UseCase (escrita)  ·  Query (leitura)            │
│                ports/out (contratos para o mundo externo)       │
├─────────────────────────────────────────────────────────────────┤
│  domain/       regras puras (WorkflowEngine, Request, …)        │
│                sem Spring, sem JPA, sem HTTP                    │
├─────────────────────────────────────────────────────────────────┤
│  infrastructure/                                                │
│    persistence/  Entity + JpaRepository + Adapter               │
│    adapters/     MinIO, JWT, e-mail, etc.                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    PostgreSQL / Redis / MinIO
```

O **BFF** (`modules/bff/`) é um módulo especial: só **agrega leituras** de outros módulos. Não é dono de tabela.

---

## 3. Glossário: papel, diferença, exemplo no repo

### 3.1 Controller

**O que é.** Classe `@RestController` que expõe rotas HTTP. Fina: recebe JSON, chama um UseCase ou Query, devolve JSON.

**O que faz numa transação.**
- Mapeia URL + método (`POST /requests`, `GET /requests/{id}`).
- Lê o usuário autenticado (`currentUser()`).
- Aplica `@PreAuthorize` (FGAC).
- Valida o DTO de entrada (`@Valid`).
- **Não** acessa banco. ArchUnit impede import de `infrastructure.persistence`.

**O que não é.** Não orquestra regra de negócio. Não monta SQL.

**Exemplo.** `RequestController` (`modules/solicitacoes/api/`). POST chama `OpenRequestUseCase`; GET chama `RequestQuery`.

---

### 3.2 BFF (Backend for Frontend)

**O que é.** Um **tipo de controller + query**, no módulo `bff`, feito para **uma tela** (dashboard, relatório, busca). Uma chamada HTTP junta dados de vários bounded contexts.

**Diferença para um controller de domínio.**

| Controller de domínio | Controller BFF |
|-----------------------|----------------|
| Dono de um recurso (`/requests/{id}`) | Dono de uma **visão de tela** (`/bff/dashboard/aluno`) |
| Create / update / transition | Quase só GET |
| Fala com o próprio módulo | Fala com **ports** de vários módulos |
| Ex.: `RequestController` | Ex.: `DashboardAlunoController`, `ReportsController` |

**Por que existe.** Sem BFF, o React faria 6–8 GETs para montar o dashboard (solicitações + formativas + eventos + IAM). Com BFF, faz **um**.

**Regra dura.** BFF **não** injeta `*JpaRepository`. Só ports. ArchUnit: `bffMustNotDependOnJpa`.

---

### 3.3 DTO (Data Transfer Object)

**O que é.** `data class` que descreve o **contrato JSON** da API. Entrada (`*Request` / `*Dto`) e saída (`*Response`).

**O que faz.**
- Entrada: o que o cliente **pode enviar** (`OpenRequestDto` com `@Valid`).
- Saída: o que o cliente **recebe**, inclusive `_links` HATEOAS (`RequestDetailResponse`).

**O que não é.**
- Não é tabela (`RequestEntity`).
- Não é domínio (`Request`).
- Não é o Command interno do use case (`OpenRequestCommand`).

**Por que separar DTO e Command.** O JSON pode mudar (renomear campo, adicionar `_links`) sem mudar a lógica. O Command é o vocabulário da aplicação, estável.

Fluxo típico de entrada:

```
JSON  →  OpenRequestDto  →  OpenRequestCommand  →  UseCase
```

---

### 3.4 UseCase (escrita / comando)

**O que é.** Uma classe em `application/` com **uma intenção de negócio** que **muda estado**: abrir solicitação, deliberar, confirmar colação.

Padrão: um arquivo, um `execute(command)`.

**O que faz numa transação.**
- Recebe um **Command** (não o DTO HTTP).
- Valida regras (schema, anexos, workflow).
- Persiste (via JPA do próprio módulo ou via port).
- Enfileira outbox (`OutboxEventPublisher`) na **mesma** `@Transactional`.
- Devolve um resultado simples (`UUID`, `ConfirmGraduationResult`) — **não** devolve `*Entity` para o controller.

**Exemplo.** `OpenRequestUseCase.execute(OpenRequestCommand): UUID`

**Anotação `@Service`.** Use cases usam `@Service` do Spring só para virar bean injetável. Isso **não** os torna “a camada Service” clássica (ver §3.5).

---

### 3.5 Service (`*Service`)

Há **duas** coisas com esse nome. Não misturar.

| | `@Service` (anotação Spring) | Classe `*Service` |
|--|------------------------------|-------------------|
| O que é | “esta classe é um bean” | Helper de domínio/aplicação **reutilizado** por um ou mais use cases |
| Quem tem | UseCase, Query, adapters… | `GraduationEligibilityService`, `DiplomaPdfService`, `JwtTokenService`, `MinioStorageService` |
| Papel | Infraestrutura do framework | Lógica compartilhada que **não** é um caso de uso isolado |

**UseCase vs Service no nosso código.**

- `ConfirmGraduationUseCase` = a transação “confirmar colação”.
- `GraduationEligibilityService` = calcula se o aluno **pode** colar grau; o use case **chama** o service, depois grava.

Se a operação é “o usuário pediu X e o sistema faz X”, é **UseCase**.  
Se é “um cálculo/utilitário usado por mais de um fluxo”, tende a ser **Service**.

Não existe uma pasta `service/` obrigatória. Os `*Service` ficam em `application/` ou `infrastructure/services/` conforme o caso.

---

### 3.6 Query (leitura)

**O que é.** Classe em `application/` que **só lê**. Lista, detalhe, dashboard, relatório. Não grava, não dispara outbox.

**Par com UseCase (CQRS leve).**

| Query | UseCase |
|-------|---------|
| GET | POST / PATCH / DELETE |
| Monta DTO + `_links` | Muda estado |
| Pode agregar vários ports (BFF) | Orquestra persistência + eventos |
| `RequestQuery.getById()` | `OpenRequestUseCase.execute()` |

**Por que não juntar tudo num “RequestService”.** Um serviço único mistura leitura e escrita, incha e vira God class. Query e UseCase deixam o controller escolher o caminho certo.

**Exemplo.** `RequestQuery` (módulo dono) vs `ReportsQuery` / `DashboardAlunoQuery` (BFF, só ports).

---

### 3.7 Port

**O que é.** `interface` em `application/ports/out/` que diz **o que** a aplicação precisa do mundo externo, sem dizer **como**.

“Preciso contar solicitações por estado.”  
Não: “chame `RequestJpaRepository.countByEstado`.”

**Dois sentidos clássicos (Ports & Adapters):**

| Tipo | Direção | No TCC |
|------|---------|--------|
| **port out** | aplicação → persistência / e-mail / JWT / MinIO | O que vocês usam: `SolicitacaoBffReadPort`, `OutboxEventPublisher`, `TokenServicePort` |
| **port in** | quem dispara o use case (interface do caso de uso) | Quase não existe pasta `ports/in/` — o controller chama a classe UseCase direto |

**Quando é obrigatório.**
- BFF lendo outro módulo.
- Use case de um módulo precisando de outro (outbox, token).

**Quando o TCC ainda admite atalho.** Use case **do mesmo módulo** injetando `*JpaRepository` direto (`OpenRequestUseCase` → `RequestJpaRepository`). O acoplamento fica **dentro** do bounded context.

---

### 3.8 Adapter

**O que é.** Classe em `infrastructure/` que **implementa** um port. É a tradução: contrato da aplicação → JPA, MinIO, jjwt, etc.

```
Query/UseCase  →  Port (interface)  →  Adapter  →  JpaRepository / MinioClient
```

**Exemplo.** `SolicitacaoDashboardAdapter` implementa `SolicitacaoBffReadPort` e `SolicitacaoDashboardPort`. Por baixo, chama `RequestJpaRepository`.

O BFF nunca vê o adapter. O Spring injeta a interface; o adapter é o bean concreto.

**Analogia.** Port = tomada. Adapter = o que está atrás da parede (Postgres hoje, outra fonte amanhã).

---

### 3.9 Entidade JPA (`*Entity`)

**O que é.** Classe `@Entity` mapeada a uma **tabela**. Campos = colunas. Vive em `infrastructure/persistence/`.

**Exemplo.** `RequestEntity` ↔ tabela `request`.

**O que não é.**
- Não é o JSON da API (`RequestDetailResponse`).
- Não é o modelo de domínio puro (`domain/Request.kt`) — embora hoje muitos use cases trabalhem direto na Entity (atalho de TCC).

**Regra.** Entity **não sai** do módulo. Controller não recebe `RequestEntity`. Use case devolve `UUID` / DTO; Query monta o response.

---

### 3.10 Repositório JPA (`*JpaRepository`)

**O que é.** Interface Spring Data (`JpaRepository<RequestEntity, UUID>`) com `findBy…`, `@Query` JPQL. É o **acesso SQL/JPQL** àquela tabela.

**O que faz.** `save`, `findById`, `findWithFilters`, `countByEstado`. Hibernate gera o SQL.

**Quem pode chamar.**
- Adapter do próprio módulo (preferido para leitura cruzada).
- UseCase / Query **do mesmo módulo** (atalho aceito).
- **Nunca** Controller.
- **Nunca** BFF.

---

### 3.11 (Bônus) Domain e Assembler

Não estavam na lista, mas aparecem no mesmo fluxo.

**Domain** (`domain/`): regras sem framework — `WorkflowEngine`, `FormSchemaValidator`, `Request`. UseCase/Query **chamam** o domínio; o domínio não conhece HTTP nem JPA.

**Assembler:** classe que só monta DTO + `_links`. Prevista em `api/assembler/`, **ainda não existe** como pasta. Hoje essa lógica está dentro das Queries (`RequestQuery` monta o mapa `links`). Comportamento HATEOAS existe; extração em assembler é opcional para o TCC.

---

## 4. Duas transações ponta a ponta

### 4.1 Escrita — `POST /requests` (abrir solicitação)

Uma **transação** aqui = um `@Transactional` no use case: ou grava solicitação + anexos + outbox juntos, ou nada.

```
Aluno (React)
    │  POST /requests   { idRequestType, idCurso, dados, attachments }
    ▼
RequestController.open()
    │  valida OpenRequestDto (@Valid)
    │  lê currentUser()
    │  monta OpenRequestCommand
    ▼
OpenRequestUseCase.execute(command)          ← @Transactional
    │  RequestTypeJpaRepository.findById     ← repositório
    │  FormSchemaValidator.validate          ← domain
    │  AttachmentPolicy.assert…              ← domain
    │  new RequestEntity(…)                  ← entidade JPA
    │  RequestJpaRepository.save(entity)     ← SQL INSERT
    │  OutboxEventPublisher.enqueue(…)       ← port (outro módulo)
    │  return UUID
    ▼
Controller monta RequestCreatedResponse
    │  { id, _links: { self: "/requests/{id}" } }
    ▼
JSON 201 → React
```

**Quem entra / quem não entra**

| Peça | Nesta transação |
|------|-----------------|
| Controller | Sim — HTTP |
| DTO | Sim — `OpenRequestDto` in, `RequestCreatedResponse` out |
| UseCase | Sim — o coração da transação |
| Query | Não |
| BFF | Não |
| Port | Sim — outbox (`OutboxEventPublisher`) |
| Adapter | Impl. do outbox (módulo notificações) |
| Entity + JpaRepository | Sim — persistência da solicitação |
| Service `*Service` | Não neste fluxo (em colação, sim: eligibility) |

---

### 4.2 Leitura de recurso — `GET /requests/{id}`

Não precisa de transação de escrita. É Query.

```
React  GET /requests/{id}
    ▼
RequestController.getById()
    ▼
RequestQuery.getById(id, user)
    │  RequestJpaRepository.findById
    │  checa dono vs authorities
    │  WorkflowEngine.allowedTransitions    ← domain
    │  monta RequestDetailResponse + _links
    ▼
JSON 200 → React  (botões = chaves de _links)
```

UseCase não participa. Port/Adapter não são necessários: Query e repositório são **do mesmo módulo**.

---

### 4.3 Leitura agregada — `GET /bff/dashboard/aluno`

Aqui o **port/adapter** entram de verdade, porque o BFF não pode ver JPA de solicitacoes/iam/presenca.

```
React  GET /bff/dashboard/aluno
    ▼
DashboardAlunoController          ← BFF controller
    ▼
DashboardAlunoQuery.execute()     ← BFF query
    │
    ├─ SolicitacaoDashboardPort.findPendenciasAluno()
    │       ▼
    │   SolicitacaoDashboardAdapter   ← adapter no módulo solicitacoes
    │       ▼
    │   RequestJpaRepository          ← JPA só neste módulo
    │
    ├─ FormativaDashboardPort …
    ├─ PresencaDashboardPort …
    └─ IamDashboardPort …
    ▼
DashboardAlunoResponse (DTO do BFF) → JSON
```

**Quem entra / quem não entra**

| Peça | Nesta transação |
|------|-----------------|
| BFF Controller + BFF Query | Sim |
| Ports de vários módulos | Sim |
| Adapters + JPA **dentro de cada módulo dono** | Sim |
| UseCase | Não (nada muda) |
| `RequestController` | Não — o BFF não chama o outro controller; chama o port |

Importante: BFF **não** faz HTTP interno para `RequestController`. No monólito, injeta o port e o Spring resolve o adapter.

---

## 5. Transformação da informação (o “formato” muda)

Seguindo **um** objeto “solicitação” ao longo do caminho:

```
Tela
  { tipo, curso, campos do wizard }

DTO de entrada (OpenRequestDto)
  contrato HTTP, validação Jakarta

Command (OpenRequestCommand)
  vocabulário do use case (ids, maps, attachments)

Entidade (RequestEntity)
  colunas: estado, dados JSONB, id_solicitante, …

Tabela request (Postgres)
  linha persistida

────────── na leitura ──────────

Entity (RequestEntity)
  lida pelo JpaRepository

Query monta DTO de saída (RequestDetailResponse)
  protocolo, formSchema, _links

JSON
  o que o useActions() do frontend consome
```

Se pular uma tradução (devolver Entity no controller, ou mandar DTO para o repositório), as camadas colapsam e o ArchUnit / o TCC quebram o desenho.

---

## 6. Relação resumida (quem chama quem)

```
Controller ──► UseCase ──► (mesmo módulo) JpaRepository ──► Entity ──► tabela
     │              └──► Port out ──► Adapter ──► JpaRepository / MinIO / Redis
     │
     └──► Query ──► (mesmo módulo) JpaRepository
              └──► Port out ──► Adapter     ← caminho do BFF

UseCase ──► *Service (helper) ──► JpaRepository / EntityManager
UseCase ──► domain (WorkflowEngine, validators)
```

**Setas que são proibidas (hoje, por ArchUnit + convenção):**

- Controller → Entity / JpaRepository
- BFF → Entity / JpaRepository de outro módulo
- domain → Spring / JPA / HTTP

---

## 7. Tabela rápida de diferenças

| Peça | Camada | Muda estado? | Fala HTTP? | Fala SQL? | Exemplo |
|------|--------|--------------|------------|-----------|---------|
| Controller | api | não | sim | não | `RequestController` |
| BFF | bff | não | sim (visão de tela) | não | `ReportsController` |
| DTO | api/dto | não | é o JSON | não | `OpenRequestDto` |
| UseCase | application | **sim** | não | às vezes (atalho) | `OpenRequestUseCase` |
| Query | application | não | não (monta DTO) | às vezes | `RequestQuery` |
| `*Service` | application / infra | depende | não | às vezes | `GraduationEligibilityService` |
| Port | application/ports/out | não | não | não | `SolicitacaoBffReadPort` |
| Adapter | infrastructure | não (executa o acesso) | não | sim, via repo | `SolicitacaoDashboardAdapter` |
| Entity | infrastructure | é o registro | não | mapeia tabela | `RequestEntity` |
| JpaRepository | infrastructure | persistência | não | **sim** | `RequestJpaRepository` |

---

## 8. Como escolher o que criar (quando for implementar)

1. **Nova rota GET de um recurso que o módulo já dono?**  
   Controller fino + `*Query`. Sem port se for o próprio módulo.

2. **Nova rota que muda algo?**  
   Controller + `*UseCase` + Command. Persistência no próprio JPA (TCC) ou via port.

3. **Nova tela que junta 3+ módulos?**  
   Controller no **BFF** + `*Query` no BFF + **port + adapter** em cada módulo dono.

4. **Cálculo usado por dois use cases?**  
   Extrair `*Service` (helper), não um terceiro use case.

5. **Precisa de arquivo (PDF)?**  
   UseCase/Query chama `MinioStorageService` (já é um adapter de storage, mesmo sem interface `Port` no nome).

---

## 9. Atalhos conscientes deste TCC (para não se confundir com o template ideal)

O `agents/backend-architect.md` descreve o desenho **completo** (ports/in, assembler HATEOAS, use case nunca vendo JPA). O código real está **parcialmente** lá:

| Template | Situação atual |
|----------|----------------|
| `ports/in` para cada use case | Controller chama a classe UseCase direto |
| Use case só fala com ports | Use case do mesmo módulo injeta `*JpaRepository` |
| Pasta `assembler/` | `_links` montados na Query / no controller |
| Entity nunca no application | Application ainda instancia `RequestEntity` no use case |

O que **já está** alinhado e vale internamente:

- Controller sem JPA
- BFF sem JPA, só ports
- GET em Query, escrita em UseCase
- Domain sem Spring/JPA (ArchUnit)
- Entity não retornada ao controller

Esses atalhos são aceitáveis no TCC; o importante é não misturar de novo HTTP com persistência.

---

## 10. Uma frase para cada peça

| Peça | Frase |
|------|--------|
| **Controller** | Porta HTTP: traduz request/response, nada mais. |
| **BFF** | Controller+Query de **tela**, agrega vários módulos. |
| **DTO** | Formato JSON público. |
| **UseCase** | “Faça esta ação de negócio” (transação de escrita). |
| **Query** | “Traga estes dados já no formato da API”. |
| **Service** | Helper reutilizável; não confundir com a anotação `@Service`. |
| **Port** | Contrato: o que preciso, sem o como. |
| **Adapter** | Implementação do contrato (JPA, MinIO, JWT…). |
| **Entity** | Linha da tabela, vocabulário do Hibernate. |
| **JpaRepository** | Como ler/gravar essa tabela. |

Se lembrar só disto: **HTTP → Controller → UseCase ou Query → (Port → Adapter) → Repository → Entity → Postgres**, e o caminho inverso na response com DTO — o resto é detalhe de pasta e de atalho de TCC.
