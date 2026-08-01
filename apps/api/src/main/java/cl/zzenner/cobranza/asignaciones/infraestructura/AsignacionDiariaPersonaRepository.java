package cl.zzenner.cobranza.asignaciones.infraestructura;

import cl.zzenner.cobranza.asignaciones.dominio.AsignacionDiariaPersona;
import cl.zzenner.cobranza.asignaciones.dominio.AsignacionDiariaPersonaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AsignacionDiariaPersonaRepository
        extends JpaRepository<AsignacionDiariaPersona, AsignacionDiariaPersonaId> {

    @Query("SELECT COUNT(adp) FROM AsignacionDiariaPersona adp WHERE adp.id.asignacionDiariaId = :did")
    int countByAsignacionDiariaId(@Param("did") UUID asignacionDiariaId);

    @Query("SELECT CASE WHEN COUNT(adp) > 0 THEN true ELSE false END FROM AsignacionDiariaPersona adp WHERE adp.id.asignacionDiariaId = :did AND adp.id.personaId = :pid")
    boolean existsByAsignacionDiariaIdAndPersonaId(@Param("did") UUID asignacionDiariaId, @Param("pid") UUID personaId);
}
