package cl.zzenner.cobranza.usuarios.web;

import cl.zzenner.cobranza.usuarios.dominio.EstadoUsuario;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DetalleUsuarioAdmin(
        UUID id,
        String nombreUsuario,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,
        EstadoUsuario estadoCalculado,
        boolean activo,
        boolean bloqueado,
        Instant bloqueadoHasta,
        int intentosFallidos,
        Instant fechaUltimoAcceso,
        List<RolVigenteAdmin> roles,
        List<String> permisosEfectivos,
        UUID supervisorId,
        String supervisorNombreUsuario,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {}
