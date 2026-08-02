package cl.zzenner.cobranza.asignaciones.api;

import java.util.UUID;

public class AsignacionDiariaNoEncontradaException extends RuntimeException {

    private final UUID id;

    public AsignacionDiariaNoEncontradaException(UUID id) {
        super("Asignación diaria no encontrada: " + id);
        this.id = id;
    }

    public UUID getId() { return id; }
}
