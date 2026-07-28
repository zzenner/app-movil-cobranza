package cl.zzenner.cobranza.dispositivos.dominio;

import java.util.UUID;

public class DispositivoRevocadoException extends RuntimeException {
    public DispositivoRevocadoException(UUID id) {
        super("El dispositivo ya está revocado: " + id);
    }
    public DispositivoRevocadoException(String identificadorInstalacion) {
        super("El dispositivo ya está revocado: " + identificadorInstalacion);
    }
}
