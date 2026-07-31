package cl.zzenner.cobranza.personas.dominio;

public class RutInvalidoException extends RuntimeException {

    public RutInvalidoException(String mensaje) {
        super(mensaje);
    }
}
