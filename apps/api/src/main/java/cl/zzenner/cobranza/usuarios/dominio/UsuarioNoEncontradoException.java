package cl.zzenner.cobranza.usuarios.dominio;

import java.util.UUID;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(UUID id) {
        super("Usuario no encontrado: " + id);
    }
    public UsuarioNoEncontradoException(String nombreUsuario) {
        super("Usuario no encontrado: " + nombreUsuario);
    }
}
