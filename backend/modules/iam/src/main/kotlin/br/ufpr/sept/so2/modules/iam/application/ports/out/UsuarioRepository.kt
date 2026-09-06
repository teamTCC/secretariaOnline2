package br.ufpr.sept.so2.modules.iam.application.ports.out

import br.ufpr.sept.so2.modules.iam.domain.Usuario
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface UsuarioRepository {
    fun findById(id: UUID): Usuario?

    fun findByIdentificador(identificador: String): Usuario?

    fun findByEmail(email: String): Usuario?

    fun findByGrr(grr: String): Usuario?

    fun save(usuario: Usuario): Usuario

    fun findAll(pageable: Pageable): Page<Usuario>

    fun existsByEmail(email: String): Boolean

    fun existsByGrr(grr: String): Boolean

    fun updateFailedAttempts(
        id: UUID,
        attempts: Int,
        bloqueadoAte: java.time.OffsetDateTime?,
    )

    fun updatePassword(
        id: UUID,
        newHash: String,
    )

    fun updateMetadata(
        id: UUID,
        metadata: Map<String, Any>,
    )

    fun invalidateAllSessions(id: UUID)
}
