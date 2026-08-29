package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeVersionEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeVersionJpaRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RequestTypeVersionStore(
    private val versionRepo: RequestTypeVersionJpaRepository,
) {
    fun latestId(typeId: UUID): UUID? =
        versionRepo.findFirstByIdRequestTypeOrderByVersionDesc(typeId).map { it.id }.orElse(null)

    fun snapshot(type: RequestTypeEntity): RequestTypeVersionEntity {
        val next = (versionRepo.findMaxVersion(type.id) ?: 0) + 1
        return versionRepo.save(
            RequestTypeVersionEntity(
                idRequestType = type.id,
                version = next,
                formSchema = type.formSchema,
                workflowJson = type.workflowJson,
            ),
        )
    }

    fun formSchemaFor(
        request: RequestEntity,
        fallback: Map<String, Any>,
    ): Map<String, Any> {
        val versionId = request.idRequestTypeVersion ?: return fallback
        return versionRepo.findById(versionId).map { it.formSchema }.orElse(fallback)
    }
}
