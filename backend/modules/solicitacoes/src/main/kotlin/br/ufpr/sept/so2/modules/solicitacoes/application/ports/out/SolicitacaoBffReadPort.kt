package br.ufpr.sept.so2.modules.solicitacoes.application.ports.out

import java.time.OffsetDateTime
import java.util.UUID

data class CountBucket(
    val key: String,
    val total: Long,
)

data class CursoCount(
    val cursoId: UUID,
    val total: Long,
)

data class SlaRequestHit(
    val id: UUID,
    val tipo: String,
    val prazoEm: OffsetDateTime?,
    val idCurso: UUID?,
)

data class CargaDeliberadorHit(
    val atorId: UUID,
    val deliberacoes: Long,
)

data class RequestSearchHit(
    val id: UUID,
    val tipo: String,
    val estado: String,
)

data class RequestExportRow(
    val id: UUID,
    val ano: Short,
    val numeroAnual: Int,
    val tipo: String,
    val estado: String,
)

interface SolicitacaoBffReadPort {
    fun countByTypeFiltered(
        cursoId: UUID?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
    ): List<CountBucket>

    fun countByEstadoFiltered(
        cursoId: UUID?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
    ): List<CountBucket>

    fun countByCurso(): List<CursoCount>

    fun countByMonth(
        cursoId: UUID?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
    ): List<CountBucket>

    fun countAbertas(cursoId: UUID?): Long

    fun avgDeliberationSeconds(cursoId: UUID?): Double

    fun findSlaAbertas(limit: Int): List<SlaRequestHit>

    fun countCargaPorDeliberador(): List<CargaDeliberadorHit>

    fun search(
        q: String,
        solicitanteId: UUID?,
        page: Int,
        size: Int,
    ): List<RequestSearchHit>

    fun listForExport(limit: Int): List<RequestExportRow>
}
