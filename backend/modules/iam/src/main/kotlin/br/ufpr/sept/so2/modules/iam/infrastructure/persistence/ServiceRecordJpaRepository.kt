package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServiceRecordJpaRepository : JpaRepository<ServiceRecordEntity, UUID> {
    fun findAllByIdAluno(
        idAluno: UUID,
        pageable: Pageable,
    ): Page<ServiceRecordEntity>

    fun findAllByIdAlunoAndEstado(
        idAluno: UUID,
        estado: String,
        pageable: Pageable,
    ): Page<ServiceRecordEntity>

    fun findAllByIdSecretario(
        idSecretario: UUID,
        pageable: Pageable,
    ): Page<ServiceRecordEntity>

    fun countByIdAlunoAndEstado(
        idAluno: UUID,
        estado: String,
    ): Long
}
