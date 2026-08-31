package br.ufpr.sept.so2.modules.iam.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// ─── Profile ──────────────────────────────────────────────────────────────────

data class UpdateProfileDto(
    val nome: String?,
    val metadata: Map<String, Any>?,
)

data class ChangePasswordDto(
    @field:NotBlank val senhaAtual: String,
    @field:NotBlank @field:Size(min = 8) val novaSenha: String,
)

data class UpdateNotificationsDto(
    val emailEnabled: Boolean?,
    val pushEnabled: Boolean?,
    val inAppEnabled: Boolean?,
)

// ─── Users ────────────────────────────────────────────────────────────────────

data class CreateUsuarioDto(
    @field:NotBlank val nome: String,
    @field:NotBlank @field:Email val email: String,
    val grr: String?,
    @field:NotBlank val roleCode: String,
)

data class UpdateStatusDto(
    val ativo: Boolean,
)

// ─── Graduation ───────────────────────────────────────────────────────────────

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

// ─── Service Records ──────────────────────────────────────────────────────────

data class CreateServiceRecordDto(
    val idAluno: UUID,
    @field:NotBlank val assunto: String,
    val descricao: String? = null,
    val tipo: String = "PRESENCIAL",
)

data class ScheduleServiceRecordDto(
    @field:NotBlank val assunto: String,
    val descricao: String? = null,
    val tipo: String = "AGENDAMENTO",
)

// ─── Admin Roles ──────────────────────────────────────────────────────────────

data class CreateRoleDto(
    @field:NotBlank val code: String,
    @field:NotBlank val descricao: String,
)

data class UpdateRoleDto(
    val descricao: String?,
)

data class RoleAuthoritiesDto(
    @field:NotEmpty val authorityCodes: List<String>,
)

data class UserRolesDto(
    @field:NotEmpty val roleCodes: List<String>,
)

// ─── FCM Token ────────────────────────────────────────────────────────────────

data class RegisterFcmTokenDto(
    @field:NotBlank val fcmToken: String,
    val plataforma: String = "ANDROID",
)

data class UnregisterFcmTokenDto(
    @field:NotBlank val fcmToken: String,
)

// ─── Contato Público ──────────────────────────────────────────────────────────

data class ContatoFormDto(
    @field:NotBlank @field:Size(max = 200) val nome: String = "",
    @field:NotBlank @field:Email val email: String = "",
    @field:NotBlank @field:Size(max = 300) val assunto: String = "",
    @field:NotBlank @field:Size(max = 4000) val mensagem: String = "",
)

// ─── Secretary Tasks ──────────────────────────────────────────────────────────

data class CreateTaskDto(
    @field:NotBlank val titulo: String,
    val descricao: String? = null,
    val prioridade: String = "NORMAL",
    val prazoEm: OffsetDateTime? = null,
    val idAssignee: UUID? = null,
)

data class PatchTaskDto(
    val titulo: String? = null,
    val descricao: String? = null,
    val estado: String? = null,
    val prioridade: String? = null,
    val idAssignee: UUID? = null,
    val prazoEm: OffsetDateTime? = null,
)

// ─── Support / Tickets ────────────────────────────────────────────────────────

data class CreateTicketDto(
    @field:NotBlank val assunto: String,
    @field:NotBlank val descricao: String,
)

data class RespondTicketDto(
    @field:NotBlank val resposta: String,
)

data class CreateFaqItemDto(
    @field:NotBlank val categoria: String,
    @field:NotBlank val pergunta: String,
    @field:NotBlank val resposta: String,
    val ordem: Int = 0,
)

data class UpdateFaqItemDto(
    val pergunta: String?,
    val resposta: String?,
    val ordem: Int?,
    val ativo: Boolean?,
)
