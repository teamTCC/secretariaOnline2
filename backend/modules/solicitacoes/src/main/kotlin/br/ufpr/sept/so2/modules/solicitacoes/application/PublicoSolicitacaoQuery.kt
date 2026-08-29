package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.api.PublicoSolicitacaoResponse
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import org.springframework.stereotype.Component

@Component
class PublicoSolicitacaoQuery(
    private val requestRepo: RequestJpaRepository,
) {
    fun verificarProtocolo(
        ano: Short,
        numero: Int,
    ): PublicoSolicitacaoResponse {
        val request =
            requestRepo
                .findByNumeroAnualAndAno(numero, ano)
                .orElseThrow { NoSuchElementException("Protocolo não encontrado: $ano/$numero") }

        return PublicoSolicitacaoResponse(
            protocolo = "$ano/${numero.toString().padStart(4, '0')}",
            tipo = request.requestTypeCode,
            estado = request.estado,
            abertaEm = request.createdAt,
            prazoEm = request.prazoEm,
            links = mapOf("self" to "/publico/solicitacoes/$ano/$numero"),
        )
    }
}
