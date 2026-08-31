package br.ufpr.sept.so2.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.security.KeyPairGenerator
import java.util.Base64

/**
 * Local `dev` only: if JWT PEMs are still the placeholder, mint an ephemeral RSA pair
 * so the walking skeleton can boot without committing keys.
 */
class DevJwtKeyEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val configuredProfiles =
            environment.getProperty("spring.profiles.active").orEmpty().split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        val isDev = environment.activeProfiles.contains("dev") || configuredProfiles.contains("dev")
        if (!isDev) return

        val current = environment.getProperty("security.jwt.private-key").orEmpty()
        if (current.isNotBlank() && current != PLACEHOLDER) return

        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val pair = generator.generateKeyPair()
        environment.propertySources.addFirst(
            MapPropertySource(
                "dev-ephemeral-jwt",
                mapOf(
                    "security.jwt.private-key" to pem("PRIVATE KEY", pair.private.encoded),
                    "security.jwt.public-key" to pem("PUBLIC KEY", pair.public.encoded),
                ),
            ),
        )
    }

    private fun pem(
        type: String,
        der: ByteArray,
    ): String {
        val body = Base64.getEncoder().encodeToString(der)
        return "-----BEGIN $type-----\n$body\n-----END $type-----"
    }

    companion object {
        private const val PLACEHOLDER = "PLACEHOLDER_REPLACE_ME"
    }
}
