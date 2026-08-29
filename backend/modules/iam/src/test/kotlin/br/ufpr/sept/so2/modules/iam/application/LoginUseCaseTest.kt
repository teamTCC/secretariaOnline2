package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.PasswordHasherPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.RefreshTokenRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenRevocationPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenServicePort
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.domain.Role
import br.ufpr.sept.so2.modules.iam.domain.Usuario
import br.ufpr.sept.so2.modules.iam.domain.UsuarioRole
import br.ufpr.sept.so2.modules.iam.domain.exceptions.AccountBlockedException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.InvalidCredentialsException
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import br.ufpr.sept.so2.shared.domain.valueobject.Email
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.OffsetDateTime
import java.util.UUID

class LoginUseCaseTest :
    BehaviorSpec({

        val usuarioRepository = mockk<UsuarioRepository>()
        val refreshTokenRepository = mockk<RefreshTokenRepository>()
        val tokenService = mockk<TokenServicePort>()
        val passwordService = mockk<PasswordHasherPort>()
        val tokenRevocationPort = mockk<TokenRevocationPort>()
        val auditPublisher = mockk<AuditPublisher>(relaxed = true)

        val useCase =
            LoginUseCase(
                usuarioRepository,
                refreshTokenRepository,
                tokenService,
                passwordService,
                tokenRevocationPort,
                auditPublisher,
            )

        fun buildActiveUser(
            tentativasFalhas: Int = 0,
            senhaAlterada: Boolean = true,
            bloqueadoAte: OffsetDateTime? = null,
            metadata: Map<String, Any> = mapOf("aceite_lgpd_em" to "2026-01-01"),
        ) = Usuario(
            id = UUID.randomUUID(),
            nome = "Test User",
            email = Email.of("test@ufpr.br"),
            grr = null,
            senhaHash = "\$argon2id\$hashed_password",
            senhaAlterada = senhaAlterada,
            ativo = true,
            bloqueadoAte = bloqueadoAte,
            tentativasFalhas = tentativasFalhas,
            metadata = metadata,
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

        val command =
            LoginCommand(
                identificador = "test@ufpr.br",
                senha = "SenhaCorreta@123",
                ip = "127.0.0.1",
                userAgent = "TestAgent/1.0",
            )

        fun stubSuccessfulLogin(user: Usuario) {
            every { usuarioRepository.findByIdentificador("test@ufpr.br") } returns user
            every { passwordService.verify(command.senha, user.senhaHash) } returns true
            every { tokenService.issueAccessToken(user, any()) } returns "access_token_mock"
            every { tokenService.accessTtlSeconds } returns 900
            justRun { tokenRevocationPort.createSession(any(), user.id, any()) }
            every { refreshTokenRepository.save(any()) } answers { firstArg() }
        }

        Given("LoginUseCase") {

            When("valid credentials are provided") {
                val user = buildActiveUser()
                stubSuccessfulLogin(user)

                val result = useCase.execute(command)

                Then("returns non-null access and refresh tokens") {
                    result.accessToken shouldBe "access_token_mock"
                    result.refreshToken shouldNotBe null
                }

                Then("mustChangePassword is false when already changed") {
                    result.mustChangePassword shouldBe false
                }

                Then("mustAcceptLgpd is false when already accepted") {
                    result.mustAcceptLgpd shouldBe false
                }
            }

            When("user has never changed password") {
                val user = buildActiveUser(senhaAlterada = false)
                stubSuccessfulLogin(user)

                val result = useCase.execute(command)

                Then("mustChangePassword is true") {
                    result.mustChangePassword shouldBe true
                }
            }

            When("user has not accepted LGPD") {
                val user = buildActiveUser(metadata = emptyMap())
                stubSuccessfulLogin(user)

                val result = useCase.execute(command)

                Then("mustAcceptLgpd is true") {
                    result.mustAcceptLgpd shouldBe true
                }
            }

            When("user is not found") {
                every { usuarioRepository.findByIdentificador(any()) } returns null

                Then("throws InvalidCredentialsException (no user enumeration)") {
                    shouldThrow<InvalidCredentialsException> { useCase.execute(command) }
                }
            }

            When("user is inactive") {
                val inactiveUser = buildActiveUser().copy(ativo = false)
                every { usuarioRepository.findByIdentificador(any()) } returns inactiveUser

                Then("throws InvalidCredentialsException (same as not found — no enumeration)") {
                    shouldThrow<InvalidCredentialsException> { useCase.execute(command) }
                }
            }

            When("user is blocked") {
                val blockedUser = buildActiveUser(bloqueadoAte = OffsetDateTime.now().plusMinutes(5))
                every { usuarioRepository.findByIdentificador("test@ufpr.br") } returns blockedUser

                Then("throws AccountBlockedException") {
                    shouldThrow<AccountBlockedException> { useCase.execute(command) }
                }
            }

            When("wrong password is provided") {
                val user = buildActiveUser()
                every { usuarioRepository.findByIdentificador("test@ufpr.br") } returns user
                every { passwordService.verify(command.senha, user.senhaHash) } returns false
                every { usuarioRepository.updateFailedAttempts(user.id, 1, null) } returns Unit

                Then("throws InvalidCredentialsException") {
                    shouldThrow<InvalidCredentialsException> { useCase.execute(command) }
                }

                Then("increments failed attempts counter") {
                    verify { usuarioRepository.updateFailedAttempts(user.id, 1, null) }
                }
            }

            When("failed attempts reach MAX threshold") {
                val user = buildActiveUser(tentativasFalhas = Usuario.MAX_FAILED_ATTEMPTS - 1)
                every { usuarioRepository.findByIdentificador("test@ufpr.br") } returns user
                every { passwordService.verify(command.senha, user.senhaHash) } returns false
                every { usuarioRepository.updateFailedAttempts(user.id, Usuario.MAX_FAILED_ATTEMPTS, any()) } returns Unit

                Then("blocks the account (non-null bloqueadoAte)") {
                    shouldThrow<InvalidCredentialsException> { useCase.execute(command) }
                    verify { usuarioRepository.updateFailedAttempts(user.id, Usuario.MAX_FAILED_ATTEMPTS, any()) }
                }
            }

            When("successful login after previous failures") {
                val user = buildActiveUser(tentativasFalhas = 3)
                stubSuccessfulLogin(user)
                every { usuarioRepository.updateFailedAttempts(user.id, 0, null) } returns Unit

                useCase.execute(command)

                Then("resets failed attempts counter to zero") {
                    verify { usuarioRepository.updateFailedAttempts(user.id, 0, null) }
                }
            }
        }
    })
