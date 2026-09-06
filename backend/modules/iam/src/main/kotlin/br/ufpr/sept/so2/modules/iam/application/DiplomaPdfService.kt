package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import org.springframework.stereotype.Service
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.format.DateTimeFormatter

@Service
class DiplomaPdfService(
    private val minio: MinioStorageService,
    private val usuarioRepo: UsuarioJpaRepository,
) {
    fun generateAndStore(rec: GraduationRecordEntity): Pair<String, String> {
        val aluno = usuarioRepo.findById(rec.idAluno).orElseThrow { NoSuchElementException("Aluno ${rec.idAluno}") }
        val bytes = renderPdf(aluno.nome, aluno.grr, rec)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val key = "diplomas/${rec.id}.pdf"
        minio.upload(key, bytes.inputStream(), "application/pdf", bytes.size.toLong())
        rec.diplomaStorageKey = key
        rec.diplomaHashSha256 = hash
        return key to hash
    }

    private fun renderPdf(
        nome: String,
        grr: String?,
        rec: GraduationRecordEntity,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val doc = Document(PageSize.A4.rotate(), 60f, 60f, 50f, 50f)
        PdfWriter.getInstance(doc, out)
        doc.open()
        val titleFont = FontFactory.getFont(FontFactory.TIMES_BOLD, 28f, Color(26, 58, 92))
        val bodyFont = FontFactory.getFont(FontFactory.TIMES, 14f, Color.DARK_GRAY)
        val small = FontFactory.getFont(FontFactory.COURIER, 9f, Color.GRAY)
        doc.add(Paragraph("Universidade Federal do Paraná — SEPT", titleFont).also { it.alignment = Element.ALIGN_CENTER })
        doc.add(Paragraph(" "))
        doc.add(Paragraph("DIPLOMA", titleFont).also { it.alignment = Element.ALIGN_CENTER })
        doc.add(Paragraph(" "))
        doc.add(
            Paragraph(
                "Certificamos que $nome (GRR ${grr ?: "—"}) colou grau em ${
                    rec.dataColacao?.format(DateTimeFormatter.ISO_DATE) ?: "—"
                }.",
                bodyFont,
            ).also { it.alignment = Element.ALIGN_CENTER },
        )
        if (rec.livro != null || rec.folha != null || rec.ata != null) {
            doc.add(
                Paragraph(
                    "Livro ${rec.livro ?: "—"}  Folha ${rec.folha ?: "—"}  Ata ${rec.ata ?: "—"}",
                    bodyFont,
                ).also { it.alignment = Element.ALIGN_CENTER },
            )
        }
        doc.add(Paragraph(" "))
        doc.add(Paragraph("Documento gerado eletronicamente. Registro ${rec.id}", small).also { it.alignment = Element.ALIGN_CENTER })
        doc.close()
        return out.toByteArray()
    }
}
