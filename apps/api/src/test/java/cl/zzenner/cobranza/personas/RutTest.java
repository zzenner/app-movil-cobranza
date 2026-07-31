package cl.zzenner.cobranza.personas;

import cl.zzenner.cobranza.personas.dominio.Rut;
import cl.zzenner.cobranza.personas.dominio.RutInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RutTest {

    @Test
    void rut_valido_se_crea_correctamente() {
        Rut rut = Rut.of("12345678", "5");
        assertThat(rut.getNumero()).isEqualTo("12345678");
        assertThat(rut.getDv()).isEqualTo("5");
        assertThat(rut.formato()).isEqualTo("12345678-5");
    }

    @Test
    void rut_con_dv_K_mayuscula_se_acepta() {
        Rut rut = Rut.of("6", "K");
        assertThat(rut.getDv()).isEqualTo("K");
    }

    @Test
    void rut_con_dv_k_minuscula_se_normaliza_a_mayuscula() {
        Rut rut = Rut.of("6", "k");
        assertThat(rut.getDv()).isEqualTo("K");
    }

    @Test
    void rut_con_espacios_se_limpia() {
        Rut rut = Rut.of(" 12345678 ", " 5 ");
        assertThat(rut.getNumero()).isEqualTo("12345678");
        assertThat(rut.getDv()).isEqualTo("5");
    }

    @Test
    void rut_con_dv_incorrecto_lanza_excepcion() {
        assertThatThrownBy(() -> Rut.of("12345678", "1"))
                .isInstanceOf(RutInvalidoException.class)
                .hasMessageContaining("12345678");
    }

    @Test
    void rut_con_numero_nulo_lanza_excepcion() {
        assertThatThrownBy(() -> Rut.of(null, "5"))
                .isInstanceOf(RutInvalidoException.class);
    }

    @Test
    void rut_con_numero_vacio_lanza_excepcion() {
        assertThatThrownBy(() -> Rut.of("", "5"))
                .isInstanceOf(RutInvalidoException.class);
    }

    @Test
    void rut_con_dv_nulo_lanza_excepcion() {
        assertThatThrownBy(() -> Rut.of("12345678", null))
                .isInstanceOf(RutInvalidoException.class);
    }

    @Test
    void rut_con_letras_en_numero_lanza_excepcion() {
        assertThatThrownBy(() -> Rut.of("1234567A", "5"))
                .isInstanceOf(RutInvalidoException.class)
                .hasMessageContaining("solo puede contener dígitos");
    }

    @Test
    void rut_con_mas_de_8_digitos_lanza_excepcion() {
        assertThatThrownBy(() -> Rut.of("123456789", "5"))
                .isInstanceOf(RutInvalidoException.class)
                .hasMessageContaining("8 dígitos");
    }

    @Test
    void rut_con_dv_invalido_lanza_excepcion() {
        assertThatThrownBy(() -> Rut.of("12345678", "X"))
                .isInstanceOf(RutInvalidoException.class)
                .hasMessageContaining("Dígito verificador inválido");
    }

    @Test
    void igualdad_por_valor() {
        Rut a = Rut.of("12345678", "5");
        Rut b = Rut.of("12345678", "5");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void diferentes_ruts_no_son_iguales() {
        Rut a = Rut.of("12345678", "5");
        Rut b = Rut.of("11111111", "1");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rut_minimo_valido_un_digito() {
        String numero = "1";
        String dv = calcularDv(numero);
        Rut rut = Rut.of(numero, dv);
        assertThat(rut).isNotNull();
    }

    private String calcularDv(String numero) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(numero.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return "0";
        if (resto == 10) return "K";
        return String.valueOf(resto);
    }
}
