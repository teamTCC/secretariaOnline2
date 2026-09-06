package br.ufpr.sept.so2.shared.domain.valueobject

@JvmInline
value class Cpf(
    val value: String,
) {
    init {
        val digits = value.filter { it.isDigit() }
        require(digits.length == 11 && isValid(digits)) { "CPF inválido: $value" }
    }

    fun masked(): String {
        val d = value.filter { it.isDigit() }
        return "***.${ d.substring(3, 6)}.${d.substring(6, 9)}-**"
    }

    override fun toString(): String = value

    companion object {
        fun of(raw: String): Cpf = Cpf(raw.filter { it.isDigit() })

        private fun isValid(digits: String): Boolean {
            if (digits.all { it == digits[0] }) return false
            val d1 = (0..8).sumOf { (digits[it] - '0') * (10 - it) }.let { 11 - (it % 11) }.let { if (it >= 10) 0 else it }
            val d2 = (0..9).sumOf { (digits[it] - '0') * (11 - it) }.let { 11 - (it % 11) }.let { if (it >= 10) 0 else it }
            return d1 == (digits[9] - '0') && d2 == (digits[10] - '0')
        }
    }
}
