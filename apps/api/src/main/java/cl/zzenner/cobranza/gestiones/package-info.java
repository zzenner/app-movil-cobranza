/**
 * Módulo de gestiones.
 *
 * <p>Responsabilidad: recepción idempotente, persistencia y consulta de gestiones de cobranza.
 * Las gestiones son inmutables. El UUID se genera en el dispositivo Android.
 * Tipos: CONTACTO_FAMILIAR, COMPROMISO_PAGO, SIN_CONTACTO.
 * Depende de: personas, usuarios, dispositivos.
 *
 * <p>Estado: stub — pendiente de implementación en Fase 2.
 */
@ApplicationModule(displayName = "Gestiones")
package cl.zzenner.cobranza.gestiones;

import org.springframework.modulith.ApplicationModule;
