package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.aplicacion.CodificadorContrasena;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BcryptCodificadorContrasena implements CodificadorContrasena {

    private final PasswordEncoder encoder;

    BcryptCodificadorContrasena(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String codificar(String contrasenaCruda) {
        return encoder.encode(contrasenaCruda);
    }

    @Override
    public boolean verificar(String contrasenaCruda, String hash) {
        return encoder.matches(contrasenaCruda, hash);
    }
}
