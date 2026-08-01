package cl.zzenner.cobranza.asignaciones.api;

import java.time.LocalDate;
import java.util.UUID;

public record DatosAsignacionMensual(
        UUID id,
        UUID carteraId,
        UUID ejecutivoId,
        UUID supervisorId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activa
) {}
