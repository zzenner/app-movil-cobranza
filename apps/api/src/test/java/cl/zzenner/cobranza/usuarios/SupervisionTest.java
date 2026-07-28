package cl.zzenner.cobranza.usuarios;

import cl.zzenner.cobranza.usuarios.dominio.RelacionSupervisionInvalidaException;
import cl.zzenner.cobranza.usuarios.dominio.SupervisionUsuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SupervisionTest {

    private static final UUID SUPERVISOR = UUID.randomUUID();
    private static final UUID EJECUTIVO  = UUID.randomUUID();

    @Test
    void crear_supervision_valida() {
        SupervisionUsuario s = new SupervisionUsuario(SUPERVISOR, EJECUTIVO, LocalDate.now());
        assertThat(s.isActivo()).isTrue();
        assertThat(s.getFechaTermino()).isNull();
    }

    @Test
    void supervisor_y_ejecutivo_no_pueden_ser_el_mismo() {
        assertThatThrownBy(() -> new SupervisionUsuario(SUPERVISOR, SUPERVISOR, LocalDate.now()))
                .isInstanceOf(RelacionSupervisionInvalidaException.class);
    }

    @Test
    void finalizar_supervision_establece_fecha_y_desactiva() {
        SupervisionUsuario s = new SupervisionUsuario(SUPERVISOR, EJECUTIVO, LocalDate.now());
        LocalDate hoy = LocalDate.now();
        s.finalizar(hoy);
        assertThat(s.isActivo()).isFalse();
        assertThat(s.getFechaTermino()).isEqualTo(hoy);
    }

    @Test
    void fecha_termino_no_puede_ser_anterior_a_inicio() {
        LocalDate inicio = LocalDate.of(2026, 7, 15);
        SupervisionUsuario s = new SupervisionUsuario(SUPERVISOR, EJECUTIVO, inicio);
        assertThatThrownBy(() -> s.finalizar(LocalDate.of(2026, 7, 1)))
                .isInstanceOf(RelacionSupervisionInvalidaException.class);
    }

    @Test
    void finalizar_supervision_ya_finalizada_lanza_excepcion() {
        SupervisionUsuario s = new SupervisionUsuario(SUPERVISOR, EJECUTIVO, LocalDate.now());
        s.finalizar(LocalDate.now());
        assertThatThrownBy(() -> s.finalizar(LocalDate.now()))
                .isInstanceOf(RelacionSupervisionInvalidaException.class);
    }
}
