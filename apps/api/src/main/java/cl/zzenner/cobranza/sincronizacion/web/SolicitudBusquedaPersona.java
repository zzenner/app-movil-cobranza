package cl.zzenner.cobranza.sincronizacion.web;

import jakarta.validation.constraints.NotBlank;

public record SolicitudBusquedaPersona(
    @NotBlank String rutNumero,
    @NotBlank String rutDv
) {}
