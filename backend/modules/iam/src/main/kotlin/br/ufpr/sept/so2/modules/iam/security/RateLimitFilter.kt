package br.ufpr.sept.so2.modules.iam.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val loginBuckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        if (request.requestURI.endsWith("/auth/login") && request.method == "POST") {
            val identifier = extractIdentifier(request)
            val key = "${request.remoteAddr}:$identifier"
            val bucket = loginBuckets.computeIfAbsent(key) { buildLoginBucket() }

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit atingido para chave: {}", key.replace(":", "_"))
                response.status = HttpServletResponse.SC_OK.let { 429 }
                response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
                response.writer.write(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "title" to "Muitas tentativas",
                            "status" to 429,
                            "detail" to "Muitas tentativas. Aguarde antes de tentar novamente.",
                            "type" to "https://secretariaonline.ufpr.br/errors/rate-limit",
                        ),
                    ),
                )
                return
            }
        }

        chain.doFilter(request, response)
    }

    private fun buildLoginBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
            .build()

    private fun extractIdentifier(request: HttpServletRequest): String =
        try {
            val body = request.reader.readText()
            val json = objectMapper.readValue(body, Map::class.java)
            (json["identificador"] as? String)?.take(50) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
}
