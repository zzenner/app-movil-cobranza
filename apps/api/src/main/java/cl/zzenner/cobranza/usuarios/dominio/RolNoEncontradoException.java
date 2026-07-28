package cl.zzenner.cobranza.usuarios.dominio;

public class RolNoEncontradoException extends RuntimeException {
    public RolNoEncontradoException(String codigo) {
        super("Rol no encontrado: " + codigo);
    }
}
