package cl.zzenner.cobranza.asignaciones.dominio;

import java.util.UUID;

public class PersonaFueraDeAsignacionMensualException extends RuntimeException {
    public PersonaFueraDeAsignacionMensualException(UUID personaId, UUID asignacionMensualId) {
        super("La persona " + personaId + " no pertenece a la asignación mensual " + asignacionMensualId);
    }
}
