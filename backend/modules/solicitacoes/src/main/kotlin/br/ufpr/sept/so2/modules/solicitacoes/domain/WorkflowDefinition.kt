package br.ufpr.sept.so2.modules.solicitacoes.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowDefinition(
    val initial: String,
    val states: List<String>,
    val transitions: List<Transition>,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Transition(
        val from: String,
        val to: String,
        val action: String,
        val requiresAuthority: List<String> = emptyList(),
        val guard: String? = null,
        val notifyTemplate: String? = null,
        val generateOneTimeToken: Boolean = false,
    )
}

enum class RequestState {
    RASCUNHO,
    ABERTA,
    EM_TRIAGEM,
    EM_DELIBERACAO,
    EM_AJUSTE,
    DEFERIDA,
    INDEFERIDA,
    EM_REVISAO,
    ARQUIVADA,
    ;

    fun isFinal(): Boolean = this == DEFERIDA || this == INDEFERIDA || this == ARQUIVADA

    fun isActive(): Boolean = !isFinal()
}

data class RequestTransitionResult(
    val newState: RequestState,
    val event: RequestEvent,
    val notifyTemplate: String?,
    val generateOneTimeToken: Boolean,
)
