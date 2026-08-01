package cl.zzenner.cobranza;

import cl.zzenner.cobranza.autenticacion.AutenticacionTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Import(AutenticacionTestConfig.class)
@TestPropertySource(properties = "security.bcrypt.strength=4")
class InfraestructuraTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void esquemaCobranzaExiste() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'cobranza'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void esquemaAuditoriaExiste() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'auditoria'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void postgisDisponible() {
        String version = jdbc.queryForObject("SELECT PostGIS_Version()", String.class);
        assertThat(version).isNotNull().isNotBlank();
    }

    @Test
    void flyway_crea_tablas_en_esquema_cobranza() {
        // V002: 7; V004: +2 sesiones/tokens; V006: +4 carteras/personas/avales/direcciones; V007: +2 operaciones/cuotas; V008: +1 carteras_personas → 16
        Integer tablas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'cobranza'",
                Integer.class);
        assertThat(tablas).isEqualTo(16);
    }

    @Test
    void flywayHistorialExiste() {
        // Flyway crea su tabla de historial en el esquema public por defecto
        Integer historial = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables"
                        + " WHERE table_name = 'flyway_schema_history'",
                Integer.class);
        assertThat(historial).isEqualTo(1);
    }
}
