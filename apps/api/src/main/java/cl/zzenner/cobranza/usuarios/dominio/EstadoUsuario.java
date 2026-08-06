package cl.zzenner.cobranza.usuarios.dominio;

/**
 * Estado calculado de un usuario. Precedencia descendente:
 * 1. activo == false → INACTIVO
 * 2. bloqueado == true → BLOQUEADO
 * 3. bloqueadoHasta != null y bloqueadoHasta > ahora → BLOQUEADO_TEMPORAL
 * 4. en cualquier otro caso → ACTIVO
 */
public enum EstadoUsuario {
    ACTIVO,
    BLOQUEADO_TEMPORAL,
    BLOQUEADO,
    INACTIVO
}
