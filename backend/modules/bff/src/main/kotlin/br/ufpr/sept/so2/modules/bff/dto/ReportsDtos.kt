package br.ufpr.sept.so2.modules.bff.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportFilters(
    val periodo: String?,
    val curso: String?,
    val cursoId: UUID?,
)

// ── Secretary Report ─────────────────────────────────────────────────────────

data class SecretaryKpis(
    val alunosAtivos: Long,
    val egressos: Long,
    val solicitacoesAbertas: Long,
    val eventosAgendados: Long,
)

data class TipoSolicitacaoItem(
    val tipo: String,
    val total: Long,
)

data class EstadoSolicitacaoItem(
    val estado: String,
    val total: Long,
)

data class CursoRankingItem(
    val cursoId: UUID,
    val sigla: String,
    val total: Long,
)

data class EvolucaoMensalItem(
    val mes: String,
    val total: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SecretaryReportResponse(
    val filtros: ReportFilters,
    val kpis: SecretaryKpis,
    val solicitacoesPorTipo: List<TipoSolicitacaoItem>,
    val distribuicaoPorEstado: List<EstadoSolicitacaoItem>,
    val evolucaoTemporal: List<EvolucaoMensalItem>,
    val rankingCursos: List<CursoRankingItem>,
)

// ── Coordinator Report ───────────────────────────────────────────────────────

data class CoordinatorKpis(
    val tempoMedioDeliberacaoSegundos: Double,
    val taxaIndeferimento: Double,
    val thresholdIndeferimento: Double,
    val volumeFormativas: Long,
    val taxaPresencaEventosAgendados: Long,
    val tccEmAndamento: Long,
    val estagiosSemSupervisor: Long,
)

data class FormativaCategoriaItem(
    val categoria: String,
    val total: Long,
)

data class ColacaoAnoItem(
    val ano: Int,
    val colacoes: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CargaDeliberadorItem(
    val idAtor: UUID,
    val nome: String,
    val deliberacoes: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PendenciaSlaItem(
    val id: UUID,
    val tipo: String,
    val prazoEm: OffsetDateTime?,
    val href: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventoProximoItem(
    val id: UUID,
    val titulo: String,
    val inicioEm: OffsetDateTime,
    val estado: String,
    val href: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoordinatorReportResponse(
    val filtros: ReportFilters,
    val kpis: CoordinatorKpis,
    val seriesFormativas: List<FormativaCategoriaItem>,
    val evasaoPorPeriodo: List<ColacaoAnoItem>,
    val cargaPorDeliberador: List<CargaDeliberadorItem>,
    val pendencias: List<PendenciaSlaItem>,
    val proximosEventos: List<EventoProximoItem>,
    @JsonProperty("_links") val links: Map<String, String>,
)
