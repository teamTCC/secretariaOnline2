package br.ufpr.sept.so2.modules.iam.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FcmTokenJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FcmOutboxHandler(
    private val fcmTokenRepo: FcmTokenJpaRepository,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean = eventType == OutboxEventTypes.FCM_PUSH

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val idUsuario = UUID.fromString(payload["idUsuario"] as? String ?: error("payload.idUsuario ausente"))
        val titulo = payload["titulo"] as? String ?: "SecretariaOnline"
        val corpo = payload["corpo"] as? String ?: ""
        val data = payload["data"] as? Map<*, *> ?: emptyMap<String, String>()

        val tokens = fcmTokenRepo.findAllByIdUsuarioAndAtivo(idUsuario, true)
        if (tokens.isEmpty()) {
            log.debug("Nenhum token FCM ativo para usuário {} — push ignorado", idUsuario)
            return
        }

        val firebaseAvailable = FirebaseApp.getApps().isNotEmpty()

        tokens.forEach { tokenEntity ->
            if (!firebaseAvailable) {
                log.info(
                    "FCM [STUB — Firebase não configurado] → usuário={} token={}... titulo='{}' corpo='{}'",
                    idUsuario, tokenEntity.fcmToken.take(20), titulo, corpo,
                )
                return@forEach
            }

            try {
                val messageBuilder = Message.builder()
                    .setToken(tokenEntity.fcmToken)
                    .setNotification(
                        Notification.builder()
                            .setTitle(titulo)
                            .setBody(corpo)
                            .build(),
                    )

                @Suppress("UNCHECKED_CAST")
                (data as? Map<String, String>)?.forEach { (k, v) ->
                    messageBuilder.putData(k, v)
                }

                val messageId = FirebaseMessaging.getInstance().send(messageBuilder.build())
                log.info("FCM enviado para usuário={} messageId={}", idUsuario, messageId)
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode?.name) {
                    "UNREGISTERED", "INVALID_ARGUMENT" -> {
                        log.warn(
                            "Token FCM inválido para usuário={} (código={}) — desativando token",
                            idUsuario, e.messagingErrorCode,
                        )
                        tokenEntity.ativo = false
                        fcmTokenRepo.save(tokenEntity)
                    }
                    else -> {
                        log.error(
                            "Erro ao enviar FCM para usuário={}: {} — {}",
                            idUsuario, e.messagingErrorCode, e.message,
                        )
                        throw e
                    }
                }
            }
        }
    }
}
