package cl.zzenner.cobranza.gestiones.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DatosGestion(
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
    boolean ubicacionSimulada,
    Instant fechaCapturaGps,
    LocalDate fechaCompromiso,
    Instant fechaCreacionServidor
) {}
