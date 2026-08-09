package br.ufpr.sept.so2.modules.formativas.infrastructure.persistence

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
    name = "formative_activity",
    indexes = [
        Index(name = "idx_formative_activity_aluno", columnList = "id_aluno"),
        Index(name = "idx_formative_activity_estado", columnList = "estado"),
    ],
)
class FormativeActivityEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(nullable = false, length = 200)
    var titulo: String,
    @Column(columnDefinition = "text")
    var descricao: String? = null,
    @Column(nullable = false, length = 50)
    var categoria: String,
    @Column(name = "carga_horaria", nullable = false)
    var cargaHoraria: Double,
    @Column(name = "data_realizacao", nullable = false)
    var dataRealizacao: LocalDate,
    @Column(nullable = false, length = 20)
    var estado: String = "PENDENTE",
    @Column(name = "parecer_revisor", columnDefinition = "text")
    var parecerRevisor: String? = null,
    @Column(name = "id_revisor")
    var idRevisor: UUID? = null,
    @Column(name = "storage_key_comprovante", length = 500)
    var storageKeyComprovante: String? = null,
) : BaseEntity(id)

@Entity
@Table(
    name = "formative_entry",
    indexes = [Index(name = "idx_formative_entry_aluno", columnList = "id_aluno")],
)
class FormativeEntryEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(name = "id_activity")
    val idActivity: UUID? = null,
    @Column(name = "id_evento")
    val idEvento: UUID? = null,
    @Column(name = "horas_aprovadas", nullable = false)
    var horasAprovadas: Double,
    @Column(name = "aprovado_em", nullable = false)
    val aprovadoEm: OffsetDateTime = OffsetDateTime.now(),
) : BaseEntity(id)
