package br.ufpr.sept.so2.modules.estagio.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Estagio(
    val id: UUID,
    val idAluno: UUID,
    val empresa: String,
    val cargo: String,
    val cargaHorariaSemanal: Int,
    val inicio: LocalDate,
    val fim: LocalDate?,
    val estado: String,
    val idSupervisor: UUID?,
    val observacoes: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun canBeFinalized(): Boolean = estado == EstagioStatus.EM_ANDAMENTO.name && fim != null
}

enum class EstagioStatus { EM_ANDAMENTO, ENCERRADO, CANCELADO }

class EstagioNotFoundException(
    id: UUID,
) : NoSuchElementException("Estágio não encontrado: $id")

class EstagioBusinessException(
    message: String,
) : IllegalArgumentException(message)
