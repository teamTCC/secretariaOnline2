package br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

@Entity
@Table(
    name = "communication_template",
    indexes = [Index(name = "idx_comm_template_codigo", columnList = "codigo", unique = true)],
)
class CommunicationTemplateEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 80)
    var codigo: String,
    @Column(nullable = false, length = 200)
    var titulo: String,
    @Column(nullable = false, length = 300)
    var assunto: String,
    @Column(nullable = false, columnDefinition = "text")
    var corpo: String,
    @Column(nullable = false, length = 20)
    var canal: String = "EMAIL",
    @Column(nullable = false)
    var versao: Int = 1,
    @Column(nullable = false)
    var ativo: Boolean = true,
) : BaseEntity(id)

@Entity
@Table(name = "communication_template_revision")
class CommunicationTemplateRevisionEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_template", nullable = false)
    val idTemplate: UUID,
    @Column(nullable = false)
    val versao: Int,
    @Column(nullable = false, length = 300)
    val assunto: String,
    @Column(nullable = false, columnDefinition = "text")
    val corpo: String,
    @Column(name = "id_autor")
    val idAutor: UUID?,
) : BaseEntity(id)

interface CommunicationTemplateJpaRepository : JpaRepository<CommunicationTemplateEntity, UUID> {
    fun findByCodigo(codigo: String): Optional<CommunicationTemplateEntity>
}

interface CommunicationTemplateRevisionJpaRepository : JpaRepository<CommunicationTemplateRevisionEntity, UUID> {
    fun findAllByIdTemplateOrderByVersaoDesc(idTemplate: UUID): List<CommunicationTemplateRevisionEntity>

    fun findByIdTemplateAndVersao(
        idTemplate: UUID,
        versao: Int,
    ): Optional<CommunicationTemplateRevisionEntity>
}
