package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.api.DemoAsignacionSeedApi;
import cl.zzenner.cobranza.usuarios.api.SupervisionSeedApi;
import cl.zzenner.cobranza.usuarios.api.UsuarioSeedApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Crea idempotentemente datos de desarrollo: usuario admin, supervisores demo y ejecutivos demo.
 * Solo se activa en el perfil "docker" y cuando dev.seed.enabled=true.
 * No se ejecuta en tests ni en producción.
 */
@Component
@Profile("docker")
@ConditionalOnProperty(name = "dev.seed.enabled", havingValue = "true")
class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    // Supervisores ficticios de desarrollo
    private static final List<String[]> SUPERVISORES_DEMO = List.of(
            new String[]{"sup_demo_norte",  "Supervisor Demo", "Norte"},
            new String[]{"sup_demo_centro", "Supervisor Demo", "Centro"},
            new String[]{"sup_demo_sur",    "Supervisor Demo", "Sur"}
    );

    // Ejecutivos ficticios de desarrollo — código de origen exacto del sistema legado
    // 18 códigos extraídos del archivo de importación real; sin datos de clientes.
    // Distribución determinista por supervisor (6 por supervisor, índices 0-5 / 6-11 / 12-17).
    private static final List<String[]> EJECUTIVOS_DEMO = List.of(
            new String[]{"ej_demo_133",  "Ejecutivo Demo", "01", "133"},
            new String[]{"ej_demo_1190", "Ejecutivo Demo", "02", "1190"},
            new String[]{"ej_demo_1205", "Ejecutivo Demo", "03", "1205"},
            new String[]{"ej_demo_1424", "Ejecutivo Demo", "04", "1424"},
            new String[]{"ej_demo_1889", "Ejecutivo Demo", "05", "1889"},
            new String[]{"ej_demo_1967", "Ejecutivo Demo", "06", "1967"},
            new String[]{"ej_demo_2009", "Ejecutivo Demo", "07", "2009"},
            new String[]{"ej_demo_2025", "Ejecutivo Demo", "08", "2025"},
            new String[]{"ej_demo_2127", "Ejecutivo Demo", "09", "2127"},
            new String[]{"ej_demo_2152", "Ejecutivo Demo", "10", "2152"},
            new String[]{"ej_demo_2230", "Ejecutivo Demo", "11", "2230"},
            new String[]{"ej_demo_2238", "Ejecutivo Demo", "12", "2238"},
            new String[]{"ej_demo_2356", "Ejecutivo Demo", "13", "2356"},
            new String[]{"ej_demo_2366", "Ejecutivo Demo", "14", "2366"},
            new String[]{"ej_demo_2447", "Ejecutivo Demo", "15", "2447"},
            new String[]{"ej_demo_2463", "Ejecutivo Demo", "16", "2463"},
            new String[]{"ej_demo_2477", "Ejecutivo Demo", "17", "2477"},
            new String[]{"ej_demo_2524", "Ejecutivo Demo", "18", "2524"}
    );

    // UUID fijo de la cartera "Temprana" (creada por Flyway, inmutable en DEV)
    private static final UUID CARTERA_TEMPRANA_ID =
            UUID.fromString("00000000-0000-0000-0001-000000000001");

    private final UsuarioSeedApi usuarioSeedApi;
    private final SupervisionSeedApi supervisionSeedApi;
    private final DemoAsignacionSeedApi demoAsignacionSeedApi;

    @Value("${dev.admin.username:}") private String username;
    @Value("${dev.admin.password:}") private String password;
    @Value("${dev.admin.email:}")    private String email;
    @Value("${dev.admin.rol:TECNOLOGIA}") private String rol;

    DevSeedRunner(UsuarioSeedApi usuarioSeedApi, SupervisionSeedApi supervisionSeedApi,
                  DemoAsignacionSeedApi demoAsignacionSeedApi) {
        this.usuarioSeedApi = usuarioSeedApi;
        this.supervisionSeedApi = supervisionSeedApi;
        this.demoAsignacionSeedApi = demoAsignacionSeedApi;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            log.warn("DevSeedRunner: DEV_ADMIN_USERNAME o DEV_ADMIN_PASSWORD no configurados — seed omitido.");
            return;
        }

        // Usuario administrador
        boolean creado = usuarioSeedApi.crearSiNoExiste(username, password,
                email.isBlank() ? null : email, rol);
        if (creado) {
            log.info("DevSeedRunner: usuario '{}' creado con rol '{}'.", username, rol);
        } else {
            log.info("DevSeedRunner: usuario '{}' ya existe — seed omitido.", username);
        }

        // Supervisores demo
        UUID[] supervisorIds = new UUID[SUPERVISORES_DEMO.size()];
        for (int i = 0; i < SUPERVISORES_DEMO.size(); i++) {
            String[] s = SUPERVISORES_DEMO.get(i);
            try {
                supervisorIds[i] = usuarioSeedApi.crearSupervisorDemoSiNoExiste(s[0], s[1], s[2], password);
                log.info("DevSeedRunner: supervisor demo '{}' listo.", s[0]);
            } catch (Exception ex) {
                log.error("DevSeedRunner: error creando supervisor '{}': {}", s[0], ex.getMessage());
            }
        }

        // Ejecutivos demo + asignación a supervisor (6 por supervisor)
        int ejPorSupervisor = (int) Math.ceil((double) EJECUTIVOS_DEMO.size() / SUPERVISORES_DEMO.size());
        int creados = 0;
        UUID ejDemo133Id = null;
        for (int i = 0; i < EJECUTIVOS_DEMO.size(); i++) {
            String[] e = EJECUTIVOS_DEMO.get(i);
            try {
                UUID ejId = usuarioSeedApi.crearEjecutivoDemoSiNoExiste(e[0], e[1], e[2], password, e[3]);
                log.info("DevSeedRunner: ejecutivo demo '{}' (codigo={}) listo.", e[0], e[3]);

                if ("ej_demo_133".equals(e[0])) ejDemo133Id = ejId;

                int supIdx = Math.min(i / ejPorSupervisor, supervisorIds.length - 1);
                if (supervisorIds[supIdx] != null) {
                    supervisionSeedApi.asignarEjecutivoSiNoAsignado(supervisorIds[supIdx], ejId);
                    log.info("DevSeedRunner: '{}' asignado a '{}'.", e[0], SUPERVISORES_DEMO.get(supIdx)[0]);
                }
                creados++;
            } catch (Exception ex) {
                log.error("DevSeedRunner: error procesando ejecutivo '{}' (codigo={}): {}",
                        e[0], e[3], ex.getMessage());
            }
        }

        log.info("DevSeedRunner: seed completado — {} supervisores, {}/{} ejecutivos.",
                SUPERVISORES_DEMO.size(), creados, EJECUTIVOS_DEMO.size());

        // Escenario de asignación demo: una diaria publicada para el ejecutivo ej_demo_133
        UUID publicadorId = usuarioSeedApi.findIdByNombreUsuario(username);
        if (ejDemo133Id != null && supervisorIds[0] != null && publicadorId != null) {
            try {
                demoAsignacionSeedApi.prepararEscenarioDemo(
                        CARTERA_TEMPRANA_ID, ejDemo133Id, supervisorIds[0], publicadorId, 5);
                log.info("DevSeedRunner: escenario asignación demo listo para ej_demo_133.");
            } catch (Exception ex) {
                log.error("DevSeedRunner: error preparando escenario asignación demo: {}", ex.getMessage());
            }
        } else {
            log.warn("DevSeedRunner: escenario asignación demo omitido — faltan IDs requeridos.");
        }
    }
}
