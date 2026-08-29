package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.SecretaryTaskResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SecretaryTaskEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SecretaryTaskJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Component
class SecretaryTaskQuery(
    private val taskRepo: SecretaryTaskJpaRepository,
) {
    fun list(
        estado: String?,
        pageable: Pageable,
    ): PageResponse<SecretaryTaskResponse> {
        val page = if (estado != null) taskRepo.findAllByEstado(estado.uppercase(), pageable) else taskRepo.findAll(pageable)
        return PageResponse.ofWithLinks(page) { t -> t.toResponse() }
    }
}

data class CreateSecretaryTaskCommand(
    val titulo: String,
    val descricao: String?,
    val prioridade: String,
    val prazoEm: OffsetDateTime?,
    val idAssignee: UUID?,
)

data class PatchSecretaryTaskCommand(
    val id: UUID,
    val titulo: String?,
    val descricao: String?,
    val estado: String?,
    val prioridade: String?,
    val idAssignee: UUID?,
    val prazoEm: OffsetDateTime?,
)

@Service
@Transactional
class ManageSecretaryTaskUseCase(
    private val taskRepo: SecretaryTaskJpaRepository,
) {
    fun create(cmd: CreateSecretaryTaskCommand): SecretaryTaskResponse {
        val saved =
            taskRepo.save(
                SecretaryTaskEntity(
                    titulo = cmd.titulo,
                    descricao = cmd.descricao,
                    prioridade = cmd.prioridade.uppercase(),
                    prazoEm = cmd.prazoEm,
                    idAssignee = cmd.idAssignee,
                ),
            )
        return saved.toResponse()
    }

    fun patch(cmd: PatchSecretaryTaskCommand): SecretaryTaskResponse {
        val task = taskRepo.findById(cmd.id).orElseThrow { NoSuchElementException("Tarefa não encontrada: ${cmd.id}") }
        cmd.titulo?.let { task.titulo = it }
        cmd.descricao?.let { task.descricao = it }
        cmd.estado?.let {
            val novo = it.uppercase()
            require(novo in ESTADOS) { "Estado inválido: $novo" }
            task.estado = novo
        }
        cmd.prioridade?.let { task.prioridade = it.uppercase() }
        cmd.idAssignee?.let { task.idAssignee = it }
        cmd.prazoEm?.let { task.prazoEm = it }
        taskRepo.save(task)
        return task.toResponse()
    }

    fun delete(id: UUID) {
        val task = taskRepo.findById(id).orElseThrow { NoSuchElementException("Tarefa não encontrada: $id") }
        require(task.estado == "PENDENTE") { "Só é possível excluir tarefas PENDENTE." }
        taskRepo.delete(task)
    }

    companion object {
        private val ESTADOS = setOf("PENDENTE", "EM_ANDAMENTO", "CONCLUIDA")
    }
}

internal fun SecretaryTaskEntity.toResponse(): SecretaryTaskResponse =
    SecretaryTaskResponse(
        id = id,
        titulo = titulo,
        descricao = descricao,
        estado = estado,
        prioridade = prioridade,
        idAssignee = idAssignee,
        prazoEm = prazoEm,
    )
