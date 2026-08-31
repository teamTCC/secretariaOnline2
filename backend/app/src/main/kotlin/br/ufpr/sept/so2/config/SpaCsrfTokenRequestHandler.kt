package br.ufpr.sept.so2.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CsrfTokenRequestHandler
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.util.StringUtils
import java.util.function.Supplier

/**
 * Double Submit Cookie para SPA: cookie `XSRF-TOKEN` (não httpOnly) + header `X-XSRF-TOKEN`.
 * Usa token plano (sem XOR) para o JSON de `GET /auth/csrf` coincidir com o cookie —
 * o SPA e o HTTPie ecoam esse valor no header. XOR só entra se o token vier como parâmetro de form.
 */
class SpaCsrfTokenRequestHandler : CsrfTokenRequestHandler {
    private val plain = CsrfTokenRequestAttributeHandler()
    private val xor = XorCsrfTokenRequestAttributeHandler()

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        csrfToken: Supplier<CsrfToken>,
    ) {
        plain.handle(request, response, csrfToken)
        csrfToken.get()
    }

    override fun resolveCsrfTokenValue(
        request: HttpServletRequest,
        csrfToken: CsrfToken,
    ): String? {
        val headerValue = request.getHeader(csrfToken.headerName)
        return if (StringUtils.hasText(headerValue)) {
            plain.resolveCsrfTokenValue(request, csrfToken)
        } else {
            xor.resolveCsrfTokenValue(request, csrfToken)
        }
    }
}
