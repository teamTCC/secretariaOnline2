package br.ufpr.sept.so2.modules.tcc.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Tcc(
    val id: UUID,
    val titulo: String,
    val idOrientador: UUID,
    val idCurso: UUID,
    val estado: String,
    val dataDefesa: LocalDate?,
    val notaFinal: Double?,
    val aprovado: Boolean?,
    val hashSha256Pdf: String?,
    val storageKeyPdf: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun canGrade(): Boolean = estado == TccStatus.AGUARDANDO_DEFESA.name
}

data class TccMember(
    val idTcc: UUID,
    val idAluno: UUID,
    val papel: String,
    val joinedAt: OffsetDateTime,
)

data class TccExaminer(
    val idTcc: UUID,
    val idProfessor: UUID,
    val papel: String,
    val nota: Double?,
)

enum class TccStatus { EM_ANDAMENTO, AGUARDANDO_DEFESA, DEFENDIDO, CANCELADO }

class TccNotFoundException(
    id: UUID,
) : NoSuchElementException("TCC não encontrado: $id")

class TccBusinessException(
    message: String,
) : IllegalArgumentException(message)
