package cl.zzenner.cobranza.personas.api;

import java.util.UUID;

public record DatosAval(
        UUID personaId,
        String rutNumero,
        String rutDv,
        String nombre
) {}
