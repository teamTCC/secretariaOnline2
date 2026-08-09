package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.application.DataExportUseCase
import br.ufpr.sept.so2.modules.iam.application.RequestDataExportCommand
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints de perfil do usuário autenticado (/me).
 *
 * RF-F1-003-a: PATCH /me — editar dados pessoais (TODO: P2)
 * RF-F1-003-b: POST /me/password — trocar senha (TODO: P2)
 * RF-F1-003-c: PATCH /me/notifications — preferências (TODO: P2)
 * RF-F1-003-d: POST /me/data-export — exportação LGPD (estrutura base)
 */
@RestController
@RequestMapping("/me")
@Tag(name = "Perfil", description = "Gerenciamento do perfil do usuário autenticado")
class ProfileController(
    private val dataExportUseCase: DataExportUseCase,
) {

    // RF-F1-003-d — Solicitar exportação de dados pessoais (LGPD Art. 18, III)
    @PostMapping("/data-export")
    @PreAuthorize("hasAuthority('user.export_own_data')")
    @Operation(
        summary = "Solicitar exportação de dados pessoais",
        description = """
            Solicita geração assíncrona de arquivo com todos os dados pessoais do usuário
            (direito de portabilidade — LGPD Art. 18, III).
            Retorna 202 com jobId para acompanhamento.
            Limite: 1 exportação por usuário por 24 horas.
        """,
    )
    @ApiResponse(responseCode = "202", description = "Exportação solicitada — acompanhe via jobId")
    @ApiResponse(responseCode = "429", description = "Já existe exportação nas últimas 24 horas")
    fun requestDataExport(httpRequest: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val result = dataExportUseCase.requestExport(
            RequestDataExportCommand(
                usuarioId = currentUserId(),
                ip = httpRequest.remoteAddr,
                userAgent = httpRequest.getHeader("User-Agent"),
            ),
        )
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(
                mapOf(
                    "jobId" to result.jobId.toString(),
                    "mensagem" to "Exportação solicitada. O arquivo estará disponível em alguns minutos.",
                ),
            )
    }

    // RF-F1-003-d — Verificar status do job de exportação
    @GetMapping("/data-export/{jobId}")
    @PreAuthorize("hasAuthority('user.export_own_data')")
    @Operation(
        summary = "Verificar status da exportação de dados",
        description = "Retorna PENDING, READY (com URL pré-assinada por 24h) ou EXPIRED.",
    )
    @ApiResponse(responseCode = "200", description = "Status do job de exportação")
    @ApiResponse(responseCode = "404", description = "Job não encontrado ou pertence a outro usuário")
    fun getDataExportStatus(@PathVariable jobId: String): ResponseEntity<Map<String, Any?>> {
        val result = dataExportUseCase.getExportStatus(
            usuarioId = currentUserId(),
            jobId = jobId,
        )
        return ResponseEntity.ok(
            mapOf(
                "jobId" to result.jobId.toString(),
                "status" to result.status.name,
                "downloadUrl" to result.downloadUrl,
                "expiresAt" to result.expiresAt?.toString(),
            ),
        )
    }
}
