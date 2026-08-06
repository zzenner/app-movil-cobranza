package cl.zzenner.cobranza.usuarios.api;

/**
 * Puerto público del módulo usuarios para inicialización de datos de desarrollo.
 * Solo para uso del DevSeedRunner bajo el perfil docker.
 */
public interface UsuarioSeedApi {

    /**
     * Crea un usuario con el rol indicado si no existe. Idempotente: si el usuario
     * ya existe (por nombre de usuario), no realiza ninguna acción.
     *
     * @return true si el usuario fue creado, false si ya existía
     */
    boolean crearSiNoExiste(String nombreUsuario, String password, String email, String rol);
}
