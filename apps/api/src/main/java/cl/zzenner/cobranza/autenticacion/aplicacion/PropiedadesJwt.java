package cl.zzenner.cobranza.autenticacion.aplicacion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record PropiedadesJwt(
        Duration duracionAccessToken,
        Duration duracionRefreshToken,
        Duration duracionSesionAbsoluta,
        int maxIntentosFallidos,
        Duration duracionBloqueoTemporal,
        String issuer,
        String audience,
        String kid,
        Rsa rsa
) {
    public record Rsa(String clavePrivada, String clavePublica) {}
}
