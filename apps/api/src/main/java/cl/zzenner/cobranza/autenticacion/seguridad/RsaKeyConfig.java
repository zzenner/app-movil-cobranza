package cl.zzenner.cobranza.autenticacion.seguridad;

import cl.zzenner.cobranza.autenticacion.aplicacion.PropiedadesJwt;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Carga las claves RSA desde el sistema de archivos local.
 * En tests, reemplazar con @TestConfiguration que provea JwtDecoder y JwtEncoder en memoria.
 */
@Configuration
class RsaKeyConfig {

    private final PropiedadesJwt propiedades;

    RsaKeyConfig(PropiedadesJwt propiedades) {
        this.propiedades = propiedades;
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder() {
        RSAPublicKey publicKey = cargarPublica();

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(propiedades.issuer()),
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(propiedades.audience()))
        ));

        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(JwtEncoder.class)
    JwtEncoder jwtEncoder() {
        RSAPublicKey publicKey = cargarPublica();
        RSAPrivateKey privateKey = cargarPrivada();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(propiedades.kid())
                .build();

        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    private RSAPublicKey cargarPublica() {
        String ruta = propiedades.rsa().clavePublica();
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.rsa.clave-publica no configurada. Establezca JWT_RSA_PUBLICA_PATH.");
        }
        return CargadorClavesRsa.cargarPublica(ruta);
    }

    private RSAPrivateKey cargarPrivada() {
        String ruta = propiedades.rsa().clavePrivada();
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.rsa.clave-privada no configurada. Establezca JWT_RSA_PRIVADA_PATH.");
        }
        return CargadorClavesRsa.cargarPrivada(ruta);
    }
}
