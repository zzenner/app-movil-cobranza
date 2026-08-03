package cl.zzenner.cobranza.autenticacion;

import cl.zzenner.cobranza.autenticacion.web.RespuestaLoginWeb;
import cl.zzenner.cobranza.autenticacion.web.SolicitudLoginWeb;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = {"security.bcrypt.strength=4", "app.web.allowed-origin=http://localhost:4200"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutenticacionWebIntegracionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired UsuarioService usuarioService;

    static final String CONTRASENA = "ClaveTest.123!";

    @BeforeAll
    void crearUsuarios() {
        usuarioService.crearUsuario("web.activo", "Web", "Activo", null, null, CONTRASENA);
        usuarioService.crearUsuario("web.otro", "Web", "Otro", null, null, CONTRASENA);
    }

    // ─── Login web básico ─────────────────────────────────────────────────────

    @Test
    void login_web_con_credenciales_validas_retorna_200_con_access_token() {
        ResponseEntity<RespuestaLoginWeb> resp = loginWeb("web.activo");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaLoginWeb body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.expiresInSeconds()).isGreaterThan(0);
        assertThat(body.sessionExpiresAt()).isNotNull();
    }

    @Test
    void login_web_no_retorna_refresh_token_en_body() {
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/v1/auth/web/login",
                new SolicitudLoginWeb("web.activo", CONTRASENA),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // El body no debe contener ningún campo llamado refreshToken
        assertThat(resp.getBody()).doesNotContain("refreshToken");
    }

    @Test
    void login_web_entrega_refresh_token_como_cookie_httponly() {
        ResponseEntity<RespuestaLoginWeb> resp = loginWeb("web.activo");

        List<String> setCookieHeaders = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).isNotNull().isNotEmpty();

        String cookieHeader = setCookieHeaders.stream()
                .filter(h -> h.startsWith("rt_web="))
                .findFirst()
                .orElse(null);
        assertThat(cookieHeader).isNotNull();
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("SameSite=Strict");
        assertThat(cookieHeader).contains("Path=/api/v1/auth/web/refresh");
    }

    @Test
    void login_web_con_credenciales_invalidas_retorna_401() {
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/v1/auth/web/login",
                new SolicitudLoginWeb("web.activo", "clave-mala"),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_web_no_requiere_identificador_instalacion() {
        // Validación: si enviáramos un cuerpo con identificadorInstalacion, debe ser ignorado —
        // el endpoint web solo acepta nombreUsuario + clave.
        // Aquí verificamos que un body sin identificadorInstalacion no da 400.
        ResponseEntity<RespuestaLoginWeb> resp = loginWeb("web.activo");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ─── JWT de sesión WEB ────────────────────────────────────────────────────

    @Test
    void jwt_web_no_contiene_claim_did() throws Exception {
        ResponseEntity<RespuestaLoginWeb> resp = loginWeb("web.activo");
        String at = resp.getBody().accessToken();

        JWTClaimsSet claims = SignedJWT.parse(at).getJWTClaimsSet();
        assertThat(claims.getStringClaim("did")).isNull();
    }

    @Test
    void jwt_web_contiene_claim_tipo_cliente_web() throws Exception {
        ResponseEntity<RespuestaLoginWeb> resp = loginWeb("web.activo");
        String at = resp.getBody().accessToken();

        JWTClaimsSet claims = SignedJWT.parse(at).getJWTClaimsSet();
        assertThat(claims.getStringClaim("tipo_cliente")).isEqualTo("WEB");
    }

    @Test
    void jwt_web_contiene_claims_basicos() throws Exception {
        ResponseEntity<RespuestaLoginWeb> resp = loginWeb("web.activo");
        String at = resp.getBody().accessToken();

        JWTClaimsSet claims = SignedJWT.parse(at).getJWTClaimsSet();
        assertThat(claims.getStringClaim("sid")).isNotBlank();
        assertThat(claims.getStringClaim("preferred_username")).isEqualTo("web.activo");
        assertThat(claims.getIssuer()).isEqualTo("cobranza-api-dev");
    }

    // ─── Refresh web ──────────────────────────────────────────────────────────

    @Test
    void refresh_web_con_cookie_valida_retorna_nuevo_access_token() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String cookieRt = extraerValorCookieRt(loginResp);
        assertThat(cookieRt).isNotBlank();

        ResponseEntity<RespuestaLoginWeb> refreshResp = refreshWebConCookie(cookieRt);

        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        RespuestaLoginWeb body = refreshResp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.accessToken()).isNotEqualTo(loginResp.getBody().accessToken());
    }

    @Test
    void refresh_web_rota_el_refresh_token_en_cookie() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String rtOriginal = extraerValorCookieRt(loginResp);

        ResponseEntity<RespuestaLoginWeb> refreshResp = refreshWebConCookie(rtOriginal);
        String rtNuevo = extraerValorCookieRt(refreshResp);

        assertThat(rtNuevo).isNotBlank().isNotEqualTo(rtOriginal);
    }

    @Test
    void refresh_web_con_token_consumido_retorna_401() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String rtOriginal = extraerValorCookieRt(loginResp);

        // Primer refresh consume el token
        refreshWebConCookie(rtOriginal);

        // Segundo intento con el mismo token → 401
        ResponseEntity<String> reuso = refreshWebConCookieStr(rtOriginal);
        assertThat(reuso.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_web_sin_cookie_retorna_401() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Origin", "http://localhost:4200");
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/web/refresh", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── Validación de Origin ─────────────────────────────────────────────────

    @Test
    void refresh_web_sin_origin_retorna_403() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String cookieRt = extraerValorCookieRt(loginResp);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "rt_web=" + cookieRt);
        // Sin header Origin
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/web/refresh", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void refresh_web_con_origin_no_permitido_retorna_403() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String cookieRt = extraerValorCookieRt(loginResp);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "rt_web=" + cookieRt);
        headers.add("Origin", "https://evil.example.com");
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/web/refresh", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void refresh_web_con_referer_valido_retorna_200() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String cookieRt = extraerValorCookieRt(loginResp);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "rt_web=" + cookieRt);
        headers.add("Referer", "http://localhost:4200/home");
        ResponseEntity<RespuestaLoginWeb> resp = rest.exchange(
                "/api/v1/auth/web/refresh", HttpMethod.POST,
                new HttpEntity<>(headers), RespuestaLoginWeb.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_web_sin_origin_retorna_403() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String at = loginResp.getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(at);
        // Sin header Origin
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/web/logout", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void logout_web_con_origin_no_permitido_retorna_403() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String at = loginResp.getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(at);
        headers.add("Origin", "https://evil.example.com");
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/auth/web/logout", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─── Logout web ───────────────────────────────────────────────────────────

    @Test
    void logout_web_retorna_204_y_limpia_cookie() {
        ResponseEntity<RespuestaLoginWeb> loginResp = loginWeb("web.activo");
        String at = loginResp.getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(at);
        headers.add("Origin", "http://localhost:4200");
        ResponseEntity<Void> logoutResp = rest.exchange(
                "/api/v1/auth/web/logout", HttpMethod.POST,
                new HttpEntity<>(headers), Void.class);

        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // La cookie de limpieza debe tener Max-Age=0
        List<String> setCookieHeaders = logoutResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).isNotNull();
        boolean tieneMaxAgeZero = setCookieHeaders.stream()
                .filter(h -> h.startsWith("rt_web="))
                .anyMatch(h -> h.contains("Max-Age=0"));
        assertThat(tieneMaxAgeZero).isTrue();
    }

    // ─── Segundo login cierra sesión anterior ────────────────────────────────

    @Test
    void segundo_login_web_cierra_sesion_anterior() {
        ResponseEntity<RespuestaLoginWeb> sesion1 = loginWeb("web.otro");
        String rt1 = extraerValorCookieRt(sesion1);

        ResponseEntity<RespuestaLoginWeb> sesion2 = loginWeb("web.otro");
        String rt2 = extraerValorCookieRt(sesion2);

        assertThat(rt1).isNotEqualTo(rt2);

        // El RT de la sesión 1 ya fue revocado → 401
        ResponseEntity<String> reuso = refreshWebConCookieStr(rt1);
        assertThat(reuso.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // El RT de la sesión 2 sigue válido
        ResponseEntity<RespuestaLoginWeb> renovado = refreshWebConCookie(rt2);
        assertThat(renovado.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    private ResponseEntity<RespuestaLoginWeb> loginWeb(String usuario) {
        return rest.postForEntity(
                "/api/v1/auth/web/login",
                new SolicitudLoginWeb(usuario, CONTRASENA),
                RespuestaLoginWeb.class);
    }

    private ResponseEntity<RespuestaLoginWeb> refreshWebConCookie(String cookieRt) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "rt_web=" + cookieRt);
        headers.add("Origin", "http://localhost:4200");
        return rest.exchange("/api/v1/auth/web/refresh", HttpMethod.POST,
                new HttpEntity<>(headers), RespuestaLoginWeb.class);
    }

    private ResponseEntity<String> refreshWebConCookieStr(String cookieRt) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "rt_web=" + cookieRt);
        headers.add("Origin", "http://localhost:4200");
        return rest.exchange("/api/v1/auth/web/refresh", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
    }

    private String extraerValorCookieRt(ResponseEntity<?> resp) {
        List<String> setCookieHeaders = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookieHeaders == null) return null;
        return setCookieHeaders.stream()
                .filter(h -> h.startsWith("rt_web" + "="))
                .map(h -> h.split(";")[0].substring(
                        ("rt_web" + "=").length()))
                .findFirst()
                .orElse(null);
    }
}
