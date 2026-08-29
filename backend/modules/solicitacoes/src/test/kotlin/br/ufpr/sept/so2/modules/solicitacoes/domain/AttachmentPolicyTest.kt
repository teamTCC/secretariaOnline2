package br.ufpr.sept.so2.modules.solicitacoes.domain

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import java.util.UUID

class AttachmentPolicyTest :
    BehaviorSpec({

        Given("assertUploadMetadata") {
            When("content-type and size are allowed") {
                Then("should not throw") {
                    shouldNotThrowAny {
                        AttachmentPolicy.assertUploadMetadata("application/pdf", 1024)
                    }
                }
            }

            When("content-type is not in the allowlist") {
                Then("should reject") {
                    shouldThrow<IllegalArgumentException> {
                        AttachmentPolicy.assertUploadMetadata("application/x-msdownload", 1024)
                    }
                }
            }

            When("size is zero or over 20 MB") {
                Then("should reject") {
                    shouldThrow<IllegalArgumentException> {
                        AttachmentPolicy.assertUploadMetadata("application/pdf", 0)
                    }
                    shouldThrow<IllegalArgumentException> {
                        AttachmentPolicy.assertUploadMetadata("application/pdf", AttachmentPolicy.MAX_SIZE_BYTES + 1)
                    }
                }
            }
        }

        Given("sanitizeFilename") {
            When("filename contains path separators") {
                Then("should keep only the base name and strip unsafe chars") {
                    AttachmentPolicy.sanitizeFilename("../../etc/passwd.pdf") shouldBe "passwd.pdf"
                    AttachmentPolicy.sanitizeFilename("histórico escolar.pdf") shouldBe "hist_rico_escolar.pdf"
                }
            }

            When("filename is only dots or empty after sanitizing") {
                Then("should reject") {
                    shouldThrow<IllegalArgumentException> {
                        AttachmentPolicy.sanitizeFilename("..")
                    }
                }
            }
        }

        Given("storage keys") {
            val requestId = UUID.fromString("01932e8a-0000-7000-8000-000000000001")

            When("orphanStorageKey is generated") {
                Then("should use requests/orphan prefix") {
                    val key = AttachmentPolicy.orphanStorageKey("atestado.pdf")
                    key shouldStartWith "requests/orphan/"
                    key.shouldNotContain("..")
                }
            }

            When("requestStorageKey is generated") {
                Then("should be scoped to the request id") {
                    val key = AttachmentPolicy.requestStorageKey(requestId, "atestado.pdf")
                    key shouldStartWith "requests/$requestId/"
                }
            }

            When("assertStorageKeyBindable receives a matching orphan or request key") {
                Then("should accept") {
                    shouldNotThrowAny {
                        AttachmentPolicy.assertStorageKeyBindable(
                            "requests/orphan/abc_atestado.pdf",
                            requestId,
                        )
                    }
                    shouldNotThrowAny {
                        AttachmentPolicy.assertStorageKeyBindable(
                            "requests/$requestId/abc_atestado.pdf",
                            requestId,
                        )
                    }
                }
            }

            When("assertStorageKeyBindable receives another request's key") {
                Then("should reject") {
                    shouldThrow<IllegalArgumentException> {
                        AttachmentPolicy.assertStorageKeyBindable(
                            "requests/${UUID.randomUUID()}/stolen.pdf",
                            requestId,
                        )
                    }
                }
            }

            When("storageKey contains path traversal") {
                Then("should reject") {
                    shouldThrow<IllegalArgumentException> {
                        AttachmentPolicy.assertStorageKeyBindable(
                            "requests/orphan/../secret.pdf",
                            requestId,
                        )
                    }
                }
            }
        }

        Given("required attachments from form_schema") {
            val schema =
                mapOf(
                    "type" to "object",
                    "properties" to emptyMap<String, Any>(),
                    "x-required-attachments" to listOf("ATESTADO_MEDICO"),
                )

            When("the required category is present") {
                Then("should not throw") {
                    shouldNotThrowAny {
                        AttachmentPolicy.assertRequiredAttachments(schema, listOf("ATESTADO_MEDICO"))
                    }
                }
            }

            When("the required category is missing") {
                Then("should throw SchemaValidationException") {
                    val ex =
                        shouldThrow<SchemaValidationException> {
                            AttachmentPolicy.assertRequiredAttachments(schema, emptyList())
                        }
                    ex.errors.single() shouldBe "Anexo obrigatório ausente: ATESTADO_MEDICO"
                }
            }

            When("form_schema has no x-required-attachments") {
                Then("should not require anything") {
                    AttachmentPolicy.requiredCategories(mapOf("type" to "object")) shouldBe emptyList()
                }
            }
        }
    })
