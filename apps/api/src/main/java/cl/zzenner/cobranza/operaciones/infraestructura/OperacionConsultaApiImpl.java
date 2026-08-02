package cl.zzenner.cobranza.operaciones.infraestructura;

import cl.zzenner.cobranza.operaciones.api.DatosCuota;
import cl.zzenner.cobranza.operaciones.api.DatosOperacion;
import cl.zzenner.cobranza.operaciones.api.OperacionConsultaApi;
import cl.zzenner.cobranza.operaciones.dominio.Cuota;
import cl.zzenner.cobranza.operaciones.dominio.EstadoCuota;
import cl.zzenner.cobranza.operaciones.dominio.EstadoOperacion;
import cl.zzenner.cobranza.operaciones.dominio.Operacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
class OperacionConsultaApiImpl implements OperacionConsultaApi {

    private static final List<EstadoCuota> ESTADOS_CUOTA_RELEVANTES =
            List.of(EstadoCuota.VENCIDA, EstadoCuota.VIGENTE, EstadoCuota.FUTURA);

    private final OperacionRepository operacionRepository;
    private final CuotaRepository cuotaRepository;

    OperacionConsultaApiImpl(OperacionRepository operacionRepository,
                              CuotaRepository cuotaRepository) {
        this.operacionRepository = operacionRepository;
        this.cuotaRepository = cuotaRepository;
    }

    @Override
    public List<DatosOperacion> findOperacionesActivasConCuotas(Collection<UUID> personaIds) {
        if (personaIds.isEmpty()) return List.of();

        // Query 1: operaciones ACTIVA para todas las personas (IN)
        List<Operacion> operaciones = operacionRepository
                .findByPersonaIdInAndEstado(personaIds, EstadoOperacion.ACTIVA);

        if (operaciones.isEmpty()) return List.of();

        // Query 2: cuotas relevantes para todas las operaciones (IN)
        Set<UUID> operacionIds = operaciones.stream()
                .map(Operacion::getId)
                .collect(Collectors.toSet());

        Map<UUID, List<Cuota>> cuotasPorOperacion = cuotaRepository
                .findByOperacionIdInAndEstadoIn(operacionIds, ESTADOS_CUOTA_RELEVANTES)
                .stream()
                .collect(Collectors.groupingBy(Cuota::getOperacionId));

        return operaciones.stream()
                .map(op -> toDto(op, cuotasPorOperacion.getOrDefault(op.getId(), List.of())))
                .toList();
    }

    private DatosOperacion toDto(Operacion op, List<Cuota> cuotas) {
        List<DatosCuota> datosCuotas = cuotas.stream()
                .sorted(java.util.Comparator.comparingInt(Cuota::getNumeroCuota))
                .map(this::toCuotaDto)
                .toList();
        return new DatosOperacion(
                op.getId(), op.getPersonaId(), op.getNumeroOperacion(),
                op.getEstado().name(), op.getCapital(), op.getInteresPenal(),
                op.getGastosCobranza(), op.getTotalVigente(), datosCuotas);
    }

    private DatosCuota toCuotaDto(Cuota c) {
        return new DatosCuota(
                c.getNumeroCuota(), c.getEstado().name(),
                c.getMontoTotal(), c.getSaldo(), c.getFechaVencimiento());
    }
}
