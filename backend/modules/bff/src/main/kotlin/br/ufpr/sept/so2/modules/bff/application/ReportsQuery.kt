package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.academico.application.ports.out.AcademicoReadPort
import br.ufpr.sept.so2.modules.bff.dto.CargaDeliberadorItem
import br.ufpr.sept.so2.modules.bff.dto.ColacaoAnoItem
import br.ufpr.sept.so2.modules.bff.dto.CoordinatorKpis
import br.ufpr.sept.so2.modules.bff.dto.CoordinatorReportResponse
import br.ufpr.sept.so2.modules.bff.dto.CursoRankingItem
import br.ufpr.sept.so2.modules.bff.dto.EstadoSolicitacaoItem
import br.ufpr.sept.so2.modules.bff.dto.EventoProximoItem
import br.ufpr.sept.so2.modules.bff.dto.EvolucaoMensalItem
import br.ufpr.sept.so2.modules.bff.dto.FormativaCategoriaItem
import br.ufpr.sept.so2.modules.bff.dto.PendenciaSlaItem
import br.ufpr.sept.so2.modules.bff.dto.ReportFilters
import br.ufpr.sept.so2.modules.bff.dto.SecretaryKpis
import br.ufpr.sept.so2.modules.bff.dto.SecretaryReportResponse
import br.ufpr.sept.so2.modules.bff.dto.TipoSolicitacaoItem
import br.ufpr.sept.so2.modules.estagio.application.ports.out.EstagioSummaryPort
import br.ufpr.sept.so2.modules.formativas.application.ports.out.FormativaBffReadPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamBffReadPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamDashboardPort
import br.ufpr.sept.so2.modules.presenca.application.ports.out.PresencaBffReadPort
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoBffReadPort
import br.ufpr.sept.so2.modules.tcc.application.ports.out.TccDashboardPort
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class ReportsQuery(
    private val iam: IamDashboardPort,
    private val iamRead: IamBffReadPort,
    private val requests: SolicitacaoBffReadPort,
    private val tcc: TccDashboardPort,
    private val estagio: EstagioSummaryPort,
    private val formativas: FormativaBffReadPort,
    private val presenca: PresencaBffReadPort,
    private val academico: AcademicoReadPort,
) {
    fun secretary(
        periodo: String?,
        curso: String?,
    ): SecretaryReportResponse {
        val (cursoId, from, to) = resolveFilters(periodo, curso)

        return SecretaryReportResponse(
            filtros = ReportFilters(periodo = periodo, curso = curso, cursoId = cursoId),
            kpis =
                SecretaryKpis(
                    alunosAtivos = iam.countAlunosAtivos(),
                    egressos = iamRead.countByRoleCode("EGRESSO"),
                    solicitacoesAbertas = requests.countAbertas(cursoId),
                    eventosAgendados = presenca.countByEstado("AGENDADO"),
                ),
            solicitacoesPorTipo =
                requests.countByTypeFiltered(cursoId, from, to).map { TipoSolicitacaoItem(it.key, it.total) },
            distribuicaoPorEstado =
                requests.countByEstadoFiltered(cursoId, from, to).map { EstadoSolicitacaoItem(it.key, it.total) },
            evolucaoTemporal =
                requests.countByMonth(cursoId, from, to).map { EvolucaoMensalItem(it.key, it.total) },
            rankingCursos =
                requests.countByCurso().map { row ->
                    CursoRankingItem(
                        cursoId = row.cursoId,
                        sigla = academico.findSigla(row.cursoId) ?: row.cursoId.toString(),
                        total = row.total,
                    )
                },
        )
    }

    fun coordinator(
        periodo: String?,
        curso: String?,
    ): CoordinatorReportResponse {
        val (cursoId, from, to) = resolveFilters(periodo, curso)

        val estados = requests.countByEstadoFiltered(cursoId, from, to).associate { it.key to it.total }
        val deferidas = estados["DEFERIDA"] ?: 0L
        val indeferidas = estados["INDEFERIDA"] ?: 0L
        val decididas = deferidas + indeferidas
        val taxaIndeferimento = if (decididas == 0L) 0.0 else indeferidas.toDouble() / decididas

        return CoordinatorReportResponse(
            filtros = ReportFilters(periodo = periodo, curso = curso, cursoId = cursoId),
            kpis =
                CoordinatorKpis(
                    tempoMedioDeliberacaoSegundos = requests.avgDeliberationSeconds(cursoId),
                    taxaIndeferimento = taxaIndeferimento,
                    thresholdIndeferimento = 0.3,
                    volumeFormativas = formativas.countByEstado("APROVADA"),
                    taxaPresencaEventosAgendados = presenca.countByEstado("CONCLUIDO"),
                    tccEmAndamento = tcc.countByEstado("EM_ANDAMENTO"),
                    estagiosSemSupervisor = estagio.countSemSupervisor(),
                ),
            seriesFormativas =
                formativas.countAprovadasByCategoria().map {
                    FormativaCategoriaItem(it.categoria, it.total)
                },
            evasaoPorPeriodo =
                iamRead.countColacoesByAno().map { ColacaoAnoItem(it.ano, it.colacoes) },
            cargaPorDeliberador =
                requests.countCargaPorDeliberador().map { row ->
                    CargaDeliberadorItem(
                        idAtor = row.atorId,
                        nome = iamRead.findNome(row.atorId) ?: row.atorId.toString(),
                        deliberacoes = row.deliberacoes,
                    )
                },
            pendencias =
                requests.findSlaAbertas(10)
                    .filter { cursoId == null || it.idCurso == cursoId }
                    .map { r ->
                        PendenciaSlaItem(id = r.id, tipo = r.tipo, prazoEm = r.prazoEm, href = "/requests/${r.id}")
                    },
            proximosEventos =
                presenca.findUpcoming(OffsetDateTime.now(), 10).map { e ->
                    EventoProximoItem(
                        id = e.id,
                        titulo = e.titulo,
                        inicioEm = e.inicioEm,
                        estado = e.estado,
                        href = "/events/${e.id}",
                    )
                },
            links = mapOf("self" to "/reports/coordinator", "curso" to "/academico/relatorios/curso"),
        )
    }

    private fun resolveFilters(
        periodo: String?,
        curso: String?,
    ): Triple<UUID?, OffsetDateTime?, OffsetDateTime?> {
        val cursoId =
            curso?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching { UUID.fromString(raw) }.getOrNull()
                    ?: academico.findCursoIdBySigla(raw)
            }
        var from: OffsetDateTime? = null
        var to: OffsetDateTime? = null
        if (!periodo.isNullOrBlank()) {
            val parts = periodo.split("-")
            if (parts.size == 2) {
                val ano = parts[0].toShortOrNull()
                val sem = parts[1].toShortOrNull()
                if (ano != null && sem != null) {
                    val window = academico.findPeriodoWindow(ano, sem)
                    if (window != null) {
                        from = window.inicio.atStartOfDay().atOffset(ZoneOffset.UTC)
                        to = window.fim.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
                    } else {
                        val month = if (sem == 1.toShort()) 1 else 7
                        val start = LocalDate.of(ano.toInt(), month, 1)
                        from = start.atStartOfDay().atOffset(ZoneOffset.UTC)
                        to = start.plusMonths(6).atStartOfDay().atOffset(ZoneOffset.UTC)
                    }
                }
            }
        }
        return Triple(cursoId, from, to)
    }
}
