package br.ufpr.sept.so2.modules.solicitacoes.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.OffsetDateTime
import java.util.UUID

class WorkflowEngineTest :
    BehaviorSpec({

        val definition =
            WorkflowDefinition(
                initial = "ABERTA",
                states = listOf("ABERTA", "EM_TRIAGEM", "EM_DELIBERACAO", "DEFERIDA", "INDEFERIDA", "EM_AJUSTE"),
                transitions =
                    listOf(
                        WorkflowDefinition.Transition(
                            from = "ABERTA",
                            to = "EM_TRIAGEM",
                            action = "ASSIGN",
                            requiresAuthority = listOf("request.deliberate"),
                        ),
                        WorkflowDefinition.Transition(
                            from = "EM_TRIAGEM",
                            to = "EM_DELIBERACAO",
                            action = "FORWARD_TO_DELIBERATOR",
                            requiresAuthority = listOf("request.deliberate"),
                        ),
                        WorkflowDefinition.Transition(
                            from = "EM_DELIBERACAO",
                            to = "DEFERIDA",
                            action = "DEFER",
                            requiresAuthority = listOf("request.deliberate"),
                        ),
                        WorkflowDefinition.Transition(
                            from = "EM_DELIBERACAO",
                            to = "INDEFERIDA",
                            action = "DENY",
                            requiresAuthority = listOf("request.deliberate"),
                        ),
                        WorkflowDefinition.Transition(
                            from = "EM_DELIBERACAO",
                            to = "EM_AJUSTE",
                            action = "REQUEST_ADJUSTMENT",
                            requiresAuthority = listOf("request.deliberate"),
                        ),
                        WorkflowDefinition.Transition(
                            from = "EM_AJUSTE",
                            to = "ABERTA",
                            action = "RESUBMIT",
                            requiresAuthority = listOf("request.open"),
                            guard = "actor.id == request.idSolicitante",
                        ),
                    ),
            )

        val engine = WorkflowEngine(definition)
        val secretariaAuthorities = setOf("request.deliberate", "request.view_curso")
        val alunoAuthorities = setOf("request.open", "request.view_own")
        val actorId = UUID.randomUUID()

        fun buildRequest(
            estado: RequestState,
            idSolicitante: UUID = actorId,
        ) = Request(
            id = UUID.randomUUID(),
            numeroAnual = 1,
            ano = 2026.toShort(),
            idRequestType = UUID.randomUUID(),
            requestTypeCode = "SEGUNDA_CHAMADA",
            idSolicitante = idSolicitante,
            idCurso = UUID.randomUUID(),
            estado = estado,
            dados = emptyMap(),
            parecer = null,
            prazoEm = OffsetDateTime.now().plusDays(5),
            concludedAt = null,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )

        Given("a WorkflowEngine with standard definition") {

            When("secretaria applies ASSIGN transition from ABERTA") {
                val request = buildRequest(RequestState.ABERTA)
                val result = engine.applyTransition(request, "ASSIGN", actorId, secretariaAuthorities, null)

                Then("the new state should be EM_TRIAGEM") {
                    result.newState shouldBe RequestState.EM_TRIAGEM
                }

                Then("the event type should match the action") {
                    result.event.tipo shouldBe "ASSIGN"
                }

                Then("the event records previous and new states") {
                    result.event.estadoAnterior shouldBe RequestState.ABERTA
                    result.event.estadoNovo shouldBe RequestState.EM_TRIAGEM
                }
            }

            When("aluno tries ASSIGN — not allowed") {
                val request = buildRequest(RequestState.ABERTA)

                Then("should throw InsufficientAuthorityException") {
                    shouldThrow<InsufficientAuthorityException> {
                        engine.applyTransition(request, "ASSIGN", actorId, alunoAuthorities, null)
                    }
                }
            }

            When("secretaria tries DEFER from ABERTA — invalid transition") {
                val request = buildRequest(RequestState.ABERTA)

                Then("should throw InvalidTransitionException") {
                    shouldThrow<InvalidTransitionException> {
                        engine.applyTransition(request, "DEFER", actorId, secretariaAuthorities, null)
                    }
                }
            }

            When("aluno resubmits as the original requester") {
                val alunoId = UUID.randomUUID()
                val request = buildRequest(RequestState.EM_AJUSTE, idSolicitante = alunoId)
                val result = engine.applyTransition(request, "RESUBMIT", alunoId, alunoAuthorities, null)

                Then("state should go back to ABERTA") {
                    result.newState shouldBe RequestState.ABERTA
                }
            }

            When("different user tries to resubmit with guard protection") {
                val originalAluno = UUID.randomUUID()
                val differentUser = UUID.randomUUID()
                val request = buildRequest(RequestState.EM_AJUSTE, idSolicitante = originalAluno)

                Then("should throw TransitionGuardFailedException") {
                    shouldThrow<TransitionGuardFailedException> {
                        engine.applyTransition(request, "RESUBMIT", differentUser, alunoAuthorities, null)
                    }
                }
            }

            When("checking allowed transitions in EM_DELIBERACAO for secretaria") {
                val allowed = engine.allowedTransitions(RequestState.EM_DELIBERACAO, secretariaAuthorities)

                Then("should include DEFER, DENY, REQUEST_ADJUSTMENT") {
                    allowed.map { it.action } shouldBe listOf("DEFER", "DENY", "REQUEST_ADJUSTMENT")
                }
            }

            When("checking allowed transitions in EM_DELIBERACAO for aluno") {
                val allowed = engine.allowedTransitions(RequestState.EM_DELIBERACAO, alunoAuthorities)

                Then("should return empty list — aluno cannot deliberate") {
                    allowed shouldBe emptyList()
                }
            }

            When("final state DEFERIDA is reached") {
                val result =
                    buildRequest(RequestState.EM_DELIBERACAO).let { req ->
                        engine.applyTransition(req, "DEFER", actorId, secretariaAuthorities, "Aprovado conforme documentação")
                    }

                Then("new state is DEFERIDA (final)") {
                    result.newState shouldBe RequestState.DEFERIDA
                    result.newState.isFinal() shouldBe true
                }
            }

            When("checking notifyTemplate propagation on a transition that has one") {
                val defWithTemplate = WorkflowDefinition(
                    initial = "ABERTA",
                    states = listOf("ABERTA", "EM_AJUSTE"),
                    transitions = listOf(
                        WorkflowDefinition.Transition(
                            from = "ABERTA",
                            to = "EM_AJUSTE",
                            action = "REQUEST_ADJUSTMENT",
                            requiresAuthority = listOf("request.deliberate"),
                            notifyTemplate = "solicitacoes.pedido_ajuste",
                            generateOneTimeToken = true,
                        ),
                    ),
                )
                val engineWithTemplate = WorkflowEngine(defWithTemplate)
                val request = buildRequest(RequestState.ABERTA)
                val result =
                    engineWithTemplate.applyTransition(
                        request,
                        "REQUEST_ADJUSTMENT",
                        actorId,
                        secretariaAuthorities,
                        "Pendência na documentação",
                    )

                Then("notifyTemplate should be propagated in the result") {
                    result.notifyTemplate shouldBe "solicitacoes.pedido_ajuste"
                }

                Then("generateOneTimeToken should be true in the result") {
                    result.generateOneTimeToken shouldBe true
                }
            }

            When("actor has multiple authorities and one of them matches") {
                val multiAuthorities = setOf("request.view_own", "request.deliberate", "request.open")
                val allowed = engine.allowedTransitions(RequestState.EM_DELIBERACAO, multiAuthorities)

                Then("should include transitions requiring any of the matching authorities") {
                    allowed.map { it.action } shouldBe listOf("DEFER", "DENY", "REQUEST_ADJUSTMENT")
                }
            }

            When("checking allowed transitions from a final state") {
                val allowed = engine.allowedTransitions(RequestState.DEFERIDA, secretariaAuthorities)

                Then("should return empty — no transitions from terminal states") {
                    allowed shouldBe emptyList()
                }
            }
        }

        Given("RequestState isFinal/isActive contract") {

            Then("DEFERIDA, INDEFERIDA, ARQUIVADA are final") {
                RequestState.DEFERIDA.isFinal() shouldBe true
                RequestState.INDEFERIDA.isFinal() shouldBe true
                RequestState.ARQUIVADA.isFinal() shouldBe true
            }

            Then("RASCUNHO, ABERTA, EM_TRIAGEM, EM_DELIBERACAO, EM_AJUSTE, EM_REVISAO are active") {
                RequestState.RASCUNHO.isActive() shouldBe true
                RequestState.ABERTA.isActive() shouldBe true
                RequestState.EM_TRIAGEM.isActive() shouldBe true
                RequestState.EM_DELIBERACAO.isActive() shouldBe true
                RequestState.EM_AJUSTE.isActive() shouldBe true
                RequestState.EM_REVISAO.isActive() shouldBe true
            }
        }

        Given("Request.allowsReview()") {
            val recentlyIndeferida = Request(
                id = UUID.randomUUID(),
                numeroAnual = 1,
                ano = 2026.toShort(),
                idRequestType = UUID.randomUUID(),
                requestTypeCode = "SEGUNDA_CHAMADA",
                idSolicitante = UUID.randomUUID(),
                idCurso = UUID.randomUUID(),
                estado = RequestState.INDEFERIDA,
                dados = emptyMap(),
                parecer = "Documentação insuficiente",
                prazoEm = null,
                concludedAt = OffsetDateTime.now().minusDays(2),
                createdAt = OffsetDateTime.now().minusDays(10),
                updatedAt = OffsetDateTime.now().minusDays(2),
            )

            When("request is INDEFERIDA and concludedAt within 5 days") {
                Then("allowsReview() returns true") {
                    recentlyIndeferida.allowsReview() shouldBe true
                }
            }

            When("request is INDEFERIDA but concludedAt is more than 5 days ago") {
                val staleRequest = recentlyIndeferida.copy(concludedAt = OffsetDateTime.now().minusDays(6))
                Then("allowsReview() returns false") {
                    staleRequest.allowsReview() shouldBe false
                }
            }

            When("request is DEFERIDA (not indeferida)") {
                val deferida = recentlyIndeferida.copy(estado = RequestState.DEFERIDA)
                Then("allowsReview() returns false regardless of concludedAt") {
                    deferida.allowsReview() shouldBe false
                }
            }

            When("request has concludedAt = null") {
                val noConcluded = recentlyIndeferida.copy(concludedAt = null)
                Then("allowsReview() returns false") {
                    noConcluded.allowsReview() shouldBe false
                }
            }
        }
    })
