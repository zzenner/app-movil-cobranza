package cl.zzenner.cobranza.personas.dominio;

import java.util.UUID;

public class VinculoNoEncontradoException extends RuntimeException {
    public VinculoNoEncontradoException(UUID personaId, UUID carteraId) {
        super("No existe un vínculo activo entre la persona " + personaId
                + " y la cartera " + carteraId);
    }
}
