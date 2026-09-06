package br.ufpr.sept.so2.modules.notificacoes

import java.util.UUID

/**
 * Handler de um tipo (ou família) de evento do outbox.
 * Cada bounded context registra o seu — o [OutboxDispatcher] roteia por [supports].
 */
interface OutboxEventHandler {
    fun supports(eventType: String): Boolean

    fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    )
}

object OutboxEventTypes {
    // IAM
    const val PASSWORD_RESET_REQUESTED = "iam.password_reset_requested"
    const val USUARIO_CRIADO = "iam.usuario_criado"

    // Solicitações
    const val SOLICITACAO_ABERTA = "solicitacoes.aberta"
    const val SOLICITACAO_TRANSICIONADA = "solicitacoes.transicionada"

    // Formativas
    const val FORMATIVA_REVISADA = "formativas.revisada"
    const val FORMATIVA_BATCH_REVISADA = "formativas.batch_revisada"

    // Presença
    const val PRESENCA_CONFIRMADA = "presenca.confirmada"
    const val CERTIFICATE_ISSUED = "certificate.issued"

    // Estágio
    const val ESTAGIO_DECLARADO = "estagio.declarado"
    const val ESTAGIO_CONCLUIDO = "estagio.concluido"
    const val ESTAGIO_SUPERVISOR_ATRIBUIDO = "estagio.supervisor_atribuido"

    // TCC
    const val TCC_CRIADO = "tcc.criado"
    const val TCC_DELIBERADO = "tcc.deliberado"

    // Push FCM
    const val FCM_PUSH = "push.fcm.send"

    const val IMPORTS_COMPLETED = "imports.completed"
    const val ATENDIMENTO_CRIADO = "atendimentos.created"
    const val GRADUATION_CONFIRMED = "graduations.confirmed"
    const val EXPORTS_READY = "exports.ready"
    const val CONTATO_RECEBIDO = "contato.recebido"
}
