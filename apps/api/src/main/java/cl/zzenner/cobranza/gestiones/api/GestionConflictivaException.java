package cl.zzenner.cobranza.gestiones.api;

import java.util.UUID;

public class GestionConflictivaException extends RuntimeException {

    private final UUID id;

    public GestionConflictivaException(UUID id) {
        super("UUID ya registrado con contenido distinto: " + id);
        this.id = id;
    }

    public UUID getId() { return id; }
}
