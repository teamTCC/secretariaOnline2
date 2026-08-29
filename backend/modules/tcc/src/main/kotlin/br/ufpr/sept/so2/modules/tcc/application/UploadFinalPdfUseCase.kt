package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.tcc.domain.TccNotFoundException
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccMemberJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class GenerateUploadUrlCommand(
    val idTcc: UUID,
    val nomeOriginal: String,
    val idAluno: UUID,
)

data class UploadUrlResult(
    val uploadUrl: String,
    val storageKey: String,
)

data class ConfirmUploadCommand(
    val idTcc: UUID,
    val storageKey: String,
    val sha256: String,
    val idAluno: UUID,
)

data class ConfirmUploadResult(
    val id: UUID,
    val hashSha256Pdf: String?,
)

@Service
@Transactional
class UploadFinalPdfUseCase(
    private val tccRepo: TccJpaRepository,
    private val memberRepo: TccMemberJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    fun generateUploadUrl(command: GenerateUploadUrlCommand): UploadUrlResult {
        tccRepo.findById(command.idTcc).orElseThrow { TccNotFoundException(command.idTcc) }
        assertTccMember(command.idTcc, command.idAluno)
        val storageKey = "tccs/${command.idTcc}/final_${UUID.randomUUID()}.pdf"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, "application/pdf")
        return UploadUrlResult(uploadUrl = uploadUrl, storageKey = storageKey)
    }

    fun confirmUpload(command: ConfirmUploadCommand): ConfirmUploadResult {
        val tcc = tccRepo.findById(command.idTcc).orElseThrow { TccNotFoundException(command.idTcc) }
        assertTccMember(command.idTcc, command.idAluno)
        tcc.storageKeyPdf = command.storageKey
        tcc.hashSha256Pdf = command.sha256
        tccRepo.save(tcc)
        return ConfirmUploadResult(id = tcc.id, hashSha256Pdf = tcc.hashSha256Pdf)
    }

    private fun assertTccMember(idTcc: UUID, idAluno: UUID) {
        val isMember = memberRepo.findAllByIdTcc(idTcc).any { it.idAluno == idAluno }
        if (!isMember) {
            throw AccessDeniedException("Apenas membros do TCC podem enviar o PDF final.")
        }
    }
}
