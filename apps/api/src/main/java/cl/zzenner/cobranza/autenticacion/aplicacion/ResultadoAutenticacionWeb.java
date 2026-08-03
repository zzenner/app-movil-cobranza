package cl.zzenner.cobranza.autenticacion.aplicacion;

import java.time.Instant;

/**
 * Resultado interno del flujo de autenticación web.
 * El controller decide cómo presentar cada campo:
 * accessToken en el body, refreshTokenCrudo en cookie HttpOnly.
 */
public record ResultadoAutenticacionWeb(
        String accessToken,
        String refreshTokenCrudo,
        long expiresInSeconds,
        Instant sessionExpiresAt,
        Instant refreshExpiresAt
) {}
