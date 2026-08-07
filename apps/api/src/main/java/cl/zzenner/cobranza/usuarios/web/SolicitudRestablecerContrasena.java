package cl.zzenner.cobranza.usuarios.web;

import jakarta.validation.constraints.NotBlank;

public record SolicitudRestablecerContrasena(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        String nuevaContrasena
) {}
