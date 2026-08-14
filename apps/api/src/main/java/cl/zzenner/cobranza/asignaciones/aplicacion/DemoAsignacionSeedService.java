package cl.zzenner.cobranza.asignaciones.aplicacion;

import cl.zzenner.cobranza.asignaciones.api.DemoAsignacionSeedApi;
import cl.zzenner.cobranza.asignaciones.dominio.EstadoAsignacionDiaria;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionDiariaPersonaRepository;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionDiariaRepository;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionMensualPersonaRepository;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionMensualRepository;
import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
class DemoAsignacionSeedService implements DemoAsignacionSeedApi {

    private final AsignacionService asignacionService;
    private final AsignacionMensualRepository asignacionMensualRepository;
    private final AsignacionMensualPersonaRepository asignacionMensualPersonaRepository;
    private final AsignacionDiariaRepository asignacionDiariaRepository;
    private final AsignacionDiariaPersonaRepository asignacionDiariaPersonaRepository;
    private final PersonaConsultaApi personaConsultaApi;

    DemoAsignacionSeedService(AsignacionService asignacionService,
                               AsignacionMensualRepository asignacionMensualRepository,
                               AsignacionMensualPersonaRepository asignacionMensualPersonaRepository,
                               AsignacionDiariaRepository asignacionDiariaRepository,
                               AsignacionDiariaPersonaRepository asignacionDiariaPersonaRepository,
                               PersonaConsultaApi personaConsultaApi) {
        this.asignacionService = asignacionService;
        this.asignacionMensualRepository = asignacionMensualRepository;
        this.asignacionMensualPersonaRepository = asignacionMensualPersonaRepository;
        this.asignacionDiariaRepository = asignacionDiariaRepository;
        this.asignacionDiariaPersonaRepository = asignacionDiariaPersonaRepository;
        this.personaConsultaApi = personaConsultaApi;
    }

    @Override
    public void prepararEscenarioDemo(UUID carteraId, UUID ejecutivoId,
                                      UUID supervisorId, UUID publicadorId,
                                      int cantidadPersonas) {
        LocalDate hoy = LocalDate.now();

        // Idempotente: si ya hay diaria PUBLICADA para hoy, nada que hacer
        if (asignacionDiariaRepository.findByEjecutivoIdAndFechaAndEstado(
                ejecutivoId, hoy, EstadoAsignacionDiaria.PUBLICADA).isPresent()) {
            return;
        }

        // Mensual: buscar existente o crear nueva para el mes actual
        UUID mensualId = asignacionMensualRepository
                .findByEjecutivoIdAndCarteraIdAndActivaTrue(ejecutivoId, carteraId)
                .map(am -> am.getId())
                .orElseGet(() -> {
                    LocalDate inicioMes = hoy.withDayOfMonth(1);
                    LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
                    return asignacionService.crearAsignacionMensual(
                            carteraId, ejecutivoId, supervisorId, inicioMes, finMes, "Demo seed");
                });

        // Personas: tomar los primeros N de la cartera
        List<UUID> personaIds = personaConsultaApi.findIdsByCarteraIdActiva(carteraId, cantidadPersonas);

        // Agregar personas a mensual — verificar estado antes de llamar (evita rollback-only)
        for (UUID personaId : personaIds) {
            if (!asignacionMensualPersonaRepository
                    .existsByAsignacionMensualIdAndPersonaIdAndActivaTrue(mensualId, personaId)) {
                asignacionService.agregarPersonaAMensual(mensualId, personaId);
            }
        }

        // Diaria: buscar BORRADOR existente para hoy o crear nueva
        UUID diariaId = asignacionDiariaRepository
                .findByEjecutivoIdAndFechaAndEstado(ejecutivoId, hoy, EstadoAsignacionDiaria.BORRADOR)
                .map(ad -> ad.getId())
                .orElseGet(() -> asignacionService.crearAsignacionDiaria(mensualId, supervisorId, hoy));

        // Agregar personas a diaria — verificar estado antes de llamar (evita rollback-only)
        for (UUID personaId : personaIds) {
            if (!asignacionDiariaPersonaRepository
                    .existsByAsignacionDiariaIdAndPersonaId(diariaId, personaId)) {
                asignacionService.agregarPersonaADiaria(diariaId, personaId);
            }
        }

        // Publicar
        asignacionService.publicarAsignacionDiaria(diariaId, publicadorId);
    }
}
