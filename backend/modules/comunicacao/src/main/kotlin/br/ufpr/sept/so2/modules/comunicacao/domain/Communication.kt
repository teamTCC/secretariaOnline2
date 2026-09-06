package br.ufpr.sept.so2.modules.comunicacao.domain

import java.time.OffsetDateTime
import java.util.UUID

data class CommunicationDomain(
    val id: UUID,
    val idAutor: UUID,
    val titulo: String,
    val conteudo: String,
    val tipo: String,
    val audiencia: Map<String, Any>,
    val publishedAt: OffsetDateTime?,
    val expiresAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
)

enum class CommunicationType {
    AVISO,
    URGENTE,
    INFORMATIVO,
}

class CommunicationNotFoundException(id: UUID) :
    NoSuchElementException("Comunicado não encontrado: $id")
