package cl.zzenner.cobranza.personas;

import cl.zzenner.cobranza.personas.dominio.CarteraPersona;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarteraPersonaDominioTest {

    private final UUID carteraId = UUID.randomUUID();
    private final UUID personaId = UUID.randomUUID();
    private final LocalDate inicio = LocalDate.of(2026, 8, 1);

    @Test
    void vinculo_nuevo_esta_activo_por_defecto() {
        CarteraPersona cp = new CarteraPersona(carteraId, personaId, inicio);
        assertThat(cp.isActiva()).isTrue();
        assertThat(cp.getFechaFin()).isNull();
        assertThat(cp.getId()).isNotNull();
    }

    @Test
    void cerrar_con_fecha_valida_desactiva_vinculo() {
        CarteraPersona cp = new CarteraPersona(carteraId, personaId, inicio);
        LocalDate fin = inicio.plusMonths(1);
        cp.cerrar(fin);
        assertThat(cp.isActiva()).isFalse();
        assertThat(cp.getFechaFin()).isEqualTo(fin);
    }

    @Test
    void cerrar_con_misma_fecha_inicio_es_valido() {
        CarteraPersona cp = new CarteraPersona(carteraId, personaId, inicio);
        cp.cerrar(inicio);
        assertThat(cp.isActiva()).isFalse();
    }

    @Test
    void cerrar_con_fecha_anterior_a_inicio_lanza_excepcion() {
        CarteraPersona cp = new CarteraPersona(carteraId, personaId, inicio);
        assertThatThrownBy(() -> cp.cerrar(inicio.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cerrar_vinculo_ya_cerrado_lanza_excepcion() {
        CarteraPersona cp = new CarteraPersona(carteraId, personaId, inicio);
        cp.cerrar(inicio);
        assertThatThrownBy(() -> cp.cerrar(inicio.plusDays(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dos_vinculos_distintos_tienen_uuid_diferentes() {
        CarteraPersona cp1 = new CarteraPersona(carteraId, personaId, inicio);
        CarteraPersona cp2 = new CarteraPersona(carteraId, personaId, inicio);
        assertThat(cp1.getId()).isNotEqualTo(cp2.getId());
    }
}
