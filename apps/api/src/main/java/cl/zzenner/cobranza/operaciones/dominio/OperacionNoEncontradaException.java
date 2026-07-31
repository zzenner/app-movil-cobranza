package cl.zzenner.cobranza.operaciones.dominio;

import java.util.UUID;

public class OperacionNoEncontradaException extends RuntimeException {

    public OperacionNoEncontradaException(UUID id) {
        super("Operacion no encontrada: " + id);
    }

    public OperacionNoEncontradaException(String numeroOperacion) {
        super("Operacion no encontrada con número: " + numeroOperacion);
    }
}
