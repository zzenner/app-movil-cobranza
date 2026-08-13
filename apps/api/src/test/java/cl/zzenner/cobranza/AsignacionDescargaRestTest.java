package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionService;
import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.gestiones.aplicacion.ComandoCrearGestion;
import cl.zzenner.cobranza.gestiones.aplicacion.GestionService;
import cl.zzenner.cobranza.personas.aplicacion.PersonaService;
import cl.zzenner.cobranza.personas.dominio.Rut;
import cl.zzenner.cobranza.sincronizacion.web.RespuestaAsignacionDiaria;
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
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsignacionDescargaRestTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;
    @Autowired CarteraService carteraService;
    @Autowired PersonaService personaService;
    @Autowired AsignacionService asignacionService;
    @Autowired GestionService gestionService;

    static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    static final String ENDPOINT = "/api/v1/asignaciones/diaria/activa";

    UUID ejecutivoId;
    UUID supervisorId;
    UUID personaId;
    UUID diariaId;
    String tokenEjecutivo;
    String tokenSinRol;
    LocalDate fechaHoy;

    @BeforeAll
    void setup() {
        fechaHoy = LocalDate.now(SANTIAGO);

        supervisorId = usuarioService.crearUsuario(
                "adr.sup", "Supervisor", "ADR", "", "adr.sup@test.cl", "pass123");
        usuarioService.asignarRol(supervisorId, "SUPERVISOR", null);

        ejecutivoId = usuarioService.crearUsuario(
                "adr.eje", "Ejecutivo", "ADR", "", "adr.eje@test.cl", "pass123");
        usuarioService.asignarRol(ejecutivoId, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(supervisorId, ejecutivoId, fechaHoy);

        var cartera = carteraService.registrar("Cartera ADR", null);
        var persona = personaService.upsertPersona(
                Rut.of("27000001", "0"), "Persona ADR", "LEGADO", null, Instant.now());
        personaService.vincularCartera(persona.getId(), cartera.getId(), fechaHoy);
        personaId = persona.getId();

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejecutivoId, supervisorId,
                fechaHoy.withDayOfMonth(1),
                fechaHoy.withDayOfMonth(fechaHoy.lengthOfMonth()),
                null);
        asignacionService.agregarPersonaAMensual(amId, personaId);

        diariaId = asignacionService.crearAsignacionDiaria(amId, supervisorId, fechaHoy);
        asignacionService.agregarPersonaADiaria(diariaId, personaId);
        asignacionService.publicarAsignacionDiaria(diariaId, null);

        // Agregar una gestión de prueba
        var cmd = new ComandoCrearGestion(
                UUID.randomUUID(), "ASIGNACION_DIARIA", diariaId, personaId, ejecutivoId,
                "SIN_CONTACTO", Instant.now(), "obs test", null,
                -33.45, -70.66, 10f, null, false, Instant.now(), null);
        gestionService.recibirGestion(cmd);

        tokenEjecutivo = generarToken(ejecutivoId, List.of("EJECUTIVO_TERRENO"));
        UUID otroId = usuarioService.crearUsuario(
                "adr.otro", "Otro", "ADR", "", "adr.otro@test.cl", "pass123");
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

    private HttpHeaders conToken(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    // ── Test 1: Sin token → 401 con ProblemDetail uniforme ────────────────────

    @Test
    void get_diaria_activa_sin_token_retorna_401_con_problem_detail() {
        var resp = rest.getForEntity(ENDPOINT, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).contains("\"code\"");
        assertThat(resp.getBody()).contains("NO_AUTENTICADO");
        assertThat(resp.getBody()).contains("\"timestamp\"");
        assertThat(resp.getBody()).contains("\"path\"");
    }

    // ── Test 2: Sin rol → 403 con ProblemDetail uniforme ──────────────────────

    @Test
    void get_diaria_activa_sin_rol_retorna_403_con_problem_detail() {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenSinRol)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).contains("\"code\"");
        assertThat(resp.getBody()).contains("ACCESO_DENEGADO");
        assertThat(resp.getBody()).contains("\"timestamp\"");
        assertThat(resp.getBody()).contains("\"path\"");
    }

    // ── Test 3: Sin asignación activa → 204 ───────────────────────────────────

    @Test
    void get_diaria_activa_sin_asignacion_retorna_204() {
        UUID ejeId2 = usuarioService.crearUsuario(
                "adr.sinasig", "Sin", "Asig", "", "adr.sinasig@test.cl", "pass123");
        usuarioService.asignarRol(ejeId2, "EJECUTIVO_TERRENO", null);
        String token2 = generarToken(ejeId2, List.of("EJECUTIVO_TERRENO"));

        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(token2)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── Test 4: Con asignación activa → 200 con bundle ────────────────────────

    @Test
    void get_diaria_activa_con_asignacion_retorna_200() {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), RespuestaAsignacionDiaria.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    // ── Test 5: Respuesta tiene id de la asignación diaria ───────────────────

    @Test
    void get_diaria_activa_respuesta_tiene_id_asignacion() {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), RespuestaAsignacionDiaria.class);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isEqualTo(diariaId);
        assertThat(resp.getBody().estado()).isEqualTo("PUBLICADA");
    }

    // ── Test 6: Bundle contiene al menos una persona ──────────────────────────

    @Test
    void get_diaria_activa_bundle_contiene_personas() {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), RespuestaAsignacionDiaria.class);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().personas()).isNotEmpty();
    }

    // ── Test 7: Las personas tienen RUT y nombre ──────────────────────────────

    @Test
    void get_diaria_activa_personas_tienen_rut_y_nombre() {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), RespuestaAsignacionDiaria.class);
        assertThat(resp.getBody()).isNotNull();
        var persona = resp.getBody().personas().stream()
                .filter(p -> p.id().equals(personaId))
                .findFirst();
        assertThat(persona).isPresent();
        assertThat(persona.get().rutNumero()).isEqualTo("27000001");
        assertThat(persona.get().nombre()).isEqualTo("Persona ADR");
    }

    // ── Test 8: Bundle persona incluye gestiones ──────────────────────────────

    @Test
    void get_diaria_activa_bundle_incluye_ultimas_gestiones() {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), RespuestaAsignacionDiaria.class);
        assertThat(resp.getBody()).isNotNull();
        var persona = resp.getBody().personas().stream()
                .filter(p -> p.id().equals(personaId))
                .findFirst();
        assertThat(persona).isPresent();
        assertThat(persona.get().ultimasGestiones()).isNotEmpty();
    }

    // ── Test 9: Parámetro fecha personalizado ─────────────────────────────────

    @Test
    void get_diaria_activa_fecha_parametro_usada_cuando_se_informa() {
        // Sin asignación para una fecha futura → 204
        LocalDate futuro = fechaHoy.plusDays(90);
        var resp = rest.exchange(ENDPOINT + "?fecha=" + futuro, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), String.class);
        // No hay asignación para 90 días en el futuro
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── Test 10: Fecha de hoy produce resultado ───────────────────────────────

    @Test
    void get_diaria_activa_fecha_hoy_producce_resultado() {
        var resp = rest.exchange(ENDPOINT + "?fecha=" + fechaHoy, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), RespuestaAsignacionDiaria.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── Test 11: Fecha por defecto sin parámetro ──────────────────────────────

    @Test
    void get_diaria_activa_sin_parametro_fecha_usa_fecha_hoy() {
        // Sin ?fecha, el servidor usa hoy en Santiago — debe coincidir con la asignación creada para hoy
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), RespuestaAsignacionDiaria.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().fecha()).isEqualTo(fechaHoy);
    }

    // ── Test 12: Content-Type es application/json ─────────────────────────────

    @Test
    void get_diaria_activa_content_type_es_json() {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        MediaType ct = resp.getHeaders().getContentType();
        assertThat(ct).isNotNull();
        assertThat(ct.toString()).contains("application/json");
    }

    // ── Test 13: Estructura exacta del JSON — contrato Android ──────────────
    //
    // Verifica los nombres de campos tal como los leerá el cliente Android
    // (kotlinx.serialization + BigDecimalSerializer). Un cambio en el contrato
    // rompería la deserialización en el cliente sin error en tiempo de compilación.

    @Test
    void get_diaria_activa_json_tiene_estructura_de_contrato_android() throws Exception {
        var resp = rest.exchange(ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(conToken(tokenEjecutivo)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode root = objectMapper.readTree(resp.getBody());

        // Campos raíz del bundle
        assertThat(root.has("id")).isTrue();
        assertThat(root.has("ejecutivoId")).isTrue();
        assertThat(root.has("fecha")).isTrue();
        assertThat(root.has("estado")).isTrue();
        assertThat(root.has("personas")).isTrue();

        // UUIDs en minúsculas con guiones (lowercase)
        assertThat(root.get("id").asText())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        // fecha como "YYYY-MM-DD"
        assertThat(root.get("fecha").asText()).matches("\\d{4}-\\d{2}-\\d{2}");

        JsonNode personas = root.get("personas");
        assertThat(personas.isArray()).isTrue();
        assertThat(personas.size()).isGreaterThan(0);

        JsonNode persona = personas.get(0);
        assertThat(persona.has("id")).isTrue();
        assertThat(persona.has("rutNumero")).isTrue();
        assertThat(persona.has("rutDv")).isTrue();
        assertThat(persona.has("nombre")).isTrue();
        assertThat(persona.has("direcciones")).isTrue();
        assertThat(persona.has("avales")).isTrue();
        assertThat(persona.has("operaciones")).isTrue();
        assertThat(persona.has("ultimasGestiones")).isTrue();

        // operaciones: BigDecimal debe serializarse como número JSON (no string)
        JsonNode operaciones = persona.get("operaciones");
        assertThat(operaciones.isArray()).isTrue();
        if (operaciones.size() > 0) {
            JsonNode op = operaciones.get(0);
            assertThat(op.has("id")).isTrue();
            assertThat(op.has("personaId")).isTrue();
            assertThat(op.has("numeroOperacion")).isTrue();
            assertThat(op.has("estado")).isTrue();
            assertThat(op.get("capital").isNumber()).as("capital debe ser número JSON").isTrue();
            assertThat(op.get("interesPenal").isNumber()).as("interesPenal debe ser número JSON").isTrue();
            assertThat(op.get("gastosCobranza").isNumber()).as("gastosCobranza debe ser número JSON").isTrue();
            assertThat(op.get("totalVigente").isNumber()).as("totalVigente debe ser número JSON").isTrue();
            assertThat(op.has("cuotas")).isTrue();
        }

        // ultimasGestiones: fechas como strings ISO-8601 (no números epoch)
        JsonNode gestiones = persona.get("ultimasGestiones");
        assertThat(gestiones.isArray()).isTrue();
        assertThat(gestiones.size()).isGreaterThan(0);
        JsonNode gestion = gestiones.get(0);
        assertThat(gestion.has("id")).isTrue();
        assertThat(gestion.has("tipoGestion")).isTrue();
        assertThat(gestion.has("fechaGestion")).isTrue();
        assertThat(gestion.get("fechaGestion").isTextual()).as("fechaGestion debe ser ISO-8601 string").isTrue();
        assertThat(gestion.has("fechaCreacionServidor")).isTrue();
        assertThat(gestion.get("fechaCreacionServidor").isTextual())
                .as("fechaCreacionServidor debe ser ISO-8601 string").isTrue();
    }
}
