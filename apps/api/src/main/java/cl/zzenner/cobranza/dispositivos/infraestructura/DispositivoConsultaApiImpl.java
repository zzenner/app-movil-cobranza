package cl.zzenner.cobranza.dispositivos.infraestructura;

import cl.zzenner.cobranza.dispositivos.api.DatosDispositivo;
import cl.zzenner.cobranza.dispositivos.api.DispositivoConsultaApi;
import cl.zzenner.cobranza.dispositivos.api.DispositivoDeOtroUsuarioException;
import cl.zzenner.cobranza.dispositivos.api.DispositivoNoValidoException;
import cl.zzenner.cobranza.dispositivos.dominio.Dispositivo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class DispositivoConsultaApiImpl implements DispositivoConsultaApi {

    private final DispositivoRepository dispositivoRepository;

    DispositivoConsultaApiImpl(DispositivoRepository dispositivoRepository) {
        this.dispositivoRepository = dispositivoRepository;
    }

    @Override
    public DatosDispositivo buscarPorId(UUID dispositivoId, UUID usuarioId) {
        Dispositivo d = dispositivoRepository.findById(dispositivoId)
                .orElseThrow(() -> new DispositivoNoValidoException(dispositivoId));

        if (!d.getUsuarioId().equals(usuarioId)) {
            throw new DispositivoDeOtroUsuarioException(dispositivoId);
        }
        if (d.isRevocado() || !d.isActivo()) {
            throw new DispositivoNoValidoException(dispositivoId);
        }

        return toDto(d);
    }

    @Override
    public Optional<DatosDispositivo> buscarPorIdentificadorInstalacion(String identificadorInstalacion) {
        return dispositivoRepository.findByIdentificadorInstalacion(identificadorInstalacion)
                .map(this::toDto);
    }

    /**
     * Busca el dispositivo por identificadorInstalacion; si no existe lo registra.
     * Patrón en dos fases para seguridad ante concurrencia (ADR-0031):
     *   1. Fast-path: findBy para reintentos simples (caso más común).
     *   2. Insert atómico: ON CONFLICT DO NOTHING; si retorna 0, el registro
     *      lo creó otro hilo concurrente — releer y validar.
     */
    @Override
    @Transactional
    public DatosDispositivo buscarORegistrar(String identificadorInstalacion, UUID usuarioId) {
        // Fast-path para el caso común: dispositivo ya registrado
        Optional<Dispositivo> existente = dispositivoRepository.findByIdentificadorInstalacion(identificadorInstalacion);
        if (existente.isPresent()) {
            return validarYConvertir(existente.get(), identificadorInstalacion, usuarioId);
        }

        // Insert atómico: si dos hilos llegan aquí simultáneamente, solo uno insertará.
        // El otro recibe 0 (DO NOTHING) sin excepción de clave duplicada.
        dispositivoRepository.insertarSiNoExiste(usuarioId, identificadorInstalacion);

        // Releer siempre después del insert (tanto si se insertó como si ya existía).
        Dispositivo d = dispositivoRepository.findByIdentificadorInstalacion(identificadorInstalacion)
                .orElseThrow(() -> new IllegalStateException(
                        "Dispositivo no encontrado tras insertarSiNoExiste: " + identificadorInstalacion));
        return validarYConvertir(d, identificadorInstalacion, usuarioId);
    }

    private DatosDispositivo validarYConvertir(Dispositivo d, String identificadorInstalacion, UUID usuarioId) {
        if (!d.getUsuarioId().equals(usuarioId)) {
            throw new DispositivoDeOtroUsuarioException(identificadorInstalacion);
        }
        if (d.isRevocado()) {
            throw new DispositivoNoValidoException(identificadorInstalacion);
        }
        return toDto(d);
    }

    private DatosDispositivo toDto(Dispositivo d) {
        return new DatosDispositivo(
                d.getId(), d.getUsuarioId(), d.getIdentificadorInstalacion(),
                d.getNombreDispositivo(), d.getFabricante(), d.getModelo(),
                d.getVersionAndroid(), d.getVersionAplicacion(),
                d.isActivo(), d.isRevocado(), d.getFechaRegistro());
    }
}
