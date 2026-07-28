package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.dominio.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

    List<Permiso> findAllByActivoTrue();
}
