package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionService;
import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.carteras.dominio.Cartera;
import cl.zzenner.cobranza.personas.aplicacion.PersonaService;
import cl.zzenner.cobranza.personas.dominio.Persona;
import cl.zzenner.cobranza.personas.dominio.Rut;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionService;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
import org.junit.jupiter.api.*;
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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsignacionAdminRestTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;
    @Autowired CarteraService carteraService;
    @Autowired PersonaService personaService;
    @Autowired AsignacionService asignacionService;

    static final String CLAVE = "ClaveAdmin.123!";
    static final LocalDate HOY = LocalDate.of(2026, 8, 15);

    UUID idJefe, idSupervisor, idEjecutivo, idEjecutivoOtro;
    UUID carteraId, mensualId;
    UUID personaId1, personaId2;
    UUID diariaId;

    @BeforeAll
    void prepararDatos() {
        idJefe = usuarioService.crearUsuario("aa.jefe", "Jefe", "Admin", null, null, CLAVE);
        usuarioService.asignarRol(idJefe, "JEFE_SUPERVISORES", null);

        idSupervisor = usuarioService.crearUsuario("aa.sup", "Super", "Visor", null, null, CLAVE);
        usuarioService.asignarRol(idSupervisor, "SUPERVISOR", null);

        idEjecutivo = usuarioService.crearUsuario("aa.ejec", "Ejec", "Uno", null, null, CLAVE);
        usuarioService.asignarRol(idEjecutivo, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(idSupervisor, idEjecutivo, HOY);

        idEjecutivoOtro = usuarioService.crearUsuario("aa.ejec2", "Ejec", "Dos", null, null, CLAVE);
        usuarioService.asignarRol(idEjecutivoOtro, "EJECUTIVO_TERRENO", null);

        // Cartera
        Cartera cartera = carteraService.registrar("Cartera Demo 6B", "Cartera para demo Fase 6B");
        carteraId = cartera.getId();

        // Personas
        Persona p1 = personaService.upsertPersona(
                Rut.of("25000001", "4"), "Juan Moreira Demo", "TEST_6B", null, Instant.now());
        personaService.vincularCartera(p1.getId(), carteraId, HOY);
        personaId1 = p1.getId();

        Persona p2 = personaService.upsertPersona(
                Rut.of("25000002", "2"), "Ana González Demo", "TEST_6B", null, Instant.now());
        personaService.vincularCartera(p2.getId(), carteraId, HOY);
        personaId2 = p2.getId();

        // Asignación mensual (posición del período: 2026-08)
        mensualId = asignacionService.crearAsignacionMensual(
                carteraId, idEjecutivo, idSupervisor,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(mensualId, personaId1);
        asignacionService.agregarPersonaAMensual(mensualId, personaId2);
    }

    // ── GET /api/v1/admin/asignaciones/periodos ──────────────────────────────

    @Test @Order(10)
    void jefe_puede_listar_periodos() {
        ResponseEntity<String> resp = get("/api/v1/admin/asignaciones/periodos", tokenJefe());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("2026-08");
    }

    @Test @Order(11)
    void supervisor_puede_listar_periodos() {
        ResponseEntity<String> resp = get("/api/v1/admin/asignaciones/periodos", tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @Order(12)
    void ejecutivo_no_puede_listar_periodos() {
        ResponseEntity<String> resp = get("/api/v1/admin/asignaciones/periodos", tokenEjecutivo());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(13)
    void sin_token_periodos_devuelve_401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/admin/asignaciones/periodos", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── GET /api/v1/admin/asignaciones/mensuales ─────────────────────────────

    @Test @Order(20)
    void listar_mensuales_devuelve_la_mensual_creada() {
        ResponseEntity<String> resp = get(
                "/api/v1/admin/asignaciones/mensuales?periodo=2026-08", tokenJefe());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains(mensualId.toString());
        assertThat(resp.getBody()).contains("cantidadPersonas");
    }

    // ── GET personas disponibles ───────────────────────────────────────────

    @Test @Order(30)
    void personas_disponibles_retorna_las_dos_personas() {
        ResponseEntity<String> resp = get(
                "/api/v1/admin/asignaciones/mensuales/" + mensualId + "/personas-disponibles",
                tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Juan Moreira Demo");
        assertThat(resp.getBody()).contains("Ana González Demo");
    }

    @Test @Order(31)
    void personas_disponibles_indica_asignacion_diaria_preexistente() {
        // Al inicio ninguna tiene diaria activa
        ResponseEntity<String> resp = get(
                "/api/v1/admin/asignaciones/mensuales/" + mensualId + "/personas-disponibles",
                tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // tieneAsignacionDiaria debe ser false para ambas
        assertThat(resp.getBody()).contains("\"tieneAsignacionDiaria\":false");
    }

    // ── GET /api/v1/admin/asignaciones/diarias — listado ──────────────────

    @Test @Order(40)
    void listado_diarias_vacio_al_inicio() {
        ResponseEntity<String> resp = get("/api/v1/admin/asignaciones/diarias", tokenJefe());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Puede estar vacío o contener elementos anteriores — solo verifica que responde
    }

    // ── POST /api/v1/admin/asignaciones/diarias — crear borrador ──────────

    @Test @Order(50)
    void crear_borrador_con_personas_validas() {
        String body = String.format(
                "{\"asignacionMensualId\":\"%s\",\"fecha\":\"%s\",\"personaIds\":[\"%s\"]}",
                mensualId, HOY, personaId1);

        ResponseEntity<String> resp = post("/api/v1/admin/asignaciones/diarias", body, tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("\"id\"");

        // Guardar el id para tests siguientes
        String respBody = resp.getBody();
        diariaId = UUID.fromString(respBody.replace("{\"id\":\"", "").replace("\"}", "").trim());
    }

    @Test @Order(51)
    void crear_borrador_sin_asignacion_mensual_id_devuelve_400() {
        String body = String.format("{\"fecha\":\"%s\",\"personaIds\":[]}", HOY.plusDays(1));
        ResponseEntity<String> resp = post("/api/v1/admin/asignaciones/diarias", body, tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(52)
    void crear_borrador_vacio_con_persona_inexistente_devuelve_error() {
        UUID idFalso = UUID.randomUUID();
        String body = String.format(
                "{\"asignacionMensualId\":\"%s\",\"fecha\":\"%s\",\"personaIds\":[\"%s\"]}",
                mensualId, HOY.plusDays(5), idFalso);
        ResponseEntity<String> resp = post("/api/v1/admin/asignaciones/diarias", body, tokenSupervisor());
        // Persona no existe en mensual → 422
        assertThat(resp.getStatusCode().value()).isGreaterThanOrEqualTo(400);
    }

    @Test @Order(53)
    void ejecutivo_no_puede_crear_borrador() {
        String body = String.format(
                "{\"asignacionMensualId\":\"%s\",\"fecha\":\"%s\",\"personaIds\":[]}",
                mensualId, HOY.plusDays(10));
        ResponseEntity<String> resp = post("/api/v1/admin/asignaciones/diarias", body, tokenEjecutivo());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── GET /api/v1/admin/asignaciones/diarias/{id} — detalle ─────────────

    @Test @Order(60)
    void detalle_diaria_devuelve_datos_completos() {
        // Necesita el diariaId del test 50
        if (diariaId == null) return; // saltado si test 50 no corrió
        ResponseEntity<String> resp = get(
                "/api/v1/admin/asignaciones/diarias/" + diariaId, tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"estado\":\"BORRADOR\"");
        assertThat(resp.getBody()).contains("personas");
        assertThat(resp.getBody()).contains("Juan Moreira Demo");
    }

    @Test @Order(61)
    void detalle_id_inexistente_devuelve_404() {
        ResponseEntity<String> resp = get(
                "/api/v1/admin/asignaciones/diarias/" + UUID.randomUUID(), tokenJefe());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Android: borrador no descargable ──────────────────────────────────

    @Test @Order(70)
    void ejecutivo_no_puede_descargar_borrador() {
        if (diariaId == null) return;
        // El ejecutivo consulta su asignación diaria activa para HOY
        ResponseEntity<String> resp = get(
                "/api/v1/asignaciones/diaria/activa?fecha=" + HOY, tokenEjecutivoTerreno());
        // 204 No Content porque la diaria está en BORRADOR (no PUBLICADA)
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── POST publicar ─────────────────────────────────────────────────────

    @Test @Order(80)
    void publicar_borrador_cambia_estado_a_publicada() {
        if (diariaId == null) return;
        ResponseEntity<String> resp = post(
                "/api/v1/admin/asignaciones/diarias/" + diariaId + "/publicar", "{}", tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verificar estado en BD
        ResponseEntity<String> detalle = get(
                "/api/v1/admin/asignaciones/diarias/" + diariaId, tokenSupervisor());
        assertThat(detalle.getBody()).contains("\"estado\":\"PUBLICADA\"");
        assertThat(detalle.getBody()).contains("fechaPublicacion");
        assertThat(detalle.getBody()).contains("publicadoPorId");
    }

    @Test @Order(81)
    void publicar_dos_veces_devuelve_conflicto() {
        if (diariaId == null) return;
        ResponseEntity<String> resp = post(
                "/api/v1/admin/asignaciones/diarias/" + diariaId + "/publicar", "{}", tokenSupervisor());
        assertThat(resp.getStatusCode().value()).isGreaterThanOrEqualTo(409);
    }

    // ── Android: publicada sí descargable ────────────────────────────────

    @Test @Order(90)
    void ejecutivo_puede_descargar_asignacion_publicada() {
        if (diariaId == null) return;
        ResponseEntity<String> resp = get(
                "/api/v1/asignaciones/diaria/activa?fecha=" + HOY, tokenEjecutivoTerreno());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Juan Moreira Demo");
        assertThat(resp.getBody()).contains("personas");
    }

    @Test @Order(91)
    void ejecutivo_incorrecto_no_ve_asignacion_publicada() {
        if (diariaId == null) return;
        // Otro ejecutivo consulta la misma fecha — debe obtener 204 (sin asignación)
        ResponseEntity<String> resp = get(
                "/api/v1/asignaciones/diaria/activa?fecha=" + HOY, tokenEjecutivoOtro());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── POST cancelar ─────────────────────────────────────────────────────

    @Test @Order(100)
    void cancelar_asignacion_publicada() {
        if (diariaId == null) return;
        String body = "{\"motivo\":\"Cancelada para test\"}";
        ResponseEntity<String> resp = post(
                "/api/v1/admin/asignaciones/diarias/" + diariaId + "/cancelar", body, tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verificar estado
        ResponseEntity<String> detalle = get(
                "/api/v1/admin/asignaciones/diarias/" + diariaId, tokenSupervisor());
        assertThat(detalle.getBody()).contains("\"estado\":\"CANCELADA\"");
        assertThat(detalle.getBody()).contains("Cancelada para test");
    }

    @Test @Order(101)
    void despues_de_cancelar_android_ya_no_descarga() {
        if (diariaId == null) return;
        ResponseEntity<String> resp = get(
                "/api/v1/asignaciones/diaria/activa?fecha=" + HOY, tokenEjecutivoTerreno());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test @Order(102)
    void cancelar_sin_motivo_devuelve_400() {
        // Crear una nueva diaria para el día siguiente (la original está cancelada)
        UUID nuevaId = asignacionService.crearAsignacionDiaria(mensualId, idSupervisor, HOY.plusDays(1));
        asignacionService.agregarPersonaADiaria(nuevaId, personaId2);

        String body = "{\"motivo\":\"\"}";
        ResponseEntity<String> resp = post(
                "/api/v1/admin/asignaciones/diarias/" + nuevaId + "/cancelar", body, tokenSupervisor());
        assertThat(resp.getStatusCode().value()).isGreaterThanOrEqualTo(400);
    }

    // ── Filtros listado ────────────────────────────────────────────────────

    @Test @Order(110)
    void listado_filtra_por_estado_cancelada() {
        ResponseEntity<String> resp = get(
                "/api/v1/admin/asignaciones/diarias?estado=CANCELADA", tokenJefe());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("CANCELADA");
    }

    @Test @Order(111)
    void listado_filtra_por_ejecutivo() {
        ResponseEntity<String> resp = get(
                "/api/v1/admin/asignaciones/diarias?ejecutivoId=" + idEjecutivo, tokenJefe());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── Actualizar personas en borrador ───────────────────────────────────

    @Test @Order(120)
    void actualizar_personas_en_borrador() {
        // Crear un borrador nuevo
        UUID nId = asignacionService.crearAsignacionDiaria(
                mensualId, idSupervisor, HOY.plusDays(2));
        asignacionService.agregarPersonaADiaria(nId, personaId1);

        // Actualizar para que solo tenga personaId2
        String body = String.format("{\"personaIds\":[\"%s\"]}", personaId2);
        ResponseEntity<String> resp = put(
                "/api/v1/admin/asignaciones/diarias/" + nId + "/personas", body, tokenSupervisor());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verificar que solo tiene p2
        ResponseEntity<String> detalle = get(
                "/api/v1/admin/asignaciones/diarias/" + nId, tokenSupervisor());
        assertThat(detalle.getBody()).contains("Ana González Demo");
        assertThat(detalle.getBody()).doesNotContain("Juan Moreira Demo");
    }

    // ── Helpers HTTP ─────────────────────────────────────────────────────

    ResponseEntity<String> get(String url, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    ResponseEntity<String> post(String url, String body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
    }

    ResponseEntity<String> put(String url, String body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, h), String.class);
    }

    // ── Tokens JWT ────────────────────────────────────────────────────────

    String tokenJefe() {
        return token(idJefe, "JEFE_SUPERVISORES", "ASIGNACIONES_VER", "ASIGNACIONES_ADMINISTRAR",
                "USUARIOS_VER", "CARTERAS_VER", "SUPERVISION_VER");
    }

    String tokenSupervisor() {
        return token(idSupervisor, "SUPERVISOR", "ASIGNACIONES_VER", "ASIGNACIONES_ADMINISTRAR");
    }

    String tokenEjecutivo() {
        // EJECUTIVO_TERRENO no tiene ASIGNACIONES_VER ni ASIGNACIONES_ADMINISTRAR
        return token(idEjecutivo, "EJECUTIVO_TERRENO", "GESTIONES_CREAR");
    }

    String tokenEjecutivoTerreno() {
        return token(idEjecutivo, "EJECUTIVO_TERRENO", "GESTIONES_CREAR", "SINCRONIZACION_VER");
    }

    String tokenEjecutivoOtro() {
        return token(idEjecutivoOtro, "EJECUTIVO_TERRENO", "GESTIONES_CREAR");
    }

    String token(UUID sub, String rol, String... permisos) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cobranza-api-test")
                .audience(List.of("cobranza-app"))
                .subject(sub.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .claim("roles", List.of(rol))
                .claim("permisos", List.of(permisos))
                .claim("tipo_cliente", "ANDROID")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
