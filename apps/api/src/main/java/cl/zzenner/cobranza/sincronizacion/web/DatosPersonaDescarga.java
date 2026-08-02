package cl.zzenner.cobranza.sincronizacion.web;

import cl.zzenner.cobranza.gestiones.api.DatosGestion;
import cl.zzenner.cobranza.operaciones.api.DatosOperacion;
import cl.zzenner.cobranza.personas.api.DatosAval;
import cl.zzenner.cobranza.personas.api.DatosDireccion;

import java.util.List;
import java.util.UUID;

public record DatosPersonaDescarga(
    UUID id,
    String rutNumero,
    String rutDv,
    String nombre,
    List<DatosDireccion> direcciones,
    List<DatosAval> avales,
    List<DatosOperacion> operaciones,
    List<DatosGestion> ultimasGestiones
) {}
