package br.ufpr.sept.so2.modules.iam.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val loginBuckets = ConcurrentHashMap<String, Bucket>()
    private val forgotPasswordBuckets = ConcurrentHashMap<String, Bucket>()
    private val publicGetBuckets = ConcurrentHashMap<String, Bucket>()
    private val attendanceBuckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val isLogin = request.requestURI.endsWith("/auth/login") && request.method == "POST"
        val isForgotPassword = request.requestURI.endsWith("/auth/forgot-password") && request.method == "POST"
        val isPublicGet =
            request.method == "GET" &&
                (
                    request.requestURI.contains("/publico/solicitacoes/") ||
                        request.requestURI.contains("/publico/verificar-certificado/")
                )
        val isContact =
            request.method == "POST" && request.requestURI.contains("/publico/contato")
        val isAttendanceConfirm =
            request.method == "POST" &&
                (
                    request.requestURI.contains("/attendance/entry") ||
                        request.requestURI.contains("/attendance/exit") ||
                        request.requestURI.contains("/attendance/qr/validate")
                )

        if (isPublicGet) {
            val key = request.remoteAddr
            val probe = publicGetBuckets.computeIfAbsent(key) { buildPublicGetBucket() }.tryConsumeAndReturnRemaining(1)
            if (!probe.isConsumed) {
                val retryAfter = (probe.nanosToWaitForRefill / 1_000_000_000L).coerceAtLeast(1L)
                log.warn("Rate limit de consulta pública atingido para IP: {}", key)
                rejectWithRateLimit(response, retryAfter)
                return
            }
            chain.doFilter(request, response)
            return
        }

        if (isContact) {
            val key = request.remoteAddr
            val probe = publicGetBuckets.computeIfAbsent(key) { buildPublicGetBucket() }.tryConsumeAndReturnRemaining(1)
            if (!probe.isConsumed) {
                val retryAfter = (probe.nanosToWaitForRefill / 1_000_000_000L).coerceAtLeast(1L)
                log.warn("Rate limit de contato público atingido para IP: {}", key)
                rejectWithRateLimit(response, retryAfter)
                return
            }
            chain.doFilter(request, response)
            return
        }

        if (isAttendanceConfirm) {
            val key = request.remoteAddr
            val probe = attendanceBuckets.computeIfAbsent(key) { buildAttendanceBucket() }.tryConsumeAndReturnRemaining(1)
            if (!probe.isConsumed) {
                val retryAfter = (probe.nanosToWaitForRefill / 1_000_000_000L).coerceAtLeast(1L)
                log.warn("Rate limit de confirmação de presença atingido para IP: {}", key)
                rejectWithRateLimit(response, retryAfter)
                return
            }
            chain.doFilter(request, response)
            return
        }

        if (!isLogin && !isForgotPassword) {
            chain.doFilter(request, response)
            return
        }

        // Wrap the request so the body can be read here AND again downstream by the controller.
        // Without this, reading the InputStream in the filter exhausts it and the controller
        // receives an empty body.
        val cached = CachedBodyHttpServletRequest(request)

        if (isLogin) {
            val identifier = extractField(cached, "identificador")
            val key = "${cached.remoteAddr}:$identifier"
            val probe = loginBuckets.computeIfAbsent(key) { buildLoginBucket() }.tryConsumeAndReturnRemaining(1)
            if (!probe.isConsumed) {
                val retryAfter = (probe.nanosToWaitForRefill / 1_000_000_000L).coerceAtLeast(1L)
                log.warn("Rate limit de login atingido para chave: {}", key.replace(":", "_"))
                rejectWithRateLimit(response, retryAfter)
                return
            }
        }

        if (isForgotPassword) {
            val email = extractField(cached, "email")
            val key = "${cached.remoteAddr}:$email"
            val probe = forgotPasswordBuckets.computeIfAbsent(key) { buildForgotPasswordBucket() }.tryConsumeAndReturnRemaining(1)
            if (!probe.isConsumed) {
                val retryAfter = (probe.nanosToWaitForRefill / 1_000_000_000L).coerceAtLeast(1L)
                log.warn("Rate limit de forgot-password atingido para chave: {}", key.replace(":", "_"))
                rejectWithRateLimit(response, retryAfter)
                return
            }
        }

        chain.doFilter(cached, response)
    }

    private fun rejectWithRateLimit(
        response: HttpServletResponse,
        retryAfterSeconds: Long,
    ) {
        response.status = 429
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.setHeader("Retry-After", retryAfterSeconds.toString())
        response.writer.write(
            objectMapper.writeValueAsString(
                mapOf(
                    "type" to "https://secretariaonline.ufpr.br/errors/rate-limit",
                    "title" to "Muitas tentativas",
                    "status" to 429,
                    "detail" to "Muitas tentativas. Aguarde antes de tentar novamente.",
                    "retryAfterSeconds" to retryAfterSeconds,
                ),
            ),
        )
    }

    private fun buildLoginBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
            .build()

    // 3 solicitações por hora por email+IP — evita spam de e-mails de reset
    private fun buildForgotPasswordBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1))))
            .build()

    // 10 consultas/min por IP nas rotas públicas de protocolo e certificado
    private fun buildPublicGetBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
            .build()

    private fun buildAttendanceBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))))
            .build()

    private fun extractField(
        request: CachedBodyHttpServletRequest,
        field: String,
    ): String =
        try {
            val json = objectMapper.readValue(request.cachedBody, Map::class.java)
            (json[field] as? String)?.trim()?.lowercase()?.take(100) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

    /**
     * Wraps a servlet request and eagerly caches the body bytes so that
     * [getInputStream] and [getReader] can be called multiple times — once
     * here in the filter and again by Spring's [org.springframework.web.bind.annotation.RequestBody] deserializer.
     */
    private inner class CachedBodyHttpServletRequest(
        delegate: HttpServletRequest,
    ) : HttpServletRequestWrapper(delegate) {
        val cachedBody: ByteArray = delegate.inputStream.readBytes()

        override fun getInputStream(): ServletInputStream =
            object : ServletInputStream() {
                private val stream = ByteArrayInputStream(cachedBody)

                override fun read(): Int = stream.read()

                override fun isFinished(): Boolean = stream.available() == 0

                override fun isReady(): Boolean = true

                override fun setReadListener(listener: ReadListener?) {}
            }

        override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
    }
}
