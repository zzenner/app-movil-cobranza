package cl.zzenner.cobranza.importacion.infraestructura;

import cl.zzenner.cobranza.importacion.dominio.EstadoImportacion;
import cl.zzenner.cobranza.importacion.dominio.ImportacionMensual;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportacionMensualRepository extends JpaRepository<ImportacionMensual, UUID> {

    Optional<ImportacionMensual> findByHashArchivoAndPeriodoAndCarteraIdAndSistemaOrigenAndEstado(
            String hashArchivo, String periodo, UUID carteraId, String sistemaOrigen, EstadoImportacion estado);

    List<ImportacionMensual> findByPeriodoAndCarteraIdAndSistemaOrigenAndEstado(
            String periodo, UUID carteraId, String sistemaOrigen, EstadoImportacion estado);

    @Query("""
            SELECT MAX(im.periodo)
            FROM ImportacionMensual im
            WHERE im.carteraId = :carteraId
              AND im.sistemaOrigen = :sistemaOrigen
              AND im.estado = 'COMPLETADA'
            """)
    Optional<String> findMaxPeriodoCompletado(@Param("carteraId") UUID carteraId,
                                               @Param("sistemaOrigen") String sistemaOrigen);

    @Query("""
            SELECT im FROM ImportacionMensual im
            WHERE im.carteraId = :carteraId
              AND im.periodo = :periodo
              AND im.sistemaOrigen = :sistemaOrigen
              AND im.estado IN ('VALIDANDO', 'PROCESANDO')
            """)
    List<ImportacionMensual> findEnProgreso(@Param("carteraId") UUID carteraId,
                                             @Param("periodo") String periodo,
                                             @Param("sistemaOrigen") String sistemaOrigen);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT im FROM ImportacionMensual im WHERE im.id = :id")
    Optional<ImportacionMensual> findByIdConBloqueo(@Param("id") UUID id);

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

    boolean existsByPeriodoAndCarteraIdAndSistemaOrigenAndEstado(
            String periodo, UUID carteraId, String sistemaOrigen, EstadoImportacion estado);
}
