package cl.zzenner.cobranza.importacion.web;

import java.util.List;

record RespuestaPaginaErrores(
        List<ErrorImportacionDto> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {}
