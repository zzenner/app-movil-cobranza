package cl.zzenner.cobranza.usuarios.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SolicitudCrearUsuario(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 50, message = "El nombre de usuario no puede superar 50 caracteres")
        String nombreUsuario,

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

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena,

        @NotEmpty(message = "Se requiere al menos un rol inicial")
        List<@NotBlank(message = "El código de rol no puede estar en blanco") String> rolesIniciales
) {}
