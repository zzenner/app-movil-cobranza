package cl.zzenner.cobranza.gestiones.web;

import cl.zzenner.cobranza.gestiones.dominio.OrigenGestion;
import cl.zzenner.cobranza.gestiones.dominio.TipoGestion;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SolicitudCrearGestion(
    @NotNull UUID id,
    @NotNull UUID personaId,
    @NotNull OrigenGestion origenGestion,
    UUID asignacionDiariaId,
    @NotNull TipoGestion tipoGestion,
    @NotNull Instant fechaGestion,
    @NotNull Instant fechaCapturaGps,
    String observacion,
    String observacionDireccion,
    @NotNull Double latitud,
    @NotNull Double longitud,
    @NotNull Double precisionMetros,
    @NotNull Boolean ubicacionSimulada,
    String proveedorGps,
    LocalDate fechaCompromiso
) {}
