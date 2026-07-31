/**
 * Módulo de carteras.
 *
 * <p>Responsabilidad: gestión de carteras de cobranza. La API es autoridad
 * de las carteras; no son una copia operacional del sistema externo.
 * Otros módulos acceden a carteras únicamente a través de {@code carteras::api}.
 *
 * <p>Dependencias permitidas: compartido.
 */
@ApplicationModule(displayName = "Carteras", allowedDependencies = "compartido")
package cl.zzenner.cobranza.carteras;

import org.springframework.modulith.ApplicationModule;
