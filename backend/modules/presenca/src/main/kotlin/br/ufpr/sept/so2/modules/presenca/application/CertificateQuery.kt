package br.ufpr.sept.so2.modules.presenca.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.presenca.api.dto.CertificateSummaryResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.DownloadUrlResponse
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CertificateQuery(
    private val certificateRepo: CertificateJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    fun mine(alunoId: UUID): List<CertificateSummaryResponse> =
        certificateRepo.findAllByIdAluno(alunoId).map { c ->
            CertificateSummaryResponse(
                id = c.id,
                idEvento = c.idEvento,
                origem = c.origem,
                hashSha256 = c.hashSha256,
                chCreditadas = c.chCreditadas,
                issuedAt = c.issuedAt,
                links =
                    mapOf(
                        "download" to "/certificates/${c.id}/download-url",
                        "verify" to "/publico/verificar-certificado/${c.hashSha256}",
                    ),
            )
        }

    fun downloadUrl(
        id: UUID,
        userId: UUID,
        authorities: Set<String>,
    ): DownloadUrlResponse {
        val cert =
            certificateRepo.findById(id).orElseThrow { NoSuchElementException("Certificado não encontrado: $id") }
        val allowed = cert.idAluno == userId || authorities.contains("event.manage")
        if (!allowed) {
            throw AccessDeniedException("Acesso negado ao certificado $id")
        }
        val downloadUrl = minioStorageService.generateDownloadUrl(cert.storageKey, expiryMinutes = 15)
        return DownloadUrlResponse(downloadUrl = downloadUrl)
    }
}
