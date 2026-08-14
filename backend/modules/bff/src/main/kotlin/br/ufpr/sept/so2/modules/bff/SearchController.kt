package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RestController
@RequestMapping("/search")
@Tag(name = "BFF — Busca Global", description = "Fan-out de busca textual por alunos, eventos, solicitações e cursos")
class SearchController(
    private val usuarioRepo: UsuarioJpaRepository,
    private val eventRepo: EventAttendanceJpaRepository,
    private val requestRepo: RequestJpaRepository,
    private val cursoRepo: CursoJpaRepository,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Busca global",
        description = "Fan-out por múltiplos repositórios. Use o parâmetro `types` para filtrar: " +
            "USUARIO, EVENTO, REQUEST, CURSO (separados por vírgula). Sem `types`, busca em todos.",
    )
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false) types: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): Map<String, Any?> {
        if (q.isBlank()) {
            return mapOf("query" to q, "results" to emptyList<Any>(), "totalResults" to 0)
        }

        return try {
            CompletableFuture
                .supplyAsync { executeSearch(q, types, page, size) }
                .get(5, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            mapOf(
                "query" to q,
                "results" to emptyList<Any>(),
                "totalResults" to 0,
                "timedOut" to true,
            )
        } catch (e: Exception) {
            val cause = e.cause
            if (cause is TimeoutException) {
                mapOf("query" to q, "results" to emptyList<Any>(), "totalResults" to 0, "timedOut" to true)
            } else {
                throw e
            }
        }
    }

    private fun executeSearch(
        q: String,
        types: String?,
        page: Int,
        size: Int,
    ): Map<String, Any?> {
        val requestedTypes =
            types
                ?.uppercase()
                ?.split(",")
                ?.map { it.trim() }
                ?.toSet()
                ?: setOf("USUARIO", "EVENTO", "REQUEST", "CURSO")

        val user = currentUser()
        val canSearchUsers =
            user.authorities.any {
                it == "user.manage_students" || it == "user.manage_all" || it == "system.admin"
            }
        val canViewAllRequests =
            user.authorities.any { it == "request.view_curso" || it == "request.deliberate" }

        val pageable = PageRequest.of(page, size.coerceAtMost(50))
        val results = mutableListOf<Map<String, Any?>>()

        if ("USUARIO" in requestedTypes && canSearchUsers) {
            usuarioRepo.searchByQ(q, pageable).forEach { u ->
                results += mapOf(
                    "type" to "USUARIO",
                    "id" to u.id,
                    "title" to u.nome,
                    "subtitle" to u.email,
                    "href" to "/usuarios/${u.id}",
                )
            }
        }

        if ("EVENTO" in requestedTypes) {
            eventRepo.searchByTitulo(q, pageable).forEach { e ->
                results += mapOf(
                    "type" to "EVENTO",
                    "id" to e.id,
                    "title" to e.titulo,
                    "subtitle" to e.estado,
                    "href" to "/events/${e.id}",
                )
            }
        }

        if ("REQUEST" in requestedTypes) {
            val requests =
                if (canViewAllRequests) {
                    requestRepo.searchByQ(q, pageable)
                } else {
                    requestRepo.searchByQAndSolicitante(q, user.userId, pageable)
                }
            requests.forEach { r ->
                results += mapOf(
                    "type" to "REQUEST",
                    "id" to r.id,
                    "title" to r.requestTypeCode,
                    "subtitle" to r.estado,
                    "href" to "/requests/${r.id}",
                )
            }
        }

        if ("CURSO" in requestedTypes) {
            cursoRepo.findAllByAtivoTrue()
                .filter { c ->
                    c.nome.contains(q, ignoreCase = true) || c.sigla.contains(q, ignoreCase = true)
                }
                .forEach { c ->
                    results += mapOf(
                        "type" to "CURSO",
                        "id" to c.id,
                        "title" to c.nome,
                        "subtitle" to c.sigla,
                        "href" to "/academico/cursos/${c.id}",
                    )
                }
        }

        return mapOf(
            "query" to q,
            "results" to results,
            "totalResults" to results.size,
        )
    }
}
