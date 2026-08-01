package cl.zzenner.cobranza.asignaciones.infraestructura;

import cl.zzenner.cobranza.asignaciones.dominio.AsignacionDiaria;
import cl.zzenner.cobranza.asignaciones.dominio.EstadoAsignacionDiaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AsignacionDiariaRepository extends JpaRepository<AsignacionDiaria, UUID> {

    Optional<AsignacionDiaria> findByEjecutivoIdAndFechaAndEstado(
            UUID ejecutivoId, LocalDate fecha, EstadoAsignacionDiaria estado);

    boolean existsByEjecutivoIdAndFechaAndEstadoIn(
            UUID ejecutivoId, LocalDate fecha, java.util.List<EstadoAsignacionDiaria> estados);
}
