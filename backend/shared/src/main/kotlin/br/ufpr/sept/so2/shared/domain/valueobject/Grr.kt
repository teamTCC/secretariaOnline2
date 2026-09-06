package br.ufpr.sept.so2.shared.domain.valueobject

@JvmInline
value class Grr(
    val value: String,
) {
    init {
        require(value.matches(GRR_REGEX)) { "GRR inválido: $value (esperado GRR + 8 dígitos)" }
    }

    override fun toString(): String = value

    companion object {
        private val GRR_REGEX = "^GRR\\d{8}\$".toRegex()

        fun of(raw: String): Grr = Grr(raw.trim().uppercase())
    }
}
