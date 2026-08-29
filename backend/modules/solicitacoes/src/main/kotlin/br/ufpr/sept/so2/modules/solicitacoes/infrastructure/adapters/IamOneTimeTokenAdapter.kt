package br.ufpr.sept.so2.modules.solicitacoes.infrastructure.adapters

import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.OneTimeTokenPort
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * Adapter: bridges the solicitacoes port to the IAM JwtTokenService.
 *
 * Lives in the infrastructure layer so the application/domain layers remain
 * free of JWT dependencies. The token has a 3-day TTL — enough time for the
 * student to act on the notification but short enough to reduce risk.
 */
@Component
class IamOneTimeTokenAdapter(
    private val jwtTokenService: JwtTokenService,
) : OneTimeTokenPort {
    override fun issueForRequest(
        subjectId: UUID,
        requestId: UUID,
    ): String =
        jwtTokenService.issueOneTimeToken(
            subject = subjectId,
            audience = "request:$requestId",
            ttl = Duration.ofDays(3),
        )
}
