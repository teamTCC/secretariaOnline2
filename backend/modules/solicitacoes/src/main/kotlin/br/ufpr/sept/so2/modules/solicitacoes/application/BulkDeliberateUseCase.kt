package br.ufpr.sept.so2.modules.solicitacoes.application

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class BulkDeliberateCommand(
    val ids: List<UUID>,
    val action: String,
    val actorId: UUID,
    val actorAuthorities: Set<String>,
    val parecer: String? = null,
)

@Service
@Transactional
class BulkDeliberateUseCase(
    private val transitionUseCase: TransitionRequestUseCase,
) {
    fun execute(cmd: BulkDeliberateCommand): Int {
        try {
            cmd.ids.forEach { id ->
                transitionUseCase.execute(
                    TransitionCommand(
                        requestId = id,
                        action = cmd.action,
                        actorId = cmd.actorId,
                        actorAuthorities = cmd.actorAuthorities,
                        parecer = cmd.parecer,
                    ),
                )
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Falha parcial na deliberação em lote: ${e.message}",
                e,
            )
        }
        return cmd.ids.size
    }
}
