package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import cl.zzenner.cobranza.importacion.dominio.FilaCsv;
import cl.zzenner.cobranza.importacion.dominio.NivelError;
import cl.zzenner.cobranza.importacion.infraestructura.ErrorImportacionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class ImportacionPersistenciaService {

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

    public ResultadoProcesamiento procesarFilas(List<FilaCsv> filas, UUID carteraId,
                                                  String periodo, String sistemaOrigen,
                                                  UUID importacionId) {
        int personasCreadas = 0, personasActualizadas = 0;
        int operacionesCreadas = 0, operacionesActualizadas = 0;
        int cuotasCreadas = 0, cuotasActualizadas = 0;
        int filasRechazadas = 0, filasAdvertencia = 0;

        // Agrupar filas únicas por RUT para batch
        Map<String, List<FilaCsv>> filasPorRut = new LinkedHashMap<>();
        for (FilaCsv f : filas) {
            String key = f.rutNumero() + "-" + f.rutDv();
            filasPorRut.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }

        // Obtener ejecutivos únicos
        Set<String> usernames = new HashSet<>();
        for (FilaCsv f : filas) usernames.add(f.ejecutivoUsername());

        Map<String, UUID> ejecutivoIds = resolverEjecutivos(usernames);
        Map<UUID, UUID> supervisorPorEjecutivo = resolverSupervisores(ejecutivoIds.values());

        Instant ahora = Instant.now();

        for (Map.Entry<String, List<FilaCsv>> entry : filasPorRut.entrySet()) {
            FilaCsv primeraFila = entry.getValue().get(0);
            String rutNumero = primeraFila.rutNumero();
            String rutDv = primeraFila.rutDv();

            // Upsert persona
            UUID personaId = upsertPersona(rutNumero, rutDv, primeraFila.nombrePersona(),
                    primeraFila.codigoExtPersona(), sistemaOrigen, ahora);
            boolean esPersonaNueva = personaId == null;
            if (esPersonaNueva) {
                personaId = UUID.randomUUID();
                insertPersona(personaId, rutNumero, rutDv, primeraFila.nombrePersona(),
                        primeraFila.codigoExtPersona(), sistemaOrigen, ahora);
                personasCreadas++;
            } else {
                updatePersona(personaId, primeraFila.nombrePersona(), sistemaOrigen, ahora);
                personasActualizadas++;
            }

            // Upsert dirección principal
            upsertDireccion(personaId, primeraFila, sistemaOrigen, ahora);

            // Upsert cartera_persona
            String ejecutivoUsername = primeraFila.ejecutivoUsername();
            UUID ejecutivoId = ejecutivoIds.get(ejecutivoUsername);
            if (ejecutivoId != null) {
                UUID supervisorId = supervisorPorEjecutivo.get(ejecutivoId);
                upsertCarteraPersona(carteraId, personaId, ahora.toEpochMilli());
                upsertAsignacionMensual(carteraId, ejecutivoId, supervisorId, periodo, personaId, ahora);
            }

            // Upsert operaciones y cuotas
            Set<String> operacionesVistas = new HashSet<>();
            for (FilaCsv fila : entry.getValue()) {
                String opIdExt = fila.operacionIdExt();
                boolean opNueva = !operacionesVistas.contains(opIdExt);
                operacionesVistas.add(opIdExt);

                UUID operacionId = upsertOperacion(personaId, fila, sistemaOrigen, ahora);
                if (operacionId == null) {
                    operacionId = UUID.randomUUID();
                    insertOperacion(operacionId, personaId, fila, sistemaOrigen, ahora);
                    if (opNueva) operacionesCreadas++;
                } else {
                    updateOperacion(operacionId, fila, sistemaOrigen, ahora);
                    if (opNueva) operacionesActualizadas++;
                }

                // Upsert cuota
                boolean cuotaNueva = upsertCuota(operacionId, fila, sistemaOrigen, ahora);
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

    private Map<String, UUID> resolverEjecutivos(Set<String> usernames) {
        if (usernames.isEmpty()) return new HashMap<>();
        List<Object[]> rows = em.createNativeQuery(
                "SELECT nombre_usuario, id FROM cobranza.usuarios " +
                "WHERE nombre_usuario IN (:usernames) AND activo = TRUE")
                .setParameter("usernames", usernames)
                .getResultList();
        Map<String, UUID> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (UUID) row[1]);
        }
        return result;
    }

    private Map<UUID, UUID> resolverSupervisores(Collection<UUID> ejecutivoIds) {
        if (ejecutivoIds.isEmpty()) return new HashMap<>();
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

    private UUID upsertPersona(String rutNumero, String rutDv, String nombre,
                                String codigoExt, String sistemaOrigen, Instant ahora) {
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.personas WHERE rut_numero = :rn AND rut_dv = :rd")
                .setParameter("rn", rutNumero)
                .setParameter("rd", rutDv)
                .getResultList();
        return rows.isEmpty() ? null : (UUID) rows.get(0);
    }

    private void insertPersona(UUID id, String rutNumero, String rutDv, String nombre,
                                String codigoExt, String sistemaOrigen, Instant ahora) {
        em.createNativeQuery("""
            INSERT INTO cobranza.personas
                (id, rut_numero, rut_dv, nombre, codigo_externo, sistema_origen,
                 fecha_actualizacion_origen, fecha_importacion, fecha_creacion, fecha_actualizacion, version)
            VALUES (:id, :rn, :rd, :nombre, :codigoExt, :sistemaOrigen, :ahora, :ahora, :ahora, :ahora, 0)
            """)
                .setParameter("id", id)
                .setParameter("rn", rutNumero)
                .setParameter("rd", rutDv)
                .setParameter("nombre", nombre)
                .setParameter("codigoExt", codigoExt)
                .setParameter("sistemaOrigen", sistemaOrigen)
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private void updatePersona(UUID id, String nombre, String sistemaOrigen, Instant ahora) {
        em.createNativeQuery("""
            UPDATE cobranza.personas
            SET nombre = :nombre, fecha_actualizacion_origen = :ahora,
                fecha_importacion = :ahora, fecha_actualizacion = :ahora, version = version + 1
            WHERE id = :id
            """)
                .setParameter("id", id)
                .setParameter("nombre", nombre)
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private void upsertDireccion(UUID personaId, FilaCsv fila, String sistemaOrigen, Instant ahora) {
        String tipo = fila.direccionTipo();
        String codigoExt = fila.codigoExtDireccion();

        List<?> rows;
        if (codigoExt != null && !codigoExt.isBlank()) {
            rows = em.createNativeQuery(
                    "SELECT id FROM cobranza.direcciones " +
                    "WHERE persona_id = :pid AND sistema_origen = :so AND codigo_externo = :ce")
                    .setParameter("pid", personaId)
                    .setParameter("so", sistemaOrigen)
                    .setParameter("ce", codigoExt)
                    .getResultList();
        } else {
            rows = em.createNativeQuery(
                    "SELECT id FROM cobranza.direcciones " +
                    "WHERE persona_id = :pid AND tipo = :tipo AND es_principal = TRUE AND vigente = TRUE")
                    .setParameter("pid", personaId)
                    .setParameter("tipo", tipo)
                    .getResultList();
        }

        if (rows.isEmpty()) {
            // Desactivar principal vigente del mismo tipo si existe
            em.createNativeQuery("""
                UPDATE cobranza.direcciones
                SET es_principal = FALSE, vigente = FALSE
                WHERE persona_id = :pid AND tipo = :tipo AND es_principal = TRUE AND vigente = TRUE
                """)
                    .setParameter("pid", personaId)
                    .setParameter("tipo", tipo)
                    .executeUpdate();

            UUID dirId = UUID.randomUUID();
            em.createNativeQuery("""
                INSERT INTO cobranza.direcciones
                    (id, persona_id, tipo, texto, comuna, ciudad, es_principal, vigente,
                     codigo_externo, sistema_origen, fecha_actualizacion_origen, fecha_creacion)
                VALUES (:id, :pid, :tipo, :texto, :comuna, :ciudad, TRUE, TRUE,
                        :codigoExt, :so, :ahora, :ahora)
                """)
                    .setParameter("id", dirId)
                    .setParameter("pid", personaId)
                    .setParameter("tipo", tipo)
                    .setParameter("texto", fila.direccionTexto())
                    .setParameter("comuna", fila.direccionComuna())
                    .setParameter("ciudad", fila.direccionCiudad())
                    .setParameter("codigoExt", codigoExt)
                    .setParameter("so", sistemaOrigen)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        } else {
            UUID dirId = (UUID) rows.get(0);
            em.createNativeQuery("""
                UPDATE cobranza.direcciones
                SET texto = :texto, comuna = :comuna, ciudad = :ciudad,
                    es_principal = TRUE, vigente = TRUE,
                    fecha_actualizacion_origen = :ahora
                WHERE id = :id
                """)
                    .setParameter("id", dirId)
                    .setParameter("texto", fila.direccionTexto())
                    .setParameter("comuna", fila.direccionComuna())
                    .setParameter("ciudad", fila.direccionCiudad())
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        }
    }

    private void upsertCarteraPersona(UUID carteraId, UUID personaId, long timestamp) {
        // Cerrar cualquier vínculo activo con otra cartera (RN-03 revisado)
        em.createNativeQuery("""
            UPDATE cobranza.carteras_personas
            SET activa = FALSE, fecha_fin = CURRENT_DATE, fecha_actualizacion = :ahora, version = version + 1
            WHERE persona_id = :pid AND activa = TRUE AND cartera_id <> :cid
            """)
                .setParameter("pid", personaId)
                .setParameter("cid", carteraId)
                .setParameter("ahora", Instant.ofEpochMilli(timestamp))
                .executeUpdate();

        // Insertar si no existe el vínculo activo con la cartera actual
        em.createNativeQuery("""
            INSERT INTO cobranza.carteras_personas (id, cartera_id, persona_id, activa, fecha_inicio, fecha_creacion, fecha_actualizacion, version)
            SELECT :id, :cid, :pid, TRUE, CURRENT_DATE, :ahora, :ahora, 0
            WHERE NOT EXISTS (
                SELECT 1 FROM cobranza.carteras_personas
                WHERE cartera_id = :cid AND persona_id = :pid AND activa = TRUE
            )
            """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("cid", carteraId)
                .setParameter("pid", personaId)
                .setParameter("ahora", Instant.ofEpochMilli(timestamp))
                .executeUpdate();
    }

    private void upsertAsignacionMensual(UUID carteraId, UUID ejecutivoId, UUID supervisorId,
                                          String periodo, UUID personaId, Instant ahora) {
        if (supervisorId == null) return;

        // Calcular fecha inicio/fin del período
        int year = Integer.parseInt(periodo.substring(0, 4));
        int month = Integer.parseInt(periodo.substring(5, 7));
        LocalDate fechaInicio = LocalDate.of(year, month, 1);
        LocalDate fechaFin = fechaInicio.withDayOfMonth(fechaInicio.lengthOfMonth());

        // Upsert AsignacionMensual del ejecutivo para esta cartera/periodo
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.asignaciones_mensuales " +
                "WHERE cartera_id = :cid AND ejecutivo_id = :eid AND activa = TRUE " +
                "AND fecha_inicio = :fi")
                .setParameter("cid", carteraId)
                .setParameter("eid", ejecutivoId)
                .setParameter("fi", fechaInicio)
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
                    "SET supervisor_id = :sid, fecha_fin = :ff, fecha_actualizacion = :ahora, version = version + 1 " +
                    "WHERE id = :id")
                    .setParameter("id", asignacionId)
                    .setParameter("sid", supervisorId)
                    .setParameter("ff", fechaFin)
                    .setParameter("ahora", ahora)
                    .executeUpdate();
        }

        // Upsert asignacion_mensual_persona
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

    private UUID upsertOperacion(UUID personaId, FilaCsv fila, String sistemaOrigen, Instant ahora) {
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.operaciones " +
                "WHERE sistema_origen = :so AND identificador_externo = :ext")
                .setParameter("so", sistemaOrigen)
                .setParameter("ext", fila.operacionIdExt())
                .getResultList();
        return rows.isEmpty() ? null : (UUID) rows.get(0);
    }

    private void insertOperacion(UUID id, UUID personaId, FilaCsv fila,
                                  String sistemaOrigen, Instant ahora) {
        em.createNativeQuery("""
            INSERT INTO cobranza.operaciones
                (id, persona_id, numero_operacion, identificador_externo, sistema_origen,
                 tipo_operacion, estado, capital, interes_penal, gastos_cobranza, total_vigente,
                 fecha_vencimiento, fecha_actualizacion_origen, fecha_importacion,
                 fecha_creacion, fecha_actualizacion, version)
            VALUES (:id, :pid, :num, :ext, :so, :tipo, :estado,
                    :capital, :ip, :gastos, :total, :fvto, :ahora, :ahora, :ahora, :ahora, 0)
            """)
                .setParameter("id", id)
                .setParameter("pid", personaId)
                .setParameter("num", fila.operacionNumero())
                .setParameter("ext", fila.operacionIdExt())
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

    private void updateOperacion(UUID id, FilaCsv fila, String sistemaOrigen, Instant ahora) {
        em.createNativeQuery("""
            UPDATE cobranza.operaciones
            SET estado = :estado, capital = :capital, interes_penal = :ip,
                gastos_cobranza = :gastos, total_vigente = :total,
                fecha_vencimiento = :fvto, fecha_actualizacion_origen = :ahora,
                fecha_importacion = :ahora, fecha_actualizacion = :ahora, version = version + 1
            WHERE id = :id
            """)
                .setParameter("id", id)
                .setParameter("estado", fila.operacionEstado())
                .setParameter("capital", fila.operacionCapital())
                .setParameter("ip", nvl(fila.operacionInteresPenal(), BigDecimal.ZERO))
                .setParameter("gastos", nvl(fila.operacionGastos(), BigDecimal.ZERO))
                .setParameter("total", fila.operacionTotalVigente())
                .setParameter("fvto", fila.operacionFechaVto())
                .setParameter("ahora", ahora)
                .executeUpdate();
    }

    private boolean upsertCuota(UUID operacionId, FilaCsv fila, String sistemaOrigen, Instant ahora) {
        List<?> rows = em.createNativeQuery(
                "SELECT id FROM cobranza.cuotas WHERE operacion_id = :oid AND numero_cuota = :num")
                .setParameter("oid", operacionId)
                .setParameter("num", fila.cuotaNumero())
                .getResultList();

        if (rows.isEmpty()) {
            UUID cuotaId = UUID.randomUUID();
            em.createNativeQuery("""
                INSERT INTO cobranza.cuotas
                    (id, operacion_id, numero_cuota, identificador_externo, estado,
                     capital, interes, interes_penal, gastos_cobranza, monto_total, saldo,
                     fecha_vencimiento, fecha_actualizacion_origen, fecha_importacion,
                     fecha_creacion, fecha_actualizacion)
                VALUES (:id, :oid, :num, :ext, :estado,
                        :capital, :interes, :ip, :gastos, :monto, :saldo,
                        :fvto, :ahora, :ahora, :ahora, :ahora)
                """)
                    .setParameter("id", cuotaId)
                    .setParameter("oid", operacionId)
                    .setParameter("num", fila.cuotaNumero())
                    .setParameter("ext", fila.cuotaIdExt())
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
