package cl.zzenner.cobranza.gestiones.infraestructura;

import cl.zzenner.cobranza.gestiones.api.DatosGestion;
import cl.zzenner.cobranza.gestiones.api.GestionConsultaApi;
import cl.zzenner.cobranza.gestiones.dominio.Gestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class GestionConsultaApiImpl implements GestionConsultaApi {

    private final GestionRepository gestionRepository;

    GestionConsultaApiImpl(GestionRepository gestionRepository) {
        this.gestionRepository = gestionRepository;
    }

    @Override
    public List<DatosGestion> findUltimasGestiones(UUID personaId, int limite) {
        return gestionRepository.findByPersonaIdOrderByFechaGestionDesc(personaId)
            .stream()
            .limit(limite)
            .map(this::toDto)
            .toList();
    }

    private DatosGestion toDto(Gestion g) {
        return new DatosGestion(
            g.getId(), g.getOrigenGestion().name(), g.getAsignacionDiariaId(),
            g.getPersonaId(), g.getEjecutivoId(), g.getTipoGestion().name(),
            g.getFechaGestion(), g.getObservacion(), g.getObservacionDireccion(),
            g.getLatitud(), g.getLongitud(), g.getPrecisionMetros(),
            g.getProveedorGps(), g.isUbicacionSimulada(), g.getFechaCapturaGps(),
            g.getFechaCompromiso(), g.getFechaCreacionServidor()
        );
    }
}
