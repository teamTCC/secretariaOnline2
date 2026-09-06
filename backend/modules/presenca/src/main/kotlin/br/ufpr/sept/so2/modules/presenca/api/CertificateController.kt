package br.ufpr.sept.so2.modules.presenca.api

import br.ufpr.sept.so2.modules.presenca.api.dto.CertificateSummaryResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.DownloadUrlResponse
import br.ufpr.sept.so2.modules.presenca.application.CertificateQuery
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
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
    private val certificateQuery: CertificateQuery,
) {
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar certificados do aluno autenticado")
    fun mine(): List<CertificateSummaryResponse> = certificateQuery.mine(currentUser().userId)

    @GetMapping("/{id}/download-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "URL presignada MinIO para download do certificado (dono ou event.manage)")
    fun downloadUrl(
        @PathVariable id: UUID,
    ): ResponseEntity<DownloadUrlResponse> {
        val user = currentUser()
        return ResponseEntity.ok(certificateQuery.downloadUrl(id, user.userId, user.authorities))
    }
}
