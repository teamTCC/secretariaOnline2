# T-F1-005 — Solicitações Acadêmicas (Motor de Workflow)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-005-SOLICITACOES.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-005-SOLICITACOES.md)  
> **Status:** ✅ Implementado — Motor de workflow genérico, HATEOAS transitions, paginação

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller principal | [`solicitacoes/api/RequestController.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/RequestController.kt) — GET → `RequestQuery`; POST/PATCH → `*UseCase` |
| Query — list/detalhe/events/types | [`solicitacoes/application/RequestQuery.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/RequestQuery.kt) |
| Query — catálogo admin | [`solicitacoes/application/RequestTypeQuery.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/RequestTypeQuery.kt) |
| Controller de anexos | [`solicitacoes/api/RequestAttachmentController.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/RequestAttachmentController.kt) |
| Use Case — Abrir (com anexos) | [`solicitacoes/application/OpenRequestUseCase.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/OpenRequestUseCase.kt) |
| Use Case — Salvar Rascunho (NOVO) | [`solicitacoes/application/SaveDraftUseCase.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/SaveDraftUseCase.kt) |
| Use Case — Submeter Rascunho (NOVO) | [`solicitacoes/application/SubmitDraftUseCase.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/SubmitDraftUseCase.kt) |
| Use Case — Transicionar | [`solicitacoes/application/TransitionRequestUseCase.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/TransitionRequestUseCase.kt) |
| Motor de Workflow | [`solicitacoes/domain/WorkflowEngine.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/domain/WorkflowEngine.kt) |
| Entidade de domínio | [`solicitacoes/domain/Request.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/domain/Request.kt) |
| Definição de workflow | [`solicitacoes/domain/WorkflowDefinition.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/domain/WorkflowDefinition.kt) |
| Teste do WorkflowEngine | [`solicitacoes/test/WorkflowEngineTest.kt`](../../backend/modules/solicitacoes/src/test/kotlin/br/ufpr/sept/so2/modules/solicitacoes/domain/WorkflowEngineTest.kt) |

---

## F1.7-D01 — Listar Solicitações do Aluno

### Chamada e JSON de resposta

```
GET /requests?estado=ABERTA&page=0&size=20
Authorization: Bearer eyJhbGci...
```

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "numeroAnual": 42,
      "ano": 2026,
      "tipoCode": "APROVEITAMENTO_DISCIPLINA",
      "estado": "EM_DELIBERACAO",
      "prazoEm": "2026-08-25T23:59:59Z",
      "_links": {
        "self": "/requests/550e8400-e29b-41d4-a716-446655440000"
      }
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### FGAC — filtro automático por `request.view_own`

`GET /requests` chama `RequestQuery.list(...)` (não JPA no controller).

```kotlin
// RequestQuery.kt
val idSolicitante = if (
    user.authorities.contains("request.view_own") &&
    !user.authorities.contains("request.view_curso") &&
    !user.authorities.contains("request.deliberate")
) {
    user.userId  // aluno só vê as SUAS solicitações
} else {
    null  // secretaria/professor vê todas
}
```

Um aluno com apenas `request.view_own` **só pode ver suas próprias solicitações** — mesmo que passe `idSolicitante` diferente na query, o filtro do servidor ignora e usa o UUID do JWT.

---

## F1.8-D02 — Listar Tipos de Solicitação

### Chamada

```
GET /requests/types
Authorization: Bearer eyJhbGci...
```

```json
[
  {
    "id": "a3bb189e-8bf9-3888-9912-3e6bad1d8f7e",
    "code": "APROVEITAMENTO_DISCIPLINA",
    "descricao": "Aproveitamento de Disciplina Cursada",
    "prazoDias": 30,
    "formSchema": {
      "type": "object",
      "properties": {
        "disciplina": { "type": "string", "title": "Nome da disciplina" },
        "instituicao": { "type": "string", "title": "Instituição de origem" },
        "cargaHoraria": { "type": "number", "title": "Carga horária (horas)" }
      },
      "required": ["disciplina", "cargaHoraria"]
    }
  }
]
```

> O `formSchema` é um JSON Schema completo. Lookup de disciplinas no wizard: alias **`GET /academico/disciplinas`** (`AcademicoController.listDisciplinasAlias`, `idCurso` opcional) — não só `GET /academico/cursos/{id}/disciplinas`. **Não é necessário criar um componente React por tipo de solicitação** — este é o coração do princípio DRY do sistema.

---

## F1.8-D04 — Abrir Nova Solicitação

### JSON de entrada

```json
POST /requests
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "idRequestType": "a3bb189e-8bf9-3888-9912-3e6bad1d8f7e",
  "idCurso": "c9bf9e57-1685-4c89-bafb-ff5af830be8a",
  "dados": {
    "disciplina": "Cálculo Diferencial e Integral I",
    "instituicao": "UFPR",
    "cargaHoraria": 60,
    "observacoes": "Cursada no período 2024/1 com aprovação."
  }
}
```

### DTO de entrada

```kotlin
// RequestController.kt
data class OpenRequestDto(
    val idRequestType: UUID,
    val idCurso: UUID,
    val dados: Map<String, Any>,  // JSONB — schema-less, validado pelo formSchema no frontend
)
```

### O que o `OpenRequestUseCase` faz

```kotlin
// OpenRequestUseCase.kt
@Transactional
fun execute(command: OpenRequestCommand): UUID {
    val requestType = requestTypeRepo.findById(command.idRequestType).orElseThrow()
    require(requestType.ativo) { "Tipo de solicitação inativo" }
    
    val ano = OffsetDateTime.now().year.toShort()
    val ultimoNumero = requestRepo.findMaxNumeroAnual(ano, command.idCurso) ?: 0
    val numeroAnual = ultimoNumero + 1          // ex: solicitação nº 43/2026
    val prazoEm = OffsetDateTime.now().plusDays(requestType.prazoDias.toLong())
    
    val entity = RequestEntity(
        numeroAnual = numeroAnual,
        ano = ano,
        idRequestType = requestType.id,
        requestTypeCode = requestType.code,
        idSolicitante = command.idSolicitante,  // do JWT
        idCurso = command.idCurso,
        estado = "ABERTA",                      // estado inicial sempre ABERTA
        dados = command.dados,                  // JSONB livre
        prazoEm = prazoEm,
    )
    return requestRepo.save(entity).id
}
```

### JSON de saída — 201 Created

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "_links": {
    "self": "/requests/550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## F1.9-D06 — Detalhe de Solicitação com HATEOAS Transitions

Esta é a resposta mais rica do sistema de solicitações — inclui o estado atual e as **ações disponíveis** para o usuário atual baseadas no workflow e nas suas capabilities.

### Chamada

```
GET /requests/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer eyJhbGci...
```

### JSON de saída — `_links` como `Map<String,String>`

Não é HAL `EntityModel` (`{ rel, href }`). Rel = chave; valor = path string.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "numeroAnual": 42,
  "ano": 2026,
  "tipoCode": "APROVEITAMENTO_DISCIPLINA",
  "tipoDescricao": "Aproveitamento de Disciplina Cursada",
  "estado": "EM_DELIBERACAO",
  "dados": {
    "disciplina": "Cálculo Diferencial e Integral I",
    "cargaHoraria": 60
  },
  "parecer": null,
  "prazoEm": "2026-08-25T23:59:59Z",
  "concludedAt": null,
  "createdAt": "2026-07-15T10:30:00Z",
  "idRequestTypeVersion": "…",
  "formSchema": { "type": "object" },
  "_links": {
    "self": "/requests/550e8400-...",
    "events": "/requests/550e8400-.../events",
    "attachments": "/requests/550e8400-.../attachments",
    "defer": "/requests/550e8400-.../transitions",
    "deny": "/requests/550e8400-.../transitions",
    "request-adjustment": "/requests/550e8400-.../transitions"
  }
}
```

> Os `_links` disponíveis dependem de **dois fatores**: (1) o estado atual da solicitação e (2) as capabilities do usuário autenticado. Um aluno em `ABERTA` vê `self` / `events` / `attachments` (e `upload-url` se dono); um deliberador ganha as ações do workflow em kebab-case. `formSchema` no GET detalhe vem do snapshot **`request_type_version`** (Flyway **V019**) da instância, não necessariamente do tipo “live”.

### Como o WorkflowEngine determina as ações disponíveis

```kotlin
// RequestQuery.getById — não o controller
val allowedTransitions = engine.allowedTransitions(currentState, user.authorities)
allowedTransitions.forEach { transition ->
    links[transition.action.lowercase().replace('_', '-')] = "/requests/$id/transitions"
}
```

O `WorkflowDefinition` é um JSON armazenado na tabela `request_type.workflow_json`. Para adicionar um novo tipo de solicitação basta inserir uma linha no banco com o workflow JSON correto — sem código Kotlin novo.

---

## Aplicar Transição (Deliberar/Encaminhar/Ajustar)

### JSON de entrada

```json
POST /requests/550e8400-e29b-41d4-a716-446655440000/transitions
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "action": "DEFERIR",
  "parecer": "Aprovado por cumprimento dos requisitos do PPC 2022."
}
```

### DTO de entrada

```kotlin
// RequestController.kt
data class TransitionDto(
    @field:NotBlank val action: String,
    val parecer: String?,
)
```

### O que o `TransitionRequestUseCase` faz

```kotlin
// TransitionRequestUseCase.kt
@Transactional
fun execute(command: TransitionCommand) {
    val entity = requestRepo.findById(command.requestId).orElseThrow()
    val workflowDef = objectMapper.convertValue(requestType.workflowJson, WorkflowDefinition::class.java)
    val engine = WorkflowEngine(workflowDef)
    
    val domainRequest = Request(/* mapeia entity → domínio */)
    
    // O WorkflowEngine valida se a transição é permitida para as capabilities do ator
    val result = engine.applyTransition(
        request = domainRequest,
        action = command.action,
        actorId = command.actorId,
        actorAuthorities = command.actorAuthorities,
        parecer = command.parecer,
    )
    
    requestRepo.updateEstado(
        id = command.requestId,
        estado = result.newState.name,
        parecer = command.parecer,
        concluded = result.newState.isFinal(),
    )
    
    requestEventRepo.save(RequestEventEntity(
        idRequest = command.requestId,
        tipo = command.action,
        estadoAnterior = domainRequest.estado.name,
        estadoNovo = result.newState.name,
        idAtor = command.actorId,
        parecer = command.parecer,
    ))
}
```

### JSON de saída — 200

```json
HTTP/1.1 200 OK
{
  "mensagem": "Transição 'DEFERIR' aplicada com sucesso."
}
```

---

## Histórico de Eventos (Trilha de Auditoria)

```
GET /requests/550e8400-.../events
Authorization: Bearer eyJhbGci...
```

```json
[
  {
    "tipo": "ABERTURA",
    "estadoAnterior": null,
    "estadoNovo": "ABERTA",
    "parecer": null,
    "createdAt": "2026-07-15T10:30:00Z"
  },
  {
    "tipo": "ENCAMINHAR_DELIBERACAO",
    "estadoAnterior": "ABERTA",
    "estadoNovo": "EM_DELIBERACAO",
    "parecer": null,
    "createdAt": "2026-07-16T09:00:00Z"
  },
  {
    "tipo": "DEFERIR",
    "estadoAnterior": "EM_DELIBERACAO",
    "estadoNovo": "DELIBERADA",
    "parecer": "Aprovado.",
    "createdAt": "2026-07-20T14:00:00Z"
  }
]
```

---

---

## Fluxo com Anexos (atualizado)

### 1. Obter URL de upload (presigned PUT)

```
POST /requests/attachments/presigned-url
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "nomeOriginal": "historico_escolar.pdf",
  "contentType": "application/pdf",
  "categoria": "HISTORICO_ESCOLAR"
}
```

```json
// Response 200
{
  "uploadUrl": "https://minio.local/solicitacoes/tmp/uuid-rand.pdf?X-Amz-Signature=...",
  "storageKey": "solicitacoes/tmp/uuid-rand.pdf"
}
```

O cliente recebe uma URL de upload pré-assinada válida por 15 minutos.

### 2. Upload direto para MinIO

O cliente faz `PUT` diretamente com o `uploadUrl` — sem passar pelo backend. Isso evita saturar a memória do servidor com arquivos grandes.

```
PUT https://minio.local/solicitacoes/tmp/uuid-rand.pdf?X-Amz-Signature=...
Content-Type: application/pdf
[body: bytes do arquivo]
```

### 3. Submeter solicitação com lista de anexos

```json
POST /requests
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "idRequestType": "a3bb189e-8bf9-3888-9912-3e6bad1d8f7e",
  "idCurso": "c9bf9e57-1685-4c89-bafb-ff5af830be8a",
  "dados": {
    "disciplina": "Cálculo Diferencial e Integral I",
    "cargaHoraria": 60
  },
  "attachments": [
    {
      "storageKey": "solicitacoes/tmp/uuid-rand.pdf",
      "sha256": "e3b0c44298fc1c149afb...",
      "nomeOriginal": "historico_escolar.pdf",
      "contentType": "application/pdf",
      "categoria": "HISTORICO_ESCOLAR",
      "tamanhoBytes": 204800
    }
  ]
}
```

O `OpenRequestUseCase` salva `RequestEntity` + uma `RequestAttachmentEntity` por anexo na **mesma transação**:

```kotlin
// OpenRequestUseCase.kt (trecho — com suporte a anexos)
@Transactional
fun execute(command: OpenRequestCommand): UUID {
    // ... cria RequestEntity normalmente ...

    command.attachments.forEach { att ->
        attachmentRepo.save(RequestAttachmentEntity(
            idRequest    = entity.id,
            storageKey   = att.storageKey,
            sha256       = att.sha256,
            nomeOriginal = att.nomeOriginal,
            contentType  = att.contentType,
            categoria    = att.categoria,
            tamanhoBytes = att.tamanhoBytes,
        ))
    }

        outboxPublisher.enqueue(
            eventType = "solicitacoes.aberta",
            aggregateType = "Request",
            aggregateId = entity.id,
            payload = mapOf(
                "requestId" to entity.id,
                "tipoCode" to requestType.code,
                "estadoNovo" to "ABERTA",
                "idSolicitante" to command.idSolicitante,
            ),
        )

    return entity.id
}
```

### 4. Listar e baixar anexos

```
GET /requests/{id}/attachments
→ [ { id, nomeOriginal, contentType, categoria, tamanhoBytes, createdAt } ]

GET /requests/{id}/attachments/{attachmentId}/download-url
→ { "downloadUrl": "https://minio.local/...?X-Amz-Signature=..." }

DELETE /requests/{id}/attachments/{attachmentId}
→ 204 No Content
```

---

## Rascunho (Draft)

O fluxo de rascunho permite que o aluno salve uma solicitação sem submetê-la (estado `RASCUNHO`), e depois a promova para `ABERTA` quando estiver pronto.

### Salvar rascunho

```
POST /requests/draft
Authorization: Bearer eyJhbGci...
```

- Persiste `RequestEntity` com `estado = RASCUNHO`
- **Não** incrementa `numeroAnual`
- **Não** calcula `prazoEm`
- **Não** enfileira evento no outbox

```json
// Response 201
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "_links": {
    "self": "/requests/550e8400-e29b-41d4-a716-446655440000",
    "submit": "/requests/550e8400-e29b-41d4-a716-446655440000/submit"
  }
}
```

### Promover rascunho para ABERTA

```
POST /requests/{id}/submit
Authorization: Bearer eyJhbGci...
```

O `SubmitDraftUseCase`:
1. Verifica que o estado é `RASCUNHO`
2. Atribui `numeroAnual` (próximo da sequência do curso/ano)
3. Calcula `prazoEm` com base em `requestType.prazoDias`
4. Muda estado para `ABERTA`
5. Enfileira `solicitacoes.aberta` no outbox

```json
// Response 200
{
  "id": "550e8400-...",
  "estado": "ABERTA",
  "protocolo": "2026/0043",
  "_links": { "self": "/requests/550e8400-..." }
}
```

---

## Protocolo Público

```
GET /requests/{id}/protocol
Authorization: Bearer eyJhbGci...
```

```json
// Response 200
{
  "protocolo": "2026/0042",
  "tipo": "APROVEITAMENTO_DISCIPLINA",
  "estado": "EM_DELIBERACAO",
  "_links": {
    "public": "/publico/solicitacoes/2026/42"
  }
}
```

Útil para o aluno compartilhar o protocolo com a secretaria (balcão) ou verificar o status publicamente.

**FGAC:** `GET /requests/{id}/protocol` e anexos (`list`/`download`/`delete`) exigem dono (`idSolicitante`) **ou** `request.view_curso` / `request.deliberate`. Delete de anexo só o solicitante, e só em `ABERTA`/`RASCUNHO`.

---

## Deliberação em lote (secretaria / autorização de imagem)

```
GET /requests?type=AUTORIZACAO_IMAGEM
PATCH /requests/bulk-deliberate
```

`type` é alias de `typeCode`. Detalhes e 409 all-or-nothing: [T-F5-SECRETARIA](../F5 — Secretaria/T-F5-SECRETARIA.md) § F5.2. Catálogo admin de tipos: [T-F7-003](../F7 — Admin/T-F7-003-WORKFLOW-ENGINE.md).

---

## Checklist de Verificação

- [x] `GET /requests/types` → lista de tipos com `formSchema` JSON Schema
- [x] `POST /requests` com `dados` JSONB livre → `201` com UUID
- [x] `POST /requests` com `attachments` → `RequestAttachmentEntity` salva na mesma TX
- [x] `numeroAnual` incrementado corretamente por curso/ano
- [x] `prazoEm` calculado com base em `requestType.prazoDias`
- [x] `GET /requests/{id}` → detalhes + `_links` de transições baseados em capabilities + estado
- [x] `POST /requests/{id}/transitions` → aplica transição via WorkflowEngine
- [x] `GET /requests/{id}/events` → trilha de auditoria cronológica
- [x] FGAC: aluno com `request.view_own` só vê suas solicitações
- [x] Outbox após transição para notificar aluno — `TransitionRequestUseCase` + `RequestTransitionOutboxHandler`
- [x] Outbox ao abrir solicitação — `OpenRequestUseCase` + `solicitacoes.aberta`
- [x] `POST /requests/attachments/presigned-url` → URL de upload MinIO
- [x] `GET /requests/{id}/attachments` → lista de anexos
- [x] `GET /requests/{id}/attachments/{attachmentId}/download-url` → URL de download
- [x] `DELETE /requests/{id}/attachments/{attachmentId}` → 204
- [x] `POST /requests/draft` → salva com `estado=RASCUNHO`, sem outbox
- [x] `POST /requests/{id}/submit` → promove para `ABERTA`, atribui `numeroAnual`, enfileira outbox
- [x] `GET /requests/{id}/protocol` → `{protocolo, tipo, estado, _links.public}` (dono ou staff)
- [x] `GET /requests` / `GET /requests/{id}` / events / types → `RequestQuery` (controller sem JPA)
- [x] `_links` = `Map<String,String>` (não HAL `EntityModel`)
- [x] GET detalhe usa `form_schema` da `request_type_version` (V019)
- [x] Outbox via `OutboxEventPublisher.enqueue` (`OpenRequestUseCase`, `TransitionRequestUseCase`)
- [x] `PATCH /requests/bulk-deliberate` → 200 ou 409 (rollback)
