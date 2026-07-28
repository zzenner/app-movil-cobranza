package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.aplicacion.CodificadorContrasena;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BcryptCodificadorContrasena implements CodificadorContrasena {

    private final BCryptPasswordEncoder encoder;

    BcryptCodificadorContrasena(@Value("${security.bcrypt.strength:12}") int strength) {
        this.encoder = new BCryptPasswordEncoder(strength);
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
