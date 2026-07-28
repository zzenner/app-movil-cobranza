package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.aplicacion.CodificadorContrasena;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Prueba directa del adaptador BCrypt con costo 4 para velocidad.
class CodificadorContrasenaTest {

    private final CodificadorContrasena codificador = new BcryptCodificadorContrasena(4);

    @Test
    void codificar_produce_hash_no_vacio() {
        String hash = codificador.codificar("ContraseñaSegura1!");
        assertThat(hash).isNotBlank().startsWith("$2a$");
    }

    @Test
    void verificar_contrasena_correcta_retorna_true() {
        String hash = codificador.codificar("ContraseñaSegura1!");
        assertThat(codificador.verificar("ContraseñaSegura1!", hash)).isTrue();
    }

    @Test
    void verificar_contrasena_incorrecta_retorna_false() {
        String hash = codificador.codificar("ContraseñaSegura1!");
        assertThat(codificador.verificar("OtraContrasena", hash)).isFalse();
    }

    @Test
    void dos_hashes_del_mismo_valor_son_distintos() {
        String h1 = codificador.codificar("abc123");
        String h2 = codificador.codificar("abc123");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void hash_no_revela_contrasena_original() {
        String hash = codificador.codificar("secreto");
        assertThat(hash).doesNotContain("secreto");
    }
}
