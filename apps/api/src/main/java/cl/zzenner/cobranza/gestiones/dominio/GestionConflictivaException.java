package cl.zzenner.cobranza.gestiones.dominio;

import java.util.UUID;

public class GestionConflictivaException extends RuntimeException {

    private final UUID id;

    public GestionConflictivaException(UUID id) {
        super("UUID de gestión en conflicto: ya existe una gestión con el mismo ID pero diferente contenido: " + id);
        this.id = id;
    }

    public UUID getId() { return id; }
}
