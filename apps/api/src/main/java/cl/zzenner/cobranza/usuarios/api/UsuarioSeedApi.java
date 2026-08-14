package cl.zzenner.cobranza.usuarios.api;

import java.util.UUID;

/**
 * Puerto público del módulo usuarios para inicialización de datos de desarrollo.
 * Solo para uso del DevSeedRunner bajo el perfil docker.
 */
public interface UsuarioSeedApi {

    /**
     * Crea un usuario con el rol indicado si no existe. Idempotente.
     *
     * @return true si el usuario fue creado, false si ya existía
     */
    boolean crearSiNoExiste(String nombreUsuario, String password, String email, String rol);

    /**
     * Crea o recupera un supervisor demo. Idempotente por nombre de usuario.
     *
     * @return UUID del supervisor (nuevo o existente)
     */
    UUID crearSupervisorDemoSiNoExiste(String nombreUsuario, String nombres, String apellido, String password);

    /**
     * Crea o recupera un ejecutivo demo con código de origen. Idempotente por nombre de usuario.
     * El codigoEjecutivo se preserva exactamente como se recibe (String, con ceros iniciales si los tiene).
     *
     * @return UUID del ejecutivo (nuevo o existente)
     */
    UUID crearEjecutivoDemoSiNoExiste(String nombreUsuario, String nombres, String apellido,
                                      String password, String codigoEjecutivo);

    /**
     * Devuelve el UUID del usuario con ese nombre de usuario, o null si no existe.
     */
    UUID findIdByNombreUsuario(String nombreUsuario);
}
