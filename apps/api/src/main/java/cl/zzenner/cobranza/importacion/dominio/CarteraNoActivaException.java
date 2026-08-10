package cl.zzenner.cobranza.importacion.dominio;

import java.util.UUID;

public class CarteraNoActivaException extends RuntimeException {
    private final UUID carteraId;

    public CarteraNoActivaException(UUID carteraId) {
        super("La cartera no existe o no está activa: " + carteraId);
        this.carteraId = carteraId;
    }

    public UUID getCarteraId() { return carteraId; }
}
