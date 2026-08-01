package cl.zzenner.cobranza.asignaciones;

import cl.zzenner.cobranza.asignaciones.dominio.AsignacionMensual;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsignacionMensualDominioTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIN    = LocalDate.of(2026, 8, 31);

    private AsignacionMensual nuevaAsignacion() {
        return new AsignacionMensual(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                INICIO, FIN, null);
    }

    @Test
    void nueva_asignacion_mensual_esta_activa() {
        AsignacionMensual am = nuevaAsignacion();
        assertThat(am.isActiva()).isTrue();
        assertThat(am.getId()).isNotNull();
        assertThat(am.getFechaInicio()).isEqualTo(INICIO);
        assertThat(am.getFechaFin()).isEqualTo(FIN);
    }

    @Test
    void cerrar_asignacion_mensual_la_desactiva() {
        AsignacionMensual am = nuevaAsignacion();
        am.cerrar();
        assertThat(am.isActiva()).isFalse();
    }

    @Test
    void cerrar_asignacion_ya_cerrada_lanza_excepcion() {
        AsignacionMensual am = nuevaAsignacion();
        am.cerrar();
        assertThatThrownBy(am::cerrar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fecha_fin_anterior_a_inicio_lanza_excepcion() {
        assertThatThrownBy(() ->
                new AsignacionMensual(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        FIN, INICIO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dos_asignaciones_tienen_uuid_distintos() {
        AsignacionMensual a1 = nuevaAsignacion();
        AsignacionMensual a2 = nuevaAsignacion();
        assertThat(a1.getId()).isNotEqualTo(a2.getId());
    }
}
