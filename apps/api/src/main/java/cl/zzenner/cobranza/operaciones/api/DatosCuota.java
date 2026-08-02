package cl.zzenner.cobranza.operaciones.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosCuota(
        int numeroCuota,
        String estado,
        BigDecimal montoTotal,
        BigDecimal saldo,
        LocalDate fechaVencimiento
) {}
