package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "contact_message")
class ContactMessageEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 200)
    val nome: String,
    @Column(nullable = false, length = 200)
    val email: String,
    @Column(nullable = false, length = 300)
    val assunto: String,
    @Column(nullable = false, columnDefinition = "text")
    val mensagem: String,
    @Column(nullable = false, length = 20)
    var status: String = "NOVO",
) : BaseEntity(id)

interface ContactMessageJpaRepository : JpaRepository<ContactMessageEntity, UUID>
