package br.ufpr.sept.so2.modules.estagio.api

import br.ufpr.sept.so2.modules.estagio.api.dto.AtualizarEstagioDto
import br.ufpr.sept.so2.modules.estagio.api.dto.DeclararEstagioDto
import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioConcludeResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioCreatedResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioDetailResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioSummaryResponse
import br.ufpr.sept.so2.modules.estagio.application.AtualizarEstagioCommand
import br.ufpr.sept.so2.modules.estagio.application.AtualizarEstagioUseCase
import br.ufpr.sept.so2.modules.estagio.application.ConcludeEstagioCommand
import br.ufpr.sept.so2.modules.estagio.application.DeclararEstagioCommand
import br.ufpr.sept.so2.modules.estagio.application.DeclararEstagioUseCase
import br.ufpr.sept.so2.modules.estagio.application.EncerrarEstagioUseCase
import br.ufpr.sept.so2.modules.estagio.application.EstagioQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internships")
@Tag(name = "Estágios", description = "Gestão de estágios obrigatórios e não-obrigatórios")
class EstagioController(
    private val estagioQuery: EstagioQuery,
    private val declararEstagioUseCase: DeclararEstagioUseCase,
    private val atualizarEstagioUseCase: AtualizarEstagioUseCase,
    private val encerrarEstagioUseCase: EncerrarEstagioUseCase,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('internship.view_own')")
    @Operation(summary = "Declarar início de estágio (ALUNO)")
    fun declarar(
        @Valid @RequestBody dto: DeclararEstagioDto,
    ): ResponseEntity<EstagioCreatedResponse> {
        val result =
            declararEstagioUseCase.execute(
                DeclararEstagioCommand(
                    idAluno = currentUserId(),
                    empresa = dto.empresa,
                    cargo = dto.cargo,
                    cargaHorariaSemanal = dto.cargaHorariaSemanal,
                    inicio = dto.inicio,
                    observacoes = dto.observacoes,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            EstagioCreatedResponse(id = result.id, estado = result.estado),
        )
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('internship.view_own')")
    @Operation(summary = "Listar estágios do aluno autenticado")
    fun mine(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<EstagioSummaryResponse> = estagioQuery.mine(currentUserId(), pageable)

    @GetMapping
    @PreAuthorize("hasAuthority('internship.review') or hasAuthority('internship.supervise')")
    @Operation(summary = "Listar estágios — COE vê todos, supervisor vê seus supervisionados")
    fun list(
        @RequestParam(defaultValue = "EM_ANDAMENTO") estado: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<EstagioSummaryResponse> {
        val user = currentUser()
        return estagioQuery.list(estado, user.userId, user.authorities, pageable)
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhe de estágio")
    fun get(
        @PathVariable id: UUID,
    ): EstagioDetailResponse {
        val user = currentUser()
        return estagioQuery.get(id, user.userId, user.authorities)
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('internship.supervise') or hasAuthority('internship.review')")
    @Operation(summary = "Atualizar dados do estágio (supervisor/COE)")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AtualizarEstagioDto,
    ): ResponseEntity<EstagioDetailResponse> {
        val internshipId =
            atualizarEstagioUseCase.execute(
                AtualizarEstagioCommand(
                    id = id,
                    cargo = dto.cargo,
                    cargaHorariaSemanal = dto.cargaHorariaSemanal,
                    fim = dto.fim,
                    observacoes = dto.observacoes,
                    idSupervisor = dto.idSupervisor,
                ),
            )
        val user = currentUser()
        return ResponseEntity.ok(estagioQuery.get(internshipId, user.userId, user.authorities))
    }

    @PostMapping("/{id}/conclude")
    @PreAuthorize("hasAuthority('internship.review')")
    @Operation(summary = "Concluir estágio (COE)")
    fun conclude(
        @PathVariable id: UUID,
    ): ResponseEntity<EstagioConcludeResponse> {
        val result = encerrarEstagioUseCase.conclude(ConcludeEstagioCommand(id = id))
        return ResponseEntity.ok(EstagioConcludeResponse(estado = result.estado, fim = result.fim))
    }
}
