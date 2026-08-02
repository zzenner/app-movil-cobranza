package cl.zzenner.cobranza.personas.infraestructura;

import cl.zzenner.cobranza.personas.dominio.Aval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AvalRepository extends JpaRepository<Aval, UUID> {

    List<Aval> findByPersonaId(UUID personaId);

    List<Aval> findByPersonaIdIn(Collection<UUID> personaIds);
}
