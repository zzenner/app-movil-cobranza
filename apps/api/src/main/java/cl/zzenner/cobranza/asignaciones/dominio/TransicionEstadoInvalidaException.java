package cl.zzenner.cobranza.asignaciones.dominio;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(String message) {
        super(message);
    }
}
