package br.ufpr.sept.so2.modules.iam.application

data class CsvUsuarioRow(
    val line: Int,
    val nome: String,
    val email: String,
    val grr: String?,
    val roleCode: String,
)

data class CsvParseResult(
    val rows: List<CsvUsuarioRow>,
    val errors: List<Map<String, Any?>>,
)

object CsvUsuarioParser {
    fun parse(content: String, defaultRole: String = "ALUNO"): CsvParseResult {
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) {
            return CsvParseResult(emptyList(), listOf(mapOf("linha" to 0, "erro" to "Arquivo vazio")))
        }
        val header = lines.first().lowercase().split(',').map { it.trim() }
        val idxNome = header.indexOf("nome")
        val idxEmail = header.indexOf("email")
        val idxGrr = header.indexOf("grr")
        val idxRole = header.indexOfFirst { it == "role" || it == "rolecode" }
        if (idxNome < 0 || idxEmail < 0) {
            return CsvParseResult(
                emptyList(),
                listOf(mapOf("linha" to 1, "erro" to "Cabeçalho obrigatório: nome,email[,grr,role]")),
            )
        }
        val rows = mutableListOf<CsvUsuarioRow>()
        val errors = mutableListOf<Map<String, Any?>>()
        lines.drop(1).forEachIndexed { i, line ->
            val cols = line.split(',').map { it.trim().trim('"') }
            val lineNo = i + 2
            val nome = cols.getOrNull(idxNome).orEmpty()
            val email = cols.getOrNull(idxEmail).orEmpty().lowercase()
            val grr = cols.getOrNull(idxGrr)?.takeIf { it.isNotBlank() }
            val role = cols.getOrNull(idxRole)?.takeIf { it.isNotBlank() } ?: defaultRole
            when {
                nome.isBlank() -> errors += mapOf("linha" to lineNo, "erro" to "nome vazio")
                !email.contains("@") -> errors += mapOf("linha" to lineNo, "erro" to "email inválido")
                else ->
                    rows +=
                        CsvUsuarioRow(
                            line = lineNo,
                            nome = nome,
                            email = email,
                            grr = grr,
                            roleCode = role.uppercase(),
                        )
            }
        }
        return CsvParseResult(rows, errors)
    }
}
