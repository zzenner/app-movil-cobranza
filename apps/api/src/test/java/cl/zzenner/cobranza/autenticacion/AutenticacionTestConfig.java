package cl.zzenner.cobranza.autenticacion;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Configuración de pruebas para autenticación.
 * Genera un par RSA 2048-bit en memoria; nunca toca el sistema de archivos.
 * Usar con @Import(AutenticacionTestConfig.class) en pruebas de integración.
 */
@TestConfiguration
public class AutenticacionTestConfig {

    static final KeyPair RSA_KEY_PAIR = generarPar();

    private static KeyPair generarPar() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar par RSA en memoria", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) RSA_KEY_PAIR.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) RSA_KEY_PAIR.getPublic())
                .privateKey((RSAPrivateKey) RSA_KEY_PAIR.getPrivate())
                .keyID("test-key")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }
}
