package cl.zzenner.cobranza;

import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.importacion.dominio.EstadoImportacion;
import cl.zzenner.cobranza.importacion.infraestructura.ImportacionMensualRepository;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionService;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = {
        "security.bcrypt.strength=4",
        "app.importacion.storage.directorio=/tmp/importaciones-test",
        "app.importacion.recuperacion.threshold-huerfanas=PT30S",
        "app.importacion.recuperacion.ttl-validadas=PT5M",
        "app.importacion.recuperacion.intervalo=PT30S",
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ImportacionAdminRestTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired TestRestTemplate rest;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;
    @Autowired CarteraService carteraService;
    @Autowired ImportacionMensualRepository importacionRepository;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JdbcTemplate jdbc;

    static final String CLAVE = "ClaveTest.123!";
    static final String BASE = "/api/v1/admin/importaciones/mensuales";
    static final String FIXTURE_VALIDO = "fixtures/importacion/importacion_valida_2026-08.csv";
    static final String FIXTURE_ERRORES = "fixtures/importacion/importacion_con_errores.csv";

    UUID idJefe;
    UUID idTecnologia;
    UUID idSupervisor;
    UUID idEjecutivoJlopez;
    UUID idEjecutivoMgarcia;
    UUID idActorDesactivado;
    UUID idActorBloqueado;
    UUID idActorBloqTemp;
    UUID idSupervisorSinPermiso;
    UUID carteraPrincipal;

    // Mantenemos el id de la importacion del flujo completo para compartirlo entre tests de orden
    UUID importacionFlujoCompleto;
    UUID carteraFlujoCompleto;

    @BeforeAll
    void prepararDatos() {
        // Usuarios con DATOS_IMPORTAR
        idJefe = usuarioService.crearUsuario("imp.jefe", "Jefe", "Importacion", null, null, CLAVE);
        usuarioService.asignarRol(idJefe, "JEFE_SUPERVISORES", null);

        idTecnologia = usuarioService.crearUsuario("imp.tec", "Tec", "Importacion", null, null, CLAVE);
        usuarioService.asignarRol(idTecnologia, "TECNOLOGIA", null);

        // Usuarios para pruebas de seguridad de actor
        idActorDesactivado = usuarioService.crearUsuario("imp.desact", "Desact", "User", null, null, CLAVE);
        usuarioService.asignarRol(idActorDesactivado, "JEFE_SUPERVISORES", null);
        jdbc.update("UPDATE cobranza.usuarios SET activo = FALSE WHERE id = ?", idActorDesactivado);

        idActorBloqueado = usuarioService.crearUsuario("imp.bloq", "Bloq", "User", null, null, CLAVE);
        usuarioService.asignarRol(idActorBloqueado, "JEFE_SUPERVISORES", null);
        jdbc.update("UPDATE cobranza.usuarios SET bloqueado = TRUE WHERE id = ?", idActorBloqueado);

        idActorBloqTemp = usuarioService.crearUsuario("imp.bloqtmp", "BloqTmp", "User", null, null, CLAVE);
        usuarioService.asignarRol(idActorBloqTemp, "JEFE_SUPERVISORES", null);
        jdbc.update("UPDATE cobranza.usuarios SET bloqueado_hasta = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().plus(Duration.ofHours(2))), idActorBloqTemp);

        idSupervisorSinPermiso = usuarioService.crearUsuario("imp.sup.nop", "Sup", "NoPermiso", null, null, CLAVE);
        usuarioService.asignarRol(idSupervisorSinPermiso, "SUPERVISOR", null);

        // Ejecutivos para el flujo completo (referenciados en el fixture CSV)
        idEjecutivoJlopez = usuarioService.crearUsuario("jlopez", "J", "Lopez", null, null, CLAVE);
        usuarioService.asignarRol(idEjecutivoJlopez, "EJECUTIVO_TERRENO", null);

        idEjecutivoMgarcia = usuarioService.crearUsuario("mgarcia", "M", "Garcia", null, null, CLAVE);
        usuarioService.asignarRol(idEjecutivoMgarcia, "EJECUTIVO_TERRENO", null);

        idSupervisor = usuarioService.crearUsuario("imp.supervisor", "Sup", "Principal", null, null, CLAVE);
        usuarioService.asignarRol(idSupervisor, "SUPERVISOR", null);
        supervisionService.asignarEjecutivo(idSupervisor, idEjecutivoJlopez, java.time.LocalDate.now());
        supervisionService.asignarEjecutivo(idSupervisor, idEjecutivoMgarcia, java.time.LocalDate.now());

        carteraPrincipal = carteraService.registrar("Cartera Principal Importacion", "Test").getId();
        carteraFlujoCompleto = carteraService.registrar("Cartera Flujo Completo", "Test").getId();
    }

    // ─── 1. Autenticación ─────────────────────────────────────────────────────

    @Test @Order(10)
    void sin_token_recibe_401() {
        ResponseEntity<String> resp = rest.getForEntity(BASE, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(11)
    void sin_permiso_datos_importar_recibe_403() {
        // SUPERVISOR con GESTIONES_VER pero sin DATOS_IMPORTAR
        String token = tokenPara(idSupervisorSinPermiso, "SUPERVISOR", List.of("GESTIONES_VER"));
        assertThat(get(BASE, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(12)
    void actor_desactivado_con_jwt_valido_recibe_403() {
        String token = tokenConPermiso(idActorDesactivado, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = subirCsv(token, carteraPrincipal, "2026-08", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(13)
    void actor_bloqueado_con_jwt_valido_recibe_403() {
        String token = tokenConPermiso(idActorBloqueado, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = subirCsv(token, carteraPrincipal, "2026-08", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(14)
    void actor_bloqueado_temporalmente_con_jwt_valido_recibe_403() {
        String token = tokenConPermiso(idActorBloqTemp, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = subirCsv(token, carteraPrincipal, "2026-08", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(15)
    void permiso_en_jwt_pero_no_en_bd_recibe_403() {
        // JWT reclama DATOS_IMPORTAR pero el usuario solo tiene SUPERVISOR sin ese permiso
        String token = tokenPara(idSupervisorSinPermiso, "SUPERVISOR", List.of("DATOS_IMPORTAR"));
        ResponseEntity<String> resp = subirCsv(token, carteraPrincipal, "2026-08", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─── 2. Validación de petición ────────────────────────────────────────────

    @Test @Order(20)
    void upload_sin_archivo_recibe_error_4xx() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("carteraId", carteraPrincipal.toString());
        body.add("periodo", "2026-08");
        ResponseEntity<String> resp = rest.exchange(BASE, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @Order(21)
    void upload_periodo_invalido_recibe_4xx() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = subirCsv(token, carteraPrincipal, "08-2026", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @Order(22)
    void upload_cartera_inexistente_recibe_404() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = subirCsv(token, UUID.randomUUID(), "2026-08", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("CARTERA_NO_ENCONTRADA");
    }

    // ─── 3. Upload → 202 + cuerpo enriquecido ────────────────────────────────

    @Test @Order(30)
    void jefe_puede_subir_csv_y_recibe_202_con_campos() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera 202 Jefe", "Test").getId();
        ResponseEntity<String> resp = subirCsv(token, c, "2025-01", FIXTURE_VALIDO);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getBody()).contains("importacionId");
        assertThat(resp.getBody()).contains("RECIBIDA");
        assertThat(resp.getBody()).contains("periodo");
        assertThat(resp.getBody()).contains("nombreArchivoOriginal");
        assertThat(resp.getHeaders().getLocation()).isNotNull();
    }

    @Test @Order(31)
    void tecnologia_puede_subir_csv_y_recibe_202() {
        String token = tokenConPermiso(idTecnologia, "TECNOLOGIA");
        UUID c = carteraService.registrar("Cartera 202 Tec", "Test").getId();
        ResponseEntity<String> resp = subirCsv(token, c, "2025-02", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    // ─── 4. GET /importaciones/mensuales ─────────────────────────────────────

    @Test @Order(40)
    void listar_retorna_paginacion_con_estructura_correcta() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(BASE, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"contenido\"");
        assertThat(resp.getBody()).contains("\"totalElementos\"");
        assertThat(resp.getBody()).contains("\"totalPaginas\"");
        assertThat(resp.getBody()).doesNotContain("rutaArchivo");
    }

    // ─── 5. Workflow async: CSV válido → VALIDADA ─────────────────────────────

    @Test @Order(50)
    void csv_valido_alcanza_estado_validada() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera Validada", "Test").getId();
        ResponseEntity<String> resp = subirCsv(token, c, "2025-03", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        String importacionId = extraerImportacionId(resp.getBody());

        Awaitility.await("validacion completa").atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    EstadoImportacion estado = importacionRepository.findById(UUID.fromString(importacionId))
                            .map(im -> im.getEstado()).orElse(null);
                    return estado == EstadoImportacion.VALIDADA || estado == EstadoImportacion.CON_ERRORES;
                });

        EstadoImportacion estadoFinal = importacionRepository.findById(UUID.fromString(importacionId))
                .map(im -> im.getEstado()).orElseThrow();
        assertThat(estadoFinal).isEqualTo(EstadoImportacion.VALIDADA);
    }

    @Test @Order(51)
    void antes_de_confirmar_no_existen_datos_de_dominio() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera PreValidacion", "Test").getId();
        ResponseEntity<String> resp = subirCsv(token, c, "2025-04", FIXTURE_VALIDO);
        String importacionId = extraerImportacionId(resp.getBody());

        Awaitility.await("esperando VALIDADA").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(UUID.fromString(importacionId))
                        .map(im -> im.getEstado() == EstadoImportacion.VALIDADA).orElse(false));

        // Verificar que NO se escribieron datos de dominio
        int personas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.personas WHERE rut_numero = '12345678'", Integer.class);
        int personasMgarcia = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.personas WHERE rut_numero = '98765432'", Integer.class);
        int cartPersonas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.carteras_personas WHERE cartera_id = ?::uuid", Integer.class, c.toString());

        assertThat(personas).isEqualTo(0);
        assertThat(personasMgarcia).isEqualTo(0);
        assertThat(cartPersonas).isEqualTo(0);
    }

    // ─── 6. Workflow async: CSV con errores → CON_ERRORES ────────────────────

    @Test @Order(60)
    void csv_con_rut_invalido_alcanza_estado_con_errores() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera ConErrores", "Test").getId();
        ResponseEntity<String> resp = subirCsv(token, c, "2025-05", FIXTURE_ERRORES);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String importacionId = extraerImportacionId(resp.getBody());

        Awaitility.await("estado con errores").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(UUID.fromString(importacionId))
                        .map(im -> im.getEstado() == EstadoImportacion.CON_ERRORES
                                   || im.getEstado() == EstadoImportacion.VALIDADA).orElse(false));

        EstadoImportacion estadoFinal = importacionRepository.findById(UUID.fromString(importacionId))
                .map(im -> im.getEstado()).orElseThrow();
        // RUT con DV inválido debe resultar en CON_ERRORES
        assertThat(estadoFinal).isEqualTo(EstadoImportacion.CON_ERRORES);
    }

    @Test @Order(61)
    void no_se_puede_confirmar_importacion_con_errores_recibe_404_o_409() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera Err Confirm", "Test").getId();
        ResponseEntity<String> resp = subirCsv(token, c, "2025-06", FIXTURE_ERRORES);
        String importacionId = extraerImportacionId(resp.getBody());

        Awaitility.await("CON_ERRORES").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(UUID.fromString(importacionId))
                        .map(im -> im.getEstado() == EstadoImportacion.CON_ERRORES).orElse(false));

        ResponseEntity<String> confirmar = post(BASE + "/" + importacionId + "/confirmar", null, token);
        assertThat(confirmar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(confirmar.getBody()).contains("ESTADO_INVALIDO_PARA_CONFIRMAR");
    }

    // ─── 7. Confirmar en RECIBIDA/VALIDANDO → 409 ─────────────────────────────

    @Test @Order(70)
    void confirmar_importacion_en_estado_recibida_recibe_409() {
        // Insert directly in RECIBIDA state to avoid timing races with async validation
        UUID importacionId = UUID.randomUUID();
        UUID c = carteraService.registrar("Cartera Recibida Confirm", "Test").getId();
        jdbc.update("""
            INSERT INTO cobranza.importaciones_mensuales
                (id, cartera_id, usuario_id, periodo, sistema_origen, estado,
                 hash_archivo, nombre_archivo_original)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, 'RECIBIDA', ?, ?)
            """,
            importacionId.toString(), c.toString(), idJefe.toString(),
            "2025-07", "LEGADO",
            "a".repeat(64), "test_recibida.csv");

        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> confirmar = post(BASE + "/" + importacionId + "/confirmar", null, token);
        assertThat(confirmar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(confirmar.getBody()).contains("ESTADO_INVALIDO_PARA_CONFIRMAR");
    }

    @Test @Order(71)
    void confirmar_importacion_inexistente_retorna_404() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = post(BASE + "/" + UUID.randomUUID() + "/confirmar", null, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("IMPORTACION_NO_ENCONTRADA");
    }

    // ─── 8. Flujo completo: VALIDADA → PROCESANDO → COMPLETADA ───────────────

    @Test @Order(80)
    void flujo_completo_recibe_validada() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = subirCsv(token, carteraFlujoCompleto, "2026-08", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        importacionFlujoCompleto = UUID.fromString(extraerImportacionId(resp.getBody()));

        Awaitility.await("VALIDADA flujo completo").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(importacionFlujoCompleto)
                        .map(im -> im.getEstado() == EstadoImportacion.VALIDADA).orElse(false));

        assertThat(importacionRepository.findById(importacionFlujoCompleto).get().getEstado())
                .isEqualTo(EstadoImportacion.VALIDADA);
    }

    @Test @Order(81)
    void flujo_completo_confirma_y_llega_a_completada() {
        assertThat(importacionFlujoCompleto).isNotNull();
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");

        ResponseEntity<String> confirmar = post(BASE + "/" + importacionFlujoCompleto + "/confirmar", null, token);
        assertThat(confirmar.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Awaitility.await("COMPLETADA flujo completo").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(importacionFlujoCompleto)
                        .map(im -> im.getEstado() == EstadoImportacion.COMPLETADA
                                   || im.getEstado() == EstadoImportacion.FALLIDA).orElse(false));

        assertThat(importacionRepository.findById(importacionFlujoCompleto).get().getEstado())
                .isEqualTo(EstadoImportacion.COMPLETADA);
    }

    @Test @Order(82)
    void flujo_completo_verificar_persona_creada() {
        assertThat(importacionFlujoCompleto).isNotNull();
        // Esperar COMPLETADA si el test anterior no corrió en orden
        Awaitility.await("COMPLETADA antes de verificar").atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(importacionFlujoCompleto)
                        .map(im -> im.getEstado() == EstadoImportacion.COMPLETADA).orElse(false));

        Integer personas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.personas WHERE rut_numero = '12345678' AND rut_dv = '5'",
                Integer.class);
        assertThat(personas).isGreaterThanOrEqualTo(1);

        Integer personasMgarcia = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.personas WHERE rut_numero = '98765432' AND rut_dv = '5'",
                Integer.class);
        assertThat(personasMgarcia).isGreaterThanOrEqualTo(1);
    }

    @Test @Order(83)
    void flujo_completo_verificar_operacion_y_cuotas() {
        Awaitility.await("COMPLETADA para verificar ops").atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(importacionFlujoCompleto)
                        .map(im -> im.getEstado() == EstadoImportacion.COMPLETADA).orElse(false));

        Integer operaciones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.operaciones WHERE numero_operacion = 'OP-001'", Integer.class);
        assertThat(operaciones).isGreaterThanOrEqualTo(1);

        Integer cuotas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.cuotas c "
                + "JOIN cobranza.operaciones o ON c.operacion_id = o.id "
                + "WHERE o.numero_operacion = 'OP-001'", Integer.class);
        assertThat(cuotas).isGreaterThanOrEqualTo(3);
    }

    @Test @Order(84)
    void flujo_completo_verificar_asignacion_mensual_y_supervisor() {
        Awaitility.await("COMPLETADA para verificar asignaciones").atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(importacionFlujoCompleto)
                        .map(im -> im.getEstado() == EstadoImportacion.COMPLETADA).orElse(false));

        // Debe existir asignación mensual para el ejecutivo jlopez
        Integer asignaciones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales am "
                + "JOIN cobranza.usuarios u ON am.ejecutivo_id = u.id "
                + "WHERE u.nombre_usuario = 'jlopez'", Integer.class);
        assertThat(asignaciones).isGreaterThanOrEqualTo(1);

        // El supervisor debe resolverse (supervisor_id no nulo)
        Integer conSupervisor = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales am "
                + "JOIN cobranza.usuarios u ON am.ejecutivo_id = u.id "
                + "WHERE u.nombre_usuario = 'jlopez' AND am.supervisor_id IS NOT NULL",
                Integer.class);
        assertThat(conSupervisor).isGreaterThanOrEqualTo(1);
    }

    @Test @Order(85)
    void flujo_completo_detalle_muestra_contadores() {
        assertThat(importacionFlujoCompleto).isNotNull();
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(BASE + "/" + importacionFlujoCompleto, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"estado\":\"COMPLETADA\"");
        assertThat(resp.getBody()).contains("personasCreadas");
        assertThat(resp.getBody()).doesNotContain("rutaArchivo");
    }

    // ─── 9. Reimportación idempotencia ────────────────────────────────────────

    @Test @Order(90)
    void mismo_csv_completado_da_409_archivo_ya_importado() {
        Awaitility.await("COMPLETADA antes de reimportar").atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(importacionFlujoCompleto)
                        .map(im -> im.getEstado() == EstadoImportacion.COMPLETADA).orElse(false));

        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        // Mismo archivo, mismo período, misma cartera → 409
        ResponseEntity<String> resp = subirCsv(token, carteraFlujoCompleto, "2026-08", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("ARCHIVO_YA_IMPORTADO");
    }

    @Test @Order(91)
    void periodo_anterior_al_completado_recibe_422() {
        Awaitility.await("COMPLETADA").atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(importacionFlujoCompleto)
                        .map(im -> im.getEstado() == EstadoImportacion.COMPLETADA).orElse(false));

        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        // Período anterior al último completado (2026-08) → 422
        UUID c2 = carteraService.registrar("Cartera Periodo Anterior", "Test").getId();
        // Primero completamos 2026-08 en c2...
        ResponseEntity<String> r1 = subirCsv(token, c2, "2026-08", FIXTURE_VALIDO);
        String id1 = extraerImportacionId(r1.getBody());
        Awaitility.await("VALIDADA c2").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(UUID.fromString(id1))
                        .map(im -> im.getEstado() == EstadoImportacion.VALIDADA).orElse(false));
        post(BASE + "/" + id1 + "/confirmar", null, token);
        Awaitility.await("COMPLETADA c2").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(UUID.fromString(id1))
                        .map(im -> im.getEstado() == EstadoImportacion.COMPLETADA).orElse(false));

        // Ahora intentar período 2025-01 (anterior a 2026-08) → 422
        ResponseEntity<String> resp = subirCsv(token, c2, "2025-01", FIXTURE_VALIDO);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).contains("PERIODO_ANTERIOR_NO_PERMITIDO");
    }

    // ─── 10. GET detalle y errores ────────────────────────────────────────────

    @Test @Order(100)
    void obtener_importacion_existente_retorna_detalle_completo() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera Detalle", "Test").getId();
        ResponseEntity<String> crearResp = subirCsv(token, c, "2024-01", FIXTURE_VALIDO);
        String importacionId = extraerImportacionId(crearResp.getBody());

        ResponseEntity<String> detalle = get(BASE + "/" + importacionId, token);
        assertThat(detalle.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detalle.getBody()).contains("\"estado\"");
        assertThat(detalle.getBody()).contains("\"periodo\"");
        assertThat(detalle.getBody()).contains("\"nombreArchivoOriginal\"");
        assertThat(detalle.getBody()).doesNotContain("rutaArchivo");
    }

    @Test @Order(101)
    void obtener_importacion_inexistente_retorna_404() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(BASE + "/" + UUID.randomUUID(), token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("IMPORTACION_NO_ENCONTRADA");
    }

    @Test @Order(102)
    void listar_errores_importacion_inexistente_retorna_404() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get(BASE + "/" + UUID.randomUUID() + "/errores", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(103)
    void listar_errores_de_importacion_con_errores_tiene_contenido() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera Err Lista", "Test").getId();
        ResponseEntity<String> resp = subirCsv(token, c, "2024-02", FIXTURE_ERRORES);
        String importacionId = extraerImportacionId(resp.getBody());

        Awaitility.await("CON_ERRORES para lista").atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .until(() -> importacionRepository.findById(UUID.fromString(importacionId))
                        .map(im -> im.getEstado() == EstadoImportacion.CON_ERRORES).orElse(false));

        ResponseEntity<String> errores = get(BASE + "/" + importacionId + "/errores", token);
        assertThat(errores.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(errores.getBody()).contains("\"contenido\"");
        // Debe tener al menos un error (RUT DV inválido según módulo 11)
        assertThat(errores.getBody()).contains("RUT_INVALIDO_MODULO_11");
    }

    // ─── 11. GET /carteras/activas ────────────────────────────────────────────

    @Test @Order(110)
    void jefe_puede_listar_carteras_activas() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        ResponseEntity<String> resp = get("/api/v1/admin/carteras/activas", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"id\"");
        assertThat(resp.getBody()).contains("\"nombre\"");
    }

    @Test @Order(111)
    void supervisor_no_puede_listar_carteras_activas() {
        String token = tokenPara(idSupervisorSinPermiso, "SUPERVISOR", List.of("GESTIONES_VER"));
        ResponseEntity<String> resp = get("/api/v1/admin/carteras/activas", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─── 12. Respuestas no exponen datos internos ─────────────────────────────

    @Test @Order(120)
    void respuesta_no_expone_ruta_fisica_ni_hash() {
        String token = tokenConPermiso(idJefe, "JEFE_SUPERVISORES");
        UUID c = carteraService.registrar("Cartera Privacidad", "Test").getId();
        ResponseEntity<String> crearResp = subirCsv(token, c, "2023-12", FIXTURE_VALIDO);
        String importacionId = extraerImportacionId(crearResp.getBody());

        ResponseEntity<String> detalle = get(BASE + "/" + importacionId, token);
        String body = detalle.getBody();
        assertThat(body).doesNotContain("/tmp");
        assertThat(body).doesNotContain("/var");
        assertThat(body).doesNotContain("rutaArchivo");
        assertThat(body).doesNotContain("hashArchivo");
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    private ResponseEntity<String> subirCsv(String token, UUID carteraId, String periodo, String recurso) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("carteraId", carteraId.toString());
        body.add("periodo", periodo);
        body.add("sistemaOrigen", "LEGADO");
        body.add("archivo", new ClassPathResource(recurso));
        return rest.exchange(BASE, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> post(String url, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private String extraerImportacionId(String json) {
        int start = json.indexOf("\"importacionId\":\"") + 17;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String tokenConPermiso(UUID usuarioId, String rol) {
        return tokenPara(usuarioId, rol, List.of("DATOS_IMPORTAR"));
    }

    private String tokenPara(UUID usuarioId, String rol, List<String> permisos) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cobranza-api-dev")
                .audience(List.of("cobranza-api"))
                .subject(usuarioId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString())
                .claim("preferred_username", "test.user")
                .claim("tipo_cliente", "WEB")
                .claim("roles", List.of(rol))
                .claim("permisos", permisos)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
