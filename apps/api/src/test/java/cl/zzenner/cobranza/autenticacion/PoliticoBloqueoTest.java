package cl.zzenner.cobranza.autenticacion;

import cl.zzenner.cobranza.usuarios.dominio.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticoBloqueoTest {

    @Test
    void usuario_sin_bloqueo_temporal_esta_habilitado() {
        Usuario u = new Usuario("ana", "Ana", "Garcia", null, null, "hash");
        assertThat(u.estaHabilitadoEn(Instant.now())).isTrue();
    }

    @Test
    void usuario_con_bloqueo_temporal_vigente_no_esta_habilitado() {
        Usuario u = new Usuario("pedro", "Pedro", "Soto", null, null, "hash");
        u.setBloqueadoHasta(Instant.now().plusSeconds(1800));
        assertThat(u.estaHabilitadoEn(Instant.now())).isFalse();
    }

    @Test
    void usuario_con_bloqueo_temporal_expirado_esta_habilitado() {
        Usuario u = new Usuario("lucia", "Lucía", "Ramos", null, null, "hash");
        u.setBloqueadoHasta(Instant.now().minusSeconds(1));
        assertThat(u.estaHabilitadoEn(Instant.now())).isTrue();
    }

    @Test
    void registrar_acceso_exitoso_limpia_bloqueo_temporal() {
        Usuario u = new Usuario("mario", "Mario", "Vega", null, null, "hash");
        u.setBloqueadoHasta(Instant.now().plusSeconds(1800));
        u.registrarAccesoExitoso();
        assertThat(u.getBloqueadoHasta()).isNull();
        assertThat(u.getIntentosFallidos()).isZero();
    }

    @Test
    void bloqueo_administrativo_prevalece_sobre_bloqueo_temporal_expirado() {
        Usuario u = new Usuario("carlos", "Carlos", "Mora", null, null, "hash");
        u.bloquear();
        u.setBloqueadoHasta(Instant.now().minusSeconds(1));
        assertThat(u.estaHabilitadoEn(Instant.now())).isFalse();
    }

    @Test
    void usuario_inactivo_no_esta_habilitado_aunque_no_bloqueado() {
        Usuario u = new Usuario("jose", "José", "Reyes", null, null, "hash");
        u.setActivo(false);
        assertThat(u.estaHabilitadoEn(Instant.now())).isFalse();
    }
}
