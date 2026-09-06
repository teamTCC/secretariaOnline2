package br.ufpr.sept.so2.solicitacoes

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.shared.security.AuthenticatedUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * Testes de integração do motor genérico de workflow de solicitações.
 *
 * Cenários cobertos (§TST-02 do gap report):
 *  1. POST /requests com payload válido → 201 Created
 *  2. POST /requests com dados inválidos → 422 (RFC 7807) com campo `erros`
 *  3. POST /requests/{id}/transitions ASSIGN como secretário → 200, estado EM_TRIAGEM
 *  4. POST /requests/{id}/transitions ASSIGN como aluno → 403 Forbidden
 *
 * Infra:
 *  - PostgreSQL 16 via Testcontainers (Flyway roda todas as migrations, incluindo seeds)
 *  - MinioStorageService é @MockBean: evita conexão real ao MinIO no boot
 *  - Autenticação via SecurityMockMvcRequestPostProcessors (bypassa JWT, testa FGAC real)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RequestWorkflowIntegrationTest {
    companion object {
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

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    // Previne que MinioStorageService.run() tente criar bucket no boot
    @MockBean
    @Suppress("unused")
    lateinit var minioStorageService: MinioStorageService

    // IDs de usuários criados no @BeforeAll para isolar dados de teste
    private lateinit var alunoId: UUID
    private lateinit var secretarioId: UUID
    private lateinit var cursoId: UUID

    // ID de uma solicitação aberta usada nos cenários de transição
    private var solicitacaoAbertaId: String? = null

    @BeforeAll
    fun setupTestData() {
        cursoId = jdbcTemplate.queryForObject(
            "SELECT id FROM curso WHERE sigla = 'TADS'",
            UUID::class.java,
        )!!

        alunoId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO usuario (id, nome, email, senha_hash, senha_alterada, ativo, metadata)
            VALUES (?::uuid, ?, ?, ?, true, true, '{}'::jsonb)
            """.trimIndent(),
            alunoId.toString(),
            "Aluno Integração Teste",
            "aluno.integracao@ufpr.br",
            "hash_irrelevante",
        )
        jdbcTemplate.update(
            """
            INSERT INTO usuario_role (id_usuario, id_role, escopo)
            SELECT ?::uuid, r.id, '{}'::jsonb
            FROM role r WHERE r.code = 'ALUNO'
            """.trimIndent(),
            alunoId.toString(),
        )

        secretarioId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO usuario (id, nome, email, senha_hash, senha_alterada, ativo, metadata)
            VALUES (?::uuid, ?, ?, ?, true, true, '{}'::jsonb)
            """.trimIndent(),
            secretarioId.toString(),
            "Secretário Integração Teste",
            "secretario.integracao@ufpr.br",
            "hash_irrelevante",
        )
        jdbcTemplate.update(
            """
            INSERT INTO usuario_role (id_usuario, id_role, escopo)
            SELECT ?::uuid, r.id, '{}'::jsonb
            FROM role r WHERE r.code = 'SECRETARIO'
            """.trimIndent(),
            secretarioId.toString(),
        )
    }

    // ─── Cenário 1: abertura válida ───────────────────────────────────────────

    @Test
    @Order(1)
    fun `POST requests com payload valido retorna 201 e persiste solicitacao`() {
        val tipoId = jdbcTemplate.queryForObject(
            "SELECT id FROM request_type WHERE code = 'DECLARACAO_MATRICULA'",
            UUID::class.java,
        )!!

        val body = mapOf(
            "idRequestType" to tipoId,
            "idCurso" to cursoId,
            "dados" to mapOf("finalidade" to "BOLSA"),
        )

        val result = mockMvc
            .perform(
                post("/requests")
                    .with(alunoAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isString)
            .andExpect(jsonPath("$._links.self").isString)
            .andReturn()

        // Captura o ID para reutilizar nos cenários de transição
        val json = objectMapper.readTree(result.response.contentAsString)
        solicitacaoAbertaId = json["id"].asText()
    }

    // ─── Cenário 2: form_schema inválido → 422 RFC 7807 ───────────────────────

    @Test
    @Order(2)
    fun `POST requests com dados faltando campo obrigatorio retorna 422 com erros do form_schema`() {
        val tipoId = jdbcTemplate.queryForObject(
            "SELECT id FROM request_type WHERE code = 'DECLARACAO_MATRICULA'",
            UUID::class.java,
        )!!

        val body = mapOf(
            "idRequestType" to tipoId,
            "idCurso" to cursoId,
            "dados" to emptyMap<String, Any>(), // ausência de `finalidade` (obrigatório)
        )

        mockMvc
            .perform(
                post("/requests")
                    .with(alunoAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.title").value("Payload inválido segundo o form_schema"))
            .andExpect(jsonPath("$.erros").isArray)
            .andExpect(jsonPath("$.erros[0]").isString)
    }

    // ─── Cenário 3: transição ASSIGN como secretário → 200, EM_TRIAGEM ────────

    @Test
    @Order(3)
    fun `POST transitions ASSIGN como secretario retorna 200 e avanca para EM_TRIAGEM`() {
        requireNotNull(solicitacaoAbertaId) { "Dependência: cenário 1 deve ter criado a solicitação" }

        val body = mapOf("action" to "ASSIGN")

        mockMvc
            .perform(
                post("/requests/$solicitacaoAbertaId/transitions")
                    .with(secretarioAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estadoNovo").value("EM_TRIAGEM"))
    }

    // ─── Cenário 4: transição ASSIGN como aluno → 403 ─────────────────────────

    @Test
    @Order(4)
    fun `POST transitions ASSIGN como aluno retorna 403 por falta de autoridade`() {
        val tipoId = jdbcTemplate.queryForObject(
            "SELECT id FROM request_type WHERE code = 'DECLARACAO_MATRICULA'",
            UUID::class.java,
        )!!

        // Cria uma nova solicitação para este cenário (a anterior já mudou de estado)
        val abertura = mapOf(
            "idRequestType" to tipoId,
            "idCurso" to cursoId,
            "dados" to mapOf("finalidade" to "CONVENIO"),
        )
        val aberturaResult = mockMvc
            .perform(
                post("/requests")
                    .with(alunoAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(abertura)),
            )
            .andExpect(status().isCreated)
            .andReturn()

        val novaId = objectMapper
            .readTree(aberturaResult.response.contentAsString)["id"]
            .asText()

        val transicao = mapOf("action" to "ASSIGN")

        mockMvc
            .perform(
                post("/requests/$novaId/transitions")
                    .with(alunoAuth()) // aluno não tem request.deliberate
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transicao)),
            )
            .andExpect(status().isForbidden)
    }

    // ─── Helpers de autenticação ──────────────────────────────────────────────

    private fun alunoAuth() =
        authentication(
            UsernamePasswordAuthenticationToken(
                AuthenticatedUser(
                    userId = alunoId,
                    authorities = setOf("request.open", "request.view_own"),
                ),
                null,
                listOf(
                    SimpleGrantedAuthority("request.open"),
                    SimpleGrantedAuthority("request.view_own"),
                ),
            ),
        )

    private fun secretarioAuth() =
        authentication(
            UsernamePasswordAuthenticationToken(
                AuthenticatedUser(
                    userId = secretarioId,
                    authorities = setOf(
                        "request.deliberate",
                        "request.view_curso",
                        "request.internal_open",
                    ),
                ),
                null,
                listOf(
                    SimpleGrantedAuthority("request.deliberate"),
                    SimpleGrantedAuthority("request.view_curso"),
                    SimpleGrantedAuthority("request.internal_open"),
                ),
            ),
        )
}
