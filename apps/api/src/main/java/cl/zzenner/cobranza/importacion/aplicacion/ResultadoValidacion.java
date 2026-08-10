package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import cl.zzenner.cobranza.importacion.dominio.FilaCsv;

import java.util.List;

record ResultadoValidacion(
        List<FilaCsv> filasValidas,
        List<ErrorImportacion> errores,
        int totalFilas,
        int filasRechazadas,
        int filasAdvertencia
) {
    boolean tieneErrores() {
        return filasRechazadas > 0 ||
                errores.stream().anyMatch(e ->
                        e.getNivel() == cl.zzenner.cobranza.importacion.dominio.NivelError.ERROR);
    }
}
