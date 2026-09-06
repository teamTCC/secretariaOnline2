package br.ufpr.sept.so2.modules.academico.domain

import java.time.OffsetDateTime
import java.util.UUID

data class Curso(
    val id: UUID,
    val nome: String,
    val sigla: String,
    val idCoordenador: UUID?,
    val ativo: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class Disciplina(
    val id: UUID,
    val idCurso: UUID,
    val codigo: String,
    val nome: String,
    val cargaHorariaTotal: Int,
    val creditos: Int,
    val ativa: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class PeriodoLetivo(
    val id: UUID,
    val ano: Short,
    val semestre: Short,
    val inicio: java.time.LocalDate,
    val fim: java.time.LocalDate,
    val ativo: Boolean,
)

data class CalendarioAcademico(
    val id: UUID,
    val idPeriodoLetivo: UUID,
    val idRequestType: UUID?,
    val descricao: String,
    val prazoInicio: java.time.LocalDate?,
    val prazoFim: java.time.LocalDate?,
)
