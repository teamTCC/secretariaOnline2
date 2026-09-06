package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.tcc.api.dto.TccDetailResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccExaminerResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccMemberResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccSummaryResponse
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccEntity
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccExaminerJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccMemberJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TccQuery(
    private val tccRepo: TccJpaRepository,
    private val memberRepo: TccMemberJpaRepository,
    private val examinerRepo: TccExaminerJpaRepository,
) {
    fun mine(userId: UUID): List<TccSummaryResponse> = tccRepo.findByAluno(userId).map { toSummary(it) }

    fun list(
        estado: String,
        userId: UUID,
        authorities: Set<String>,
        pageable: Pageable,
    ): PageResponse<TccSummaryResponse> {
        val page =
            if (authorities.contains("tcc.supervise") && !authorities.contains("request.deliberate")) {
                tccRepo.findAllByIdOrientador(userId, pageable)
            } else {
                tccRepo.findAllByEstado(estado, pageable)
            }
        return PageResponse.ofWithLinks(page) { toSummary(it) }
    }

    fun get(
        id: UUID,
        userId: UUID,
        authorities: Set<String>,
    ): TccDetailResponse {
        val tcc = tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        val members = memberRepo.findAllByIdTcc(id)
        val examiners = examinerRepo.findAllByIdTcc(id)
        val isOrientador = tcc.idOrientador == userId
        val isMember = members.any { it.idAluno == userId }
        val isExaminer = examiners.any { it.idProfessor == userId }
        val canManage = authorities.contains("request.deliberate")
        if (!isOrientador && !isMember && !isExaminer && !canManage) {
            throw AccessDeniedException("Acesso negado ao TCC $id")
        }
        val links = linkedMapOf("self" to "/tccs/$id")
        if (isOrientador) {
            links["update"] = "/tccs/$id"
            links["add-member"] = "/tccs/$id/members"
            links["add-examiner"] = "/tccs/$id/examiners"
            links["approve"] = "/tccs/$id/approve"
        }
        if (isMember) links["submit-final-url"] = "/tccs/$id/submit-final/url"
        if (isExaminer) links["grade"] = "/tccs/$id/grade"
        return TccDetailResponse(
            id = tcc.id,
            titulo = tcc.titulo,
            idOrientador = tcc.idOrientador,
            idCurso = tcc.idCurso,
            estado = tcc.estado,
            dataDefesa = tcc.dataDefesa,
            notaFinal = tcc.notaFinal,
            aprovado = tcc.aprovado,
            hashSha256Pdf = tcc.hashSha256Pdf,
            members = members.map { TccMemberResponse(it.idAluno, it.papel) },
            examiners = examiners.map { TccExaminerResponse(it.idProfessor, it.papel, it.nota) },
            createdAt = tcc.createdAt,
            updatedAt = tcc.updatedAt,
            links = links,
        )
    }

    fun toDetailAfterUpdate(tcc: TccEntity): TccDetailResponse =
        TccDetailResponse(
            id = tcc.id,
            titulo = tcc.titulo,
            idOrientador = tcc.idOrientador,
            idCurso = tcc.idCurso,
            estado = tcc.estado,
            dataDefesa = tcc.dataDefesa,
            notaFinal = tcc.notaFinal,
            aprovado = tcc.aprovado,
            hashSha256Pdf = tcc.hashSha256Pdf,
            members = emptyList(),
            examiners = emptyList(),
            createdAt = tcc.createdAt,
            updatedAt = tcc.updatedAt,
        )

    private fun toSummary(tcc: TccEntity) =
        TccSummaryResponse(
            id = tcc.id,
            titulo = tcc.titulo,
            estado = tcc.estado,
            dataDefesa = tcc.dataDefesa,
            idOrientador = tcc.idOrientador,
        )
}
