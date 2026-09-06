package br.ufpr.sept.so2.modules.iam.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.contato")
data class ContatoProperties(
    val nome: String = "Secretaria SEPT — UFPR",
    val endereco: String = "Rua Dr. Alcides Vieira Arcoverde, 1225 — Jardim das Américas, Curitiba/PR",
    val telefone: String = "(41) 3360-4900",
    val email: String = "secretaria.sept@ufpr.br",
    val horario: String = "Segunda a sexta, 8h–17h",
)
