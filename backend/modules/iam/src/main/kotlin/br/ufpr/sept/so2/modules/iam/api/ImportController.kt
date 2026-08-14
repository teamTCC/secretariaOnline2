package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.application.CsvUsuarioParser
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ImportJobEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ImportJobJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/imports")
@Tag(name = "Secretaria — Importações", description = "Importação CSV de alunos em duas fases (validar + confirmar)")
@PreAuthorize("hasAuthority('import.run') or hasAuthority('system.admin')")
class ImportController(
    private val importJobRepo: ImportJobJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
    private val roleRepo: RoleJpaRepository,
    private val argon2: Argon2PasswordService,
    private val outboxPublisher: OutboxEventPublisher,
) {
    @GetMapping("/templates/{kind}")
    @Operation(summary = "Baixar modelo CSV")
    fun template(
        @PathVariable kind: String,
    ): ResponseEntity<String> {
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
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv)
    }

    @PostMapping("/{kind}")
    @Operation(summary = "Upload CSV — valida e grava job VALIDATED (ainda não persiste usuários)")
    fun upload(
        @PathVariable kind: String,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<Map<String, Any?>> {
        val normalized = kind.lowercase()
        require(normalized in setOf("alunos", "professores")) { "kind suportado: alunos, professores" }
        require(!file.isEmpty) { "Arquivo vazio." }
        require(file.size <= 20 * 1024 * 1024) { "Arquivo excede 20 MB." }
        val defaultRole = if (normalized == "professores") "PROFESSOR" else "ALUNO"
        val parsed = CsvUsuarioParser.parse(file.bytes.toString(Charsets.UTF_8), defaultRole)
        val job =
            importJobRepo.save(
                ImportJobEntity(
                    kind = normalized,
                    filename = file.originalFilename ?: "upload.csv",
                    status = if (parsed.errors.isEmpty() && parsed.rows.isNotEmpty()) "VALIDATED" else "INVALID",
                    totalRows = parsed.rows.size,
                    errorCount = parsed.errors.size,
                    rowsPayload =
                        parsed.rows.map {
                            mapOf("nome" to it.nome, "email" to it.email, "grr" to it.grr, "roleCode" to it.roleCode)
                        },
                    errors = parsed.errors,
                    idAtor = currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf(
                "jobId" to job.id,
                "status" to job.status,
                "totalRows" to job.totalRows,
                "errorCount" to job.errorCount,
                "errors" to job.errors,
            ),
        )
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Status do job de importação")
    fun get(
        @PathVariable jobId: UUID,
    ): Map<String, Any?> {
        val job = importJobRepo.findById(jobId).orElseThrow { NoSuchElementException("Job não encontrado: $jobId") }
        return mapOf(
            "jobId" to job.id,
            "status" to job.status,
            "totalRows" to job.totalRows,
            "successCount" to job.successCount,
            "errorCount" to job.errorCount,
            "errors" to job.errors,
        )
    }

    @PostMapping("/{jobId}/confirm")
    @Operation(summary = "Confirmar importação válida — cria usuários")
    @Transactional
    fun confirm(
        @PathVariable jobId: UUID,
    ): ResponseEntity<Map<String, Any?>> {
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
                val role =
                    roleRepo.findByCode(roleCode).orElseThrow { NoSuchElementException("Role $roleCode") }
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
        return ResponseEntity.ok(
            mapOf("jobId" to job.id, "status" to job.status, "successCount" to success, "errorCount" to errors.size),
        )
    }
}
