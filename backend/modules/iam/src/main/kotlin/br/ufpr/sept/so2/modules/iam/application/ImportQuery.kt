package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.ImportJobDetailResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ImportJobEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ImportJobJpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ImportTemplateResult(
    val filename: String,
    val csv: String,
)

@Component
class ImportQuery(
    private val importJobRepo: ImportJobJpaRepository,
) {
    fun template(kind: String): ImportTemplateResult {
        require(kind.equals("alunos", ignoreCase = true) || kind.equals("professores", ignoreCase = true)) {
            "kind suportado: alunos, professores"
        }
        val filename = if (kind.equals("professores", ignoreCase = true)) "modelo-professores.csv" else "modelo-alunos.csv"
        val csv =
            if (kind.equals("professores", ignoreCase = true)) {
                "nome,email,role\nJoão Professor,joao.prof@ufpr.br,PROFESSOR\n"
            } else {
                "nome,email,grr,role\nMaria Silva,maria@ufpr.br,GRR20220001,ALUNO\n"
            }
        return ImportTemplateResult(filename = filename, csv = csv)
    }

    fun get(jobId: UUID): ImportJobDetailResponse {
        val job = importJobRepo.findById(jobId).orElseThrow { NoSuchElementException("Job não encontrado: $jobId") }
        return ImportJobDetailResponse(
            jobId = job.id,
            status = job.status,
            totalRows = job.totalRows,
            successCount = job.successCount,
            errorCount = job.errorCount,
            errors = job.errors,
        )
    }
}

@Service
@Transactional
class UploadImportUseCase(
    private val importJobRepo: ImportJobJpaRepository,
) {
    fun execute(
        kind: String,
        filename: String,
        bytes: ByteArray,
        empty: Boolean,
        size: Long,
        actorId: UUID,
    ): ImportJobDetailResponse {
        val normalized = kind.lowercase()
        require(normalized in setOf("alunos", "professores")) { "kind suportado: alunos, professores" }
        require(!empty) { "Arquivo vazio." }
        require(size <= 20 * 1024 * 1024) { "Arquivo excede 20 MB." }
        val defaultRole = if (normalized == "professores") "PROFESSOR" else "ALUNO"
        val parsed = CsvUsuarioParser.parse(bytes.toString(Charsets.UTF_8), defaultRole)
        val job =
            importJobRepo.save(
                ImportJobEntity(
                    kind = normalized,
                    filename = filename,
                    status = if (parsed.errors.isEmpty() && parsed.rows.isNotEmpty()) "VALIDATED" else "INVALID",
                    totalRows = parsed.rows.size,
                    errorCount = parsed.errors.size,
                    rowsPayload =
                        parsed.rows.map {
                            mapOf("nome" to it.nome, "email" to it.email, "grr" to it.grr, "roleCode" to it.roleCode)
                        },
                    errors = parsed.errors,
                    idAtor = actorId,
                ),
            )
        return ImportJobDetailResponse(
            jobId = job.id,
            status = job.status,
            totalRows = job.totalRows,
            errorCount = job.errorCount,
            successCount = null,
            errors = job.errors,
        )
    }
}
