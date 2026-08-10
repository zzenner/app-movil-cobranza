package cl.zzenner.cobranza.importacion.dominio;

import java.util.UUID;

public class EstadoInvalidoParaConfirmarException extends RuntimeException {
    private final UUID importacionId;
    private final EstadoImportacion estadoActual;

    public EstadoInvalidoParaConfirmarException(UUID importacionId, EstadoImportacion estadoActual) {
        super("La importación " + importacionId + " no puede confirmarse en estado " + estadoActual);
        this.importacionId = importacionId;
        this.estadoActual = estadoActual;
    }

    public UUID getImportacionId() { return importacionId; }
    public EstadoImportacion getEstadoActual() { return estadoActual; }
}
