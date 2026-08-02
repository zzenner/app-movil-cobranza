package cl.zzenner.cobranza.dispositivos.infraestructura;

import cl.zzenner.cobranza.dispositivos.dominio.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {

    boolean existsByIdentificadorInstalacion(String identificadorInstalacion);

    Optional<Dispositivo> findByIdentificadorInstalacion(String identificadorInstalacion);

    List<Dispositivo> findAllByUsuarioIdAndActivoTrue(UUID usuarioId);

    /**
     * Inserta un dispositivo solo si no existe ya un registro con el mismo
     * identificador_instalacion. Ante concurrencia, el segundo hilo recibirá 0
     * en lugar de una excepción de clave duplicada (ADR-0031).
     *
     * @return 1 si se insertó, 0 si ya existía.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO cobranza.dispositivos (usuario_id, identificador_instalacion)
            VALUES (:usuarioId, :identificadorInstalacion)
            ON CONFLICT (identificador_instalacion) DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(@Param("usuarioId") UUID usuarioId,
                           @Param("identificadorInstalacion") String identificadorInstalacion);
}
