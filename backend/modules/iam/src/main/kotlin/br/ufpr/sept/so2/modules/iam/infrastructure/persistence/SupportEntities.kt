package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "faq_item")
class FaqItemEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    val categoria: String,
    @Column(nullable = false, length = 500)
    var pergunta: String,
    @Column(nullable = false, columnDefinition = "text")
    var resposta: String,
    @Column(nullable = false)
    var ordem: Int = 0,
    @Column(nullable = false)
    var ativo: Boolean = true,
) : BaseEntity(id)

@Entity
@Table(
    name = "support_ticket",
    indexes = [
        Index(name = "idx_support_ticket_usuario", columnList = "id_usuario"),
        Index(name = "idx_support_ticket_estado", columnList = "estado"),
    ],
)
class SupportTicketEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_usuario", nullable = false)
    val idUsuario: UUID,
    @Column(nullable = false, length = 300)
    val assunto: String,
    @Column(nullable = false, columnDefinition = "text")
    val descricao: String,
    @Column(nullable = false, length = 20)
    var estado: String = "ABERTO",
    @Column(columnDefinition = "text")
    var resposta: String? = null,
    @Column(name = "id_atendente")
    var idAtendente: UUID? = null,
) : BaseEntity(id)
