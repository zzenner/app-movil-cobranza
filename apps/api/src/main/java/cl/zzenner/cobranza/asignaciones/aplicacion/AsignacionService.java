package cl.zzenner.cobranza.asignaciones.aplicacion;

import cl.zzenner.cobranza.asignaciones.dominio.*;
import cl.zzenner.cobranza.asignaciones.infraestructura.*;
import cl.zzenner.cobranza.carteras.api.CarteraConsultaApi;
import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AsignacionService {

    private final AsignacionMensualRepository asignacionMensualRepository;
    private final AsignacionMensualPersonaRepository asignacionMensualPersonaRepository;
    private final AsignacionDiariaRepository asignacionDiariaRepository;
    private final AsignacionDiariaPersonaRepository asignacionDiariaPersonaRepository;
    private final CarteraConsultaApi carteraConsultaApi;
    private final PersonaConsultaApi personaConsultaApi;
    private final UsuarioConsultaApi usuarioConsultaApi;

    public AsignacionService(AsignacionMensualRepository asignacionMensualRepository,
                              AsignacionMensualPersonaRepository asignacionMensualPersonaRepository,
                              AsignacionDiariaRepository asignacionDiariaRepository,
                              AsignacionDiariaPersonaRepository asignacionDiariaPersonaRepository,
                              CarteraConsultaApi carteraConsultaApi,
                              PersonaConsultaApi personaConsultaApi,
                              UsuarioConsultaApi usuarioConsultaApi) {
        this.asignacionMensualRepository = asignacionMensualRepository;
        this.asignacionMensualPersonaRepository = asignacionMensualPersonaRepository;
        this.asignacionDiariaRepository = asignacionDiariaRepository;
        this.asignacionDiariaPersonaRepository = asignacionDiariaPersonaRepository;
        this.carteraConsultaApi = carteraConsultaApi;
        this.personaConsultaApi = personaConsultaApi;
        this.usuarioConsultaApi = usuarioConsultaApi;
    }

    public UUID crearAsignacionMensual(UUID carteraId, UUID ejecutivoId, UUID supervisorId,
                                        LocalDate fechaInicio, LocalDate fechaFin, String observacion) {
        if (!carteraConsultaApi.existeActiva(carteraId)) {
            throw new IllegalArgumentException("La cartera no existe o no está activa: " + carteraId);
        }
        if (!usuarioConsultaApi.tieneRolActivo(ejecutivoId, "EJECUTIVO_TERRENO")) {
            throw new IllegalArgumentException("El usuario no tiene rol EJECUTIVO_TERRENO activo: " + ejecutivoId);
        }
        if (!usuarioConsultaApi.tieneRolActivo(supervisorId, "SUPERVISOR")) {
            throw new IllegalArgumentException("El usuario no tiene rol SUPERVISOR activo: " + supervisorId);
        }
        if (!usuarioConsultaApi.tieneSupervisionActiva(supervisorId, ejecutivoId)) {
            throw new IllegalArgumentException(
                    "No existe supervisión activa entre supervisor " + supervisorId + " y ejecutivo " + ejecutivoId);
        }

        AsignacionMensual am = new AsignacionMensual(
                carteraId, ejecutivoId, supervisorId, fechaInicio, fechaFin, observacion);
        return asignacionMensualRepository.save(am).getId();
    }

    public void agregarPersonaAMensual(UUID asignacionMensualId, UUID personaId) {
        AsignacionMensual am = asignacionMensualRepository.findById(asignacionMensualId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionMensualId));

        if (!am.isActiva()) {
            throw new IllegalStateException("La asignación mensual no está activa: " + asignacionMensualId);
        }
        if (!personaConsultaApi.existe(personaId)) {
            throw new IllegalArgumentException("Persona no encontrada: " + personaId);
        }
        if (!personaConsultaApi.personaActivaEnCartera(personaId, am.getCarteraId())) {
            throw new IllegalArgumentException(
                    "La persona " + personaId + " no está activa en la cartera " + am.getCarteraId());
        }
        // Solo se verifica existencia de vínculo ACTIVO para permitir reincorporación tras cierre individual
        if (asignacionMensualPersonaRepository.existsByAsignacionMensualIdAndPersonaIdAndActivaTrue(
                asignacionMensualId, personaId)) {
            throw new PersonaYaEnAsignacionException(personaId, asignacionMensualId);
        }

        asignacionMensualPersonaRepository.save(
                new AsignacionMensualPersona(asignacionMensualId, personaId, am.getCarteraId(), LocalDate.now()));
    }

    public void removerPersonaDeMensual(UUID asignacionMensualId, UUID personaId, LocalDate fechaFin) {
        asignacionMensualRepository.findById(asignacionMensualId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionMensualId));

        AsignacionMensualPersona vinculo = asignacionMensualPersonaRepository
                .findByAsignacionMensualIdAndPersonaIdAndActivaTrue(asignacionMensualId, personaId)
                .orElseThrow(() -> new PersonaFueraDeAsignacionMensualException(personaId, asignacionMensualId));

        vinculo.cerrar(fechaFin);
        asignacionMensualPersonaRepository.save(vinculo);
    }

    public void cerrarAsignacionMensual(UUID asignacionMensualId) {
        AsignacionMensual am = asignacionMensualRepository.findById(asignacionMensualId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionMensualId));
        am.cerrar();
        asignacionMensualRepository.save(am);
        asignacionMensualPersonaRepository.cerrarTodasParaMensual(
                asignacionMensualId, LocalDate.now(), Instant.now());
    }

    public UUID crearAsignacionDiaria(UUID asignacionMensualId, UUID supervisorId, LocalDate fecha) {
        AsignacionMensual am = asignacionMensualRepository.findById(asignacionMensualId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionMensualId));

        if (!am.isActiva()) {
            throw new IllegalStateException("La asignación mensual no está activa: " + asignacionMensualId);
        }
        if (!usuarioConsultaApi.tieneRolActivo(supervisorId, "SUPERVISOR")) {
            throw new IllegalArgumentException("El usuario no tiene rol SUPERVISOR activo: " + supervisorId);
        }
        if (!usuarioConsultaApi.tieneSupervisionActiva(supervisorId, am.getEjecutivoId())) {
            throw new IllegalArgumentException(
                    "No existe supervisión activa entre supervisor " + supervisorId
                            + " y ejecutivo " + am.getEjecutivoId());
        }
        if (asignacionDiariaRepository.existsByEjecutivoIdAndFechaAndEstadoIn(
                am.getEjecutivoId(), fecha,
                List.of(EstadoAsignacionDiaria.BORRADOR, EstadoAsignacionDiaria.PUBLICADA))) {
            throw new IllegalStateException(
                    "El ejecutivo ya tiene una asignación diaria activa para la fecha: " + fecha);
        }

        AsignacionDiaria ad = new AsignacionDiaria(asignacionMensualId, am.getEjecutivoId(), supervisorId, fecha);
        return asignacionDiariaRepository.save(ad).getId();
    }

    public void agregarPersonaADiaria(UUID asignacionDiariaId, UUID personaId) {
        AsignacionDiaria ad = asignacionDiariaRepository.findById(asignacionDiariaId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionDiariaId));

        if (ad.getEstado() != EstadoAsignacionDiaria.BORRADOR) {
            throw new TransicionEstadoInvalidaException(
                    "Solo se pueden agregar personas en estado BORRADOR. Estado actual: " + ad.getEstado());
        }
        // Solo se verifica vínculo ACTIVO: personas removidas individualmente no aparecen en diarias
        if (!asignacionMensualPersonaRepository.existsByAsignacionMensualIdAndPersonaIdAndActivaTrue(
                ad.getAsignacionMensualId(), personaId)) {
            throw new PersonaFueraDeAsignacionMensualException(personaId, ad.getAsignacionMensualId());
        }
        if (asignacionDiariaPersonaRepository.existsByAsignacionDiariaIdAndPersonaId(asignacionDiariaId, personaId)) {
            throw new PersonaYaEnAsignacionException(personaId, asignacionDiariaId);
        }

        asignacionDiariaPersonaRepository.save(new AsignacionDiariaPersona(asignacionDiariaId, personaId));
    }

    public void publicarAsignacionDiaria(UUID asignacionDiariaId, UUID publicadoPorId) {
        AsignacionDiaria ad = asignacionDiariaRepository.findById(asignacionDiariaId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionDiariaId));

        int cantidadPersonas = asignacionDiariaPersonaRepository.countByAsignacionDiariaId(asignacionDiariaId);
        if (cantidadPersonas == 0) {
            throw new IllegalStateException("No se puede publicar una asignación diaria sin personas");
        }

        ad.publicar(publicadoPorId);
        asignacionDiariaRepository.save(ad);
    }

    public void finalizarAsignacionDiaria(UUID asignacionDiariaId) {
        AsignacionDiaria ad = asignacionDiariaRepository.findById(asignacionDiariaId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionDiariaId));
        ad.finalizar();
        asignacionDiariaRepository.save(ad);
    }

    public void cancelarAsignacionDiaria(UUID asignacionDiariaId, String motivo) {
        AsignacionDiaria ad = asignacionDiariaRepository.findById(asignacionDiariaId)
                .orElseThrow(() -> new AsignacionNoEncontradaException(asignacionDiariaId));
        ad.cancelar(motivo);
        asignacionDiariaRepository.save(ad);
    }
}
