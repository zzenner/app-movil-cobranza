package cl.zzenner.cobranza;

import cl.zzenner.cobranza.usuarios.api.UsuarioSeedApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Crea idempotentemente un usuario local de desarrollo.
 * Solo se activa en el perfil "docker" y cuando dev.seed.enabled=true.
 * No se ejecuta en tests ni en producción.
 */
@Component
@Profile("docker")
@ConditionalOnProperty(name = "dev.seed.enabled", havingValue = "true")
class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final UsuarioSeedApi usuarioSeedApi;

    @Value("${dev.admin.username:}") private String username;
    @Value("${dev.admin.password:}") private String password;
    @Value("${dev.admin.email:}")    private String email;
    @Value("${dev.admin.rol:TECNOLOGIA}") private String rol;

    DevSeedRunner(UsuarioSeedApi usuarioSeedApi) {
        this.usuarioSeedApi = usuarioSeedApi;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            log.warn("DevSeedRunner: DEV_ADMIN_USERNAME o DEV_ADMIN_PASSWORD no configurados — seed omitido.");
            return;
        }

        boolean creado = usuarioSeedApi.crearSiNoExiste(username, password,
                email.isBlank() ? null : email, rol);
        if (creado) {
            log.info("DevSeedRunner: usuario '{}' creado con rol '{}'.", username, rol);
        } else {
            log.info("DevSeedRunner: usuario '{}' ya existe — seed omitido.", username);
        }
    }
}
