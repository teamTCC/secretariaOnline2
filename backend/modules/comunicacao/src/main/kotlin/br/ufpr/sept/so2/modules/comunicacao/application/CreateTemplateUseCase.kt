package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateRevisionEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateRevisionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class CreateTemplateCommand(
    val codigo: String,
    val titulo: String,
    val assunto: String,
    val corpo: String,
    val canal: String,
    val autorId: UUID?,
)

data class CreateTemplateResult(
    val id: UUID,
    val codigo: String,
    val versao: Int,
)

@Service
@Transactional
class CreateTemplateUseCase(
    private val templateRepo: CommunicationTemplateJpaRepository,
    private val revisionRepo: CommunicationTemplateRevisionJpaRepository,
) {
    fun execute(command: CreateTemplateCommand): CreateTemplateResult {
        val codigo = command.codigo.lowercase()
        require(templateRepo.findByCodigo(codigo).isEmpty) { "Template já existe: $codigo" }

        val saved =
            templateRepo.save(
                CommunicationTemplateEntity(
                    codigo = codigo,
                    titulo = command.titulo,
                    assunto = command.assunto,
                    corpo = command.corpo,
                    canal = command.canal.uppercase(),
                ),
            )
        revisionRepo.save(
            CommunicationTemplateRevisionEntity(
                idTemplate = saved.id,
                versao = 1,
                assunto = command.assunto,
                corpo = command.corpo,
                idAutor = command.autorId,
            ),
        )

        return CreateTemplateResult(id = saved.id, codigo = saved.codigo, versao = saved.versao)
    }
}
