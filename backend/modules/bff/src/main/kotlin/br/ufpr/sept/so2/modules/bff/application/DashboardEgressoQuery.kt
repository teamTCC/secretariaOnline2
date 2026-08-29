package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.bff.dto.DashboardEgressoLinks
import br.ufpr.sept.so2.modules.bff.dto.DashboardEgressoResponse
import br.ufpr.sept.so2.modules.comunicacao.application.ports.out.ComunicacaoDashboardPort
import br.ufpr.sept.so2.modules.formativas.application.ports.out.FormativaDashboardPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamDashboardPort
import br.ufpr.sept.so2.modules.presenca.application.ports.out.PresencaDashboardPort
import br.ufpr.sept.so2.modules.tcc.application.ports.out.TccDashboardPort
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DashboardEgressoQuery(
    private val iam: IamDashboardPort,
    private val tcc: TccDashboardPort,
    private val presenca: PresencaDashboardPort,
    private val comunicacao: ComunicacaoDashboardPort,
    private val formativas: FormativaDashboardPort,
    private val cacheManager: CacheManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(egressoId: UUID): DashboardEgressoResponse {
        val cacheKey = "egresso:$egressoId"
        val cache = cacheManager.getCache("bff-dashboard")

        @Suppress("UNCHECKED_CAST")
        cache?.get(cacheKey)?.get()?.let { return it as DashboardEgressoResponse }

        var degraded = false

        val tccsDefendidos =
            try {
                tcc.countDefendidosByAluno(egressoId)
            } catch (e: Exception) {
                log.warn("Dashboard egresso={}: falha ao carregar TCCs — {}", egressoId, e.message)
                degraded = true
                null
            }

        val result =
            DashboardEgressoResponse(
                nomeAluno = null,
                emailAluno = null,
                tccsDefendidos = tccsDefendidos,
                certificados = null,
                comunicados = null,
                links =
                    DashboardEgressoLinks(
                        self = "/bff/dashboard/egresso",
                        certificados = "/certificates/mine",
                        comunicados = "/communications/me",
                    ),
                _degraded = if (degraded) true else null,
            )

        if (!degraded) {
            cache?.put(cacheKey, result)
        }

        return result
    }
}
