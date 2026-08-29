package br.ufpr.sept.so2.modules.formativas.application.ports.out

data class FormativaCategoriaCount(
    val categoria: String,
    val total: Long,
)

interface FormativaBffReadPort {
    fun countByEstado(estado: String): Long

    fun countAprovadasByCategoria(): List<FormativaCategoriaCount>
}
