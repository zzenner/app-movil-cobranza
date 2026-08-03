package cl.zzenner.cobranza.autenticacion.web;

import java.time.Instant;

public record RespuestaLoginWeb(
        String accessToken,
        long expiresInSeconds,
        Instant sessionExpiresAt
) {}
