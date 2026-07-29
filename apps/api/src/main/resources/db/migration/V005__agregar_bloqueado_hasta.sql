-- V005: Columna bloqueado_hasta para bloqueo temporal automático en usuarios
-- Semántica:
--   bloqueado = TRUE                                      → bloqueo administrativo permanente
--   bloqueado_hasta IS NOT NULL AND now() < bloqueado_hasta → bloqueo temporal por intentos fallidos
--   Ambas condiciones pueden coexistir.
ALTER TABLE cobranza.usuarios
    ADD COLUMN bloqueado_hasta TIMESTAMPTZ;
