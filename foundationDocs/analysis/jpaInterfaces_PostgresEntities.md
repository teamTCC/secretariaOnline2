# JPA interfaces ↔ tabelas PostgreSQL

**As-built 2026-08-29** (`foundationDocs/analysis/as-built-backend.md` §5): Flyway V001–V019 = **~45 tabelas** de aplicação. **Não existe** `RequestLineItemJpaRepository` nem tabela `request_line_item` (linhas em `request.dados` JSONB). Escritas no outbox usam o port `OutboxEventPublisher` (`backend/shared/.../OutboxEventPublisher.kt`); `OutboxEventJpaRepository` fica no módulo notificações, atrás do publisher — use cases **não** injetam o JPA.

Listas abaixo: **primeiro o que existe no código** (`*JpaRepository` grep 2026-08); depois o rascunho 2026-06 (histórico).

---

## As-built — repositórios no código

### IAM / sessão
- `UsuarioJpaRepository` → `usuario`
- `RoleJpaRepository` → `role`
- `AuthorityJpaRepository` → `authority`
- `RefreshTokenJpaRepository` → `refresh_token`
- `JtiBlacklistJpaRepository` → `jti_blacklist` *(PK `String` / JTI; application: `EmailOneTimeTokenStore`)*
- `PasswordHistoryJpaRepository` → `password_history` *(tabela V002, não JSONB em `usuario`)*
- Sem `RoleAuthorityJpaRepository` / `UsuarioRoleJpaRepository` dedicados (`UsuarioRoleEntity` mapeada no IAM)

### IAM — V012 / V014 / V015
- `ServiceRecordJpaRepository` → `service_record`
- `FaqItemJpaRepository` → `faq_item`
- `SupportTicketJpaRepository` → `support_ticket`
- `FcmTokenJpaRepository` → `device_fcm_token`
- `GraduationRecordJpaRepository` → `graduation_record`
- `SecretaryTaskJpaRepository` → `secretary_task`
- `ImportJobJpaRepository` → `import_job`
- `ExportJobJpaRepository` → `export_job`
- `ContactMessageJpaRepository` → `contact_message`
- `NotificationPrefJpaRepository` → `notification_preference` *(IAM)*

### Acadêmico
- `CursoJpaRepository` → `curso`
- `DisciplinaJpaRepository` → `disciplina`
- `PeriodoLetivoJpaRepository` → `periodo_letivo`
- `CalendarioAcademicoJpaRepository` → `calendario_academico`
- `HistoricoEscolarJpaRepository` → `historico_escolar` *(V015)*

### Solicitações
- `RequestTypeJpaRepository` → `request_type`
- `RequestJpaRepository` → `request` *(FK `id_request_type_version`)*
- `RequestEventJpaRepository` → `request_event`
- `RequestAttachmentJpaRepository` → `request_attachment`
- `RequestTypeVersionJpaRepository` → `request_type_version` *(V019)*
- ~~`RequestLineItemJpaRepository`~~ — **não implementado**

### Formativas / Estágio / TCC
- `FormativeActivityJpaRepository` → `formative_activity`
- `FormativeEntryJpaRepository` → `formative_entry`
- `InternshipJpaRepository` → `internship`
- `InternshipDocumentJpaRepository` → `internship_document`
- `TccJpaRepository` → `tcc`
- `TccMemberJpaRepository` → `tcc_member`
- `TccExaminerJpaRepository` → `tcc_examiner`

### Comunicação / notificações / auditoria
- `CommunicationJpaRepository` → `communication`
- `CommunicationDeliveryJpaRepository` → `communication_delivery`
- `NotificationPreferenceJpaRepository` → `notification_preference` *(módulo comunicação)*
- `CommunicationTemplateJpaRepository` → `communication_template`
- `CommunicationTemplateRevisionJpaRepository` → `communication_template_revision`
- `NotificationLogJpaRepository` → `notification_log`
- **Writers:** `OutboxEventPublisher.enqueue(...)` → `outbox_event`
- `OutboxEventJpaRepository` → `outbox_event` *(infra notificações; dispatcher / publisher impl)*
- `EventAttendanceJpaRepository` → `event_attendance`
- `AttendanceSessionJpaRepository` → `attendance_session`
- `CertificateJpaRepository` → `certificate`
- `AuditLogJpaRepository` → `audit_log`

Sem `AttendanceValidationWindowJpaRepository` — janelas em `event_attendance.validation_windows` JSONB.

---

## Histórico 2026-06 (rascunho acadêmico — não usar como schema runtime)

Pelo schema proposto em `analise_arquitetural_secretariaonline2.md`, o número era:

- **29 tabelas de aplicação** (contando os `CREATE TABLE` do documento) + técnicas → **31** no `schema_completo.sql` (inclui `request_line_item` **não migrada**).

Runtime Flyway: **~45** tabelas. `flyway_schema_history` é criada pelo Flyway.

### IAM
- `UsuarioJpaRepository` → `usuario`
- `RoleJpaRepository` → `role`
- `AuthorityJpaRepository` → `authority`
- `RoleAuthorityJpaRepository` → `role_authority` *(opcional se mapear só via `@ManyToMany`)*
- `UsuarioRoleJpaRepository` → `usuario_role`

### Acadêmico
- `CursoJpaRepository` → `curso`
- `DisciplinaJpaRepository` → `disciplina`
- `PeriodoLetivoJpaRepository` → `periodo_letivo`
- `CalendarioAcademicoJpaRepository` → `calendario_academico`

### Solicitações
- `RequestTypeJpaRepository` → `request_type`
- `RequestJpaRepository` → `request`
- `RequestEventJpaRepository` → `request_event`
- ~~`RequestLineItemJpaRepository` → `request_line_item`~~ — **não existe no Flyway nem no código**
- `RequestAttachmentJpaRepository` → `request_attachment`
- `RequestTypeVersionJpaRepository` → `request_type_version` *(as-built V019)*

### Formativas
- `FormativeActivityJpaRepository` → `formative_activity`
- `FormativeEntryJpaRepository` → `formative_entry`

### Estágio
- `InternshipJpaRepository` → `internship`
- `InternshipDocumentJpaRepository` → `internship_document`

### TCC
- `TccJpaRepository` → `tcc`
- `TccMemberJpaRepository` → `tcc_member`
- `TccExaminerJpaRepository` → `tcc_examiner`

### Comunicação / Notificações
- `CommunicationJpaRepository` → `communication`
- `CommunicationDeliveryJpaRepository` → `communication_delivery`
- `NotificationPreferenceJpaRepository` → `notification_preference`
- **Writers:** `OutboxEventPublisher` → `outbox_event` *(não injetar `OutboxEventJpaRepository` no use case)*
- `OutboxEventJpaRepository` → `outbox_event` *(infra)*

### Presença / Certificados (v4.1 — `attendance_session`, modos configuráveis)
- `EventAttendanceJpaRepository` → `event_attendance`
- `AttendanceSessionJpaRepository` → `attendance_session`
- `AttendanceValidationWindowJpaRepository` → *(opcional)* `attendance_validation_window` — **não criado**; JSONB em `validation_windows`
- `CertificateJpaRepository` → `certificate`

### Auditoria
- `AuditLogJpaRepository` → `audit_log`

---


No Spring Data JPA, “interface concreta” normalmente significa: **você cria a interface** e o Spring gera a implementação concreta em runtime.

## Repositórios sugeridos (`interface` ↔ tabela)

### PK simples (`UUID`)
- `UsuarioJpaRepository` ↔ `usuario`
- `RoleJpaRepository` ↔ `role`
- `AuthorityJpaRepository` ↔ `authority`
- `CursoJpaRepository` ↔ `curso`
- `DisciplinaJpaRepository` ↔ `disciplina`
- `PeriodoLetivoJpaRepository` ↔ `periodo_letivo`
- `CalendarioAcademicoJpaRepository` ↔ `calendario_academico`
- `RequestTypeJpaRepository` ↔ `request_type`
- `RequestJpaRepository` ↔ `request`
- `RequestEventJpaRepository` ↔ `request_event`
- ~~`RequestLineItemJpaRepository` ↔ `request_line_item`~~ — **não existe (Flyway / código)**
- `RequestTypeVersionJpaRepository` ↔ `request_type_version`
- `RequestAttachmentJpaRepository` ↔ `request_attachment`
- `FormativeActivityJpaRepository` ↔ `formative_activity`
- `FormativeEntryJpaRepository` ↔ `formative_entry`
- `InternshipJpaRepository` ↔ `internship`
- `InternshipDocumentJpaRepository` ↔ `internship_document`
- `TccJpaRepository` ↔ `tcc`
- `CommunicationJpaRepository` ↔ `communication`
- `CommunicationDeliveryJpaRepository` ↔ `communication_delivery`
- `NotificationPreferenceJpaRepository` ↔ `notification_preference` *(PK também é UUID, mas é `id_usuario`)*
- `OutboxEventJpaRepository` *(ou `OutboxRepository`)* ↔ `outbox_event`
- `EventAttendanceJpaRepository` ↔ `event_attendance` *(campos `attendance_mode`, `validation_windows` ou tabela filha de janelas)*
- `AttendanceSessionJpaRepository` ↔ `attendance_session`
- `CertificateJpaRepository` ↔ `certificate`
- `AuditLogJpaRepository` ↔ `audit_log`

### PK composta (`@Embeddable` + `@EmbeddedId`)
- `RoleAuthorityJpaRepository` ↔ `role_authority` (`id_role`, `id_authority`)
- `UsuarioRoleJpaRepository` ↔ `usuario_role` (`id_usuario`, `id_role`)
- `TccMemberJpaRepository` ↔ `tcc_member` (`id_tcc`, `id_aluno`)
- `TccExaminerJpaRepository` ↔ `tcc_examiner` (`id_tcc`, `id_professor`)

---

## Esqueleto Kotlin (Spring Data JPA)

```kotlin
interface UsuarioJpaRepository : JpaRepository<UsuarioEntity, UUID>
interface RequestJpaRepository : JpaRepository<RequestEntity, UUID>
interface RequestEventJpaRepository : JpaRepository<RequestEventEntity, UUID>
interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, UUID>
interface CommunicationJpaRepository : JpaRepository<CommunicationEntity, UUID>
interface EventAttendanceJpaRepository : JpaRepository<EventAttendanceEntity, UUID>
interface AttendanceSessionJpaRepository : JpaRepository<AttendanceSessionEntity, UUID>
interface CertificateJpaRepository : JpaRepository<CertificateEntity, UUID>
interface AuditLogJpaRepository : JpaRepository<AuditLogEntity, UUID>
// Se `validation_windows` for normalizado em tabela filha:
// interface AttendanceValidationWindowJpaRepository : JpaRepository<AttendanceValidationWindowEntity, UUID>
```

### Exemplo de PK composta

```kotlin
@Embeddable
data class UsuarioRoleId(
    @Column(name = "id_usuario") val idUsuario: UUID = UUID(0, 0),
    @Column(name = "id_role") val idRole: UUID = UUID(0, 0)
) : Serializable

@Entity
@Table(name = "usuario_role")
data class UsuarioRoleEntity(
    @EmbeddedId val id: UsuarioRoleId,
    @Column(name = "escopo", columnDefinition = "jsonb") val escopo: String = "{}"
)

interface UsuarioRoleJpaRepository : JpaRepository<UsuarioRoleEntity, UsuarioRoleId>
```

---

