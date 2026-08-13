package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import cl.zzenner.cobranza.importacion.dominio.FilaCsv;
import cl.zzenner.cobranza.importacion.dominio.NivelError;
import cl.zzenner.cobranza.importacion.infraestructura.ErrorImportacionRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class ImportacionPersistenciaService {

    private static final Logger log = LoggerFactory.getLogger(ImportacionPersistenciaService.class);

    private final EntityManager em;
    private final ErrorImportacionRepository errorRepository;

    public ImportacionPersistenciaService(EntityManager em,
                                           ErrorImportacionRepository errorRepository) {
        this.em = em;
        this.errorRepository = errorRepository;
    }

    public void guardarErroresBatch(List<ErrorImportacion> errores) {
        int batch = 0;
        for (ErrorImportacion e : errores) {
            em.persist(e);
            batch++;
            if (batch % 100 == 0) {
                em.flush();
                em.clear();
            }
        }
        em.flush();
        em.clear();
    }

    // Contrato v2: carteraId y periodo provienen del CSV (no como parámetros)
    public ResultadoProcesamiento procesarFilas(List<FilaCsv> filas, String sistemaOrigen,
                                                UUID importacionId) {
        int personasCreadas = 0, personasActualizadas = 0;
        int operacionesCreadas = 0, operacionesActualizadas = 0;
        int cuotasCreadas = 0, cuotasActualizadas = 0;
        int filasRechazadas = 0, filasAdvertencia = 0;

        // Cargar catálogos de resolución
        Map<String, UUID> carteraIds = resolverCarteras();
        Set<String> codigosEjecutivo = new HashSet<>();
        for (FilaCsv f : filas) codigosEjecutivo.add(f.codigoEjecutivo());
        Map<String, UUID> ejecutivoIds = resolverEjecutivos(codigosEjecutivo);
        Map<UUID, UUID> supervisorPorEjecutivo = resolverSupervisores(ejecutivoIds.values());

        // Agrupar por RUT para procesamiento por lotes
        Map<String, List<FilaCsv>> filasPorRut = new LinkedHashMap<>();
        for (FilaCsv f : filas) {
            String key = f.rutNumero() + "-" + f.rutDv();
            filasPorRut.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }

        Instant ahora = Instant.now();

        for (Map.Entry<String, List<FilaCsv>> entry : filasPorRut.entrySet()) {
            FilaCsv primeraFila = entry.getValue().get(0);
            String rutNumero = primeraFila.rutNumero();
            String rutDv = primeraFila.rutDv();

            UUID personaId = buscarPersonaPorRut(rutNumero, rutDv);
            if (personaId == null) {
                personaId = UUID.randomUUID();
                insertPersona(personaId, rutNumero, rutDv, primeraFila.nombrePersona(),
                        sistemaOrigen, ahora);
                personasCreadas++;
            } else {
                updatePersona(personaId, primeraFila.nombrePersona(), sistemaOrigen, ahora);
                personasActualizadas++;
            }

            upsertDirecciones(personaId, primeraFila, sistemaOrigen, ahora);

            // Resolver cartera desde CSV
            UUID carteraId = carteraIds.get(primeraFila.codigoCartera());
            if (carteraId == null) {
                log.warn("[IMPORTACION] CODIGO_CARTERA '{}' no encontrado en catálogo para RUT {}",
                        primeraFila.codigoCartera(), rutNumero);
            } else {
                upsertCarteraPersona(carteraId, personaId, primeraFila.marcaJudicial(), ahora.toEpochMilli());

                String codigoEjec = primeraFila.codigoEjecutivo();
                UUID ejecutivoId = ejecutivoIds.get(codigoEjec);
                if (ejecutivoId != null) {
                    UUID supervisorId = supervisorPorEjecutivo.get(ejecutivoId);
                    upsertAsignacionMensual(carteraId, ejecutivoId, supervisorId,
                            primeraFila.periodo(), personaId, ahora);
                }
            }

            Set<String> operacionesVistas = new HashSet<>();
            for (FilaCsv fila : entry.getValue()) {
                String opNumero = fila.operacionNumero();
                boolean opNueva = !operacionesVistas.contains(opNumero);
                operacionesVistas.add(opNumero);

                UUID operacionId = buscarOperacionPorNumero(fila.operacionNumero(), sistemaOrigen);
                if (operacionId == null) {
                    operacionId = UUID.randomUUID();
                    insertOperacion(operacionId, personaId, fila, sistemaOrigen, ahora);
                    if (opNueva) operacionesCreadas++;
                } else {
                    updateOperacion(operacionId, fila, ahora);
                    if (opNueva) operacionesActualizadas++;
                }

                boolean cuotaNueva = upsertCuota(operacionId, fila, ahora);
                if (cuotaNueva) cuotasCreadas++;
                else cuotasActualizadas++;
            }

            em.flush();
            em.clear();
        }

        return new ResultadoProcesamiento(
                filas.size(), filasRechazadas, filasAdvertencia,
                personasCreadas, personasActualizadas,
                operacionesCreadas, operacionesActualizadas,
                cuotasCreadas, cuotasActualizadas);
    }

    private Map<String, UUID> resolverCarteras() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT codigo_origen, id FROM cobranza.carteras " +
                "WHERE activa = TRUE AND codigo_origen IS NOT NULL")
                .getResultList();
        Map<String, UUID> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (UUID) row[1]);
        }
        return result;
    }

    private Map<String, UUID> resolverEjecutivos(Set<String> codigos) {
        if (codigos.isEmpty()) return new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT codigo_ejecutivo_origen, id FROM cobranza.usuarios " +
                "WHERE codigo_ejecutivo_origen IN (:codigos) AND activo = TRUE")
                .setParameter("codigos", codigos)
                .getResultList();
        Map<String, UUID> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (UUID) row[1]);
        }
        return result;
    }

    private Map<UUID, UUID> resolverSupervisores(Collection<UUID> ejecutivoIds) {
        if (ejecutivoIds.isEmpty()) return new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ejecutivo_id, supervisor_id FROM cobranza.supervision_usuarios " +
                "WHERE ejecutivo_id IN (:ids) AND activo = TRUE")
                .setParameter("ids", ejecutivoIds)
                .getResultList();
        Map<UUID, UUID> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], (UUID) row[1]);
        }
        return result;
    }

    private UUID buscarPersonaPorRut(String rutNumero, String rutDv) {
        @SuppressWarnings("unchecked")
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.personas WHERE rut_numero = :rn AND rut_dv = :rd")
                .setParameter("rn", rutNumero)
                .setParameter("rd", rutDv)
                .getResultList();
        return rows.isEmpty() ? null : (UUID) rows.get(0);
    }

    private void insertPersona(UUID id, String rutNumero, String rutDv, String nombre,
                                String sistemaOrigen, Instant ahora) {
        em.createNativeQuery("""
            INSERT INTO cobranza.personas
                (id, rut_numero, rut_dv, nombre, sistema_origen,
                 fecha_actualizacion_origen, fecha_importacion,
                 fecha_creacion, fecha_actualizacion, version)
            VALUES (:id, :rn, :rd, :nombre, :sistemaOrigen,
                    :ahora, :ahora, :ahora, :ahora, 0)
            """)
                .setParameter("id", id)
                .setParameter("rn", rutNumero)
                .setParameter("rd", rutDv)
                .setParameter("nombre", nombre)
                .setParameter("sistemaOrigen", sistemaOrigen)
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private void updatePersona(UUID id, String nombre, String sistemaOrigen, Instant ahora) {
        em.createNativeQuery("""
            UPDATE cobranza.personas
            SET nombre = :nombre,
                fecha_actualizacion_origen = :ahora,
                fecha_importacion = :ahora,
                fecha_actualizacion = :ahora,
                version = version + 1
            WHERE id = :id
            """)
                .setParameter("id", id)
                .setParameter("nombre", nombre)
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private void upsertDirecciones(UUID personaId, FilaCsv fila, String sistemaOrigen, Instant ahora) {
        if (fila.dirParticular() != null && !fila.dirParticular().isBlank()) {
            upsertDireccionPorTipo(personaId, "DOMICILIO", fila.dirParticular(), true, sistemaOrigen, ahora);
        }
        if (fila.dirComercial() != null && !fila.dirComercial().isBlank()) {
            upsertDireccionPorTipo(personaId, "COMERCIAL", fila.dirComercial(), false, sistemaOrigen, ahora);
        }
    }

    private void upsertDireccionPorTipo(UUID personaId, String tipo, String texto,
                                         boolean esPrincipal, String sistemaOrigen, Instant ahora) {
        @SuppressWarnings("unchecked")
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.direcciones " +
                "WHERE persona_id = :pid AND tipo = :tipo AND vigente = TRUE")
                .setParameter("pid", personaId)
                .setParameter("tipo", tipo)
                .getResultList();

        if (rows.isEmpty()) {
            if (esPrincipal) {
                em.createNativeQuery("""
                    UPDATE cobranza.direcciones
                    SET es_principal = FALSE, vigente = FALSE
                    WHERE persona_id = :pid AND es_principal = TRUE AND vigente = TRUE
                    """)
                        .setParameter("pid", personaId)
                        .executeUpdate();
            }
            UUID dirId = UUID.randomUUID();
            em.createNativeQuery("""
                INSERT INTO cobranza.direcciones
                    (id, persona_id, tipo, texto, es_principal, vigente,
                     sistema_origen, fecha_actualizacion_origen, fecha_creacion)
                VALUES (:id, :pid, :tipo, :texto, :principal, TRUE,
                        :so, :ahora, :ahora)
                """)
                    .setParameter("id", dirId)
                    .setParameter("pid", personaId)
                    .setParameter("tipo", tipo)
                    .setParameter("texto", texto)
                    .setParameter("principal", esPrincipal)
                    .setParameter("so", sistemaOrigen)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        } else {
            UUID dirId = (UUID) rows.get(0);
            em.createNativeQuery("""
                UPDATE cobranza.direcciones
                SET texto = :texto, es_principal = :principal, vigente = TRUE,
                    fecha_actualizacion_origen = :ahora
                WHERE id = :id
                """)
                    .setParameter("id", dirId)
                    .setParameter("texto", texto)
                    .setParameter("principal", esPrincipal)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        }
    }

    private void upsertCarteraPersona(UUID carteraId, UUID personaId,
                                       String marcaJudicial, long timestamp) {
        Instant ahora = Instant.ofEpochMilli(timestamp);

        // Desactivar vínculos activos con otras carteras (una persona → una cartera activa)
        em.createNativeQuery("""
            UPDATE cobranza.carteras_personas
            SET activa = FALSE, fecha_fin = CURRENT_DATE,
                fecha_actualizacion = :ahora, version = version + 1
            WHERE persona_id = :pid AND activa = TRUE AND cartera_id <> :cid
            """)
                .setParameter("pid", personaId)
                .setParameter("cid", carteraId)
                .setParameter("ahora", ahora)
                .executeUpdate();

        // Buscar vínculo existente para esta cartera (activo o no)
        @SuppressWarnings("unchecked")
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.carteras_personas " +
                "WHERE cartera_id = :cid AND persona_id = :pid")
                .setParameter("cid", carteraId)
                .setParameter("pid", personaId)
                .getResultList();

        if (rows.isEmpty()) {
            em.createNativeQuery("""
                INSERT INTO cobranza.carteras_personas
                    (id, cartera_id, persona_id, activa, fecha_inicio, marca_judicial,
                     fecha_creacion, fecha_actualizacion, version)
                VALUES (:id, :cid, :pid, TRUE, CURRENT_DATE, :mj,
                        :ahora, :ahora, 0)
                """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("cid", carteraId)
                    .setParameter("pid", personaId)
                    .setParameter("mj", marcaJudicial)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        } else {
            UUID cpId = (UUID) rows.get(0);
            em.createNativeQuery("""
                UPDATE cobranza.carteras_personas
                SET activa = TRUE, fecha_fin = NULL, marca_judicial = :mj,
                    fecha_actualizacion = :ahora, version = version + 1
                WHERE id = :id
                """)
                    .setParameter("id", cpId)
                    .setParameter("mj", marcaJudicial)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        }
    }

    private void upsertAsignacionMensual(UUID carteraId, UUID ejecutivoId, UUID supervisorId,
                                          String periodo, UUID personaId, Instant ahora) {
        if (supervisorId == null) return;

        int year = Integer.parseInt(periodo.substring(0, 4));
        int month = Integer.parseInt(periodo.substring(5, 7));
        LocalDate fechaInicio = LocalDate.of(year, month, 1);
        LocalDate fechaFin = fechaInicio.withDayOfMonth(fechaInicio.lengthOfMonth());

        @SuppressWarnings("unchecked")
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.asignaciones_mensuales " +
                "WHERE cartera_id = :cid AND ejecutivo_id = :eid AND activa = TRUE")
                .setParameter("cid", carteraId)
                .setParameter("eid", ejecutivoId)
                .getResultList();

        UUID asignacionId;
        if (rows.isEmpty()) {
            asignacionId = UUID.randomUUID();
            em.createNativeQuery("""
                INSERT INTO cobranza.asignaciones_mensuales
                    (id, cartera_id, ejecutivo_id, supervisor_id, fecha_inicio, fecha_fin,
                     activa, fecha_creacion, fecha_actualizacion, version)
                VALUES (:id, :cid, :eid, :sid, :fi, :ff, TRUE, :ahora, :ahora, 0)
                """)
                    .setParameter("id", asignacionId)
                    .setParameter("cid", carteraId)
                    .setParameter("eid", ejecutivoId)
                    .setParameter("sid", supervisorId)
                    .setParameter("fi", fechaInicio)
                    .setParameter("ff", fechaFin)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        } else {
            asignacionId = (UUID) rows.get(0);
            em.createNativeQuery(
                    "UPDATE cobranza.asignaciones_mensuales " +
                    "SET supervisor_id = :sid, fecha_fin = :ff, " +
                    "fecha_actualizacion = :ahora, version = version + 1 " +
                    "WHERE id = :id")
                    .setParameter("id", asignacionId)
                    .setParameter("sid", supervisorId)
                    .setParameter("ff", fechaFin)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        }

        em.createNativeQuery("""
            INSERT INTO cobranza.asignaciones_mensuales_personas
                (id, asignacion_mensual_id, persona_id, cartera_id, activa, fecha_inicio,
                 fecha_creacion, fecha_actualizacion, version)
            SELECT :id, :amid, :pid, :cid, TRUE, :fi, :ahora, :ahora, 0
            WHERE NOT EXISTS (
                SELECT 1 FROM cobranza.asignaciones_mensuales_personas
                WHERE asignacion_mensual_id = :amid AND persona_id = :pid AND activa = TRUE
            )
            """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("amid", asignacionId)
                .setParameter("pid", personaId)
                .setParameter("cid", carteraId)
                .setParameter("fi", fechaInicio)
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private UUID buscarOperacionPorNumero(String numeroOperacion, String sistemaOrigen) {
        @SuppressWarnings("unchecked")
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.operaciones " +
                "WHERE sistema_origen = :so AND numero_operacion = :num")
                .setParameter("so", sistemaOrigen)
                .setParameter("num", numeroOperacion)
                .getResultList();
        return rows.isEmpty() ? null : (UUID) rows.get(0);
    }

    private void insertOperacion(UUID id, UUID personaId, FilaCsv fila,
                                  String sistemaOrigen, Instant ahora) {
        em.createNativeQuery("""
            INSERT INTO cobranza.operaciones
                (id, persona_id, numero_operacion, sistema_origen,
                 tipo_operacion, estado, capital, interes_penal, gastos_cobranza, total_vigente,
                 fecha_vencimiento, fecha_actualizacion_origen, fecha_importacion,
                 fecha_creacion, fecha_actualizacion, version)
            VALUES (:id, :pid, :num, :so, :tipo, :estado,
                    :capital, :ip, :gastos, :total, :fvto, :ahora, :ahora, :ahora, :ahora, 0)
            """)
                .setParameter("id", id)
                .setParameter("pid", personaId)
                .setParameter("num", fila.operacionNumero())
                .setParameter("so", sistemaOrigen)
                .setParameter("tipo", fila.operacionTipo())
                .setParameter("estado", fila.operacionEstado())
                .setParameter("capital", fila.operacionCapital())
                .setParameter("ip", nvl(fila.operacionInteresPenal(), BigDecimal.ZERO))
                .setParameter("gastos", nvl(fila.operacionGastos(), BigDecimal.ZERO))
                .setParameter("total", fila.operacionTotalVigente())
                .setParameter("fvto", fila.operacionFechaVto())
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private void updateOperacion(UUID id, FilaCsv fila, Instant ahora) {
        em.createNativeQuery("""
            UPDATE cobranza.operaciones
            SET estado = :estado, capital = :capital, interes_penal = :ip,
                gastos_cobranza = :gastos, total_vigente = :total,
                tipo_operacion = :tipo,
                fecha_vencimiento = :fvto, fecha_actualizacion_origen = :ahora,
                fecha_importacion = :ahora, fecha_actualizacion = :ahora, version = version + 1
            WHERE id = :id
            """)
                .setParameter("id", id)
                .setParameter("estado", fila.operacionEstado())
                .setParameter("tipo", fila.operacionTipo())
                .setParameter("capital", fila.operacionCapital())
                .setParameter("ip", nvl(fila.operacionInteresPenal(), BigDecimal.ZERO))
                .setParameter("gastos", nvl(fila.operacionGastos(), BigDecimal.ZERO))
                .setParameter("total", fila.operacionTotalVigente())
                .setParameter("fvto", fila.operacionFechaVto())
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private boolean upsertCuota(UUID operacionId, FilaCsv fila, Instant ahora) {
        @SuppressWarnings("unchecked")
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.cuotas WHERE operacion_id = :oid AND numero_cuota = :num")
                .setParameter("oid", operacionId)
                .setParameter("num", fila.cuotaNumero())
                .getResultList();

        if (rows.isEmpty()) {
            UUID cuotaId = UUID.randomUUID();
            em.createNativeQuery("""
                INSERT INTO cobranza.cuotas
                    (id, operacion_id, numero_cuota, estado,
                     capital, interes, interes_penal, gastos_cobranza, monto_total, saldo,
                     fecha_vencimiento, fecha_actualizacion_origen, fecha_importacion,
                     fecha_creacion, fecha_actualizacion)
                VALUES (:id, :oid, :num, :estado,
                        :capital, :interes, :ip, :gastos, :monto, :saldo,
                        :fvto, :ahora, :ahora, :ahora, :ahora)
                """)
                    .setParameter("id", cuotaId)
                    .setParameter("oid", operacionId)
                    .setParameter("num", fila.cuotaNumero())
                    .setParameter("estado", fila.cuotaEstado())
                    .setParameter("capital", fila.cuotaCapital())
                    .setParameter("interes", fila.cuotaInteres())
                    .setParameter("ip", nvl(fila.cuotaInteresPenal(), BigDecimal.ZERO))
                    .setParameter("gastos", nvl(fila.cuotaGastos(), BigDecimal.ZERO))
                    .setParameter("monto", fila.cuotaMontoTotal())
                    .setParameter("saldo", fila.cuotaSaldo())
                    .setParameter("fvto", fila.cuotaFechaVto())
                    .setParameter("ahora", ahora)
                    .executeUpdate();
            return true;
        } else {
            UUID cuotaId = (UUID) rows.get(0);
            em.createNativeQuery("""
                UPDATE cobranza.cuotas
                SET estado = :estado, capital = :capital, interes = :interes,
                    interes_penal = :ip, gastos_cobranza = :gastos, monto_total = :monto,
                    saldo = :saldo, fecha_vencimiento = :fvto,
                    fecha_actualizacion_origen = :ahora, fecha_importacion = :ahora,
                    fecha_actualizacion = :ahora
                WHERE id = :id
                """)
                    .setParameter("id", cuotaId)
                    .setParameter("estado", fila.cuotaEstado())
                    .setParameter("capital", fila.cuotaCapital())
                    .setParameter("interes", fila.cuotaInteres())
                    .setParameter("ip", nvl(fila.cuotaInteresPenal(), BigDecimal.ZERO))
                    .setParameter("gastos", nvl(fila.cuotaGastos(), BigDecimal.ZERO))
                    .setParameter("monto", fila.cuotaMontoTotal())
                    .setParameter("saldo", fila.cuotaSaldo())
                    .setParameter("fvto", fila.cuotaFechaVto())
                    .setParameter("ahora", ahora)
                    .executeUpdate();
            return false;
        }
    }

    private <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
