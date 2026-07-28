package cl.zzenner.cobranza.dispositivos.dominio;

public class DispositivoDuplicadoException extends RuntimeException {
    public DispositivoDuplicadoException(String identificadorInstalacion) {
        super("El dispositivo ya está registrado: " + identificadorInstalacion);
    }
}
