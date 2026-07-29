package cl.zzenner.cobranza.dispositivos.api;

import java.util.Optional;
import java.util.UUID;

public interface DispositivoConsultaApi {

    /**
     * Busca un dispositivo por su UUID y valida que esté activo, no revocado
     * y pertenezca al usuario indicado.
     *
     * @throws DispositivoNoValidoException si el dispositivo no existe, está revocado,
     *                                      está inactivo o pertenece a otro usuario
     */
    DatosDispositivo buscarPorId(UUID dispositivoId, UUID usuarioId);

    Optional<DatosDispositivo> buscarPorIdentificadorInstalacion(String identificadorInstalacion);
}
