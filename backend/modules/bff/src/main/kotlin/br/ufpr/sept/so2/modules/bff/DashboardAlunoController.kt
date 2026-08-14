package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ServiceRecordJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bff/dashboard")
@Tag(name = "BFF — Dashboard", description = "Agregadores de dados para os dashboards de cada perfil (reduz round-trips)")
class DashboardAlunoController(
    private val requestRepo: RequestJpaRepository,
    private val eventRepo: EventAttendanceJpaRepository,
    private val formativeEntryRepo: FormativeEntryJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
    private val certificateRepo: CertificateJpaRepository,
    private val tccRepo: TccJpaRepository,
    private val communicationDeliveryRepo: CommunicationDeliveryJpaRepository,
    private val serviceRecordRepo: ServiceRecordJpaRepository,
    private val cacheManager: CacheManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/aluno")
    @PreAuthorize("hasAuthority('dashboard.view_own')")
    @Operation(
        summary = "Dashboard do Aluno",
        description = "Retorna saudação, KPIs de horas formativas, pendências, " +
            "eventos com janela aberta, últimas solicitações e prazos em uma única chamada. " +
            "Responde sempre 200; blocos com falha chegam como null e _degraded=true é incluído.",
    )
    fun dashboardAluno(): Map<String, Any?> {
        val user = currentUser()
        val alunoId = user.userId

        val cacheKey = "aluno:$alunoId"
        val cache = cacheManager.getCache("bff-dashboard")

        @Suppress("UNCHECKED_CAST")
        cache?.get(cacheKey)?.get()?.let { return it as Map<String, Any?> }

        var degraded = false

        val pendencias =
            try {
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
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar pendencias — {}", alunoId, e.message)
                degraded = true
                null
            }

        val eventos =
            try {
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
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar eventos — {}", alunoId, e.message)
                degraded = true
                null
            }

        val horasRequeridas = 120.0
        val horasAprovadas =
            try {
                formativeEntryRepo.sumHorasAprovadas(alunoId)
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar horas formativas — {}", alunoId, e.message)
                degraded = true
                0.0
            }

        val ultimasSolicitacoes =
            try {
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
            } catch (e: Exception) {
                log.warn("Dashboard aluno={}: falha ao carregar ultimas solicitacoes — {}", alunoId, e.message)
                degraded = true
                null
            }

        val result = buildMap {
            put(
                "kpis",
                mapOf(
                    "horasFormativas" to
                        mapOf(
                            "atual" to horasAprovadas,
                            "requerido" to horasRequeridas,
                            "percentual" to (horasAprovadas / horasRequeridas * 100).coerceAtMost(100.0),
                        ),
                    "atendimentosPendentes" to
                        try {
                            serviceRecordRepo.countByIdAlunoAndEstado(alunoId, "PENDENTE_CIENCIA")
                        } catch (e: Exception) {
                            log.warn("Dashboard aluno={}: falha ao contar atendimentos — {}", alunoId, e.message)
                            degraded = true
                            null
                        },
                ),
            )
            put("pendencias", pendencias)
            put("eventos", eventos)
            put("ultimasSolicitacoes", ultimasSolicitacoes)
            put(
                "_links",
                mapOf(
                    "self" to "/bff/dashboard/aluno",
                    "novaSolicitacao" to "/requests/types",
                    "formativas" to "/formativas/minhas",
                    "eventos" to "/events?audience=me",
                ),
            )
            if (degraded) put("_degraded", true)
        }

        if (!result.containsKey("_degraded")) {
            cache?.put(cacheKey, result)
        }

        return result
    }

    @GetMapping("/professor")
    @PreAuthorize("hasAuthority('dashboard.view_self_professor')")
    @Operation(summary = "Dashboard do Professor — pendências de deliberação e eventos ativos")
    fun dashboardProfessor(): Map<String, Any?> {
        val user = currentUser()
        val professorId = user.userId

        val cacheKey = "professor:$professorId"
        val cache = cacheManager.getCache("bff-dashboard")

        @Suppress("UNCHECKED_CAST")
        cache?.get(cacheKey)?.get()?.let { return it as Map<String, Any?> }

        var degraded = false

        val meusEventos =
            try {
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
            } catch (e: Exception) {
                log.warn("Dashboard professor={}: falha ao carregar eventos — {}", professorId, e.message)
                degraded = true
                null
            }

        val solicitacoesPendentes =
            try {
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
            } catch (e: Exception) {
                log.warn("Dashboard professor={}: falha ao carregar solicitacoes — {}", professorId, e.message)
                degraded = true
                null
            }

        val result = buildMap {
            put("meusEventos", meusEventos)
            put("solicitacoesPendentes", solicitacoesPendentes)
            put(
                "_links",
                mapOf(
                    "self" to "/bff/dashboard/professor",
                    "novoEvento" to "/events",
                    "meuEventos" to "/events?host=me",
                ),
            )
            if (degraded) put("_degraded", true)
        }

        if (!result.containsKey("_degraded")) {
            cache?.put(cacheKey, result)
        }

        return result
    }

    @GetMapping("/egresso")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Dashboard do Egresso",
        description = "Retorna dados consolidados do egresso: TCCs defendidos, certificados e comunicados recentes.",
    )
    fun dashboardEgresso(): Map<String, Any?> {
        val user = currentUser()
        val egressoId = user.userId

        val cacheKey = "egresso:$egressoId"
        val cache = cacheManager.getCache("bff-dashboard")

        @Suppress("UNCHECKED_CAST")
        cache?.get(cacheKey)?.get()?.let { return it as Map<String, Any?> }

        var degraded = false

        val usuarioInfo =
            try {
                usuarioRepo.findById(egressoId).map { u ->
                    mapOf("nome" to u.nome, "email" to u.email, "grr" to u.grr)
                }.orElse(null)
            } catch (e: Exception) {
                log.warn("Dashboard egresso={}: falha ao carregar usuário — {}", egressoId, e.message)
                degraded = true
                null
            }

        val tccsDefendidos =
            try {
                tccRepo.findByAluno(egressoId).count { it.aprovado == true }
            } catch (e: Exception) {
                log.warn("Dashboard egresso={}: falha ao carregar TCCs — {}", egressoId, e.message)
                degraded = true
                0
            }

        val certificados =
            try {
                certificateRepo.findAllByIdAluno(egressoId).map { c ->
                    mapOf(
                        "id" to c.id,
                        "hashSha256" to c.hashSha256,
                        "createdAt" to c.createdAt,
                        "_link" to "/certificates/${c.id}",
                    )
                }
            } catch (e: Exception) {
                log.warn("Dashboard egresso={}: falha ao carregar certificados — {}", egressoId, e.message)
                degraded = true
                null
            }

        val comunicados =
            try {
                communicationDeliveryRepo
                    .findAllByIdUsuarioOrderByDeliveredAtDesc(egressoId, PageRequest.of(0, 5))
                    .content
                    .map { d ->
                        mapOf(
                            "id" to d.id,
                            "idCommunication" to d.idCommunication,
                            "deliveredAt" to d.deliveredAt,
                            "readAt" to d.readAt,
                        )
                    }
            } catch (e: Exception) {
                log.warn("Dashboard egresso={}: falha ao carregar comunicados — {}", egressoId, e.message)
                degraded = true
                null
            }

        val result = buildMap {
            put("usuario", usuarioInfo)
            put("tccsDefendidos", tccsDefendidos)
            put("certificados", certificados)
            put("comunicados", comunicados)
            put(
                "_links",
                mapOf(
                    "self" to "/bff/dashboard/egresso",
                    "certificados" to "/certificates/mine",
                    "comunicados" to "/communications/me",
                ),
            )
            if (degraded) put("_degraded", true)
        }

        if (!result.containsKey("_degraded")) {
            cache?.put(cacheKey, result)
        }

        return result
    }

    @GetMapping("/secretaria")
    @PreAuthorize("hasAuthority('dashboard.view_secretary')")
    @Operation(summary = "Dashboard da Secretaria — fila de solicitações e prazos críticos")
    fun dashboardSecretaria(): Map<String, Any?> {
        val cacheKey = "secretaria:static"
        val cache = cacheManager.getCache("bff-dashboard")

        @Suppress("UNCHECKED_CAST")
        cache?.get(cacheKey)?.get()?.let { return it as Map<String, Any?> }

        var degraded = false

        val emTriagem =
            try {
                requestRepo.countByEstado("ABERTA")
            } catch (e: Exception) {
                log.warn("Dashboard secretaria: falha ao carregar emTriagem — {}", e.message)
                degraded = true
                null
            }

        val emDeliberacao =
            try {
                requestRepo.countByEstado("EM_DELIBERACAO")
            } catch (e: Exception) {
                log.warn("Dashboard secretaria: falha ao carregar emDeliberacao — {}", e.message)
                degraded = true
                null
            }

        val result = buildMap {
            put("kpis", mapOf("emTriagem" to emTriagem, "emDeliberacao" to emDeliberacao))
            put(
                "_links",
                mapOf(
                    "self" to "/bff/dashboard/secretaria",
                    "solicitacoes" to "/requests",
                    "usuarios" to "/usuarios",
                ),
            )
            if (degraded) put("_degraded", true)
        }

        if (!result.containsKey("_degraded")) {
            cache?.put(cacheKey, result)
        }

        return result
    }
}
