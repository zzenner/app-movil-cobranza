package cl.zzenner.cobranza.dispositivos.api;

import java.util.UUID;

public class DispositivoNoValidoException extends RuntimeException {

    public DispositivoNoValidoException(UUID dispositivoId) {
        super("Dispositivo no válido o no autorizado: " + dispositivoId);
    }

    public DispositivoNoValidoException(String identificadorInstalacion) {
        super("Dispositivo no válido o no autorizado: " + identificadorInstalacion);
    }
}
