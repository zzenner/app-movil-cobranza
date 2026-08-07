package cl.zzenner.cobranza.usuarios.api;

import java.util.UUID;

public record SeguridadUsuarioModificadaEvent(UUID usuarioId, Motivo motivo) {

    public enum Motivo {
        DESACTIVACION,
        BLOQUEO_ADMINISTRATIVO,
        RESTABLECIMIENTO_CONTRASENA
    }
}
