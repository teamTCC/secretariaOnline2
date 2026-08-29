package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.DashboardSecretariaQuery
import br.ufpr.sept.so2.modules.bff.dto.DashboardSecretariaResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bff/dashboard")
@Tag(name = "BFF — Dashboard")
class DashboardSecretariaController(
    private val query: DashboardSecretariaQuery,
) {
    @GetMapping("/secretaria")
    @PreAuthorize("hasAuthority('dashboard.view_secretary')")
    @Operation(summary = "Dashboard da Secretaria — fila de solicitações e prazos críticos")
    fun dashboard(): DashboardSecretariaResponse = query.execute()
}
