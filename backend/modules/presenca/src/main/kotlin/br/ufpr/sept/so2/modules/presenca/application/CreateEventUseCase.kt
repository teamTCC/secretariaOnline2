package br.ufpr.sept.so2.modules.presenca.application

import br.ufpr.sept.so2.modules.presenca.domain.AttendanceMode
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceEntity
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class CreateEventCommand(
    val titulo: String,
    val descricao: String?,
    val idCurso: UUID?,
    val attendanceMode: AttendanceMode,
    val chCreditadas: Double,
    val inicioEm: OffsetDateTime,
    val fimEm: OffsetDateTime,
    val idOrganizador: UUID,
)

@Service
@Transactional
class CreateEventUseCase(
    private val eventRepo: EventAttendanceJpaRepository,
) {
    fun execute(command: CreateEventCommand): UUID =
        eventRepo.save(
            EventAttendanceEntity(
                titulo = command.titulo,
                descricao = command.descricao,
                idOrganizador = command.idOrganizador,
                idCurso = command.idCurso,
                attendanceMode = command.attendanceMode.name,
                chCreditadas = command.chCreditadas,
                inicioEm = command.inicioEm,
                fimEm = command.fimEm,
            ),
        ).id
}
