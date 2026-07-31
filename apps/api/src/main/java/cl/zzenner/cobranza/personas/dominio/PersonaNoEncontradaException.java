package cl.zzenner.cobranza.personas.dominio;

import java.util.UUID;

public class PersonaNoEncontradaException extends RuntimeException {

    public PersonaNoEncontradaException(UUID id) {
        super("Persona no encontrada: " + id);
    }

    public PersonaNoEncontradaException(String rutNumero, String rutDv) {
        super("Persona no encontrada con RUT: " + rutNumero + "-" + rutDv);
    }
}
