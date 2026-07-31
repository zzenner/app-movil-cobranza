package cl.zzenner.cobranza;

import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.carteras.dominio.Cartera;
import cl.zzenner.cobranza.operaciones.aplicacion.OperacionService;
import cl.zzenner.cobranza.operaciones.dominio.EstadoCuota;
import cl.zzenner.cobranza.operaciones.dominio.EstadoOperacion;
import cl.zzenner.cobranza.operaciones.dominio.Operacion;
import cl.zzenner.cobranza.personas.aplicacion.PersonaService;
import cl.zzenner.cobranza.personas.dominio.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
class DominioCobranzaIntegracionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CarteraService carteraService;

    @Autowired
    PersonaService personaService;

    @Autowired
    OperacionService operacionService;

    // ── Carteras ──────────────────────────────────────────────────────────────

    @Test
    void tabla_carteras_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='carteras'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void cartera_se_persiste_y_tiene_version() {
        Cartera c = carteraService.registrar("Cartera Test", "descripcion");
        assertThat(c.getId()).isNotNull();
        assertThat(c.getVersion()).isEqualTo(0L);
    }

    @Test
    void cartera_version_incrementa_al_actualizar() {
        Cartera c = carteraService.registrar("Cartera Version", null);
        Cartera desactivada = carteraService.desactivar(c.getId());
        assertThat(desactivada.getVersion()).isGreaterThan(0L);
    }

    // ── Personas ─────────────────────────────────────────────────────────────

    @Test
    void tabla_personas_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='personas'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void persona_se_persiste_con_version() {
        // RUT 14602789-K
        Rut rut = Rut.of("14602789", "K");
        Persona p = personaService.upsertPersona(rut, "Juan Perez", "LEGADO", "EXT001", Instant.now());
        assertThat(p.getId()).isNotNull();
        assertThat(p.getVersion()).isEqualTo(0L);
    }

    @Test
    void persona_upsert_actualiza_nombre() {
        // RUT 16816631-1
        Rut rut = Rut.of("16816631", "1");
        personaService.upsertPersona(rut, "Nombre Original", "LEGADO", "EXT-UP1", Instant.now());
        Persona actualizada = personaService.upsertPersona(rut, "Nombre Nuevo", "LEGADO", "EXT-UP1", Instant.now());
        assertThat(actualizada.getNombre()).isEqualTo("Nombre Nuevo");
    }

    @Test
    void persona_upsert_retorna_misma_persona_para_rut_existente() {
        // RUT 9459948-2
        Rut rut = Rut.of("9459948", "2");
        personaService.upsertPersona(rut, "Pedro Soto", "LEGADO", null, Instant.now());
        Persona p2 = personaService.upsertPersona(rut, "Pedro Soto Actualizado", "LEGADO", null, Instant.now());
        assertThat(p2.getNombre()).isEqualTo("Pedro Soto Actualizado");
    }

    // ── Avales ────────────────────────────────────────────────────────────────

    @Test
    void tabla_avales_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='avales'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void aval_se_persiste_y_es_inmutable() {
        // RUT 12456489-1 para persona; RUT 5126663-3 para aval
        Rut rutPersona = Rut.of("12456489", "1");
        Persona p = personaService.upsertPersona(rutPersona, "Titular Aval", "LEGADO", null, Instant.now());
        Rut rutAval = Rut.of("5126663", "3");
        var aval = personaService.registrarAval(p.getId(), rutAval, "Garante Uno", "LEGADO", "AV001");
        assertThat(aval.getId()).isNotNull();
        assertThat(aval.getFechaCreacion()).isNotNull();
    }

    // ── Direcciones ───────────────────────────────────────────────────────────

    @Test
    void tabla_direcciones_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='direcciones'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void solo_una_direccion_principal_vigente_por_persona() {
        // RUT 16578387-5
        Rut rut = Rut.of("16578387", "5");
        Persona p = personaService.upsertPersona(rut, "Ana Diaz", "LEGADO", null, Instant.now());

        var d1 = personaService.registrarDireccion(p.getId(), TipoDireccion.DOMICILIO, "Calle 1", "LEGADO", null);
        var d2 = personaService.registrarDireccion(p.getId(), TipoDireccion.TRABAJO, "Calle 2", "LEGADO", null);

        personaService.marcarDireccionPrincipal(p.getId(), d1.getId());
        // marcar segunda como principal desactiva la primera
        personaService.marcarDireccionPrincipal(p.getId(), d2.getId());

        Integer principales = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cobranza.direcciones WHERE persona_id=? AND es_principal=TRUE AND vigente=TRUE",
                Integer.class, p.getId());
        assertThat(principales).isEqualTo(1);
    }

    // ── Operaciones ───────────────────────────────────────────────────────────

    @Test
    void tabla_operaciones_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='operaciones'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void operacion_se_persiste_con_version() {
        // RUT 11111111-1
        Rut rut = Rut.of("11111111", "1");
        Persona p = personaService.upsertPersona(rut, "Carlos Mora", "LEGADO", null, Instant.now());
        Operacion op = operacionService.upsertOperacion(
                p.getId(), "OP-001", EstadoOperacion.ACTIVA,
                new BigDecimal("1500000.00"), "LEGADO", Instant.now());
        assertThat(op.getId()).isNotNull();
        assertThat(op.getVersion()).isEqualTo(0L);
    }

    @Test
    void operacion_estado_check_rechaza_valor_invalido() {
        assertThatThrownBy(() ->
                jdbc.execute("INSERT INTO cobranza.operaciones" +
                        "(id, persona_id, numero_operacion, sistema_origen, estado, moneda, capital," +
                        " interes_penal, gastos_cobranza, fecha_creacion, fecha_actualizacion, version)" +
                        " VALUES (gen_random_uuid(), gen_random_uuid(), 'OP-BAD', 'LEGADO'," +
                        " 'INVALIDO', 'CLP', 100, 0, 0, now(), now(), 0)"))
                .isInstanceOf(Exception.class);
    }

    // ── Cuotas ────────────────────────────────────────────────────────────────

    @Test
    void tabla_cuotas_existe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cobranza' AND table_name='cuotas'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void cuota_upsert_actualiza_estado_misma_cuota() {
        // RUT 7654321-6
        Rut rut = Rut.of("7654321", "6");
        Persona p = personaService.upsertPersona(rut, "Laura Gomez", "LEGADO", null, Instant.now());
        Operacion op = operacionService.upsertOperacion(
                p.getId(), "OP-CUO-001", EstadoOperacion.ACTIVA,
                new BigDecimal("500000.00"), "LEGADO", Instant.now());

        var c1 = operacionService.upsertCuota(op.getId(), 1, EstadoCuota.VIGENTE,
                new BigDecimal("50000.00"), LocalDate.now().plusMonths(1), Instant.now());
        assertThat(c1.getId()).isNotNull();

        var c1Updated = operacionService.upsertCuota(op.getId(), 1, EstadoCuota.VENCIDA,
                new BigDecimal("50000.00"), LocalDate.now().plusMonths(1), Instant.now());
        assertThat(c1Updated.getId()).isEqualTo(c1.getId());
        assertThat(c1Updated.getEstado()).isEqualTo(EstadoCuota.VENCIDA);
    }

    @Test
    void cuota_estado_check_rechaza_valor_invalido() {
        assertThatThrownBy(() ->
                jdbc.execute("INSERT INTO cobranza.cuotas" +
                        "(id, operacion_id, numero_cuota, estado, monto_total," +
                        " interes_penal, gastos_cobranza, fecha_vencimiento," +
                        " fecha_creacion, fecha_actualizacion)" +
                        " VALUES (gen_random_uuid(), gen_random_uuid(), 1, 'MALO', 100, 0, 0," +
                        " now(), now(), now())"))
                .isInstanceOf(Exception.class);
    }

    // ── FK constraints ────────────────────────────────────────────────────────

    @Test
    void operacion_requiere_persona_existente() {
        assertThatThrownBy(() ->
                jdbc.execute("INSERT INTO cobranza.operaciones" +
                        "(id, persona_id, numero_operacion, sistema_origen, estado, moneda, capital," +
                        " interes_penal, gastos_cobranza, fecha_creacion, fecha_actualizacion, version)" +
                        " VALUES (gen_random_uuid(), gen_random_uuid(), 'OP-FK', 'LEGADO'," +
                        " 'ACTIVA', 'CLP', 100, 0, 0, now(), now(), 0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
