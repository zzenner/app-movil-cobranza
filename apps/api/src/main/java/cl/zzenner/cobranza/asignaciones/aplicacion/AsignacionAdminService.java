package cl.zzenner.cobranza.asignaciones.aplicacion;

import cl.zzenner.cobranza.asignaciones.dominio.*;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionDiariaPersonaRepository;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionDiariaRepository;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionMensualPersonaRepository;
import cl.zzenner.cobranza.asignaciones.infraestructura.AsignacionMensualRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Operaciones administrativas de asignaciones diarias.
 * Delega la lógica de dominio en AsignacionService; añade composición transaccional
 * de varios pasos (crear borrador + agregar personas en una sola transacción).
 */
@Service
@Transactional
public class AsignacionAdminService {

    private final AsignacionService asignacionService;
    private final AsignacionMensualRepository mensualRepository;
    private final AsignacionDiariaRepository diariaRepository;
    private final AsignacionDiariaPersonaRepository diariaPersonaRepository;
    private final AsignacionMensualPersonaRepository mensualPersonaRepository;

    public AsignacionAdminService(
            AsignacionService asignacionService,
            AsignacionMensualRepository mensualRepository,
            AsignacionDiariaRepository diariaRepository,
            AsignacionDiariaPersonaRepository diariaPersonaRepository,
            AsignacionMensualPersonaRepository mensualPersonaRepository) {
        this.asignacionService = asignacionService;
        this.mensualRepository = mensualRepository;
        this.diariaRepository = diariaRepository;
        this.diariaPersonaRepository = diariaPersonaRepository;
        this.mensualPersonaRepository = mensualPersonaRepository;
    }

    /**
     * Crea un borrador de asignación diaria y agrega las personas iniciales.
     * Si personaIds está vacío se crea un borrador sin personas (válido hasta publicar).
     */
    public UUID crearBorrador(UUID asignacionMensualId, UUID supervisorId,
                               LocalDate fecha, List<UUID> personaIds) {
        UUID diariaId = asignacionService.crearAsignacionDiaria(
                asignacionMensualId, supervisorId, fecha);

        for (UUID personaId : personaIds) {
            asignacionService.agregarPersonaADiaria(diariaId, personaId);
        }
        return diariaId;
    }

    /**
     * Reemplaza el conjunto completo de personas de un borrador.
     * Solo permitido en estado BORRADOR.
     */
    public void actualizarPersonas(UUID asignacionDiariaId, List<UUID> personaIds, UUID asignacionMensualId) {
        AsignacionDiaria ad = diariaRepository.findById(asignacionDiariaId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionDiariaId));

        if (ad.getEstado() != EstadoAsignacionDiaria.BORRADOR) {
            throw new TransicionEstadoInvalidaException(
                    "Solo se pueden modificar personas en estado BORRADOR. Estado actual: " + ad.getEstado());
        }

        // Obtener personas actuales y calcular diferencia
        Set<UUID> actuales = Set.copyOf(
                diariaPersonaRepository.findPersonaIdsByAsignacionDiariaId(asignacionDiariaId));
        Set<UUID> nuevas = Set.copyOf(personaIds);

        // Quitar las que no están en la nueva lista
        for (UUID pid : actuales) {
            if (!nuevas.contains(pid)) {
                diariaPersonaRepository.deleteByAsignacionDiariaIdAndPersonaId(asignacionDiariaId, pid);
            }
        }

        // Agregar las que son nuevas
        for (UUID pid : nuevas) {
            if (!actuales.contains(pid)) {
                asignacionService.agregarPersonaADiaria(asignacionDiariaId, pid);
            }
        }
    }

    /** Delega la publicación al servicio de dominio. */
    public void publicar(UUID asignacionDiariaId, UUID publicadoPorId) {
        asignacionService.publicarAsignacionDiaria(asignacionDiariaId, publicadoPorId);
    }

    /** Delega la cancelación al servicio de dominio. */
    public void cancelar(UUID asignacionDiariaId, String motivo) {
        asignacionService.cancelarAsignacionDiaria(asignacionDiariaId, motivo);
    }
}
