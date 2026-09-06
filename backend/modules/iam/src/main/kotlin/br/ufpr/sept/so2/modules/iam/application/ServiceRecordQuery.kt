package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.ServiceRecordLinks
import br.ufpr.sept.so2.modules.iam.api.dto.ServiceRecordResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ServiceRecordEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ServiceRecordJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.AuthenticatedUser
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ServiceRecordQuery(
    private val serviceRecordRepo: ServiceRecordJpaRepository,
) {
    fun list(
        user: AuthenticatedUser,
        idAluno: UUID?,
        aluno: String?,
        status: String?,
        pageable: Pageable,
    ): PageResponse<ServiceRecordResponse> {
        val ownOnly =
            aluno.equals("me", ignoreCase = true) ||
                (
                    user.authorities.contains("service_record.view_own") &&
                        !user.authorities.contains("user.manage_students")
                )
        if (ownOnly &&
            !user.authorities.contains("service_record.view_own") &&
            !user.authorities.contains("user.manage_students")
        ) {
            throw AccessDeniedException("Capability service_record.view_own ausente.")
        }
        val targetAluno = if (ownOnly) user.userId else idAluno
        val estado = status?.uppercase()
        val page =
            when {
                targetAluno != null && estado != null ->
                    serviceRecordRepo.findAllByIdAlunoAndEstado(targetAluno, estado, pageable)
                targetAluno != null -> serviceRecordRepo.findAllByIdAluno(targetAluno, pageable)
                else -> {
                    if (!user.authorities.contains("user.manage_students")) {
                        throw AccessDeniedException("Listagem geral exige user.manage_students.")
                    }
                    serviceRecordRepo.findAll(pageable)
                }
            }
        return PageResponse.ofWithLinks(page) { r -> r.toResponse(includeAcknowledge = ownOnly) }
    }

    fun historyByAluno(
        id: UUID,
        pageable: Pageable,
    ): PageResponse<ServiceRecordResponse> =
        PageResponse.ofWithLinks(serviceRecordRepo.findAllByIdAluno(id, pageable)) { r ->
            r.toResponse(includeAcknowledge = false)
        }

    fun myHistory(
        alunoId: UUID,
        status: String?,
        pageable: Pageable,
    ): PageResponse<ServiceRecordResponse> {
        val page =
            if (status != null) {
                serviceRecordRepo.findAllByIdAlunoAndEstado(alunoId, status.uppercase(), pageable)
            } else {
                serviceRecordRepo.findAllByIdAluno(alunoId, pageable)
            }
        return PageResponse.ofWithLinks(page) { r -> r.toResponse(includeAcknowledge = true) }
    }
}

internal fun ServiceRecordEntity.toResponse(includeAcknowledge: Boolean): ServiceRecordResponse =
    ServiceRecordResponse(
        id = id,
        idAluno = idAluno,
        idSecretario = idSecretario,
        assunto = assunto,
        tipo = tipo,
        descricao = descricao,
        estado = estado,
        acknowledgedAt = acknowledgedAt,
        createdAt = createdAt,
        links =
            ServiceRecordLinks(
                self = "/service-records/$id",
                acknowledge =
                    if (includeAcknowledge && estado == "PENDENTE_CIENCIA") {
                        "/service-records/$id/acknowledge"
                    } else {
                        null
                    },
            ),
    )
