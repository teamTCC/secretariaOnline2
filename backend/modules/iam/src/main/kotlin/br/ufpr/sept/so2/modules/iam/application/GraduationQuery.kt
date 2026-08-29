package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.iam.api.dto.DiplomaUrlResponse
import br.ufpr.sept.so2.modules.iam.api.dto.EligibilityBloqueio
import br.ufpr.sept.so2.modules.iam.api.dto.GraduationEgressoItem
import br.ufpr.sept.so2.modules.iam.api.dto.GraduationRecordResponse
import br.ufpr.sept.so2.modules.iam.api.dto.StudentEligibilityItem
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class EgressosPage(
    val items: List<GraduationEgressoItem>,
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Component
class GraduationQuery(
    private val graduationRepo: GraduationRecordJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
    private val eligibilityService: GraduationEligibilityService,
    private val minio: MinioStorageService,
) {
    @Transactional(readOnly = true)
    fun listEgressos(
        situacaoDiploma: String?,
        pageable: Pageable,
    ): EgressosPage {
        val page = usuarioRepo.findAllByRoleCode("EGRESSO", pageable)
        val recs =
            if (page.content.isEmpty()) {
                emptyMap()
            } else {
                graduationRepo.findAllByIdAlunoIn(page.content.map { it.id }).associateBy { it.idAluno }
            }
        val items =
            page.content.map { u ->
                val rec = recs[u.id]
                GraduationEgressoItem(
                    id = u.id,
                    nome = u.nome,
                    email = u.email,
                    grr = u.grr,
                    situacaoDiploma = rec?.estado ?: "SEM_REGISTRO",
                    dataColacao = rec?.dataColacao,
                    graduationId = rec?.id,
                )
            }
        val filtered =
            if (situacaoDiploma != null) {
                items.filter { it.situacaoDiploma == situacaoDiploma.uppercase() }
            } else {
                items
            }
        return EgressosPage(
            items = filtered,
            number = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    fun listGraduations(
        estado: String?,
        pageable: Pageable,
    ): PageResponse<GraduationRecordResponse> {
        val page =
            if (estado != null) {
                graduationRepo.findAllByEstado(estado.uppercase(), pageable)
            } else {
                graduationRepo.findAll(pageable)
            }
        return PageResponse.of(page) { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun listStudents(
        eligibleForGraduation: Boolean?,
        pageable: Pageable,
    ): PageResponse<StudentEligibilityItem> {
        val page =
            if (eligibleForGraduation == true) {
                usuarioRepo.findEligibleForGraduation(pageable)
            } else {
                usuarioRepo.searchUsuarios(null, null, true, pageable)
            }
        return PageResponse.of(page) { u ->
            val elig = if (eligibleForGraduation == true) eligibilityService.evaluate(u) else null
            StudentEligibilityItem(
                id = u.id,
                nome = u.nome,
                email = u.email,
                grr = u.grr,
                eligible = elig?.eligible ?: false,
                bloqueios = elig?.bloqueios?.map { EligibilityBloqueio(razao = it.razao, detalhe = it.detalhe) } ?: emptyList(),
            )
        }
    }

    fun diplomaUrl(id: UUID): DiplomaUrlResponse {
        val rec = graduationRepo.findById(id).orElseThrow { NoSuchElementException("Colação não encontrada: $id") }
        val key =
            rec.diplomaStorageKey
                ?: return DiplomaUrlResponse(id = rec.id, hashSha256 = null, downloadUrl = null, status = "SEM_PDF")
        return DiplomaUrlResponse(
            id = rec.id,
            hashSha256 = rec.diplomaHashSha256,
            downloadUrl = minio.generateDownloadUrl(key, expiryMinutes = 60),
        )
    }

    private fun GraduationRecordEntity.toResponse(): GraduationRecordResponse {
        val links = mutableMapOf("self" to "/graduations/$id")
        if (estado != "DIPLOMA_ENTREGUE") {
            links["confirm-delivery"] = "/graduations/$id/confirm-delivery"
        }
        return GraduationRecordResponse(
            id = id,
            idAluno = idAluno,
            idCurso = idCurso,
            dataColacao = dataColacao,
            estado = estado,
            deliveredAt = deliveredAt,
            livro = livro,
            folha = folha,
            ata = ata,
            diplomaHashSha256 = diplomaHashSha256,
            links = links,
        )
    }
}
