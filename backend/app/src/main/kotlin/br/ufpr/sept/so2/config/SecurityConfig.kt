package br.ufpr.sept.so2.config

import br.ufpr.sept.so2.modules.iam.security.JwtAuthenticationFilter
import br.ufpr.sept.so2.modules.iam.security.RateLimitFilter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.OffsetDateTime

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
    private val objectMapper: ObjectMapper,
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { csrf ->
                val cookieRepo = CookieCsrfTokenRepository.withHttpOnlyFalse()
                cookieRepo.cookiePath = "/"
                cookieRepo.setCookieMaxAge(12 * 60 * 60)
                csrf
                    .csrfTokenRepository(SkipBlankCsrfCookieRepository(cookieRepo))
                    .csrfTokenRequestHandler(SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers(
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/ott",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/**",
                        "/.well-known/**",
                    )
            }.cors { it.configurationSource(corsConfigSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/ott",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/auth/csrf",
                        "/publico/**",
                        "/faq",
                        "/faq/**",
                        "/.well-known/**",
                        "/actuator/health",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.addFilterAfter(CsrfCookieFilter(), CsrfFilter::class.java)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.characterEncoding = Charsets.UTF_8.name()
                    response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "type" to "https://secretariaonline.ufpr.br/errors/unauthorized",
                                "title" to "Não autenticado",
                                "status" to 401,
                                "detail" to "Token JWT inválido ou expirado.",
                                "timestamp" to OffsetDateTime.now().toString(),
                            ),
                        ),
                    )
                }
                ex.accessDeniedHandler { _, response, _ ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.characterEncoding = Charsets.UTF_8.name()
                    response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "type" to "https://secretariaonline.ufpr.br/errors/forbidden",
                                "title" to "Acesso negado",
                                "status" to 403,
                                "detail" to "Você não tem permissão para esta operação.",
                                "timestamp" to OffsetDateTime.now().toString(),
                            ),
                        ),
                    )
                }
            }.headers { headers ->
                headers
                    .frameOptions { it.deny() }
                    .contentTypeOptions { }
                    .httpStrictTransportSecurity { hsts ->
                        hsts.maxAgeInSeconds(31536000).includeSubDomains(true)
                    }
            }.build()

    @Bean
    fun corsConfigSource(): CorsConfigurationSource {
        val config = CorsConfiguration()

        // Exact origins (no wildcards) — primary list
        val filteredOrigins = corsProperties.allowedOrigins.filter { it.isNotBlank() }
        if (filteredOrigins.isNotEmpty()) {
            config.allowedOrigins = filteredOrigins
        }

        // Pattern-based origins — supports wildcards, needed for Vercel preview URLs
        // e.g. "https://*.vercel.app" or "https://secretaria-online-*.vercel.app"
        val patterns = corsProperties.allowedOriginPatterns.filter { it.isNotBlank() }
        if (patterns.isNotEmpty()) {
            config.allowedOriginPatterns = patterns
        }

        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders =
            listOf(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-XSRF-TOKEN",
            )
        // Expose Set-Cookie so the browser SPA can observe cookie changes if needed
        config.exposedHeaders = listOf("Set-Cookie")
        config.allowCredentials = true
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
