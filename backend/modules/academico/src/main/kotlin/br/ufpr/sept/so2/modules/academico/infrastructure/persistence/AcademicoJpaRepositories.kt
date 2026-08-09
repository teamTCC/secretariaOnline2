package br.ufpr.sept.so2.modules.academico.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface CursoJpaRepository : JpaRepository<CursoEntity, UUID> {
    fun findBySigla(sigla: String): Optional<CursoEntity>

    fun findAllByAtivoTrue(): List<CursoEntity>
}

interface DisciplinaJpaRepository : JpaRepository<DisciplinaEntity, UUID> {
    fun findAllByIdCursoAndAtivaTrue(idCurso: UUID): List<DisciplinaEntity>

    fun findByIdCursoAndCodigo(
        idCurso: UUID,
        codigo: String,
    ): Optional<DisciplinaEntity>

    @Query(
        "SELECT d FROM DisciplinaEntity d WHERE d.idCurso = :cursoId AND (:search IS NULL OR LOWER(d.nome) LIKE LOWER(CONCAT('%', :search, '%')))",
    )
    fun searchByCurso(
        @Param("cursoId") cursoId: UUID,
        @Param("search") search: String?,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<DisciplinaEntity>
}

interface PeriodoLetivoJpaRepository : JpaRepository<PeriodoLetivoEntity, UUID> {
    fun findByAnoAndSemestre(
        ano: Short,
        semestre: Short,
    ): Optional<PeriodoLetivoEntity>

    fun findFirstByAtivoTrueOrderByAnoDescSemestreDesc(): Optional<PeriodoLetivoEntity>
}

interface CalendarioAcademicoJpaRepository : JpaRepository<CalendarioAcademicoEntity, UUID> {
    fun findAllByIdPeriodoLetivo(idPeriodoLetivo: UUID): List<CalendarioAcademicoEntity>
}
