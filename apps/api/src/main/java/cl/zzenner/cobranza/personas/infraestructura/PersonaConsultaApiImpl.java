package cl.zzenner.cobranza.personas.infraestructura;

import cl.zzenner.cobranza.personas.api.DatosAval;
import cl.zzenner.cobranza.personas.api.DatosDireccion;
import cl.zzenner.cobranza.personas.api.DatosPersona;
import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import cl.zzenner.cobranza.personas.dominio.Aval;
import cl.zzenner.cobranza.personas.dominio.Direccion;
import cl.zzenner.cobranza.personas.dominio.Persona;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
class PersonaConsultaApiImpl implements PersonaConsultaApi {

    private final PersonaRepository personaRepository;
    private final CarteraPersonaRepository carteraPersonaRepository;
    private final DireccionRepository direccionRepository;
    private final AvalRepository avalRepository;

    PersonaConsultaApiImpl(PersonaRepository personaRepository,
                           CarteraPersonaRepository carteraPersonaRepository,
                           DireccionRepository direccionRepository,
                           AvalRepository avalRepository) {
        this.personaRepository = personaRepository;
        this.carteraPersonaRepository = carteraPersonaRepository;
        this.direccionRepository = direccionRepository;
        this.avalRepository = avalRepository;
    }

    @Override
    public Optional<DatosPersona> findById(UUID id) {
        return personaRepository.findById(id).map(this::toDto);
    }

    @Override
    public boolean existe(UUID id) {
        return personaRepository.existsById(id);
    }

    @Override
    public Optional<DatosPersona> findByRut(String rutNumero, String rutDv) {
        return personaRepository.findByRutNumeroAndRutDv(rutNumero, rutDv).map(this::toDto);
    }

    @Override
    public boolean personaActivaEnCartera(UUID personaId, UUID carteraId) {
        return carteraPersonaRepository.existsByCarteraIdAndPersonaIdAndActivaTrue(carteraId, personaId);
    }

    @Override
    public List<DatosPersona> findAllByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return personaRepository.findAllById(ids).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<DatosDireccion> findDireccionesVigentesPorPersonas(Collection<UUID> personaIds) {
        if (personaIds.isEmpty()) return List.of();
        return direccionRepository.findByPersonaIdInAndVigenteTrue(personaIds).stream()
                .map(this::toDireccionDto)
                .toList();
    }

    @Override
    public List<DatosAval> findAvalesPorPersonas(Collection<UUID> personaIds) {
        if (personaIds.isEmpty()) return List.of();
        return avalRepository.findByPersonaIdIn(personaIds).stream()
                .map(this::toAvalDto)
                .toList();
    }

    @Override
    public List<UUID> findIdsByCarteraIdActiva(UUID carteraId, int limit) {
        return carteraPersonaRepository.findByCarteraIdAndActivaTrue(carteraId).stream()
                .limit(limit)
                .map(cp -> cp.getPersonaId())
                .collect(Collectors.toList());
    }

    private DatosPersona toDto(Persona p) {
        return new DatosPersona(p.getId(), p.getRut().getNumero(), p.getRut().getDv(), p.getNombre());
    }

    private DatosDireccion toDireccionDto(Direccion d) {
        return new DatosDireccion(
                d.getPersonaId(), d.getTipo().name(), d.getTexto(),
                d.getComuna(), d.getCiudad(), d.isVigente());
    }

    private DatosAval toAvalDto(Aval a) {
        return new DatosAval(
                a.getPersonaId(), a.getRut().getNumero(), a.getRut().getDv(), a.getNombre());
    }
}
