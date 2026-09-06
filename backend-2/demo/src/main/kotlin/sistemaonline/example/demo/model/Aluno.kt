package sistemaonline.example.demo.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

import io.swagger.v3.oas.annotations.media.Schema

@Entity
data class Aluno(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long? = null,
    
    var nome: String,
    
    var idade: Int
)
