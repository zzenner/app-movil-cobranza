package cl.zzenner.cobranza.dispositivos.infraestructura;

import cl.zzenner.cobranza.dispositivos.dominio.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {

    boolean existsByIdentificadorInstalacion(String identificadorInstalacion);

    Optional<Dispositivo> findByIdentificadorInstalacion(String identificadorInstalacion);

    List<Dispositivo> findAllByUsuarioIdAndActivoTrue(UUID usuarioId);
}
