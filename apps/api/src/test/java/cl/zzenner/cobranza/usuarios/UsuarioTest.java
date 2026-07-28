package cl.zzenner.cobranza.usuarios;

import cl.zzenner.cobranza.usuarios.dominio.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private static Usuario nuevoUsuario(String nombreUsuario) {
        return new Usuario(nombreUsuario, "Ana", "García", null, null, "hash_placeholder");
    }

    @Test
    void usuario_activo_no_bloqueado_esta_habilitado() {
        Usuario u = nuevoUsuario("ana.garcia");
        assertThat(u.estaHabilitado()).isTrue();
    }

    @Test
    void usuario_inactivo_no_esta_habilitado() {
        Usuario u = nuevoUsuario("ana.garcia");
        u.setActivo(false);
        assertThat(u.estaHabilitado()).isFalse();
    }

    @Test
    void usuario_bloqueado_no_esta_habilitado() {
        Usuario u = nuevoUsuario("ana.garcia");
        u.bloquear();
        assertThat(u.estaHabilitado()).isFalse();
        assertThat(u.isBloqueado()).isTrue();
    }

    @Test
    void desbloquear_restaura_estado_habilitado() {
        Usuario u = nuevoUsuario("ana.garcia");
        u.bloquear();
        u.registrarIntentoFallido();
        u.desbloquear();
        assertThat(u.estaHabilitado()).isTrue();
        assertThat(u.getIntentosFallidos()).isZero();
    }

    @Test
    void registrar_intentos_fallidos_incrementa_contador() {
        Usuario u = nuevoUsuario("ana.garcia");
        u.registrarIntentoFallido();
        u.registrarIntentoFallido();
        assertThat(u.getIntentosFallidos()).isEqualTo(2);
    }

    @Test
    void nombre_usuario_almacenado_tal_como_se_recibe() {
        // La normalización ocurre en el servicio; la entidad conserva el valor.
        Usuario u = nuevoUsuario("ana.garcia");
        assertThat(u.getNombreUsuario()).isEqualTo("ana.garcia");
    }

    @Test
    void contrasena_hash_no_expuesta_en_toString() {
        Usuario u = nuevoUsuario("ana.garcia");
        assertThat(u.toString()).doesNotContain("hash_placeholder");
    }
}
