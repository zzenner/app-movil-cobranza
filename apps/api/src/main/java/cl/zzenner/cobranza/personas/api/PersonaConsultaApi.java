package cl.zzenner.cobranza.personas.api;

import java.util.Optional;
import java.util.UUID;

public interface PersonaConsultaApi {

    Optional<DatosPersona> findById(UUID id);

    boolean existe(UUID id);

    Optional<DatosPersona> findByRut(String rutNumero, String rutDv);
}
