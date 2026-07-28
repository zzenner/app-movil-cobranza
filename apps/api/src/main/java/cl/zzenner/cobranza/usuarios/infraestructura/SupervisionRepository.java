package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.dominio.SupervisionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupervisionRepository extends JpaRepository<SupervisionUsuario, UUID> {

    boolean existsByEjecutivoIdAndActivoTrue(UUID ejecutivoId);

    Optional<SupervisionUsuario> findByEjecutivoIdAndActivoTrue(UUID ejecutivoId);

    List<SupervisionUsuario> findAllBySupervisorIdAndActivoTrue(UUID supervisorId);
}
