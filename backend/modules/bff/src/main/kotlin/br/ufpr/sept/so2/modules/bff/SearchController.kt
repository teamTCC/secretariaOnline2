package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.SearchQuery
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SearchResultItem(
    val type: String,
    val id: UUID,
    val title: String,
    val subtitle: String,
    val href: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SearchResponse(
    val query: String,
    val results: List<SearchResultItem>,
    val totalResults: Int,
    val timedOut: Boolean? = null,
)

@RestController
@RequestMapping("/search")
@Tag(name = "BFF — Busca Global", description = "Fan-out de busca textual por alunos, eventos, solicitações e cursos")
class SearchController(
    private val searchQuery: SearchQuery,
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
    ): SearchResponse {
        if (q.isBlank()) {
            return SearchResponse(query = q, results = emptyList(), totalResults = 0)
        }

        return try {
            CompletableFuture
                .supplyAsync { searchQuery.execute(q, types, page, size) }
                .get(5, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            SearchResponse(query = q, results = emptyList(), totalResults = 0, timedOut = true)
        } catch (e: Exception) {
            val cause = e.cause
            if (cause is TimeoutException) {
                SearchResponse(query = q, results = emptyList(), totalResults = 0, timedOut = true)
            } else {
                throw e
            }
        }
    }
}
