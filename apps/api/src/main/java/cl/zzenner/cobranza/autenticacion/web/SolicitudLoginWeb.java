package cl.zzenner.cobranza.autenticacion.web;

import jakarta.validation.constraints.NotBlank;

public record SolicitudLoginWeb(
        @NotBlank String nombreUsuario,
        @NotBlank String clave
) {}
