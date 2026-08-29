package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.modules.iam.application.ports.out.ColacaoAnoCount
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamBffReadPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamDashboardPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamSearchHit
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioBasicoDto
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioExportRow
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IamDashboardAdapter(
    private val usuarioRepo: UsuarioJpaRepository,
    private val serviceRecordRepo: ServiceRecordJpaRepository,
    private val graduationRepo: GraduationRecordJpaRepository,
) : IamDashboardPort,
    IamBffReadPort {
    override fun countAtendimentosPendentes(alunoId: UUID): Long =
        serviceRecordRepo.countByIdAlunoAndEstado(alunoId, "PENDENTE_CIENCIA")

    override fun findUsuarioBasico(id: UUID): UsuarioBasicoDto? =
        usuarioRepo.findById(id).map { u ->
            UsuarioBasicoDto(nome = u.nome, email = u.email, grr = u.grr)
        }.orElse(null)

    override fun countAlunosAtivos(): Long =
        usuarioRepo.countByAtivoTrueAndGrrIsNotNull()

    override fun findUserCourseId(userId: UUID): UUID? =
        usuarioRepo.findById(userId).orElse(null)
            ?.metadata
            ?.get("idCurso")
            ?.toString()
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    override fun countByRoleCode(roleCode: String): Long =
        usuarioRepo.countByRoleCode(roleCode)

    override fun findNome(id: UUID): String? =
        usuarioRepo.findById(id).map { it.nome }.orElse(null)

    override fun search(
        q: String,
        page: Int,
        size: Int,
    ): List<IamSearchHit> =
        usuarioRepo.searchByQ(q, PageRequest.of(page, size)).content.map { u ->
            IamSearchHit(id = u.id, title = u.nome, subtitle = u.email)
        }

    override fun countColacoesByAno(): List<ColacaoAnoCount> =
        graduationRepo.countByAnoColacao().map { row ->
            ColacaoAnoCount(ano = (row[0] as Number).toInt(), colacoes = (row[1] as Number).toLong())
        }

    override fun listAlunosExport(limit: Int): List<UsuarioExportRow> =
        usuarioRepo.searchUsuarios(null, null, true, PageRequest.of(0, limit)).content.map { it.toExport() }

    override fun listByRoleExport(
        roleCode: String,
        limit: Int,
    ): List<UsuarioExportRow> =
        usuarioRepo.findAllByRoleCode(roleCode, PageRequest.of(0, limit)).content.map { it.toExport() }

    private fun UsuarioEntity.toExport() =
        UsuarioExportRow(id = id, nome = nome, email = email, grr = grr)
}
