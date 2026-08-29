package br.ufpr.sept.so2.modules.academico.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CursoSummaryResponse(
    val id: UUID,
    val nome: String,
    val sigla: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CursoDetailResponse(
    val id: UUID,
    val nome: String,
    val sigla: String,
    val idCoordenador: UUID?,
    val ativo: Boolean,
    val horasFormativasMinimas: Int,
    val duracaoCalendario: String,
    val bancaMembrosExternos: Int,
    val bancaModalidade: String,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CursoUpdatedResponse(
    val id: UUID,
    val nome: String,
    val sigla: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DisciplinaSummaryResponse(
    val id: UUID,
    val codigo: String,
    val nome: String,
    val creditos: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DisciplinaCreatedResponse(
    val id: UUID,
    val codigo: String,
    val nome: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PeriodoLetivoSummaryResponse(
    val id: UUID,
    val ano: Short,
    val semestre: Short,
    val inicio: LocalDate,
    val fim: LocalDate,
    val ativo: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PeriodoLetivoCreatedResponse(
    val id: UUID,
    val ano: Short,
    val semestre: Short,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PeriodoAtivoResponse(
    val id: UUID,
    val ano: Short,
    val semestre: Short,
    val inicio: LocalDate,
    val fim: LocalDate,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CalendarioItemResponse(
    val id: UUID,
    val descricao: String,
    val prazoInicio: LocalDate?,
    val prazoFim: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourseConfigResponse(
    val courseId: UUID,
    val sigla: String,
    val horasFormativasMinimas: Int,
    val duracaoCalendario: String,
    val bancaMembrosExternos: Int,
    val bancaModalidade: String,
    val regimento: String?,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoricoItemResponse(
    val id: UUID,
    val idDisciplina: UUID,
    val codigo: String?,
    val nome: String?,
    val estado: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoricoUpsertResponse(
    val id: UUID,
    val idAluno: UUID,
    val idDisciplina: UUID,
    val estado: String,
)
