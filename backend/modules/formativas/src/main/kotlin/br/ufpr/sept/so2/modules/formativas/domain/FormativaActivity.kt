package br.ufpr.sept.so2.modules.formativas.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FormativaActivityDomain(
    val id: UUID,
    val idAluno: UUID,
    val titulo: String,
    val descricao: String?,
    val categoria: String,
    val cargaHoraria: Double,
    val dataRealizacao: LocalDate,
    val estado: String,
    val idRevisor: UUID?,
    val parecer: String?,
    val storageKeyComprovante: String?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
) {
    fun canBeReviewed(): Boolean = estado == FormativaStatus.PENDENTE.name
}

enum class FormativaStatus {
    PENDENTE,
    APROVADA,
    REJEITADA,
}

enum class FormativaCategoria {
    EXTENSAO,
    PESQUISA,
    MONITORIA,
    PUBLICACAO,
    EVENTO,
    EMPRESA_JUNIOR,
    ESTAGIO_NAO_OBRIGATORIO,
    REPRESENTACAO_DISCENTE,
    OUTROS,
}

class FormativaNotFoundException(id: UUID) :
    NoSuchElementException("Atividade formativa não encontrada: $id")

class FormativaBusinessException(message: String) : IllegalStateException(message)
