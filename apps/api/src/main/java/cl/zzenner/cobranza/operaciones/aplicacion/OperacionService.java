package cl.zzenner.cobranza.operaciones.aplicacion;

import cl.zzenner.cobranza.operaciones.dominio.*;
import cl.zzenner.cobranza.operaciones.infraestructura.CuotaRepository;
import cl.zzenner.cobranza.operaciones.infraestructura.OperacionRepository;
import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import cl.zzenner.cobranza.personas.api.PersonaNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OperacionService {

    private final OperacionRepository operacionRepository;
    private final CuotaRepository cuotaRepository;
    private final PersonaConsultaApi personaConsultaApi;

    public OperacionService(OperacionRepository operacionRepository,
                            CuotaRepository cuotaRepository,
                            PersonaConsultaApi personaConsultaApi) {
        this.operacionRepository = operacionRepository;
        this.cuotaRepository = cuotaRepository;
        this.personaConsultaApi = personaConsultaApi;
    }

    public Operacion upsertOperacion(UUID personaId, String numeroOperacion,
                                     EstadoOperacion estado, BigDecimal capital,
                                     String sistemaOrigen, Instant fechaActualizacionOrigen) {
        if (!personaConsultaApi.existe(personaId)) {
            throw new PersonaNoEncontradaException(personaId);
        }
        return operacionRepository.findByNumeroOperacion(numeroOperacion)
                .map(op -> {
                    op.setEstado(estado);
                    op.setCapital(capital);
                    op.registrarImportacion(fechaActualizacionOrigen);
                    return operacionRepository.save(op);
                })
                .orElseGet(() -> {
                    Operacion nueva = new Operacion(personaId, numeroOperacion, estado, capital, sistemaOrigen);
                    nueva.registrarImportacion(fechaActualizacionOrigen);
                    return operacionRepository.save(nueva);
                });
    }

    public Cuota upsertCuota(UUID operacionId, int numeroCuota, EstadoCuota estado,
                             BigDecimal montoTotal, LocalDate fechaVencimiento,
                             Instant fechaActualizacionOrigen) {
        operacionRepository.findById(operacionId)
                .orElseThrow(() -> new OperacionNoEncontradaException(operacionId));
        return cuotaRepository.findByOperacionIdAndNumeroCuota(operacionId, numeroCuota)
                .map(c -> {
                    c.setEstado(estado);
                    c.setMontoTotal(montoTotal);
                    c.registrarImportacion(fechaActualizacionOrigen);
                    return cuotaRepository.save(c);
                })
                .orElseGet(() -> {
                    Cuota nueva = new Cuota(operacionId, numeroCuota, estado, montoTotal, fechaVencimiento);
                    nueva.registrarImportacion(fechaActualizacionOrigen);
                    return cuotaRepository.save(nueva);
                });
    }

    @Transactional(readOnly = true)
    public List<Operacion> findByPersonaId(UUID personaId) {
        return operacionRepository.findByPersonaId(personaId);
    }

    @Transactional(readOnly = true)
    public List<Cuota> findCuotasByOperacionId(UUID operacionId) {
        return cuotaRepository.findByOperacionIdOrderByNumeroCuota(operacionId);
    }
}
