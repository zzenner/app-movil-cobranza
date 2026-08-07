package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.dominio.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UUID> {

    List<UsuarioRol> findAllByUsuarioIdAndActivoTrue(UUID usuarioId);

    boolean existsByUsuarioIdAndRolIdAndActivoTrue(UUID usuarioId, UUID rolId);

    List<UsuarioRol> findAllByUsuarioIdInAndActivoTrue(Collection<UUID> usuarioIds);

    @Query("SELECT COUNT(DISTINCT ur.usuarioId) FROM UsuarioRol ur " +
           "JOIN Rol r ON r.id = ur.rolId " +
           "JOIN RolPermiso rp ON rp.id.rolId = r.id " +
           "JOIN Permiso p ON p.id = rp.id.permisoId " +
           "JOIN Usuario u ON u.id = ur.usuarioId " +
           "WHERE ur.activo = true AND p.codigo = 'USUARIOS_ADMINISTRAR' " +
           "AND u.activo = true AND u.bloqueado = false AND ur.usuarioId <> :excluidoId")
    long contarAdministradoresActivosExcluyendo(UUID excluidoId);
}
