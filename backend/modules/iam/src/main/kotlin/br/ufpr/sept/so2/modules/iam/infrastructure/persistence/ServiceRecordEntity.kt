package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "service_record",
    indexes = [
        Index(name = "idx_service_record_aluno", columnList = "id_aluno"),
        Index(name = "idx_service_record_secretario", columnList = "id_secretario"),
    ],
)
class ServiceRecordEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_secretario")
    val idSecretario: UUID? = null,
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(nullable = false, length = 50)
    val tipo: String = "PRESENCIAL",
    @Column(nullable = false, length = 300)
    val assunto: String,
    @Column(columnDefinition = "text")
    val descricao: String? = null,
    @Column(nullable = false, length = 30)
    var estado: String = "PENDENTE_CIENCIA",
    @Column(name = "acknowledged_at")
    var acknowledgedAt: java.time.OffsetDateTime? = null,
) : BaseEntity(id)
