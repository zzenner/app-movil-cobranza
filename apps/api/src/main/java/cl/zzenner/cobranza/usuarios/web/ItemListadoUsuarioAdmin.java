package cl.zzenner.cobranza.usuarios.web;

import cl.zzenner.cobranza.usuarios.dominio.EstadoUsuario;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ItemListadoUsuarioAdmin(
        UUID id,
        String nombreUsuario,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,
        EstadoUsuario estadoCalculado,
        Instant bloqueadoHasta,
        List<String> roles,
        UUID supervisorId,
        String supervisorNombreUsuario,
        Instant fechaCreacion
) {}
