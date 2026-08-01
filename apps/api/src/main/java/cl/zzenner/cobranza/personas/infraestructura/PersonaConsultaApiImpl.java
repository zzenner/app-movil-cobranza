package cl.zzenner.cobranza.personas.infraestructura;

import cl.zzenner.cobranza.personas.api.DatosPersona;
import cl.zzenner.cobranza.personas.api.PersonaConsultaApi;
import cl.zzenner.cobranza.personas.dominio.Persona;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class PersonaConsultaApiImpl implements PersonaConsultaApi {

    private final PersonaRepository personaRepository;
    private final CarteraPersonaRepository carteraPersonaRepository;

    PersonaConsultaApiImpl(PersonaRepository personaRepository,
                           CarteraPersonaRepository carteraPersonaRepository) {
        this.personaRepository = personaRepository;
        this.carteraPersonaRepository = carteraPersonaRepository;
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

    private DatosPersona toDto(Persona p) {
        return new DatosPersona(
                p.getId(),
                p.getRut().getNumero(),
                p.getRut().getDv(),
                p.getNombre()
        );
    }
}
