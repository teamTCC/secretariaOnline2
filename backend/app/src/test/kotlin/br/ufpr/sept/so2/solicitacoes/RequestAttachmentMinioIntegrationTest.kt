package br.ufpr.sept.so2.solicitacoes

import br.ufpr.sept.so2.shared.security.AuthenticatedUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID

/**
 * TST-03 — fluxo real: upload-url → PUT MinIO → confirm (SHA-256 calculado no servidor).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestAttachmentMinioIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("secretaria_test")
                .withUsername("secretaria")
                .withPassword("testpwd")
                .withInitScript("db/init/enable_extensions.sql")

        @Container
        @JvmStatic
        val minio: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("minio/minio:RELEASE.2024-12-18T13-15-44Z"))
                .withCommand("server", "/data")
                .withEnv("MINIO_ROOT_USER", "minioadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                .withExposedPorts(9000)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2))

        @DynamicPropertySource
        @JvmStatic
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("minio.endpoint") { "http://${minio.host}:${minio.getMappedPort(9000)}" }
            registry.add("minio.access-key") { "minioadmin" }
            registry.add("minio.secret-key") { "minioadmin" }
            registry.add("minio.bucket") { "secretaria-docs-test" }
        }
    }

    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var alunoId: UUID
    private lateinit var cursoId: UUID
    private lateinit var tipoId: UUID

    @BeforeAll
    fun setupTestData() {
        cursoId = jdbcTemplate.queryForObject("SELECT id FROM curso WHERE sigla = 'TADS'", UUID::class.java)!!
        tipoId = jdbcTemplate.queryForObject(
            "SELECT id FROM request_type WHERE code = 'DECLARACAO_MATRICULA'",
            UUID::class.java,
        )!!
        alunoId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO usuario (id, nome, email, senha_hash, senha_alterada, ativo, metadata)
            VALUES (?::uuid, ?, ?, ?, true, true, '{}'::jsonb)
            """.trimIndent(),
            alunoId.toString(),
            "Aluno MinIO Teste",
            "aluno.minio@ufpr.br",
            "hash_irrelevante",
        )
        jdbcTemplate.update(
            """
            INSERT INTO usuario_role (id_usuario, id_role, escopo)
            SELECT ?::uuid, r.id, '{}'::jsonb FROM role r WHERE r.code = 'ALUNO'
            """.trimIndent(),
            alunoId.toString(),
        )
    }

    @Test
    fun `PUT presigned no MinIO e confirm valida SHA-256 server-side`() {
        val pdfBytes = "%PDF-1.4\nsecretaria-online-2-anexo-teste\n".toByteArray()
        val sha256 = sha256Hex(pdfBytes)

        val draftBody = mapOf(
            "idRequestType" to tipoId,
            "idCurso" to cursoId,
            "dados" to mapOf("finalidade" to "BOLSA"),
        )
        val draftResult = mockMvc.perform(
            post("/requests/draft")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(draftBody)),
        )
            .andExpect(status().isCreated)
            .andReturn()
        val draftId = objectMapper.readTree(draftResult.response.contentAsString)["id"].asText()

        val uploadBody = mapOf(
            "filename" to "historico.pdf",
            "contentType" to "application/pdf",
            "sha256" to sha256,
            "sizeBytes" to pdfBytes.size.toLong(),
            "categoria" to "HISTORICO_ESCOLAR",
        )
        val uploadResult = mockMvc.perform(
            post("/requests/$draftId/attachments/upload-url")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uploadBody)),
        )
            .andExpect(status().isOk)
            .andReturn()

        val uploadJson = objectMapper.readTree(uploadResult.response.contentAsString)
        val uploadUrl = uploadJson["uploadUrl"].asText()
        val storageKey = uploadJson["storageKey"].asText()

        val putResponse =
            HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(uploadUrl))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(pdfBytes))
                    .header("Content-Type", "application/pdf")
                    .build(),
                HttpResponse.BodyHandlers.discarding(),
            )
        require(putResponse.statusCode() in 200..299) {
            "PUT MinIO falhou: HTTP ${putResponse.statusCode()}"
        }

        val confirmBody = mapOf(
            "storageKey" to storageKey,
            "sha256" to sha256,
            "nomeOriginal" to "historico.pdf",
            "contentType" to "application/pdf",
            "categoria" to "HISTORICO_ESCOLAR",
            "tamanhoBytes" to pdfBytes.size.toLong(),
        )
        mockMvc.perform(
            post("/requests/$draftId/attachments/confirm")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmBody)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.sha256").value(sha256))
            .andExpect(jsonPath("$.storageKey").value(storageKey))
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun alunoAuth() =
        authentication(
            UsernamePasswordAuthenticationToken(
                AuthenticatedUser(userId = alunoId, authorities = setOf("request.open", "request.view_own")),
                null,
                listOf(
                    SimpleGrantedAuthority("request.open"),
                    SimpleGrantedAuthority("request.view_own"),
                ),
            ),
        )
}
