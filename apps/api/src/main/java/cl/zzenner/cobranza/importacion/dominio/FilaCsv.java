package cl.zzenner.cobranza.importacion.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FilaCsv(
        int numeroFila,

        // Persona
        String rutNumero,
        String rutDv,
        String nombrePersona,
        String codigoExtPersona,

        // Dirección
        String direccionTexto,
        String direccionTipo,
        String direccionComuna,
        String direccionCiudad,
        String codigoExtDireccion,

        // Operación
        String operacionNumero,
        String operacionIdExt,
        String operacionTipo,
        String operacionEstado,
        BigDecimal operacionCapital,
        BigDecimal operacionInteresPenal,
        BigDecimal operacionGastos,
        BigDecimal operacionTotalVigente,
        LocalDate operacionFechaVto,

        // Cuota
        Integer cuotaNumero,
        String cuotaIdExt,
        String cuotaEstado,
        BigDecimal cuotaMontoTotal,
        BigDecimal cuotaCapital,
        BigDecimal cuotaInteres,
        BigDecimal cuotaInteresPenal,
        BigDecimal cuotaGastos,
        BigDecimal cuotaSaldo,
        LocalDate cuotaFechaVto,

        // Asignación
        String ejecutivoUsername
) {}
