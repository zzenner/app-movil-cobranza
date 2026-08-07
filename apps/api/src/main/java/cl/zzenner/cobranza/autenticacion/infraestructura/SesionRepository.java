package cl.zzenner.cobranza.autenticacion.infraestructura;

import cl.zzenner.cobranza.autenticacion.dominio.SesionAutenticacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SesionRepository extends JpaRepository<SesionAutenticacion, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SesionAutenticacion s WHERE s.id = :id")
    Optional<SesionAutenticacion> findByIdWithLock(UUID id);

    @Query("SELECT s FROM SesionAutenticacion s WHERE s.usuarioId = :usuarioId AND s.dispositivoId = :dispositivoId AND s.estado = 'ACTIVA'")
    Optional<SesionAutenticacion> findActivaByUsuarioIdAndDispositivoId(UUID usuarioId, UUID dispositivoId);

    @Query("SELECT s FROM SesionAutenticacion s WHERE s.usuarioId = :usuarioId AND s.tipoCliente = 'WEB' AND s.estado = 'ACTIVA'")
    Optional<SesionAutenticacion> findActivaWebByUsuarioId(UUID usuarioId);

    @Modifying
    @Query("UPDATE SesionAutenticacion s SET s.estado = 'CERRADA', s.fechaCierre = :momento, s.motivoCierre = 'REVOCACION_ADMIN' WHERE s.usuarioId = :usuarioId AND s.estado = 'ACTIVA'")
    int cerrarActivasPorUsuario(UUID usuarioId, Instant momento);
}
