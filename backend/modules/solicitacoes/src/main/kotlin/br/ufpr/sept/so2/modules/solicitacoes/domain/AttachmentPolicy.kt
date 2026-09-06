package br.ufpr.sept.so2.modules.solicitacoes.domain

import java.util.UUID

/**
 * Regras de segurança de anexos — domínio puro, sem Spring/MinIO.
 * Content-type, tamanho, prefixo de storageKey e categorias obrigatórias do form_schema.
 */
object AttachmentPolicy {
    val ALLOWED_CONTENT_TYPES: Set<String> =
        setOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )

    const val MAX_SIZE_BYTES: Long = 20L * 1024 * 1024

    val MODIFIABLE_STATES: Set<String> = setOf("RASCUNHO", "ABERTA", "EM_AJUSTE")

    private const val ORPHAN_PREFIX = "requests/orphan/"

    fun assertUploadMetadata(
        contentType: String,
        sizeBytes: Long,
    ) {
        require(contentType in ALLOWED_CONTENT_TYPES) {
            "Tipo de conteúdo '$contentType' não é permitido. Tipos aceitos: ${ALLOWED_CONTENT_TYPES.joinToString()}."
        }
        require(sizeBytes in 1..MAX_SIZE_BYTES) {
            "Tamanho do arquivo inválido (recebido: $sizeBytes bytes, máximo: $MAX_SIZE_BYTES bytes)."
        }
    }

    fun sanitizeFilename(filename: String): String {
        val base = filename.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        require(cleaned.isNotBlank() && cleaned != "." && cleaned != "..") {
            "Nome de arquivo inválido."
        }
        return cleaned.take(200)
    }

    fun orphanStorageKey(filename: String): String =
        "$ORPHAN_PREFIX${UUID.randomUUID()}_${sanitizeFilename(filename)}"

    fun requestStorageKey(
        requestId: UUID,
        filename: String,
    ): String = "requests/$requestId/${UUID.randomUUID()}_${sanitizeFilename(filename)}"

    /**
     * Wizard gera chave órfã antes de existir o request; após save/open a chave
     * pode ser `requests/{id}/…` (upload vinculado) ou `requests/orphan/…`.
     */
    fun assertStorageKeyBindable(
        storageKey: String,
        requestId: UUID,
    ) {
        require(!storageKey.contains("..")) { "storageKey inválido." }
        val requestPrefix = "requests/$requestId/"
        require(storageKey.startsWith(ORPHAN_PREFIX) || storageKey.startsWith(requestPrefix)) {
            "storageKey inválido: deve pertencer a esta solicitação ou ser um upload órfão do wizard."
        }
    }

    fun assertOrphanStorageKey(storageKey: String) {
        require(!storageKey.contains("..")) { "storageKey inválido." }
        require(storageKey.startsWith(ORPHAN_PREFIX)) {
            "storageKey inválido: upload sem solicitação deve usar o prefixo $ORPHAN_PREFIX."
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun requiredCategories(formSchema: Map<String, Any>): List<String> {
        val raw = formSchema["x-required-attachments"] ?: return emptyList()
        return when (raw) {
            is List<*> -> raw.mapNotNull { it as? String }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    fun assertRequiredAttachments(
        formSchema: Map<String, Any>,
        presentCategories: Collection<String>,
    ) {
        val missing = requiredCategories(formSchema).filter { it !in presentCategories }
        if (missing.isNotEmpty()) {
            throw SchemaValidationException(missing.map { "Anexo obrigatório ausente: $it" })
        }
    }
}
