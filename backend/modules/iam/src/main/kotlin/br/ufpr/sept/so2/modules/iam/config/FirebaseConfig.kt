package br.ufpr.sept.so2.modules.iam.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${app.firebase.service-account-json:}")
    private lateinit var serviceAccountJson: String

    @Value("\${app.firebase.project-id:}")
    private lateinit var projectId: String

    @PostConstruct
    fun initialize() {
        if (serviceAccountJson.isBlank()) {
            log.warn(
                "Firebase não configurado — push notifications desabilitadas. " +
                    "Configure APP_FIREBASE_SERVICE_ACCOUNT_JSON para habilitar.",
            )
            return
        }

        if (FirebaseApp.getApps().isNotEmpty()) {
            log.debug("FirebaseApp já inicializado.")
            return
        }

        try {
            val credentials = GoogleCredentials.fromStream(serviceAccountJson.byteInputStream())
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId.ifBlank { null })
                .build()
            FirebaseApp.initializeApp(options)
            log.info("Firebase Admin SDK inicializado com sucesso.")
        } catch (e: Exception) {
            log.error("Falha ao inicializar Firebase Admin SDK: {}", e.message)
        }
    }
}
