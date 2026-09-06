package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.DashboardAlunoQuery
import br.ufpr.sept.so2.modules.bff.dto.DashboardAlunoResponse
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bff/dashboard")
@Tag(name = "BFF — Dashboard", description = "Agregadores de dados para os dashboards de cada perfil (reduz round-trips)")
class DashboardAlunoController(
    private val query: DashboardAlunoQuery,
) {
    @GetMapping("/aluno")
    @PreAuthorize("hasAuthority('dashboard.view_own')")
    @Operation(
        summary = "Dashboard do Aluno",
        description = "Retorna saudação, KPIs de horas formativas, pendências, " +
            "eventos com janela aberta, últimas solicitações e prazos em uma única chamada. " +
            "Responde sempre 200; blocos com falha chegam como null e _degraded=true é incluído.",
    )
    fun dashboard(): DashboardAlunoResponse {
        val user = currentUser()
        return query.execute(user.userId, user.authorities)
    }
}
