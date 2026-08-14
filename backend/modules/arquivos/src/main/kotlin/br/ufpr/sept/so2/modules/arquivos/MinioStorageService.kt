package br.ufpr.sept.so2.modules.arquivos

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Service
class MinioStorageService(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket:secretaria-docs}") private val bucket: String,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        ensureBucketExists()
    }

    fun generateUploadUrl(
        storageKey: String,
        contentType: String,
        expiryMinutes: Int = 15,
    ): String =
        minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs
                .builder()
                .method(Method.PUT)
                .bucket(bucket)
                .`object`(storageKey)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .extraQueryParams(mapOf("Content-Type" to contentType))
                .build(),
        )

    fun generateDownloadUrl(
        storageKey: String,
        expiryMinutes: Int = 60,
    ): String =
        minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs
                .builder()
                .method(Method.GET)
                .bucket(bucket)
                .`object`(storageKey)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .build(),
        )

    fun upload(
        storageKey: String,
        inputStream: InputStream,
        contentType: String,
        size: Long,
    ) {
        minioClient.putObject(
            PutObjectArgs
                .builder()
                .bucket(bucket)
                .`object`(storageKey)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build(),
        )
        log.debug("Arquivo enviado para MinIO: {}", storageKey)
    }

    fun delete(storageKey: String) {
        minioClient.removeObject(
            RemoveObjectArgs
                .builder()
                .bucket(bucket)
                .`object`(storageKey)
                .build(),
        )
        log.debug("Arquivo removido do MinIO: {}", storageKey)
    }

    fun download(storageKey: String): ByteArray =
        minioClient
            .getObject(
                GetObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(storageKey)
                    .build(),
            ).use { it.readBytes() }

    fun exists(storageKey: String): Boolean =
        try {
            minioClient.statObject(
                StatObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(storageKey)
                    .build(),
            )
            true
        } catch (e: Exception) {
            false
        }

    private fun ensureBucketExists() {
        val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            log.info("Bucket MinIO criado: {}", bucket)
        }
    }
}
