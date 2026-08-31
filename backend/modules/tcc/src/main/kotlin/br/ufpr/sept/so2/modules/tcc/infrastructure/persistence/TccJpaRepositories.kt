package br.ufpr.sept.so2.modules.tcc.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface TccJpaRepository : JpaRepository<TccEntity, UUID> {
    fun findAllByIdOrientador(
        idOrientador: UUID,
        pageable: Pageable,
    ): Page<TccEntity>

    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<TccEntity>

    fun countByEstado(estado: String): Long

    @Query(
        """
        SELECT t FROM TccEntity t
        JOIN TccMemberEntity m ON m.idTcc = t.id
        WHERE m.idAluno = :alunoId
    """,
    )
    fun findByAluno(
        @Param("alunoId") alunoId: UUID,
    ): List<TccEntity>
}

interface TccMemberJpaRepository : JpaRepository<TccMemberEntity, TccMemberId> {
    fun findAllByIdTcc(idTcc: UUID): List<TccMemberEntity>

    fun findAllByIdAluno(idAluno: UUID): List<TccMemberEntity>
}

interface TccExaminerJpaRepository : JpaRepository<TccExaminerEntity, TccExaminerId> {
    fun findAllByIdTcc(idTcc: UUID): List<TccExaminerEntity>
}
