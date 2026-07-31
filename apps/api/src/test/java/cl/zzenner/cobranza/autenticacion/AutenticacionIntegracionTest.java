package cl.zzenner.cobranza.autenticacion;

import cl.zzenner.cobranza.autenticacion.web.RespuestaToken;
import cl.zzenner.cobranza.autenticacion.web.SolicitudLogin;
import cl.zzenner.cobranza.autenticacion.web.SolicitudRenovacion;
import cl.zzenner.cobranza.dispositivos.aplicacion.DispositivoService;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutenticacionIntegracionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired UsuarioService usuarioService;
    @Autowired DispositivoService dispositivoService;
    @Autowired UsuarioConsultaApi usuarioConsultaApi;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JdbcTemplate jdbc;

    static final String CONTRASENA = "ClaveTest.123!";

    UUID usuarioId;
    UUID dispositivoId;
    UUID inactivoId;
    UUID dispInactivoId;
    UUID bloqueadoAdminId;
    UUID dispBloqueadoAdminId;
    UUID tempBloqueadoId;
    UUID dispTempBloqueadoId;
    UUID revoId;
    UUID dispRevoId;
    UUID otroId;
    UUID dispOtroId;

    @BeforeAll
    void setupClase() {
        usuarioId = usuarioService.crearUsuario(
                "login.test", "Login", "Test", null, null, CONTRASENA);
        dispositivoId = dispositivoService.registrarDispositivo(
                usuarioId, UUID.randomUUID().toString(),
                "Device Principal", "Test", "Model", "12", "1.0.0");

        inactivoId = usuarioService.crearUsuario(
                "login.inactivo", "Inactivo", "Test", null, null, CONTRASENA);
        dispInactivoId = dispositivoService.registrarDispositivo(
                inactivoId, UUID.randomUUID().toString(),
                "Device Inactivo", "Test", "Model", "12", "1.0.0");
        jdbc.update("UPDATE cobranza.usuarios SET activo = false WHERE id = ?", inactivoId);

        bloqueadoAdminId = usuarioService.crearUsuario(
                "login.bloq.admin", "Bloq", "Admin", null, null, CONTRASENA);
        dispBloqueadoAdminId = dispositivoService.registrarDispositivo(
                bloqueadoAdminId, UUID.randomUUID().toString(),
                "Device BloqAdmin", "Test", "Model", "12", "1.0.0");
        jdbc.update("UPDATE cobranza.usuarios SET bloqueado = true WHERE id = ?", bloqueadoAdminId);

        tempBloqueadoId = usuarioService.crearUsuario(
                "login.bloq.temp", "Bloq", "Temp", null, null, CONTRASENA);
        dispTempBloqueadoId = dispositivoService.registrarDispositivo(
                tempBloqueadoId, UUID.randomUUID().toString(),
                "Device BloqTemp", "Test", "Model", "12", "1.0.0");
        usuarioConsultaApi.aplicarBloqueoTemporal(
                tempBloqueadoId, Instant.now().plus(Duration.ofHours(1)));

        revoId = usuarioService.crearUsuario(
                "login.revocado", "Revocado", "Test", null, null, CONTRASENA);
        dispRevoId = dispositivoService.registrarDispositivo(
                revoId, UUID.randomUUID().toString(),
                "Device Revocado", "Test", "Model", "12", "1.0.0");
        dispositivoService.revocarDispositivo(dispRevoId);

        otroId = usuarioService.crearUsuario(
                "login.otro", "Otro", "Usuario", null, null, CONTRASENA);
        dispOtroId = dispositivoService.registrarDispositivo(
                otroId, UUID.randomUUID().toString(),
                "Device Otro", "Test", "Model", "12", "1.0.0");
    }

    // ─── Login exitoso ────────────────────────────────────────────────────────────

    @Test
    void login_con_credenciales_validas_retorna_tokens() {
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, dispositivoId);
        ResponseEntity<RespuestaToken> resp = rest.postForEntity(
                "/api/v1/auth/login", req, RespuestaToken.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaToken body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.expiresInSeconds()).isGreaterThan(0);
        assertThat(body.sessionExpiresAt()).isNotNull();
    }

    // ─── Errores de login ─────────────────────────────────────────────────────────

    @Test
    void login_con_contrasena_incorrecta_retorna_401() {
        SolicitudLogin req = new SolicitudLogin("login.test", "clave-incorrecta", dispositivoId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_inexistente_retorna_401_generico() {
        SolicitudLogin req = new SolicitudLogin(
                "usuario.que.no.existe", "cualquier", UUID.randomUUID());
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_inactivo_retorna_401() {
        SolicitudLogin req = new SolicitudLogin("login.inactivo", CONTRASENA, dispInactivoId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_bloqueado_administrativamente_retorna_401() {
        SolicitudLogin req = new SolicitudLogin("login.bloq.admin", CONTRASENA, dispBloqueadoAdminId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_bloqueado_temporalmente_retorna_401() {
        SolicitudLogin req = new SolicitudLogin("login.bloq.temp", CONTRASENA, dispTempBloqueadoId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_dispositivo_revocado_retorna_401() {
        SolicitudLogin req = new SolicitudLogin("login.revocado", CONTRASENA, dispRevoId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_dispositivo_de_otro_usuario_retorna_409() {
        // dispOtroId pertenece a "login.otro"; intentar autenticar "login.test" con ese dispositivo
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, dispOtroId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ─── Segunda sesión cierra la anterior ───────────────────────────────────────

    @Test
    void segundo_login_mismo_dispositivo_cierra_sesion_anterior() {
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, dispositivoId);

        RespuestaToken sesion1 = rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class)
                .getBody();
        assertThat(sesion1).isNotNull();

        // Segundo login con el mismo par usuario-dispositivo: sesion1 queda CERRADA
        RespuestaToken sesion2 = rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class)
                .getBody();
        assertThat(sesion2).isNotNull();
        assertThat(sesion2.refreshToken()).isNotEqualTo(sesion1.refreshToken());

        // Refresh token de sesion1 fue REVOCADO
        ResponseEntity<String> reusoViejo = rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(sesion1.refreshToken()), String.class);
        assertThat(reusoViejo.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Refresh token de sesion2 sigue válido
        ResponseEntity<RespuestaToken> renovNueva = rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(sesion2.refreshToken()), RespuestaToken.class);
        assertThat(renovNueva.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ─── Refresh token ────────────────────────────────────────────────────────────

    @Test
    void refresh_token_valido_emite_nuevos_tokens() {
        RespuestaToken login = loginPrincipal();

        ResponseEntity<RespuestaToken> renov = rest.postForEntity(
                "/api/v1/auth/refresh",
                new SolicitudRenovacion(login.refreshToken()), RespuestaToken.class);

        assertThat(renov.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaToken nuevo = renov.getBody();
        assertThat(nuevo).isNotNull();
        assertThat(nuevo.accessToken()).isNotEqualTo(login.accessToken());
        assertThat(nuevo.refreshToken()).isNotEqualTo(login.refreshToken());
    }

    @Test
    void reutilizar_refresh_token_consumido_retorna_401() {
        RespuestaToken login = loginPrincipal();

        // Consumir el token original
        rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(login.refreshToken()), RespuestaToken.class);

        // Reutilización detectada → 401
        ResponseEntity<String> reuso = rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(login.refreshToken()), String.class);
        assertThat(reuso.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void hash_del_refresh_token_almacenado_en_bd_es_sha256() throws Exception {
        RespuestaToken tokens = loginPrincipal();

        // El token crudo devuelto tiene 43 chars (Base64URL sin padding, 32 bytes)
        assertThat(tokens.refreshToken()).hasSize(43);

        // Recuperar el hash almacenado en BD para la sesión activa del usuario
        String hashEnBd = jdbc.queryForObject(
                "SELECT rt.hash_token FROM cobranza.refresh_tokens rt"
                + " JOIN cobranza.sesiones_autenticacion s ON rt.sesion_id = s.id"
                + " WHERE s.usuario_id = ? AND s.estado = 'ACTIVA' AND rt.estado = 'ACTIVO'",
                String.class, usuarioId);

        // SHA-256 hex = 64 chars, distinto al token crudo
        assertThat(hashEnBd).hasSize(64).doesNotContain(tokens.refreshToken());

        // Verificar que es el SHA-256 correcto del token crudo
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expectedHash = HexFormat.of().formatHex(
                digest.digest(tokens.refreshToken().getBytes()));
        assertThat(hashEnBd).isEqualTo(expectedHash);
    }

    // ─── Endpoints protegidos y ProblemDetail ─────────────────────────────────────

    @Test
    void endpoint_protegido_sin_token_retorna_problem_detail_401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/auth/me", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        MediaType contentType = resp.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString()).contains("application/problem+json");
        assertThat(resp.getBody()).contains("status").contains("401");
    }

    // ─── /me ─────────────────────────────────────────────────────────────────────

    @Test
    void me_con_token_valido_retorna_datos_usuario() {
        RespuestaToken tokens = loginPrincipal();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("login.test");
    }

    // ─── JWT claims ───────────────────────────────────────────────────────────────

    @Test
    void jwt_access_token_contiene_claims_correctos() throws Exception {
        RespuestaToken tokens = loginPrincipal();

        // Parsear sin verificar firma para inspeccionar claims del payload
        JWTClaimsSet claims = SignedJWT.parse(tokens.accessToken()).getJWTClaimsSet();

        assertThat(claims.getIssuer()).isEqualTo("cobranza-api-dev");
        assertThat(claims.getAudience()).contains("cobranza-api");
        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(claims.getStringClaim("sid")).isNotBlank();
        assertThat(claims.getStringClaim("did")).isEqualTo(dispositivoId.toString());
        assertThat(claims.getStringClaim("preferred_username")).isEqualTo("login.test");
        assertThat((List<?>) claims.getClaim("roles")).isNotNull();
        assertThat((List<?>) claims.getClaim("permisos")).isNotNull();
        assertThat(claims.getExpirationTime()).isAfter(new Date());
    }

    // ─── Seguridad criptográfica ──────────────────────────────────────────────────

    @Test
    void token_con_firma_invalida_retorna_401() throws Exception {
        // Generar un par RSA distinto, ajeno al servidor de pruebas
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair parAjeno = gen.generateKeyPair();

        RSAKey rsaAjeno = new RSAKey.Builder((RSAPublicKey) parAjeno.getPublic())
                .privateKey((RSAPrivateKey) parAjeno.getPrivate())
                .keyID("ajeno-key")
                .build();
        JwtEncoder encoderAjeno = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaAjeno)));

        JwtClaimsSet claimsFalsos = JwtClaimsSet.builder()
                .issuer("cobranza-api-dev")
                .audience(List.of("cobranza-api"))
                .subject(usuarioId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString())
                .build();

        String tokenFalso = encoderAjeno.encode(JwtEncoderParameters.from(claimsFalsos))
                .getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFalso);
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void token_expirado_retorna_401() {
        // Emitir JWT firmado con clave correcta pero con exp en el pasado
        JwtClaimsSet claimsExpirado = JwtClaimsSet.builder()
                .issuer("cobranza-api-dev")
                .audience(List.of("cobranza-api"))
                .subject(usuarioId.toString())
                .issuedAt(Instant.now().minus(Duration.ofHours(2)))
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5)))
                .id(UUID.randomUUID().toString())
                .build();

        String tokenExpirado = jwtEncoder.encode(JwtEncoderParameters.from(claimsExpirado))
                .getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenExpirado);
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── Logout ───────────────────────────────────────────────────────────────────

    @Test
    void logout_retorna_204_y_cierra_sesion() {
        RespuestaToken tokens = loginPrincipal();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        ResponseEntity<Void> resp = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void logout_idempotente_doble_llamada_retorna_204() {
        RespuestaToken tokens = loginPrincipal();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());

        ResponseEntity<Void> primero = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        assertThat(primero.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // El JWT sigue siendo criptográficamente válido (15 min); el logout es idempotente
        ResponseEntity<Void> segundo = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        assertThat(segundo.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void excepcion_interna_con_token_valido_retorna_404_no_401_y_sin_stack_trace() {
        // Con token válido la solicitud pasa el filtro de seguridad; Spring MVC no encuentra
        // el handler y Tomcat re-despacha a /error como ERROR dispatch.
        // Sin dispatcherTypeMatchers(DispatcherType.ERROR).permitAll() en SeguridadConfig,
        // Spring Security bloquearía ese re-despacho y devolvería 401 en lugar de 404.
        RespuestaToken tokens = loginPrincipal();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());

        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/ruta-que-no-existe-" + UUID.randomUUID(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Sin stack trace en el cuerpo
        String cuerpo = resp.getBody();
        if (cuerpo != null) {
            assertThat(cuerpo).doesNotContain("java.", "Exception", "at cl.", "Caused by");
        }
    }

    // ─── Utilidad ─────────────────────────────────────────────────────────────────

    private RespuestaToken loginPrincipal() {
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, dispositivoId);
        ResponseEntity<RespuestaToken> resp = rest.postForEntity(
                "/api/v1/auth/login", req, RespuestaToken.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        return resp.getBody();
    }
}
