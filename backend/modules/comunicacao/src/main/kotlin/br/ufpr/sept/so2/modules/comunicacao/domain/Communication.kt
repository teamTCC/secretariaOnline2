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

/** Semantic constraint (e.g. cursoId for publish_class) — maps to RFC 7807 422, not 400. */
class CommunicationBusinessException(message: String) : RuntimeException(message)
