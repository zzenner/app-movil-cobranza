package cl.zzenner.cobranza.asignaciones.infraestructura;

import cl.zzenner.cobranza.asignaciones.api.AsignacionConsultaApi;
import cl.zzenner.cobranza.asignaciones.api.DatosAsignacionDiaria;
import cl.zzenner.cobranza.asignaciones.api.DatosAsignacionMensual;
import cl.zzenner.cobranza.asignaciones.dominio.AsignacionDiaria;
import cl.zzenner.cobranza.asignaciones.dominio.AsignacionMensual;
import cl.zzenner.cobranza.asignaciones.dominio.AsignacionMensualPersona;
import cl.zzenner.cobranza.asignaciones.dominio.EstadoAsignacionDiaria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class AsignacionConsultaApiImpl implements AsignacionConsultaApi {

    private final AsignacionMensualRepository asignacionMensualRepository;
    private final AsignacionMensualPersonaRepository asignacionMensualPersonaRepository;
    private final AsignacionDiariaRepository asignacionDiariaRepository;

    AsignacionConsultaApiImpl(AsignacionMensualRepository asignacionMensualRepository,
                               AsignacionMensualPersonaRepository asignacionMensualPersonaRepository,
                               AsignacionDiariaRepository asignacionDiariaRepository) {
        this.asignacionMensualRepository = asignacionMensualRepository;
        this.asignacionMensualPersonaRepository = asignacionMensualPersonaRepository;
        this.asignacionDiariaRepository = asignacionDiariaRepository;
    }

    @Override
    public Optional<DatosAsignacionMensual> findAsignacionMensualActiva(UUID ejecutivoId, UUID carteraId) {
        return asignacionMensualRepository
                .findByEjecutivoIdAndCarteraIdAndActivaTrue(ejecutivoId, carteraId)
                .map(this::toDtoMensual);
    }

    @Override
    public List<UUID> findPersonasEnAsignacionMensualActiva(UUID ejecutivoId, UUID carteraId) {
        return asignacionMensualRepository
                .findByEjecutivoIdAndCarteraIdAndActivaTrue(ejecutivoId, carteraId)
                .map(am -> asignacionMensualPersonaRepository
                        .findAllByAsignacionMensualIdAndActivaTrue(am.getId())
                        .stream()
                        .map(AsignacionMensualPersona::getPersonaId)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public boolean personaEnAsignacionMensualActiva(UUID personaId, UUID carteraId) {
        return asignacionMensualPersonaRepository
                .existsByPersonaIdAndCarteraIdAndActivaTrue(personaId, carteraId);
    }

    @Override
    public Optional<DatosAsignacionDiaria> findAsignacionDiariaPublicada(UUID ejecutivoId, LocalDate fecha) {
        return asignacionDiariaRepository
                .findByEjecutivoIdAndFechaAndEstado(ejecutivoId, fecha, EstadoAsignacionDiaria.PUBLICADA)
                .map(this::toDtoDiaria);
    }

    private DatosAsignacionMensual toDtoMensual(AsignacionMensual am) {
        return new DatosAsignacionMensual(
                am.getId(), am.getCarteraId(), am.getEjecutivoId(), am.getSupervisorId(),
                am.getFechaInicio(), am.getFechaFin(), am.isActiva());
    }

    private DatosAsignacionDiaria toDtoDiaria(AsignacionDiaria ad) {
        return new DatosAsignacionDiaria(
                ad.getId(), ad.getAsignacionMensualId(), ad.getEjecutivoId(), ad.getSupervisorId(),
                ad.getFecha(), ad.getEstado().name(), ad.getFechaPublicacion());
    }
}
