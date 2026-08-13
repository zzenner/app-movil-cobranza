package cl.zzenner.cobranza.importacion.infraestructura;

import cl.zzenner.cobranza.importacion.dominio.EstadoImportacion;
import cl.zzenner.cobranza.importacion.dominio.ImportacionMensual;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportacionMensualRepository extends JpaRepository<ImportacionMensual, UUID> {

    // Idempotencia: mismo archivo + mismo sistema ya está COMPLETADA
    Optional<ImportacionMensual> findByHashArchivoAndSistemaOrigenAndEstado(
            String hashArchivo, String sistemaOrigen, EstadoImportacion estado);

    // Para expirar validadas anteriores del mismo sistema
    List<ImportacionMensual> findBySistemaOrigenAndEstado(
            String sistemaOrigen, EstadoImportacion estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT im FROM ImportacionMensual im WHERE im.id = :id")
    Optional<ImportacionMensual> findByIdConBloqueo(@Param("id") UUID id);

    // Detectar importaciones en vuelo para evitar confirmaciones concurrentes
    @Query("""
            SELECT im FROM ImportacionMensual im
            WHERE im.sistemaOrigen = :sistemaOrigen
              AND im.estado IN ('VALIDANDO', 'PROCESANDO')
            """)
    List<ImportacionMensual> findEnProgresoByOrigen(@Param("sistemaOrigen") String sistemaOrigen);

    Page<ImportacionMensual> findByCarteraIdOrderByFechaCreacionDesc(UUID carteraId, Pageable pageable);

    Page<ImportacionMensual> findAllByOrderByFechaCreacionDesc(Pageable pageable);

    @Query("""
            SELECT im FROM ImportacionMensual im
            WHERE im.estado IN ('VALIDANDO', 'PROCESANDO')
              AND im.fechaActualizacion < :umbral
            """)
    List<ImportacionMensual> findHuerfanas(@Param("umbral") Instant umbral);

    @Query("""
            SELECT im FROM ImportacionMensual im
            WHERE im.estado = 'VALIDADA'
              AND im.fechaActualizacion < :umbral
            """)
    List<ImportacionMensual> findValidadasExpiradas(@Param("umbral") Instant umbral);
}
