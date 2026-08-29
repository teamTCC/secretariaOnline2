package br.ufpr.sept.so2.modules.formativas.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import org.springframework.stereotype.Service
import java.util.UUID

data class GenerateComprovanteUploadUrlCommand(
    val filename: String,
    val contentType: String,
)

data class GenerateComprovanteUploadUrlResult(
    val uploadUrl: String,
    val storageKey: String,
)

@Service
class GenerateComprovanteUploadUrlUseCase(
    private val minioStorageService: MinioStorageService,
) {
    fun execute(command: GenerateComprovanteUploadUrlCommand): GenerateComprovanteUploadUrlResult {
        val storageKey = "formativas/orphan/${UUID.randomUUID()}_${command.filename}"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, command.contentType, expiryMinutes = 30)
        return GenerateComprovanteUploadUrlResult(uploadUrl = uploadUrl, storageKey = storageKey)
    }
}
