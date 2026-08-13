package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionService;
import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.carteras.dominio.Cartera;
import cl.zzenner.cobranza.gestiones.aplicacion.ComandoCrearGestion;
import cl.zzenner.cobranza.gestiones.aplicacion.GestionService;
import cl.zzenner.cobranza.gestiones.api.DatosGestion;
import cl.zzenner.cobranza.gestiones.api.GestionConsultaApi;
import cl.zzenner.cobranza.gestiones.dominio.Gestion;
import cl.zzenner.cobranza.gestiones.api.GestionConflictivaException;
import cl.zzenner.cobranza.gestiones.dominio.OrigenGestion;
import cl.zzenner.cobranza.gestiones.dominio.TipoGestion;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
class GestionesIntegracionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired JdbcTemplate jdbc;
    @Autowired GestionService gestionService;
    @Autowired GestionConsultaApi gestionConsultaApi;
    @Autowired AsignacionService asignacionService;
    @Autowired CarteraService carteraService;
    @Autowired PersonaService personaService;
    @Autowired UsuarioService usuarioService;
    @Autowired SupervisionService supervisionService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID crearSupervisor(String suffix) {
        UUID id = usuarioService.crearUsuario(
                "gsup." + suffix, "Supervisor", suffix, "", "gsup." + suffix + "@test.cl", "pass123");
        usuarioService.asignarRol(id, "SUPERVISOR", null);
        return id;
    }

    private UUID crearEjecutivo(String suffix) {
        UUID id = usuarioService.crearUsuario(
                "geje." + suffix, "Ejecutivo", suffix, "", "geje." + suffix + "@test.cl", "pass123");
        usuarioService.asignarRol(id, "EJECUTIVO_TERRENO", null);
        return id;
    }

    private UUID crearEjecutivoConSupervision(String suffix, UUID supervisorId) {
        UUID ejeId = crearEjecutivo(suffix);
        supervisionService.asignarEjecutivo(supervisorId, ejeId, LocalDate.now());
        return ejeId;
    }

    private Persona crearPersonaEnCartera(Rut rut, String nombre, Cartera cartera) {
        Persona p = personaService.upsertPersona(rut, nombre, "LEGADO", null, Instant.now());
        personaService.vincularCartera(p.getId(), cartera.getId(), LocalDate.now());
        return p;
    }

    // Crea una diaria publicada con la persona incluida y devuelve el id
    private UUID crearDiariaPublicadaConPersona(UUID amId, UUID supId, LocalDate fecha, UUID personaId) {
        UUID adId = asignacionService.crearAsignacionDiaria(amId, supId, fecha);
        asignacionService.agregarPersonaADiaria(adId, personaId);
        asignacionService.publicarAsignacionDiaria(adId, null);
        return adId;
    }

    private ComandoCrearGestion comandoAsignacionDiaria(UUID diariaId, UUID personaId, UUID ejecutivoId) {
        return new ComandoCrearGestion(
            UUID.randomUUID(), "ASIGNACION_DIARIA", diariaId, personaId, ejecutivoId,
            "SIN_CONTACTO", Instant.now(), "obs", null,
            -33.45, -70.66, 10f, "GPS", false, Instant.now(), null
        );
    }

    private ComandoCrearGestion comandoBusquedaDirecta(UUID personaId, UUID ejecutivoId) {
        return new ComandoCrearGestion(
            UUID.randomUUID(), "BUSQUEDA_DIRECTA", null, personaId, ejecutivoId,
            "SIN_CONTACTO", Instant.now(), null, null,
            -33.45, -70.66, 10f, null, false, Instant.now(), null
        );
    }

    // ── Test 1: ASIGNACION_DIARIA desde diaria PUBLICADA ─────────────────────

    @Test
    void gestion_desde_diaria_publicada() {
        UUID supId = crearSupervisor("g1");
        UUID ejeId = crearEjecutivoConSupervision("g1", supId);
        Cartera c  = carteraService.registrar("Cartera G1", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000000", "6"), "Persona G1", c);

        UUID amId = asignacionService.crearAsignacionMensual(
                c.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, p.getId());
        UUID adId = crearDiariaPublicadaConPersona(amId, supId, LocalDate.of(2026, 8, 4), p.getId());

        ComandoCrearGestion cmd = comandoAsignacionDiaria(adId, p.getId(), ejeId);
        assertThatCode(() -> gestionService.recibirGestion(cmd)).doesNotThrowAnyException();

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.gestiones WHERE id=?", Integer.class, cmd.id());
        assertThat(count).isEqualTo(1);
    }

    // ── Test 2: Gestión offline sincronizada con diaria FINALIZADA ─────────

    @Test
    void gestion_offline_con_diaria_finalizada() {
        UUID supId = crearSupervisor("g2");
        UUID ejeId = crearEjecutivoConSupervision("g2", supId);
        Cartera c  = carteraService.registrar("Cartera G2", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000001", "4"), "Persona G2", c);

        UUID amId = asignacionService.crearAsignacionMensual(
                c.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, p.getId());
        UUID adId = crearDiariaPublicadaConPersona(amId, supId, LocalDate.of(2026, 8, 5), p.getId());
        asignacionService.finalizarAsignacionDiaria(adId);

        String estado = jdbc.queryForObject(
                "SELECT estado FROM cobranza.asignaciones_diarias WHERE id=?", String.class, adId);
        assertThat(estado).isEqualTo("FINALIZADA");

        ComandoCrearGestion cmd = comandoAsignacionDiaria(adId, p.getId(), ejeId);
        assertThatCode(() -> gestionService.recibirGestion(cmd)).doesNotThrowAnyException();
    }

    // ── Test 3: Rechazo si diaria en BORRADOR ────────────────────────────────

    @Test
    void gestion_rechazada_si_diaria_en_borrador() {
        UUID supId = crearSupervisor("g3");
        UUID ejeId = crearEjecutivoConSupervision("g3", supId);
        Cartera c  = carteraService.registrar("Cartera G3", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000002", "2"), "Persona G3", c);

        UUID amId = asignacionService.crearAsignacionMensual(
                c.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, p.getId());
        // Diaria en BORRADOR (no publicada)
        UUID adId = asignacionService.crearAsignacionDiaria(amId, supId, LocalDate.of(2026, 8, 6));
        asignacionService.agregarPersonaADiaria(adId, p.getId());

        assertThatThrownBy(() -> gestionService.recibirGestion(
                comandoAsignacionDiaria(adId, p.getId(), ejeId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BORRADOR");
    }

    // ── Test 4: Rechazo si diaria CANCELADA ──────────────────────────────────

    @Test
    void gestion_rechazada_si_diaria_cancelada() {
        UUID supId = crearSupervisor("g4");
        UUID ejeId = crearEjecutivoConSupervision("g4", supId);
        Cartera c  = carteraService.registrar("Cartera G4", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000003", "0"), "Persona G4", c);

        UUID amId = asignacionService.crearAsignacionMensual(
                c.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, p.getId());
        UUID adId = crearDiariaPublicadaConPersona(amId, supId, LocalDate.of(2026, 8, 7), p.getId());
        asignacionService.cancelarAsignacionDiaria(adId, "cancelada por test");

        assertThatThrownBy(() -> gestionService.recibirGestion(
                comandoAsignacionDiaria(adId, p.getId(), ejeId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELADA");
    }

    // ── Test 5: Rechazo si persona no está en la diaria ──────────────────────

    @Test
    void gestion_rechazada_si_persona_no_en_diaria() {
        UUID supId = crearSupervisor("g5");
        UUID ejeId = crearEjecutivoConSupervision("g5", supId);
        Cartera c  = carteraService.registrar("Cartera G5", null);
        Persona pEnDiaria  = crearPersonaEnCartera(Rut.of("25000004", "9"), "Persona G5a", c);
        Persona pFuera     = crearPersonaEnCartera(Rut.of("25000005", "7"), "Persona G5b", c);

        UUID amId = asignacionService.crearAsignacionMensual(
                c.getId(), ejeId, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, pEnDiaria.getId());
        asignacionService.agregarPersonaAMensual(amId, pFuera.getId());
        // Solo pEnDiaria está en la diaria
        UUID adId = crearDiariaPublicadaConPersona(amId, supId, LocalDate.of(2026, 8, 8), pEnDiaria.getId());

        assertThatThrownBy(() -> gestionService.recibirGestion(
                comandoAsignacionDiaria(adId, pFuera.getId(), ejeId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    // ── Test 6: Rechazo si ejecutivo no es el destinatario ───────────────────

    @Test
    void gestion_rechazada_si_ejecutivo_no_es_destinatario() {
        UUID supId  = crearSupervisor("g6");
        UUID ejeId1 = crearEjecutivoConSupervision("g6a", supId);
        UUID ejeId2 = crearEjecutivoConSupervision("g6b", supId);
        Cartera c  = carteraService.registrar("Cartera G6", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000006", "5"), "Persona G6", c);

        UUID amId = asignacionService.crearAsignacionMensual(
                c.getId(), ejeId1, supId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        asignacionService.agregarPersonaAMensual(amId, p.getId());
        // Diaria pertenece a ejeId1
        UUID adId = crearDiariaPublicadaConPersona(amId, supId, LocalDate.of(2026, 8, 9), p.getId());

        // ejeId2 intenta gestionar con la diaria de ejeId1
        assertThatThrownBy(() -> gestionService.recibirGestion(
                comandoAsignacionDiaria(adId, p.getId(), ejeId2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no es el destinatario");
    }

    // ── Test 7: BUSQUEDA_DIRECTA sin asignación ───────────────────────────────

    @Test
    void gestion_busqueda_directa_sin_asignacion() {
        UUID supId = crearSupervisor("g7");
        UUID ejeId = crearEjecutivoConSupervision("g7", supId);
        Cartera c  = carteraService.registrar("Cartera G7", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000007", "3"), "Persona G7", c);

        ComandoCrearGestion cmd = comandoBusquedaDirecta(p.getId(), ejeId);
        assertThatCode(() -> gestionService.recibirGestion(cmd)).doesNotThrowAnyException();

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.gestiones WHERE id=? AND origen_gestion='BUSQUEDA_DIRECTA'",
                Integer.class, cmd.id());
        assertThat(count).isEqualTo(1);
    }

    // ── Test 8: BUSQUEDA_DIRECTA persona de cartera diferente ────────────────

    @Test
    void gestion_directa_persona_de_otra_cartera() {
        UUID supId = crearSupervisor("g8");
        UUID ejeId = crearEjecutivoConSupervision("g8", supId);
        Cartera cA = carteraService.registrar("Cartera G8A", null);
        Cartera cB = carteraService.registrar("Cartera G8B", null);
        // Persona vinculada a cartera B (diferente a la cartera del ejecutivo)
        Persona p  = crearPersonaEnCartera(Rut.of("25000008", "1"), "Persona G8", cB);

        // BUSQUEDA_DIRECTA no tiene restricción de cartera
        ComandoCrearGestion cmd = comandoBusquedaDirecta(p.getId(), ejeId);
        assertThatCode(() -> gestionService.recibirGestion(cmd)).doesNotThrowAnyException();
    }

    // ── Test 9: BUSQUEDA_DIRECTA rechaza asignacion_diaria_id informado ───────

    @Test
    void gestion_busqueda_directa_rechaza_con_asignacion_diaria_id() {
        UUID fakePersonaId  = UUID.randomUUID();
        UUID fakeEjecutivoId = UUID.randomUUID();
        UUID fakeDiariaId   = UUID.randomUUID();

        // El constructor de Gestion valida: BUSQUEDA_DIRECTA + diariaId != null → error
        assertThatThrownBy(() -> new Gestion(
                UUID.randomUUID(), OrigenGestion.BUSQUEDA_DIRECTA,
                fakeDiariaId, fakePersonaId, fakeEjecutivoId,
                TipoGestion.SIN_CONTACTO,
                Instant.now(), null, null,
                -33.45, -70.66, 10f, null, false, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BUSQUEDA_DIRECTA");
    }

    // ── Test 10: ASIGNACION_DIARIA rechaza sin asignacion_diaria_id ──────────

    @Test
    void gestion_asignacion_diaria_rechaza_sin_asignacion_diaria_id() {
        UUID fakePersonaId   = UUID.randomUUID();
        UUID fakeEjecutivoId = UUID.randomUUID();

        assertThatThrownBy(() -> new Gestion(
                UUID.randomUUID(), OrigenGestion.ASIGNACION_DIARIA,
                null, fakePersonaId, fakeEjecutivoId,
                TipoGestion.SIN_CONTACTO,
                Instant.now(), null, null,
                -33.45, -70.66, 10f, null, false, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ASIGNACION_DIARIA requiere");
    }

    // ── Test 11: Idempotencia — mismo UUID, mismo contenido ──────────────────

    @Test
    void gestion_idempotente_mismo_uuid_mismo_contenido() {
        UUID supId = crearSupervisor("g11");
        UUID ejeId = crearEjecutivoConSupervision("g11", supId);
        Cartera c  = carteraService.registrar("Cartera G11", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000009", "K"), "Persona G11", c);

        ComandoCrearGestion cmd = comandoBusquedaDirecta(p.getId(), ejeId);
        gestionService.recibirGestion(cmd);

        // Segunda llamada con mismo UUID y mismo contenido → éxito idempotente
        assertThatCode(() -> gestionService.recibirGestion(cmd)).doesNotThrowAnyException();

        // Solo debe existir 1 fila
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.gestiones WHERE id=?", Integer.class, cmd.id());
        assertThat(count).isEqualTo(1);
    }

    // ── Test 12: Conflicto — mismo UUID, contenido diferente ─────────────────

    @Test
    void gestion_conflicto_mismo_uuid_diferente_contenido() {
        UUID supId = crearSupervisor("g12");
        UUID ejeId = crearEjecutivoConSupervision("g12", supId);
        Cartera c  = carteraService.registrar("Cartera G12", null);
        Persona p1 = crearPersonaEnCartera(Rut.of("25000010", "3"), "Persona G12a", c);
        Persona p2 = crearPersonaEnCartera(Rut.of("25000011", "1"), "Persona G12b", c);

        UUID idShared = UUID.randomUUID();
        Instant ts = Instant.now();

        ComandoCrearGestion cmd1 = new ComandoCrearGestion(
            idShared, "BUSQUEDA_DIRECTA", null, p1.getId(), ejeId,
            "SIN_CONTACTO", ts, null, null, -33.45, -70.66, 10f, null, false, ts, null);

        ComandoCrearGestion cmd2 = new ComandoCrearGestion(
            idShared, "BUSQUEDA_DIRECTA", null, p2.getId(), ejeId,  // persona diferente
            "SIN_CONTACTO", ts, null, null, -33.45, -70.66, 10f, null, false, ts, null);

        gestionService.recibirGestion(cmd1);

        assertThatThrownBy(() -> gestionService.recibirGestion(cmd2))
                .isInstanceOf(GestionConflictivaException.class)
                .hasMessageContaining(idShared.toString());
    }

    // ── Test 13: COMPROMISO_PAGO sin fecha_compromiso ────────────────────────

    @Test
    void compromiso_pago_rechazado_sin_fecha_compromiso() {
        UUID fakeId = UUID.randomUUID();
        assertThatThrownBy(() -> new Gestion(
                fakeId, OrigenGestion.BUSQUEDA_DIRECTA,
                null, UUID.randomUUID(), UUID.randomUUID(),
                TipoGestion.COMPROMISO_PAGO,
                Instant.now(), null, null,
                -33.0, -70.0, 5f, null, false, Instant.now(), null))  // fechaCompromiso = null
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMPROMISO_PAGO requiere fecha_compromiso");
    }

    // ── Test 14: Tipo sin compromiso con fecha_compromiso ────────────────────

    @Test
    void tipo_sin_compromiso_rechazado_con_fecha_compromiso() {
        assertThatThrownBy(() -> new Gestion(
                UUID.randomUUID(), OrigenGestion.BUSQUEDA_DIRECTA,
                null, UUID.randomUUID(), UUID.randomUUID(),
                TipoGestion.SIN_CONTACTO,
                Instant.now(), null, null,
                -33.0, -70.0, 5f, null, false, Instant.now(),
                LocalDate.of(2026, 8, 20)))  // fecha_compromiso informada para tipo SIN_CONTACTO
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo COMPROMISO_PAGO");
    }

    // ── Test 15: Coordenadas fuera de rango ──────────────────────────────────

    @Test
    void gestion_rechazada_con_coordenadas_fuera_de_rango() {
        // latitud > 90
        assertThatThrownBy(() -> new Gestion(
                UUID.randomUUID(), OrigenGestion.BUSQUEDA_DIRECTA,
                null, UUID.randomUUID(), UUID.randomUUID(),
                TipoGestion.SIN_CONTACTO,
                Instant.now(), null, null,
                91.0, -70.0, 5f, null, false, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitud fuera de rango");

        // longitud > 180
        assertThatThrownBy(() -> new Gestion(
                UUID.randomUUID(), OrigenGestion.BUSQUEDA_DIRECTA,
                null, UUID.randomUUID(), UUID.randomUUID(),
                TipoGestion.SIN_CONTACTO,
                Instant.now(), null, null,
                -33.0, 181.0, 5f, null, false, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitud fuera de rango");
    }

    // ── Test 21: Concurrencia — mismo UUID, mismo contenido, 5 hilos simultáneos ──

    @Test
    void gestion_concurrente_mismo_uuid_idempotente() throws InterruptedException {
        UUID supId = crearSupervisor("g21");
        UUID ejeId = crearEjecutivoConSupervision("g21", supId);
        Cartera c  = carteraService.registrar("Cartera G21", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000014", "6"), "Persona G21", c);

        ComandoCrearGestion cmd = comandoBusquedaDirecta(p.getId(), ejeId);

        int hilos = 5;
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fin    = new CountDownLatch(hilos);
        List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                try {
                    inicio.await();
                    gestionService.recibirGestion(cmd);
                } catch (Throwable t) {
                    errores.add(t);
                } finally {
                    fin.countDown();
                }
            });
        }

        inicio.countDown();
        assertThat(fin.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Ningún hilo debe haber lanzado excepción
        assertThat(errores).isEmpty();

        // Exactamente 1 fila en BD — el INSERT atómico garantiza no duplicados
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.gestiones WHERE id=?", Integer.class, cmd.id());
        assertThat(count).isEqualTo(1);
    }

    // ── Test 16: precision_metros negativa ───────────────────────────────────

    @Test
    void gestion_rechazada_con_precision_negativa() {
        assertThatThrownBy(() -> new Gestion(
                UUID.randomUUID(), OrigenGestion.BUSQUEDA_DIRECTA,
                null, UUID.randomUUID(), UUID.randomUUID(),
                TipoGestion.SIN_CONTACTO,
                Instant.now(), null, null,
                -33.0, -70.0, -1f, null, false, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precision_metros no puede ser negativa");
    }

    // ── Test 17: Rol EJECUTIVO_TERRENO obligatorio ────────────────────────────

    @Test
    void gestion_rechazada_si_ejecutivo_sin_rol_ejecutivo_terreno() {
        // Usuario sin ningún rol
        UUID sinRolId = usuarioService.crearUsuario(
                "geje.sin.rol", "Sin", "Rol", "", "geje.sin.rol@test.cl", "pass123");
        UUID supId = crearSupervisor("g17");
        Cartera c  = carteraService.registrar("Cartera G17", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000012", "K"), "Persona G17", c);

        assertThatThrownBy(() -> gestionService.recibirGestion(
                comandoBusquedaDirecta(p.getId(), sinRolId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EJECUTIVO_TERRENO");
    }

    // ── Test 18: Tabla gestiones no tiene columna fecha_actualizacion ─────────

    @Test
    void tabla_gestiones_no_tiene_columna_fecha_actualizacion() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns"
                + " WHERE table_schema='cobranza' AND table_name='gestiones'"
                + " AND column_name='fecha_actualizacion'",
                Integer.class);
        assertThat(count).isEqualTo(0);
    }

    // ── Test 19: Spring Modulith verifica modularidad ─────────────────────────

    @Test
    void modularidad_gestiones_pasa_verificacion() {
        ApplicationModules modules = ApplicationModules.of(CobranzaApplication.class);
        assertThatCode(modules::verify).doesNotThrowAnyException();
    }

    // ── Test 22: Concurrencia — mismo UUID, contenido conflictivo ────────────

    @Test
    void gestion_concurrente_mismo_uuid_contenido_conflictivo() throws InterruptedException {
        UUID supId = crearSupervisor("g22");
        UUID ejeId = crearEjecutivoConSupervision("g22", supId);
        Cartera c  = carteraService.registrar("Cartera G22", null);
        Persona p1 = crearPersonaEnCartera(Rut.of("25000015", "4"), "Persona G22a", c);
        Persona p2 = crearPersonaEnCartera(Rut.of("25000016", "2"), "Persona G22b", c);

        UUID idShared = UUID.randomUUID();
        Instant ts = Instant.now();

        // Mismo UUID, personas distintas → contenido conflictivo
        ComandoCrearGestion cmdA = new ComandoCrearGestion(
            idShared, "BUSQUEDA_DIRECTA", null, p1.getId(), ejeId,
            "SIN_CONTACTO", ts, null, null, -33.45, -70.66, 10f, null, false, ts, null);
        ComandoCrearGestion cmdB = new ComandoCrearGestion(
            idShared, "BUSQUEDA_DIRECTA", null, p2.getId(), ejeId,
            "SIN_CONTACTO", ts, null, null, -33.45, -70.66, 10f, null, false, ts, null);

        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fin    = new CountDownLatch(2);
        AtomicInteger exitos  = new AtomicInteger(0);
        List<Throwable> erroresInesperados = Collections.synchronizedList(new ArrayList<>());

        Thread tA = new Thread(() -> {
            try { inicio.await(); gestionService.recibirGestion(cmdA); exitos.incrementAndGet(); }
            catch (GestionConflictivaException ignored) { /* esperado para el perdedor */ }
            catch (Throwable t) { erroresInesperados.add(t); }
            finally { fin.countDown(); }
        });
        Thread tB = new Thread(() -> {
            try { inicio.await(); gestionService.recibirGestion(cmdB); exitos.incrementAndGet(); }
            catch (GestionConflictivaException ignored) { /* esperado para el perdedor */ }
            catch (Throwable t) { erroresInesperados.add(t); }
            finally { fin.countDown(); }
        });
        tA.start(); tB.start();

        inicio.countDown();
        assertThat(fin.await(10, TimeUnit.SECONDS)).isTrue();

        // Sin errores inesperados; exactamente un ganador y una sola fila
        assertThat(erroresInesperados).isEmpty();
        assertThat(exitos.get()).isEqualTo(1);
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cobranza.gestiones WHERE id=?", Integer.class, idShared);
        assertThat(count).isEqualTo(1);
    }

    // ── Test 23: Reintento idempotente no modifica fecha_creacion_servidor ────
    // Cubre también: fecha_creacion_servidor excluida de comparación de contenido

    @Test
    void idempotencia_no_modifica_fecha_creacion_servidor() {
        UUID supId = crearSupervisor("g23");
        UUID ejeId = crearEjecutivoConSupervision("g23", supId);
        Cartera c  = carteraService.registrar("Cartera G23", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000017", "0"), "Persona G23", c);

        ComandoCrearGestion cmd = comandoBusquedaDirecta(p.getId(), ejeId);

        // Primera inserción
        gestionService.recibirGestion(cmd);
        Instant fcs1 = gestionConsultaApi.findUltimasGestiones(p.getId(), 1)
            .get(0).fechaCreacionServidor();

        // Reintento idempotente con el mismo cmd (mismo UUID, mismo contenido)
        gestionService.recibirGestion(cmd);
        Instant fcs2 = gestionConsultaApi.findUltimasGestiones(p.getId(), 1)
            .get(0).fechaCreacionServidor();

        // fecha_creacion_servidor no debe cambiar en el reintento
        assertThat(fcs2).isEqualTo(fcs1);
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cobranza.gestiones WHERE id=?", Integer.class, cmd.id());
        assertThat(count).isEqualTo(1);
    }

    // ── Test 24: fecha_creacion_servidor generada en servidor, no en dispositivo

    @Test
    void fecha_creacion_servidor_generada_en_servidor() {
        UUID supId = crearSupervisor("g24");
        UUID ejeId = crearEjecutivoConSupervision("g24", supId);
        Cartera c  = carteraService.registrar("Cartera G24", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000018", "9"), "Persona G24", c);

        // fecha del dispositivo fija en el pasado — claramente distinta de la hora del servidor
        Instant fechaGestionDispositivo = Instant.parse("2026-08-01T10:00:00Z");
        ComandoCrearGestion cmd = new ComandoCrearGestion(
            UUID.randomUUID(), "BUSQUEDA_DIRECTA", null, p.getId(), ejeId,
            "SIN_CONTACTO", fechaGestionDispositivo, null, null,
            -33.45, -70.66, 10f, null, false, fechaGestionDispositivo, null);

        Instant antes = Instant.now();
        gestionService.recibirGestion(cmd);
        Instant despues = Instant.now();

        DatosGestion g = gestionConsultaApi.findUltimasGestiones(p.getId(), 1).get(0);

        // fecha_creacion_servidor entre antes y después de la llamada (generada en servidor)
        assertThat(g.fechaCreacionServidor()).isAfterOrEqualTo(antes).isBeforeOrEqualTo(despues);
        // fecha_creacion_servidor distinta de fecha_gestion del dispositivo
        assertThat(g.fechaCreacionServidor()).isNotEqualTo(g.fechaGestion());
        // fecha_gestion preserva el valor del dispositivo
        assertThat(g.fechaGestion()).isEqualTo(fechaGestionDispositivo);
    }

    // ── Test 25: Conflicto no modifica la gestión original ───────────────────

    @Test
    void conflicto_no_modifica_gestion_original() {
        UUID supId = crearSupervisor("g25");
        UUID ejeId = crearEjecutivoConSupervision("g25", supId);
        Cartera c  = carteraService.registrar("Cartera G25", null);
        Persona p1 = crearPersonaEnCartera(Rut.of("25000019", "7"), "Persona G25a", c);
        Persona p2 = crearPersonaEnCartera(Rut.of("25000020", "0"), "Persona G25b", c);

        UUID idShared = UUID.randomUUID();
        Instant ts = Instant.now();

        ComandoCrearGestion cmd1 = new ComandoCrearGestion(
            idShared, "BUSQUEDA_DIRECTA", null, p1.getId(), ejeId,
            "SIN_CONTACTO", ts, null, null, -33.45, -70.66, 10f, null, false, ts, null);
        ComandoCrearGestion cmd2 = new ComandoCrearGestion(
            idShared, "BUSQUEDA_DIRECTA", null, p2.getId(), ejeId, // persona distinta
            "SIN_CONTACTO", ts, null, null, -33.45, -70.66, 10f, null, false, ts, null);

        gestionService.recibirGestion(cmd1);

        // La segunda solicitud con contenido distinto debe rechazarse
        assertThatThrownBy(() -> gestionService.recibirGestion(cmd2))
            .isInstanceOf(GestionConflictivaException.class);

        // La gestión original debe permanecer inalterada
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cobranza.gestiones WHERE id=?", Integer.class, idShared);
        assertThat(count).isEqualTo(1);

        // persona_id en BD corresponde a la primera inserción (p1), no a la rechazada (p2)
        String personaIdBD = jdbc.queryForObject(
            "SELECT persona_id::text FROM cobranza.gestiones WHERE id=?", String.class, idShared);
        assertThat(UUID.fromString(personaIdBD)).isEqualTo(p1.getId());
    }

    // ── Test 20: Persistencia completa de todos los campos ────────────────────

    @Test
    void gestion_persiste_todos_los_campos_gps_correctamente() {
        UUID supId = crearSupervisor("g20");
        UUID ejeId = crearEjecutivoConSupervision("g20", supId);
        Cartera c  = carteraService.registrar("Cartera G20", null);
        Persona p  = crearPersonaEnCartera(Rut.of("25000013", "8"), "Persona G20", c);

        Instant fechaGestion    = Instant.parse("2026-08-01T14:00:00Z");
        Instant fechaCapturaGps = Instant.parse("2026-08-01T13:59:55Z");

        ComandoCrearGestion cmd = new ComandoCrearGestion(
            UUID.randomUUID(), "BUSQUEDA_DIRECTA", null, p.getId(), ejeId,
            "COMPROMISO_PAGO", fechaGestion, "obs test", "Dir test",
            -33.4569, -70.6483, 12.5f, "GPS_PROVIDER", true, fechaCapturaGps,
            LocalDate.of(2026, 8, 15)
        );

        gestionService.recibirGestion(cmd);

        List<DatosGestion> gestiones = gestionConsultaApi.findUltimasGestiones(p.getId(), 1);
        assertThat(gestiones).hasSize(1);
        DatosGestion g = gestiones.get(0);

        assertThat(g.id()).isEqualTo(cmd.id());
        assertThat(g.origenGestion()).isEqualTo("BUSQUEDA_DIRECTA");
        assertThat(g.asignacionDiariaId()).isNull();
        assertThat(g.personaId()).isEqualTo(p.getId());
        assertThat(g.ejecutivoId()).isEqualTo(ejeId);
        assertThat(g.tipoGestion()).isEqualTo("COMPROMISO_PAGO");
        assertThat(g.fechaGestion()).isEqualTo(fechaGestion);
        assertThat(g.observacion()).isEqualTo("obs test");
        assertThat(g.observacionDireccion()).isEqualTo("Dir test");
        assertThat(g.latitud()).isEqualTo(-33.4569);
        assertThat(g.longitud()).isEqualTo(-70.6483);
        assertThat(g.precisionMetros()).isEqualTo(12.5f);
        assertThat(g.proveedorGps()).isEqualTo("GPS_PROVIDER");
        assertThat(g.ubicacionSimulada()).isTrue();
        assertThat(g.fechaCapturaGps()).isEqualTo(fechaCapturaGps);
        assertThat(g.fechaCompromiso()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(g.fechaCreacionServidor()).isNotNull();
    }
}
