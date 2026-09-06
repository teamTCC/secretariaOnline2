package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.academico.application.ports.out.AcademicoReadPort
import br.ufpr.sept.so2.modules.bff.SearchResponse
import br.ufpr.sept.so2.modules.bff.SearchResultItem
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamBffReadPort
import br.ufpr.sept.so2.modules.presenca.application.ports.out.PresencaBffReadPort
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoBffReadPort
import br.ufpr.sept.so2.shared.security.currentUser
import org.springframework.stereotype.Component

@Component
class SearchQuery(
    private val iam: IamBffReadPort,
    private val events: PresencaBffReadPort,
    private val requests: SolicitacaoBffReadPort,
    private val academico: AcademicoReadPort,
) {
    fun execute(
        q: String,
        types: String?,
        page: Int,
        size: Int,
    ): SearchResponse {
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

        val pageSize = size.coerceAtMost(50)
        val results = mutableListOf<SearchResultItem>()

        if ("USUARIO" in requestedTypes && canSearchUsers) {
            iam.search(q, page, pageSize).forEach { u ->
                results +=
                    SearchResultItem(
                        type = "USUARIO",
                        id = u.id,
                        title = u.title,
                        subtitle = u.subtitle,
                        href = "/usuarios/${u.id}",
                    )
            }
        }

        if ("EVENTO" in requestedTypes) {
            events.searchByTitulo(q, page, pageSize).forEach { e ->
                results +=
                    SearchResultItem(
                        type = "EVENTO",
                        id = e.id,
                        title = e.titulo,
                        subtitle = e.estado,
                        href = "/events/${e.id}",
                    )
            }
        }

        if ("REQUEST" in requestedTypes) {
            val solicitanteId = if (canViewAllRequests) null else user.userId
            requests.search(q, solicitanteId, page, pageSize).forEach { r ->
                results +=
                    SearchResultItem(
                        type = "REQUEST",
                        id = r.id,
                        title = r.tipo,
                        subtitle = r.estado,
                        href = "/requests/${r.id}",
                    )
            }
        }

        if ("CURSO" in requestedTypes) {
            academico.searchCursos(q).forEach { c ->
                results +=
                    SearchResultItem(
                        type = "CURSO",
                        id = c.id,
                        title = c.nome,
                        subtitle = c.sigla,
                        href = "/academico/cursos/${c.id}",
                    )
            }
        }

        return SearchResponse(query = q, results = results, totalResults = results.size)
    }
}
