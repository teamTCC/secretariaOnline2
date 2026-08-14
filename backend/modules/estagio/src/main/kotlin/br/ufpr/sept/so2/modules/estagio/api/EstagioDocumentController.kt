package br.ufpr.sept.so2.modules.estagio.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipDocumentEntity
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipDocumentJpaRepository
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class GenerateUploadUrlDto(
    @field:NotBlank val tipo: String,
    @field:NotBlank val nomeOriginal: String,
    @field:NotBlank val contentType: String,
)

data class RegisterDocumentDto(
    @field:NotBlank val tipo: String,
    @field:NotBlank val storageKey: String,
    @field:NotBlank val sha256: String,
    @field:NotBlank val nomeOriginal: String,
)

@RestController
@RequestMapping("/internships/{id}/documents")
@Tag(name = "Estágios — Documentos", description = "Upload e gestão de documentos de estágio via MinIO")
class EstagioDocumentController(
    private val internshipRepo: InternshipJpaRepository,
    private val documentRepo: InternshipDocumentJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar documentos do estágio")
    fun list(
        @PathVariable id: UUID,
    ): List<Map<String, Any?>> {
        val internship = internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        assertCanViewInternship(internship)
        return documentRepo.findAllByIdInternship(id).map { doc ->
            mapOf(
                "id" to doc.id,
                "tipo" to doc.tipo,
                "nomeOriginal" to doc.nomeOriginal,
                "storageKey" to doc.storageKey,
                "uploadedAt" to doc.uploadedAt,
            )
        }
    }

    @PostMapping("/upload-url")
    @PreAuthorize("hasAuthority('internship.upload_doc_own')")
    @Operation(summary = "Gerar URL presignada MinIO para upload de documento")
    fun generateUploadUrl(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: GenerateUploadUrlDto,
    ): ResponseEntity<Map<String, String>> {
        val internship = internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        val userId = currentUserId()
        require(internship.idAluno == userId) { "Você não é o dono deste estágio." }
        val storageKey = "internships/$id/${UUID.randomUUID()}_${dto.nomeOriginal}"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, dto.contentType)
        return ResponseEntity.ok(mapOf("uploadUrl" to uploadUrl, "storageKey" to storageKey))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('internship.upload_doc_own')")
    @Operation(summary = "Registrar documento após upload MinIO confirmado")
    @Transactional
    fun registerDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RegisterDocumentDto,
    ): ResponseEntity<Map<String, Any>> {
        val internship = internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        val userId = currentUserId()
        require(internship.idAluno == userId) { "Você não é o dono deste estágio." }
        val doc =
            InternshipDocumentEntity(
                idInternship = id,
                tipo = dto.tipo,
                storageKey = dto.storageKey,
                sha256 = dto.sha256,
                nomeOriginal = dto.nomeOriginal,
            )
        val saved = documentRepo.save(doc)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "tipo" to saved.tipo, "nomeOriginal" to saved.nomeOriginal),
        )
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasAuthority('internship.upload_doc_own')")
    @Operation(summary = "Remover documento do estágio e do MinIO")
    @Transactional
    fun deleteDocument(
        @PathVariable id: UUID,
        @PathVariable docId: UUID,
    ): ResponseEntity<Void> {
        val internship = internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        val userId = currentUserId()
        require(internship.idAluno == userId) { "Você não é o dono deste estágio." }
        val doc = documentRepo.findById(docId).orElseThrow { NoSuchElementException("Documento não encontrado: $docId") }
        require(doc.idInternship == id) { "Documento não pertence a este estágio." }
        minioStorageService.delete(doc.storageKey)
        documentRepo.delete(doc)
        return ResponseEntity.noContent().build()
    }

    private fun assertCanViewInternship(internship: br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipEntity) {
        val user = currentUser()
        val isOwner = internship.idAluno == user.userId
        val isSupervisor = internship.idSupervisor == user.userId
        val canReview = user.authorities.contains("internship.review")
        if (!isOwner && !isSupervisor && !canReview) {
            throw AccessDeniedException("Acesso negado aos documentos do estágio ${internship.id}")
        }
    }
}
