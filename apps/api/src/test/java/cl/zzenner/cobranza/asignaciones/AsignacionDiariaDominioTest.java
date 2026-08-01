package cl.zzenner.cobranza.asignaciones;

import cl.zzenner.cobranza.asignaciones.dominio.AsignacionDiaria;
import cl.zzenner.cobranza.asignaciones.dominio.EstadoAsignacionDiaria;
import cl.zzenner.cobranza.asignaciones.dominio.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsignacionDiariaDominioTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 8, 5);

    private AsignacionDiaria nuevaDiaria() {
        return new AsignacionDiaria(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), FECHA);
    }

    @Test
    void nueva_diaria_tiene_estado_borrador() {
        AsignacionDiaria ad = nuevaDiaria();
        assertThat(ad.getEstado()).isEqualTo(EstadoAsignacionDiaria.BORRADOR);
        assertThat(ad.getId()).isNotNull();
        assertThat(ad.getFechaPublicacion()).isNull();
    }

    @Test
    void publicar_diaria_cambia_estado_a_publicada() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        assertThat(ad.getEstado()).isEqualTo(EstadoAsignacionDiaria.PUBLICADA);
    }

    @Test
    void publicar_diaria_registra_fecha_publicacion() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        assertThat(ad.getFechaPublicacion()).isNotNull();
    }

    @Test
    void publicar_diaria_ya_publicada_lanza_excepcion() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        assertThatThrownBy(ad::publicar)
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void publicar_diaria_finalizada_lanza_excepcion() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        ad.finalizar();
        assertThatThrownBy(ad::publicar)
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void finalizar_diaria_publicada_la_finaliza() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        ad.finalizar();
        assertThat(ad.getEstado()).isEqualTo(EstadoAsignacionDiaria.FINALIZADA);
    }

    @Test
    void finalizar_diaria_en_borrador_lanza_excepcion() {
        AsignacionDiaria ad = nuevaDiaria();
        assertThatThrownBy(ad::finalizar)
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void finalizar_diaria_ya_finalizada_lanza_excepcion() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        ad.finalizar();
        assertThatThrownBy(ad::finalizar)
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cancelar_borrador_con_motivo_valido_la_cancela() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.cancelar("Motivo de prueba");
        assertThat(ad.getEstado()).isEqualTo(EstadoAsignacionDiaria.CANCELADA);
        assertThat(ad.getMotivoCancelacion()).isEqualTo("Motivo de prueba");
    }

    @Test
    void cancelar_publicada_con_motivo_valido_la_cancela() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        ad.cancelar("Cancelada por supervisor");
        assertThat(ad.getEstado()).isEqualTo(EstadoAsignacionDiaria.CANCELADA);
    }

    @Test
    void cancelar_sin_motivo_lanza_excepcion() {
        AsignacionDiaria ad = nuevaDiaria();
        assertThatThrownBy(() -> ad.cancelar(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ad.cancelar(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ad.cancelar("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelar_diaria_finalizada_lanza_excepcion() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.publicar();
        ad.finalizar();
        assertThatThrownBy(() -> ad.cancelar("Motivo"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cancelar_diaria_cancelada_lanza_excepcion() {
        AsignacionDiaria ad = nuevaDiaria();
        ad.cancelar("Primera cancelación");
        assertThatThrownBy(() -> ad.cancelar("Segunda cancelación"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }
}
