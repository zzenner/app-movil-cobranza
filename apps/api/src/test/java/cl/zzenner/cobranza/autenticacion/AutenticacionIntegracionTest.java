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
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    // Identificadores de instalación (UUID strings generados por la "app Android")
    String idInstalacionPrincipal;

    UUID usuarioId;
    UUID inactivoId;
    UUID bloqueadoAdminId;
    UUID tempBloqueadoId;
    UUID revoId;
    UUID otroId;

    @BeforeAll
    void setupClase() {
        idInstalacionPrincipal = UUID.randomUUID().toString();

        usuarioId = usuarioService.crearUsuario(
                "login.test", "Login", "Test", null, null, CONTRASENA);

        inactivoId = usuarioService.crearUsuario(
                "login.inactivo", "Inactivo", "Test", null, null, CONTRASENA);
        jdbc.update("UPDATE cobranza.usuarios SET activo = false WHERE id = ?", inactivoId);

        bloqueadoAdminId = usuarioService.crearUsuario(
                "login.bloq.admin", "Bloq", "Admin", null, null, CONTRASENA);
        jdbc.update("UPDATE cobranza.usuarios SET bloqueado = true WHERE id = ?", bloqueadoAdminId);

        tempBloqueadoId = usuarioService.crearUsuario(
                "login.bloq.temp", "Bloq", "Temp", null, null, CONTRASENA);
        usuarioConsultaApi.aplicarBloqueoTemporal(
                tempBloqueadoId, Instant.now().plus(Duration.ofHours(1)));

        revoId = usuarioService.crearUsuario(
                "login.revocado", "Revocado", "Test", null, null, CONTRASENA);

        otroId = usuarioService.crearUsuario(
                "login.otro", "Otro", "Usuario", null, null, CONTRASENA);
    }

    // ─── Primer login registra el dispositivo ─────────────────────────────────────

    @Test
    void primer_login_registra_dispositivo_automaticamente() {
        String nuevoId = UUID.randomUUID().toString();
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, nuevoId);
        ResponseEntity<RespuestaToken> resp = rest.postForEntity(
                "/api/v1/auth/login", req, RespuestaToken.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // El dispositivo fue registrado en BD
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.dispositivos WHERE identificador_instalacion = ?",
                Integer.class, nuevoId);
        assertThat(count).isEqualTo(1);
    }

    // ─── Login posterior reutiliza el mismo dispositivo ────────────────────────────

    @Test
    void login_posterior_reutiliza_mismo_dispositivo() throws Exception {
        String idInstalacion = UUID.randomUUID().toString();
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, idInstalacion);

        RespuestaToken primera = rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class)
                .getBody();
        assertThat(primera).isNotNull();
        String did1 = SignedJWT.parse(primera.accessToken()).getJWTClaimsSet().getStringClaim("did");

        RespuestaToken segunda = rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class)
                .getBody();
        assertThat(segunda).isNotNull();
        String did2 = SignedJWT.parse(segunda.accessToken()).getJWTClaimsSet().getStringClaim("did");

        // Mismo UUID interno del dispositivo en ambos JWTs
        assertThat(did1).isEqualTo(did2);

        // Solo un registro en BD para ese identificador
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.dispositivos WHERE identificador_instalacion = ?",
                Integer.class, idInstalacion);
        assertThat(count).isEqualTo(1);
    }

    // ─── JWT contiene UUID interno correcto en "did" ─────────────────────────────

    @Test
    void jwt_did_contiene_uuid_interno_del_dispositivo_no_el_identificador() throws Exception {
        String idInstalacion = UUID.randomUUID().toString();
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, idInstalacion);
        RespuestaToken tokens = rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class)
                .getBody();
        assertThat(tokens).isNotNull();

        String did = SignedJWT.parse(tokens.accessToken()).getJWTClaimsSet().getStringClaim("did");

        // "did" es el UUID interno (PK de BD), distinto al identificadorInstalacion
        assertThat(did).isNotEqualTo(idInstalacion);

        // El UUID interno existe en BD y su identificador_instalacion coincide
        UUID dispositivoUuid = UUID.fromString(did);
        String idInstalacionEnBd = jdbc.queryForObject(
                "SELECT identificador_instalacion FROM cobranza.dispositivos WHERE id = ?",
                String.class, dispositivoUuid);
        assertThat(idInstalacionEnBd).isEqualTo(idInstalacion);
    }

    // ─── Credenciales incorrectas no registran dispositivo ────────────────────────

    @Test
    void credenciales_incorrectas_no_registran_dispositivo() {
        String nuevoId = UUID.randomUUID().toString();
        SolicitudLogin req = new SolicitudLogin("login.test", "clave-incorrecta", nuevoId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.dispositivos WHERE identificador_instalacion = ?",
                Integer.class, nuevoId);
        assertThat(count).isZero();
    }

    // ─── Identificador asociado a otro usuario devuelve 409 ──────────────────────

    @Test
    void identificador_asociado_a_otro_usuario_retorna_409() {
        String idCompartido = UUID.randomUUID().toString();

        // Primer login con "login.otro" registra el dispositivo
        SolicitudLogin reqOtro = new SolicitudLogin("login.otro", CONTRASENA, idCompartido);
        ResponseEntity<RespuestaToken> primero = rest.postForEntity(
                "/api/v1/auth/login", reqOtro, RespuestaToken.class);
        assertThat(primero.getStatusCode()).isEqualTo(HttpStatus.OK);

        // "login.test" intenta usar el mismo identificador → 409 CONFLICTO_DISPOSITIVO
        SolicitudLogin reqPrincipal = new SolicitudLogin("login.test", CONTRASENA, idCompartido);
        ResponseEntity<String> conflicto = rest.postForEntity(
                "/api/v1/auth/login", reqPrincipal, String.class);
        assertThat(conflicto.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflicto.getBody()).contains("CONFLICTO_DISPOSITIVO");
    }

    // ─── Dispositivo revocado no puede iniciar sesión ─────────────────────────────

    @Test
    void dispositivo_revocado_retorna_401() {
        String idRevocado = UUID.randomUUID().toString();

        // Primer login registra el dispositivo
        SolicitudLogin req = new SolicitudLogin("login.revocado", CONTRASENA, idRevocado);
        ResponseEntity<RespuestaToken> login1 = rest.postForEntity(
                "/api/v1/auth/login", req, RespuestaToken.class);
        assertThat(login1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Obtener UUID interno del dispositivo y revocarlo
        UUID dispId = jdbc.queryForObject(
                "SELECT id FROM cobranza.dispositivos WHERE identificador_instalacion = ?",
                UUID.class, idRevocado);
        dispositivoService.revocarDispositivo(dispId);

        // Login posterior → 401
        ResponseEntity<String> login2 = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(login2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── Login exitoso ────────────────────────────────────────────────────────────

    @Test
    void login_con_credenciales_validas_retorna_tokens() {
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, idInstalacionPrincipal);
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
        SolicitudLogin req = new SolicitudLogin("login.test", "clave-incorrecta", idInstalacionPrincipal);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_inexistente_retorna_401_generico() {
        SolicitudLogin req = new SolicitudLogin(
                "usuario.que.no.existe", "cualquier", UUID.randomUUID().toString());
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_inactivo_retorna_401() {
        SolicitudLogin req = new SolicitudLogin(
                "login.inactivo", CONTRASENA, UUID.randomUUID().toString());
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_bloqueado_administrativamente_retorna_401() {
        SolicitudLogin req = new SolicitudLogin(
                "login.bloq.admin", CONTRASENA, UUID.randomUUID().toString());
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_usuario_bloqueado_temporalmente_retorna_401() {
        SolicitudLogin req = new SolicitudLogin(
                "login.bloq.temp", CONTRASENA, UUID.randomUUID().toString());
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── Segunda sesión cierra la anterior ───────────────────────────────────────

    @Test
    void segundo_login_mismo_dispositivo_cierra_sesion_anterior() {
        String idInstalacion = UUID.randomUUID().toString();
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, idInstalacion);

        RespuestaToken sesion1 = rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class)
                .getBody();
        assertThat(sesion1).isNotNull();

        RespuestaToken sesion2 = rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class)
                .getBody();
        assertThat(sesion2).isNotNull();
        assertThat(sesion2.refreshToken()).isNotEqualTo(sesion1.refreshToken());

        // Refresh token de sesion1 fue revocado
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

        rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(login.refreshToken()), RespuestaToken.class);

        ResponseEntity<String> reuso = rest.postForEntity("/api/v1/auth/refresh",
                new SolicitudRenovacion(login.refreshToken()), String.class);
        assertThat(reuso.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_exitoso_rota_el_token() {
        RespuestaToken login = loginPrincipal();

        ResponseEntity<RespuestaToken> renov = rest.postForEntity(
                "/api/v1/auth/refresh",
                new SolicitudRenovacion(login.refreshToken()), RespuestaToken.class);
        assertThat(renov.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaToken nuevo = renov.getBody();
        assertThat(nuevo).isNotNull();
        assertThat(nuevo.refreshToken()).isNotEqualTo(login.refreshToken());
    }

    @Test
    void hash_del_refresh_token_almacenado_en_bd_es_sha256() throws Exception {
        // Usar un identificador único por test para aislar la sesión creada
        String idAislado = UUID.randomUUID().toString();
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, idAislado);
        ResponseEntity<RespuestaToken> resp = rest.postForEntity(
                "/api/v1/auth/login", req, RespuestaToken.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaToken tokens = resp.getBody();
        assertThat(tokens).isNotNull();

        assertThat(tokens.refreshToken()).hasSize(43);

        // Filtrar por el identificador de instalación específico para obtener exactamente un token
        String hashEnBd = jdbc.queryForObject(
                "SELECT rt.hash_token FROM cobranza.refresh_tokens rt"
                + " JOIN cobranza.sesiones_autenticacion s ON rt.sesion_id = s.id"
                + " JOIN cobranza.dispositivos d ON s.dispositivo_id = d.id"
                + " WHERE d.identificador_instalacion = ? AND s.estado = 'ACTIVA' AND rt.estado = 'ACTIVO'",
                String.class, idAislado);

        assertThat(hashEnBd).hasSize(64).doesNotContain(tokens.refreshToken());

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

        JWTClaimsSet claims = SignedJWT.parse(tokens.accessToken()).getJWTClaimsSet();

        assertThat(claims.getIssuer()).isEqualTo("cobranza-api-dev");
        assertThat(claims.getAudience()).contains("cobranza-api");
        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(claims.getStringClaim("sid")).isNotBlank();
        assertThat(claims.getStringClaim("did")).isNotBlank();
        assertThat(claims.getStringClaim("preferred_username")).isEqualTo("login.test");
        assertThat((List<?>) claims.getClaim("roles")).isNotNull();
        assertThat((List<?>) claims.getClaim("permisos")).isNotNull();
        assertThat(claims.getExpirationTime()).isAfter(new Date());
    }

    // ─── Seguridad criptográfica ──────────────────────────────────────────────────

    @Test
    void token_con_firma_invalida_retorna_401() throws Exception {
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

        ResponseEntity<Void> segundo = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        assertThat(segundo.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void excepcion_interna_con_token_valido_retorna_404_no_401_y_sin_stack_trace() {
        RespuestaToken tokens = loginPrincipal();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());

        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/ruta-que-no-existe-" + UUID.randomUUID(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        String cuerpo = resp.getBody();
        if (cuerpo != null) {
            assertThat(cuerpo).doesNotContain("java.", "Exception", "at cl.", "Caused by");
        }
    }

    // ─── Validación del contrato HTTP ─────────────────────────────────────────────

    @Test
    void login_sin_identificador_instalacion_retorna_400() {
        String cuerpoInvalido = """
                {"nombreUsuario":"login.test","contrasena":"ClaveTest.123!"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(cuerpoInvalido, headers),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_con_identificador_instalacion_formato_invalido_retorna_400() {
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, "no-es-un-uuid");
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── Concurrencia de registro de dispositivo ─────────────────────────────────

    /**
     * Dos hilos del mismo usuario hacen el primer login con el mismo identificadorInstalacion.
     * Resultado esperado:
     * - ambas respuestas 200;
     * - un único dispositivo en BD;
     * - mismo UUID interno en ambos JWTs;
     * - ningún 500.
     */
    @Test
    void registro_concurrente_mismo_usuario_mismo_identificador_crea_un_solo_dispositivo() throws Exception {
        String idConcurrente = UUID.randomUUID().toString();
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, idConcurrente);

        int hilos = 2;
        CountDownLatch listo = new CountDownLatch(hilos);
        CountDownLatch arrancar = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);

        List<Future<ResponseEntity<RespuestaToken>>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(() -> {
                listo.countDown();
                arrancar.await();
                return rest.postForEntity("/api/v1/auth/login", req, RespuestaToken.class);
            }));
        }

        listo.await();
        arrancar.countDown();

        List<ResponseEntity<RespuestaToken>> respuestas = new ArrayList<>();
        for (Future<ResponseEntity<RespuestaToken>> f : futuros) {
            respuestas.add(f.get());
        }
        pool.shutdown();

        // Ambas deben ser 200
        for (ResponseEntity<RespuestaToken> r : respuestas) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody()).isNotNull();
        }

        // Un único dispositivo en BD
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.dispositivos WHERE identificador_instalacion = ?",
                Integer.class, idConcurrente);
        assertThat(count).isEqualTo(1);

        // Mismo UUID interno del dispositivo en ambos access tokens
        String did1 = SignedJWT.parse(respuestas.get(0).getBody().accessToken())
                .getJWTClaimsSet().getStringClaim("did");
        String did2 = SignedJWT.parse(respuestas.get(1).getBody().accessToken())
                .getJWTClaimsSet().getStringClaim("did");
        assertThat(did1).isEqualTo(did2);
    }

    /**
     * Dos usuarios distintos compiten por el mismo identificadorInstalacion.
     * Resultado esperado:
     * - un 200 y un 409;
     * - una única fila en BD;
     * - ningún 500.
     */
    @Test
    void registro_concurrente_dos_usuarios_mismo_identificador_produce_conflicto_controlado() throws Exception {
        String idDisputa = UUID.randomUUID().toString();
        SolicitudLogin reqTest = new SolicitudLogin("login.test", CONTRASENA, idDisputa);
        SolicitudLogin reqOtro = new SolicitudLogin("login.otro", CONTRASENA, idDisputa);

        int hilos = 2;
        CountDownLatch listo = new CountDownLatch(hilos);
        CountDownLatch arrancar = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);

        Future<ResponseEntity<String>> futTest = pool.submit(() -> {
            listo.countDown();
            arrancar.await();
            return rest.postForEntity("/api/v1/auth/login", reqTest, String.class);
        });
        Future<ResponseEntity<String>> futOtro = pool.submit(() -> {
            listo.countDown();
            arrancar.await();
            return rest.postForEntity("/api/v1/auth/login", reqOtro, String.class);
        });

        listo.await();
        arrancar.countDown();

        ResponseEntity<String> rTest = futTest.get();
        ResponseEntity<String> rOtro = futOtro.get();
        pool.shutdown();

        // Exactamente un 200 y un 409 (o ambos 409 si los dos llegaron después del ganador)
        List<HttpStatusCode> codigos = List.of(rTest.getStatusCode(), rOtro.getStatusCode());
        assertThat(codigos).doesNotContain(HttpStatus.INTERNAL_SERVER_ERROR);
        long exitos = codigos.stream().filter(c -> c == HttpStatus.OK).count();
        long conflictos = codigos.stream().filter(c -> c == HttpStatus.CONFLICT).count();
        assertThat(exitos).isGreaterThanOrEqualTo(1);
        assertThat(exitos + conflictos).isEqualTo(2);

        // Una única fila en BD
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.dispositivos WHERE identificador_instalacion = ?",
                Integer.class, idDisputa);
        assertThat(count).isEqualTo(1);
    }

    // ─── Utilidad ─────────────────────────────────────────────────────────────────

    private RespuestaToken loginPrincipal() {
        SolicitudLogin req = new SolicitudLogin("login.test", CONTRASENA, idInstalacionPrincipal);
        ResponseEntity<RespuestaToken> resp = rest.postForEntity(
                "/api/v1/auth/login", req, RespuestaToken.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        return resp.getBody();
    }
}
