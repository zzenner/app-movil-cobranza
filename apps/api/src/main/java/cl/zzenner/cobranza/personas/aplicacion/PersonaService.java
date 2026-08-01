package cl.zzenner.cobranza.personas.aplicacion;

import cl.zzenner.cobranza.carteras.api.CarteraConsultaApi;
import cl.zzenner.cobranza.carteras.api.CarteraNoEncontradaException;
import cl.zzenner.cobranza.personas.dominio.*;
import cl.zzenner.cobranza.personas.infraestructura.AvalRepository;
import cl.zzenner.cobranza.personas.infraestructura.CarteraPersonaRepository;
import cl.zzenner.cobranza.personas.infraestructura.DireccionRepository;
import cl.zzenner.cobranza.personas.infraestructura.PersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final AvalRepository avalRepository;
    private final DireccionRepository direccionRepository;
    private final CarteraPersonaRepository carteraPersonaRepository;
    private final CarteraConsultaApi carteraConsultaApi;

    public PersonaService(PersonaRepository personaRepository,
                          AvalRepository avalRepository,
                          DireccionRepository direccionRepository,
                          CarteraPersonaRepository carteraPersonaRepository,
                          CarteraConsultaApi carteraConsultaApi) {
        this.personaRepository = personaRepository;
        this.avalRepository = avalRepository;
        this.direccionRepository = direccionRepository;
        this.carteraPersonaRepository = carteraPersonaRepository;
        this.carteraConsultaApi = carteraConsultaApi;
    }

    public Persona upsertPersona(Rut rut, String nombre, String sistemaOrigen,
                                 String codigoExterno, Instant fechaActualizacionOrigen) {
        return personaRepository.findByRutNumeroAndRutDv(rut.getNumero(), rut.getDv())
                .map(p -> {
                    p.setNombre(nombre);
                    p.setCodigoExterno(codigoExterno);
                    p.registrarImportacion(fechaActualizacionOrigen);
                    return personaRepository.save(p);
                })
                .orElseGet(() -> {
                    Persona nueva = new Persona(rut, nombre, sistemaOrigen);
                    nueva.setCodigoExterno(codigoExterno);
                    nueva.registrarImportacion(fechaActualizacionOrigen);
                    return personaRepository.save(nueva);
                });
    }

    public void vincularCartera(UUID personaId, UUID carteraId, LocalDate fechaInicio) {
        if (!personaRepository.existsById(personaId)) {
            throw new PersonaNoEncontradaException(personaId);
        }
        if (!carteraConsultaApi.existeActiva(carteraId)) {
            throw new CarteraNoEncontradaException(carteraId);
        }
        if (carteraPersonaRepository.existsByCarteraIdAndPersonaIdAndActivaTrue(carteraId, personaId)) {
            throw new VinculoYaActivoException(personaId, carteraId);
        }
        carteraPersonaRepository.save(new CarteraPersona(carteraId, personaId, fechaInicio));
    }

    public void cerrarVinculo(UUID personaId, UUID carteraId, LocalDate fechaFin) {
        CarteraPersona vinculo = carteraPersonaRepository
                .findByCarteraIdAndPersonaIdAndActivaTrue(carteraId, personaId)
                .orElseThrow(() -> new VinculoNoEncontradoException(personaId, carteraId));
        vinculo.cerrar(fechaFin);
        carteraPersonaRepository.save(vinculo);
    }

    public List<UUID> consultarCarterasActivas(UUID personaId) {
        return carteraPersonaRepository.findByPersonaIdAndActivaTrue(personaId)
                .stream()
                .map(CarteraPersona::getCarteraId)
                .toList();
    }

    public List<UUID> consultarPersonasActivas(UUID carteraId) {
        return carteraPersonaRepository.findByCarteraIdAndActivaTrue(carteraId)
                .stream()
                .map(CarteraPersona::getPersonaId)
                .toList();
    }

    public Direccion registrarDireccion(UUID personaId, TipoDireccion tipo, String texto,
                                        String sistemaOrigen, String codigoExterno) {
        if (!personaRepository.existsById(personaId)) {
            throw new PersonaNoEncontradaException(personaId);
        }
        Direccion d = new Direccion(personaId, tipo, texto, sistemaOrigen);
        d.setCodigoExterno(codigoExterno);
        return direccionRepository.save(d);
    }

    public void marcarDireccionPrincipal(UUID personaId, UUID direccionId) {
        direccionRepository.desmarcarPrincipalDePersona(personaId);
        Direccion d = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Direccion no encontrada: " + direccionId));
        if (!d.getPersonaId().equals(personaId)) {
            throw new IllegalArgumentException("La dirección no pertenece a la persona indicada");
        }
        d.marcarPrincipal();
        direccionRepository.save(d);
    }

    public Aval registrarAval(UUID personaId, Rut rutAval, String nombre,
                              String sistemaOrigen, String codigoExterno) {
        if (!personaRepository.existsById(personaId)) {
            throw new PersonaNoEncontradaException(personaId);
        }
        Aval aval = new Aval(personaId, rutAval, nombre, sistemaOrigen);
        aval.setCodigoExterno(codigoExterno);
        return avalRepository.save(aval);
    }
}
