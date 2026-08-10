package cl.zzenner.cobranza.importacion.web;

import cl.zzenner.cobranza.importacion.dominio.ImportacionMensual;

import java.time.Instant;
import java.util.UUID;

record ImportacionDetalle(
        UUID id,
        UUID carteraId,
        UUID usuarioId,
        String periodo,
        String sistemaOrigen,
        String estado,
        String nombreArchivoOriginal,
        Integer filasTotales,
        Integer filasProcesadas,
        Integer filasRechazadas,
        Integer filasAdvertencia,
        Integer personasCreadas,
        Integer personasActualizadas,
        Integer operacionesCreadas,
        Integer operacionesActualizadas,
        Integer cuotasCreadas,
        Integer cuotasActualizadas,
        String mensajeError,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        long version
) {
    static ImportacionDetalle desde(ImportacionMensual im) {
        return new ImportacionDetalle(
                im.getId(), im.getCarteraId(), im.getUsuarioId(),
                im.getPeriodo(), im.getSistemaOrigen(), im.getEstado().name(),
                im.getNombreArchivoOriginal(),
                im.getFilasTotales(), im.getFilasProcesadas(),
                im.getFilasRechazadas(), im.getFilasAdvertencia(),
                im.getPersonasCreadas(), im.getPersonasActualizadas(),
                im.getOperacionesCreadas(), im.getOperacionesActualizadas(),
                im.getCuotasCreadas(), im.getCuotasActualizadas(),
                im.getMensajeError(), im.getFechaCreacion(), im.getFechaActualizacion(),
                im.getVersion());
    }
}
