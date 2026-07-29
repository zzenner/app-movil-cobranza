package cl.zzenner.cobranza.autenticacion.infraestructura;

import cl.zzenner.cobranza.autenticacion.dominio.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.hashToken = :hash")
    Optional<RefreshToken> findByHashTokenWithLock(String hash);

    Optional<RefreshToken> findByHashToken(String hash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.estado = 'REVOCADO', t.fechaRevocacion = :momento WHERE t.sesionId = :sesionId AND t.estado = 'ACTIVO'")
    int revocarActivosDeSesion(UUID sesionId, Instant momento);

    @Modifying
    @Query("""
            UPDATE RefreshToken t SET t.estado = 'REVOCADO', t.fechaRevocacion = :momento
            WHERE t.sesionId IN (
                SELECT s.id FROM SesionAutenticacion s WHERE s.usuarioId = :usuarioId AND s.estado != 'CERRADA'
            ) AND t.estado IN ('ACTIVO', 'CONSUMIDO')
            """)
    int revocarTodosDeUsuario(UUID usuarioId, Instant momento);
}
