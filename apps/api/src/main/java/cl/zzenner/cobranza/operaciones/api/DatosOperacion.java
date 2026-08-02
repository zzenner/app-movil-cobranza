package cl.zzenner.cobranza.operaciones.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DatosOperacion(
        UUID id,
        UUID personaId,
        String numeroOperacion,
        String estado,
        BigDecimal capital,
        BigDecimal interesPenal,
        BigDecimal gastosCobranza,
        BigDecimal totalVigente,
        List<DatosCuota> cuotas
) {}
