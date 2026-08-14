package br.ufpr.sept.so2.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

/** Garante que o cookie XSRF-TOKEN seja emitido em GETs (SPA lê o cookie e ecoa no header). */
class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val csrf = request.getAttribute("_csrf") as? CsrfToken
            ?: request.getAttribute(CsrfToken::class.java.name) as? CsrfToken
        csrf?.token
        filterChain.doFilter(request, response)
    }
}
