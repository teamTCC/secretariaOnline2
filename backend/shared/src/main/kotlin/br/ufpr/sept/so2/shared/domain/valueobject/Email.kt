package br.ufpr.sept.so2.shared.domain.valueobject

@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(value.matches(EMAIL_REGEX)) { "Formato de email inválido: $value" }
    }

    fun isInstitutional(): Boolean = value.endsWith("@ufpr.br")

    override fun toString(): String = value

    companion object {
        private val EMAIL_REGEX = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}\$".toRegex()

        fun of(raw: String): Email = Email(raw.trim().lowercase())
    }
}
