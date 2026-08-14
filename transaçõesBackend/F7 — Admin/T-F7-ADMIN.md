# T-F7 — Admin: IAM, Workflow Engine, Auditoria, Saúde

> **Diagramas de referência:** [`foundationDocs/sequenceDiagrams/F7 — Admin/`](../../foundationDocs/sequenceDiagrams/F7 — Admin/)  
> **Status:** ✅ Auditoria, Outbox admin, FAQ, LGPD, roles/authorities, RequestType e templates

Tutoriais específicos:

| HU | Tutorial |
|----|----------|
| F7.2 Perfis / authorities | [T-F7-002-IAM-PERFIS.md](T-F7-002-IAM-PERFIS.md) |
| F7.3 Workflow / RequestType | [T-F7-003-WORKFLOW-ENGINE.md](T-F7-003-WORKFLOW-ENGINE.md) |
| F7.4 Templates | [T-F7-004-TEMPLATES-COMUNICACAO.md](T-F7-004-TEMPLATES-COMUNICACAO.md) |
| F7.5 Jobs outbox | [T-10.6-ADMIN-OUTBOX.md](../transversal/T-10.6-ADMIN-OUTBOX.md) |

---

## F7.6 — Audit Log ✅

Todo evento mutante do sistema é registrado via `AuditPublisher`:

```kotlin
// shared/audit/AuditPublisher.kt
auditPublisher.publish(AuditPayload(
    acao = "LOGIN_SUCCESS",
    idAtor = usuario.id,
    alvoTipo = "usuario",
    alvoId = usuario.id,
    ip = command.ip,
    userAgent = command.userAgent,
    resultado = "OK",
))
```

A tabela `audit_log` (migration V007) registra todas as ações.

### Consulta de Auditoria — `GET /admin/audit`

```
GET /admin/audit?acao=LOGIN_SUCCESS&idAtor={uuid}&alvoTipo=usuario&de=2026-08-01&ate=2026-08-12&page=0&size=20
Authorization: Bearer eyJhbGci...
```

```json
// Response 200 — PageResponse<AuditLogDto>
{
  "content": [
    {
      "id": "...",
      "acao": "LOGIN_SUCCESS",
      "idAtor": "a3bb189e-...",
      "alvoTipo": "usuario",
      "alvoId": "a3bb189e-...",
      "ip": "192.168.1.10",
      "userAgent": "Mozilla/5.0...",
      "resultado": "OK",
      "createdAt": "2026-08-12T14:32:00Z"
    }
  ],
  "totalElements": 1543,
  "totalPages": 78,
  "number": 0,
  "size": 20
}
```

```kotlin
// AuditController.kt
@GetMapping("/admin/audit")
@PreAuthorize("hasAuthority('system.admin')")
fun listAudit(
    @RequestParam(required = false) acao: String?,
    @RequestParam(required = false) idAtor: UUID?,
    @RequestParam(required = false) alvoTipo: String?,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) de: LocalDate?,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) ate: LocalDate?,
    pageable: Pageable,
): ResponseEntity<PageResponse<AuditLogDto>> {
    val page = auditRepo.findWithFilters(acao, idAtor, alvoTipo, de, ate, pageable)
    return ResponseEntity.ok(PageResponse.from(page) { AuditLogDto.from(it) })
}
```

### Eventos de auditoria já emitidos

| Use Case | Evento Auditado |
|----------|----------------|
| `LoginUseCase` | `LOGIN_SUCCESS`, `LOGIN_FAILED`, `ACCOUNT_BLOCKED` |
| `RefreshTokenUseCase` | `SUSPICIOUS_TOKEN_REUSE` |
| `ForgotPasswordUseCase` | `PASSWORD_RESET_REQUESTED` |
| `ResetPasswordUseCase` | `PASSWORD_CHANGED` |
| `FirstAccessUseCase` | `FIRST_ACCESS_COMPLETED` |
| `ServiceRecordController` | `SERVICE_RECORD_CREATED`, `SERVICE_RECORD_ACKNOWLEDGED` |

---

## F7.1/F7.2 — IAM: Usuários e Perfis ✅

CRUD de **usuários** está em `/usuarios` (não `/admin/usuarios`):

| Endpoint | Capability | Status |
|----------|-----------|--------|
| `GET /usuarios` | `user.manage_students` / `user.manage_all` | ✅ |
| `POST /usuarios` | idem | ✅ |
| `PATCH /usuarios/{id}/status` | idem | ✅ |
| `GET /admin/roles` · `PATCH /admin/roles/{id}/authorities` · `PUT /admin/usuarios/{id}/roles` | `iam.manage_roles` | ✅ ver [T-F7-002](T-F7-002-IAM-PERFIS.md) |

---

## F7.A — FAQ Admin ✅

Endpoints de gerenciamento de FAQ, restritos a `system.admin`. O `SupportController` foi estendido com as rotas admin:

### Criar item de FAQ

```
POST /faq
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "pergunta": "Como solicitar aproveitamento de disciplina?",
  "resposta": "Acesse Solicitações > Nova Solicitação > Aproveitamento de Disciplina...",
  "categoria": "SOLICITACOES",
  "ordem": 1
}
```

```json
// Response 201
{
  "id": "e3b0c442-...",
  "pergunta": "Como solicitar aproveitamento de disciplina?",
  "ativo": true,
  "ordem": 1,
  "_links": { "self": "/faq/e3b0c442-..." }
}
```

### Atualizar item de FAQ

```
PATCH /faq/{id}
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "resposta": "Resposta atualizada...",
  "ordem": 2
}
```

### Desativar item de FAQ (soft-delete)

```
DELETE /faq/{id}
Authorization: Bearer eyJhbGci...
```

- Retorna `204 No Content`
- **Não** exclui o registro — apenas seta `ativo = false`
- O endpoint público `GET /faq` filtra automaticamente por `ativo = true`

```kotlin
// SupportController.kt (trecho — rotas admin)
@PostMapping("/faq")
@PreAuthorize("hasAuthority('system.admin')")
fun createFaq(@RequestBody @Valid dto: FaqDto): ResponseEntity<FaqItemResponse> { ... }

@PatchMapping("/faq/{id}")
@PreAuthorize("hasAuthority('system.admin')")
fun updateFaq(@PathVariable id: UUID, @RequestBody dto: FaqUpdateDto): ResponseEntity<FaqItemResponse> { ... }

@DeleteMapping("/faq/{id}")
@PreAuthorize("hasAuthority('system.admin')")
fun deleteFaq(@PathVariable id: UUID): ResponseEntity<Void> {
    faqRepo.findById(id).orElseThrow().also { it.ativo = false; faqRepo.save(it) }
    return ResponseEntity.noContent().build()
}
```

**Seed inicial:** `V013__faq_seed.sql` — 8 itens de FAQ pré-populados na migration.

---

## F7.B — LGPD: Exportação de Dados Pessoais ✅

### Endpoint

```
POST /me/data-export
Authorization: Bearer eyJhbGci...
```

```json
// Response 202 Accepted
{
  "jobId": "f1e2d3c4-...",
  "downloadUrl": "https://minio.local/lgpd-exports/f1e2d3c4-....json?X-Amz-Signature=...&X-Amz-Expires=86400"
}
```

### Implementação Síncrona Real

O `DataExportUseCase` implementa o fluxo de forma síncrona (retorna `202` mas o arquivo já está pronto):

```kotlin
// DataExportUseCase.kt
@Transactional(readOnly = true)
fun execute(idUsuario: UUID): DataExportResult {
    val usuario     = usuarioRepo.findById(idUsuario).orElseThrow()
    val solicitacoes = requestRepo.findByIdSolicitante(idUsuario)
    val formativas  = formativasRepo.findByIdAluno(idUsuario)
    val presencas   = attendanceRepo.findByIdAluno(idUsuario)

    val exportData = mapOf(
        "usuario"     to UsuarioExportDto.from(usuario),
        "solicitacoes" to solicitacoes.map { RequestExportDto.from(it) },
        "formativas"  to formativas.map { FormativaExportDto.from(it) },
        "presencas"   to presencas.map { PresencaExportDto.from(it) },
        "exportadoEm" to OffsetDateTime.now().toString(),
    )

    val json = objectMapper.writeValueAsBytes(exportData)
    val storageKey = "lgpd-exports/${UUID.randomUUID()}.json"
    minioStorageService.upload(storageKey, json, "application/json")

    val downloadUrl = minioStorageService.presignedGetUrl(
        storageKey,
        Duration.ofHours(24)  // URL válida por 24h
    )

    return DataExportResult(
        jobId = UUID.randomUUID(),
        downloadUrl = downloadUrl,
    )
}
```

### O que é exportado

| Seção | Descrição |
|-------|-----------|
| `usuario` | Dados cadastrais (nome, e-mail, GRR, CPF mascarado) |
| `solicitacoes` | Todas as solicitações com estados e pareceres |
| `formativas` | Horas formativas submetidas e aprovadas |
| `presencas` | Presenças confirmadas em eventos |
| `exportadoEm` | Timestamp ISO 8601 da geração |

- A URL de download é pré-assinada com validade de **24 horas**
- Após 24h, o arquivo expira automaticamente (política de ciclo de vida MinIO)
- Para exportar novamente, o usuário faz novo `POST /me/data-export`

---

## F7.3 — Workflow Engine Admin ✅

Ver [T-F7-003-WORKFLOW-ENGINE.md](T-F7-003-WORKFLOW-ENGINE.md) — `GET/POST/PATCH /request-types` + publish + delete.

Seed continua em `V010` / `V011`.

---

## F7.4 — Templates de Comunicação ✅

Ver [T-F7-004-TEMPLATES-COMUNICACAO.md](T-F7-004-TEMPLATES-COMUNICACAO.md).

---

## F7.5 — Jobs / Outbox ✅

Implementado em [T-10.6-ADMIN-OUTBOX.md](../transversal/T-10.6-ADMIN-OUTBOX.md):

```
GET  /admin/outbox?status=DEAD
POST /admin/outbox/{id}/retry
DELETE /admin/outbox/{id}
```

---

## F7.9 — Saúde do Sistema (Actuator) ✅

Já configurado via Spring Boot Actuator:
```
GET /actuator/health  → UP/DOWN + health dos dependências (DB, Redis)
GET /actuator/info    → versão da aplicação
GET /actuator/metrics → métricas Micrometer
```

---

## Checklist de Verificação

- [x] Audit log registrado em todos os use cases críticos de IAM
- [x] `GET /admin/audit` — `AuditController` com filtros (ação, ator, tipo, intervalo de datas)
- [x] `GET /actuator/health` → `{"status": "UP"}`
- [x] `POST /faq` → cria item de FAQ (authority `system.admin`)
- [x] `PATCH /faq/{id}` → atualiza item de FAQ
- [x] `DELETE /faq/{id}` → soft-delete (`ativo = false`)
- [x] `V013__faq_seed.sql` → 8 itens de FAQ pré-populados
- [x] `POST /me/data-export` → exportação LGPD síncrona real, URL MinIO pré-assinada 24h
- [x] `GET /me/data-export/{jobId}` → `READY` + URL se o objeto existe; `EXPIRED` se não
- [x] `GET /admin/roles` + matriz de authorities + `PUT /admin/usuarios/{id}/roles`
- [x] `GET/POST /request-types` + publish
- [x] `GET/POST /communication-templates` + revisões
- [x] Admin outbox — ver T-10.6
