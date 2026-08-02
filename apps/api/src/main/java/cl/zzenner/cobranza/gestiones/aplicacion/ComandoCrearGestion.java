package cl.zzenner.cobranza.gestiones.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ComandoCrearGestion(
    UUID id,
    String origenGestion,
    UUID asignacionDiariaId,
    UUID personaId,
    UUID ejecutivoId,
    String tipoGestion,
    Instant fechaGestion,
    String observacion,
    String observacionDireccion,
    double latitud,
    double longitud,
    float precisionMetros,
    String proveedorGps,
    Boolean ubicacionSimulada,
    Instant fechaCapturaGps,
    LocalDate fechaCompromiso
) {}
