package cl.zzenner.cobranza.usuarios.dominio;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String correo) {
        super("El correo electrónico ya está en uso: " + correo);
    }
}
