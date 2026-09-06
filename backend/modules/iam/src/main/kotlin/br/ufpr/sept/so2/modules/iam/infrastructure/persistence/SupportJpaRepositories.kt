package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FaqItemJpaRepository : JpaRepository<FaqItemEntity, UUID> {
    fun findAllByAtivoOrderByOrdemAsc(ativo: Boolean): List<FaqItemEntity>

    fun findAllByCategoriaAndAtivoOrderByOrdemAsc(
        categoria: String,
        ativo: Boolean,
    ): List<FaqItemEntity>
}

interface SupportTicketJpaRepository : JpaRepository<SupportTicketEntity, UUID> {
    fun findAllByIdUsuario(
        idUsuario: UUID,
        pageable: Pageable,
    ): Page<SupportTicketEntity>

    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<SupportTicketEntity>
}
