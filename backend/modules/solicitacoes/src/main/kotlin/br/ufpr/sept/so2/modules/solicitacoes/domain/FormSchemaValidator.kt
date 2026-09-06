package br.ufpr.sept.so2.modules.solicitacoes.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion

/**
 * Validates a JSONB dados payload against a JSON Schema Draft-07 definition.
 * This is a pure domain service — no Spring beans, no JPA.
 *
 * Why here (not in application): Validation is part of the domain invariant
 * "request.dados must conform to its RequestType.formSchema". The use case
 * delegates to this service before persisting anything.
 */
object FormSchemaValidator {
    private val mapper = ObjectMapper()
    private val factory: JsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    private val validatorsConfig = SchemaValidatorsConfig.Builder()
        .typeLoose(false)
        .build()

    /**
     * @throws SchemaValidationException with all validation errors if dados is invalid
     */
    fun validate(dados: Map<String, Any>, formSchema: Map<String, Any>) {
        val schemaNode: JsonNode = mapper.valueToTree(formSchema)
        val dataNode: JsonNode = mapper.valueToTree(dados)
        val schema = factory.getSchema(schemaNode, validatorsConfig)
        val errors = schema.validate(dataNode)
        if (errors.isNotEmpty()) {
            val messages = errors.map { it.message }
            throw SchemaValidationException(messages)
        }
    }

    /**
     * Validates the structure of a form_schema itself (must be a JSON Schema Draft-07 object).
     * Used during request type publish to prevent invalid schemas from going live.
     */
    fun validateSchemaStructure(formSchema: Map<String, Any>) {
        require(formSchema.isNotEmpty()) { "form_schema não pode ser vazio." }
        require(formSchema["type"] == "object") { "form_schema deve ter \"type\": \"object\" na raiz." }
        require(formSchema.containsKey("properties")) { "form_schema deve ter a chave \"properties\"." }
    }

    /**
     * Validates that workflow_json can be deserialized as a WorkflowDefinition.
     * Used during request type publish.
     */
    fun validateWorkflowStructure(workflowJson: Map<String, Any>, objectMapper: ObjectMapper): WorkflowDefinition {
        require(workflowJson.isNotEmpty()) { "workflow_json não pode ser vazio." }
        return try {
            val def = objectMapper.convertValue(workflowJson, WorkflowDefinition::class.java)
            require(def.initial.isNotBlank()) { "workflow_json.initial não pode ser vazio." }
            require(def.states.isNotEmpty()) { "workflow_json.states não pode ser vazio." }
            require(def.states.contains(def.initial)) {
                "workflow_json.initial '${def.initial}' não está em workflow_json.states."
            }
            def.transitions.forEach { t ->
                require(def.states.contains(t.from)) {
                    "Transição '${t.action}' referencia estado inválido 'from=${t.from}'."
                }
                require(def.states.contains(t.to)) {
                    "Transição '${t.action}' referencia estado inválido 'to=${t.to}'."
                }
                require(t.requiresAuthority.isNotEmpty()) {
                    "Transição '${t.action}' deve ter ao menos uma authority em requiresAuthority."
                }
            }
            def
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("workflow_json inválido: ${e.message}", e)
        }
    }
}

/** Thrown when the request dados payload does not conform to the RequestType form_schema. */
class SchemaValidationException(
    val errors: List<String>,
) : IllegalArgumentException("Payload inválido segundo o form_schema: ${errors.joinToString("; ")}")
