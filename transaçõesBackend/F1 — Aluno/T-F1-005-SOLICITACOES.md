# T-F1-005 — Solicitações Acadêmicas (Motor de Workflow)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-005-SOLICITACOES.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-005-SOLICITACOES.md)  
> **Status:** ✅ Implementado — Motor de workflow genérico, HATEOAS transitions, paginação

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller | [`solicitacoes/api/RequestController.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/api/RequestController.kt) |
| Use Case — Abrir | [`solicitacoes/application/OpenRequestUseCase.kt`](../../backend/modules/solicitacoes/src/main/kotlin/br/ufpr/sept/so2/modules/solicitacoes/application/OpenRequestUseCase.kt) |
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

```kotlin
// RequestController.kt
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

> O `formSchema` é um JSON Schema completo. O frontend usa o campo `formSchema` para renderizar dinamicamente o formulário de nova solicitação via `DS/DynamicForm`. **Não é necessário criar um componente React por tipo de solicitação** — este é o coração do princípio DRY do sistema.

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

### JSON de saída — `EntityModel` com `_links`

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
  "_links": [
    { "rel": "self",       "href": "/requests/550e8400-...", "type": null },
    { "rel": "deferir",    "href": "/requests/550e8400-.../transitions", "type": "POST" },
    { "rel": "indeferir",  "href": "/requests/550e8400-.../transitions", "type": "POST" },
    { "rel": "solicitar-ajuste", "href": "/requests/550e8400-.../transitions", "type": "POST" }
  ]
}
```

> Os `_links` disponíveis dependem de **dois fatores**: (1) o estado atual da solicitação e (2) as capabilities do usuário autenticado. Um aluno veria apenas `_links: [self]` enquanto um professor deliberador veria `deferir`, `indeferir` e `solicitar-ajuste`.

### Como o WorkflowEngine determina as ações disponíveis

```kotlin
// RequestController.kt — getById
val requestType = requestTypeRepo.findById(entity.idRequestType).orElseThrow()
val workflowDef = objectMapper.convertValue(requestType.workflowJson, WorkflowDefinition::class.java)
val engine = WorkflowEngine(workflowDef)
val currentState = RequestState.valueOf(entity.estado)
val allowedTransitions = engine.allowedTransitions(currentState, user.authorities)

allowedTransitions.forEach { transition ->
    model.add(
        Link.of("/requests/$id/transitions")
            .withRel(transition.action.lowercase().replace('_', '-'))
            .withType("POST")
    )
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

## Checklist de Verificação

- [x] `GET /requests/types` → lista de tipos com `formSchema` JSON Schema
- [x] `POST /requests` com `dados` JSONB livre → `201` com UUID
- [x] `numeroAnual` incrementado corretamente por curso/ano
- [x] `prazoEm` calculado com base em `requestType.prazoDias`
- [x] `GET /requests/{id}` → detalhes + `_links` de transições baseados em capabilities + estado
- [x] `POST /requests/{id}/transitions` → aplica transição via WorkflowEngine
- [x] `GET /requests/{id}/events` → trilha de auditoria cronológica
- [x] FGAC: aluno com `request.view_own` só vê suas solicitações
- [ ] Outbox após transição para notificar aluno — **não implementado no TransitionUseCase**
- [ ] Draft de solicitação (`POST /requests/draft`) — **não implementado**
- [ ] Download de anexo via presigned URL MinIO — **não implementado**
