package cl.zzenner.cobranza.importacion.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FilaCsv(
        int numeroFila,

        // Temporal — período de cobranza (YYYY-MM)
        String periodo,

        // Persona
        String rutNumero,
        String rutDv,
        String nombrePersona,

        // Operación
        String operacionNumero,
        String operacionTipo,
        String operacionEstado,
        BigDecimal operacionCapital,
        BigDecimal operacionInteresPenal,
        BigDecimal operacionGastos,
        BigDecimal operacionTotalVigente,
        LocalDate operacionFechaVto,

        // Cuota
        Integer cuotaNumero,
        String cuotaEstado,
        BigDecimal cuotaMontoTotal,
        BigDecimal cuotaCapital,
        BigDecimal cuotaInteres,
        BigDecimal cuotaInteresPenal,
        BigDecimal cuotaGastos,
        BigDecimal cuotaSaldo,
        LocalDate cuotaFechaVto,

        // Ejecutivo — identificador en el sistema origen (ej: '2127')
        // No es el nombre_usuario de la plataforma
        String codigoEjecutivo,

        // Direcciones del sistema origen
        String dirParticular,
        String dirComercial,

        // Clasificación de cartera y estado judicial
        String codigoCartera,
        String marcaJudicial
) {}
