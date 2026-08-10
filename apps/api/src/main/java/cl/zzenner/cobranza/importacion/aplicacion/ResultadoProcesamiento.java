package cl.zzenner.cobranza.importacion.aplicacion;

record ResultadoProcesamiento(
        int filasProcesadas,
        int filasRechazadas,
        int filasAdvertencia,
        int personasCreadas,
        int personasActualizadas,
        int operacionesCreadas,
        int operacionesActualizadas,
        int cuotasCreadas,
        int cuotasActualizadas
) {}
