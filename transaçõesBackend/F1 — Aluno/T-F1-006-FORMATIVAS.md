# T-F1-006 — Horas Formativas (Atividades Complementares)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-006-FORMATIVAS.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-006-FORMATIVAS.md)  
> **Status:** ✅ Implementado — submit, listagem, revisão CAAF, resumo KPI

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller | [`formativas/api/FormativasController.kt`](../../backend/modules/formativas/src/main/kotlin/br/ufpr/sept/so2/modules/formativas/api/FormativasController.kt) |
| DTOs (inline no controller) | `SubmitFormativaDto`, `ReviewFormativaDto` |
| Repositórios | [`formativas/persistence/FormativasJpaRepositories.kt`](../../backend/modules/formativas/src/main/kotlin/br/ufpr/sept/so2/modules/formativas/infrastructure/persistence/FormativasJpaRepositories.kt) |

---

## Submeter Atividade (Aluno)

### JSON de entrada

```json
POST /formativas
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "titulo": "Palestra: Machine Learning Aplicado",
  "descricao": "Participação na palestra promovida pelo DINF em 2026-06-15",
  "categoria": "PALESTRA",
  "cargaHoraria": 4.0,
  "dataRealizacao": "2026-06-15"
}
```

### DTO de entrada

```kotlin
// FormativasController.kt
data class SubmitFormativaDto(
    @field:NotBlank val titulo: String,
    val descricao: String?,
    @field:NotBlank val categoria: String,
    val cargaHoraria: Double,
    val dataRealizacao: LocalDate,
)
```

### JSON de saída — 201 Created

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "a3bb189e-8bf9-3888-9912-3e6bad1d8f7e",
  "estado": "PENDENTE"
}
```

> A atividade nasce sempre no estado `PENDENTE` e aguarda revisão da CAAF.

---

## Listar Minhas Formativas (Aluno)

```
GET /formativas/minhas?page=0&size=20
Authorization: Bearer eyJhbGci...
```

```json
{
  "content": [
    {
      "id": "a3bb189e-8bf9-3888-9912-3e6bad1d8f7e",
      "titulo": "Palestra: Machine Learning Aplicado",
      "categoria": "PALESTRA",
      "cargaHoraria": 4.0,
      "estado": "PENDENTE",
      "dataRealizacao": "2026-06-15"
    },
    {
      "id": "b2cc290f-9cga-41e5-b823-557766551111",
      "titulo": "Hackathon UFPR 2026",
      "categoria": "COMPETICAO",
      "cargaHoraria": 8.0,
      "estado": "APROVADA",
      "dataRealizacao": "2026-05-10"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

## Resumo de Horas (KPI do Dashboard)

```
GET /formativas/resumo
Authorization: Bearer eyJhbGci...
```

```json
{
  "horasAprovadas": 47.5,
  "horasRequeridas": 120.0,
  "percentual": 39.58
}
```

```kotlin
// FormativasController.kt
fun resumo(): Map<String, Any> {
    val user = currentUser()
    val total = entryRepo.sumHorasAprovadas(user.userId)
    return mapOf(
        "horasAprovadas" to total,
        "horasRequeridas" to 120.0,
        "percentual" to (total / 120.0 * 100).coerceAtMost(100.0),
    )
}
```

> O valor de `120.0` (horas requeridas) está **hardcoded** por ora. Para cursos com cargas diferentes isso precisará ser lido da tabela de configuração do curso.

---

## Revisar Atividade (CAAF — `formative.review`)

### JSON de entrada

```json
PATCH /formativas/{id}/review
Authorization: Bearer eyJhbGci...  (professor/membro CAAF)
Content-Type: application/json

{
  "acao": "APROVAR",
  "parecer": "Comprovante válido, carga horária conferida."
}
```

### DTO de entrada

```kotlin
data class ReviewFormativaDto(
    @field:NotBlank val acao: String,     // "APROVAR" ou "REJEITAR"
    val parecer: String?,
)
```

### Lógica de revisão

```kotlin
// FormativasController.kt
fun review(@PathVariable id: UUID, @Valid @RequestBody dto: ReviewFormativaDto): ResponseEntity<Map<String, Any>> {
    val activity = activityRepo.findById(id).orElseThrow()
    require(activity.estado == "PENDENTE") { "Atividade não está pendente de revisão." }
    
    activity.estado = when (dto.acao.uppercase()) {
        "APROVAR"  -> "APROVADA"
        "REJEITAR" -> "REJEITADA"
        else -> throw IllegalArgumentException("Ação inválida: ${dto.acao}")
    }
    activity.parecerRevisor = dto.parecer
    activity.idRevisor = user.userId
    activityRepo.save(activity)
    
    return ResponseEntity.ok(mapOf("estado" to activity.estado))
}
```

### JSON de saída — 200

```json
{
  "estado": "APROVADA"
}
```

---

## Listar Pendentes de Revisão (CAAF)

```
GET /formativas/pendentes?page=0&size=20
Authorization: Bearer eyJhbGci...  (hasAuthority('formative.review'))
```

```json
{
  "content": [
    {
      "id": "a3bb189e-...",
      "idAluno": "c9bf9e57-...",
      "titulo": "Palestra: ML",
      "categoria": "PALESTRA",
      "cargaHoraria": 4.0,
      "dataRealizacao": "2026-06-15"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

## Mapa de Capabilities (FGAC)

| Authority | Pode fazer |
|-----------|-----------|
| `formative.submit` | `POST /formativas` — submeter nova atividade |
| `formative.view_own` | `GET /formativas/minhas` e `GET /formativas/resumo` |
| `formative.review` | `GET /formativas/pendentes` e `PATCH /formativas/{id}/review` |

---

## Checklist de Verificação

- [x] `POST /formativas` → `201` com estado `PENDENTE`
- [x] `GET /formativas/minhas` → paginado, só as do aluno autenticado
- [x] `GET /formativas/resumo` → KPI com percentual calculado
- [x] `GET /formativas/pendentes` → só acessível com `formative.review`
- [x] `PATCH /formativas/{id}/review` com `APROVAR`/`REJEITAR` → `200` com novo estado
- [x] Revisão só permitida quando `estado == "PENDENTE"`
- [ ] Notificação ao aluno após revisão via Outbox — **não implementado**
- [ ] Upload de comprovante (PDF/imagem) via MinIO — **não implementado**
- [ ] Horas requeridas configuráveis por curso — hardcoded 120h
