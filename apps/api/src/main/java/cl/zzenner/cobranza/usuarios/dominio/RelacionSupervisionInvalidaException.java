package cl.zzenner.cobranza.usuarios.dominio;

public class RelacionSupervisionInvalidaException extends RuntimeException {
    public RelacionSupervisionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
