package br.ufpr.sept.so2.modules.academico.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "curso",
    indexes = [
        Index(name = "idx_curso_sigla", columnList = "sigla", unique = true),
        Index(name = "idx_curso_coordenador", columnList = "id_coordenador"),
    ],
)
class CursoEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 200)
    var nome: String,
    @Column(nullable = false, unique = true, length = 20)
    var sigla: String,
    @Column(name = "id_coordenador")
    var idCoordenador: UUID? = null,
    @Column(nullable = false)
    var ativo: Boolean = true,
) : BaseEntity(id)

@Entity
@Table(
    name = "disciplina",
    uniqueConstraints = [UniqueConstraint(name = "uk_disciplina_curso_codigo", columnNames = ["id_curso", "codigo"])],
    indexes = [
        Index(name = "idx_disciplina_curso", columnList = "id_curso"),
        Index(name = "idx_disciplina_codigo", columnList = "codigo"),
    ],
)
class DisciplinaEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_curso", nullable = false)
    var idCurso: UUID,
    @Column(nullable = false, length = 20)
    var codigo: String,
    @Column(nullable = false, length = 200)
    var nome: String,
    @Column(name = "carga_horaria_total", nullable = false)
    var cargaHorariaTotal: Int,
    @Column(nullable = false)
    var creditos: Int,
    @Column(nullable = false)
    var ativa: Boolean = true,
) : BaseEntity(id)

@Entity
@Table(
    name = "periodo_letivo",
    uniqueConstraints = [UniqueConstraint(name = "uk_periodo_ano_semestre", columnNames = ["ano", "semestre"])],
)
class PeriodoLetivoEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ano: Short,
    @Column(nullable = false)
    var semestre: Short,
    @Column(nullable = false)
    var inicio: LocalDate,
    @Column(nullable = false)
    var fim: LocalDate,
    @Column(nullable = false)
    var ativo: Boolean = true,
) : BaseEntity(id)

@Entity
@Table(
    name = "calendario_academico",
    indexes = [
        Index(name = "idx_calendario_periodo", columnList = "id_periodo_letivo"),
    ],
)
class CalendarioAcademicoEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_periodo_letivo", nullable = false)
    var idPeriodoLetivo: UUID,
    @Column(name = "id_request_type")
    var idRequestType: UUID? = null,
    @Column(nullable = false, length = 300)
    var descricao: String,
    @Column(name = "prazo_inicio")
    var prazoInicio: LocalDate? = null,
    @Column(name = "prazo_fim")
    var prazoFim: LocalDate? = null,
) : BaseEntity(id)
