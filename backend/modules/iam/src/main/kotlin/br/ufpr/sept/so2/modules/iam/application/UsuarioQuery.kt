package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioDetailResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioSummaryResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class UsuarioQuery(
    private val usuarioRepo: UsuarioJpaRepository,
) {
    @Transactional(readOnly = true)
    fun list(
        nome: String?,
        email: String?,
        ativo: Boolean?,
        pageable: Pageable,
    ): PageResponse<UsuarioSummaryResponse> =
        PageResponse.ofWithLinks(usuarioRepo.searchUsuarios(nome, email, ativo, pageable)) { u ->
            UsuarioSummaryResponse(
                id = u.id,
                nome = u.nome,
                email = u.email,
                grr = u.grr,
                ativo = u.ativo,
                roles = u.usuarioRoles.map { it.role.code }.distinct(),
            )
        }

    @Transactional(readOnly = true)
    fun getById(id: UUID): UsuarioDetailResponse {
        val usuario =
            usuarioRepo
                .findByIdWithRoles(id)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }
        return UsuarioDetailResponse(
            id = usuario.id,
            nome = usuario.nome,
            email = usuario.email,
            grr = usuario.grr,
            ativo = usuario.ativo,
            metadata = usuario.metadata,
            roles = usuario.usuarioRoles.map { it.role.code }.distinct(),
            senhaAlterada = usuario.senhaAlterada,
        )
    }
}
