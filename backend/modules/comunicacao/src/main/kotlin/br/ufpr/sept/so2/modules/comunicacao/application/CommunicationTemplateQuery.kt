package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateRevisionDetailResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateRevisionSummaryResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateSummaryResponse
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateRevisionJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CommunicationTemplateQuery(
    private val templateRepo: CommunicationTemplateJpaRepository,
    private val revisionRepo: CommunicationTemplateRevisionJpaRepository,
) {
    fun list(): List<TemplateSummaryResponse> =
        templateRepo.findAll().map { t ->
            TemplateSummaryResponse(
                id = t.id,
                codigo = t.codigo,
                titulo = t.titulo,
                assunto = t.assunto,
                corpo = t.corpo,
                canal = t.canal,
                versao = t.versao,
                ativo = t.ativo,
                variaveis = listOf("nome", "email", "protocolo", "eventoTitulo"),
            )
        }

    fun versions(templateId: UUID): List<TemplateRevisionSummaryResponse> =
        revisionRepo.findAllByIdTemplateOrderByVersaoDesc(templateId).map { r ->
            TemplateRevisionSummaryResponse(
                versao = r.versao,
                assunto = r.assunto,
                createdAt = r.createdAt,
                idAutor = r.idAutor,
            )
        }

    fun version(
        templateId: UUID,
        rev: Int,
    ): TemplateRevisionDetailResponse {
        val r =
            revisionRepo.findByIdTemplateAndVersao(templateId, rev).orElseThrow {
                NoSuchElementException("Revisão $rev não encontrada")
            }
        return TemplateRevisionDetailResponse(
            versao = r.versao,
            assunto = r.assunto,
            corpo = r.corpo,
            createdAt = r.createdAt,
        )
    }
}
