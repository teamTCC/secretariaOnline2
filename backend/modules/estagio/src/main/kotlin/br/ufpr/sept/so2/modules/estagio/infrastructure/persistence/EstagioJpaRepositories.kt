package br.ufpr.sept.so2.modules.estagio.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InternshipJpaRepository : JpaRepository<InternshipEntity, UUID> {
    fun findAllByIdAluno(
        idAluno: UUID,
        pageable: Pageable,
    ): Page<InternshipEntity>

    fun findAllByIdSupervisor(
        idSupervisor: UUID,
        pageable: Pageable,
    ): Page<InternshipEntity>

    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<InternshipEntity>
}

interface InternshipDocumentJpaRepository : JpaRepository<InternshipDocumentEntity, UUID> {
    fun findAllByIdInternship(idInternship: UUID): List<InternshipDocumentEntity>
}
