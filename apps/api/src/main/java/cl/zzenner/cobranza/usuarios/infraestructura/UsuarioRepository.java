package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    List<Usuario> findAllByIdIn(Collection<UUID> ids);

    boolean existsByNombreUsuarioAndIdNot(String nombreUsuario, UUID id);

    boolean existsByCorreoAndIdNot(String correo, UUID id);

    boolean existsByCodigoEjecutivoOrigenAndIdNot(String codigoEjecutivoOrigen, UUID id);

    Optional<Usuario> findByCodigoEjecutivoOrigen(String codigoEjecutivoOrigen);
}
