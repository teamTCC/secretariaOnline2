package br.ufpr.sept.so2.modules.solicitacoes.domain

import java.time.OffsetDateTime
import java.util.UUID

data class RequestType(
    val id: UUID,
    val code: String,
    val descricao: String,
    val formSchema: Map<String, Any>,
    val workflowDefinition: WorkflowDefinition,
    val prazoDias: Int,
    val ativo: Boolean,
)

data class Request(
    val id: UUID,
    val numeroAnual: Int,
    val ano: Short,
    val idRequestType: UUID,
    val requestTypeCode: String,
    val idSolicitante: UUID,
    val idCurso: UUID,
    val estado: RequestState,
    val dados: Map<String, Any>,
    val parecer: String?,
    val prazoEm: OffsetDateTime?,
    val concludedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun allowsReview(): Boolean = estado == RequestState.INDEFERIDA && concludedAt?.isAfter(OffsetDateTime.now().minusDays(5)) == true
}

data class RequestEvent(
    val id: UUID,
    val idRequest: UUID,
    val tipo: String,
    val estadoAnterior: RequestState,
    val estadoNovo: RequestState,
    val idAtor: UUID,
    val parecer: String?,
    val createdAt: OffsetDateTime,
)

data class RequestAttachment(
    val id: UUID,
    val idRequest: UUID,
    val categoria: String,
    val storageKey: String,
    val sha256: String,
    val nomeOriginal: String,
    val contentType: String,
    val tamanhoBytes: Long,
    val createdAt: OffsetDateTime,
)
