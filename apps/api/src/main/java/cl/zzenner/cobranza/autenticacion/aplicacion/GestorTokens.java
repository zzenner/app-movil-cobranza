package cl.zzenner.cobranza.autenticacion.aplicacion;

import cl.zzenner.cobranza.usuarios.api.CredencialesUsuario;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
public class GestorTokens {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private final JwtEncoder jwtEncoder;
    private final PropiedadesJwt propiedades;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public GestorTokens(JwtEncoder jwtEncoder, PropiedadesJwt propiedades, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public String emitirAccessToken(CredencialesUsuario usuario, UUID sesionId, UUID dispositivoId) {
        Instant ahora = clock.instant();
        Instant expira = ahora.plus(propiedades.duracionAccessToken());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.issuer())
                .audience(List.of(propiedades.audience()))
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expira)
                .id(UUID.randomUUID().toString())
                .claim("sid", sesionId.toString())
                .claim("did", dispositivoId.toString())
                .claim("preferred_username", usuario.getNombreUsuario())
                .claim("roles", usuario.getCodigosRoles())
                .claim("permisos", usuario.getCodigosPermisos())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /** Genera un refresh token opaco de 256 bits, Base64URL sin padding. */
    public String generarRefreshTokenCrudo() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex del token crudo. Solo el hash se almacena en DB. */
    public String hashearRefreshToken(String tokenCrudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(tokenCrudo.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public Instant calcularVencimientoRefreshToken(Instant ahora, Instant vencimientoAbsSesion) {
        Instant deslizante = ahora.plus(propiedades.duracionRefreshToken());
        return deslizante.isBefore(vencimientoAbsSesion) ? deslizante : vencimientoAbsSesion;
    }
}
