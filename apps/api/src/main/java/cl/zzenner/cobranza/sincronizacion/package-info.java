/**
 * Módulo de sincronización.
 *
 * <p>Responsabilidad: coordinación del ciclo de sincronización con dispositivos Android.
 * Incluye descarga de asignaciones diarias activas y recepción de gestiones del outbox.
 *
 * <p>Depende de: asignaciones::api, personas::api, operaciones::api, gestiones::api.
 */
@ApplicationModule(
    displayName = "Sincronizacion",
    allowedDependencies = {"asignaciones::api", "personas::api", "operaciones::api", "gestiones::api"}
)
package cl.zzenner.cobranza.sincronizacion;

import org.springframework.modulith.ApplicationModule;
