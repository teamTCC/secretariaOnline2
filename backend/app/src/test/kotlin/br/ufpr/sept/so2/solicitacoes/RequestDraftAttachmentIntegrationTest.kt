package br.ufpr.sept.so2.solicitacoes

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.shared.security.AuthenticatedUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * Fase 2 — rascunho (APP-04) + anexos (API-10 / SEC).
 * MinIO é mockado: cobre ownership, allowlist, SHA-256 e existência sem container S3.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestDraftAttachmentIntegrationTest {
    companion object {
        private const val VALID_SHA = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        private const val OTHER_SHA = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        private const val FILE_SIZE = 2048L

        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("secretaria_test")
                .withUsername("secretaria")
                .withPassword("testpwd")
                .withInitScript("db/init/enable_extensions.sql")

        @DynamicPropertySource
        @JvmStatic
        fun configureDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @MockBean lateinit var minioStorageService: MinioStorageService

    private lateinit var alunoId: UUID
    private lateinit var outroAlunoId: UUID
    private lateinit var cursoId: UUID
    private lateinit var tipoDeclaracaoId: UUID
    private lateinit var tipoSegundaChamadaId: UUID

    @BeforeAll
    fun setupTestData() {
        cursoId = jdbcTemplate.queryForObject(
            "SELECT id FROM curso WHERE sigla = 'TADS'",
            UUID::class.java,
        )!!
        tipoDeclaracaoId = jdbcTemplate.queryForObject(
            "SELECT id FROM request_type WHERE code = 'DECLARACAO_MATRICULA'",
            UUID::class.java,
        )!!
        tipoSegundaChamadaId = jdbcTemplate.queryForObject(
            "SELECT id FROM request_type WHERE code = 'SEGUNDA_CHAMADA'",
            UUID::class.java,
        )!!

        alunoId = insertUser("Aluno Draft Teste", "aluno.draft@ufpr.br", "ALUNO")
        outroAlunoId = insertUser("Outro Aluno Draft", "aluno.draft2@ufpr.br", "ALUNO")
    }

    @BeforeEach
    fun stubMinio() {
        Mockito.reset(minioStorageService)
        Mockito.`when`(minioStorageService.generateUploadUrl(anyString(), anyString(), anyInt()))
            .thenReturn("http://localhost:9000/presigned-put")
        Mockito.`when`(minioStorageService.exists(anyString())).thenReturn(true)
        Mockito.`when`(minioStorageService.objectSize(anyString())).thenReturn(FILE_SIZE)
        Mockito.`when`(minioStorageService.sha256(anyString())).thenReturn(VALID_SHA)
    }

    @Test
    fun `POST draft aceita dados incompletos e retorna RASCUNHO com links de submit`() {
        val body = mapOf(
            "idRequestType" to tipoDeclaracaoId,
            "idCurso" to cursoId,
            "dados" to emptyMap<String, Any>(),
        )

        mockMvc.perform(
            post("/requests/draft")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.estado").value("RASCUNHO"))
            .andExpect(jsonPath("$._links.submit").isString)
            .andExpect(jsonPath("$._links.update-draft").isString)
            .andExpect(jsonPath("$._links.upload-url").isString)
    }

    @Test
    fun `PATCH draft atualiza dados e POST submit promove para ABERTA`() {
        val draftId = createDraft(emptyMap())

        mockMvc.perform(
            patch("/requests/$draftId/draft")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("dados" to mapOf("finalidade" to "BOLSA")))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("RASCUNHO"))

        mockMvc.perform(post("/requests/$draftId/submit").with(alunoAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("ABERTA"))
            .andExpect(jsonPath("$.protocolo").isString)

        mockMvc.perform(get("/requests/$draftId/events").with(alunoAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].tipo").value("ABERTURA"))
            .andExpect(jsonPath("$[0].estadoNovo").value("ABERTA"))
    }

    @Test
    fun `POST draft duas vezes no mesmo curso e ano retorna 201`() {
        createDraft(emptyMap())
        createDraft(emptyMap())
    }

    @Test
    fun `POST submit com schema incompleto retorna 422`() {
        val draftId = createDraft(emptyMap())

        mockMvc.perform(post("/requests/$draftId/submit").with(alunoAuth()))
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.erros").isArray)
    }

    @Test
    fun `POST requests SEGUNDA_CHAMADA sem atestado obrigatorio retorna 422`() {
        val body = mapOf(
            "idRequestType" to tipoSegundaChamadaId,
            "idCurso" to cursoId,
            "dados" to mapOf(
                "idDisciplina" to UUID.randomUUID().toString(),
                "dataProva" to "2026-03-15",
                "motivoAusencia" to "SAUDE",
                "descricaoMotivo" to "Consulta médica no horário da prova.",
            ),
        )

        mockMvc.perform(
            post("/requests")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.erros[0]").value("Anexo obrigatório ausente: ATESTADO_MEDICO"))
    }

    @Test
    fun `POST attachments presigned-url rejeita content-type fora da allowlist`() {
        val body = mapOf(
            "filename" to "malware.exe",
            "contentType" to "application/x-msdownload",
            "sha256" to VALID_SHA,
            "sizeBytes" to FILE_SIZE,
            "categoria" to "OUTRO",
        )

        mockMvc.perform(
            post("/requests/attachments/presigned-url")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `fluxo upload-url vinculado e confirm persiste anexo com sha256`() {
        val draftId = createDraft(mapOf("finalidade" to "BOLSA"))

        val uploadBody = mapOf(
            "filename" to "historico.pdf",
            "contentType" to "application/pdf",
            "sha256" to VALID_SHA,
            "sizeBytes" to FILE_SIZE,
            "categoria" to "HISTORICO_ESCOLAR",
        )

        val uploadResult = mockMvc.perform(
            post("/requests/$draftId/attachments/upload-url")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uploadBody)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uploadUrl").value("http://localhost:9000/presigned-put"))
            .andExpect(jsonPath("$.storageKey").isString)
            .andReturn()

        val storageKey = objectMapper.readTree(uploadResult.response.contentAsString)["storageKey"].asText()

        val confirmBody = mapOf(
            "storageKey" to storageKey,
            "sha256" to VALID_SHA,
            "nomeOriginal" to "historico.pdf",
            "contentType" to "application/pdf",
            "categoria" to "HISTORICO_ESCOLAR",
            "tamanhoBytes" to FILE_SIZE,
        )

        mockMvc.perform(
            post("/requests/$draftId/attachments/confirm")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmBody)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.sha256").value(VALID_SHA))
            .andExpect(jsonPath("$.categoria").value("HISTORICO_ESCOLAR"))

        mockMvc.perform(get("/requests/$draftId/attachments").with(alunoAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].storageKey").value(storageKey))
    }

    @Test
    fun `POST confirm com SHA-256 divergente retorna 400`() {
        val draftId = createDraft(mapOf("finalidade" to "BOLSA"))
        Mockito.`when`(minioStorageService.sha256(anyString())).thenReturn(OTHER_SHA)

        val confirmBody = mapOf(
            "storageKey" to "requests/orphan/${UUID.randomUUID()}_historico.pdf",
            "sha256" to VALID_SHA,
            "nomeOriginal" to "historico.pdf",
            "contentType" to "application/pdf",
            "categoria" to "HISTORICO_ESCOLAR",
            "tamanhoBytes" to FILE_SIZE,
        )

        mockMvc.perform(
            post("/requests/$draftId/attachments/confirm")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmBody)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST confirm quando arquivo nao existe no MinIO retorna 400`() {
        val draftId = createDraft(mapOf("finalidade" to "BOLSA"))
        Mockito.`when`(minioStorageService.exists(anyString())).thenReturn(false)

        val confirmBody = mapOf(
            "storageKey" to "requests/orphan/${UUID.randomUUID()}_historico.pdf",
            "sha256" to VALID_SHA,
            "nomeOriginal" to "historico.pdf",
            "contentType" to "application/pdf",
            "categoria" to "HISTORICO_ESCOLAR",
            "tamanhoBytes" to FILE_SIZE,
        )

        mockMvc.perform(
            post("/requests/$draftId/attachments/confirm")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmBody)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST confirm de outro aluno retorna 403`() {
        val draftId = createDraft(mapOf("finalidade" to "BOLSA"))
        val confirmBody = mapOf(
            "storageKey" to "requests/orphan/${UUID.randomUUID()}_historico.pdf",
            "sha256" to VALID_SHA,
            "nomeOriginal" to "historico.pdf",
            "contentType" to "application/pdf",
            "categoria" to "HISTORICO_ESCOLAR",
            "tamanhoBytes" to FILE_SIZE,
        )

        mockMvc.perform(
            post("/requests/$draftId/attachments/confirm")
                .with(alunoAuth(outroAlunoId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmBody)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST confirm rejeita storageKey de outra solicitacao`() {
        val draftId = createDraft(mapOf("finalidade" to "BOLSA"))
        val confirmBody = mapOf(
            "storageKey" to "requests/${UUID.randomUUID()}/stolen.pdf",
            "sha256" to VALID_SHA,
            "nomeOriginal" to "stolen.pdf",
            "contentType" to "application/pdf",
            "categoria" to "HISTORICO_ESCOLAR",
            "tamanhoBytes" to FILE_SIZE,
        )

        mockMvc.perform(
            post("/requests/$draftId/attachments/confirm")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmBody)),
        )
            .andExpect(status().isBadRequest)
    }

    private fun createDraft(dados: Map<String, Any>): String {
        val body = mapOf(
            "idRequestType" to tipoDeclaracaoId,
            "idCurso" to cursoId,
            "dados" to dados,
        )
        val result = mockMvc.perform(
            post("/requests/draft")
                .with(alunoAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"].asText()
    }

    private fun insertUser(nome: String, email: String, role: String): UUID {
        val id = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO usuario (id, nome, email, senha_hash, senha_alterada, ativo, metadata)
            VALUES (?::uuid, ?, ?, ?, true, true, '{}'::jsonb)
            """.trimIndent(),
            id.toString(),
            nome,
            email,
            "hash_irrelevante",
        )
        jdbcTemplate.update(
            """
            INSERT INTO usuario_role (id_usuario, id_role, escopo)
            SELECT ?::uuid, r.id, '{}'::jsonb FROM role r WHERE r.code = ?
            """.trimIndent(),
            id.toString(),
            role,
        )
        return id
    }

    private fun alunoAuth(userId: UUID = alunoId) =
        authentication(
            UsernamePasswordAuthenticationToken(
                AuthenticatedUser(userId = userId, authorities = setOf("request.open", "request.view_own")),
                null,
                listOf(
                    SimpleGrantedAuthority("request.open"),
                    SimpleGrantedAuthority("request.view_own"),
                ),
            ),
        )
}
