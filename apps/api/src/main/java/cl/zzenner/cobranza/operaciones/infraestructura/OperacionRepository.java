package cl.zzenner.cobranza.operaciones.infraestructura;

import cl.zzenner.cobranza.operaciones.dominio.Operacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperacionRepository extends JpaRepository<Operacion, UUID> {

    List<Operacion> findByPersonaId(UUID personaId);

    Optional<Operacion> findByNumeroOperacion(String numeroOperacion);

    Optional<Operacion> findByIdentificadorExterno(String identificadorExterno);
}
