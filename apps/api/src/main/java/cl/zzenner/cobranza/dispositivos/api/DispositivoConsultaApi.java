package cl.zzenner.cobranza.dispositivos.api;

import java.util.Optional;
import java.util.UUID;

public interface DispositivoConsultaApi {

    /**
     * Busca un dispositivo por su UUID interno y valida que esté activo, no revocado
     * y pertenezca al usuario indicado.
     *
     * @throws DispositivoNoValidoException       si el dispositivo no existe, está revocado o inactivo
     * @throws DispositivoDeOtroUsuarioException  si pertenece a un usuario diferente
     */
    DatosDispositivo buscarPorId(UUID dispositivoId, UUID usuarioId);

    Optional<DatosDispositivo> buscarPorIdentificadorInstalacion(String identificadorInstalacion);

    /**
     * Busca el dispositivo por identificadorInstalacion; si no existe lo registra con datos mínimos.
     * Se debe invocar únicamente DESPUÉS de validar las credenciales del usuario.
     *
     * @param identificadorInstalacion UUID v4 generado en el dispositivo Android
     * @param usuarioId                ID del usuario ya autenticado
     * @return datos del dispositivo existente o recién creado
     * @throws DispositivoDeOtroUsuarioException si el identificador ya pertenece a otro usuario
     * @throws DispositivoNoValidoException       si el dispositivo existe pero está revocado
     */
    DatosDispositivo buscarORegistrar(String identificadorInstalacion, UUID usuarioId);
}
