package cl.zzenner.cobranza.asignaciones.infraestructura;

import cl.zzenner.cobranza.asignaciones.dominio.AsignacionMensualPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsignacionMensualPersonaRepository
        extends JpaRepository<AsignacionMensualPersona, UUID> {

    List<AsignacionMensualPersona> findAllByAsignacionMensualIdAndActivaTrue(UUID asignacionMensualId);

    boolean existsByPersonaIdAndCarteraIdAndActivaTrue(UUID personaId, UUID carteraId);

    boolean existsByAsignacionMensualIdAndPersonaIdAndActivaTrue(UUID asignacionMensualId, UUID personaId);

    Optional<AsignacionMensualPersona> findByAsignacionMensualIdAndPersonaIdAndActivaTrue(
            UUID asignacionMensualId, UUID personaId);

    @Modifying
    @Query("UPDATE AsignacionMensualPersona amp " +
           "SET amp.activa = false, amp.fechaFin = :fechaFin, amp.fechaActualizacion = :ahora " +
           "WHERE amp.asignacionMensualId = :mid AND amp.activa = true")
    void cerrarTodasParaMensual(@Param("mid") UUID asignacionMensualId,
                                @Param("fechaFin") LocalDate fechaFin,
                                @Param("ahora") Instant ahora);
}
