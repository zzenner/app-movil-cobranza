package cl.zzenner.cobranza.autenticacion.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SolicitudLogin(
        @NotBlank String nombreUsuario,
        @NotBlank String contrasena,
        @NotNull UUID dispositivoId
) {}
