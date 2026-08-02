package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionService;
import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.gestiones.web.RespuestaCrearGestion;
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
class GestionRestTest {

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
    @Autowired AsignacionService asignacionService;

    UUID ejecutivoId;
    UUID supervisorId;
    UUID personaId;
    UUID diariaId;
    String tokenEjecutivo;
    String tokenSinRol;

    static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");

    @BeforeAll
    void setup() {
        supervisorId = usuarioService.crearUsuario(
                "gr.sup", "Supervisor", "Rest", "", "gr.sup@test.cl", "pass123");
        usuarioService.asignarRol(supervisorId, "SUPERVISOR", null);

        ejecutivoId = usuarioService.crearUsuario(
                "gr.eje", "Ejecutivo", "Rest", "", "gr.eje@test.cl", "pass123");
        usuarioService.asignarRol(ejecutivoId, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(supervisorId, ejecutivoId, LocalDate.now(SANTIAGO));

        var cartera = carteraService.registrar("Cartera GR", null);
        var persona = personaService.upsertPersona(
                Rut.of("26000001", "2"), "Persona GR", "LEGADO", null, Instant.now());
        personaService.vincularCartera(persona.getId(), cartera.getId(), LocalDate.now(SANTIAGO));
        personaId = persona.getId();

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejecutivoId, supervisorId,
                LocalDate.now(SANTIAGO).withDayOfMonth(1),
                LocalDate.now(SANTIAGO).withDayOfMonth(
                        LocalDate.now(SANTIAGO).lengthOfMonth()),
                null);
        asignacionService.agregarPersonaAMensual(amId, personaId);

        diariaId = asignacionService.crearAsignacionDiaria(amId, supervisorId, LocalDate.now(SANTIAGO));
        asignacionService.agregarPersonaADiaria(diariaId, personaId);
        asignacionService.publicarAsignacionDiaria(diariaId);

        tokenEjecutivo = generarToken(ejecutivoId, List.of("EJECUTIVO_TERRENO"));
        UUID otroId = usuarioService.crearUsuario(
                "gr.otro", "Otro", "Rest", "", "gr.otro@test.cl", "pass123");
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

    private Map<String, Object> solicitudBase(UUID id, UUID personaId, UUID diariaId) {
        var map = new HashMap<String, Object>();
        map.put("id",               id.toString());
        map.put("personaId",        personaId.toString());
        map.put("origenGestion",    diariaId != null ? "ASIGNACION_DIARIA" : "BUSQUEDA_DIRECTA");
        map.put("asignacionDiariaId", diariaId != null ? diariaId.toString() : null);
        map.put("tipoGestion",      "SIN_CONTACTO");
        map.put("fechaGestion",     Instant.now().toString());
        map.put("fechaCapturaGps",  Instant.now().toString());
        map.put("latitud",          -33.45);
        map.put("longitud",         -70.66);
        map.put("precisionMetros",  10.0);
        map.put("ubicacionSimulada", false);
        return map;
    }

    private HttpEntity<Map<String, Object>> conToken(Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    // ── Test 1: Sin token → 401 con ProblemDetail uniforme ────────────────────

    @Test
    void post_gestiones_sin_token_retorna_401_con_problem_detail() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                new HttpEntity<>(solicitudBase(UUID.randomUUID(), personaId, null), h), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).contains("\"code\"");
        assertThat(resp.getBody()).contains("NO_AUTENTICADO");
        assertThat(resp.getBody()).contains("\"timestamp\"");
        assertThat(resp.getBody()).contains("\"path\"");
    }

    // ── Test 2: Sin rol → 403 con ProblemDetail uniforme ──────────────────────

    @Test
    void post_gestiones_sin_rol_ejecutivo_retorna_403_con_problem_detail() {
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitudBase(UUID.randomUUID(), personaId, null), tokenSinRol), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).contains("\"code\"");
        assertThat(resp.getBody()).contains("ACCESO_DENEGADO");
        assertThat(resp.getBody()).contains("\"timestamp\"");
        assertThat(resp.getBody()).contains("\"path\"");
    }

    // ── Test 3: Crear gestión válida (BUSQUEDA_DIRECTA) → 201 ────────────────

    @Test
    void post_gestiones_busqueda_directa_valida_retorna_201() {
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitudBase(UUID.randomUUID(), personaId, null), tokenEjecutivo),
                RespuestaCrearGestion.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().resultado().name()).isEqualTo("INSERTADA");
    }

    // ── Test 4: Crear gestión válida (ASIGNACION_DIARIA) → 201 ───────────────

    @Test
    void post_gestiones_asignacion_diaria_valida_retorna_201() {
        UUID id = UUID.randomUUID();
        var solicitud = solicitudBase(id, personaId, diariaId);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), RespuestaCrearGestion.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ── Test 5: Idempotencia misma gestión → 200 IDEMPOTENTE ─────────────────

    @Test
    void post_gestiones_idempotente_retorna_200_con_resultado_idempotente() {
        UUID id = UUID.randomUUID();
        var solicitud = solicitudBase(id, personaId, null);

        var primer = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), RespuestaCrearGestion.class);
        assertThat(primer.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var segundo = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), RespuestaCrearGestion.class);
        assertThat(segundo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(segundo.getBody()).isNotNull();
        assertThat(segundo.getBody().resultado().name()).isEqualTo("IDEMPOTENTE");
    }

    // ── Test 6: Conflicto mismo UUID diferente contenido → 409 ───────────────

    @Test
    void post_gestiones_conflicto_uuid_retorna_409() {
        // Crear segunda persona para el conflicto
        var p2 = personaService.upsertPersona(
                Rut.of("26000002", "0"), "Persona GR2", "LEGADO", null, Instant.now());

        UUID idCompartido = UUID.randomUUID();
        var sol1 = solicitudBase(idCompartido, personaId, null);
        rest.exchange("/api/v1/gestiones", HttpMethod.POST, conToken(sol1, tokenEjecutivo), String.class);

        var sol2 = solicitudBase(idCompartido, p2.getId(), null);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(sol2, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── Test 7: Sin personaId en body → 400 ──────────────────────────────────

    @Test
    void post_gestiones_sin_personaId_retorna_400() {
        var solicitud = new HashMap<String, Object>();
        solicitud.put("id",               UUID.randomUUID().toString());
        solicitud.put("origenGestion",    "BUSQUEDA_DIRECTA");
        solicitud.put("tipoGestion",      "SIN_CONTACTO");
        solicitud.put("fechaGestion",     Instant.now().toString());
        solicitud.put("fechaCapturaGps",  Instant.now().toString());
        solicitud.put("latitud",          -33.45);
        solicitud.put("longitud",         -70.66);
        solicitud.put("precisionMetros",  10.0);
        solicitud.put("ubicacionSimulada", false);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 8: Sin id en body → 400 ─────────────────────────────────────────

    @Test
    void post_gestiones_sin_id_retorna_400() {
        var solicitud = new HashMap<String, Object>();
        solicitud.put("personaId",        personaId.toString());
        solicitud.put("origenGestion",    "BUSQUEDA_DIRECTA");
        solicitud.put("tipoGestion",      "SIN_CONTACTO");
        solicitud.put("fechaGestion",     Instant.now().toString());
        solicitud.put("fechaCapturaGps",  Instant.now().toString());
        solicitud.put("latitud",          -33.45);
        solicitud.put("longitud",         -70.66);
        solicitud.put("precisionMetros",  10.0);
        solicitud.put("ubicacionSimulada", false);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 9: Persona inexistente (BUSQUEDA_DIRECTA) → 404 ─────────────────

    @Test
    void post_gestiones_persona_inexistente_retorna_404() {
        var solicitud = solicitudBase(UUID.randomUUID(), UUID.randomUUID(), null);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Test 10: Asignación diaria inexistente → 404 ──────────────────────────

    @Test
    void post_gestiones_asignacion_diaria_inexistente_retorna_404() {
        var solicitud = solicitudBase(UUID.randomUUID(), personaId, UUID.randomUUID());
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Test 11: Origen de gestión inválido (no es enum) → 400 ───────────────

    @Test
    void post_gestiones_origen_invalido_retorna_400() {
        var solicitud = Map.<String, Object>of(
            "id",               UUID.randomUUID().toString(),
            "personaId",        personaId.toString(),
            "origenGestion",    "ORIGEN_INVALIDO",
            "tipoGestion",      "SIN_CONTACTO",
            "fechaGestion",     Instant.now().toString(),
            "fechaCapturaGps",  Instant.now().toString(),
            "latitud",          -33.45,
            "longitud",         -70.66,
            "precisionMetros",  10.0,
            "ubicacionSimulada", false
        );
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 12: Tipo de gestión inválido → 400 ───────────────────────────────

    @Test
    void post_gestiones_tipo_invalido_retorna_400() {
        var solicitud = Map.<String, Object>of(
            "id",               UUID.randomUUID().toString(),
            "personaId",        personaId.toString(),
            "origenGestion",    "BUSQUEDA_DIRECTA",
            "tipoGestion",      "TIPO_INVALIDO",
            "fechaGestion",     Instant.now().toString(),
            "fechaCapturaGps",  Instant.now().toString(),
            "latitud",          -33.45,
            "longitud",         -70.66,
            "precisionMetros",  10.0,
            "ubicacionSimulada", false
        );
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    // ── Test 13: Respuesta contiene id y resultado ────────────────────────────

    @Test
    void post_gestiones_respuesta_contiene_id_y_resultado() {
        UUID id = UUID.randomUUID();
        var solicitud = solicitudBase(id, personaId, null);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), RespuestaCrearGestion.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RespuestaCrearGestion body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(id);
        assertThat(body.resultado()).isNotNull();
    }

    // ── Test 14: No hay header Location en la respuesta 201 ──────────────────

    @Test
    void post_gestiones_no_tiene_header_location() {
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitudBase(UUID.randomUUID(), personaId, null), tokenEjecutivo),
                RespuestaCrearGestion.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).isNull();
    }

    // ── Test 16: Asignación en estado BORRADOR → 400 ESTADO_INVALIDO ─────────

    @Test
    void post_gestiones_asignacion_borrador_retorna_400_con_problem_detail() {
        // Cartera y persona exclusivos para este test (nueva cartera → no conflicto)
        var carteraBorrador = carteraService.registrar("Cartera Borrador Test", null);
        var personaBorrador = personaService.upsertPersona(
                Rut.of("28000001", "9"), "Persona Borrador", "LEGADO", null, Instant.now());
        personaService.vincularCartera(personaBorrador.getId(), carteraBorrador.getId(), LocalDate.now(SANTIAGO));

        // Mensual para mes siguiente → sin conflicto con la diaria PUBLICADA del setup
        LocalDate primerDiaSiguiente = LocalDate.now(SANTIAGO).plusMonths(1).withDayOfMonth(1);
        UUID amBorradorId = asignacionService.crearAsignacionMensual(
                carteraBorrador.getId(), ejecutivoId, supervisorId,
                primerDiaSiguiente,
                primerDiaSiguiente.withDayOfMonth(primerDiaSiguiente.lengthOfMonth()),
                null);
        asignacionService.agregarPersonaAMensual(amBorradorId, personaBorrador.getId());

        // Diaria creada pero NO publicada → estado BORRADOR
        UUID borradorId = asignacionService.crearAsignacionDiaria(amBorradorId, supervisorId, primerDiaSiguiente);
        asignacionService.agregarPersonaADiaria(borradorId, personaBorrador.getId());

        var solicitud = solicitudBase(UUID.randomUUID(), personaBorrador.getId(), borradorId);
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("\"code\"");
        assertThat(resp.getBody()).contains("ESTADO_INVALIDO");
        assertThat(resp.getBody()).contains("BORRADOR");
    }

    // ── Test 15: COMPROMISO_PAGO sin fecha_compromiso → 400 dominio ──────────

    @Test
    void post_gestiones_compromiso_pago_sin_fecha_retorna_400() {
        var solicitud = Map.<String, Object>of(
            "id",               UUID.randomUUID().toString(),
            "personaId",        personaId.toString(),
            "origenGestion",    "BUSQUEDA_DIRECTA",
            "tipoGestion",      "COMPROMISO_PAGO",
            "fechaGestion",     Instant.now().toString(),
            "fechaCapturaGps",  Instant.now().toString(),
            "latitud",          -33.45,
            "longitud",         -70.66,
            "precisionMetros",  10.0,
            "ubicacionSimulada", false
        );
        var resp = rest.exchange("/api/v1/gestiones", HttpMethod.POST,
                conToken(solicitud, tokenEjecutivo), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
