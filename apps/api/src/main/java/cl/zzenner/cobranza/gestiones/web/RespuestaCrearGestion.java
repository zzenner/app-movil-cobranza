package cl.zzenner.cobranza.gestiones.web;

import cl.zzenner.cobranza.gestiones.api.ResultadoRecepcion;

import java.util.UUID;

public record RespuestaCrearGestion(UUID id, ResultadoRecepcion resultado) {}
