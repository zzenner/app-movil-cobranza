package cl.zzenner.cobranza.personas.dominio;

import java.util.UUID;

public class VinculoYaActivoException extends RuntimeException {
    public VinculoYaActivoException(UUID personaId, UUID carteraId) {
        super("Ya existe un vínculo activo entre la persona " + personaId
                + " y la cartera " + carteraId);
    }
}
