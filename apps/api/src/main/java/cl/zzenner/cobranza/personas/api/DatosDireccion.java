package cl.zzenner.cobranza.personas.api;

import java.util.UUID;

public record DatosDireccion(
        UUID personaId,
        String tipo,
        String texto,
        String comuna,
        String ciudad,
        boolean vigente
) {}
