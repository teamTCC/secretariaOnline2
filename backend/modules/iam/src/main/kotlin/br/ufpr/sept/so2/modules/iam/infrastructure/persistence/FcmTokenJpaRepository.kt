package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface FcmTokenJpaRepository : JpaRepository<FcmTokenEntity, UUID> {
    fun findAllByIdUsuarioAndAtivo(
        idUsuario: UUID,
        ativo: Boolean,
    ): List<FcmTokenEntity>

    fun findByIdUsuarioAndFcmToken(
        idUsuario: UUID,
        fcmToken: String,
    ): Optional<FcmTokenEntity>

    @Modifying
    @Query("UPDATE FcmTokenEntity f SET f.ativo = false WHERE f.idUsuario = :idUsuario AND f.fcmToken != :keepToken")
    fun deactivateOthers(
        @Param("idUsuario") idUsuario: UUID,
        @Param("keepToken") keepToken: String,
    )
}
