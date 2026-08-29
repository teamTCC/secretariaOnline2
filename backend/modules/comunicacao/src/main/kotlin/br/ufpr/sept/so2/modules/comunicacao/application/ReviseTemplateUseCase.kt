package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateRevisionEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateRevisionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ReviseTemplateCommand(
    val id: UUID,
    val assunto: String,
    val corpo: String,
    val autorId: UUID?,
)

data class ReviseTemplateResult(
    val id: UUID,
    val versao: Int,
)

@Service
@Transactional
class ReviseTemplateUseCase(
    private val templateRepo: CommunicationTemplateJpaRepository,
    private val revisionRepo: CommunicationTemplateRevisionJpaRepository,
) {
    fun execute(command: ReviseTemplateCommand): ReviseTemplateResult {
        val template =
            templateRepo
                .findById(command.id)
                .orElseThrow { NoSuchElementException("Template não encontrado: ${command.id}") }

        template.versao += 1
        template.assunto = command.assunto
        template.corpo = command.corpo
        templateRepo.save(template)

        revisionRepo.save(
            CommunicationTemplateRevisionEntity(
                idTemplate = template.id,
                versao = template.versao,
                assunto = command.assunto,
                corpo = command.corpo,
                idAutor = command.autorId,
            ),
        )

        return ReviseTemplateResult(id = template.id, versao = template.versao)
    }
}
