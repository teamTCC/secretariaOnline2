package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class DeliveryConfirmedResult(
    val id: UUID,
    val estado: String,
    val deliveredAt: OffsetDateTime?,
)

@Service
@Transactional
class ConfirmDiplomaDeliveryUseCase(
    private val graduationRepo: GraduationRecordJpaRepository,
) {
    fun execute(graduationId: UUID, confirmedById: UUID): DeliveryConfirmedResult {
        val rec =
            graduationRepo.findById(graduationId)
                .orElseThrow { NoSuchElementException("Colação não encontrada: $graduationId") }
        if (rec.estado == "DIPLOMA_ENTREGUE") {
            throw AccessDeniedException("Diploma já entregue para colação $graduationId")
        }
        rec.estado = "DIPLOMA_ENTREGUE"
        rec.deliveredAt = OffsetDateTime.now()
        rec.deliveredBy = confirmedById
        graduationRepo.save(rec)
        return DeliveryConfirmedResult(id = rec.id, estado = rec.estado, deliveredAt = rec.deliveredAt)
    }
}
