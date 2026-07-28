package cl.zzenner.cobranza.dispositivos.aplicacion;

import cl.zzenner.cobranza.dispositivos.dominio.Dispositivo;
import cl.zzenner.cobranza.dispositivos.dominio.DispositivoDuplicadoException;
import cl.zzenner.cobranza.dispositivos.dominio.DispositivoRevocadoException;
import cl.zzenner.cobranza.dispositivos.infraestructura.DispositivoRepository;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DispositivoService {

    private final DispositivoRepository dispositivoRepository;
    private final UsuarioConsultaApi usuarioConsultaApi;

    public DispositivoService(DispositivoRepository dispositivoRepository,
                               UsuarioConsultaApi usuarioConsultaApi) {
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioConsultaApi = usuarioConsultaApi;
    }

    /**
     * Registra un nuevo dispositivo Android para un usuario.
     * El identificador de instalación es un UUID generado por la app Android,
     * distinto del ID de la fila en la base de datos.
     *
     * @return UUID del dispositivo creado
     */
    public UUID registrarDispositivo(UUID usuarioId, String identificadorInstalacion,
                                      String nombreDispositivo, String fabricante, String modelo,
                                      String versionAndroid, String versionAplicacion) {
        if (!usuarioConsultaApi.existeYEstaHabilitado(usuarioId)) {
            throw new IllegalArgumentException(
                    "El usuario no existe o no está habilitado: " + usuarioId);
        }
        if (dispositivoRepository.existsByIdentificadorInstalacion(identificadorInstalacion)) {
            throw new DispositivoDuplicadoException(identificadorInstalacion);
        }

        Dispositivo dispositivo = new Dispositivo(
                usuarioId, identificadorInstalacion, nombreDispositivo,
                fabricante, modelo, versionAndroid, versionAplicacion);

        return dispositivoRepository.save(dispositivo).getId();
    }

    /**
     * Revoca un dispositivo. El dispositivo queda inactivo y no puede
     * volver a considerarse activo sin una operación administrativa explícita.
     */
    public void revocarDispositivo(UUID dispositivoId) {
        Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dispositivo no encontrado: " + dispositivoId));

        if (dispositivo.isRevocado()) {
            throw new DispositivoRevocadoException(dispositivoId);
        }

        dispositivo.revocar();
        dispositivoRepository.save(dispositivo);
    }
}
