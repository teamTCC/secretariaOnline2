package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateJpaRepository
import org.springframework.stereotype.Service

data class RenderedTemplate(
    val assunto: String,
    val corpo: String,
    val fromCatalog: Boolean,
)

@Service
class TemplateEngine(
    private val templateRepo: CommunicationTemplateJpaRepository,
) {
    fun render(
        codigo: String,
        vars: Map<String, String>,
        fallbackAssunto: String,
        fallbackCorpo: String,
    ): RenderedTemplate {
        val tpl = templateRepo.findByCodigo(codigo).orElse(null)
        if (tpl == null || !tpl.ativo) {
            return RenderedTemplate(
                assunto = interpolate(fallbackAssunto, vars),
                corpo = interpolate(fallbackCorpo, vars),
                fromCatalog = false,
            )
        }
        return RenderedTemplate(
            assunto = interpolate(tpl.assunto, vars),
            corpo = interpolate(tpl.corpo, vars),
            fromCatalog = true,
        )
    }

    fun interpolate(
        text: String,
        vars: Map<String, String>,
    ): String {
        var out = text
        vars.forEach { (k, v) ->
            out = out.replace("{{$k}}", v)
        }
        return out.replace(Regex("\\{\\{[a-zA-Z0-9_]+}}"), "")
    }
}
