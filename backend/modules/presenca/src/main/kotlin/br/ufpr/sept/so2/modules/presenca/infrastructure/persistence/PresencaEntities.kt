package br.ufpr.sept.so2.modules.presenca.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Type
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "event_attendance",
    indexes = [
        Index(name = "idx_event_attendance_organizador", columnList = "id_organizador"),
        Index(name = "idx_event_attendance_curso", columnList = "id_curso"),
        Index(name = "idx_event_attendance_estado", columnList = "estado"),
    ],
)
class EventAttendanceEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 300)
    var titulo: String,
    @Column(columnDefinition = "text")
    var descricao: String? = null,
    @Column(name = "id_organizador", nullable = false)
    var idOrganizador: UUID,
    @Column(name = "id_curso")
    var idCurso: UUID? = null,
    @Column(name = "attendance_mode", nullable = false, length = 20)
    var attendanceMode: String,
    @Column(nullable = false, length = 20)
    var estado: String = "AGENDADO",
    @Column(name = "ch_creditadas", nullable = false)
    var chCreditadas: Double,
    @Column(name = "inicio_em", nullable = false)
    var inicioEm: OffsetDateTime,
    @Column(name = "fim_em", nullable = false)
    var fimEm: OffsetDateTime,
    @Column(name = "validation_windows", columnDefinition = "jsonb", nullable = false)
    @Type(JsonType::class)
    var validationWindows: List<Map<String, Any>> = emptyList(),
) : BaseEntity(id)

@Entity
@Table(
    name = "attendance_session",
    uniqueConstraints = [UniqueConstraint(name = "uk_attendance_evento_aluno", columnNames = ["id_evento", "id_aluno"])],
    indexes = [
        Index(name = "idx_attendance_evento", columnList = "id_evento"),
        Index(name = "idx_attendance_aluno", columnList = "id_aluno"),
    ],
)
class AttendanceSessionEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_evento", nullable = false)
    val idEvento: UUID,
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(name = "device_uuid", length = 100)
    var deviceUuid: String? = null,
    @Column(name = "entry_confirmed_at")
    var entryConfirmedAt: OffsetDateTime? = null,
    @Column(name = "exit_confirmed_at")
    var exitConfirmedAt: OffsetDateTime? = null,
) : BaseEntity(id)

@Entity
@Table(
    name = "certificate",
    indexes = [
        Index(name = "idx_certificate_aluno", columnList = "id_aluno"),
        Index(name = "idx_certificate_evento", columnList = "id_evento"),
        Index(name = "idx_certificate_hash", columnList = "hash_sha256", unique = true),
    ],
)
class CertificateEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(name = "id_evento")
    val idEvento: UUID? = null,
    @Column(nullable = false, length = 20)
    val origem: String = "EVENTO",
    @Column(name = "id_activity")
    val idActivity: UUID? = null,
    @Column(name = "hash_sha256", nullable = false, unique = true, length = 64)
    val hashSha256: String,
    @Column(name = "signature_ed25519", nullable = false, length = 200)
    val signatureEd25519: String,
    @Column(name = "storage_key", nullable = false, length = 500)
    val storageKey: String,
    @Column(name = "ch_creditadas", nullable = false)
    val chCreditadas: Double,
    @Column(name = "issued_at", nullable = false, updatable = false)
    val issuedAt: OffsetDateTime = OffsetDateTime.now(),
) : BaseEntity(id)
