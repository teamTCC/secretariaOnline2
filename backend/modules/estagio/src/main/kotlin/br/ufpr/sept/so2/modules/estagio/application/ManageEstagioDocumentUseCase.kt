package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.estagio.domain.EstagioNotFoundException
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipDocumentEntity
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipDocumentJpaRepository
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class GenerateDocUploadUrlCommand(
    val idInternship: UUID,
    val tipo: String,
    val nomeOriginal: String,
    val contentType: String,
    val idAluno: UUID,
)

data class DocUploadUrlResult(
    val uploadUrl: String,
    val storageKey: String,
)

data class RegisterDocCommand(
    val idInternship: UUID,
    val tipo: String,
    val storageKey: String,
    val sha256: String,
    val nomeOriginal: String,
    val idAluno: UUID,
)

data class RegisterDocResult(
    val id: UUID,
    val tipo: String,
    val nomeOriginal: String,
)

data class DeleteDocCommand(
    val idInternship: UUID,
    val docId: UUID,
    val idAluno: UUID,
)

@Service
@Transactional
class ManageEstagioDocumentUseCase(
    private val internshipRepo: InternshipJpaRepository,
    private val documentRepo: InternshipDocumentJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    fun generateUploadUrl(command: GenerateDocUploadUrlCommand): DocUploadUrlResult {
        val internship =
            internshipRepo.findById(command.idInternship).orElseThrow { EstagioNotFoundException(command.idInternship) }
        require(internship.idAluno == command.idAluno) { "Você não é o dono deste estágio." }
        val storageKey = "internships/${command.idInternship}/${UUID.randomUUID()}_${command.nomeOriginal}"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, command.contentType)
        return DocUploadUrlResult(uploadUrl = uploadUrl, storageKey = storageKey)
    }

    fun registerDocument(command: RegisterDocCommand): RegisterDocResult {
        val internship =
            internshipRepo.findById(command.idInternship).orElseThrow { EstagioNotFoundException(command.idInternship) }
        require(internship.idAluno == command.idAluno) { "Você não é o dono deste estágio." }
        val doc =
            InternshipDocumentEntity(
                idInternship = command.idInternship,
                tipo = command.tipo,
                storageKey = command.storageKey,
                sha256 = command.sha256,
                nomeOriginal = command.nomeOriginal,
            )
        val saved = documentRepo.save(doc)
        return RegisterDocResult(id = saved.id, tipo = saved.tipo, nomeOriginal = saved.nomeOriginal)
    }

    fun deleteDocument(command: DeleteDocCommand) {
        val internship =
            internshipRepo.findById(command.idInternship).orElseThrow { EstagioNotFoundException(command.idInternship) }
        require(internship.idAluno == command.idAluno) { "Você não é o dono deste estágio." }
        val doc =
            documentRepo.findById(command.docId).orElseThrow { NoSuchElementException("Documento não encontrado: ${command.docId}") }
        require(doc.idInternship == command.idInternship) { "Documento não pertence a este estágio." }
        minioStorageService.delete(doc.storageKey)
        documentRepo.delete(doc)
    }
}
