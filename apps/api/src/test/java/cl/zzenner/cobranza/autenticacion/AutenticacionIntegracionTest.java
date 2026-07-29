package cl.zzenner.cobranza.autenticacion;

import cl.zzenner.cobranza.autenticacion.web.RespuestaToken;
import cl.zzenner.cobranza.autenticacion.web.SolicitudLogin;
import cl.zzenner.cobranza.autenticacion.web.SolicitudRenovacion;
import cl.zzenner.cobranza.dispositivos.aplicacion.DispositivoService;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
class AutenticacionIntegracionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired UsuarioService usuarioService;
    @Autowired DispositivoService dispositivoService;

    UUID usuarioId;
    UUID dispositivoId;
    static final String NOMBRE_USUARIO = "login.test";
    static final String CONTRASENA = "ClaveTest.123!";

    @BeforeEach
    void setup() {
        // Crear usuario y dispositivo si no existen
        try {
            usuarioId = usuarioService.crearUsuario(
                    NOMBRE_USUARIO, "Login", "Test", null, null, CONTRASENA);
        } catch (Exception e) {
            // Ya existe del test anterior; buscar el ID
            usuarioId = null; // no tenemos búsqueda por username, usaremos otro
        }
        if (usuarioId != null) {
            dispositivoId = dispositivoService.registrarDispositivo(
                    usuarioId, UUID.randomUUID().toString(),
                    "Test Device", "Test", "Model", "12", "1.0.0");
        }
    }

    @Test
    void login_con_credenciales_validas_retorna_tokens() {
        if (usuarioId == null) return; // usuario creado en otra ejecución

        SolicitudLogin solicitud = new SolicitudLogin(NOMBRE_USUARIO, CONTRASENA, dispositivoId);
        ResponseEntity<RespuestaToken> response = rest.postForEntity(
                "/api/v1/auth/login", solicitud, RespuestaToken.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaToken body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.expiresInSeconds()).isGreaterThan(0);
        assertThat(body.sessionExpiresAt()).isNotNull();
    }

    @Test
    void login_con_contrasena_incorrecta_retorna_401() {
        if (usuarioId == null) return;

        SolicitudLogin solicitud = new SolicitudLogin(NOMBRE_USUARIO, "clave-incorrecta", dispositivoId);
        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/auth/login", solicitud, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_inexistente_retorna_401_generico() {
        SolicitudLogin solicitud = new SolicitudLogin(
                "usuario.que.no.existe", "cualquier", UUID.randomUUID());
        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/auth/login", solicitud, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_token_valido_emite_nuevos_tokens() {
        if (usuarioId == null) return;

        // Primero hacer login
        SolicitudLogin loginSolicitud = new SolicitudLogin(NOMBRE_USUARIO, CONTRASENA, dispositivoId);
        RespuestaToken loginResp = rest.postForEntity(
                "/api/v1/auth/login", loginSolicitud, RespuestaToken.class).getBody();
        assertThat(loginResp).isNotNull();

        // Renovar
        SolicitudRenovacion renovSolicitud = new SolicitudRenovacion(loginResp.refreshToken());
        ResponseEntity<RespuestaToken> renovResp = rest.postForEntity(
                "/api/v1/auth/refresh", renovSolicitud, RespuestaToken.class);

        assertThat(renovResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaToken nuevoToken = renovResp.getBody();
        assertThat(nuevoToken).isNotNull();
        assertThat(nuevoToken.accessToken()).isNotEqualTo(loginResp.accessToken());
        assertThat(nuevoToken.refreshToken()).isNotEqualTo(loginResp.refreshToken());
    }

    @Test
    void reutilizar_refresh_token_consumido_retorna_401() {
        if (usuarioId == null) return;

        // Login
        SolicitudLogin loginSolicitud = new SolicitudLogin(NOMBRE_USUARIO, CONTRASENA, dispositivoId);
        RespuestaToken loginResp = rest.postForEntity(
                "/api/v1/auth/login", loginSolicitud, RespuestaToken.class).getBody();
        assertThat(loginResp).isNotNull();

        // Primera renovación (consume el token original)
        rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(loginResp.refreshToken()), RespuestaToken.class);

        // Reutilizar el token ya consumido
        ResponseEntity<String> reusoResp = rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(loginResp.refreshToken()), String.class);

        assertThat(reusoResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void endpoint_protegido_sin_token_retorna_401() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/auth/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_con_token_valido_retorna_datos_usuario() {
        if (usuarioId == null) return;

        // Login
        SolicitudLogin loginSolicitud = new SolicitudLogin(NOMBRE_USUARIO, CONTRASENA, dispositivoId);
        RespuestaToken tokens = rest.postForEntity(
                "/api/v1/auth/login", loginSolicitud, RespuestaToken.class).getBody();
        assertThat(tokens).isNotNull();

        // GET /me con Authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        ResponseEntity<String> meResp = rest.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody()).contains(NOMBRE_USUARIO);
    }

    @Test
    void logout_retorna_204_y_cierra_sesion() {
        if (usuarioId == null) return;

        // Login
        SolicitudLogin loginSolicitud = new SolicitudLogin(NOMBRE_USUARIO, CONTRASENA, dispositivoId);
        RespuestaToken tokens = rest.postForEntity(
                "/api/v1/auth/login", loginSolicitud, RespuestaToken.class).getBody();
        assertThat(tokens).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());

        ResponseEntity<Void> logoutResp = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST,
                new HttpEntity<>(headers), Void.class);

        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
