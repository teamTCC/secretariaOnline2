package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.bff.dto.DashboardProfessorLinks
import br.ufpr.sept.so2.modules.bff.dto.DashboardProfessorResponse
import br.ufpr.sept.so2.modules.bff.dto.EventoOrganizadorItem
import br.ufpr.sept.so2.modules.bff.dto.ProfessorPendenciaItem
import br.ufpr.sept.so2.modules.presenca.application.ports.out.PresencaDashboardPort
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoDashboardPort
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DashboardProfessorQuery(
    private val solicitacoes: SolicitacaoDashboardPort,
    private val presenca: PresencaDashboardPort,
    private val cacheManager: CacheManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(professorId: UUID): DashboardProfessorResponse {
        val cacheKey = "professor:$professorId"
        val cache = cacheManager.getCache("bff-dashboard")

        cache?.get(cacheKey, DashboardProfessorResponse::class.java)?.let { return it }

        var degraded = false

        val meusEventos =
            try {
                presenca.findByOrganizador(professorId, 5).map { e ->
                    EventoOrganizadorItem(
                        id = e.id,
                        titulo = e.titulo,
                        estado = e.estado,
                        inicioEm = e.inicioEm,
                        fimEm = e.fimEm,
                    )
                }
            } catch (e: Exception) {
                log.warn("Dashboard professor={}: falha ao carregar eventos — {}", professorId, e.message)
                degraded = true
                null
            }

        val solicitacoesPendentes =
            try {
                solicitacoes.findPendentesDeliberacao(5).map { r ->
                    ProfessorPendenciaItem(
                        id = r.id,
                        tipo = r.tipo,
                        prazoEm = r.prazoEm,
                        link = "/requests/${r.id}",
                    )
                }
            } catch (e: Exception) {
                log.warn("Dashboard professor={}: falha ao carregar solicitacoes — {}", professorId, e.message)
                degraded = true
                null
            }

        val result = DashboardProfessorResponse(
            meusEventos = meusEventos,
            solicitacoesPendentes = solicitacoesPendentes,
            links = DashboardProfessorLinks(
                self = "/bff/dashboard/professor",
                novoEvento = "/events",
                meusEventos = "/events?host=me",
            ),
            _degraded = if (degraded) true else null,
        )

        if (!degraded) {
            cache?.put(cacheKey, result)
        }

        return result
    }
}
