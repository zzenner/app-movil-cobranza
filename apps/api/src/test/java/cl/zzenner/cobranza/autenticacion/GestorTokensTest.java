package cl.zzenner.cobranza.autenticacion;

import cl.zzenner.cobranza.autenticacion.aplicacion.GestorTokens;
import cl.zzenner.cobranza.autenticacion.aplicacion.PropiedadesJwt;
import cl.zzenner.cobranza.usuarios.api.CredencialesUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GestorTokensTest {

    static final PropiedadesJwt PROPIEDADES = new PropiedadesJwt(
            Duration.ofMinutes(15),
            Duration.ofDays(30),
            Duration.ofDays(90),
            5,
            Duration.ofMinutes(30),
            "cobranza-api-test",
            "cobranza-api",
            "test-kid",
            new PropiedadesJwt.Rsa("", "")
    );

    JwtEncoder encoder = mock(JwtEncoder.class);
    Clock clock = Clock.systemUTC();
    GestorTokens gestor;

    @BeforeEach
    void setup() {
        gestor = new GestorTokens(encoder, PROPIEDADES, clock);
    }

    @RepeatedTest(10)
    void generarRefreshTokenCrudo_produce_valores_distintos() {
        String t1 = gestor.generarRefreshTokenCrudo();
        String t2 = gestor.generarRefreshTokenCrudo();
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void generarRefreshTokenCrudo_tiene_longitud_correcta() {
        // 32 bytes en Base64URL sin padding = 43 caracteres
        String token = gestor.generarRefreshTokenCrudo();
        assertThat(token).hasSize(43);
    }

    @Test
    void hashearRefreshToken_produce_hex_de_64_chars() {
        String hash = gestor.hashearRefreshToken("token-de-prueba");
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void hashearRefreshToken_mismo_input_mismo_hash() {
        String h1 = gestor.hashearRefreshToken("abc");
        String h2 = gestor.hashearRefreshToken("abc");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void calcularVencimiento_usa_minimo_entre_deslizante_y_abs() {
        Instant ahora = Instant.now();
        Instant vencAbs = ahora.plusSeconds(3600);      // sesión expira en 1h
        Instant resultado = gestor.calcularVencimientoRefreshToken(ahora, vencAbs);

        // El deslizante es 30 días, que es mayor a 1h → debe usar el absoluto
        assertThat(resultado).isEqualTo(vencAbs);
    }

    @Test
    void calcularVencimiento_usa_deslizante_cuando_es_menor() {
        Instant ahora = Instant.now();
        Instant vencAbs = ahora.plus(Duration.ofDays(365)); // sesión larga
        Instant resultado = gestor.calcularVencimientoRefreshToken(ahora, vencAbs);

        // El deslizante (30 días) es menor que 1 año
        Instant deslizante = ahora.plus(Duration.ofDays(30));
        assertThat(resultado).isEqualTo(deslizante);
    }

    @Test
    void token_crudo_no_es_el_hash() {
        String crudo = gestor.generarRefreshTokenCrudo();
        String hash = gestor.hashearRefreshToken(crudo);
        assertThat(crudo).isNotEqualTo(hash);
    }

    @Test
    void credenciales_toString_no_contiene_hash() {
        CredencialesUsuario creds = new CredencialesUsuario(
                UUID.randomUUID(), "usuario.test", "$2a$12$hash_secreto",
                true, false, null, 0,
                List.of("ROL_SUPERVISOR"), List.of("GESTIONES_VER"));
        assertThat(creds.toString()).doesNotContain("$2a$12$hash_secreto");
        assertThat(creds.toString()).contains("usuario.test");
    }
}
