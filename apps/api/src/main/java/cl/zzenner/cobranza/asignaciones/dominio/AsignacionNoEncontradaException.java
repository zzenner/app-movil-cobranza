package cl.zzenner.cobranza.asignaciones.dominio;

import java.util.UUID;

public class AsignacionNoEncontradaException extends RuntimeException {
    public AsignacionNoEncontradaException(UUID id) {
        super("Asignación no encontrada: " + id);
    }
}
