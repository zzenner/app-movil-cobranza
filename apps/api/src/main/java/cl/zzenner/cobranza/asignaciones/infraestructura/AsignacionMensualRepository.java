package cl.zzenner.cobranza.asignaciones.infraestructura;

import cl.zzenner.cobranza.asignaciones.dominio.AsignacionMensual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AsignacionMensualRepository extends JpaRepository<AsignacionMensual, UUID> {

    Optional<AsignacionMensual> findByEjecutivoIdAndCarteraIdAndActivaTrue(UUID ejecutivoId, UUID carteraId);

    boolean existsByEjecutivoIdAndCarteraIdAndActivaTrue(UUID ejecutivoId, UUID carteraId);
}
