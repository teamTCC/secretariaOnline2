package br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence

import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.CargaDeliberadorHit
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.CountBucket
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.CursoCount
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.RequestCardDto
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.RequestExportRow
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.RequestSearchHit
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SlaRequestHit
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoBffReadPort
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoDashboardPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class SolicitacaoDashboardAdapter(
    private val requestRepo: RequestJpaRepository,
    private val requestEventRepo: RequestEventJpaRepository,
) : SolicitacaoDashboardPort,
    SolicitacaoBffReadPort {
    override fun findPendenciasAluno(alunoId: UUID, limit: Int): List<RequestCardDto> =
        requestRepo.findWithFilters(
            estado = "EM_AJUSTE",
            idSolicitante = alunoId,
            idCurso = null,
            typeCode = null,
            pageable = PageRequest.of(0, limit),
        ).content.map { it.toDto() }

    override fun findRecentesAluno(alunoId: UUID, limit: Int): List<RequestCardDto> =
        requestRepo.findWithFilters(
            estado = null,
            idSolicitante = alunoId,
            idCurso = null,
            typeCode = null,
            pageable = PageRequest.of(0, limit),
        ).content.map { it.toDto() }

    override fun findPendentesDeliberacao(limit: Int): List<RequestCardDto> =
        requestRepo.findWithFilters(
            estado = "EM_DELIBERACAO",
            idSolicitante = null,
            idCurso = null,
            typeCode = null,
            pageable = PageRequest.of(0, limit),
        ).content.map { it.toDto() }

    override fun countByEstado(estado: String): Long =
        requestRepo.countByEstado(estado)

    override fun countByTypeFiltered(
        cursoId: UUID?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
    ): List<CountBucket> =
        requestRepo.countGroupedByTypeFiltered(cursoId, from, to).map { it.toBucket() }

    override fun countByEstadoFiltered(
        cursoId: UUID?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
    ): List<CountBucket> =
        requestRepo.countGroupedByEstadoFiltered(cursoId, from, to).map { it.toBucket() }

    override fun countByCurso(): List<CursoCount> =
        requestRepo.countGroupedByCurso().map { row ->
            CursoCount(cursoId = row[0] as UUID, total = (row[1] as Number).toLong())
        }

    override fun countByMonth(
        cursoId: UUID?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
    ): List<CountBucket> =
        requestRepo.countByMonth(cursoId, from, to).map { it.toBucket() }

    override fun countAbertas(cursoId: UUID?): Long =
        if (cursoId != null) {
            requestRepo.countByEstadoAndIdCurso("ABERTA", cursoId)
        } else {
            requestRepo.countByEstado("ABERTA")
        }

    override fun avgDeliberationSeconds(cursoId: UUID?): Double =
        requestRepo.avgDeliberationSecondsFiltered(cursoId)?.toDouble() ?: 0.0

    override fun findSlaAbertas(limit: Int): List<SlaRequestHit> =
        requestRepo.findTop10ByEstadoAndPrazoEmIsNotNullOrderByPrazoEmAsc("ABERTA")
            .take(limit)
            .map { r ->
                SlaRequestHit(id = r.id, tipo = r.requestTypeCode, prazoEm = r.prazoEm, idCurso = r.idCurso)
            }

    override fun countCargaPorDeliberador(): List<CargaDeliberadorHit> =
        requestEventRepo.countCargaPorDeliberador().map { row ->
            CargaDeliberadorHit(
                atorId = UUID.fromString(row[0].toString()),
                deliberacoes = (row[1] as Number).toLong(),
            )
        }

    override fun search(
        q: String,
        solicitanteId: UUID?,
        page: Int,
        size: Int,
    ): List<RequestSearchHit> {
        val pageable = PageRequest.of(page, size)
        val page =
            if (solicitanteId == null) {
                requestRepo.searchByQ(q, pageable)
            } else {
                requestRepo.searchByQAndSolicitante(q, solicitanteId, pageable)
            }
        return page.content.map { r ->
            RequestSearchHit(id = r.id, tipo = r.requestTypeCode, estado = r.estado)
        }
    }

    override fun listForExport(limit: Int): List<RequestExportRow> =
        requestRepo.findWithFilters(null, null, null, null, PageRequest.of(0, limit)).content.map { r ->
            RequestExportRow(
                id = r.id,
                ano = r.ano,
                numeroAnual = r.numeroAnual,
                tipo = r.requestTypeCode,
                estado = r.estado,
            )
        }

    private fun RequestEntity.toDto() =
        RequestCardDto(
            id = id,
            tipo = requestTypeCode,
            estado = estado,
            prazoEm = prazoEm,
            createdAt = createdAt,
        )

    private fun Array<Any>.toBucket() =
        CountBucket(key = this[0].toString(), total = (this[1] as Number).toLong())
}
