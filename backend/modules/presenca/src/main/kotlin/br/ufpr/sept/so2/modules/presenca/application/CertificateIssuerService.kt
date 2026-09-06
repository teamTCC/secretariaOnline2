package br.ufpr.sept.so2.modules.presenca.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.modules.presenca.config.CertificateProperties
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.AttendanceSessionJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateEntity
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

@Service
class CertificateIssuerService(
    private val eventRepo: EventAttendanceJpaRepository,
    private val sessionRepo: AttendanceSessionJpaRepository,
    private val certRepo: CertificateJpaRepository,
    private val minioStorageService: MinioStorageService,
    private val certProperties: CertificateProperties,
    private val outboxPublisher: OutboxEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun issueCertificatesForEvent(eventoId: UUID): Int {
        val event =
            eventRepo.findById(eventoId)
                .orElseThrow { NoSuchElementException("Evento não encontrado: $eventoId") }

        val sessions = sessionRepo.findAllByIdEvento(eventoId).filter { it.entryConfirmedAt != null }

        var count = 0
        for (session in sessions) {
            val alunoId = session.idAluno
            if (certRepo.findByIdEventoAndIdAluno(eventoId, alunoId).isPresent) {
                continue
            }
            try {
                issue(
                    alunoId = alunoId,
                    titulo = event.titulo,
                    chCreditadas = event.chCreditadas,
                    origem = "EVENTO",
                    idEvento = eventoId,
                    idActivity = null,
                    storagePrefix = "certificates/eventos/$eventoId",
                )
                count++
            } catch (e: Exception) {
                log.error("Falha ao emitir certificado para aluno={} evento={}: {}", alunoId, eventoId, e.message)
            }
        }
        log.info("Emissão concluída para evento={}: {} certificados gerados", eventoId, count)
        return count
    }

    @Transactional
    fun issueFormativeCertificate(
        alunoId: UUID,
        activityId: UUID,
        titulo: String,
        chCreditadas: Double,
    ): CertificateEntity? {
        if (certRepo.findByIdActivityAndIdAluno(activityId, alunoId).isPresent) {
            return null
        }
        return issue(
            alunoId = alunoId,
            titulo = titulo,
            chCreditadas = chCreditadas,
            origem = "FORMATIVA",
            idEvento = null,
            idActivity = activityId,
            storagePrefix = "certificates/formativas/$activityId",
        )
    }

    private fun issue(
        alunoId: UUID,
        titulo: String,
        chCreditadas: Double,
        origem: String,
        idEvento: UUID?,
        idActivity: UUID?,
        storagePrefix: String,
    ): CertificateEntity {
        val issuedAt = OffsetDateTime.now()
        val pdfBytes = renderPdf(titulo, chCreditadas, issuedAt, origem)
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(pdfBytes)
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
        val signatureBase64 = signHash(hashBytes)
        val storageKey = "$storagePrefix/$alunoId.pdf"
        minioStorageService.upload(
            storageKey = storageKey,
            inputStream = pdfBytes.inputStream(),
            contentType = "application/pdf",
            size = pdfBytes.size.toLong(),
        )
        val saved =
            certRepo.save(
                CertificateEntity(
                    idAluno = alunoId,
                    idEvento = idEvento,
                    origem = origem,
                    idActivity = idActivity,
                    hashSha256 = hashHex,
                    signatureEd25519 = signatureBase64,
                    storageKey = storageKey,
                    chCreditadas = chCreditadas,
                    issuedAt = issuedAt,
                ),
            )
        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.CERTIFICATE_ISSUED,
            aggregateType = "Certificate",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "certificateId" to saved.id.toString(),
                    "alunoId" to alunoId.toString(),
                    "eventoId" to (idEvento?.toString() ?: ""),
                    "activityId" to (idActivity?.toString() ?: ""),
                    "origem" to origem,
                    "eventoTitulo" to titulo,
                    "hashSha256" to hashHex,
                ),
        )
        log.info("Certificado {} emitido aluno={} hash={}", origem, alunoId, hashHex.take(16))
        return saved
    }

    private fun signHash(hashBytes: ByteArray): String {
        val keyBytes = Base64.getDecoder().decode(certProperties.privateKey)
        val privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(privateKey)
        sig.update(hashBytes)
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    private fun renderPdf(
        titulo: String,
        chCreditadas: Double,
        issuedAt: OffsetDateTime,
        origem: String,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val doc = Document(PageSize.A4.rotate(), 50f, 50f, 40f, 40f)
        PdfWriter.getInstance(doc, out)
        doc.open()
        val titleFont = FontFactory.getFont(FontFactory.TIMES_BOLD, 26f, Color(26, 58, 92))
        val body = FontFactory.getFont(FontFactory.TIMES, 14f, Color.DARK_GRAY)
        val small = FontFactory.getFont(FontFactory.COURIER, 8f, Color.GRAY)
        val kind = if (origem == "FORMATIVA") "Certificado de Atividade Formativa" else "Certificado de Participação"
        doc.add(Paragraph("Universidade Federal do Paraná — SEPT", titleFont).also { it.alignment = Element.ALIGN_CENTER })
        doc.add(Paragraph(" "))
        doc.add(Paragraph(kind, titleFont).also { it.alignment = Element.ALIGN_CENTER })
        doc.add(Paragraph(" "))
        doc.add(Paragraph(titulo, body).also { it.alignment = Element.ALIGN_CENTER })
        doc.add(Paragraph("Carga horária: ${chCreditadas}h", body).also { it.alignment = Element.ALIGN_CENTER })
        doc.add(
            Paragraph(
                "Emitido em ${issuedAt.toLocalDate().format(DateTimeFormatter.ISO_DATE)}",
                body,
            ).also { it.alignment = Element.ALIGN_CENTER },
        )
        doc.add(Paragraph(" "))
        doc.add(
            Paragraph(
                "Documento emitido eletronicamente. Verifique em /publico/verificar-certificado/{hash}",
                small,
            ).also { it.alignment = Element.ALIGN_CENTER },
        )
        doc.close()
        return out.toByteArray()
    }
}
