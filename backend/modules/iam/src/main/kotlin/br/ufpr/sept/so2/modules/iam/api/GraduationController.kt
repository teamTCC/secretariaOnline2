package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.iam.application.DiplomaPdfService
import br.ufpr.sept.so2.modules.iam.application.GraduationEligibilityService
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ConfirmGraduationDto(
    @field:NotEmpty val alunoIds: List<UUID>,
    val idCurso: UUID? = null,
    val dataColacao: LocalDate? = null,
    val observacao: String? = null,
    val livro: String? = null,
    val folha: String? = null,
    val ata: String? = null,
    val periodoId: UUID? = null,
)

@RestController
@Tag(name = "Secretaria — Egressos e Diplomas", description = "Colação de grau e entrega de diploma")
class GraduationController(
    private val graduationRepo: GraduationRecordJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
    private val roleRepo: RoleJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val eligibilityService: GraduationEligibilityService,
    private val diplomaPdfService: DiplomaPdfService,
    private val minio: MinioStorageService,
) {
    @GetMapping("/secretaria/egressos")
    @PreAuthorize("hasAuthority('alumni.list') or hasAuthority('system.admin')")
    @Operation(summary = "Listar egressos (format=csv para download)")
    fun listEgressos(
        @RequestParam(required = false) format: String?,
        @RequestParam(required = false) situacaoDiploma: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Any {
        val page = usuarioRepo.findAllByRoleCode("EGRESSO", pageable)
        val recs =
            if (page.content.isEmpty()) {
                emptyMap()
            } else {
                graduationRepo.findAllByIdAlunoIn(page.content.map { it.id }).associateBy { it.idAluno }
            }
        val mapped =
            page.content.map { u ->
                val rec = recs[u.id]
                mapOf(
                    "id" to u.id,
                    "nome" to u.nome,
                    "email" to u.email,
                    "grr" to u.grr,
                    "situacaoDiploma" to (rec?.estado ?: "SEM_REGISTRO"),
                    "dataColacao" to rec?.dataColacao,
                    "graduationId" to rec?.id,
                )
            }
        val filtered =
            if (situacaoDiploma != null) {
                mapped.filter { it["situacaoDiploma"] == situacaoDiploma.uppercase() }
            } else {
                mapped
            }
        if (format.equals("csv", ignoreCase = true)) {
            val csv =
                buildString {
                    appendLine("id,nome,email,grr,situacaoDiploma,dataColacao")
                    filtered.forEach { row ->
                        appendLine(
                            "${row["id"]},${row["nome"]},${row["email"]},${row["grr"] ?: ""},${row["situacaoDiploma"]},${row["dataColacao"] ?: ""}",
                        )
                    }
                }
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"egressos.csv\"")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csv)
        }
        return PageResponse(
            content = filtered,
            page =
                PageResponse.PageMeta(
                    number = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                ),
        )
    }

    @GetMapping("/graduations")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('alumni.list') or hasAuthority('system.admin')")
    @Operation(summary = "Listar registros de colação / diplomas")
    fun listGraduations(
        @RequestParam(required = false) estado: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val page =
            if (estado != null) {
                graduationRepo.findAllByEstado(estado.uppercase(), pageable)
            } else {
                graduationRepo.findAll(pageable)
            }
        return PageResponse.of(page) { rec ->
            mapOf(
                "id" to rec.id,
                "idAluno" to rec.idAluno,
                "idCurso" to rec.idCurso,
                "dataColacao" to rec.dataColacao,
                "estado" to rec.estado,
                "deliveredAt" to rec.deliveredAt,
                "livro" to rec.livro,
                "folha" to rec.folha,
                "ata" to rec.ata,
                "diplomaHashSha256" to rec.diplomaHashSha256,
                "_links" to
                    if (rec.estado != "DIPLOMA_ENTREGUE") {
                        mapOf("confirm-delivery" to "/graduations/${rec.id}/confirm-delivery")
                    } else {
                        emptyMap()
                    },
            )
        }
    }

    @GetMapping("/students")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('user.manage_students') or hasAuthority('system.admin')")
    @Operation(summary = "Listar alunos; eligibleForGraduation=true aplica os critérios de colação")
    fun listStudents(
        @RequestParam(required = false) eligibleForGraduation: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val page =
            if (eligibleForGraduation == true) {
                usuarioRepo.findEligibleForGraduation(pageable)
            } else {
                usuarioRepo.searchUsuarios(null, null, true, pageable)
            }
        return PageResponse.of(page) { u ->
            val elig = if (eligibleForGraduation == true) eligibilityService.evaluate(u) else null
            mapOf(
                "id" to u.id,
                "nome" to u.nome,
                "email" to u.email,
                "grr" to u.grr,
                "eligible" to (elig?.eligible ?: false),
                "bloqueios" to (elig?.bloqueios?.map { mapOf("razao" to it.razao, "detalhe" to it.detalhe) } ?: emptyList()),
            )
        }
    }

    @PostMapping("/graduations")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Confirmar colação em lote — atribui role EGRESSO")
    @Transactional
    fun confirm(
        @Valid @RequestBody dto: ConfirmGraduationDto,
    ): ResponseEntity<Map<String, Any>> {
        val egressoRole =
            roleRepo.findByCode("EGRESSO").orElseThrow { NoSuchElementException("Role EGRESSO não cadastrada") }
        val records = mutableListOf<GraduationRecordEntity>()
        dto.alunoIds.forEach { alunoId ->
            val usuario =
                usuarioRepo.findByIdWithRoles(alunoId).orElseThrow {
                    NoSuchElementException("Aluno não encontrado: $alunoId")
                }
            val elig = eligibilityService.evaluate(usuario)
            require(elig.eligible) {
                "Aluno $alunoId não está elegível: ${elig.bloqueios.joinToString("; ") { it.razao + " — " + it.detalhe }}"
            }
            if (!graduationRepo.existsByIdAluno(alunoId)) {
                val rec =
                    graduationRepo.save(
                        GraduationRecordEntity(
                            idAluno = alunoId,
                            idCurso = dto.idCurso ?: eligibilityService.courseIdOf(usuario),
                            dataColacao = dto.dataColacao ?: LocalDate.now(),
                            observacao = dto.observacao,
                            livro = dto.livro,
                            folha = dto.folha,
                            ata = dto.ata,
                            idPeriodo = dto.periodoId,
                        ),
                    )
                runCatching { diplomaPdfService.generateAndStore(rec) }
                graduationRepo.save(rec)
                records += rec
            }
            val alreadyEgresso = usuario.usuarioRoles.any { it.role.code == "EGRESSO" }
            if (!alreadyEgresso) {
                usuario.usuarioRoles.add(UsuarioRoleEntity(usuario = usuario, role = egressoRole))
                usuarioRepo.save(usuario)
            }
        }
        records.forEach { rec ->
            outboxPublisher.enqueue(
                eventType = OutboxEventTypes.GRADUATION_CONFIRMED,
                aggregateType = "GraduationRecord",
                aggregateId = rec.id,
                payload = mapOf("alunoId" to rec.idAluno.toString()),
            )
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("processados" to dto.alunoIds.size, "registros" to records.size),
        )
    }

    @PatchMapping("/graduations/{id}/confirm-delivery")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Confirmar entrega física do diploma")
    @Transactional
    fun confirmDelivery(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val rec = graduationRepo.findById(id).orElseThrow { NoSuchElementException("Colação não encontrada: $id") }
        rec.estado = "DIPLOMA_ENTREGUE"
        rec.deliveredAt = OffsetDateTime.now()
        rec.deliveredBy = currentUserId()
        graduationRepo.save(rec)
        return ResponseEntity.ok(mapOf("id" to rec.id, "estado" to rec.estado, "deliveredAt" to rec.deliveredAt))
    }

    @GetMapping("/graduations/{id}/diploma-url")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('alumni.list') or hasAuthority('system.admin')")
    @Operation(summary = "URL pré-assinada do PDF do diploma")
    fun diplomaUrl(
        @PathVariable id: UUID,
    ): Map<String, Any?> {
        val rec = graduationRepo.findById(id).orElseThrow { NoSuchElementException("Colação não encontrada: $id") }
        val key = rec.diplomaStorageKey ?: return mapOf("id" to rec.id, "downloadUrl" to null, "status" to "SEM_PDF")
        return mapOf(
            "id" to rec.id,
            "hashSha256" to rec.diplomaHashSha256,
            "downloadUrl" to minio.generateDownloadUrl(key, expiryMinutes = 60),
        )
    }
}
