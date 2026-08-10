package cl.zzenner.cobranza.importacion.dominio;

import java.util.UUID;

public class ArchivoYaImportadoException extends RuntimeException {
    private final UUID importacionExistenteId;

    public ArchivoYaImportadoException(UUID importacionExistenteId) {
        super("El archivo ya fue importado. Importación existente: " + importacionExistenteId);
        this.importacionExistenteId = importacionExistenteId;
    }

    public UUID getImportacionExistenteId() { return importacionExistenteId; }
}
