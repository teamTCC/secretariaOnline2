package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.bff.dto.DashboardSecretariaKpis
import br.ufpr.sept.so2.modules.bff.dto.DashboardSecretariaLinks
import br.ufpr.sept.so2.modules.bff.dto.DashboardSecretariaResponse
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoDashboardPort
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class DashboardSecretariaQuery(
    private val solicitacoes: SolicitacaoDashboardPort,
    private val cacheManager: CacheManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Cache key "secretaria:static" is intentional: global counts are the same
    // for all secretaries — no need to partition by user.
    fun execute(): DashboardSecretariaResponse {
        val cacheKey = "secretaria:static"
        val cache = cacheManager.getCache("bff-dashboard")

        cache?.get(cacheKey, DashboardSecretariaResponse::class.java)?.let { return it }

        var degraded = false

        val emTriagem =
            try {
                solicitacoes.countByEstado("ABERTA")
            } catch (e: Exception) {
                log.warn("Dashboard secretaria: falha ao carregar emTriagem — {}", e.message)
                degraded = true
                null
            }

        val emDeliberacao =
            try {
                solicitacoes.countByEstado("EM_DELIBERACAO")
            } catch (e: Exception) {
                log.warn("Dashboard secretaria: falha ao carregar emDeliberacao — {}", e.message)
                degraded = true
                null
            }

        val result = DashboardSecretariaResponse(
            kpis = DashboardSecretariaKpis(
                emTriagem = emTriagem,
                emDeliberacao = emDeliberacao,
            ),
            links = DashboardSecretariaLinks(
                self = "/bff/dashboard/secretaria",
                solicitacoes = "/requests",
                usuarios = "/usuarios",
            ),
            _degraded = if (degraded) true else null,
        )

        if (!degraded) {
            cache?.put(cacheKey, result)
        }

        return result
    }
}
