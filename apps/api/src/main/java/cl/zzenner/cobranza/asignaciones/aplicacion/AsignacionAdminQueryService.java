package cl.zzenner.cobranza.asignaciones.aplicacion;

import cl.zzenner.cobranza.asignaciones.dominio.EstadoAsignacionDiaria;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AsignacionAdminQueryService {

    private final EntityManager em;

    public AsignacionAdminQueryService(EntityManager em) {
        this.em = em;
    }

    // ── Records de respuesta ───────────────────────────────────────────────────

    public record ItemPeriodo(String periodo) {}

    public record ItemAsignacionMensualAdmin(
            UUID id, String periodo, UUID carteraId, String nombreCartera,
            UUID ejecutivoId, String nombreEjecutivo, String codigoEjecutivo,
            UUID supervisorId, String nombreSupervisor, int cantidadPersonas) {}

    public record ItemPersonaDisponible(
            UUID personaId, String rutNumero, String rutDv, String nombre,
            UUID carteraId, String nombreCartera, int cantidadOperaciones,
            boolean tieneAsignacionDiaria) {}

    public record ItemAsignacionDiariaAdmin(
            UUID id, LocalDate fecha, String periodo, UUID carteraId, String nombreCartera,
            UUID ejecutivoId, String nombreEjecutivo, UUID supervisorId, String nombreSupervisor,
            EstadoAsignacionDiaria estado, Instant fechaPublicacion, int cantidadPersonas) {}

    public record DetalleAsignacionDiariaAdmin(
            UUID id, LocalDate fecha, String periodo, UUID carteraId, String nombreCartera,
            UUID ejecutivoId, String nombreEjecutivo, UUID supervisorId, String nombreSupervisor,
            EstadoAsignacionDiaria estado, Instant fechaPublicacion,
            UUID publicadoPorId, String nombrePublicador,
            String motivoCancelacion, Instant fechaCreacion, long version,
            int cantidadPersonas, List<ItemPersonaEnDiaria> personas) {}

    public record ItemPersonaEnDiaria(
            UUID personaId, String rutNumero, String rutDv, String nombre) {}

    // ── Períodos disponibles ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<ItemPeriodo> listarPeriodos(UUID carteraId, UUID supervisorId, UUID ejecutivoId) {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT TO_CHAR(am.fecha_inicio, 'YYYY-MM') AS periodo " +
                "FROM cobranza.asignaciones_mensuales am " +
                "WHERE am.activa = TRUE");
        if (carteraId != null) sql.append(" AND am.cartera_id = :carteraId");
        if (supervisorId != null) sql.append(" AND am.supervisor_id = :supervisorId");
        if (ejecutivoId != null) sql.append(" AND am.ejecutivo_id = :ejecutivoId");
        sql.append(" ORDER BY periodo DESC");

        var q = em.createNativeQuery(sql.toString());
        if (carteraId != null) q.setParameter("carteraId", carteraId);
        if (supervisorId != null) q.setParameter("supervisorId", supervisorId);
        if (ejecutivoId != null) q.setParameter("ejecutivoId", ejecutivoId);

        return ((List<String>) q.getResultList()).stream()
                .map(ItemPeriodo::new)
                .toList();
    }

    // ── Asignaciones mensuales (posiciones del período) ────────────────────────

    @SuppressWarnings("unchecked")
    public List<ItemAsignacionMensualAdmin> listarMensuales(
            String periodo, UUID carteraId, UUID supervisorId, UUID ejecutivoId) {

        StringBuilder sql = new StringBuilder(
                "SELECT am.id, TO_CHAR(am.fecha_inicio, 'YYYY-MM') AS periodo, " +
                "  am.cartera_id, c.nombre AS nombre_cartera, " +
                "  am.ejecutivo_id, " +
                "  COALESCE(ue.nombres || ' ' || ue.apellido_paterno, ue.nombre_usuario) AS nombre_ejecutivo, " +
                "  ue.codigo_ejecutivo_origen AS codigo_ejecutivo, " +
                "  am.supervisor_id, " +
                "  COALESCE(us.nombres || ' ' || us.apellido_paterno, us.nombre_usuario) AS nombre_supervisor, " +
                "  (SELECT COUNT(*) FROM cobranza.asignaciones_mensuales_personas amp " +
                "   WHERE amp.asignacion_mensual_id = am.id AND amp.activa = TRUE) AS cantidad_personas " +
                "FROM cobranza.asignaciones_mensuales am " +
                "JOIN cobranza.carteras c ON c.id = am.cartera_id " +
                "JOIN cobranza.usuarios ue ON ue.id = am.ejecutivo_id " +
                "JOIN cobranza.usuarios us ON us.id = am.supervisor_id " +
                "WHERE am.activa = TRUE");

        if (periodo != null && !periodo.isBlank())
            sql.append(" AND TO_CHAR(am.fecha_inicio, 'YYYY-MM') = :periodo");
        if (carteraId != null)
            sql.append(" AND am.cartera_id = :carteraId");
        if (supervisorId != null)
            sql.append(" AND am.supervisor_id = :supervisorId");
        if (ejecutivoId != null)
            sql.append(" AND am.ejecutivo_id = :ejecutivoId");

        sql.append(" ORDER BY periodo DESC, nombre_cartera, nombre_ejecutivo");

        var q = em.createNativeQuery(sql.toString());
        if (periodo != null && !periodo.isBlank()) q.setParameter("periodo", periodo);
        if (carteraId != null) q.setParameter("carteraId", carteraId);
        if (supervisorId != null) q.setParameter("supervisorId", supervisorId);
        if (ejecutivoId != null) q.setParameter("ejecutivoId", ejecutivoId);

        return ((List<Object[]>) q.getResultList()).stream().map(row -> new ItemAsignacionMensualAdmin(
                (UUID) row[0], (String) row[1], (UUID) row[2], (String) row[3],
                (UUID) row[4], (String) row[5], (String) row[6],
                (UUID) row[7], (String) row[8],
                ((Number) row[9]).intValue()
        )).toList();
    }

    // ── Personas disponibles en una mensual ────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<ItemPersonaDisponible> listarPersonasDisponibles(UUID asignacionMensualId) {
        String sql =
                "SELECT p.id AS persona_id, p.rut_numero, p.rut_dv, p.nombre, " +
                "  amp.cartera_id, c.nombre AS nombre_cartera, " +
                "  (SELECT COUNT(*) FROM cobranza.operaciones op " +
                "   WHERE op.persona_id = p.id AND op.estado = 'ACTIVA') AS cantidad_operaciones, " +
                "  EXISTS(" +
                "    SELECT 1 FROM cobranza.asignaciones_diarias_personas adp " +
                "    JOIN cobranza.asignaciones_diarias ad ON ad.id = adp.asignacion_diaria_id " +
                "    WHERE adp.persona_id = p.id " +
                "    AND ad.estado IN ('BORRADOR','PUBLICADA')" +
                "  ) AS tiene_asignacion_diaria " +
                "FROM cobranza.asignaciones_mensuales_personas amp " +
                "JOIN cobranza.personas p ON p.id = amp.persona_id " +
                "JOIN cobranza.carteras c ON c.id = amp.cartera_id " +
                "WHERE amp.asignacion_mensual_id = :mensualId AND amp.activa = TRUE " +
                "ORDER BY p.nombre";

        return ((List<Object[]>) em.createNativeQuery(sql)
                .setParameter("mensualId", asignacionMensualId)
                .getResultList()).stream()
                .map(row -> new ItemPersonaDisponible(
                        (UUID) row[0], (String) row[1], (String) row[2], (String) row[3],
                        (UUID) row[4], (String) row[5],
                        ((Number) row[6]).intValue(),
                        (Boolean) row[7]
                )).toList();
    }

    // ── Listado de asignaciones diarias ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<ItemAsignacionDiariaAdmin> listarDiarias(
            LocalDate fecha, String estado, UUID carteraId, UUID supervisorId, UUID ejecutivoId) {

        StringBuilder sql = new StringBuilder(
                "SELECT ad.id, ad.fecha, " +
                "  TO_CHAR(am.fecha_inicio, 'YYYY-MM') AS periodo, " +
                "  am.cartera_id, c.nombre AS nombre_cartera, " +
                "  ad.ejecutivo_id, " +
                "  COALESCE(ue.nombres || ' ' || ue.apellido_paterno, ue.nombre_usuario) AS nombre_ejecutivo, " +
                "  ad.supervisor_id, " +
                "  COALESCE(us.nombres || ' ' || us.apellido_paterno, us.nombre_usuario) AS nombre_supervisor, " +
                "  ad.estado, ad.fecha_publicacion, " +
                "  (SELECT COUNT(*) FROM cobranza.asignaciones_diarias_personas adp " +
                "   WHERE adp.asignacion_diaria_id = ad.id) AS cantidad_personas " +
                "FROM cobranza.asignaciones_diarias ad " +
                "JOIN cobranza.asignaciones_mensuales am ON am.id = ad.asignacion_mensual_id " +
                "JOIN cobranza.carteras c ON c.id = am.cartera_id " +
                "JOIN cobranza.usuarios ue ON ue.id = ad.ejecutivo_id " +
                "JOIN cobranza.usuarios us ON us.id = ad.supervisor_id " +
                "WHERE 1=1");

        if (fecha != null) sql.append(" AND ad.fecha = :fecha");
        if (estado != null && !estado.isBlank()) sql.append(" AND ad.estado = :estado");
        if (carteraId != null) sql.append(" AND am.cartera_id = :carteraId");
        if (supervisorId != null) sql.append(" AND ad.supervisor_id = :supervisorId");
        if (ejecutivoId != null) sql.append(" AND ad.ejecutivo_id = :ejecutivoId");
        sql.append(" ORDER BY ad.fecha DESC, nombre_ejecutivo LIMIT 200");

        var q = em.createNativeQuery(sql.toString());
        if (fecha != null) q.setParameter("fecha", fecha);
        if (estado != null && !estado.isBlank()) q.setParameter("estado", estado);
        if (carteraId != null) q.setParameter("carteraId", carteraId);
        if (supervisorId != null) q.setParameter("supervisorId", supervisorId);
        if (ejecutivoId != null) q.setParameter("ejecutivoId", ejecutivoId);

        return ((List<Object[]>) q.getResultList()).stream().map(row -> new ItemAsignacionDiariaAdmin(
                (UUID) row[0],
                row[1] instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) row[1],
                (String) row[2], (UUID) row[3], (String) row[4],
                (UUID) row[5], (String) row[6], (UUID) row[7], (String) row[8],
                EstadoAsignacionDiaria.valueOf((String) row[9]),
                row[10] != null ? ((java.sql.Timestamp) row[10]).toInstant() : null,
                ((Number) row[11]).intValue()
        )).toList();
    }

    // ── Detalle de asignación diaria ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public DetalleAsignacionDiariaAdmin obtenerDetalleDiaria(UUID asignacionDiariaId) {
        String sqlDiaria =
                "SELECT ad.id, ad.fecha, " +
                "  TO_CHAR(am.fecha_inicio, 'YYYY-MM') AS periodo, " +
                "  am.cartera_id, c.nombre AS nombre_cartera, " +
                "  ad.ejecutivo_id, " +
                "  COALESCE(ue.nombres || ' ' || ue.apellido_paterno, ue.nombre_usuario) AS nombre_ejecutivo, " +
                "  ad.supervisor_id, " +
                "  COALESCE(us.nombres || ' ' || us.apellido_paterno, us.nombre_usuario) AS nombre_supervisor, " +
                "  ad.estado, ad.fecha_publicacion, " +
                "  ad.publicado_por_id, " +
                "  COALESCE(up.nombres || ' ' || up.apellido_paterno, up.nombre_usuario) AS nombre_publicador, " +
                "  ad.motivo_cancelacion, ad.fecha_creacion, ad.version " +
                "FROM cobranza.asignaciones_diarias ad " +
                "JOIN cobranza.asignaciones_mensuales am ON am.id = ad.asignacion_mensual_id " +
                "JOIN cobranza.carteras c ON c.id = am.cartera_id " +
                "JOIN cobranza.usuarios ue ON ue.id = ad.ejecutivo_id " +
                "JOIN cobranza.usuarios us ON us.id = ad.supervisor_id " +
                "LEFT JOIN cobranza.usuarios up ON up.id = ad.publicado_por_id " +
                "WHERE ad.id = :id";

        List<Object[]> rows = (List<Object[]>) em.createNativeQuery(sqlDiaria)
                .setParameter("id", asignacionDiariaId)
                .getResultList();

        if (rows.isEmpty()) return null;
        Object[] row = rows.get(0);

        String sqlPersonas =
                "SELECT p.id, p.rut_numero, p.rut_dv, p.nombre " +
                "FROM cobranza.asignaciones_diarias_personas adp " +
                "JOIN cobranza.personas p ON p.id = adp.persona_id " +
                "WHERE adp.asignacion_diaria_id = :id ORDER BY p.nombre";

        List<ItemPersonaEnDiaria> personas = ((List<Object[]>) em.createNativeQuery(sqlPersonas)
                .setParameter("id", asignacionDiariaId)
                .getResultList()).stream()
                .map(r -> new ItemPersonaEnDiaria(
                        (UUID) r[0], (String) r[1], (String) r[2], (String) r[3]))
                .toList();

        LocalDate fecha = row[1] instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) row[1];
        Instant fechaPub = row[10] != null ? ((java.sql.Timestamp) row[10]).toInstant() : null;
        Instant fechaCreacion = row[14] instanceof java.sql.Timestamp ts ? ts.toInstant() : (Instant) row[14];

        return new DetalleAsignacionDiariaAdmin(
                (UUID) row[0], fecha, (String) row[2], (UUID) row[3], (String) row[4],
                (UUID) row[5], (String) row[6], (UUID) row[7], (String) row[8],
                EstadoAsignacionDiaria.valueOf((String) row[9]),
                fechaPub, (UUID) row[11], (String) row[12],
                (String) row[13], fechaCreacion, ((Number) row[15]).longValue(),
                personas.size(), personas
        );
    }

    // ── Personas actualmente en borrador (para mostrar selección pre-poblada) ─

    @SuppressWarnings("unchecked")
    public List<UUID> listarPersonasEnDiaria(UUID asignacionDiariaId) {
        return (List<UUID>) em.createNativeQuery(
                "SELECT persona_id FROM cobranza.asignaciones_diarias_personas " +
                "WHERE asignacion_diaria_id = :id")
                .setParameter("id", asignacionDiariaId)
                .getResultList();
    }

    // ── Validación de scope supervisor ────────────────────────────────────────

    public boolean supervisorTieneAccesoADiaria(UUID supervisorId, UUID asignacionDiariaId) {
        List<?> rows = em.createNativeQuery(
                "SELECT 1 FROM cobranza.asignaciones_diarias ad " +
                "WHERE ad.id = :id AND ad.supervisor_id = :supervisorId")
                .setParameter("id", asignacionDiariaId)
                .setParameter("supervisorId", supervisorId)
                .getResultList();
        return !rows.isEmpty();
    }

    public boolean supervisorTieneAccesoAMensual(UUID supervisorId, UUID asignacionMensualId) {
        List<?> rows = em.createNativeQuery(
                "SELECT 1 FROM cobranza.asignaciones_mensuales am " +
                "WHERE am.id = :id AND am.supervisor_id = :supervisorId AND am.activa = TRUE")
                .setParameter("id", asignacionMensualId)
                .setParameter("supervisorId", supervisorId)
                .getResultList();
        return !rows.isEmpty();
    }
}
