package cl.zzenner.cobranza.usuarios.web;

import java.util.List;

public record RespuestaListadoUsuarios(
        List<ItemListadoUsuarioAdmin> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {}
