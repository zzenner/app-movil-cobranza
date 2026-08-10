-- V012: Importación mensual administrativa de datos de cobranza (Fase 5C)
-- Flyway es el propietario exclusivo de todos los objetos de base de datos.
--
-- Incluye:
--   1. Unicidad persona↔cartera activa (RN-03 revisada)
--   2. Tabla importaciones_mensuales
--   3. Tabla errores_importacion
--   4. Permiso DATOS_IMPORTAR
--   5. Asignación de permiso a roles administradores

-- ============================================================
-- 1. UNICIDAD PERSONA–CARTERA ACTIVA
-- RN-03 (revisado Fase 5C): una persona tiene como máximo UNA cartera activa.
-- Antes de crear el índice verificar que no existan conflictos.
-- ============================================================
DO $$
DECLARE
    v_conflictos INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO v_conflictos
    FROM (
        SELECT persona_id
        FROM cobranza.carteras_personas
        WHERE activa = TRUE
        GROUP BY persona_id
        HAVING COUNT(*) > 1
    ) sub;

    IF v_conflictos > 0 THEN
        RAISE EXCEPTION
            'V012 abortada: existen % persona(s) con más de una cartera activa. '
            'Resolver manualmente antes de migrar.',
            v_conflictos;
    END IF;
END $$;

-- Índice único: una persona solo puede tener una cartera activa
CREATE UNIQUE INDEX uq_cp_persona_activa
    ON cobranza.carteras_personas (persona_id)
    WHERE activa = TRUE;

-- ============================================================
-- 2. IMPORTACIONES_MENSUALES
-- Registro de cada intento de importación CSV.
-- El archivo físico se gestiona externamente (ArchivoImportacionStorage).
-- ============================================================
CREATE TABLE cobranza.importaciones_mensuales (
    id                      UUID          NOT NULL,
    cartera_id              UUID          NOT NULL,
    usuario_id              UUID          NOT NULL,
    periodo                 VARCHAR(7)    NOT NULL,
    sistema_origen          VARCHAR(50)   NOT NULL DEFAULT 'LEGADO',
    estado                  VARCHAR(20)   NOT NULL DEFAULT 'RECIBIDA',
    hash_archivo            VARCHAR(64)   NOT NULL,
    nombre_archivo_original VARCHAR(500)  NOT NULL,
    ruta_archivo            TEXT,
    filas_totales           INTEGER,
    filas_procesadas        INTEGER,
    filas_rechazadas        INTEGER,
    filas_advertencia       INTEGER,
    personas_creadas        INTEGER,
    personas_actualizadas   INTEGER,
    operaciones_creadas     INTEGER,
    operaciones_actualizadas INTEGER,
    cuotas_creadas          INTEGER,
    cuotas_actualizadas     INTEGER,
    mensaje_error           TEXT,
    fecha_creacion          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_importaciones_mensuales     PRIMARY KEY (id),
    CONSTRAINT fk_im_cartera                  FOREIGN KEY (cartera_id)
        REFERENCES cobranza.carteras(id),
    CONSTRAINT fk_im_usuario                  FOREIGN KEY (usuario_id)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT ck_im_estado                   CHECK (estado IN (
        'RECIBIDA','VALIDANDO','VALIDADA','CON_ERRORES','PROCESANDO','COMPLETADA','FALLIDA','EXPIRADA'
    )),
    CONSTRAINT ck_im_periodo                  CHECK (periodo ~ '^\d{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT ck_im_nombre_no_vacio          CHECK (btrim(nombre_archivo_original) <> ''),
    CONSTRAINT ck_im_hash_longitud            CHECK (length(hash_archivo) = 64)
);

-- Índice para detección de duplicados (idempotencia)
CREATE UNIQUE INDEX uq_im_idempotencia
    ON cobranza.importaciones_mensuales (hash_archivo, periodo, cartera_id, sistema_origen)
    WHERE estado = 'COMPLETADA';

-- Índices de consulta frecuente
CREATE INDEX idx_im_cartera     ON cobranza.importaciones_mensuales (cartera_id);
CREATE INDEX idx_im_periodo     ON cobranza.importaciones_mensuales (periodo);
CREATE INDEX idx_im_estado      ON cobranza.importaciones_mensuales (estado);
CREATE INDEX idx_im_usuario     ON cobranza.importaciones_mensuales (usuario_id);
CREATE INDEX idx_im_actualizacion ON cobranza.importaciones_mensuales (fecha_actualizacion);

-- ============================================================
-- 3. ERRORES_IMPORTACION
-- Registro de errores y advertencias por fila o globales.
-- NO almacenar PII (RUT, nombres, montos, texto de fila original).
-- numero_fila nullable para errores globales del archivo.
-- ============================================================
CREATE TABLE cobranza.errores_importacion (
    id              UUID         NOT NULL,
    importacion_id  UUID         NOT NULL,
    numero_fila     INTEGER,
    columna         VARCHAR(100),
    codigo_error    VARCHAR(100) NOT NULL,
    nivel           VARCHAR(20)  NOT NULL DEFAULT 'ERROR',
    mensaje         TEXT         NOT NULL,
    CONSTRAINT pk_errores_importacion       PRIMARY KEY (id),
    CONSTRAINT fk_ei_importacion            FOREIGN KEY (importacion_id)
        REFERENCES cobranza.importaciones_mensuales(id),
    CONSTRAINT ck_ei_nivel                  CHECK (nivel IN ('ERROR','ADVERTENCIA')),
    CONSTRAINT ck_ei_codigo_no_vacio        CHECK (btrim(codigo_error) <> ''),
    CONSTRAINT ck_ei_numero_fila            CHECK (numero_fila IS NULL OR numero_fila > 0)
);

CREATE INDEX idx_ei_importacion ON cobranza.errores_importacion (importacion_id);
CREATE INDEX idx_ei_nivel       ON cobranza.errores_importacion (importacion_id, nivel);

-- ============================================================
-- 4. AMPLIAR ESTADOS VÁLIDOS DE OPERACIONES
-- El sistema origen usa vocabulario externo (VIGENTE, VENCIDO, CASTIGADO).
-- Se amplía el check constraint para aceptar estados del sistema externo.
-- ============================================================
ALTER TABLE cobranza.operaciones
    DROP CONSTRAINT ck_operaciones_estado;

ALTER TABLE cobranza.operaciones
    ADD CONSTRAINT ck_operaciones_estado
        CHECK (estado IN ('ACTIVA','ANULADA','CERRADA','PAGADA','VIGENTE','VENCIDO','CASTIGADO'));

-- ============================================================
-- 5. PERMISO DATOS_IMPORTAR
-- Siguiente UUID en la secuencia: a1b2c3d4-0002-0002-0002-000000000008
-- Verificar que no esté utilizado antes de insertar.
-- ============================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM cobranza.permisos
        WHERE id = 'a1b2c3d4-0002-0002-0002-000000000008'::uuid
    ) THEN
        RAISE EXCEPTION 'UUID a1b2c3d4-0002-0002-0002-000000000008 ya está en uso en permisos.';
    END IF;
END $$;

INSERT INTO cobranza.permisos (id, codigo, nombre, descripcion) VALUES
    ('a1b2c3d4-0002-0002-0002-000000000008',
     'DATOS_IMPORTAR',
     'Importar datos de cobranza',
     'Subir, validar y confirmar archivos CSV de importación mensual.');

-- ============================================================
-- 6. ASIGNAR DATOS_IMPORTAR A ROLES ADMINISTRADORES
-- Solo JEFE_SUPERVISORES y TECNOLOGIA (no SUPERVISOR ni EJECUTIVO_TERRENO).
-- ============================================================
INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo IN ('JEFE_SUPERVISORES', 'TECNOLOGIA')
  AND p.codigo = 'DATOS_IMPORTAR';
