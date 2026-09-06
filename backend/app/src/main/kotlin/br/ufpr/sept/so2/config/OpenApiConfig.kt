package br.ufpr.sept.so2.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
) {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("SecretariaOnline2 API")
                    .description("API REST do sistema de gestão acadêmica — UFPR SEPT (TCC)")
                    .version("0.1.0")
                    .contact(Contact().name("UFPR SEPT").url("https://sept.ufpr.br"))
                    .license(License().name("Uso interno UFPR").url("https://ufpr.br")),
            ).servers(listOf(Server().url(baseUrl).description("Servidor atual")))
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token JWT obtido via POST /auth/login"),
                ),
            ).addSecurityItem(SecurityRequirement().addList("bearerAuth"))
}
