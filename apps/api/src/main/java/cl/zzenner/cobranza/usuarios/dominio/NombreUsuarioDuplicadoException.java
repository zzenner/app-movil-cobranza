package cl.zzenner.cobranza.usuarios.dominio;

public class NombreUsuarioDuplicadoException extends RuntimeException {
    public NombreUsuarioDuplicadoException(String nombreUsuario) {
        super("El nombre de usuario ya está en uso: " + nombreUsuario);
    }
}
