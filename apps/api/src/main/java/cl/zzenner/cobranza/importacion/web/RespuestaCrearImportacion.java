package cl.zzenner.cobranza.importacion.web;

import java.util.UUID;

record RespuestaCrearImportacion(
        UUID importacionId,
        String estado,
        String periodo,
        String nombreArchivoOriginal
) {}
