package cl.zzenner.cobranza.usuarios.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto público del módulo usuarios para consulta y operaciones de estado.
 * No expone repositorios ni entidades internas.
 */
public interface UsuarioConsultaApi {

    /**
     * Verifica que el usuario existe, está activo y no está bloqueado.
     * Considera tanto el bloqueo administrativo como el temporal.
     */
    boolean existeYEstaHabilitado(UUID usuarioId);

    /**
     * Busca los datos de autenticación de un usuario por nombre de usuario normalizado.
     * El resultado incluye el hash de contraseña para uso exclusivo en el flujo de autenticación.
     * No usar desde controladores ni para propósitos distintos al login.
     */
    Optional<CredencialesUsuario> buscarParaAutenticacion(String nombreUsuario);

    /**
     * Busca los datos de autenticación de un usuario por su UUID.
     * Usado en renovación de tokens para refrescar roles y permisos sin re-autenticar.
     */
    Optional<CredencialesUsuario> buscarCredencialesPorId(UUID usuarioId);

    /**
     * Registra un intento de autenticación fallido para el usuario.
     * Incrementa el contador de intentos_fallidos.
     * No incrementa si el usuario está en período de bloqueo temporal activo.
     *
     * @return nuevo valor de intentos_fallidos tras el incremento
     */
    int registrarIntentoFallido(UUID usuarioId);

    /**
     * Aplica un bloqueo temporal automático al usuario hasta la fecha indicada.
     * No afecta al campo bloqueado (bloqueo administrativo).
     */
    void aplicarBloqueoTemporal(UUID usuarioId, Instant hasta);

    /**
     * Registra un acceso exitoso: resetea intentos_fallidos, limpia bloqueado_hasta
     * y actualiza fecha_ultimo_acceso.
     */
    void registrarAccesoExitoso(UUID usuarioId);
}
