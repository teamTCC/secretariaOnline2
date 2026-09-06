package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.bff.dto.AcademicoSummaryResponse
import br.ufpr.sept.so2.modules.estagio.application.ports.out.EstagioSummaryPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamDashboardPort
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoDashboardPort
import br.ufpr.sept.so2.modules.tcc.application.ports.out.TccDashboardPort
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class AcademicoSummaryQuery(
    private val iam: IamDashboardPort,
    private val tcc: TccDashboardPort,
    private val estagio: EstagioSummaryPort,
    private val solicitacoes: SolicitacaoDashboardPort,
    private val cacheManager: CacheManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(): AcademicoSummaryResponse {
        val cacheKey = "academico:summary"
        val cache = cacheManager.getCache("bff-dashboard")

        cache?.get(cacheKey, AcademicoSummaryResponse::class.java)?.let { return it }

        var degraded = false

        fun <T> safely(name: String, block: () -> T): T? =
            try {
                block()
            } catch (e: Exception) {
                log.warn("AcademicoSummary: falha ao carregar {} — {}", name, e.message)
                degraded = true
                null
            }

        val result = AcademicoSummaryResponse(
            totalAlunos = safely("totalAlunos") { iam.countAlunosAtivos() },
            tccEmAndamento = safely("tccEmAndamento") { tcc.countByEstado("EM_ANDAMENTO") },
            estagiosAtivos = safely("estagiosAtivos") { estagio.countByEstado("EM_ANDAMENTO") },
            solicitacoesAbertas = safely("solicitacoesAbertas") { solicitacoes.countByEstado("ABERTA") },
            _degraded = if (degraded) true else null,
        )

        if (!degraded) {
            cache?.put(cacheKey, result)
        }

        return result
    }
}
