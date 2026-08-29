package br.ufpr.sept.so2.modules.tcc.api

import br.ufpr.sept.so2.modules.tcc.api.dto.AddExaminerDto
import br.ufpr.sept.so2.modules.tcc.api.dto.AddMemberDto
import br.ufpr.sept.so2.modules.tcc.api.dto.ApproveDto
import br.ufpr.sept.so2.modules.tcc.api.dto.CreateTccDto
import br.ufpr.sept.so2.modules.tcc.api.dto.GradeDto
import br.ufpr.sept.so2.modules.tcc.api.dto.SubmitFinalConfirmDto
import br.ufpr.sept.so2.modules.tcc.api.dto.SubmitFinalUrlDto
import br.ufpr.sept.so2.modules.tcc.api.dto.TccApproveResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccCreatedResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccDetailResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccExaminerCreatedResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccGradeResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccMemberCreatedResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccSummaryResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccUploadConfirmResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.TccUploadUrlResponse
import br.ufpr.sept.so2.modules.tcc.api.dto.UpdateTccDto
import br.ufpr.sept.so2.modules.tcc.application.AddTccExaminerCommand
import br.ufpr.sept.so2.modules.tcc.application.AddTccMemberCommand
import br.ufpr.sept.so2.modules.tcc.application.ApproveTccCommand
import br.ufpr.sept.so2.modules.tcc.application.ApproveTccUseCase
import br.ufpr.sept.so2.modules.tcc.application.ConfirmUploadCommand
import br.ufpr.sept.so2.modules.tcc.application.CreateTccCommand
import br.ufpr.sept.so2.modules.tcc.application.CreateTccUseCase
import br.ufpr.sept.so2.modules.tcc.application.GenerateUploadUrlCommand
import br.ufpr.sept.so2.modules.tcc.application.GradeDefenseCommand
import br.ufpr.sept.so2.modules.tcc.application.GradeDefenseUseCase
import br.ufpr.sept.so2.modules.tcc.application.ManageTccExaminersUseCase
import br.ufpr.sept.so2.modules.tcc.application.ManageTccMembersUseCase
import br.ufpr.sept.so2.modules.tcc.application.TccQuery
import br.ufpr.sept.so2.modules.tcc.application.UpdateTccCommand
import br.ufpr.sept.so2.modules.tcc.application.UpdateTccUseCase
import br.ufpr.sept.so2.modules.tcc.application.UploadFinalPdfUseCase
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tccs")
@Tag(name = "TCC", description = "Gestão de Trabalhos de Conclusão de Curso")
class TccController(
    private val tccQuery: TccQuery,
    private val createTccUseCase: CreateTccUseCase,
    private val updateTccUseCase: UpdateTccUseCase,
    private val manageTccMembersUseCase: ManageTccMembersUseCase,
    private val manageTccExaminersUseCase: ManageTccExaminersUseCase,
    private val gradeDefenseUseCase: GradeDefenseUseCase,
    private val uploadFinalPdfUseCase: UploadFinalPdfUseCase,
    private val approveTccUseCase: ApproveTccUseCase,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Criar TCC (orientador)")
    fun create(
        @Valid @RequestBody dto: CreateTccDto,
    ): ResponseEntity<TccCreatedResponse> {
        val result =
            createTccUseCase.execute(
                CreateTccCommand(
                    titulo = dto.titulo,
                    idCurso = dto.idCurso,
                    idOrientador = currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            TccCreatedResponse(id = result.id, estado = result.estado, links = mapOf("self" to "/tccs/${result.id}")),
        )
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('tcc.view_own')")
    @Operation(summary = "Listar TCCs do aluno autenticado")
    fun mine(): List<TccSummaryResponse> = tccQuery.mine(currentUserId())

    @GetMapping
    @PreAuthorize("hasAuthority('request.deliberate') or hasAuthority('tcc.supervise')")
    @Operation(summary = "Listar TCCs — secretaria vê todos, orientador vê os seus")
    fun list(
        @RequestParam(defaultValue = "EM_ANDAMENTO") estado: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<TccSummaryResponse> {
        val user = currentUser()
        return tccQuery.list(estado, user.userId, user.authorities, pageable)
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhe de TCC com membros e banca")
    fun get(
        @PathVariable id: UUID,
    ): TccDetailResponse {
        val user = currentUser()
        return tccQuery.get(id, user.userId, user.authorities)
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Atualizar título ou data de defesa (orientador)")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateTccDto,
    ): ResponseEntity<TccDetailResponse> {
        val tccId =
            updateTccUseCase.execute(
                UpdateTccCommand(
                    id = id,
                    titulo = dto.titulo,
                    dataDefesa = dto.dataDefesa,
                    idOrientador = currentUserId(),
                ),
            )
        val user = currentUser()
        return ResponseEntity.ok(tccQuery.get(tccId, user.userId, user.authorities))
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Vincular aluno ao TCC (orientador)")
    fun addMember(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AddMemberDto,
    ): ResponseEntity<TccMemberCreatedResponse> {
        val result =
            manageTccMembersUseCase.addMember(
                AddTccMemberCommand(
                    idTcc = id,
                    idAluno = dto.idAluno,
                    papel = dto.papel,
                    idOrientador = currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            TccMemberCreatedResponse(idTcc = result.idTcc, idAluno = result.idAluno, papel = result.papel),
        )
    }

    @PostMapping("/{id}/examiners")
    @PreAuthorize("hasAuthority('tcc.supervise') or hasAuthority('request.deliberate')")
    @Operation(summary = "Adicionar membro de banca (orientador/secretaria)")
    fun addExaminer(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AddExaminerDto,
    ): ResponseEntity<TccExaminerCreatedResponse> {
        val result =
            manageTccExaminersUseCase.addExaminer(
                AddTccExaminerCommand(
                    idTcc = id,
                    idProfessor = dto.idProfessor,
                    papel = dto.papel,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            TccExaminerCreatedResponse(idTcc = result.idTcc, idProfessor = result.idProfessor, papel = result.papel),
        )
    }

    @PatchMapping("/{id}/grade")
    @PreAuthorize("hasAuthority('tcc.examine')")
    @Operation(summary = "Registrar nota de banca")
    fun grade(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: GradeDto,
    ): ResponseEntity<TccGradeResponse> {
        val result =
            gradeDefenseUseCase.execute(
                GradeDefenseCommand(
                    idTcc = id,
                    idProfessor = currentUserId(),
                    nota = dto.nota,
                ),
            )
        return ResponseEntity.ok(TccGradeResponse(idProfessor = result.idProfessor, nota = result.nota))
    }

    @PostMapping("/{id}/submit-final/url")
    @PreAuthorize("hasAuthority('tcc.upload_final')")
    @Operation(summary = "Gerar URL presignada MinIO para upload do PDF final")
    fun submitFinalUrl(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: SubmitFinalUrlDto,
    ): ResponseEntity<TccUploadUrlResponse> {
        val result =
            uploadFinalPdfUseCase.generateUploadUrl(
                GenerateUploadUrlCommand(
                    idTcc = id,
                    nomeOriginal = dto.nomeOriginal,
                    idAluno = currentUserId(),
                ),
            )
        return ResponseEntity.ok(TccUploadUrlResponse(uploadUrl = result.uploadUrl, storageKey = result.storageKey))
    }

    @PostMapping("/{id}/submit-final/confirm")
    @PreAuthorize("hasAuthority('tcc.upload_final')")
    @Operation(summary = "Registrar PDF final após upload MinIO confirmado")
    fun submitFinalConfirm(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: SubmitFinalConfirmDto,
    ): ResponseEntity<TccUploadConfirmResponse> {
        val result =
            uploadFinalPdfUseCase.confirmUpload(
                ConfirmUploadCommand(
                    idTcc = id,
                    storageKey = dto.storageKey,
                    sha256 = dto.sha256,
                    idAluno = currentUserId(),
                ),
            )
        return ResponseEntity.ok(TccUploadConfirmResponse(id = result.id, hashSha256Pdf = result.hashSha256Pdf))
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('tcc.supervise')")
    @Operation(summary = "Aprovar ou reprovar TCC após banca (orientador)")
    fun approve(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: ApproveDto,
    ): ResponseEntity<TccApproveResponse> {
        val result =
            approveTccUseCase.execute(
                ApproveTccCommand(
                    idTcc = id,
                    aprovado = dto.aprovado,
                    notaFinal = dto.notaFinal,
                    idOrientador = currentUserId(),
                ),
            )
        return ResponseEntity.ok(
            TccApproveResponse(estado = result.estado, aprovado = result.aprovado, notaFinal = result.notaFinal),
        )
    }
}
