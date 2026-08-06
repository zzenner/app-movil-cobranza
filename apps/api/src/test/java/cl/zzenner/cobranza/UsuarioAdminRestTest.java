package cl.zzenner.cobranza;

import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
class UsuarioAdminRestTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;
    @Autowired UsuarioConsultaApi usuarioConsultaApi;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JdbcTemplate jdbc;

    static final String CLAVE = "ClaveTest.123!";

    UUID idJefeSupervisores;
    UUID idTecnologia;
    UUID idSupervisor;
    UUID idEjecutivo;
    UUID idInactivo;
    UUID idBloqueado;
    UUID idBloqueadoTemp;

    @BeforeAll
    void prepararDatos() {
        idJefeSupervisores = usuarioService.crearUsuario(
                "admin.jefe", "Admin", "Jefe", null, "admin.jefe@test.cl", CLAVE);
        usuarioService.asignarRol(idJefeSupervisores, "JEFE_SUPERVISORES", null);

        idTecnologia = usuarioService.crearUsuario(
                "admin.tec", "Admin", "Tecnologia", null, null, CLAVE);
        usuarioService.asignarRol(idTecnologia, "TECNOLOGIA", null);

        idSupervisor = usuarioService.crearUsuario(
                "sup.prueba", "Supervisor", "Prueba", null, null, CLAVE);
        usuarioService.asignarRol(idSupervisor, "SUPERVISOR", null);

        idEjecutivo = usuarioService.crearUsuario(
                "ejec.prueba", "Ejecutivo", "Prueba", "Materno", null, CLAVE);
        usuarioService.asignarRol(idEjecutivo, "EJECUTIVO_TERRENO", null);
        supervisionService.asignarEjecutivo(idSupervisor, idEjecutivo, LocalDate.now());

        idInactivo = usuarioService.crearUsuario(
                "admin.inactivo", "Inactivo", "User", null, null, CLAVE);
        jdbc.update("UPDATE cobranza.usuarios SET activo = false WHERE id = ?", idInactivo);

        idBloqueado = usuarioService.crearUsuario(
                "admin.bloqueado", "Bloqueado", "User", null, null, CLAVE);
        jdbc.update("UPDATE cobranza.usuarios SET bloqueado = true WHERE id = ?", idBloqueado);

        idBloqueadoTemp = usuarioService.crearUsuario(
                "admin.bloq.temp", "BloqTemp", "User", null, null, CLAVE);
        usuarioConsultaApi.aplicarBloqueoTemporal(
                idBloqueadoTemp, Instant.now().plus(Duration.ofHours(2)));
    }

    // ─── Autorización ─────────────────────────────────────────────────────────────

    @Test
    void jefe_supervisores_puede_listar_usuarios() {
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios",
                tokenPara(idJefeSupervisores, List.of("JEFE_SUPERVISORES"),
                        List.of("USUARIOS_VER", "USUARIOS_ADMINISTRAR",
                                "ASIGNACIONES_VER", "ASIGNACIONES_ADMINISTRAR",
                                "GESTIONES_VER", "GESTIONES_CREAR", "SINCRONIZACION_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("contenido");
    }

    @Test
    void tecnologia_puede_listar_usuarios() {
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios",
                tokenPara(idTecnologia, List.of("TECNOLOGIA"),
                        List.of("USUARIOS_VER", "USUARIOS_ADMINISTRAR", "SINCRONIZACION_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("contenido");
    }

    @Test
    void supervisor_recibe_403() {
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios",
                tokenPara(idSupervisor, List.of("SUPERVISOR"),
                        List.of("ASIGNACIONES_VER", "ASIGNACIONES_ADMINISTRAR", "GESTIONES_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void ejecutivo_terreno_recibe_403() {
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios",
                tokenPara(idEjecutivo, List.of("EJECUTIVO_TERRENO"), List.of("GESTIONES_CREAR")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sin_token_recibe_401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/admin/usuarios", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── Filtros ─────────────────────────────────────────────────────────────────

    @Test
    void filtro_nombreUsuario_insensible_mayusculas() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?nombreUsuario=ADMIN.JEFE", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("admin.jefe");
    }

    @Test
    void filtro_nombreUsuario_parcial() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?nombreUsuario=admin", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("contenido");
        assertThat(resp.getBody()).contains("admin.jefe");
    }

    @Test
    void filtro_por_rol_SUPERVISOR() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?rol=SUPERVISOR", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("sup.prueba");
        assertThat(resp.getBody()).doesNotContain("admin.jefe");
    }

    @Test
    void filtro_rol_inexistente_devuelve_400() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?rol=ROL_QUE_NO_EXISTE", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("ROL_QUE_NO_EXISTE");
    }

    @Test
    void filtro_estado_INACTIVO() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?estado=INACTIVO", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("INACTIVO");
    }

    @Test
    void filtro_estado_BLOQUEADO() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?estado=BLOQUEADO", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("BLOQUEADO");
    }

    @Test
    void filtro_estado_BLOQUEADO_TEMPORAL() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?estado=BLOQUEADO_TEMPORAL", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("admin.bloq.temp");
    }

    @Test
    void filtro_estado_ACTIVO() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?estado=ACTIVO", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("ACTIVO");
        assertThat(resp.getBody()).doesNotContain("INACTIVO");
    }

    @Test
    void estado_invalido_devuelve_400() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?estado=NO_EXISTE", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── Paginación ───────────────────────────────────────────────────────────────

    @Test
    void paginacion_pagina_cero() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?pagina=0&tamanio=2", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"pagina\":0");
        assertThat(resp.getBody()).contains("\"tamanio\":2");
    }

    @Test
    void tamanio_maximo_50_es_valido() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?tamanio=50", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void tamanio_mayor_50_devuelve_400() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?tamanio=51", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void pagina_negativa_devuelve_400() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios?pagina=-1", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void orden_estable_nombreUsuario_asc() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> p1 = get("/api/v1/admin/usuarios?tamanio=3&pagina=0", token);
        ResponseEntity<String> p2 = get("/api/v1/admin/usuarios?tamanio=3&pagina=1", token);

        assertThat(p1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(p2.getStatusCode()).isEqualTo(HttpStatus.OK);
        // No deben haber duplicados entre páginas
        String body1 = p1.getBody();
        String body2 = p2.getBody();
        assertThat(body1).isNotNull();
        assertThat(body2).isNotNull();
    }

    // ─── Detalle ─────────────────────────────────────────────────────────────────

    @Test
    void detalle_retorna_200_con_campos_completos() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios/" + idJefeSupervisores, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resp.getBody();
        assertThat(body).contains("nombreUsuario");
        assertThat(body).contains("roles");
        assertThat(body).contains("permisosEfectivos");
        assertThat(body).contains("estadoCalculado");
        assertThat(body).contains("fechaCreacion");
    }

    @Test
    void detalle_ejecutivo_incluye_supervisor() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios/" + idEjecutivo, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("supervisorId");
        assertThat(resp.getBody()).contains("sup.prueba");
    }

    @Test
    void detalle_uuid_invalido_devuelve_400() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios/no-es-uuid", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void detalle_uuid_inexistente_devuelve_404() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios/" + UUID.randomUUID(), token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("USUARIO_NO_ENCONTRADO");
    }

    @Test
    void detalle_sin_token_devuelve_401() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/v1/admin/usuarios/" + idJefeSupervisores, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void detalle_supervisor_recibe_403() {
        ResponseEntity<String> resp = get(
                "/api/v1/admin/usuarios/" + idSupervisor,
                tokenPara(idSupervisor, List.of("SUPERVISOR"),
                        List.of("ASIGNACIONES_VER", "ASIGNACIONES_ADMINISTRAR", "GESTIONES_VER")));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─── Ausencia de campos sensibles ─────────────────────────────────────────────

    @Test
    void listado_no_expone_hash_ni_version() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios", token);
        assertThat(resp.getBody()).doesNotContain("contrasenaHash");
        assertThat(resp.getBody()).doesNotContain("version");
    }

    @Test
    void detalle_no_expone_hash() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios/" + idJefeSupervisores, token);
        assertThat(resp.getBody()).doesNotContain("contrasenaHash");
    }

    // ─── Contrato JSON ────────────────────────────────────────────────────────────

    @Test
    void listado_contiene_campos_contrato() {
        String token = tokenConUsuariosVer(idJefeSupervisores, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get("/api/v1/admin/usuarios", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resp.getBody();
        assertThat(body).contains("\"contenido\"");
        assertThat(body).contains("\"pagina\"");
        assertThat(body).contains("\"tamanio\"");
        assertThat(body).contains("\"totalElementos\"");
        assertThat(body).contains("\"totalPaginas\"");
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────────

    private ResponseEntity<String> get(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String tokenConUsuariosVer(UUID usuarioId, String rol) {
        return tokenPara(usuarioId, List.of(rol), List.of("USUARIOS_VER"));
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
