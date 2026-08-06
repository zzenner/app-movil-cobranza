package cl.zzenner.cobranza.usuarios;

import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioAdminService;
import cl.zzenner.cobranza.usuarios.dominio.EstadoUsuario;
import cl.zzenner.cobranza.usuarios.dominio.Usuario;
import cl.zzenner.cobranza.usuarios.infraestructura.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EstadoUsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-08-05T12:00:00Z");
    private static final Clock RELOJ_FIJO = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final UsuarioAdminService service = new UsuarioAdminService(
            mock(UsuarioRepository.class),
            mock(UsuarioRolRepository.class),
            mock(RolRepository.class),
            mock(RolPermisoRepository.class),
            mock(PermisoRepository.class),
            mock(SupervisionRepository.class),
            RELOJ_FIJO);

    @Test
    void usuario_inactivo_devuelve_INACTIVO() {
        Usuario u = usuarioBase();
        setActivo(u, false);
        assertThat(service.calcularEstado(u, AHORA)).isEqualTo(EstadoUsuario.INACTIVO);
    }

    @Test
    void usuario_bloqueado_activo_devuelve_BLOQUEADO() {
        Usuario u = usuarioBase();
        setBloqueado(u, true);
        assertThat(service.calcularEstado(u, AHORA)).isEqualTo(EstadoUsuario.BLOQUEADO);
    }

    @Test
    void usuario_inactivo_y_bloqueado_devuelve_INACTIVO_por_precedencia() {
        Usuario u = usuarioBase();
        setActivo(u, false);
        setBloqueado(u, true);
        assertThat(service.calcularEstado(u, AHORA)).isEqualTo(EstadoUsuario.INACTIVO);
    }

    @Test
    void usuario_con_bloqueado_hasta_en_el_futuro_devuelve_BLOQUEADO_TEMPORAL() {
        Usuario u = usuarioBase();
        u.setBloqueadoHasta(AHORA.plusSeconds(3600));
        assertThat(service.calcularEstado(u, AHORA)).isEqualTo(EstadoUsuario.BLOQUEADO_TEMPORAL);
    }

    @Test
    void usuario_con_bloqueado_hasta_en_el_pasado_devuelve_ACTIVO() {
        Usuario u = usuarioBase();
        u.setBloqueadoHasta(AHORA.minusSeconds(1));
        assertThat(service.calcularEstado(u, AHORA)).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void usuario_sin_bloqueado_hasta_devuelve_ACTIVO() {
        Usuario u = usuarioBase();
        assertThat(service.calcularEstado(u, AHORA)).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void usuario_bloqueado_y_con_bloqueado_hasta_devuelve_BLOQUEADO_por_precedencia() {
        Usuario u = usuarioBase();
        setBloqueado(u, true);
        u.setBloqueadoHasta(AHORA.plusSeconds(3600));
        assertThat(service.calcularEstado(u, AHORA)).isEqualTo(EstadoUsuario.BLOQUEADO);
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────────

    private Usuario usuarioBase() {
        return new Usuario("u.test", "Test", "User", null, null, "hash");
    }

    private void setActivo(Usuario u, boolean valor) {
        u.setActivo(valor);
    }

    private void setBloqueado(Usuario u, boolean valor) {
        if (valor) u.bloquear(); else u.desbloquear();
    }
}
