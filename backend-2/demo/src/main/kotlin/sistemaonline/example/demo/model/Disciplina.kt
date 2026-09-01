package sistemaonline.example.demo.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*

@Entity
data class Disciplina(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long? = null,

    var nome: String,
    
    var codigo: String,
       
    var periodo: Int,
    
    @Column(name = "carga_horaria")
    var cargaHoraria: Int,
    
    var ativa: Boolean = true
)
