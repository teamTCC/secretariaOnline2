package br.ufpr.sept.so2.modules.iam.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// ─── Profile ──────────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val grr: String?,
    val ativo: Boolean,
    val metadata: Any?,
    val roles: List<String>,
    val mustChangePassword: Boolean = false,
    val mustAcceptLgpd: Boolean = false,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileUpdatedResponse(
    val id: UUID,
    val nome: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NotificationPrefsResponse(
    val emailEnabled: Boolean,
    val pushEnabled: Boolean,
    val inAppEnabled: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AvatarUploadUrlResponse(
    val uploadUrl: String,
    val storageKey: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DataExportResponse(
    val jobId: String,
    val downloadUrl: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DataExportStatusResponse(
    val jobId: String,
    val status: String,
    val downloadUrl: String?,
    val expiresAt: String?,
)

// ─── Users Admin ──────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UsuarioSummaryResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val grr: String?,
    val ativo: Boolean,
    val roles: List<String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UsuarioDetailResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val grr: String?,
    val ativo: Boolean,
    val metadata: Any?,
    val roles: List<String>,
    val senhaAlterada: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UsuarioCreatedResponse(
    val id: UUID,
    val email: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UsuarioStatusResponse(
    val id: UUID,
    val ativo: Boolean,
)

// ─── Generic ──────────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MessageResponse(
    val mensagem: String,
)

// ─── Graduation ───────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GraduationCreatedResponse(
    val id: UUID,
    val estado: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GraduationUpdatedResponse(
    val id: UUID,
    val estado: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GraduationBatchResponse(
    val processados: Int,
    val registros: Any?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GraduationDeliveryResponse(
    val id: UUID?,
    val estado: String?,
    val deliveredAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DiplomaUrlResponse(
    val id: UUID?,
    val hashSha256: String?,
    val downloadUrl: String?,
    val status: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EgressoListItemResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val grr: String?,
    val situacaoDiploma: String,
    val dataColacao: LocalDate?,
    val graduationId: UUID?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GraduationListItemLinks(
    @JsonProperty("confirm-delivery") val confirmDelivery: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GraduationListItemResponse(
    val id: UUID,
    val idAluno: UUID?,
    val idCurso: UUID?,
    val dataColacao: LocalDate?,
    val estado: String,
    val deliveredAt: Any?,
    val livro: String?,
    val folha: String?,
    val ata: String?,
    val diplomaHashSha256: String?,
    @JsonProperty("_links") val links: GraduationListItemLinks,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EligibilityBloqueio(
    val razao: String,
    val detalhe: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StudentListItemResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val grr: String?,
    val eligible: Boolean,
    val bloqueios: List<EligibilityBloqueio>,
)

// ─── Service Records ──────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServiceRecordCreatedResponse(
    val id: UUID,
    val tipo: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServiceRecordSummaryResponse(
    val id: UUID,
    val tipo: String,
    val estado: String?,
    val createdAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServiceRecordLinks(
    val self: String,
    val acknowledge: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServiceRecordResponse(
    val id: UUID,
    val idAluno: UUID?,
    val idSecretario: UUID?,
    val assunto: String,
    val tipo: String,
    val descricao: String?,
    val estado: String?,
    val acknowledgedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
    @JsonProperty("_links") val links: ServiceRecordLinks,
)

// ─── Admin Roles ──────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoleResponse(
    val id: UUID,
    val code: String,
    val descricao: String?,
    val authorities: List<String> = emptyList(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoleCreatedResponse(
    val id: UUID,
    val code: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoleDetailResponse(
    val id: UUID?,
    val code: String,
    val descricao: String?,
    val authorities: List<String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthorityResponse(
    val id: UUID,
    val code: String,
    val descricao: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserRolesResponse(
    val id: UUID,
    val roles: List<String>,
)

// ─── Import ───────────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImportJobResponse(
    val jobId: UUID,
    val totalLinhas: Int?,
    val status: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImportJobDetailResponse(
    val jobId: UUID,
    val status: String?,
    val totalRows: Int?,
    val errorCount: Int?,
    val successCount: Int?,
    val errors: Any?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImportJobConfirmResponse(
    val jobId: UUID,
    val status: String?,
    val successCount: Int?,
    val errorCount: Int?,
)

// ─── FCM Token ────────────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FcmRegisteredResponse(
    val registered: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FcmUnregisteredResponse(
    val unregistered: Boolean,
)

// ─── Contato Público ──────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactInfoLinks(
    val enviar: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactInfoResponse(
    val nome: String,
    val endereco: String,
    val telefone: String,
    val email: String,
    val horario: String,
    @JsonProperty("_links") val links: ContactInfoLinks,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContactMessageAcceptedResponse(
    val id: UUID,
    val status: String,
    val mensagem: String,
)

// ─── Secretary Tasks ──────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SecretaryTaskResponse(
    val id: UUID,
    val titulo: String,
    val descricao: String?,
    val estado: String,
    val prioridade: String,
    val idAssignee: UUID?,
    val prazoEm: OffsetDateTime?,
)

// ─── Support / Tickets ────────────────────────────────────────────────────────

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FaqItemResponse(
    val id: UUID,
    val categoria: String,
    val pergunta: String,
    val resposta: String,
    val ordem: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FaqCreatedResponse(
    val id: UUID,
    val categoria: String,
    val pergunta: String,
    val ordem: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FaqUpdatedResponse(
    val id: UUID,
    val categoria: String,
    val pergunta: String,
    val resposta: String,
    val ordem: Int,
    val ativo: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TicketCreatedResponse(
    val id: UUID,
    val estado: String,
    val assunto: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TicketSummaryResponse(
    val id: UUID,
    val assunto: String,
    val estado: String,
    val resposta: String?,
    val createdAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TicketAdminSummaryResponse(
    val id: UUID,
    val idUsuario: UUID?,
    val assunto: String,
    val estado: String,
    val idAtendente: UUID?,
    val createdAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TicketStateResponse(
    val id: UUID,
    val estado: String,
)

// Aliases used by controllers after the DTO split (same JVM package).
typealias GraduationEgressoItem = EgressoListItemResponse
typealias StudentEligibilityItem = StudentListItemResponse
typealias GraduationConfirmedResponse = GraduationBatchResponse
typealias DiplomaDeliveryResponse = GraduationDeliveryResponse
typealias AvatarUploadResponse = AvatarUploadUrlResponse
typealias DataExportStartedResponse = DataExportResponse
typealias NotificationPrefResponse = NotificationPrefsResponse

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PasswordResetEnqueuedResponse(
    val mensagem: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GraduationRecordResponse(
    val id: UUID,
    val idAluno: UUID?,
    val idCurso: UUID?,
    val dataColacao: LocalDate?,
    val estado: String,
    val deliveredAt: Any?,
    val livro: String?,
    val folha: String?,
    val ata: String?,
    val diplomaHashSha256: String?,
    @JsonProperty("_links") val links: Map<String, String>,
)
