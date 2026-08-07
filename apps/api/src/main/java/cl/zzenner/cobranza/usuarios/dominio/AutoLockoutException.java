package cl.zzenner.cobranza.usuarios.dominio;

public class AutoLockoutException extends RuntimeException {
    public AutoLockoutException() {
        super("No se puede aplicar esta operación a la propia cuenta del administrador");
    }
}
