/**
 * Módulo de personas.
 *
 * <p>Responsabilidad: copia operacional de personas y sus datos asociados
 * (RUT, avales, direcciones importadas).
 * Los datos provienen del sistema externo y se actualizan por importación (ADR-0014).
 *
 * <p>Dependencias permitidas: compartido, carteras::api.
 */
@ApplicationModule(displayName = "Personas", allowedDependencies = {"compartido", "carteras::api"})
package cl.zzenner.cobranza.personas;

import org.springframework.modulith.ApplicationModule;
