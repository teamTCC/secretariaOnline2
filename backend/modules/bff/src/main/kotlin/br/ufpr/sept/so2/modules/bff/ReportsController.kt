package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoJpaRepository
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@RestController
@RequestMapping("/reports")
@Tag(name = "BFF — Relatórios", description = "Estatísticas da secretaria e relatórios analíticos de coordenação")
class ReportsController(
    private val usuarioRepo: UsuarioJpaRepository,
    private val requestRepo: RequestJpaRepository,
    private val requestEventRepo: RequestEventJpaRepository,
    private val tccRepo: TccJpaRepository,
    private val internshipRepo: InternshipJpaRepository,
    private val formativeRepo: FormativeActivityJpaRepository,
    private val eventRepo: EventAttendanceJpaRepository,
    private val cursoRepo: CursoJpaRepository,
    private val periodoRepo: PeriodoLetivoJpaRepository,
    private val graduationRepo: GraduationRecordJpaRepository,
) {
    @GetMapping("/secretary")
    @PreAuthorize("hasAuthority('report.view_secretary') or hasAuthority('system.admin')")
    @Operation(summary = "Estatísticas da secretaria — 4 datasets agregados")
    fun secretary(
        @RequestParam(required = false) periodo: String?,
        @RequestParam(required = false) curso: String?,
    ): Map<String, Any?> {
        val (cursoId, from, to) = resolveFilters(periodo, curso)
        val solicitacoesPorTipo =
            requestRepo.countGroupedByTypeFiltered(cursoId, from, to).map { row ->
                mapOf("tipo" to row[0].toString(), "total" to (row[1] as Number).toLong())
            }
        val distribuicaoPorEstado =
            requestRepo.countGroupedByEstadoFiltered(cursoId, from, to).map { row ->
                mapOf("estado" to row[0].toString(), "total" to (row[1] as Number).toLong())
            }
        val rankingCursos =
            requestRepo.countGroupedByCurso().map { row ->
                val id = row[0] as UUID
                val sigla = cursoRepo.findById(id).map { it.sigla }.orElse(id.toString())
                mapOf("cursoId" to id, "sigla" to sigla, "total" to (row[1] as Number).toLong())
            }
        val evolucao =
            requestRepo.countByMonth(cursoId, from, to).map { row ->
                mapOf("mes" to row[0].toString(), "total" to (row[1] as Number).toLong())
            }
        return mapOf(
            "filtros" to mapOf("periodo" to periodo, "curso" to curso, "cursoId" to cursoId),
            "kpis" to
                mapOf(
                    "alunosAtivos" to usuarioRepo.countByAtivoTrueAndGrrIsNotNull(),
                    "egressos" to usuarioRepo.countByRoleCode("EGRESSO"),
                    "solicitacoesAbertas" to
                        if (cursoId != null) {
                            requestRepo.countByEstadoAndIdCurso("ABERTA", cursoId)
                        } else {
                            requestRepo.countByEstado("ABERTA")
                        },
                    "eventosAgendados" to eventRepo.countByEstado("AGENDADO"),
                ),
            "solicitacoesPorTipo" to solicitacoesPorTipo,
            "distribuicaoPorEstado" to distribuicaoPorEstado,
            "evolucaoTemporal" to evolucao,
            "rankingCursos" to rankingCursos,
        )
    }

    @GetMapping("/coordinator")
    @PreAuthorize("hasAuthority('report.view_coordinator') or hasAuthority('system.admin')")
    @Operation(summary = "Relatório analítico de coordenação")
    fun coordinator(
        @RequestParam(required = false) periodo: String?,
        @RequestParam(required = false) curso: String?,
    ): Map<String, Any?> {
        val (cursoId, from, to) = resolveFilters(periodo, curso)
        val estados = requestRepo.countGroupedByEstadoFiltered(cursoId, from, to).associate { it[0].toString() to (it[1] as Number).toLong() }
        val deferidas = estados["DEFERIDA"] ?: 0L
        val indeferidas = estados["INDEFERIDA"] ?: 0L
        val decididas = deferidas + indeferidas
        val taxaIndeferimento = if (decididas == 0L) 0.0 else indeferidas.toDouble() / decididas
        val formativasPorCategoria =
            formativeRepo.countAprovadasByCategoria().map { row ->
                mapOf("categoria" to row[0].toString(), "total" to (row[1] as Number).toLong())
            }
        val sla =
            requestRepo.findTop10ByEstadoAndPrazoEmIsNotNullOrderByPrazoEmAsc("ABERTA")
                .filter { cursoId == null || it.idCurso == cursoId }
                .map { r ->
                    mapOf(
                        "id" to r.id,
                        "tipo" to r.requestTypeCode,
                        "prazoEm" to r.prazoEm,
                        "href" to "/requests/${r.id}",
                    )
                }
        val proximos =
            eventRepo.findUpcoming(OffsetDateTime.now(), PageRequest.of(0, 10)).content.map { e ->
                mapOf(
                    "id" to e.id,
                    "titulo" to e.titulo,
                    "inicioEm" to e.inicioEm,
                    "estado" to e.estado,
                    "href" to "/events/${e.id}",
                )
            }
        val evasao =
            graduationRepo.countByAnoColacao().map { row ->
                mapOf("ano" to (row[0] as Number).toInt(), "colacoes" to (row[1] as Number).toLong())
            }
        val carga =
            requestEventRepo.countCargaPorDeliberador().map { row ->
                val atorId = UUID.fromString(row[0].toString())
                val nome = usuarioRepo.findById(atorId).map { it.nome }.orElse(atorId.toString())
                mapOf("idAtor" to atorId, "nome" to nome, "deliberacoes" to (row[1] as Number).toLong())
            }
        return mapOf(
            "filtros" to mapOf("periodo" to periodo, "curso" to curso, "cursoId" to cursoId),
            "kpis" to
                mapOf(
                    "tempoMedioDeliberacaoSegundos" to (requestRepo.avgDeliberationSecondsFiltered(cursoId)?.toDouble() ?: 0.0),
                    "taxaIndeferimento" to taxaIndeferimento,
                    "thresholdIndeferimento" to 0.3,
                    "volumeFormativas" to formativeRepo.countByEstado("APROVADA"),
                    "taxaPresencaEventosAgendados" to eventRepo.countByEstado("CONCLUIDO"),
                    "tccEmAndamento" to tccRepo.countByEstado("EM_ANDAMENTO"),
                    "estagiosSemSupervisor" to internshipRepo.countByIdSupervisorIsNull(),
                ),
            "seriesFormativas" to formativasPorCategoria,
            "evasaoPorPeriodo" to evasao,
            "cargaPorDeliberador" to carga,
            "pendencias" to sla,
            "proximosEventos" to proximos,
            "_links" to
                mapOf(
                    "self" to "/reports/coordinator",
                    "curso" to "/academico/relatorios/curso",
                ),
        )
    }

    private fun resolveFilters(
        periodo: String?,
        curso: String?,
    ): Triple<UUID?, OffsetDateTime?, OffsetDateTime?> {
        val cursoId =
            curso?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching { UUID.fromString(raw) }.getOrNull()
                    ?: cursoRepo.findBySigla(raw.uppercase()).map { it.id }.orElse(null)
            }
        var from: OffsetDateTime? = null
        var to: OffsetDateTime? = null
        if (!periodo.isNullOrBlank()) {
            val parts = periodo.split("-")
            if (parts.size == 2) {
                val ano = parts[0].toShortOrNull()
                val sem = parts[1].toShortOrNull()
                if (ano != null && sem != null) {
                    periodoRepo.findByAnoAndSemestre(ano, sem).ifPresent { p ->
                        from = p.inicio.atStartOfDay().atOffset(ZoneOffset.UTC)
                        to = p.fim.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
                    }
                    if (from == null) {
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
