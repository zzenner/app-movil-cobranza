package cl.zzenner.cobranza.personas.infraestructura;

import cl.zzenner.cobranza.personas.dominio.CarteraPersona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarteraPersonaRepository extends JpaRepository<CarteraPersona, UUID> {

    Optional<CarteraPersona> findByCarteraIdAndPersonaIdAndActivaTrue(UUID carteraId, UUID personaId);

    boolean existsByCarteraIdAndPersonaIdAndActivaTrue(UUID carteraId, UUID personaId);

    List<CarteraPersona> findByPersonaIdAndActivaTrue(UUID personaId);

    List<CarteraPersona> findByCarteraIdAndActivaTrue(UUID carteraId);
}
