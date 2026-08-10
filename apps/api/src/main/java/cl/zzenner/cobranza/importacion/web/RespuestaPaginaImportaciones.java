package cl.zzenner.cobranza.importacion.web;

import java.util.List;

record RespuestaPaginaImportaciones(
        List<ImportacionResumen> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {}
