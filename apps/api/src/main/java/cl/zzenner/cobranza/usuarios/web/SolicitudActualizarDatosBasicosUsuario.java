package cl.zzenner.cobranza.usuarios.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudActualizarDatosBasicosUsuario(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombres,

        @NotBlank(message = "El apellido paterno es obligatorio")
        @Size(max = 100, message = "El apellido paterno no puede superar 100 caracteres")
        String apellidoPaterno,

        @Size(max = 100, message = "El apellido materno no puede superar 100 caracteres")
        String apellidoMaterno,

        @Size(max = 200, message = "El correo no puede superar 200 caracteres")
        String correo,

        @NotNull(message = "La versión es obligatoria para la actualización")
        Long version
) {}
