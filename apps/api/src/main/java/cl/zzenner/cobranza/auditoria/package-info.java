/**
 * Módulo de auditoría.
 *
 * <p>Responsabilidad: registro de eventos y trazabilidad de operaciones críticas del sistema.
 * Escribe en el esquema PostgreSQL {@code auditoria}.
 * No puede ser importado por otros módulos; solo consume eventos publicados.
 *
 * <p>Estado: stub — pendiente de implementación en Fase 2.
 */
@ApplicationModule(displayName = "Auditoria")
package cl.zzenner.cobranza.auditoria;

import org.springframework.modulith.ApplicationModule;
