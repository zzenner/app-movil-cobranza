package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.dominio.RolPermiso;
import cl.zzenner.cobranza.usuarios.dominio.RolPermisoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, RolPermisoId> {

    List<RolPermiso> findAllByIdRolId(UUID rolId);

    List<RolPermiso> findAllByIdRolIdIn(Collection<UUID> rolIds);
}
