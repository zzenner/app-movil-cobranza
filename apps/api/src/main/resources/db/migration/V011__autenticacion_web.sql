-- V011: Autenticación web separada de Android
--
-- Agrega tipo_cliente a sesiones_autenticacion y hace dispositivo_id nullable
-- para sesiones WEB (que no tienen dispositivo físico asociado).
-- Las sesiones ANDROID mantienen dispositivo_id obligatorio.

-- 1. Añadir tipo_cliente con default ANDROID (retrocompatible)
ALTER TABLE cobranza.sesiones_autenticacion
    ADD COLUMN tipo_cliente VARCHAR(10) NOT NULL DEFAULT 'ANDROID';

-- 2. Hacer dispositivo_id nullable (remover NOT NULL)
ALTER TABLE cobranza.sesiones_autenticacion
    ALTER COLUMN dispositivo_id DROP NOT NULL;

-- 3. CHECK: Android requiere dispositivo, WEB no tiene dispositivo
ALTER TABLE cobranza.sesiones_autenticacion
    ADD CONSTRAINT ck_sesiones_cliente_dispositivo CHECK (
        (tipo_cliente = 'ANDROID' AND dispositivo_id IS NOT NULL)
        OR
        (tipo_cliente = 'WEB'     AND dispositivo_id IS NULL)
    );

-- 4. CHECK: tipo_cliente solo admite valores conocidos
ALTER TABLE cobranza.sesiones_autenticacion
    ADD CONSTRAINT ck_sesiones_tipo_cliente CHECK (
        tipo_cliente IN ('ANDROID', 'WEB')
    );

-- 5. Reemplazar el índice único global por uno específico de ANDROID
--    (PostgreSQL permite múltiples NULL en índices únicos, por lo que el índice
--     original permitiría varias sesiones WEB activas por el mismo usuario,
--     pero no lo garantiza de forma explícita para ANDROID.)
DROP INDEX IF EXISTS cobranza.uq_sesiones_activa_usuario_dispositivo;

-- Índice para Android: un solo estado ACTIVA por par (usuario, dispositivo)
CREATE UNIQUE INDEX uq_sesiones_activa_android
    ON cobranza.sesiones_autenticacion (usuario_id, dispositivo_id)
    WHERE estado = 'ACTIVA' AND tipo_cliente = 'ANDROID';

-- Índice para Web: un solo estado ACTIVA por usuario (una sesión web activa por vez)
CREATE UNIQUE INDEX uq_sesiones_activa_web
    ON cobranza.sesiones_autenticacion (usuario_id)
    WHERE estado = 'ACTIVA' AND tipo_cliente = 'WEB';
