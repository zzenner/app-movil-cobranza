package cl.zzenner.cobranza.personas.api;

import java.util.UUID;

public record DatosPersona(
        UUID id,
        String rutNumero,
        String rutDv,
        String nombre,
        UUID carteraId
) {}
