package cl.zzenner.cobranza.asignaciones.dominio;

import java.util.UUID;

public class PersonaYaEnAsignacionException extends RuntimeException {
    public PersonaYaEnAsignacionException(UUID personaId, UUID asignacionId) {
        super("La persona " + personaId + " ya está en la asignación " + asignacionId);
    }
}
