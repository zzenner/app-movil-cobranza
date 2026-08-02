package cl.zzenner.cobranza.gestiones.aplicacion;

import cl.zzenner.cobranza.asignaciones.api.AsignacionConsultaApi;
import cl.zzenner.cobranza.asignaciones.api.DatosAsignacionDiaria;
import cl.zzenner.cobranza.gestiones.dominio.Gestion;
import cl.zzenner.cobranza.gestiones.dominio.GestionConflictivaException;
import cl.zzenner.cobranza.gestiones.dominio.OrigenGestion;
import cl.zzenner.cobranza.gestiones.dominio.TipoGestion;
import cl.zzenner.cobranza.gestiones.infraestructura.GestionRepository;

import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class GestionService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Santiago");

    private final GestionRepository gestionRepository;
    private final AsignacionConsultaApi asignacionConsultaApi;
    private final PersonaConsultaApi personaConsultaApi;
    private final UsuarioConsultaApi usuarioConsultaApi;

    public GestionService(GestionRepository gestionRepository,
                          AsignacionConsultaApi asignacionConsultaApi,
                          PersonaConsultaApi personaConsultaApi,
                          UsuarioConsultaApi usuarioConsultaApi) {
        this.gestionRepository    = gestionRepository;
        this.asignacionConsultaApi = asignacionConsultaApi;
        this.personaConsultaApi   = personaConsultaApi;
        this.usuarioConsultaApi   = usuarioConsultaApi;
    }

    public void recibirGestion(ComandoCrearGestion cmd) {
        Objects.requireNonNull(cmd.id(),                "id es obligatorio");
        Objects.requireNonNull(cmd.origenGestion(),     "origenGestion es obligatorio");
        Objects.requireNonNull(cmd.personaId(),         "personaId es obligatorio");
        Objects.requireNonNull(cmd.ejecutivoId(),       "ejecutivoId es obligatorio");
        Objects.requireNonNull(cmd.tipoGestion(),       "tipoGestion es obligatorio");
        Objects.requireNonNull(cmd.fechaGestion(),      "fechaGestion es obligatoria");
        Objects.requireNonNull(cmd.fechaCapturaGps(),   "fechaCapturaGps es obligatoria");
        Objects.requireNonNull(cmd.ubicacionSimulada(), "ubicacionSimulada es obligatorio");

        OrigenGestion origen;
        TipoGestion tipo;
        try {
            origen = OrigenGestion.valueOf(cmd.origenGestion());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Origen de gestión no reconocido: " + cmd.origenGestion());
        }
        try {
            tipo = TipoGestion.valueOf(cmd.tipoGestion());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de gestión no reconocido: " + cmd.tipoGestion());
        }

        Gestion nueva = new Gestion(
            cmd.id(), origen, cmd.asignacionDiariaId(), cmd.personaId(), cmd.ejecutivoId(),
            tipo, cmd.fechaGestion(), cmd.observacion(), cmd.observacionDireccion(),
            cmd.latitud(), cmd.longitud(), cmd.precisionMetros(), cmd.proveedorGps(),
            cmd.ubicacionSimulada(), cmd.fechaCapturaGps(), cmd.fechaCompromiso()
        );

        // Fast-path: UUID ya existe (reintento simple del mismo cliente)
        Optional<Gestion> existenteFast = gestionRepository.findById(nueva.getId());
        if (existenteFast.isPresent()) {
            if (existenteFast.get().tieneContenidoConflictivo(nueva)) {
                throw new GestionConflictivaException(nueva.getId());
            }
            return;
        }

        // El ejecutivo debe tener rol EJECUTIVO_TERRENO
        if (!usuarioConsultaApi.tieneRolActivo(cmd.ejecutivoId(), "EJECUTIVO_TERRENO")) {
            throw new IllegalArgumentException(
                "El ejecutivo no tiene rol EJECUTIVO_TERRENO activo");
        }

        // Validación de fecha de compromiso con zona horaria de negocio
        if (tipo == TipoGestion.COMPROMISO_PAGO) {
            var fechaGestionLocal = cmd.fechaGestion().atZone(ZONA_NEGOCIO).toLocalDate();
            if (cmd.fechaCompromiso().isBefore(fechaGestionLocal)) {
                throw new IllegalArgumentException(
                    "La fecha de compromiso no puede ser anterior a la fecha de la gestión (zona horaria: America/Santiago)");
            }
        }

        if (origen == OrigenGestion.ASIGNACION_DIARIA) {
            validarAsignacionDiaria(nueva);
        } else {
            validarBusquedaDirecta(nueva);
        }

        // Inserción atómica: ON CONFLICT (id) DO NOTHING evita excepción de PK
        // bajo concurrencia sin necesidad de bloqueo explícito.
        int insertadas = gestionRepository.insertarSiNoExiste(
            nueva.getId(), nueva.getOrigenGestion().name(), nueva.getAsignacionDiariaId(),
            nueva.getPersonaId(), nueva.getEjecutivoId(), nueva.getTipoGestion().name(),
            nueva.getFechaGestion(), nueva.getObservacion(), nueva.getObservacionDireccion(),
            nueva.getLatitud(), nueva.getLongitud(), nueva.getPrecisionMetros(),
            nueva.getProveedorGps(), nueva.isUbicacionSimulada(), nueva.getFechaCapturaGps(),
            nueva.getFechaCompromiso(), nueva.getFechaCreacionServidor()
        );

        if (insertadas == 0) {
            // Concurrencia: otra solicitud con el mismo UUID ganó la carrera
            Gestion existente = gestionRepository.findById(nueva.getId())
                .orElseThrow(() -> new IllegalStateException(
                    "Conflicto de PK sin registro posterior: " + nueva.getId()));
            if (existente.tieneContenidoConflictivo(nueva)) {
                throw new GestionConflictivaException(nueva.getId());
            }
        }
    }

    private void validarAsignacionDiaria(Gestion g) {
        DatosAsignacionDiaria diaria = asignacionConsultaApi
            .findAsignacionDiaria(g.getAsignacionDiariaId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Asignación diaria no encontrada: " + g.getAsignacionDiariaId()));

        String estado = diaria.estado();
        if (!"PUBLICADA".equals(estado) && !"FINALIZADA".equals(estado)) {
            throw new IllegalStateException(
                "La asignación diaria debe estar PUBLICADA o FINALIZADA. Estado actual: " + estado);
        }
        if (!diaria.ejecutivoId().equals(g.getEjecutivoId())) {
            throw new IllegalArgumentException(
                "El ejecutivo no es el destinatario de la asignación diaria");
        }
        if (!asignacionConsultaApi.personaEnAsignacionDiaria(g.getPersonaId(), g.getAsignacionDiariaId())) {
            throw new IllegalArgumentException(
                "La persona no pertenece a la asignación diaria indicada");
        }
    }

    private void validarBusquedaDirecta(Gestion g) {
        if (!personaConsultaApi.existe(g.getPersonaId())) {
            throw new IllegalArgumentException(
                "Persona no encontrada: " + g.getPersonaId());
        }
    }
}
