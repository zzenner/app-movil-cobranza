package cl.zzenner.cobranza.operaciones.infraestructura;

import cl.zzenner.cobranza.operaciones.dominio.Cuota;
import cl.zzenner.cobranza.operaciones.dominio.EstadoCuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuotaRepository extends JpaRepository<Cuota, UUID> {

    List<Cuota> findByOperacionIdOrderByNumeroCuota(UUID operacionId);

    List<Cuota> findByOperacionIdInAndEstadoIn(Collection<UUID> operacionIds, Collection<EstadoCuota> estados);

    Optional<Cuota> findByOperacionIdAndNumeroCuota(UUID operacionId, int numeroCuota);

    void deleteByOperacionId(UUID operacionId);
}
