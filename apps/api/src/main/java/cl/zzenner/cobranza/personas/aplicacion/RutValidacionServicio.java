package cl.zzenner.cobranza.personas.aplicacion;

import cl.zzenner.cobranza.personas.api.RutValidacionApi;
import cl.zzenner.cobranza.personas.dominio.Rut;
import cl.zzenner.cobranza.personas.dominio.RutInvalidoException;
import org.springframework.stereotype.Service;

@Service
class RutValidacionServicio implements RutValidacionApi {

    @Override
    public boolean esValido(String rutNumero, String rutDv) {
        try {
            Rut.of(rutNumero, rutDv);
            return true;
        } catch (RutInvalidoException e) {
            return false;
        }
    }
}
