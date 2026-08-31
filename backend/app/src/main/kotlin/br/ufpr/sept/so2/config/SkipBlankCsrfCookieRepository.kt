package br.ufpr.sept.so2.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository

/**
 * O [CookieCsrfTokenRepository] do Spring, com token CSRF adiado, grava
 * `Set-Cookie: XSRF-TOKEN=; Max-Age=0` e em seguida o cookie real.
 * Clientes como o HTTPie aplicam o primeiro header e descartam o segundo —
 * o cookie some da session e o Double Submit falha.
 */
class SkipBlankCsrfCookieRepository(
    private val delegate: CookieCsrfTokenRepository,
) : CsrfTokenRepository {
    override fun generateToken(request: HttpServletRequest): CsrfToken = delegate.generateToken(request)

    override fun saveToken(
        token: CsrfToken?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (token == null || token.token.isNullOrBlank()) return
        delegate.saveToken(token, request, response)
    }

    override fun loadToken(request: HttpServletRequest): CsrfToken? = delegate.loadToken(request)
}
