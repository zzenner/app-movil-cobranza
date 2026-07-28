package cl.zzenner.cobranza.usuarios.aplicacion;

/**
 * Puerto de codificación de contraseñas.
 * El adaptador BCrypt vive en la capa de infraestructura.
 * El hash nunca debe escribirse en logs, errores ni representaciones públicas.
 */
public interface CodificadorContrasena {

    /**
     * Codifica una contraseña en texto plano.
     *
     * @param contrasenaCruda contraseña sin cifrar
     * @return hash irreversible
     */
    String codificar(String contrasenaCruda);

    /**
     * Verifica que una contraseña coincide con un hash almacenado.
     *
     * @param contrasenaCruda contraseña a verificar
     * @param hash            hash almacenado
     * @return true si coincide
     */
    boolean verificar(String contrasenaCruda, String hash);
}
