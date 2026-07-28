package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.dominio.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UUID> {

    List<UsuarioRol> findAllByUsuarioIdAndActivoTrue(UUID usuarioId);

    boolean existsByUsuarioIdAndRolIdAndActivoTrue(UUID usuarioId, UUID rolId);
}
