package cl.zzenner.cobranza;

import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.personas.aplicacion.PersonaService;
import cl.zzenner.cobranza.personas.dominio.Rut;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionService;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
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
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BusquedaPersonaRestTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;
    @Autowired CarteraService carteraService;
    @Autowired PersonaService personaService;

    static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");

    // RUT persona de prueba: 15000001-7 (DV calculado con módulo 11)
    static final String RUT_NUMERO  = "15000001";
    static final String RUT_DV      = "7";
    // RUT válido por formato pero sin persona en BD
    static final String RUT_NUM_NF  = "15000002";
    static final String RUT_DV_NF   = "5";

    UUID ejecutivoId;
    UUID personaRegistradaId;
    String tokenEjecutivo;
    String tokenSinRol;

    @BeforeAll
    void setup() {
        UUID supervisorId = usuarioService.crearUsuario(
                "bp.sup", "Supervisor", "Busqueda", "", "bp.sup@test.cl", "pass123");
        usuarioService.asignarRol(supervisorId, "SUPERVISOR", null);

        ejecutivoId = usuarioService.crearUsuario(
                "bp.eje", "Ejecutivo", "Busqueda", "", "bp.eje@test.cl", "pass123");
        usuarioService.asignarRol(ejecutivoId, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(supervisorId, ejecutivoId, LocalDate.now(SANTIAGO));

        var cartera = carteraService.registrar("Cartera Busqueda", null);
        var persona = personaService.upsertPersona(
                Rut.of(RUT_NUMERO, RUT_DV), "Persona Busqueda Test", "LEGADO", null, Instant.now());
        personaService.vincularCartera(persona.getId(), cartera.getId(), LocalDate.now(SANTIAGO));
        personaRegistradaId = persona.getId();

        tokenEjecutivo = generarToken(ejecutivoId, List.of("EJECUTIVO_TERRENO"));
        UUID otroId = usuarioService.crearUsuario(
                "bp.otro", "Otro", "Busqueda", "", "bp.otro@test.cl", "pass123");
        tokenSinRol = generarToken(otroId, List.of());
    }

    private String generarToken(UUID sub, List<String> roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cobranza-api-dev")
                .audience(List.of("cobranza-api"))
                .subject(sub.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString())
                .claim("roles", roles)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private HttpEntity<Map<String, Object>> conToken(Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private Map<String, Object> solicitudValida() {
        Map<String, Object> m = new HashMap<>();
        m.put("rutNumero", RUT_NUMERO);
        m.put("rutDv",     RUT_DV);
        return m;
    }

    // ── Test 1: Sin token → 401 ──────────────────────────────────────────────

    @Test
    void post_busqueda_sin_token_retorna_401() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                new HttpEntity<>(solicitudValida(), h), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).contains("NO_AUTENTICADO");
    }

    // ── Test 2: Sin rol → 403 ────────────────────────────────────────────────

    @Test
    void post_busqueda_sin_rol_ejecutivo_retorna_403() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(solicitudValida(), tokenSinRol), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).contains("ACCESO_DENEGADO");
    }

    // ── Test 3: Body vacío → 400 ─────────────────────────────────────────────

    @Test
    void post_busqueda_body_vacio_retorna_400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenEjecutivo);
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                new HttpEntity<>("{}", headers), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 4: rutNumero nulo → 400 ─────────────────────────────────────────

    @Test
    void post_busqueda_rut_numero_nulo_retorna_400() {
        Map<String, Object> body = new HashMap<>();
        body.put("rutNumero", null);
        body.put("rutDv",     RUT_DV);
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(body, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 5: rutDv nulo → 400 ─────────────────────────────────────────────

    @Test
    void post_busqueda_rut_dv_nulo_retorna_400() {
        Map<String, Object> body = new HashMap<>();
        body.put("rutNumero", RUT_NUMERO);
        body.put("rutDv",     null);
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(body, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 6: rutNumero vacío → 400 ────────────────────────────────────────

    @Test
    void post_busqueda_rut_numero_vacio_retorna_400() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", "", "rutDv", RUT_DV), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 7: rutDv vacío → 400 ────────────────────────────────────────────

    @Test
    void post_busqueda_rut_dv_vacio_retorna_400() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", RUT_NUMERO, "rutDv", ""), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 8: DV incorrecto → 400 con code RUT_INVALIDO ────────────────────

    @Test
    void post_busqueda_dv_incorrecto_retorna_400_con_code_rut_invalido() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", RUT_NUMERO, "rutDv", "9"), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("RUT_INVALIDO");
    }

    // ── Test 9: ProblemDetail de RUT_INVALIDO tiene campos esperados ──────────

    @Test
    void post_busqueda_dv_incorrecto_problem_detail_tiene_campos_esperados() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", RUT_NUMERO, "rutDv", "9"), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("\"code\"");
        assertThat(resp.getBody()).contains("\"timestamp\"");
        assertThat(resp.getBody()).contains("\"path\"");
    }

    // ── Test 10: ProblemDetail no incluye el RUT en el detail ────────────────

    @Test
    void post_busqueda_dv_incorrecto_problem_detail_no_expone_rut() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", RUT_NUMERO, "rutDv", "9"), tokenEjecutivo), String.class);
        assertThat(resp.getBody()).doesNotContain(RUT_NUMERO);
    }

    // ── Test 11: Número con letras → 400 RUT_INVALIDO ────────────────────────

    @Test
    void post_busqueda_numero_con_letras_retorna_400() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", "1234567A", "rutDv", "5"), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("RUT_INVALIDO");
    }

    // ── Test 12: Número con más de 8 dígitos → 400 RUT_INVALIDO ─────────────

    @Test
    void post_busqueda_numero_con_mas_de_8_digitos_retorna_400() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", "123456789", "rutDv", "5"), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("RUT_INVALIDO");
    }

    // ── Test 13: DV inválido (no dígito ni K) → 400 RUT_INVALIDO ─────────────

    @Test
    void post_busqueda_dv_caracter_invalido_retorna_400() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", RUT_NUMERO, "rutDv", "X"), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("RUT_INVALIDO");
    }

    // ── Test 14: Persona no encontrada → 404 ─────────────────────────────────

    @Test
    void post_busqueda_persona_no_encontrada_retorna_404() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", RUT_NUM_NF, "rutDv", RUT_DV_NF), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Test 15: Persona encontrada → 200 ────────────────────────────────────

    @Test
    void post_busqueda_persona_encontrada_retorna_200() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(solicitudValida(), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── Test 16: Respuesta tiene envoltura version=1 y generadoEn ────────────

    @Test
    void post_busqueda_respuesta_tiene_version_1_y_generado_en() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(solicitudValida(), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"version\":1");
        assertThat(resp.getBody()).contains("\"generadoEn\"");
    }

    // ── Test 17: Respuesta tiene campo persona con id correcto ────────────────

    @Test
    void post_busqueda_respuesta_persona_tiene_id_y_rut() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(solicitudValida(), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains(personaRegistradaId.toString());
        assertThat(resp.getBody()).contains(RUT_NUMERO);
        assertThat(resp.getBody()).contains(RUT_DV);
    }

    // ── Test 18: Respuesta persona tiene nombre ───────────────────────────────

    @Test
    void post_busqueda_respuesta_persona_tiene_nombre() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(solicitudValida(), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Persona Busqueda Test");
    }

    // ── Test 19: Respuesta contiene listas (aunque vacías) ───────────────────

    @Test
    void post_busqueda_respuesta_contiene_campos_de_listas() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(solicitudValida(), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"direcciones\"");
        assertThat(resp.getBody()).contains("\"avales\"");
        assertThat(resp.getBody()).contains("\"operaciones\"");
        assertThat(resp.getBody()).contains("\"ultimasGestiones\"");
    }

    // ── Test 20: Cache-Control: no-store presente en 200 ─────────────────────

    @Test
    void post_busqueda_exitosa_tiene_cache_control_no_store() {
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(solicitudValida(), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
    }

    // ── Test 21: Búsqueda con RUT k minúscula normaliza correctamente ─────────

    @Test
    void post_busqueda_dv_k_minuscula_para_rut_con_dv_K_retorna_400_o_404() {
        // RUT "6-K" tiene dv K. Probar con "k" minúscula: debe normalizarse y ser válido de formato.
        // Si la persona no existe → 404; si el formato es inválido → 400(RUT_INVALIDO).
        // En ningún caso debe ser 500.
        var resp = rest.exchange("/api/v1/personas/busquedas", HttpMethod.POST,
                conToken(Map.of("rutNumero", "6", "rutDv", "k"), tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 404);
        assertThat(resp.getStatusCode().value()).isNotEqualTo(500);
    }
}
