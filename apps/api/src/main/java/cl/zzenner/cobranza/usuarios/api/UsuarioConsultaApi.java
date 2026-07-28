package cl.zzenner.cobranza.usuarios.api;

import java.util.UUID;

/**
 * Puerto público del módulo usuarios para consulta de estado por otros módulos.
 * No expone repositorios ni entidades internas.
 */
public interface UsuarioConsultaApi {

    /**
     * Verifica que el usuario existe, está activo y no está bloqueado.
     *
     * @param usuarioId UUID del usuario
     * @return true si el usuario existe y está habilitado
     */
    boolean existeYEstaHabilitado(UUID usuarioId);
}
