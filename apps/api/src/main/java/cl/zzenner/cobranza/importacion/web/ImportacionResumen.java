package cl.zzenner.cobranza.importacion.web;

import cl.zzenner.cobranza.importacion.dominio.ImportacionMensual;

import java.time.Instant;
import java.util.UUID;

record ImportacionResumen(
        UUID id,
        UUID carteraId,
        String periodo,
        String sistemaOrigen,
        String estado,
        String nombreArchivoOriginal,
        Integer filasTotales,
        Integer filasProcesadas,
        Integer filasRechazadas,
        Integer filasAdvertencia,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {
    static ImportacionResumen desde(ImportacionMensual im) {
        return new ImportacionResumen(
                im.getId(), im.getCarteraId(), im.getPeriodo(), im.getSistemaOrigen(),
                im.getEstado().name(), im.getNombreArchivoOriginal(),
                im.getFilasTotales(), im.getFilasProcesadas(),
                im.getFilasRechazadas(), im.getFilasAdvertencia(),
                im.getFechaCreacion(), im.getFechaActualizacion());
    }
}
