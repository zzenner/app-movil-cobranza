package cl.zzenner.cobranza.importacion.infraestructura;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ErrorImportacionRepository extends JpaRepository<ErrorImportacion, UUID> {

    Page<ErrorImportacion> findByImportacionIdOrderByNumeroFilaAscIdAsc(UUID importacionId, Pageable pageable);

    long countByImportacionId(UUID importacionId);

    void deleteByImportacionId(UUID importacionId);
}
