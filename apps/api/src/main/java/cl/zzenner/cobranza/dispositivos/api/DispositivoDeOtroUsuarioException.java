package cl.zzenner.cobranza.dispositivos.api;

import java.util.UUID;

public class DispositivoDeOtroUsuarioException extends RuntimeException {

    public DispositivoDeOtroUsuarioException(UUID dispositivoId) {
        super("El dispositivo ya está asociado a otro usuario: " + dispositivoId);
    }

    public DispositivoDeOtroUsuarioException(String identificadorInstalacion) {
        super("El identificador de instalación ya está asociado a otro usuario: " + identificadorInstalacion);
    }
}
