package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ImportJobJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ConfirmImportResult(
    val jobId: UUID,
    val status: String,
    val successCount: Int,
    val errorCount: Int,
)

@Service
@Transactional
class ConfirmImportUseCase(
    private val importJobRepo: ImportJobJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
    private val roleRepo: RoleJpaRepository,
    private val argon2: Argon2PasswordService,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun execute(jobId: UUID): ConfirmImportResult {
        val job = importJobRepo.findById(jobId).orElseThrow { NoSuchElementException("Job não encontrado: $jobId") }
        require(job.status == "VALIDATED") { "Job não está VALIDATED (status=${job.status})." }

        var success = 0
        val errors = job.errors.toMutableList()

        job.rowsPayload.forEach { row ->
            val email = row["email"]?.toString().orEmpty()
            val nome = row["nome"]?.toString().orEmpty()
            val grr = row["grr"]?.toString()
            val roleCode = row["roleCode"]?.toString() ?: "ALUNO"
            try {
                if (usuarioRepo.existsByEmail(email)) {
                    errors += mapOf("email" to email, "erro" to "email já cadastrado")
                    return@forEach
                }
                val role = roleRepo.findByCode(roleCode).orElseThrow { NoSuchElementException("Role $roleCode") }
                val senhaHash = argon2.hash(UUID.randomUUID().toString().take(12))
                val usuario =
                    UsuarioEntity(
                        nome = nome,
                        email = email,
                        grr = grr,
                        senhaHash = senhaHash,
                        senhaAlterada = false,
                    )
                val saved = usuarioRepo.save(usuario)
                saved.usuarioRoles.add(UsuarioRoleEntity(usuario = saved, role = role))
                usuarioRepo.save(saved)
                success++
            } catch (e: Exception) {
                errors += mapOf("email" to email, "erro" to (e.message ?: "falha"))
            }
        }

        job.successCount = success
        job.errorCount = errors.size
        job.errors = errors
        job.status = if (errors.isEmpty()) "COMPLETED" else "PARTIAL"
        importJobRepo.save(job)

        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.IMPORTS_COMPLETED,
            aggregateType = "ImportJob",
            aggregateId = job.id,
            payload =
                mapOf(
                    "status" to job.status,
                    "successCount" to success,
                    "errorCount" to errors.size,
                    "atorId" to job.idAtor.toString(),
                ),
        )

        return ConfirmImportResult(jobId = job.id, status = job.status, successCount = success, errorCount = errors.size)
    }
}
