package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "graduation_record",
    indexes = [
        Index(name = "idx_graduation_aluno", columnList = "id_aluno"),
        Index(name = "idx_graduation_estado", columnList = "estado"),
    ],
)
class GraduationRecordEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(name = "id_curso")
    val idCurso: UUID? = null,
    @Column(name = "data_colacao")
    var dataColacao: LocalDate? = null,
    @Column(nullable = false, length = 30)
    var estado: String = "COLOCADO",
    @Column(name = "delivered_at")
    var deliveredAt: OffsetDateTime? = null,
    @Column(name = "delivered_by")
    var deliveredBy: UUID? = null,
    @Column(columnDefinition = "text")
    var observacao: String? = null,
    @Column(length = 40)
    var livro: String? = null,
    @Column(length = 40)
    var folha: String? = null,
    @Column(length = 80)
    var ata: String? = null,
    @Column(name = "id_periodo")
    var idPeriodo: UUID? = null,
    @Column(name = "diploma_storage_key", length = 500)
    var diplomaStorageKey: String? = null,
    @Column(name = "diploma_hash_sha256", length = 64)
    var diplomaHashSha256: String? = null,
) : BaseEntity(id)

interface GraduationRecordJpaRepository : JpaRepository<GraduationRecordEntity, UUID> {
    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<GraduationRecordEntity>

    fun findAllByIdAlunoIn(ids: Collection<UUID>): List<GraduationRecordEntity>

    fun existsByIdAluno(idAluno: UUID): Boolean

    @Query(
        value =
            """
            SELECT EXTRACT(YEAR FROM data_colacao)::int, COUNT(*)
            FROM graduation_record
            WHERE data_colacao IS NOT NULL
            GROUP BY 1 ORDER BY 1
            """,
        nativeQuery = true,
    )
    fun countByAnoColacao(): List<Array<Any>>
}

@Entity
@Table(
    name = "secretary_task",
    indexes = [Index(name = "idx_secretary_task_estado", columnList = "estado")],
)
class SecretaryTaskEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 200)
    var titulo: String,
    @Column(columnDefinition = "text")
    var descricao: String? = null,
    @Column(nullable = false, length = 30)
    var estado: String = "PENDENTE",
    @Column(name = "id_assignee")
    var idAssignee: UUID? = null,
    @Column(nullable = false, length = 20)
    var prioridade: String = "NORMAL",
    @Column(name = "prazo_em")
    var prazoEm: OffsetDateTime? = null,
) : BaseEntity(id)

interface SecretaryTaskJpaRepository : JpaRepository<SecretaryTaskEntity, UUID> {
    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<SecretaryTaskEntity>
}

@Entity
@Table(name = "import_job")
class ImportJobEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 40)
    val kind: String,
    @Column(nullable = false, length = 300)
    val filename: String,
    @Column(nullable = false, length = 30)
    var status: String = "VALIDATED",
    @Column(name = "total_rows", nullable = false)
    var totalRows: Int = 0,
    @Column(name = "success_count", nullable = false)
    var successCount: Int = 0,
    @Column(name = "error_count", nullable = false)
    var errorCount: Int = 0,
    @Column(name = "rows_payload", columnDefinition = "jsonb", nullable = false)
    @Type(JsonType::class)
    var rowsPayload: List<Map<String, Any?>> = emptyList(),
    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(JsonType::class)
    var errors: List<Map<String, Any?>> = emptyList(),
    @Column(name = "id_ator", nullable = false)
    val idAtor: UUID,
) : BaseEntity(id)

interface ImportJobJpaRepository : JpaRepository<ImportJobEntity, UUID>

@Entity
@Table(name = "export_job")
class ExportJobEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 40)
    val kind: String,
    @Column(nullable = false, length = 30)
    var status: String = "PROCESSANDO",
    @Column(nullable = false, length = 300)
    val filename: String,
    @Column(name = "storage_key", nullable = false, length = 400)
    var storageKey: String,
    @Column(name = "id_ator", nullable = false)
    val idAtor: UUID,
    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null,
    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = null,
) : BaseEntity(id)

interface ExportJobJpaRepository : JpaRepository<ExportJobEntity, UUID> {
    fun findAllByIdAtor(
        idAtor: UUID,
        pageable: Pageable,
    ): Page<ExportJobEntity>

    fun findAllByStatusAndExpiresAtBefore(
        status: String,
        before: OffsetDateTime,
    ): List<ExportJobEntity>

    fun findAllByStatus(status: String): List<ExportJobEntity>
}
