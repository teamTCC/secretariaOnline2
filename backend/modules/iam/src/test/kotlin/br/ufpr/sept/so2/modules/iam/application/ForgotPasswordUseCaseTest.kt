package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenServicePort
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.domain.Role
import br.ufpr.sept.so2.modules.iam.domain.Usuario
import br.ufpr.sept.so2.modules.iam.domain.UsuarioRole
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import br.ufpr.sept.so2.shared.domain.valueobject.Email
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.OffsetDateTime
import java.util.UUID

class ForgotPasswordUseCaseTest :
    BehaviorSpec({

        val usuarioRepository = mockk<UsuarioRepository>()
        val tokenService = mockk<TokenServicePort>()
        val outboxPublisher = mockk<OutboxEventPublisher>()
        val auditPublisher = mockk<AuditPublisher>(relaxed = true)

        val useCase =
            ForgotPasswordUseCase(
                usuarioRepository,
                tokenService,
                outboxPublisher,
                auditPublisher,
            )

        fun buildActiveUser(ativo: Boolean = true) =
            Usuario(
                id = UUID.randomUUID(),
                nome = "Ana Silva",
                email = Email.of("ana@ufpr.br"),
                grr = null,
                senhaHash = "\$argon2id\$hashed",
                senhaAlterada = true,
                ativo = ativo,
                bloqueadoAte = null,
                tentativasFalhas = 0,
                metadata = emptyMap(),
                roles =
                    setOf(
                        UsuarioRole(
                            role =
                                Role(
                                    id = UUID.randomUUID(),
                                    code = "ALUNO",
                                    descricao = "Aluno",
                                    authorities = emptySet(),
                                ),
                        ),
                    ),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )

        val command = ForgotPasswordCommand(email = "ana@ufpr.br", ip = "127.0.0.1")

        beforeTest {
            clearMocks(usuarioRepository, tokenService, outboxPublisher, auditPublisher)
        }

        Given("ForgotPasswordUseCase") {

            When("email belongs to an active user") {
                Then("enqueues iam.password_reset_requested in the outbox") {
                    val user = buildActiveUser()
                    every { usuarioRepository.findByEmail("ana@ufpr.br") } returns user
                    every { tokenService.issueOneTimeToken(user.id, "password-reset", any()) } returns "reset.jwt.token"
                    justRun { outboxPublisher.enqueue(any(), any(), any(), any()) }
                    justRun { auditPublisher.publish(any()) }

                    useCase.execute(command)

                    verify {
                        outboxPublisher.enqueue(
                            eventType = OutboxEventTypes.PASSWORD_RESET_REQUESTED,
                            aggregateType = "Usuario",
                            aggregateId = user.id,
                            payload =
                                match { payload ->
                                    payload["email"] == "ana@ufpr.br" &&
                                        payload["token"] == "reset.jwt.token" &&
                                        payload["nome"] == "Ana Silva"
                                },
                        )
                    }
                    verify(exactly = 1) { outboxPublisher.enqueue(any(), any(), any(), any()) }
                }
            }

            When("email is unknown") {
                Then("does not enqueue outbox nor issue a token") {
                    every { usuarioRepository.findByEmail(any()) } returns null

                    useCase.execute(command)

                    verify(exactly = 0) { tokenService.issueOneTimeToken(any(), any(), any()) }
                    verify(exactly = 0) { outboxPublisher.enqueue(any(), any(), any(), any()) }
                }
            }

            When("user exists but is inactive") {
                Then("does not enqueue outbox") {
                    val inactive = buildActiveUser(ativo = false)
                    every { usuarioRepository.findByEmail("ana@ufpr.br") } returns inactive

                    useCase.execute(command)

                    verify(exactly = 0) { outboxPublisher.enqueue(any(), any(), any(), any()) }
                }
            }
        }
    })
