-- Esquemas lógicos iniciales.
-- Los esquemas separan responsabilidades de los módulos.
-- Las tablas de negocio se crearán con Flyway cuando comience la Fase 1.

-- Esquema principal de la aplicación.
CREATE SCHEMA IF NOT EXISTS cobranza;

-- Esquema para auditoría y trazabilidad de operaciones.
CREATE SCHEMA IF NOT EXISTS auditoria;

COMMENT ON SCHEMA cobranza IS 'Esquema principal del sistema de cobranza en terreno.';
COMMENT ON SCHEMA auditoria IS 'Esquema de auditoría y trazabilidad de operaciones.';
