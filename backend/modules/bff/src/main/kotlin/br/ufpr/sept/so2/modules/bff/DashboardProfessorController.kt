package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.DashboardProfessorQuery
import br.ufpr.sept.so2.modules.bff.dto.DashboardProfessorResponse
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
class DashboardProfessorController(
    private val query: DashboardProfessorQuery,
) {
    @GetMapping("/professor")
    @PreAuthorize("hasAuthority('dashboard.view_self_professor')")
    @Operation(summary = "Dashboard do Professor — pendências de deliberação e eventos ativos")
    fun dashboard(): DashboardProfessorResponse = query.execute(currentUser().userId)
}
