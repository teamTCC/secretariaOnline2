package br.ufpr.sept.so2.modules.estagio.api

import br.ufpr.sept.so2.modules.estagio.api.dto.DocumentUploadUrlResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioDocumentResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.GenerateUploadUrlDto
import br.ufpr.sept.so2.modules.estagio.api.dto.RegisterDocumentDto
import br.ufpr.sept.so2.modules.estagio.api.dto.RegisteredDocumentResponse
import br.ufpr.sept.so2.modules.estagio.application.DeleteDocCommand
import br.ufpr.sept.so2.modules.estagio.application.EstagioDocumentQuery
import br.ufpr.sept.so2.modules.estagio.application.GenerateDocUploadUrlCommand
import br.ufpr.sept.so2.modules.estagio.application.ManageEstagioDocumentUseCase
import br.ufpr.sept.so2.modules.estagio.application.RegisterDocCommand
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internships/{id}/documents")
@Tag(name = "Estágios — Documentos", description = "Upload e gestão de documentos de estágio via MinIO")
class EstagioDocumentController(
    private val estagioDocumentQuery: EstagioDocumentQuery,
    private val manageEstagioDocumentUseCase: ManageEstagioDocumentUseCase,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar documentos do estágio")
    fun list(
        @PathVariable id: UUID,
    ): List<EstagioDocumentResponse> {
        val user = currentUser()
        return estagioDocumentQuery.list(id, user.userId, user.authorities)
    }

    @PostMapping("/upload-url")
    @PreAuthorize("hasAuthority('internship.upload_doc_own')")
    @Operation(summary = "Gerar URL presignada MinIO para upload de documento")
    fun generateUploadUrl(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: GenerateUploadUrlDto,
    ): ResponseEntity<DocumentUploadUrlResponse> {
        val result =
            manageEstagioDocumentUseCase.generateUploadUrl(
                GenerateDocUploadUrlCommand(
                    idInternship = id,
                    tipo = dto.tipo,
                    nomeOriginal = dto.nomeOriginal,
                    contentType = dto.contentType,
                    idAluno = currentUserId(),
                ),
            )
        return ResponseEntity.ok(DocumentUploadUrlResponse(uploadUrl = result.uploadUrl, storageKey = result.storageKey))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('internship.upload_doc_own')")
    @Operation(summary = "Registrar documento após upload MinIO confirmado")
    fun registerDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RegisterDocumentDto,
    ): ResponseEntity<RegisteredDocumentResponse> {
        val result =
            manageEstagioDocumentUseCase.registerDocument(
                RegisterDocCommand(
                    idInternship = id,
                    tipo = dto.tipo,
                    storageKey = dto.storageKey,
                    sha256 = dto.sha256,
                    nomeOriginal = dto.nomeOriginal,
                    idAluno = currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            RegisteredDocumentResponse(id = result.id, tipo = result.tipo, nomeOriginal = result.nomeOriginal),
        )
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasAuthority('internship.upload_doc_own')")
    @Operation(summary = "Remover documento do estágio e do MinIO")
    fun deleteDocument(
        @PathVariable id: UUID,
        @PathVariable docId: UUID,
    ): ResponseEntity<Void> {
        manageEstagioDocumentUseCase.deleteDocument(
            DeleteDocCommand(
                idInternship = id,
                docId = docId,
                idAluno = currentUserId(),
            ),
        )
        return ResponseEntity.noContent().build()
    }
}
