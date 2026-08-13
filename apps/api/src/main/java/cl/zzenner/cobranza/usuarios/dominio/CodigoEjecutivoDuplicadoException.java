package cl.zzenner.cobranza.usuarios.dominio;

public class CodigoEjecutivoDuplicadoException extends RuntimeException {
    public CodigoEjecutivoDuplicadoException(String codigo) {
        super("El código de ejecutivo ya está en uso: " + codigo);
    }
}
