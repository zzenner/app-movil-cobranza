package cl.zzenner.cobranza.personas.infraestructura;

import cl.zzenner.cobranza.personas.dominio.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonaRepository extends JpaRepository<Persona, UUID> {

    Optional<Persona> findByRutNumeroAndRutDv(String rutNumero, String rutDv);

    boolean existsByRutNumeroAndRutDv(String rutNumero, String rutDv);
}
