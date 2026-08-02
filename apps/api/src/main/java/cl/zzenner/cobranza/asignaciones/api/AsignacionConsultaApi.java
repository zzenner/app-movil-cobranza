package cl.zzenner.cobranza.asignaciones.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsignacionConsultaApi {

    Optional<DatosAsignacionMensual> findAsignacionMensualActiva(UUID ejecutivoId, UUID carteraId);

    List<UUID> findPersonasEnAsignacionMensualActiva(UUID ejecutivoId, UUID carteraId);

    boolean personaEnAsignacionMensualActiva(UUID personaId, UUID carteraId);

    Optional<DatosAsignacionDiaria> findAsignacionDiariaPublicada(UUID ejecutivoId, LocalDate fecha);

    Optional<DatosAsignacionDiaria> findAsignacionDiaria(UUID asignacionDiariaId);

    boolean personaEnAsignacionDiaria(UUID personaId, UUID asignacionDiariaId);

    List<UUID> findPersonasEnAsignacionDiaria(UUID asignacionDiariaId);
}
