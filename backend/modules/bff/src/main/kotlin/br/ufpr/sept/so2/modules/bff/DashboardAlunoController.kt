package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/bff/dashboard")
@Tag(name = "BFF — Dashboard", description = "Agregadores de dados para os dashboards de cada perfil (reduz round-trips)")
class DashboardAlunoController(
    private val requestRepo: RequestJpaRepository,
    private val eventRepo: EventAttendanceJpaRepository,
    private val formativeEntryRepo: FormativeEntryJpaRepository,
) {
    @GetMapping("/aluno")
    @PreAuthorize("hasAuthority('dashboard.view_own')")
    @Operation(
        summary = "Dashboard do Aluno",
        description = "Retorna saudação, KPIs de horas formativas, pendências, " +
            "eventos com janela aberta, últimas solicitações e prazos em uma única chamada.",
    )
    fun dashboardAluno(): Map<String, Any?> {
        val user = currentUser()
        val alunoId = user.userId

        // Pending requests (awaiting student action)
        val pendencias =
            requestRepo
                .findWithFilters(
                    estado = "EM_AJUSTE",
                    idSolicitante = alunoId,
                    idCurso = null,
                    typeCode = null,
                    pageable = PageRequest.of(0, 3),
                ).content
                .map { r ->
                    mapOf(
                        "id" to r.id,
                        "tipo" to r.requestTypeCode,
                        "estado" to r.estado,
                        "prazoEm" to r.prazoEm,
                        "acao" to "REENVIAR",
                        "_link" to "/requests/${r.id}",
                    )
                }

        // Open events eligible for attendance
        val eventos =
            eventRepo
                .findWithFilters(
                    estado = "EM_ANDAMENTO",
                    idOrganizador = null,
                    idCurso = null,
                    pageable = PageRequest.of(0, 3),
                ).content
                .map { e ->
                    mapOf(
                        "id" to e.id,
                        "titulo" to e.titulo,
                        "chCreditadas" to e.chCreditadas,
                        "fimEm" to e.fimEm,
                        "_link" to "/events/${e.id}/attendance/session",
                    )
                }

        // Formative hours KPI
        val horasAprovadas = formativeEntryRepo.sumHorasAprovadas(alunoId)
        val horasRequeridas = 120.0

        // Recent requests
        val ultimasSolicitacoes =
            requestRepo
                .findWithFilters(
                    estado = null,
                    idSolicitante = alunoId,
                    idCurso = null,
                    typeCode = null,
                    pageable = PageRequest.of(0, 5),
                ).content
                .map { r ->
                    mapOf(
                        "id" to r.id,
                        "tipo" to r.requestTypeCode,
                        "estado" to r.estado,
                        "createdAt" to r.createdAt,
                    )
                }

        return mapOf(
            "kpis" to
                mapOf(
                    "horasFormativas" to
                        mapOf(
                            "atual" to horasAprovadas,
                            "requerido" to horasRequeridas,
                            "percentual" to (horasAprovadas / horasRequeridas * 100).coerceAtMost(100.0),
                        ),
                ),
            "pendencias" to pendencias,
            "eventos" to eventos,
            "ultimasSolicitacoes" to ultimasSolicitacoes,
            "_links" to
                mapOf(
                    "self" to "/bff/dashboard/aluno",
                    "novaSolicitacao" to "/requests/types",
                    "formativas" to "/formativas/minhas",
                    "eventos" to "/events?audience=me",
                ),
        )
    }

    @GetMapping("/professor")
    @PreAuthorize("hasAuthority('dashboard.view_self_professor')")
    @Operation(summary = "Dashboard do Professor — pendências de deliberação e eventos ativos")
    fun dashboardProfessor(): Map<String, Any?> {
        val user = currentUser()
        val professorId = user.userId

        val meusEventos =
            eventRepo
                .findWithFilters(
                    estado = null,
                    idOrganizador = professorId,
                    idCurso = null,
                    pageable = PageRequest.of(0, 5),
                ).content
                .map { e ->
                    mapOf(
                        "id" to e.id,
                        "titulo" to e.titulo,
                        "estado" to e.estado,
                        "inicioEm" to e.inicioEm,
                        "fimEm" to e.fimEm,
                    )
                }

        val solicitacoesPendentes =
            requestRepo
                .findWithFilters(
                    estado = "EM_DELIBERACAO",
                    idSolicitante = null,
                    idCurso = null,
                    typeCode = null,
                    pageable = PageRequest.of(0, 5),
                ).content
                .map { r ->
                    mapOf(
                        "id" to r.id,
                        "tipo" to r.requestTypeCode,
                        "prazoEm" to r.prazoEm,
                        "_link" to "/requests/${r.id}",
                    )
                }

        return mapOf(
            "meusEventos" to meusEventos,
            "solicitacoesPendentes" to solicitacoesPendentes,
            "_links" to
                mapOf(
                    "self" to "/bff/dashboard/professor",
                    "novoEvento" to "/events",
                    "meuEventos" to "/events?host=me",
                ),
        )
    }

    @GetMapping("/secretaria")
    @PreAuthorize("hasAuthority('dashboard.view_secretary')")
    @Operation(summary = "Dashboard da Secretaria — fila de solicitações e prazos críticos")
    fun dashboardSecretaria(): Map<String, Any?> {
        val now = OffsetDateTime.now()

        val emTriagem =
            requestRepo
                .findWithFilters(
                    estado = "ABERTA",
                    idSolicitante = null,
                    idCurso = null,
                    typeCode = null,
                    pageable = PageRequest.of(0, 10),
                ).content
                .count()

        val emDeliberacao =
            requestRepo
                .findWithFilters(
                    estado = "EM_DELIBERACAO",
                    idSolicitante = null,
                    idCurso = null,
                    typeCode = null,
                    pageable = PageRequest.of(0, 10),
                ).content
                .count()

        return mapOf(
            "kpis" to
                mapOf(
                    "emTriagem" to emTriagem,
                    "emDeliberacao" to emDeliberacao,
                ),
            "_links" to
                mapOf(
                    "self" to "/bff/dashboard/secretaria",
                    "solicitacoes" to "/requests",
                    "usuarios" to "/usuarios",
                ),
        )
    }
}
