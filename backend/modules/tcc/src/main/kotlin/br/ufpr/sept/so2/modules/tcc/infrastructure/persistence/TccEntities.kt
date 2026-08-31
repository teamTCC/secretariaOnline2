package br.ufpr.sept.so2.modules.tcc.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "tcc",
    indexes = [
        Index(name = "idx_tcc_orientador", columnList = "id_orientador"),
        Index(name = "idx_tcc_estado", columnList = "estado"),
    ],
)
class TccEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_orientador", nullable = false)
    var idOrientador: UUID,
    @Column(nullable = false, length = 300)
    var titulo: String,
    @Column(name = "id_curso", nullable = false)
    var idCurso: UUID,
    @Column(nullable = false, length = 20)
    var estado: String = "EM_ANDAMENTO",
    @Column(name = "data_defesa")
    var dataDefesa: LocalDate? = null,
    @Column(name = "storage_key_pdf", length = 500)
    var storageKeyPdf: String? = null,
    @Column(name = "hash_sha256_pdf", length = 64)
    var hashSha256Pdf: String? = null,
    @Column(name = "nota_final")
    var notaFinal: Double? = null,
    @Column(name = "aprovado")
    var aprovado: Boolean? = null,
) : BaseEntity(id)

data class TccMemberId(
    val idTcc: UUID = UUID(0, 0),
    val idAluno: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "tcc_member")
@IdClass(TccMemberId::class)
class TccMemberEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val idTcc: UUID,
    @Id
    @Column(name = "id_aluno", columnDefinition = "uuid")
    val idAluno: UUID,
    @Column(name = "papel", nullable = false, length = 20)
    val papel: String = "AUTOR",
    @Column(name = "joined_at", nullable = false, updatable = false)
    val joinedAt: OffsetDateTime = OffsetDateTime.now(),
) : Serializable

data class TccExaminerId(
    val idTcc: UUID = UUID(0, 0),
    val idProfessor: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "tcc_examiner")
@IdClass(TccExaminerId::class)
class TccExaminerEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val idTcc: UUID,
    @Id
    @Column(name = "id_professor", columnDefinition = "uuid")
    val idProfessor: UUID,
    @Column(name = "papel", nullable = false, length = 30)
    val papel: String = "BANCA",
    @Column(name = "nota")
    var nota: Double? = null,
) : Serializable
