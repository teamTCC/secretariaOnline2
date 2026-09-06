package br.ufpr.sept.so2.modules.formativas.api

import br.ufpr.sept.so2.modules.formativas.api.dto.ComprovanteUploadUrlResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaCreatedResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaPendenteResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaResumoResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaReviewedResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaSummaryResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.GenerateComprovanteUploadUrlDto
import br.ufpr.sept.so2.modules.formativas.api.dto.ReviewFormativaDto
import br.ufpr.sept.so2.modules.formativas.api.dto.SubmitFormativaDto
import br.ufpr.sept.so2.modules.formativas.application.FormativasQuery
import br.ufpr.sept.so2.modules.formativas.application.GenerateComprovanteUploadUrlCommand
import br.ufpr.sept.so2.modules.formativas.application.GenerateComprovanteUploadUrlUseCase
import br.ufpr.sept.so2.modules.formativas.application.ReviewFormativaCommand
import br.ufpr.sept.so2.modules.formativas.application.ReviewFormativaUseCase
import br.ufpr.sept.so2.modules.formativas.application.SubmitFormativaCommand
import br.ufpr.sept.so2.modules.formativas.application.SubmitFormativaUseCase
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/formativas")
@Tag(name = "Horas Formativas", description = "Submissão e revisão de atividades complementares")
class FormativasController(
    private val formativasQuery: FormativasQuery,
    private val generateUploadUrlUseCase: GenerateComprovanteUploadUrlUseCase,
    private val submitFormativaUseCase: SubmitFormativaUseCase,
    private val reviewFormativaUseCase: ReviewFormativaUseCase,
) {
    @PostMapping("/comprovantes/presigned-url")
    @PreAuthorize("hasAuthority('formative.submit')")
    @Operation(summary = "URL presignada MinIO para comprovante (orphan — vincula na submissão)")
    fun generateComprovanteUrl(
        @Valid @RequestBody dto: GenerateComprovanteUploadUrlDto,
    ): ResponseEntity<ComprovanteUploadUrlResponse> {
        val result =
            generateUploadUrlUseCase.execute(
                GenerateComprovanteUploadUrlCommand(filename = dto.filename, contentType = dto.contentType),
            )
        return ResponseEntity.ok(ComprovanteUploadUrlResponse(uploadUrl = result.uploadUrl, storageKey = result.storageKey))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('formative.submit')")
    @Operation(summary = "Submeter atividade formativa para aprovação")
    fun submit(
        @Valid @RequestBody dto: SubmitFormativaDto,
    ): ResponseEntity<FormativaCreatedResponse> {
        val user = currentUser()
        val result =
            submitFormativaUseCase.execute(
                SubmitFormativaCommand(
                    idAluno = user.userId,
                    titulo = dto.titulo,
                    descricao = dto.descricao,
                    categoria = dto.categoria,
                    cargaHoraria = dto.cargaHoraria,
                    dataRealizacao = dto.dataRealizacao,
                    storageKeyComprovante = dto.storageKeyComprovante,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            FormativaCreatedResponse(id = result.id, estado = result.estado),
        )
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAuthority('formative.view_own')")
    @Operation(summary = "Listar minhas atividades formativas")
    fun listOwn(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<FormativaSummaryResponse> = formativasQuery.listOwn(currentUser().userId, pageable)

    @GetMapping("/pendentes")
    @PreAuthorize("hasAuthority('formative.review')")
    @Operation(summary = "Listar atividades pendentes de revisão (CAAF)")
    fun listPendentes(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<FormativaPendenteResponse> = formativasQuery.listPendentes(pageable)

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAuthority('formative.review')")
    @Operation(summary = "Aprovar ou rejeitar atividade formativa (CAAF)")
    fun review(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: ReviewFormativaDto,
    ): ResponseEntity<FormativaReviewedResponse> {
        val user = currentUser()
        val result =
            reviewFormativaUseCase.execute(
                ReviewFormativaCommand(
                    id = id,
                    revisorId = user.userId,
                    acao = dto.acao,
                    parecer = dto.parecer,
                ),
            )
        return ResponseEntity.ok(FormativaReviewedResponse(estado = result.estado))
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasAuthority('formative.view_own')")
    @Operation(summary = "Resumo de horas formativas do aluno autenticado")
    fun resumo(): FormativaResumoResponse = formativasQuery.resumo(currentUser().userId)
}
