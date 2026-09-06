package br.ufpr.sept.so2.modules.bff.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.util.UUID

// ─── Shared items ─────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PendenciaItem(
    val id: UUID,
    val tipo: String,
    val estado: String,
    val prazoEm: OffsetDateTime?,
    val acao: String?,
    @JsonProperty("_link") val link: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventoItem(
    val id: UUID,
    val titulo: String,
    val chCreditadas: Double,
    val fimEm: OffsetDateTime?,
    @JsonProperty("_link") val link: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SolicitacaoItem(
    val id: UUID,
    val tipo: String,
    val estado: String,
    val createdAt: OffsetDateTime?,
)

// ─── Dashboard Aluno ──────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HorasFormativasKpi(
    val atual: Double,
    val requerido: Double,
    val percentual: Double,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardAlunoKpis(
    val horasFormativas: HorasFormativasKpi,
    val atendimentosPendentes: Long?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardAlunoLinks(
    val self: String,
    val novaSolicitacao: String?,
    val formativas: String,
    val eventos: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardAlunoResponse(
    val kpis: DashboardAlunoKpis,
    val pendencias: List<PendenciaItem>?,
    val eventos: List<EventoItem>?,
    val ultimasSolicitacoes: List<SolicitacaoItem>?,
    @JsonProperty("_links") val links: DashboardAlunoLinks,
    val _degraded: Boolean? = null,
)

// ─── Dashboard Professor ──────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventoOrganizadorItem(
    val id: UUID,
    val titulo: String,
    val estado: String,
    val inicioEm: OffsetDateTime?,
    val fimEm: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfessorPendenciaItem(
    val id: UUID,
    val tipo: String,
    val prazoEm: OffsetDateTime?,
    @JsonProperty("_link") val link: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardProfessorLinks(
    val self: String,
    val novoEvento: String?,
    val meusEventos: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardProfessorResponse(
    val meusEventos: List<EventoOrganizadorItem>?,
    val solicitacoesPendentes: List<ProfessorPendenciaItem>?,
    @JsonProperty("_links") val links: DashboardProfessorLinks,
    val _degraded: Boolean? = null,
)

// ─── Dashboard Egresso ────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CertificadoItem(
    val id: UUID,
    val hashSha256: String?,
    val issuedAt: OffsetDateTime?,
    @JsonProperty("_link") val link: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ComunicadoItem(
    val id: UUID,
    val idCommunication: UUID?,
    val deliveredAt: OffsetDateTime?,
    val readAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardEgressoLinks(
    val self: String,
    val certificados: String,
    val comunicados: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardEgressoResponse(
    val nomeAluno: String?,
    val emailAluno: String?,
    val tccsDefendidos: Int?,
    val certificados: List<CertificadoItem>?,
    val comunicados: List<ComunicadoItem>?,
    @JsonProperty("_links") val links: DashboardEgressoLinks,
    val _degraded: Boolean? = null,
)

// ─── Dashboard Secretaria ─────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardSecretariaKpis(
    val emTriagem: Long?,
    val emDeliberacao: Long?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardSecretariaLinks(
    val self: String,
    val solicitacoes: String,
    val usuarios: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DashboardSecretariaResponse(
    val kpis: DashboardSecretariaKpis,
    @JsonProperty("_links") val links: DashboardSecretariaLinks,
    val _degraded: Boolean? = null,
)

// ─── Acadêmico Summary ────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AcademicoSummaryResponse(
    val totalAlunos: Long?,
    val tccEmAndamento: Long?,
    val estagiosAtivos: Long?,
    val solicitacoesAbertas: Long?,
    val _degraded: Boolean? = null,
)
