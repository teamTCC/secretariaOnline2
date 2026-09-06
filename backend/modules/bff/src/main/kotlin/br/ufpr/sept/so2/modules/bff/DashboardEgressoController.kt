package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.DashboardEgressoQuery
import br.ufpr.sept.so2.modules.bff.dto.DashboardEgressoResponse
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bff/dashboard")
@Tag(name = "BFF — Dashboard")
class DashboardEgressoController(
    private val query: DashboardEgressoQuery,
) {
    @GetMapping("/egresso")
    @PreAuthorize("hasAuthority('alumni.view_own')")
    @Operation(
        summary = "Dashboard do Egresso",
        description = "Retorna dados consolidados do egresso: TCCs defendidos, certificados e comunicados recentes.",
    )
    fun dashboard(): DashboardEgressoResponse = query.execute(currentUser().userId)
}
