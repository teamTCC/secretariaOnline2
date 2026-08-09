package br.ufpr.sept.so2.modules.estagio.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "internship",
    indexes = [
        Index(name = "idx_internship_aluno", columnList = "id_aluno"),
        Index(name = "idx_internship_estado", columnList = "estado"),
    ],
)
class InternshipEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(name = "id_supervisor")
    var idSupervisor: UUID? = null,
    @Column(name = "empresa", nullable = false, length = 200)
    var empresa: String,
    @Column(name = "cargo", nullable = false, length = 100)
    var cargo: String,
    @Column(name = "carga_horaria_semanal", nullable = false)
    var cargaHorariaSemanal: Int,
    @Column(name = "inicio", nullable = false)
    var inicio: LocalDate,
    @Column(name = "fim")
    var fim: LocalDate? = null,
    @Column(nullable = false, length = 20)
    var estado: String = "EM_ANDAMENTO",
    @Column(name = "observacoes", columnDefinition = "text")
    var observacoes: String? = null,
) : BaseEntity(id)

@Entity
@Table(
    name = "internship_document",
    indexes = [Index(name = "idx_internship_doc_internship", columnList = "id_internship")],
)
class InternshipDocumentEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_internship", nullable = false)
    val idInternship: UUID,
    @Column(nullable = false, length = 50)
    val tipo: String,
    @Column(name = "storage_key", nullable = false, length = 500)
    val storageKey: String,
    @Column(name = "sha256", nullable = false, length = 64)
    val sha256: String,
    @Column(name = "nome_original", nullable = false, length = 300)
    val nomeOriginal: String,
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    val uploadedAt: OffsetDateTime = OffsetDateTime.now(),
) : BaseEntity(id)
