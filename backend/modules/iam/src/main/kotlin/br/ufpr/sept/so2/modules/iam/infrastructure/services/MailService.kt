package br.ufpr.sept.so2.modules.iam.infrastructure.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class MailService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.from:noreply@ufpr.br}") private val fromAddress: String,
    @Value("\${app.base-url:http://localhost:3000}") private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendPasswordResetEmail(
        to: String,
        nome: String,
        token: String,
    ) {
        val link = "$baseUrl/nova-senha?token=$token"
        val html =
            """
            <html><body>
            <h2>Redefinição de senha — SecretariaOnline</h2>
            <p>Olá, <strong>$nome</strong>!</p>
            <p>Clique no link abaixo para redefinir sua senha. O link é válido por <strong>24 horas</strong>:</p>
            <p><a href="$link">$link</a></p>
            <p>Se você não solicitou a redefinição, ignore este e-mail. Sua senha permanece a mesma.</p>
            <br><p>— Equipe SecretariaOnline UFPR</p>
            </body></html>
            """.trimIndent()

        sendHtml(to = to, subject = "Redefinição de senha — SecretariaOnline", html = html)
    }

    fun sendWelcomeEmail(
        to: String,
        nome: String,
    ) {
        val html =
            """
            <html><body>
            <h2>Bem-vindo ao SecretariaOnline!</h2>
            <p>Olá, <strong>$nome</strong>!</p>
            <p>Sua conta foi criada. Por favor, acesse o sistema e defina sua senha no primeiro acesso.</p>
            <p><a href="$baseUrl/login">Acessar o sistema</a></p>
            </body></html>
            """.trimIndent()

        sendHtml(to = to, subject = "Bem-vindo ao SecretariaOnline — UFPR", html = html)
    }

    fun sendNotificationEmail(
        to: String,
        subject: String,
        html: String,
    ) {
        sendHtml(to = to, subject = subject, html = html)
    }

    private fun sendHtml(
        to: String,
        subject: String,
        html: String,
    ) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setFrom(fromAddress)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(html, true)
            mailSender.send(message)
            log.debug("Email enviado para {}: {}", to, subject)
        } catch (e: Exception) {
            log.error("Falha ao enviar email para {}: {}", to, e.message)
            throw e
        }
    }
}
