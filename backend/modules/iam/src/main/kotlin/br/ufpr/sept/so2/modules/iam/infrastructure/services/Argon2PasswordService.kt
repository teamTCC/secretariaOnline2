package br.ufpr.sept.so2.modules.iam.infrastructure.services

import br.ufpr.sept.so2.modules.iam.application.ports.out.PasswordHasherPort
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service

@Service
class Argon2PasswordService : PasswordHasherPort {
    // OWASP recommended: memory=47104 (46MB), iterations=1, parallelism=1
    private val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()

    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    override fun verify(
        rawPassword: String,
        hash: String,
    ): Boolean = encoder.matches(rawPassword, hash)
}
