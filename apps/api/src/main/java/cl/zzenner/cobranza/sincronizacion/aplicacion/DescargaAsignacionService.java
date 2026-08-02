package cl.zzenner.cobranza.sincronizacion.aplicacion;

import cl.zzenner.cobranza.asignaciones.api.AsignacionConsultaApi;
import cl.zzenner.cobranza.asignaciones.api.DatosAsignacionDiaria;
import cl.zzenner.cobranza.gestiones.api.DatosGestion;
import cl.zzenner.cobranza.gestiones.api.GestionConsultaApi;
import cl.zzenner.cobranza.operaciones.api.DatosOperacion;
import cl.zzenner.cobranza.operaciones.api.OperacionConsultaApi;
import cl.zzenner.cobranza.personas.api.DatosAval;
import cl.zzenner.cobranza.personas.api.DatosDireccion;
import cl.zzenner.cobranza.personas.api.DatosPersona;
import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import cl.zzenner.cobranza.sincronizacion.web.DatosPersonaDescarga;
import cl.zzenner.cobranza.sincronizacion.web.RespuestaAsignacionDiaria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DescargaAsignacionService {

    private static final int LIMITE_GESTIONES = 10;

    private final AsignacionConsultaApi asignacionConsultaApi;
    private final PersonaConsultaApi personaConsultaApi;
    private final OperacionConsultaApi operacionConsultaApi;
    private final GestionConsultaApi gestionConsultaApi;
    private final Clock clock;

    public DescargaAsignacionService(AsignacionConsultaApi asignacionConsultaApi,
                                     PersonaConsultaApi personaConsultaApi,
                                     OperacionConsultaApi operacionConsultaApi,
                                     GestionConsultaApi gestionConsultaApi,
                                     Clock clock) {
        this.asignacionConsultaApi = asignacionConsultaApi;
        this.personaConsultaApi    = personaConsultaApi;
        this.operacionConsultaApi  = operacionConsultaApi;
        this.gestionConsultaApi    = gestionConsultaApi;
        this.clock                 = clock;
    }

    public Optional<RespuestaAsignacionDiaria> descargarAsignacionDiariaActiva(UUID ejecutivoId, LocalDate fecha) {
        Optional<DatosAsignacionDiaria> opt = asignacionConsultaApi.findAsignacionDiariaPublicada(ejecutivoId, fecha);
        if (opt.isEmpty()) return Optional.empty();

        DatosAsignacionDiaria diaria = opt.get();
        List<UUID> personaIds = asignacionConsultaApi.findPersonasEnAsignacionDiaria(diaria.id());

        if (personaIds.isEmpty()) {
            return Optional.of(new RespuestaAsignacionDiaria(
                diaria.id(), diaria.ejecutivoId(), diaria.fecha(), diaria.estado(), List.of()));
        }

        // 6 queries IN en paralelo lógico — cada una es O(n) y evita N+1
        List<DatosPersona>     personas    = personaConsultaApi.findAllByIds(personaIds);
        List<DatosDireccion>   direcciones = personaConsultaApi.findDireccionesVigentesPorPersonas(personaIds);
        List<DatosAval>        avales      = personaConsultaApi.findAvalesPorPersonas(personaIds);
        List<DatosOperacion>   operaciones = operacionConsultaApi.findOperacionesActivasConCuotas(personaIds);
        List<DatosGestion>     gestiones   = gestionConsultaApi.findUltimasGestionesPorPersonas(personaIds, LIMITE_GESTIONES);

        Map<UUID, DatosPersona>           personaMap  = personas.stream()
            .collect(Collectors.toMap(DatosPersona::id, p -> p));
        Map<UUID, List<DatosDireccion>>   dirMap      = direcciones.stream()
            .collect(Collectors.groupingBy(DatosDireccion::personaId));
        Map<UUID, List<DatosAval>>        avalMap     = avales.stream()
            .collect(Collectors.groupingBy(DatosAval::personaId));
        Map<UUID, List<DatosOperacion>>   opMap       = operaciones.stream()
            .collect(Collectors.groupingBy(DatosOperacion::personaId));
        Map<UUID, List<DatosGestion>>     gestMap     = gestiones.stream()
            .collect(Collectors.groupingBy(DatosGestion::personaId));

        List<DatosPersonaDescarga> bundle = personaIds.stream()
            .map(pid -> {
                DatosPersona p = personaMap.get(pid);
                if (p == null) return null;
                return new DatosPersonaDescarga(
                    p.id(), p.rutNumero(), p.rutDv(), p.nombre(),
                    dirMap.getOrDefault(pid, List.of()),
                    avalMap.getOrDefault(pid, List.of()),
                    opMap.getOrDefault(pid, List.of()),
                    gestMap.getOrDefault(pid, List.of())
                );
            })
            .filter(Objects::nonNull)
            .toList();

        return Optional.of(new RespuestaAsignacionDiaria(
            diaria.id(), diaria.ejecutivoId(), diaria.fecha(), diaria.estado(), bundle));
    }

    public Clock getClock() { return clock; }
}
