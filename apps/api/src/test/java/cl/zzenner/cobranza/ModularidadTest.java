package cl.zzenner.cobranza;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularidadTest {

    ApplicationModules modulos = ApplicationModules.of(CobranzaApplication.class);

    @Test
    void verificaEstructuraModular() {
        modulos.verify();
    }
}
