package cl.zzenner.cobranza.sincronizacion.aplicacion;

import cl.zzenner.cobranza.gestiones.api.DatosGestion;
import cl.zzenner.cobranza.gestiones.api.GestionConsultaApi;
import cl.zzenner.cobranza.operaciones.api.DatosOperacion;
import cl.zzenner.cobranza.operaciones.api.OperacionConsultaApi;
import cl.zzenner.cobranza.personas.api.DatosAval;
import cl.zzenner.cobranza.personas.api.DatosDireccion;
import cl.zzenner.cobranza.personas.api.DatosPersona;
import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import cl.zzenner.cobranza.personas.api.PersonaNoEncontradaException;
import cl.zzenner.cobranza.personas.api.RutValidacionApi;
import cl.zzenner.cobranza.sincronizacion.web.DatosPersonaDescarga;
import cl.zzenner.cobranza.sincronizacion.web.RespuestaBusquedaPersona;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BusquedaPersonaService {

    private static final Logger log = LoggerFactory.getLogger(BusquedaPersonaService.class);
    private static final int LIMITE_GESTIONES = 10;

    private final RutValidacionApi rutValidacionApi;
    private final PersonaConsultaApi personaConsultaApi;
    private final OperacionConsultaApi operacionConsultaApi;
    private final GestionConsultaApi gestionConsultaApi;

    public BusquedaPersonaService(RutValidacionApi rutValidacionApi,
                                   PersonaConsultaApi personaConsultaApi,
                                   OperacionConsultaApi operacionConsultaApi,
                                   GestionConsultaApi gestionConsultaApi) {
        this.rutValidacionApi    = rutValidacionApi;
        this.personaConsultaApi  = personaConsultaApi;
        this.operacionConsultaApi = operacionConsultaApi;
        this.gestionConsultaApi  = gestionConsultaApi;
    }

    public RespuestaBusquedaPersona buscar(String rutNumero, String rutDv, UUID ejecutivoId, String correlationId) {
        if (!rutValidacionApi.esValido(rutNumero, rutDv)) {
            log.warn("[BUSQUEDA_AUDITORIA] ejecutivoId={} resultado=RUT_INVALIDO correlationId={}",
                    ejecutivoId, correlationId);
            throw new RutInvalidoEnBusquedaException("El RUT proporcionado no es válido");
        }

        String numNorm = rutNumero.trim();
        String dvNorm  = rutDv.trim().toUpperCase();

        DatosPersona persona = personaConsultaApi.findByRut(numNorm, dvNorm)
                .orElseThrow(() -> {
                    log.info("[BUSQUEDA_AUDITORIA] ejecutivoId={} resultado=NO_ENCONTRADA correlationId={}",
                            ejecutivoId, correlationId);
                    return new PersonaNoEncontradaException(numNorm, dvNorm);
                });

        List<UUID> ids = List.of(persona.id());
        List<DatosDireccion> direcciones = personaConsultaApi.findDireccionesVigentesPorPersonas(ids);
        List<DatosAval>      avales      = personaConsultaApi.findAvalesPorPersonas(ids);
        List<DatosOperacion> operaciones = operacionConsultaApi.findOperacionesActivasConCuotas(ids);
        List<DatosGestion>   gestiones   = gestionConsultaApi.findUltimasGestiones(persona.id(), LIMITE_GESTIONES);

        log.info("[BUSQUEDA_AUDITORIA] ejecutivoId={} resultado=ENCONTRADA personaId={} correlationId={}",
                ejecutivoId, persona.id(), correlationId);

        DatosPersonaDescarga detalle = new DatosPersonaDescarga(
                persona.id(), persona.rutNumero(), persona.rutDv(), persona.nombre(),
                direcciones, avales, operaciones, gestiones);

        return new RespuestaBusquedaPersona(1, Instant.now(), detalle);
    }
}
