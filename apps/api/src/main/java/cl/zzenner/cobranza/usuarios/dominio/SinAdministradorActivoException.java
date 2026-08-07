package cl.zzenner.cobranza.usuarios.dominio;

public class SinAdministradorActivoException extends RuntimeException {
    public SinAdministradorActivoException() {
        super("No se puede completar la operación: quedaría sin ningún administrador activo en el sistema");
    }
}
