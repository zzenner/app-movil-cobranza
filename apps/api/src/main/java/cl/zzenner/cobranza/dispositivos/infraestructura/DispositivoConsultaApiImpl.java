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

    private DatosDispositivo toDto(Dispositivo d) {
        return new DatosDispositivo(
                d.getId(), d.getUsuarioId(), d.getIdentificadorInstalacion(),
                d.getNombreDispositivo(), d.getFabricante(), d.getModelo(),
                d.getVersionAndroid(), d.getVersionAplicacion(),
                d.isActivo(), d.isRevocado(), d.getFechaRegistro());
    }
}
