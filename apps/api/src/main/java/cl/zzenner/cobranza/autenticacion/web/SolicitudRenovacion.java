package cl.zzenner.cobranza.autenticacion.web;

import jakarta.validation.constraints.NotBlank;

public record SolicitudRenovacion(@NotBlank String refreshToken) {}
