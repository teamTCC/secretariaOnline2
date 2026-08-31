package br.ufpr.sept.so2.config

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Local-only walking-skeleton aluno. Password comes from [DEMO_ALUNO_SENHA];
 * never hashed in Flyway. Existing rows are left untouched.
 */
@Configuration
@Profile("dev")
class DemoAlunoSeed(
    private val usuarioRepo: UsuarioJpaRepository,
    private val roleRepo: RoleJpaRepository,
    private val passwordService: Argon2PasswordService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun seedDemoAluno(
        @Value("\${DEMO_ALUNO_EMAIL:ana.silva@ufpr.br}") email: String,
        @Value("\${DEMO_ALUNO_SENHA:}") senha: String,
        @Value("\${DEMO_ALUNO_GRR:GRR20219999}") grr: String,
    ): ApplicationRunner =
        ApplicationRunner {
            val normalizedEmail = email.trim().lowercase()
            val role = roleRepo.findByCode("ALUNO").orElse(null)
            val existing = usuarioRepo.findByEmailWithRoles(normalizedEmail).orElse(null)
            when {
                senha.isBlank() ->
                    log.info("DEMO_ALUNO_SENHA vazio — seed do aluno de demonstração ignorado")
                existing != null -> {
                    val targetGrr = normalizedGrr(grr)
                    if (existing.grr != targetGrr) {
                        existing.grr = targetGrr
                        usuarioRepo.save(existing)
                        log.info("GRR do aluno de demonstração corrigido: {}", targetGrr)
                    } else {
                        log.info("Aluno de demonstração já existe: {}", normalizedEmail)
                    }
                }
                role == null ->
                    log.warn("Role ALUNO ausente — seed do aluno de demonstração ignorado")
                else -> {
                    val targetGrr = normalizedGrr(grr)
                    val usableGrr = targetGrr.takeUnless { usuarioRepo.existsByGrr(it) }
                    val usuario =
                        usuarioRepo.save(
                            UsuarioEntity(
                                nome = "Ana Silva",
                                email = normalizedEmail,
                                grr = usableGrr,
                                senhaHash = passwordService.hash(senha),
                                senhaAlterada = false,
                            ),
                        )
                    usuario.usuarioRoles.add(UsuarioRoleEntity(usuario = usuario, role = role))
                    usuarioRepo.save(usuario)
                    log.info("Aluno de demonstração criado: {} (primeiro acesso pendente)", normalizedEmail)
                }
            }
        }

    private fun normalizedGrr(raw: String): String {
        val candidate = raw.trim().uppercase()
        return if (candidate.matches(Regex("^GRR\\d{8}$"))) candidate else "GRR20219999"
    }
}
