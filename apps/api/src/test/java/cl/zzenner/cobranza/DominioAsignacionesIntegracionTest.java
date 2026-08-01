package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.api.AsignacionConsultaApi;
import cl.zzenner.cobranza.asignaciones.api.DatosAsignacionMensual;
import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionService;
import cl.zzenner.cobranza.asignaciones.dominio.PersonaFueraDeAsignacionMensualException;
import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.carteras.dominio.Cartera;
import cl.zzenner.cobranza.personas.aplicacion.PersonaService;
import cl.zzenner.cobranza.personas.dominio.Persona;
import cl.zzenner.cobranza.personas.dominio.Rut;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionService;
import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
class DominioAsignacionesIntegracionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired JdbcTemplate jdbc;
    @Autowired AsignacionService asignacionService;
    @Autowired AsignacionConsultaApi asignacionConsultaApi;
    @Autowired CarteraService carteraService;
    @Autowired PersonaService personaService;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID crearSupervisor(String suffix) {
        UUID id = usuarioService.crearUsuario(
                "sup." + suffix, "Supervisor", suffix, "", "sup." + suffix + "@test.cl", "pass123");
        usuarioService.asignarRol(id, "SUPERVISOR", null);
        return id;
    }

    private UUID crearEjecutivo(String suffix) {
        UUID id = usuarioService.crearUsuario(
                "eje." + suffix, "Ejecutivo", suffix, "", "eje." + suffix + "@test.cl", "pass123");
        usuarioService.asignarRol(id, "EJECUTIVO_TERRENO", null);
        return id;
    }

    private UUID crearEjecutivoConSupervision(String suffix, UUID supervisorId) {
        UUID ejeId = crearEjecutivo(suffix);
        supervisionService.asignarEjecutivo(supervisorId, ejeId, LocalDate.now());
        return ejeId;
    }

    // RUTs para personas (distintos de los usados en DominioCobranzaIntegracionTest)
    // 21000000-3, 21000001-1, 21000002-K : usados en tests originales
    // 22000000-1, 22000001-K, 22000002-8, 22000003-6 : usados en tests originales
    // 23000000-K, 23000001-8, 23000002-6, 23000003-4, 23000004-2 : nuevos tests
    // 24000000-8, 24000001-6, 24000002-4 : nuevos tests
    private Persona crearPersonaEnCartera(Rut rut, String nombre, Cartera cartera) {
        Persona p = personaService.upsertPersona(rut, nombre, "LEGADO", null, java.time.Instant.now());
        personaService.vincularCartera(p.getId(), cartera.getId(), LocalDate.now());
        return p;
    }

    // ── Validación de roles y supervisión ────────────────────────────────────

    @Test
    void crear_mensual_rechaza_ejecutivo_sin_rol_ejecutivo_terreno() {
        UUID supId  = crearSupervisor("rol.eje.falta");
        // Usuario sin rol EJECUTIVO_TERRENO
        UUID sinRolId = usuarioService.crearUsuario(
                "eje.sin.rol", "Sin", "Rol", "", "eje.sin.rol@test.cl", "pass123");
        Cartera cartera = carteraService.registrar("Cartera Rol Eje", null);

        assertThatThrownBy(() -> asignacionService.crearAsignacionMensual(
                cartera.getId(), sinRolId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EJECUTIVO_TERRENO");
    }

    @Test
    void crear_mensual_rechaza_supervisor_sin_rol_supervisor() {
        UUID ejeId    = crearEjecutivo("rol.sup.falta");
        // Usuario sin rol SUPERVISOR
        UUID sinRolId = usuarioService.crearUsuario(
                "sup.sin.rol", "Sin", "RolSup", "", "sup.sin.rol@test.cl", "pass123");
        Cartera cartera = carteraService.registrar("Cartera Rol Sup", null);

        assertThatThrownBy(() -> asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, sinRolId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERVISOR");
    }

    @Test
    void crear_mensual_rechaza_sin_supervision_activa_entre_supervisor_y_ejecutivo() {
        UUID supId = crearSupervisor("sup.sin.supervision");
        UUID ejeId = crearEjecutivo("eje.sin.supervision");
        // NO se crea supervisión entre ellos
        Cartera cartera = carteraService.registrar("Cartera Sin Supervision", null);

        assertThatThrownBy(() -> asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supervisión activa");
    }

    // ── Existencia de tablas ──────────────────────────────────────────────────

    @Test
    void tabla_asignaciones_mensuales_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='asignaciones_mensuales'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void tabla_asignaciones_diarias_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='asignaciones_diarias'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ── AsignacionMensual ─────────────────────────────────────────────────────

    @Test
    void crear_asignacion_mensual_persiste_con_campos_correctos() {
        UUID supId = crearSupervisor("crear.am");
        UUID ejeId = crearEjecutivoConSupervision("crear.am", supId);
        Cartera cartera = carteraService.registrar("Cartera Test AM", null);

        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin    = LocalDate.of(2026, 8, 31);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId, inicio, fin, "Observacion test");

        assertThat(amId).isNotNull();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales WHERE id=? AND activa=TRUE",
                Integer.class, amId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void indice_unico_rechaza_dos_mensuales_activas_mismo_ejecutivo_cartera() {
        UUID supId = crearSupervisor("idx.am");
        UUID ejeId = crearEjecutivoConSupervision("idx.am", supId);
        Cartera cartera = carteraService.registrar("Cartera Idx AM", null);

        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin    = LocalDate.of(2026, 8, 31);

        asignacionService.crearAsignacionMensual(cartera.getId(), ejeId, supId, inicio, fin, null);

        // Insertar directamente para evadir validación de servicio y probar índice de BD
        assertThatThrownBy(() ->
                jdbc.update(
                        "INSERT INTO cobranza.asignaciones_mensuales" +
                        "(id, cartera_id, ejecutivo_id, supervisor_id, fecha_inicio, fecha_fin, activa, fecha_creacion, fecha_actualizacion, version)" +
                        " VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, TRUE, now(), now(), 0)",
                        cartera.getId(), ejeId, supId, inicio, fin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persona_puede_estar_activa_en_dos_carteras_distintas() {
        UUID supId = crearSupervisor("multi.cartera");
        UUID ejeId1 = crearEjecutivoConSupervision("multi.cartera.eje1", supId);
        UUID ejeId2 = crearEjecutivoConSupervision("multi.cartera.eje2", supId);

        Cartera cartA = carteraService.registrar("Cartera Multi A", null);
        Cartera cartB = carteraService.registrar("Cartera Multi B", null);

        // RUT 21000000-3
        Rut rut = Rut.of("21000000", "3");
        Persona persona = personaService.upsertPersona(rut, "Multi Persona", "LEGADO", null, java.time.Instant.now());
        personaService.vincularCartera(persona.getId(), cartA.getId(), LocalDate.now());
        personaService.vincularCartera(persona.getId(), cartB.getId(), LocalDate.now());

        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin    = LocalDate.of(2026, 8, 31);

        UUID amA = asignacionService.crearAsignacionMensual(cartA.getId(), ejeId1, supId, inicio, fin, null);
        UUID amB = asignacionService.crearAsignacionMensual(cartB.getId(), ejeId2, supId, inicio, fin, null);

        asignacionService.agregarPersonaAMensual(amA, persona.getId());
        asignacionService.agregarPersonaAMensual(amB, persona.getId());

        // Persona activa en ambas asignaciones (carteras distintas)
        Integer totalActivos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE persona_id=? AND activa=TRUE",
                Integer.class, persona.getId());
        assertThat(totalActivos).isEqualTo(2);
    }

    @Test
    void indice_unico_rechaza_persona_en_dos_mensuales_activas_misma_cartera() {
        UUID supId = crearSupervisor("idx.amp");
        UUID ejeId1 = crearEjecutivoConSupervision("idx.amp.eje1", supId);
        UUID ejeId2 = crearEjecutivoConSupervision("idx.amp.eje2", supId);

        Cartera cartera = carteraService.registrar("Cartera Idx AMP", null);

        // RUT 21000001-1
        Rut rut = Rut.of("21000001", "1");
        Persona persona = personaService.upsertPersona(rut, "Persona Idx", "LEGADO", null, java.time.Instant.now());
        personaService.vincularCartera(persona.getId(), cartera.getId(), LocalDate.now());

        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin    = LocalDate.of(2026, 8, 31);

        UUID amId = asignacionService.crearAsignacionMensual(cartera.getId(), ejeId1, supId, inicio, fin, null);
        asignacionService.agregarPersonaAMensual(amId, persona.getId());

        // Crear segunda mensual para otro ejecutivo en la misma cartera
        // y tratar de agregar la misma persona → debe fallar por índice parcial de BD
        UUID amId2 = asignacionService.crearAsignacionMensual(cartera.getId(), ejeId2, supId, inicio, fin, null);

        assertThatThrownBy(() ->
                jdbc.update(
                        "INSERT INTO cobranza.asignaciones_mensuales_personas" +
                        "(id, asignacion_mensual_id, persona_id, cartera_id, activa, fecha_inicio, fecha_creacion, fecha_actualizacion, version)" +
                        " VALUES (gen_random_uuid(), ?, ?, ?, TRUE, CURRENT_DATE, now(), now(), 0)",
                        amId2, persona.getId(), cartera.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void agregar_persona_sin_vinculo_a_cartera_lanza_excepcion() {
        UUID supId = crearSupervisor("sin.vinculo");
        UUID ejeId = crearEjecutivoConSupervision("sin.vinculo", supId);
        Cartera cartera = carteraService.registrar("Cartera Sin Vinculo", null);

        // RUT 21000002-K
        Rut rut = Rut.of("21000002", "K");
        Persona persona = personaService.upsertPersona(rut, "Persona Sin Vinculo", "LEGADO", null, java.time.Instant.now());
        // NO se vincula la persona a la cartera

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);

        assertThatThrownBy(() -> asignacionService.agregarPersonaAMensual(amId, persona.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no está activa en la cartera");
    }

    // ── AsignacionDiaria ──────────────────────────────────────────────────────

    @Test
    void publicar_diaria_sin_personas_lanza_excepcion() {
        UUID supId = crearSupervisor("pub.vacia");
        UUID ejeId = crearEjecutivoConSupervision("pub.vacia", supId);
        Cartera cartera = carteraService.registrar("Cartera Pub Vacia", null);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);

        UUID adId = asignacionService.crearAsignacionDiaria(amId, supId, LocalDate.of(2026, 8, 5));

        assertThatThrownBy(() -> asignacionService.publicarAsignacionDiaria(adId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sin personas");
    }

    @Test
    void publicar_diaria_registra_estado_y_fecha() {
        UUID supId = crearSupervisor("pub.ok");
        UUID ejeId = crearEjecutivoConSupervision("pub.ok", supId);
        Cartera cartera = carteraService.registrar("Cartera Pub Ok", null);

        // RUT 22000000-1
        Rut rut = Rut.of("22000000", "1");
        Persona persona = crearPersonaEnCartera(rut, "Persona Pub Ok", cartera);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, persona.getId());

        UUID adId = asignacionService.crearAsignacionDiaria(amId, supId, LocalDate.of(2026, 8, 5));
        asignacionService.agregarPersonaADiaria(adId, persona.getId());
        asignacionService.publicarAsignacionDiaria(adId);

        String estado = jdbc.queryForObject(
                "SELECT estado FROM cobranza.asignaciones_diarias WHERE id=?",
                String.class, adId);
        assertThat(estado).isEqualTo("PUBLICADA");

        Integer conFechaPub = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_diarias WHERE id=? AND fecha_publicacion IS NOT NULL",
                Integer.class, adId);
        assertThat(conFechaPub).isEqualTo(1);
    }

    @Test
    void agregar_persona_fuera_de_mensual_a_diaria_lanza_excepcion() {
        UUID supId = crearSupervisor("fuera.mensual");
        UUID ejeId = crearEjecutivoConSupervision("fuera.mensual", supId);
        Cartera cartera = carteraService.registrar("Cartera Fuera Mensual", null);

        // RUT 22000001-K
        Rut rutEnMensual = Rut.of("22000001", "K");
        Persona personaEnMensual = crearPersonaEnCartera(rutEnMensual, "Persona En Mensual", cartera);

        // RUT 22000002-8
        Rut rutFuera = Rut.of("22000002", "8");
        Persona personaFuera = crearPersonaEnCartera(rutFuera, "Persona Fuera", cartera);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, personaEnMensual.getId());

        UUID adId = asignacionService.crearAsignacionDiaria(amId, supId, LocalDate.of(2026, 8, 6));

        assertThatThrownBy(() -> asignacionService.agregarPersonaADiaria(adId, personaFuera.getId()))
                .isInstanceOf(PersonaFueraDeAsignacionMensualException.class);
    }

    @Test
    void cancelar_diaria_sin_motivo_rechazado_por_db() {
        UUID supId = crearSupervisor("cancel.db");
        UUID ejeId = crearEjecutivoConSupervision("cancel.db", supId);
        Cartera cartera = carteraService.registrar("Cartera Cancel DB", null);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);

        UUID adId = asignacionService.crearAsignacionDiaria(amId, supId, LocalDate.of(2026, 8, 7));

        // Actualizar directamente a CANCELADA sin motivo → viola CHECK en BD
        assertThatThrownBy(() ->
                jdbc.update(
                        "UPDATE cobranza.asignaciones_diarias SET estado='CANCELADA', motivo_cancelacion=NULL WHERE id=?",
                        adId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── AsignacionConsultaApi ─────────────────────────────────────────────────

    @Test
    void find_asignacion_mensual_activa_retorna_datos() {
        UUID supId = crearSupervisor("api.mensual");
        UUID ejeId = crearEjecutivoConSupervision("api.mensual", supId);
        Cartera cartera = carteraService.registrar("Cartera API Mensual", null);

        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin    = LocalDate.of(2026, 8, 31);

        asignacionService.crearAsignacionMensual(cartera.getId(), ejeId, supId, inicio, fin, null);

        var resultado = asignacionConsultaApi.findAsignacionMensualActiva(ejeId, cartera.getId());

        assertThat(resultado).isPresent();
        DatosAsignacionMensual datos = resultado.get();
        assertThat(datos.ejecutivoId()).isEqualTo(ejeId);
        assertThat(datos.carteraId()).isEqualTo(cartera.getId());
        assertThat(datos.activa()).isTrue();
    }

    @Test
    void persona_en_asignacion_mensual_activa_retorna_true() {
        UUID supId = crearSupervisor("api.persona");
        UUID ejeId = crearEjecutivoConSupervision("api.persona", supId);
        Cartera cartera = carteraService.registrar("Cartera API Persona", null);

        // RUT 22000003-6
        Rut rut = Rut.of("22000003", "6");
        Persona persona = crearPersonaEnCartera(rut, "Persona API", cartera);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, persona.getId());

        boolean activa = asignacionConsultaApi.personaEnAsignacionMensualActiva(
                persona.getId(), cartera.getId());
        assertThat(activa).isTrue();

        boolean noActiva = asignacionConsultaApi.personaEnAsignacionMensualActiva(
                UUID.randomUUID(), cartera.getId());
        assertThat(noActiva).isFalse();
    }

    // ── Historial individual y reasignación ───────────────────────────────────

    @Test
    void reasignar_persona_individual_no_afecta_al_resto() {
        UUID supId  = crearSupervisor("reasignar");
        UUID pedroId = crearEjecutivoConSupervision("reasignar.pedro", supId);
        UUID mariaId = crearEjecutivoConSupervision("reasignar.maria", supId);
        Cartera cartera = carteraService.registrar("Cartera Reasignacion", null);

        // 23000000-K, 23000001-8, 23000002-6
        Persona juan     = crearPersonaEnCartera(Rut.of("23000000", "K"), "Juan Reasignar", cartera);
        Persona persona2 = crearPersonaEnCartera(Rut.of("23000001", "8"), "Persona2 Reasignar", cartera);
        Persona persona3 = crearPersonaEnCartera(Rut.of("23000002", "6"), "Persona3 Reasignar", cartera);

        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin    = LocalDate.of(2026, 8, 31);

        UUID amPedro = asignacionService.crearAsignacionMensual(cartera.getId(), pedroId, supId, inicio, fin, null);
        asignacionService.agregarPersonaAMensual(amPedro, juan.getId());
        asignacionService.agregarPersonaAMensual(amPedro, persona2.getId());
        asignacionService.agregarPersonaAMensual(amPedro, persona3.getId());

        UUID amMaria = asignacionService.crearAsignacionMensual(cartera.getId(), mariaId, supId, inicio, fin, null);

        // Reasignar a Juan: remover de Pedro y agregar a María
        LocalDate fechaReasignacion = LocalDate.of(2026, 8, 15);
        asignacionService.removerPersonaDeMensual(amPedro, juan.getId(), fechaReasignacion);
        asignacionService.agregarPersonaAMensual(amMaria, juan.getId());

        // Persona2 y Persona3 siguen activos con Pedro
        Integer activosEnPedro = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND activa=TRUE",
                Integer.class, amPedro);
        assertThat(activosEnPedro).isEqualTo(2);

        // Juan está inactivo con Pedro
        Integer juanInactivoEnPedro = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND persona_id=? AND activa=FALSE",
                Integer.class, amPedro, juan.getId());
        assertThat(juanInactivoEnPedro).isEqualTo(1);

        // Juan está activo con María
        Integer juanActivoEnMaria = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND persona_id=? AND activa=TRUE",
                Integer.class, amMaria, juan.getId());
        assertThat(juanActivoEnMaria).isEqualTo(1);
    }

    @Test
    void historial_de_vinculo_conserva_fecha_inicio_y_fecha_fin() {
        UUID supId = crearSupervisor("historial.vinculo");
        UUID ejeId = crearEjecutivoConSupervision("historial.vinculo", supId);
        Cartera cartera = carteraService.registrar("Cartera Historial Vinculo", null);

        // 23000003-4
        Persona persona = crearPersonaEnCartera(Rut.of("23000003", "4"), "Persona Historial", cartera);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, persona.getId());

        LocalDate fechaCierre = LocalDate.of(2026, 8, 20);
        asignacionService.removerPersonaDeMensual(amId, persona.getId(), fechaCierre);

        // fecha_inicio debe estar registrada (no nula)
        LocalDate fechaInicioGuardada = jdbc.queryForObject(
                "SELECT fecha_inicio FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND persona_id=?",
                LocalDate.class, amId, persona.getId());
        assertThat(fechaInicioGuardada).isNotNull();

        // fecha_fin debe ser exactamente la fecha de cierre
        LocalDate fechaFinGuardada = jdbc.queryForObject(
                "SELECT fecha_fin FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND persona_id=? AND activa=FALSE",
                LocalDate.class, amId, persona.getId());
        assertThat(fechaFinGuardada).isEqualTo(fechaCierre);
    }

    @Test
    void bd_rechaza_cartera_id_diferente_al_de_mensual_padre() {
        UUID supId = crearSupervisor("fk.cartera");
        UUID ejeId = crearEjecutivoConSupervision("fk.cartera", supId);
        Cartera cartera = carteraService.registrar("Cartera FK Test", null);
        Cartera carteraErronea = carteraService.registrar("Cartera Erronea FK", null);

        // 23000004-2
        Persona persona = crearPersonaEnCartera(Rut.of("23000004", "2"), "Persona FK", cartera);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);

        // Intentar insertar con cartera_id diferente al de la mensual → FK compuesta debe rechazarlo
        assertThatThrownBy(() ->
                jdbc.update(
                        "INSERT INTO cobranza.asignaciones_mensuales_personas" +
                        "(id, asignacion_mensual_id, persona_id, cartera_id, activa, fecha_inicio, fecha_creacion, fecha_actualizacion, version)" +
                        " VALUES (gen_random_uuid(), ?, ?, ?, TRUE, CURRENT_DATE, now(), now(), 0)",
                        amId, persona.getId(), carteraErronea.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cerrar_mensual_desactiva_todos_vinculos_con_fecha_fin() {
        UUID supId = crearSupervisor("cerrar.mensual");
        UUID ejeId = crearEjecutivoConSupervision("cerrar.mensual", supId);
        Cartera cartera = carteraService.registrar("Cartera Cerrar Mensual", null);

        // 24000000-8, 24000001-6
        Persona p1 = crearPersonaEnCartera(Rut.of("24000000", "8"), "Persona Cerrar1", cartera);
        Persona p2 = crearPersonaEnCartera(Rut.of("24000001", "6"), "Persona Cerrar2", cartera);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, p1.getId());
        asignacionService.agregarPersonaAMensual(amId, p2.getId());

        asignacionService.cerrarAsignacionMensual(amId);

        // Ningún vínculo debe quedar activo
        Integer activosRestantes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND activa=TRUE",
                Integer.class, amId);
        assertThat(activosRestantes).isEqualTo(0);

        // Todos deben tener fecha_fin registrada
        Integer sinFechaFin = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND fecha_fin IS NULL",
                Integer.class, amId);
        assertThat(sinFechaFin).isEqualTo(0);
    }

    @Test
    void persona_puede_reincorporarse_a_mensual_activa_tras_cierre_individual() {
        UUID supId = crearSupervisor("reincorporar");
        UUID ejeId = crearEjecutivoConSupervision("reincorporar", supId);
        Cartera cartera = carteraService.registrar("Cartera Reincorporar", null);

        // 24000002-4
        Persona persona = crearPersonaEnCartera(Rut.of("24000002", "4"), "Persona Reincorporar", cartera);

        UUID amId = asignacionService.crearAsignacionMensual(
                cartera.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);

        asignacionService.agregarPersonaAMensual(amId, persona.getId());
        asignacionService.removerPersonaDeMensual(amId, persona.getId(), LocalDate.of(2026, 8, 10));
        asignacionService.agregarPersonaAMensual(amId, persona.getId());

        // Deben existir dos filas históricas: una cerrada y una activa
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND persona_id=?",
                Integer.class, amId, persona.getId());
        assertThat(total).isEqualTo(2);

        Integer activos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas WHERE asignacion_mensual_id=? AND persona_id=? AND activa=TRUE",
                Integer.class, amId, persona.getId());
        assertThat(activos).isEqualTo(1);
    }
}
