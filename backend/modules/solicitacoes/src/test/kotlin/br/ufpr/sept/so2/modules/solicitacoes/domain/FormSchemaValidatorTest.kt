package br.ufpr.sept.so2.modules.solicitacoes.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class FormSchemaValidatorTest :
    BehaviorSpec({

        // Kotlin data classes (WorkflowDefinition) need jackson-module-kotlin — same as Spring's ObjectMapper.
        val jackson = jacksonObjectMapper()

        val simpleStringSchema =
            mapOf(
                "type" to "object",
                "properties" to mapOf("nome" to mapOf("type" to "string")),
                "required" to listOf("nome"),
            )

        val multiFieldSchema =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "finalidade" to mapOf("type" to "string", "enum" to listOf("BOLSA", "CONVENIO", "OUTRO")),
                        "observacoes" to mapOf("type" to "string"),
                        "semestre" to mapOf("type" to "integer", "minimum" to 1, "maximum" to 12),
                    ),
                "required" to listOf("finalidade"),
            )

        Given("FormSchemaValidator.validate — simple schema") {

            When("dados has all required fields with correct types") {
                Then("should not throw") {
                    shouldNotThrowAny {
                        FormSchemaValidator.validate(mapOf("nome" to "Maria Oliveira"), simpleStringSchema)
                    }
                }
            }

            When("dados has extra fields not in schema (additionalProperties not set)") {
                Then("should not throw — extra properties are allowed by default") {
                    shouldNotThrowAny {
                        FormSchemaValidator.validate(
                            mapOf("nome" to "João", "extra" to "value"),
                            simpleStringSchema,
                        )
                    }
                }
            }

            When("dados is missing required field 'nome'") {
                Then("should throw SchemaValidationException with non-empty error list") {
                    val ex =
                        shouldThrow<SchemaValidationException> {
                            FormSchemaValidator.validate(emptyMap(), simpleStringSchema)
                        }
                    ex.errors.shouldNotBeEmpty()
                }
            }

            When("dados has 'nome' with wrong type (integer instead of string)") {
                Then("should throw SchemaValidationException") {
                    shouldThrow<SchemaValidationException> {
                        FormSchemaValidator.validate(mapOf("nome" to 42), simpleStringSchema)
                    }
                }
            }
        }

        Given("FormSchemaValidator.validate — multi-field schema with enum and integer") {

            When("valid data with finalidade in enum and valid semestre") {
                Then("should not throw") {
                    shouldNotThrowAny {
                        FormSchemaValidator.validate(
                            mapOf("finalidade" to "BOLSA", "semestre" to 1),
                            multiFieldSchema,
                        )
                    }
                }
            }

            When("finalidade is not in enum values") {
                Then("should throw SchemaValidationException") {
                    shouldThrow<SchemaValidationException> {
                        FormSchemaValidator.validate(
                            mapOf("finalidade" to "INVALIDO"),
                            multiFieldSchema,
                        )
                    }
                }
            }

            When("semestre violates minimum constraint") {
                Then("should throw SchemaValidationException") {
                    shouldThrow<SchemaValidationException> {
                        FormSchemaValidator.validate(
                            mapOf("finalidade" to "BOLSA", "semestre" to 0),
                            multiFieldSchema,
                        )
                    }
                }
            }

            When("semestre violates maximum constraint") {
                Then("should throw SchemaValidationException") {
                    shouldThrow<SchemaValidationException> {
                        FormSchemaValidator.validate(
                            mapOf("finalidade" to "BOLSA", "semestre" to 13),
                            multiFieldSchema,
                        )
                    }
                }
            }
        }

        Given("FormSchemaValidator.validateSchemaStructure") {

            When("valid schema with type=object and properties") {
                Then("should not throw") {
                    shouldNotThrowAny { FormSchemaValidator.validateSchemaStructure(simpleStringSchema) }
                }
            }

            When("empty schema map") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateSchemaStructure(emptyMap())
                    }
                }
            }

            When("schema with type != object") {
                Then("should throw — root must be object type") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateSchemaStructure(
                            mapOf("type" to "array", "properties" to emptyMap<String, Any>()),
                        )
                    }
                }
            }

            When("schema missing 'properties' key") {
                Then("should throw") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateSchemaStructure(mapOf("type" to "object"))
                    }
                }
            }
        }

        Given("FormSchemaValidator.validateWorkflowStructure") {

            val validWorkflow =
                mapOf(
                    "initial" to "ABERTA",
                    "states" to listOf("ABERTA", "DEFERIDA"),
                    "transitions" to
                        listOf(
                            mapOf(
                                "from" to "ABERTA",
                                "to" to "DEFERIDA",
                                "action" to "DEFER",
                                "requiresAuthority" to listOf("request.deliberate"),
                            ),
                        ),
                )

            When("valid workflow definition") {
                Then("should return a WorkflowDefinition with correct fields") {
                    val def = FormSchemaValidator.validateWorkflowStructure(validWorkflow, jackson)
                    def.initial shouldBe "ABERTA"
                    def.states.size shouldBe 2
                    def.transitions.size shouldBe 1
                }
            }

            When("workflow with 'initial' not in 'states'") {
                val bad = validWorkflow.toMutableMap().apply { put("initial", "NAO_EXISTE") }
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateWorkflowStructure(bad, jackson)
                    }
                }
            }

            When("transition references 'to' state not in states list") {
                val bad =
                    mapOf(
                        "initial" to "ABERTA",
                        "states" to listOf("ABERTA"),
                        "transitions" to
                            listOf(
                                mapOf(
                                    "from" to "ABERTA",
                                    "to" to "INEXISTENTE",
                                    "action" to "GO",
                                    "requiresAuthority" to listOf("request.deliberate"),
                                ),
                            ),
                    )
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateWorkflowStructure(bad, jackson)
                    }
                }
            }

            When("transition references 'from' state not in states list") {
                val bad =
                    mapOf(
                        "initial" to "ABERTA",
                        "states" to listOf("ABERTA", "DEFERIDA"),
                        "transitions" to
                            listOf(
                                mapOf(
                                    "from" to "INEXISTENTE",
                                    "to" to "DEFERIDA",
                                    "action" to "GO",
                                    "requiresAuthority" to listOf("request.deliberate"),
                                ),
                            ),
                    )
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateWorkflowStructure(bad, jackson)
                    }
                }
            }

            When("transition has empty requiresAuthority list") {
                val bad =
                    mapOf(
                        "initial" to "ABERTA",
                        "states" to listOf("ABERTA", "DEFERIDA"),
                        "transitions" to
                            listOf(
                                mapOf(
                                    "from" to "ABERTA",
                                    "to" to "DEFERIDA",
                                    "action" to "DEFER",
                                    "requiresAuthority" to emptyList<String>(),
                                ),
                            ),
                    )
                Then("should throw — every transition must have at least one authority") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateWorkflowStructure(bad, jackson)
                    }
                }
            }

            When("empty workflow map") {
                Then("should throw") {
                    shouldThrow<IllegalArgumentException> {
                        FormSchemaValidator.validateWorkflowStructure(emptyMap(), jackson)
                    }
                }
            }
        }
    })
