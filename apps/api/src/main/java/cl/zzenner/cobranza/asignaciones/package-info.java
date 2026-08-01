/**
 * Módulo de asignaciones.
 *
 * <p>Responsabilidad: asignaciones mensuales y diarias de personas a ejecutivos.
 * Estados de asignación diaria: BORRADOR, PUBLICADA, FINALIZADA, CANCELADA.
 *
 * <p>Dependencias permitidas: carteras::api, personas::api, usuarios::api.
 */
@ApplicationModule(
    displayName = "Asignaciones",
    allowedDependencies = {"carteras::api", "personas::api", "usuarios::api"}
)
package cl.zzenner.cobranza.asignaciones;

import org.springframework.modulith.ApplicationModule;
