package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.bff.dto.DashboardAlunoKpis
import br.ufpr.sept.so2.modules.bff.dto.DashboardAlunoLinks
import br.ufpr.sept.so2.modules.bff.dto.DashboardAlunoResponse
import br.ufpr.sept.so2.modules.bff.dto.EventoItem
import br.ufpr.sept.so2.modules.bff.dto.HorasFormativasKpi
import br.ufpr.sept.so2.modules.bff.dto.PendenciaItem
import br.ufpr.sept.so2.modules.bff.dto.SolicitacaoItem
import br.ufpr.sept.so2.modules.formativas.application.ports.out.FormativaDashboardPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamDashboardPort
import br.ufpr.sept.so2.modules.presenca.application.ports.out.PresencaDashboardPort
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoDashboardPort
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DashboardAlunoQuery(
    private val solicitacoes: SolicitacaoDashboardPort,
    private val presenca: PresencaDashboardPort,
    private val formativas: FormativaDashboardPort,
    private val iam: IamDashboardPort,
    private val cacheManager: CacheManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(alunoId: UUID, authorities: Set<String>): DashboardAlunoResponse {
        val cacheKey = "aluno:$alunoId"
        val cache = cacheManager.getCache("bff-dashboard")

        cache?.get(cacheKey, DashboardAlunoResponse::class.java)?.let { return it }

        var degraded = false

        val pendencias =
            try {
                solicitacoes.findPendenciasAluno(alunoId, 3).map { r ->
                    PendenciaItem(
                        id = r.id,
                        tipo = r.tipo,
                        estado = r.estado,
                        prazoEm = r.prazoEm,
                        acao = "REENVIAR",
                        link = "/requests/${r.id}",
                    )
                }
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar pendencias — {}", alunoId, e.message)
                degraded = true
                null
            }

        val eventos =
            try {
                presenca.findEmAndamento(3).map { e ->
                    EventoItem(
                        id = e.id,
                        titulo = e.titulo,
                        chCreditadas = e.chCreditadas,
                        fimEm = e.fimEm,
                        link = "/events/${e.id}/attendance/session",
                    )
                }
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar eventos — {}", alunoId, e.message)
                degraded = true
                null
            }

        val horasRequeridas = 120.0
        val horasAprovadas =
            try {
                formativas.sumHorasAprovadas(alunoId)
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar horas formativas — {}", alunoId, e.message)
                degraded = true
                0.0
            }

        val ultimasSolicitacoes =
            try {
                solicitacoes.findRecentesAluno(alunoId, 5).map { r ->
                    SolicitacaoItem(
                        id = r.id,
                        tipo = r.tipo,
                        estado = r.estado,
                        createdAt = r.createdAt,
                    )
                }
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar ultimas solicitacoes — {}", alunoId, e.message)
                degraded = true
                null
            }

        val atendimentosPendentes =
            try {
                iam.countAtendimentosPendentes(alunoId)
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao contar atendimentos — {}", alunoId, e.message)
                degraded = true
                null
            }

        val result = DashboardAlunoResponse(
            kpis = DashboardAlunoKpis(
                horasFormativas = HorasFormativasKpi(
                    atual = horasAprovadas,
                    requerido = horasRequeridas,
                    percentual = (horasAprovadas / horasRequeridas * 100).coerceAtMost(100.0),
                ),
                atendimentosPendentes = atendimentosPendentes,
            ),
            pendencias = pendencias,
            eventos = eventos,
            ultimasSolicitacoes = ultimasSolicitacoes,
            links = DashboardAlunoLinks(
                self = "/bff/dashboard/aluno",
                novaSolicitacao = if ("request.open" in authorities) "/requests/types" else null,
                formativas = "/formativas/minhas",
                eventos = "/events?audience=me",
            ),
            _degraded = if (degraded) true else null,
        )

        if (!degraded) {
            cache?.put(cacheKey, result)
        }

        return result
    }
}
