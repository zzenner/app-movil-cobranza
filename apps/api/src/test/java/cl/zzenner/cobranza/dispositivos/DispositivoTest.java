package cl.zzenner.cobranza.dispositivos;

import cl.zzenner.cobranza.dispositivos.dominio.Dispositivo;
import cl.zzenner.cobranza.dispositivos.dominio.DispositivoRevocadoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DispositivoTest {

    private static Dispositivo nuevoDispositivo() {
        return new Dispositivo(UUID.randomUUID(), UUID.randomUUID().toString(),
                "Moto G8", "Motorola", "G8 Plus", "11", "1.0.0");
    }

    @Test
    void dispositivo_nuevo_esta_activo_y_no_revocado() {
        Dispositivo d = nuevoDispositivo();
        assertThat(d.isActivo()).isTrue();
        assertThat(d.isRevocado()).isFalse();
        assertThat(d.getFechaRevocacion()).isNull();
    }

    @Test
    void revocar_desactiva_y_registra_fecha() {
        Dispositivo d = nuevoDispositivo();
        d.revocar();
        assertThat(d.isRevocado()).isTrue();
        assertThat(d.isActivo()).isFalse();
        assertThat(d.getFechaRevocacion()).isNotNull();
    }

    @Test
    void revocar_ya_revocado_lanza_excepcion() {
        Dispositivo d = nuevoDispositivo();
        d.revocar();
        assertThatThrownBy(d::revocar).isInstanceOf(DispositivoRevocadoException.class);
    }

    @Test
    void coherencia_revocacion_activo_false_fecha_presente() {
        Dispositivo d = nuevoDispositivo();
        d.revocar();
        // Todos los invariantes del CHECK de la BD deben cumplirse en la entidad.
        assertThat(d.isRevocado()).isTrue();
        assertThat(d.isActivo()).isFalse();
        assertThat(d.getFechaRevocacion()).isNotNull();
    }
}
