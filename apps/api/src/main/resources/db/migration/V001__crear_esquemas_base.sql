-- V001: Crear esquemas base del sistema
-- Flyway es el propietario exclusivo de todos los objetos de base de datos.
-- Los esquemas se crean aquí y no en los scripts de inicialización Docker.

CREATE SCHEMA IF NOT EXISTS cobranza;
CREATE SCHEMA IF NOT EXISTS auditoria;

COMMENT ON SCHEMA cobranza IS 'Esquema principal del sistema de cobranza en terreno.';
COMMENT ON SCHEMA auditoria IS 'Esquema de auditoría y trazabilidad de operaciones.';
