package cl.zzenner.cobranza.sincronizacion.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RespuestaAsignacionDiaria(
    UUID id,
    UUID ejecutivoId,
    LocalDate fecha,
    String estado,
    List<DatosPersonaDescarga> personas
) {}
