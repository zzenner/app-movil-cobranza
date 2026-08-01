package cl.zzenner.cobranza.asignaciones.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DatosAsignacionDiaria(
        UUID id,
        UUID asignacionMensualId,
        UUID ejecutivoId,
        UUID supervisorId,
        LocalDate fecha,
        String estado,
        Instant fechaPublicacion
) {}
