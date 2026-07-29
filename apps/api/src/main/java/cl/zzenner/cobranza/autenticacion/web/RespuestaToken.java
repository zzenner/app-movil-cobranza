package cl.zzenner.cobranza.autenticacion.web;

import java.time.Instant;

public record RespuestaToken(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        Instant sessionExpiresAt
) {}
