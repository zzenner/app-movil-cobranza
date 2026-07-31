package cl.zzenner.cobranza.carteras;

import cl.zzenner.cobranza.carteras.dominio.Cartera;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarteraDominioTest {

    @Test
    void cartera_nueva_esta_activa_por_defecto() {
        Cartera c = new Cartera("Cartera A", "desc");
        assertThat(c.isActiva()).isTrue();
    }

    @Test
    void desactivar_cartera_activa() {
        Cartera c = new Cartera("Cartera B", null);
        c.desactivar();
        assertThat(c.isActiva()).isFalse();
    }

    @Test
    void activar_cartera_inactiva() {
        Cartera c = new Cartera("Cartera C", null);
        c.desactivar();
        c.activar();
        assertThat(c.isActiva()).isTrue();
    }

    @Test
    void nombre_no_puede_ser_nulo() {
        assertThatThrownBy(() -> new Cartera(null, "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nombre_no_puede_estar_vacio() {
        assertThatThrownBy(() -> new Cartera("  ", "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
