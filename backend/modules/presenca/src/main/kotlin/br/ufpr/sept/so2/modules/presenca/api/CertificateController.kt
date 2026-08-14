package br.ufpr.sept.so2.modules.presenca.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateJpaRepository
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/certificates")
@Tag(name = "Certificados", description = "Certificados de participação emitidos pelo sistema (aluno autenticado)")
class CertificateController(
    private val certificateRepo: CertificateJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar certificados do aluno autenticado")
    fun mine(): List<Map<String, Any?>> {
        val user = currentUser()
        return certificateRepo.findAllByIdAluno(user.userId).map { c ->
            mapOf(
                "id" to c.id,
                "idEvento" to c.idEvento,
                "origem" to c.origem,
                "hashSha256" to c.hashSha256,
                "chCreditadas" to c.chCreditadas,
                "issuedAt" to c.issuedAt,
                "_links" to
                    mapOf(
                        "download" to "/certificates/${c.id}/download-url",
                        "verify" to "/publico/verificar-certificado/${c.hashSha256}",
                    ),
            )
        }
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "URL presignada MinIO para download do certificado (dono ou event.manage)")
    fun downloadUrl(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, String>> {
        val cert =
            certificateRepo.findById(id).orElseThrow { NoSuchElementException("Certificado não encontrado: $id") }
        val user = currentUser()
        val allowed = cert.idAluno == user.userId || user.authorities.contains("event.manage")
        if (!allowed) {
            throw AccessDeniedException("Acesso negado ao certificado $id")
        }
        val downloadUrl = minioStorageService.generateDownloadUrl(cert.storageKey, expiryMinutes = 15)
        return ResponseEntity.ok(mapOf("downloadUrl" to downloadUrl))
    }
}
