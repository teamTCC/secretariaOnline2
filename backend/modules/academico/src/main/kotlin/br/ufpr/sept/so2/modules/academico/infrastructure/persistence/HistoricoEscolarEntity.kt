package br.ufpr.sept.so2.modules.academico.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

@Entity
@Table(
    name = "historico_escolar",
    uniqueConstraints = [UniqueConstraint(name = "uk_historico_aluno_disc", columnNames = ["id_aluno", "id_disciplina"])],
    indexes = [Index(name = "idx_historico_aluno", columnList = "id_aluno")],
)
class HistoricoEscolarEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_aluno", nullable = false)
    val idAluno: UUID,
    @Column(name = "id_disciplina", nullable = false)
    val idDisciplina: UUID,
    @Column(nullable = false, length = 20)
    var estado: String = "CURSANDO",
) : BaseEntity(id)

interface HistoricoEscolarJpaRepository : JpaRepository<HistoricoEscolarEntity, UUID> {
    fun findAllByIdAluno(idAluno: UUID): List<HistoricoEscolarEntity>

    fun findByIdAlunoAndIdDisciplina(
        idAluno: UUID,
        idDisciplina: UUID,
    ): Optional<HistoricoEscolarEntity>

    fun countByIdAlunoAndEstado(
        idAluno: UUID,
        estado: String,
    ): Long
}
