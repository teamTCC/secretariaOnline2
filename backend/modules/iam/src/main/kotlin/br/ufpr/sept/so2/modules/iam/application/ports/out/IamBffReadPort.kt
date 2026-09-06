package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.util.UUID

data class IamSearchHit(
    val id: UUID,
    val title: String,
    val subtitle: String,
)

data class UsuarioExportRow(
    val id: UUID,
    val nome: String,
    val email: String,
    val grr: String?,
)

data class ColacaoAnoCount(
    val ano: Int,
    val colacoes: Long,
)

/**
 * Read-only IAM slice for BFF search/reports/export. No JPA types.
 */
interface IamBffReadPort {
    fun countByRoleCode(roleCode: String): Long

    fun findNome(id: UUID): String?

    fun search(
        q: String,
        page: Int,
        size: Int,
    ): List<IamSearchHit>

    fun countColacoesByAno(): List<ColacaoAnoCount>

    fun listAlunosExport(limit: Int): List<UsuarioExportRow>

    fun listByRoleExport(roleCode: String, limit: Int): List<UsuarioExportRow>
}
