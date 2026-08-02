package cl.zzenner.cobranza.gestiones.infraestructura;

import cl.zzenner.cobranza.gestiones.dominio.Gestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GestionRepository extends JpaRepository<Gestion, UUID> {

    List<Gestion> findByPersonaIdOrderByFechaGestionDesc(UUID personaId);

    /**
     * Inserta atómicamente una gestión ignorando conflictos de PK.
     * Retorna 1 si se insertó, 0 si ya existía (ON CONFLICT DO NOTHING).
     * Permite idempotencia segura bajo concurrencia sin riesgo de excepción de PK.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO cobranza.gestiones (
            id, origen_gestion, asignacion_diaria_id, persona_id, ejecutivo_id,
            tipo_gestion, fecha_gestion, observacion, observacion_direccion,
            latitud, longitud, precision_metros, proveedor_gps, ubicacion_simulada,
            fecha_captura_gps, fecha_compromiso, fecha_creacion_servidor
        ) VALUES (
            :id, :origenGestion, :asignacionDiariaId, :personaId, :ejecutivoId,
            :tipoGestion, :fechaGestion, :observacion, :observacionDireccion,
            :latitud, :longitud, :precisionMetros, :proveedorGps, :ubicacionSimulada,
            :fechaCapturaGps, :fechaCompromiso, :fechaCreacionServidor
        ) ON CONFLICT (id) DO NOTHING
        """, nativeQuery = true)
    int insertarSiNoExiste(
        @Param("id")                  UUID id,
        @Param("origenGestion")       String origenGestion,
        @Param("asignacionDiariaId")  UUID asignacionDiariaId,
        @Param("personaId")           UUID personaId,
        @Param("ejecutivoId")         UUID ejecutivoId,
        @Param("tipoGestion")         String tipoGestion,
        @Param("fechaGestion")        Instant fechaGestion,
        @Param("observacion")         String observacion,
        @Param("observacionDireccion") String observacionDireccion,
        @Param("latitud")             double latitud,
        @Param("longitud")            double longitud,
        @Param("precisionMetros")     float precisionMetros,
        @Param("proveedorGps")        String proveedorGps,
        @Param("ubicacionSimulada")   boolean ubicacionSimulada,
        @Param("fechaCapturaGps")     Instant fechaCapturaGps,
        @Param("fechaCompromiso")     LocalDate fechaCompromiso,
        @Param("fechaCreacionServidor") Instant fechaCreacionServidor
    );
}
