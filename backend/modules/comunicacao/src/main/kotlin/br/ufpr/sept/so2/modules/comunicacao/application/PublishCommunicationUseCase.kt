package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class PublishCommunicationCommand(
    val idAutor: UUID,
    val titulo: String,
    val conteudo: String,
    val tipo: String,
    val cursoId: UUID?,
    val isAdmin: Boolean,
)

data class PublishCommunicationResult(
    val id: UUID,
    val entregas: Int,
)

@Service
@Transactional
class PublishCommunicationUseCase(
    private val communicationRepo: CommunicationJpaRepository,
    private val deliveryRepo: CommunicationDeliveryJpaRepository,
    // TODO: replace with a ComunicacaoUserPort once IAM coupling is properly decoupled
    private val usuarioRepo: UsuarioJpaRepository,
) {
    fun execute(command: PublishCommunicationCommand): PublishCommunicationResult {
        val now = OffsetDateTime.now()

        val audiencia: Map<String, Any> =
            if (command.isAdmin) {
                emptyMap()
            } else {
                val cursoId = requireNotNull(command.cursoId) { "cursoId obrigatório para publicação de turma." }
                mapOf("cursoId" to cursoId.toString())
            }

        val communication =
            communicationRepo.save(
                CommunicationEntity(
                    idAutor = command.idAutor,
                    titulo = command.titulo,
                    conteudo = command.conteudo,
                    tipo = command.tipo,
                    audiencia = audiencia,
                    publishedAt = now,
                ),
            )

        val cursoIdStr = audiencia["cursoId"]?.toString()
        val targets =
            usuarioRepo.findAll().filter { u ->
                u.ativo && (cursoIdStr == null || u.metadata["idCurso"]?.toString() == cursoIdStr)
            }
        val deliveries =
            targets.map { u ->
                CommunicationDeliveryEntity(
                    idCommunication = communication.id,
                    idUsuario = u.id,
                    canal = "IN_APP",
                    status = "ENTREGUE",
                    deliveredAt = now,
                )
            }
        deliveryRepo.saveAll(deliveries)

        return PublishCommunicationResult(id = communication.id, entregas = deliveries.size)
    }
}
