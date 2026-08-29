package br.ufpr.sept.so2.modules.presenca.infrastructure.persistence

import br.ufpr.sept.so2.modules.presenca.application.ports.out.CertificateCardDto
import br.ufpr.sept.so2.modules.presenca.application.ports.out.EventCardDto
import br.ufpr.sept.so2.modules.presenca.application.ports.out.EventSearchHit
import br.ufpr.sept.so2.modules.presenca.application.ports.out.PresencaBffReadPort
import br.ufpr.sept.so2.modules.presenca.application.ports.out.PresencaDashboardPort
import br.ufpr.sept.so2.modules.presenca.application.ports.out.UpcomingEventHit
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class PresencaDashboardAdapter(
    private val eventRepo: EventAttendanceJpaRepository,
    private val certificateRepo: CertificateJpaRepository,
) : PresencaDashboardPort,
    PresencaBffReadPort {
    override fun findEmAndamento(limit: Int): List<EventCardDto> =
        eventRepo.findWithFilters(
            estado = "EM_ANDAMENTO",
            idOrganizador = null,
            idCurso = null,
            pageable = PageRequest.of(0, limit),
        ).content.map { it.toDto() }

    override fun findByOrganizador(organizadorId: UUID, limit: Int): List<EventCardDto> =
        eventRepo.findWithFilters(
            estado = null,
            idOrganizador = organizadorId,
            idCurso = null,
            pageable = PageRequest.of(0, limit),
        ).content.map { it.toDto() }

    override fun countEmAndamentoPorOrganizador(organizadorId: UUID): Long =
        eventRepo.countByEstadoAndIdOrganizador("EM_ANDAMENTO", organizadorId)

    override fun findCertificadosByAluno(alunoId: UUID): List<CertificateCardDto> =
        certificateRepo.findAllByIdAluno(alunoId).map { c ->
            CertificateCardDto(
                id = c.id,
                hashSha256 = c.hashSha256,
                issuedAt = c.issuedAt,
            )
        }

    override fun countByEstado(estado: String): Long =
        eventRepo.countByEstado(estado)

    override fun findUpcoming(
        from: OffsetDateTime,
        limit: Int,
    ): List<UpcomingEventHit> =
        eventRepo.findUpcoming(from, PageRequest.of(0, limit)).content.map { e ->
            UpcomingEventHit(id = e.id, titulo = e.titulo, inicioEm = e.inicioEm, estado = e.estado)
        }

    override fun searchByTitulo(
        q: String,
        page: Int,
        size: Int,
    ): List<EventSearchHit> =
        eventRepo.searchByTitulo(q, PageRequest.of(page, size)).content.map { e ->
            EventSearchHit(id = e.id, titulo = e.titulo, estado = e.estado)
        }

    private fun EventAttendanceEntity.toDto() =
        EventCardDto(
            id = id,
            titulo = titulo,
            estado = estado,
            chCreditadas = chCreditadas,
            inicioEm = inicioEm,
            fimEm = fimEm,
        )
}
