package cl.zzenner.cobranza.importacion.dominio;

import java.util.UUID;

public class ImportacionEnProgresoException extends RuntimeException {
    private final UUID importacionEnProgresoId;

    public ImportacionEnProgresoException(UUID importacionEnProgresoId) {
        super("Ya existe una importación en proceso para este periodo/cartera: " + importacionEnProgresoId);
        this.importacionEnProgresoId = importacionEnProgresoId;
    }

    public UUID getImportacionEnProgresoId() { return importacionEnProgresoId; }
}
