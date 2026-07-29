/**
 * Módulo de autenticación.
 *
 * <p>Responsabilidad: login, emisión y renovación de tokens JWT, logout.
 * Depende de: usuarios, dispositivos.
 *
 * <p>Estado: stub — pendiente de implementación en Fase 2.
 */
@ApplicationModule(
        displayName = "Autenticacion",
        allowedDependencies = {"usuarios::api", "dispositivos::api"}
)
package cl.zzenner.cobranza.autenticacion;

import org.springframework.modulith.ApplicationModule;
