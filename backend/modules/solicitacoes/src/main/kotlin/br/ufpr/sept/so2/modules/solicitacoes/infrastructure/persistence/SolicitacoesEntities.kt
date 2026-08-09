package br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.Type
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "request_type",
    indexes = [Index(name = "idx_request_type_code", columnList = "code", unique = true)],
)
class RequestTypeEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 60)
    var code: String,
    @Column(nullable = false, length = 200)
    var descricao: String,
    @Column(name = "form_schema", columnDefinition = "jsonb", nullable = false)
    @Type(JsonType::class)
    var formSchema: Map<String, Any> = emptyMap(),
    @Column(name = "workflow_json", columnDefinition = "jsonb", nullable = false)
    @Type(JsonType::class)
    var workflowJson: Map<String, Any> = emptyMap(),
    @Column(name = "prazo_dias", nullable = false)
    var prazoDias: Int = 10,
    @Column(nullable = false)
    var ativo: Boolean = true,
) : BaseEntity(id)

@Entity
@Table(
    name = "request",
    indexes = [
        Index(name = "idx_request_solicitante", columnList = "id_solicitante"),
        Index(name = "idx_request_tipo", columnList = "id_request_type"),
        Index(name = "idx_request_curso", columnList = "id_curso"),
        Index(name = "idx_request_estado", columnList = "estado"),
        Index(name = "idx_request_curso_estado", columnList = "id_curso, estado"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class RequestEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "numero_anual", nullable = false)
    var numeroAnual: Int,
    @Column(nullable = false)
    var ano: Short,
    @Column(name = "id_request_type", nullable = false)
    var idRequestType: UUID,
    @Column(name = "request_type_code", nullable = false, length = 60)
    var requestTypeCode: String,
    @Column(name = "id_solicitante", nullable = false)
    var idSolicitante: UUID,
    @Column(name = "id_curso", nullable = false)
    var idCurso: UUID,
    @Column(nullable = false, length = 30)
    var estado: String,
    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(JsonType::class)
    var dados: Map<String, Any> = emptyMap(),
    @Column(columnDefinition = "text")
    var parecer: String? = null,
    @Column(name = "prazo_em")
    var prazoEm: OffsetDateTime? = null,
    @Column(name = "concluded_at")
    var concludedAt: OffsetDateTime? = null,
    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
) : BaseEntity(id)

@Entity
@Table(
    name = "request_event",
    indexes = [Index(name = "idx_request_event_request", columnList = "id_request")],
)
class RequestEventEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_request", nullable = false)
    val idRequest: UUID,
    @Column(nullable = false, length = 50)
    val tipo: String,
    @Column(name = "estado_anterior", nullable = false, length = 30)
    val estadoAnterior: String,
    @Column(name = "estado_novo", nullable = false, length = 30)
    val estadoNovo: String,
    @Column(name = "id_ator", nullable = false)
    val idAtor: UUID,
    @Column(columnDefinition = "text")
    val parecer: String? = null,
) : BaseEntity(id)

@Entity
@Table(
    name = "request_attachment",
    indexes = [Index(name = "idx_request_attachment_request", columnList = "id_request")],
)
class RequestAttachmentEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_request", nullable = false)
    val idRequest: UUID,
    @Column(nullable = false, length = 100)
    val categoria: String,
    @Column(name = "storage_key", nullable = false, length = 500)
    val storageKey: String,
    @Column(name = "sha256", nullable = false, length = 64)
    val sha256: String,
    @Column(name = "nome_original", nullable = false, length = 300)
    val nomeOriginal: String,
    @Column(name = "content_type", nullable = false, length = 100)
    val contentType: String,
    @Column(name = "tamanho_bytes", nullable = false)
    val tamanhoBytes: Long,
) : BaseEntity(id)
