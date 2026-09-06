package sistemaonline.example.demo.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
data class Curso(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long? = null,

    var nome: String,
    
    var sigla: String,
    
    var codigo: String,
    
    @Column(name = "horas_formativas_req")
    var horasFormativasReq: Int,
    
    var ativo: Boolean = true,
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    var createdAt: LocalDateTime? = null
)
