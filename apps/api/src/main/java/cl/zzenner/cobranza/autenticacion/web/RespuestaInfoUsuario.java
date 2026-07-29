package cl.zzenner.cobranza.autenticacion.web;

import java.util.List;
import java.util.UUID;

/**
 * Información del usuario autenticado extraída del JWT.
 * Roles y permisos provienen de claims y pueden estar desactualizados hasta que el token expire o se renueve.
 */
public record RespuestaInfoUsuario(
        UUID usuarioId,
        UUID sesionId,
        UUID dispositivoId,
        String nombreUsuario,
        List<String> roles,
        List<String> permisos
) {}
