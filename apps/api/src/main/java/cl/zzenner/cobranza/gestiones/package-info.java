/**
 * Módulo de gestiones.
 *
 * <p>Responsabilidad: recepción idempotente, persistencia y consulta de gestiones de cobranza.
 * Las gestiones son inmutables. El UUID se genera en el dispositivo Android.
 * Tipos: CONTACTO_FAMILIAR, COMPROMISO_PAGO, SIN_CONTACTO.
 * Orígenes: ASIGNACION_DIARIA (diaria PUBLICADA o FINALIZADA), BUSQUEDA_DIRECTA (búsqueda global).
 *
 * <p>Dependencias permitidas: asignaciones::api, personas::api, usuarios::api.
 */
@ApplicationModule(
    displayName = "Gestiones",
    allowedDependencies = {"asignaciones::api", "personas::api", "usuarios::api"}
)
package cl.zzenner.cobranza.gestiones;

import org.springframework.modulith.ApplicationModule;
