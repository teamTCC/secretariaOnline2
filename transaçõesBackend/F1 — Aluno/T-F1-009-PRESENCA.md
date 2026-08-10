# T-F1-009 — Confirmar Presença em Eventos (v4.1)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-009-PRESENCA.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-009-PRESENCA.md)  
> **Status:** ✅ Implementado — modos SECRET e QR, janelas configuráveis, device binding, HATEOAS session

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller (eventos + presença) | [`presenca/api/EventAttendanceController.kt`](../../backend/modules/presenca/src/main/kotlin/br/ufpr/sept/so2/modules/presenca/api/EventAttendanceController.kt) |
| Domínio — modos de presença | `presenca/domain/AttendanceMode.kt` |
| Domínio — estados do evento | `presenca/domain/EventState.kt` |
| Persistência — eventos | `presenca/persistence/EventAttendanceEntity.kt` |
| Persistência — sessões | `presenca/persistence/AttendanceSessionEntity.kt` |

---

## Criar Evento (Professor/Secretaria)

```json
POST /events
Authorization: Bearer eyJhbGci...  (hasAuthority('event.manage'))
Content-Type: application/json

{
  "titulo": "Palestra: Inteligência Artificial na Engenharia",
  "descricao": "Palestra do Prof. Dr. João com 4h de carga formativa.",
  "idCurso": "c9bf9e57-1685-4c89-bafb-ff5af830be8a",
  "attendanceMode": "SECRET_SINGLE",
  "chCreditadas": 4.0,
  "inicioEm": "2026-08-10T14:00:00Z",
  "fimEm": "2026-08-10T18:00:00Z"
}
```

### DTO de entrada

```kotlin
// EventAttendanceController.kt
data class CreateEventDto(
    @field:NotBlank val titulo: String,
    val descricao: String?,
    val idCurso: UUID?,
    val attendanceMode: AttendanceMode,  // QR_SINGLE, QR_DUAL, SECRET_SINGLE, SECRET_DUAL
    val chCreditadas: Double,
    val inicioEm: OffsetDateTime,
    val fimEm: OffsetDateTime,
)
```

```json
HTTP/1.1 201 Created

{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "_links": {
    "self": "/events/7c9e6679-7425-40de-944b-e07fc1f90ae7"
  }
}
```

---

## Detalhe do Evento com HATEOAS

```
GET /events/7c9e6679-7425-40de-944b-e07fc1f90ae7
Authorization: Bearer eyJhbGci...
```

A resposta inclui `_links` diferentes dependendo de quem está consultando:

### Resposta para o professor organizador (estado `EM_ANDAMENTO`)

```json
{
  "id": "7c9e6679-...",
  "titulo": "Palestra: IA na Engenharia",
  "attendanceMode": "SECRET_SINGLE",
  "estado": "EM_ANDAMENTO",
  "chCreditadas": 4.0,
  "inicioEm": "2026-08-10T14:00:00Z",
  "fimEm": "2026-08-10T18:00:00Z",
  "_links": [
    { "rel": "self", "href": "/events/7c9e6679-..." },
    { "rel": "abrir-janela-entrada", "href": "/events/7c9e6679-.../attendance/windows/entry", "type": "POST" },
    { "rel": "encerrar-evento",      "href": "/events/7c9e6679-.../close", "type": "POST" }
  ]
}
```

> Para modo `QR_DUAL` ou `SECRET_DUAL` aparece também `abrir-janela-saida`.

### Como os links são determinados

```kotlin
// EventAttendanceController.kt — getById
val isOrganizador = event.idOrganizador == user.userId
val canManage = user.authorities.contains("event.manage")
val canHost = user.authorities.contains("event.host") || isOrganizador
val estado = EventState.valueOf(event.estado)

if (canHost && estado == EventState.EM_ANDAMENTO) {
    model.add(Link.of("/events/$eventId/attendance/windows/entry")
        .withRel("abrir-janela-entrada").withType("POST"))
    
    val mode = AttendanceMode.valueOf(event.attendanceMode)
    if (mode.isDual()) {  // QR_DUAL ou SECRET_DUAL
        model.add(Link.of("/events/$eventId/attendance/windows/exit")
            .withRel("abrir-janela-saida").withType("POST"))
    }
    model.add(Link.of("/events/$eventId/close")
        .withRel("encerrar-evento").withType("POST"))
}
```

---

## Abrir Janela de Presença (Professor)

```json
POST /events/7c9e6679-.../attendance/windows/entry
Authorization: Bearer eyJhbGci...  (hasAuthority('event.host'))

{
  "durationSeconds": 900
}
```

### O que acontece internamente

```kotlin
// EventAttendanceController.kt — openEntryWindow
val durationSecs = dto?.durationSeconds ?: 3600
val now = OffsetDateTime.now()
val window = mapOf(
    "phase"   to "ENTRY",
    "openAt"  to now.toString(),
    "closeAt" to now.plusSeconds(durationSecs.toLong()).toString(),
    "secret"  to if (mode.isSecret()) generatePin() else null,    // PIN 6 dígitos
    "qrToken" to if (mode.isQr()) generateQrToken() else null,    // UUID sem hífens
)

// Salva a janela no JSONB validationWindows do evento
val updatedWindows = event.validationWindows + window
event.validationWindows = updatedWindows
eventRepo.save(event)
```

```json
HTTP/1.1 200 OK

{
  "mensagem": "Janela de entrada aberta",
  "closeAt": "2026-08-10T14:15:00Z"
}
```

> O professor exibe o PIN ou QR gerado para os alunos presentes na sala.

---

## Sessão do Aluno com HATEOAS (F1.17-D02)

```
GET /events/7c9e6679-.../attendance/session
Authorization: Bearer eyJhbGci...  (hasAuthority('attendance.check_in'))
```

```json
{
  "idEvento": "7c9e6679-...",
  "estado": "EM_ANDAMENTO",
  "attendanceMode": "SECRET_SINGLE",
  "entryConfirmedAt": null,
  "exitConfirmedAt": null,
  "isComplete": false,
  "_links": [
    { "rel": "self", "href": "/events/7c9e6679-.../attendance/session" },
    { "rel": "confirmar-entrada", "href": "/events/7c9e6679-.../attendance/entry", "type": "POST" }
  ]
}
```

> O link `confirmar-entrada` **só aparece** se o evento está `EM_ANDAMENTO` E a entrada ainda não foi confirmada. Para modo QR, o link aponta para o endpoint de validação QR.

---

## Confirmar Entrada (Modo SECRET_SINGLE)

```json
POST /events/7c9e6679-.../attendance/entry
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "pin": "384920",
  "qrToken": null,
  "deviceUuid": "device-uuid-do-smartphone-do-aluno"
}
```

### DTO de entrada

```kotlin
data class ConfirmAttendanceDto(
    val pin: String?,         // para modos SECRET_*
    val qrToken: String?,     // para modos QR_*
    val deviceUuid: String?,  // para device binding anti-fraude
)
```

### O que o `processAttendance` faz

```kotlin
// EventAttendanceController.kt
private fun processAttendance(eventId, alunoId, phase, dto) {
    val event = eventRepo.findById(eventId).orElseThrow()
    
    // 1. Verifica estado do evento
    require(event.estado == EventState.EM_ANDAMENTO.name)
    
    // 2. Busca janela ativa (openAt < now < closeAt)
    val activeWindow = event.validationWindows.firstOrNull { w ->
        (w["phase"] as? String) == phase.name &&
        OffsetDateTime.parse(w["openAt"] as String).isBefore(OffsetDateTime.now()) &&
        OffsetDateTime.parse(w["closeAt"] as String).isAfter(OffsetDateTime.now())
    } ?: throw IllegalStateException("Janela de $phase não está ativa.")
    
    // 3. Valida PIN (SECRET_*) ou QR Token (QR_*)
    if (mode.isSecret()) {
        val expectedPin = activeWindow["secret"] as? String
        require(dto.pin == expectedPin) { "PIN inválido." }
    }
    
    // 4. Device binding — impede que 2 alunos usem o mesmo dispositivo
    if (dto.deviceUuid != null && phase == ENTRY) {
        val deviceConflict = sessionRepo.existsByIdEventoAndDeviceUuid(eventId, dto.deviceUuid)
        require(!deviceConflict) { "Este dispositivo já foi utilizado..." }
    }
    
    // 5. Cria ou recupera sessão do aluno
    val session = sessionRepo.findByIdEventoAndIdAluno(eventId, alunoId).orElseGet {
        sessionRepo.save(AttendanceSessionEntity(idEvento, idAluno, deviceUuid))
    }
    
    // 6. Confirma a fase (entrada ou saída)
    when (phase) {
        ENTRY -> {
            require(session.entryConfirmedAt == null) { "Entrada já confirmada." }
            sessionRepo.confirmEntry(session.id, OffsetDateTime.now())
        }
        EXIT -> {
            require(session.entryConfirmedAt != null) { "Entrada ainda não confirmada." }
            require(session.exitConfirmedAt == null) { "Saída já confirmada." }
            sessionRepo.confirmExit(session.id, OffsetDateTime.now())
        }
    }
}
```

```json
HTTP/1.1 200 OK

{
  "mensagem": "Entry confirmada com sucesso."
}
```

---

## Modos de Presença (v4.1)

| `attendanceMode` | PIN | QR | Fases | Janelas |
|-----------------|-----|----|-------|---------|
| `SECRET_SINGLE` | ✅ | ❌ | 1 (entrada) | 1 janela |
| `SECRET_DUAL` | ✅ | ❌ | 2 (entrada + saída) | 2 janelas |
| `QR_SINGLE` | ❌ | ✅ | 1 (entrada) | 1 janela |
| `QR_DUAL` | ❌ | ✅ | 2 (entrada + saída) | 2 janelas |

---

## Encerrar Evento e Emitir Certificados

```
POST /events/7c9e6679-.../close
Authorization: Bearer eyJhbGci...  (event.host)
```

```json
HTTP/1.1 200 OK

{
  "mensagem": "Evento encerrado. Certificados sendo processados."
}
```

> O estado do evento muda para `CONCLUIDO`. A emissão real de certificados (render PDF + SHA-256 + ED25519) ainda não está implementada — ver [T-10.4-CERTIFICADO](../transversal/T-10.4-CERTIFICADO.md).

---

## Checklist de Verificação

- [x] `POST /events` → `201` com evento no estado `AGENDADO`
- [x] `GET /events/{id}` → HATEOAS com links baseados em capabilities e estado
- [x] `POST /events/{id}/attendance/windows/entry` → abre janela com PIN ou QR token
- [x] `GET /events/{id}/attendance/session` → estado da sessão + `_links` condicionais
- [x] `POST /events/{id}/attendance/entry` → valida PIN/QR + janela ativa
- [x] Device binding — impede mesmo `deviceUuid` em múltiplas confirmações
- [x] Modo DUAL — exige `entryConfirmedAt != null` antes de confirmar saída
- [x] `POST /events/{id}/close` → estado `CONCLUIDO`
- [ ] Emissão automática de certificados após `close` — **não implementada**
- [ ] Notificação via Outbox após confirmação de presença — **não implementada**
