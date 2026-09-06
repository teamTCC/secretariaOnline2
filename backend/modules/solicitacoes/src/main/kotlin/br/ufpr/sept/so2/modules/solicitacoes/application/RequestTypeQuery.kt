package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestTypeDetailResponse
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RequestTypeQuery(
    private val typeRepo: RequestTypeJpaRepository,
) {
    fun list(): List<RequestTypeDetailResponse> = typeRepo.findAll().map { toResponse(it) }

    fun getById(id: UUID): RequestTypeDetailResponse {
        val entity = typeRepo.findById(id).orElseThrow { NoSuchElementException("Tipo não encontrado: $id") }
        return toResponse(entity)
    }

    fun toResponse(entity: RequestTypeEntity) =
        RequestTypeDetailResponse(
            id = entity.id,
            code = entity.code,
            descricao = entity.descricao,
            formSchema = entity.formSchema,
            workflowJson = entity.workflowJson,
            prazoDias = entity.prazoDias,
            ativo = entity.ativo,
        )
}
