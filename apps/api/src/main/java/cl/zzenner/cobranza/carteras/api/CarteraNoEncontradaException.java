package cl.zzenner.cobranza.carteras.api;

import java.util.UUID;

public class CarteraNoEncontradaException extends RuntimeException {

    public CarteraNoEncontradaException(UUID id) {
        super("Cartera no encontrada: " + id);
    }
}
