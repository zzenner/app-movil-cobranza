package cl.zzenner.cobranza.importacion.dominio;

import java.util.UUID;

public class ImportacionMensualNoEncontradaException extends RuntimeException {
    private final UUID importacionId;

    public ImportacionMensualNoEncontradaException(UUID importacionId) {
        super("Importación mensual no encontrada: " + importacionId);
        this.importacionId = importacionId;
    }

    public UUID getImportacionId() { return importacionId; }
}
