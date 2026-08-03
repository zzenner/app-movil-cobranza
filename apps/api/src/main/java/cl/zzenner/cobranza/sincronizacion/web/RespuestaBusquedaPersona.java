package cl.zzenner.cobranza.sincronizacion.web;

import java.time.Instant;

public record RespuestaBusquedaPersona(
    int version,
    Instant generadoEn,
    DatosPersonaDescarga persona
) {}
