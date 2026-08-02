package cl.zzenner.cobranza.autenticacion.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudLogin(
        @NotBlank String nombreUsuario,
        @NotBlank String contrasena,
        @NotBlank
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "debe ser un UUID en formato canónico"
        )
        String identificadorInstalacion
) {}
