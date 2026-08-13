package cl.zzenner.cobranza;

import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionService;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CarterasSupervisionRestTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;
    @Autowired JwtEncoder jwtEncoder;

    static final String CLAVE = "ClaveTest.123!";

    UUID idJefe;
    UUID idTecnologia;
    UUID idSupervisor;
    UUID idSupervisor2;
    UUID idEjecutivo;
    UUID idEjecutivo2;

    @BeforeAll
    void prepararDatos() {
        idJefe = usuarioService.crearUsuario(
                "cs.jefe", "Jefe", "Supervisores", null, null, CLAVE);
        usuarioService.asignarRol(idJefe, "JEFE_SUPERVISORES", null);

        idTecnologia = usuarioService.crearUsuario(
                "cs.tec", "Tec", "Admin", null, null, CLAVE);
        usuarioService.asignarRol(idTecnologia, "TECNOLOGIA", null);

        idSupervisor = usuarioService.crearUsuario(
                "cs.sup1", "Sup", "Uno", null, null, CLAVE);
        usuarioService.asignarRol(idSupervisor, "SUPERVISOR", null);

        idSupervisor2 = usuarioService.crearUsuario(
                "cs.sup2", "Sup", "Dos", null, null, CLAVE);
        usuarioService.asignarRol(idSupervisor2, "SUPERVISOR", null);

        idEjecutivo = usuarioService.crearUsuario(
                "cs.ejec1", "Ejec", "Uno", null, null, CLAVE);
        usuarioService.asignarRol(idEjecutivo, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(idSupervisor, idEjecutivo, LocalDate.now());

        idEjecutivo2 = usuarioService.crearUsuario(
                "cs.ejec2", "Ejec", "Dos", null, null, CLAVE);
        usuarioService.asignarRol(idEjecutivo2, "EJECUTIVO_TERRENO", null);
    }

    // ── GET /api/v1/admin/carteras ─────────────────────────────────────────────

    @Test
    void jefe_puede_listar_carteras() {
        ResponseEntity<String> resp = get("/api/v1/admin/carteras", tokenConCarteras(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("codigoOrigen");
    }

    @Test
    void tecnologia_puede_listar_carteras() {
        ResponseEntity<String> resp = get("/api/v1/admin/carteras", tokenConCarteras(idTecnologia, "TECNOLOGIA"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void supervisor_puede_listar_carteras() {
        ResponseEntity<String> resp = get("/api/v1/admin/carteras",
                tokenPara(idSupervisor, List.of("SUPERVISOR"), List.of("CARTERAS_VER", "SUPERVISION_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void ejecutivo_no_puede_listar_carteras() {
        ResponseEntity<String> resp = get("/api/v1/admin/carteras",
                tokenPara(idEjecutivo, List.of("EJECUTIVO_TERRENO"), List.of("GESTIONES_CREAR")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sin_token_carteras_retorna_401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/admin/carteras", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void catalogo_carteras_tiene_cuatro_registros() {
        ResponseEntity<String> resp = get("/api/v1/admin/carteras", tokenConCarteras(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        long count = resp.getBody().chars().filter(c -> c == '{').count();
        assertThat(count).isGreaterThanOrEqualTo(4);
    }

    // ── GET /api/v1/admin/supervision/ejecutivos ──────────────────────────────

    @Test
    void jefe_puede_listar_ejecutivos_supervision() {
        ResponseEntity<String> resp = get("/api/v1/admin/supervision/ejecutivos",
                tokenConSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("usuarioId");
    }

    @Test
    void supervisor_puede_listar_ejecutivos_supervision() {
        ResponseEntity<String> resp = get("/api/v1/admin/supervision/ejecutivos",
                tokenPara(idSupervisor, List.of("SUPERVISOR"), List.of("CARTERAS_VER", "SUPERVISION_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void ejecutivo_no_puede_listar_supervision() {
        ResponseEntity<String> resp = get("/api/v1/admin/supervision/ejecutivos",
                tokenPara(idEjecutivo, List.of("EJECUTIVO_TERRENO"), List.of("GESTIONES_CREAR")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sin_token_supervision_ejecutivos_retorna_401() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/v1/admin/supervision/ejecutivos", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void ejecutivos_listados_incluyen_supervisor_asignado() {
        ResponseEntity<String> resp = get("/api/v1/admin/supervision/ejecutivos",
                tokenConSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("supervisorId");
    }

    // ── GET /api/v1/admin/supervision/supervisores ────────────────────────────

    @Test
    void jefe_puede_listar_supervisores() {
        ResponseEntity<String> resp = get("/api/v1/admin/supervision/supervisores",
                tokenConSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("nombreUsuario");
    }

    @Test
    void ejecutivo_no_puede_listar_supervisores() {
        ResponseEntity<String> resp = get("/api/v1/admin/supervision/supervisores",
                tokenPara(idEjecutivo, List.of("EJECUTIVO_TERRENO"), List.of("GESTIONES_CREAR")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sin_token_supervision_supervisores_retorna_401() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/v1/admin/supervision/supervisores", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── POST asignar/reasignar ─────────────────────────────────────────────────

    @Test
    void jefe_puede_asignar_supervisor_a_ejecutivo_sin_supervision() {
        UUID ejec = usuarioService.crearUsuario("cs.asig.fresh", "Asig", "Fresh", null, null, CLAVE);
        usuarioService.asignarRol(ejec, "EJECUTIVO_TERRENO", null);

        String body = """
                {"supervisorId":"%s"}""".formatted(idSupervisor);
        ResponseEntity<String> resp = post(
                "/api/v1/admin/supervision/ejecutivos/" + ejec + "/supervisor",
                body, tokenConAdminSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void supervisor_no_puede_asignar() {
        String body = """
                {"supervisorId":"%s"}""".formatted(idSupervisor);
        ResponseEntity<String> resp = post(
                "/api/v1/admin/supervision/ejecutivos/" + idEjecutivo2 + "/supervisor",
                body, tokenPara(idSupervisor, List.of("SUPERVISOR"), List.of("CARTERAS_VER", "SUPERVISION_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void reasignar_ejecutivo_retorna_no_content() {
        UUID ejec = usuarioService.crearUsuario("cs.reas.fresh", "Reas", "Fresh", null, null, CLAVE);
        usuarioService.asignarRol(ejec, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(idSupervisor, ejec, LocalDate.now());

        String body = """
                {"supervisorId":"%s"}""".formatted(idSupervisor2);
        ResponseEntity<String> resp = post(
                "/api/v1/admin/supervision/ejecutivos/" + ejec + "/supervisor",
                body, tokenConAdminSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── DELETE quitar supervisión ──────────────────────────────────────────────

    @Test
    void jefe_puede_quitar_supervision() {
        UUID ejec = usuarioService.crearUsuario(
                "cs.ejec.del", "Ejec", "Del", null, null, CLAVE);
        usuarioService.asignarRol(ejec, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(idSupervisor, ejec, LocalDate.now());

        ResponseEntity<String> resp = delete(
                "/api/v1/admin/supervision/ejecutivos/" + ejec + "/supervisor",
                tokenConAdminSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void quitar_supervision_sin_supervision_activa_retorna_409() {
        UUID ejec = usuarioService.crearUsuario(
                "cs.ejec.noasig", "Ejec", "NoAsig", null, null, CLAVE);
        usuarioService.asignarRol(ejec, "EJECUTIVO_TERRENO", null);

        ResponseEntity<String> resp = delete(
                "/api/v1/admin/supervision/ejecutivos/" + ejec + "/supervisor",
                tokenConAdminSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── PATCH código ejecutivo ─────────────────────────────────────────────────

    @Test
    void jefe_puede_actualizar_codigo_ejecutivo() {
        UUID ejec = usuarioService.crearUsuario("cs.cod.fresh", "Cod", "Fresh", null, null, CLAVE);
        usuarioService.asignarRol(ejec, "EJECUTIVO_TERRENO", null);

        String body = """
                {"codigo":"EJ-TEST-FRESH-001"}""";
        ResponseEntity<String> resp = patch(
                "/api/v1/admin/supervision/ejecutivos/" + ejec + "/codigo",
                body, tokenConAdminSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void codigo_duplicado_retorna_409() {
        UUID ejec = usuarioService.crearUsuario(
                "cs.ejec.dup", "Ejec", "Dup", null, null, CLAVE);
        usuarioService.asignarRol(ejec, "EJECUTIVO_TERRENO", null);
        supervisionService.actualizarCodigoEjecutivo(ejec, "CODIGO-UNICO-XYZ");

        UUID ejec2 = usuarioService.crearUsuario(
                "cs.ejec.dup2", "Ejec", "Dup2", null, null, CLAVE);
        usuarioService.asignarRol(ejec2, "EJECUTIVO_TERRENO", null);

        String body = """
                {"codigo":"CODIGO-UNICO-XYZ"}""";
        ResponseEntity<String> resp = patch(
                "/api/v1/admin/supervision/ejecutivos/" + ejec2 + "/codigo",
                body, tokenConAdminSupervision(idJefe, "JEFE_SUPERVISORES"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void supervisor_no_puede_actualizar_codigo() {
        String body = """
                {"codigo":"CODIGO-SUP"}""";
        ResponseEntity<String> resp = patch(
                "/api/v1/admin/supervision/ejecutivos/" + idEjecutivo + "/codigo",
                body, tokenPara(idSupervisor, List.of("SUPERVISOR"), List.of("CARTERAS_VER", "SUPERVISION_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ResponseEntity<String> get(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> post(String url, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> patch(String url, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> delete(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    private String tokenConCarteras(UUID usuarioId, String rol) {
        return tokenPara(usuarioId, List.of(rol),
                List.of("CARTERAS_VER", "SUPERVISION_VER", "SUPERVISION_ADMINISTRAR"));
    }

    private String tokenConSupervision(UUID usuarioId, String rol) {
        return tokenPara(usuarioId, List.of(rol),
                List.of("CARTERAS_VER", "SUPERVISION_VER", "SUPERVISION_ADMINISTRAR"));
    }

    private String tokenConAdminSupervision(UUID usuarioId, String rol) {
        return tokenPara(usuarioId, List.of(rol),
                List.of("CARTERAS_VER", "SUPERVISION_VER", "SUPERVISION_ADMINISTRAR"));
    }

    private String tokenPara(UUID usuarioId, List<String> roles, List<String> permisos) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cobranza-api-dev")
                .audience(List.of("cobranza-api"))
                .subject(usuarioId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString())
                .claim("preferred_username", "test.user")
                .claim("tipo_cliente", "WEB")
                .claim("roles", roles)
                .claim("permisos", permisos)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
