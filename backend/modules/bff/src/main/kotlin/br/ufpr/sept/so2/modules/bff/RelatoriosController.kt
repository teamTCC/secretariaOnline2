package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/academico")
@Tag(name = "BFF — Relatórios", description = "Agregações de coordenação que cruzam bounded contexts")
class RelatoriosController(
    private val usuarioRepo: UsuarioJpaRepository,
    private val tccRepo: TccJpaRepository,
    private val internshipRepo: InternshipJpaRepository,
    private val requestRepo: RequestJpaRepository,
) {
    @GetMapping("/relatorios/curso")
    @PreAuthorize("hasAuthority('dashboard.view_secretary')")
    @Operation(summary = "Relatório agregado do curso — alunos, TCC, estágios e solicitações")
    fun relatorioCurso(): Map<String, Any> =
        mapOf(
            "totalAlunos" to usuarioRepo.countByAtivoTrueAndGrrIsNotNull(),
            "tccEmAndamento" to tccRepo.countByEstado("EM_ANDAMENTO"),
            "estagiosAtivos" to internshipRepo.countByEstado("EM_ANDAMENTO"),
            "solicitacoesAbertas" to requestRepo.countByEstado("ABERTA"),
        )
}
