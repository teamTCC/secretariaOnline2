package br.ufpr.sept.so2.modules.formativas.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FormativeActivityJpaRepository : JpaRepository<FormativeActivityEntity, UUID> {
    fun findAllByIdAluno(
        idAluno: UUID,
        pageable: Pageable,
    ): Page<FormativeActivityEntity>

    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<FormativeActivityEntity>
}

interface FormativeEntryJpaRepository : JpaRepository<FormativeEntryEntity, UUID> {
    fun findAllByIdAluno(idAluno: UUID): List<FormativeEntryEntity>

    @Query("SELECT COALESCE(SUM(e.horasAprovadas), 0) FROM FormativeEntryEntity e WHERE e.idAluno = :alunoId")
    fun sumHorasAprovadas(
        @Param("alunoId") alunoId: UUID,
    ): Double
}
