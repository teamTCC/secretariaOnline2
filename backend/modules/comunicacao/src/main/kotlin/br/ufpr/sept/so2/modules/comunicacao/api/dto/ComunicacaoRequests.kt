package br.ufpr.sept.so2.modules.comunicacao.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

data class PublishCommunicationDto(
    @field:NotBlank val titulo: String,
    @field:NotBlank val conteudo: String,
    @field:NotBlank @field:Pattern(regexp = "AVISO|URGENTE|INFORMATIVO") val tipo: String,
    val cursoId: UUID?,
)

data class CreateTemplateDto(
    @field:NotBlank val codigo: String,
    @field:NotBlank val titulo: String,
    @field:NotBlank val assunto: String,
    @field:NotBlank val corpo: String,
    val canal: String = "EMAIL",
)

data class TemplateRevisionDto(
    @field:NotBlank val assunto: String,
    @field:NotBlank val corpo: String,
)
