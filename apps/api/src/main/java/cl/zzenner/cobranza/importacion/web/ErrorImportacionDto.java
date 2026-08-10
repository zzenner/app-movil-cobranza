package cl.zzenner.cobranza.importacion.web;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;

import java.util.UUID;

record ErrorImportacionDto(
        UUID id,
        Integer numeroFila,
        String columna,
        String codigoError,
        String nivel,
        String mensaje
) {
    static ErrorImportacionDto desde(ErrorImportacion e) {
        return new ErrorImportacionDto(
                e.getId(), e.getNumeroFila(), e.getColumna(),
                e.getCodigoError(), e.getNivel().name(), e.getMensaje());
    }
}
