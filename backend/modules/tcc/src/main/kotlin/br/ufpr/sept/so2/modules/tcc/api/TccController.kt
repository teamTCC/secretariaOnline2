package br.ufpr.sept.so2.modules.tcc.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccEntity
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccExaminerEntity
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccExaminerJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccMemberEntity
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccMemberJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class CreateTccDto(
    @field:NotBlank val titulo: String,
    val idCurso: UUID,
)

data class UpdateTccDto(
    val titulo: String?,
    val dataDefesa: LocalDate?,
)

data class AddMemberDto(
    val idAluno: UUID,
    val papel: String = "AUTOR",
)

data class AddExaminerDto(
    val idProfessor: UUID,
    val papel: String = "BANCA",
)

data class GradeDto(
    @field:DecimalMin("0.0") @field:DecimalMax("10.0") val nota: Double,
)

data class ApproveDto(
    val aprovado: Boolean,
    val notaFinal: Double?,
)

data class SubmitFinalUrlDto(
    @field:NotBlank val nomeOriginal: String,
)

data class SubmitFinalConfirmDto(
    @field:NotBlank val storageKey: String,
    @field:NotBlank val sha256: String,
)

@RestController
@RequestMapping("/tccs")
@Tag(name = "TCC", description = "Gestão de Trabalhos de Conclusão de Curso")
class TccController(
    private val tccRepo: TccJpaRepository,
    private val memberRepo: TccMemberJpaRepository,
    private val examinerRepo: TccExaminerJpaRepository,
    private val outboxRepo: OutboxEventJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Criar TCC (orientador)")
    @Transactional
    fun create(
        @Valid @RequestBody dto: CreateTccDto,
    ): ResponseEntity<Map<String, Any>> {
        val userId = currentUserId()
        val tcc =
            TccEntity(
                idOrientador = userId,
                titulo = dto.titulo,
                idCurso = dto.idCurso,
                estado = "EM_ANDAMENTO",
            )
        val saved = tccRepo.save(tcc)
        outboxRepo.save(
            OutboxEventEntity(
                eventType = "tcc.criado",
                aggregateType = "tcc",
                aggregateId = saved.id,
                payload =
                    mapOf(
                        "tccId" to saved.id.toString(),
                        "idOrientador" to userId.toString(),
                        "titulo" to dto.titulo,
                    ),
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to saved.id, "estado" to saved.estado))
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('tcc.view_own')")
    @Operation(summary = "Listar TCCs do aluno autenticado")
    fun mine(): List<Map<String, Any?>> {
        val userId = currentUserId()
        return tccRepo.findByAluno(userId).map { it.toSummaryMap() }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('request.deliberate') or hasAuthority('tcc.supervise')")
    @Operation(summary = "Listar TCCs — secretaria vê todos, orientador vê os seus")
    fun list(
        @RequestParam(defaultValue = "EM_ANDAMENTO") estado: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val user = currentUser()
        val page =
            if (user.authorities.contains("tcc.supervise") && !user.authorities.contains("request.deliberate")) {
                tccRepo.findAllByIdOrientador(user.userId, pageable)
            } else {
                tccRepo.findAllByEstado(estado, pageable)
            }
        return PageResponse.of(page) { it.toSummaryMap() }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhe de TCC com membros e banca")
    fun get(
        @PathVariable id: UUID,
    ): EntityModel<Map<String, Any?>> {
        val tcc = tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        val members = memberRepo.findAllByIdTcc(id)
        val examiners = examinerRepo.findAllByIdTcc(id)
        val user = currentUser()
        val isOrientador = tcc.idOrientador == user.userId
        val isMember = members.any { it.idAluno == user.userId }
        val isExaminer = examiners.any { it.idProfessor == user.userId }
        val canManage = user.authorities.contains("request.deliberate")
        if (!isOrientador && !isMember && !isExaminer && !canManage) {
            throw AccessDeniedException("Acesso negado ao TCC $id")
        }
        val links = mutableListOf(Link.of("/tccs/$id").withSelfRel())
        if (isOrientador) {
            links.add(Link.of("/tccs/$id").withRel("update"))
            links.add(Link.of("/tccs/$id/members").withRel("add-member"))
            links.add(Link.of("/tccs/$id/examiners").withRel("add-examiner"))
            links.add(Link.of("/tccs/$id/approve").withRel("approve"))
        }
        if (isMember) links.add(Link.of("/tccs/$id/submit-final/url").withRel("submit-final-url"))
        if (isExaminer) links.add(Link.of("/tccs/$id/grade").withRel("grade"))
        return EntityModel.of(tcc.toDetailMap(members, examiners), links)
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Atualizar título ou data de defesa (orientador)")
    @Transactional
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateTccDto,
    ): ResponseEntity<Map<String, Any?>> {
        val tcc = tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        val userId = currentUserId()
        require(tcc.idOrientador == userId) { "Você não é o orientador deste TCC." }
        dto.titulo?.let { tcc.titulo = it }
        dto.dataDefesa?.let { tcc.dataDefesa = it }
        tccRepo.save(tcc)
        return ResponseEntity.ok(tcc.toDetailMap(emptyList(), emptyList()))
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Vincular aluno ao TCC (orientador)")
    @Transactional
    fun addMember(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AddMemberDto,
    ): ResponseEntity<Map<String, Any>> {
        val tcc = tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        val userId = currentUserId()
        require(tcc.idOrientador == userId) { "Você não é o orientador deste TCC." }
        val member = TccMemberEntity(idTcc = id, idAluno = dto.idAluno, papel = dto.papel)
        memberRepo.save(member)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("idTcc" to id, "idAluno" to dto.idAluno, "papel" to dto.papel),
        )
    }

    @PostMapping("/{id}/examiners")
    @PreAuthorize("hasAuthority('tcc.supervise') or hasAuthority('request.deliberate')")
    @Operation(summary = "Adicionar membro de banca (orientador/secretaria)")
    @Transactional
    fun addExaminer(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AddExaminerDto,
    ): ResponseEntity<Map<String, Any>> {
        tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        val examiner = TccExaminerEntity(idTcc = id, idProfessor = dto.idProfessor, papel = dto.papel)
        examinerRepo.save(examiner)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("idTcc" to id, "idProfessor" to dto.idProfessor, "papel" to dto.papel),
        )
    }

    @PatchMapping("/{id}/grade")
    @PreAuthorize("hasAuthority('tcc.examine')")
    @Operation(summary = "Registrar nota de banca")
    @Transactional
    fun grade(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: GradeDto,
    ): ResponseEntity<Map<String, Any?>> {
        tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        val userId = currentUserId()
        val examiner =
            examinerRepo.findAllByIdTcc(id).find { it.idProfessor == userId }
                ?: throw AccessDeniedException("Você não é membro da banca deste TCC.")
        examiner.nota = dto.nota
        examinerRepo.save(examiner)
        return ResponseEntity.ok(mapOf("idProfessor" to userId, "nota" to dto.nota))
    }

    @PostMapping("/{id}/submit-final/url")
    @PreAuthorize("hasAuthority('tcc.upload_final')")
    @Operation(summary = "Gerar URL presignada MinIO para upload do PDF final")
    fun submitFinalUrl(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: SubmitFinalUrlDto,
    ): ResponseEntity<Map<String, String>> {
        tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        assertTccMember(id)
        val storageKey = "tccs/$id/final_${UUID.randomUUID()}.pdf"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, "application/pdf")
        return ResponseEntity.ok(mapOf("uploadUrl" to uploadUrl, "storageKey" to storageKey))
    }

    @PostMapping("/{id}/submit-final/confirm")
    @PreAuthorize("hasAuthority('tcc.upload_final')")
    @Operation(summary = "Registrar PDF final após upload MinIO confirmado")
    @Transactional
    fun submitFinalConfirm(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: SubmitFinalConfirmDto,
    ): ResponseEntity<Map<String, Any?>> {
        val tcc = tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        assertTccMember(id)
        tcc.storageKeyPdf = dto.storageKey
        tcc.hashSha256Pdf = dto.sha256
        tccRepo.save(tcc)
        return ResponseEntity.ok(mapOf("id" to tcc.id, "hashSha256Pdf" to tcc.hashSha256Pdf))
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Aprovar ou reprovar TCC após banca (orientador)")
    @Transactional
    fun approve(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: ApproveDto,
    ): ResponseEntity<Map<String, Any?>> {
        val tcc = tccRepo.findById(id).orElseThrow { NoSuchElementException("TCC não encontrado: $id") }
        val userId = currentUserId()
        require(tcc.idOrientador == userId) { "Você não é o orientador deste TCC." }
        tcc.aprovado = dto.aprovado
        tcc.notaFinal = dto.notaFinal
        tcc.estado = if (dto.aprovado) "APROVADO" else "REPROVADO"
        tccRepo.save(tcc)
        outboxRepo.save(
            OutboxEventEntity(
                eventType = "tcc.deliberado",
                aggregateType = "tcc",
                aggregateId = tcc.id,
                payload =
                    mapOf(
                        "tccId" to tcc.id.toString(),
                        "aprovado" to dto.aprovado,
                        "notaFinal" to (dto.notaFinal ?: ""),
                    ),
            ),
        )
        return ResponseEntity.ok(mapOf("estado" to tcc.estado, "aprovado" to tcc.aprovado, "notaFinal" to tcc.notaFinal))
    }

    private fun assertTccMember(id: UUID) {
        val userId = currentUserId()
        val isMember = memberRepo.findAllByIdTcc(id).any { it.idAluno == userId }
        if (!isMember) {
            throw AccessDeniedException("Apenas membros do TCC podem enviar o PDF final.")
        }
    }

    private fun TccEntity.toSummaryMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "titulo" to titulo,
            "estado" to estado,
            "dataDefesa" to dataDefesa,
            "idOrientador" to idOrientador,
        )

    private fun TccEntity.toDetailMap(
        members: List<TccMemberEntity>,
        examiners: List<TccExaminerEntity>,
    ): Map<String, Any?> =
        mapOf(
            "id" to id,
            "titulo" to titulo,
            "idOrientador" to idOrientador,
            "idCurso" to idCurso,
            "estado" to estado,
            "dataDefesa" to dataDefesa,
            "notaFinal" to notaFinal,
            "aprovado" to aprovado,
            "hashSha256Pdf" to hashSha256Pdf,
            "members" to members.map { mapOf("idAluno" to it.idAluno, "papel" to it.papel) },
            "examiners" to examiners.map { mapOf("idProfessor" to it.idProfessor, "papel" to it.papel, "nota" to it.nota) },
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
        )
}
