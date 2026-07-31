package cl.zzenner.cobranza.carteras.api;

import java.util.UUID;

public record DatosCartera(UUID id, String nombre, boolean activa) {}
