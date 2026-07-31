-- V007: Operaciones y cuotas
-- Flyway es el propietario exclusivo de todos los objetos de base de datos.
-- Ambas tablas son copias operacionales del sistema externo (ADR-0014).
-- Los valores financieros del servidor reemplazan los locales en cada sincronización (RN-15).

-- ============================================================
-- OPERACIONES
-- Copia operacional de créditos de una persona.
-- Estados: ACTIVA (se descarga), ANULADA/CERRADA/PAGADA (no se descargan) — RN-10.
-- ============================================================
CREATE TABLE cobranza.operaciones (
    id                          UUID          NOT NULL,
    persona_id                  UUID          NOT NULL,
    numero_operacion            VARCHAR(50)   NOT NULL,
    identificador_externo       VARCHAR(100),
    sistema_origen              VARCHAR(50)   NOT NULL DEFAULT 'LEGADO',
    tipo_operacion              VARCHAR(50),
    estado                      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    moneda                      VARCHAR(3)    NOT NULL DEFAULT 'CLP',
    capital                     NUMERIC(15,2) NOT NULL,
    interes_penal               NUMERIC(15,2) NOT NULL DEFAULT 0,
    gastos_cobranza             NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_vigente               NUMERIC(15,2),
    fecha_otorgamiento          DATE,
    fecha_vencimiento           DATE,
    fecha_actualizacion_origen  TIMESTAMPTZ,
    fecha_importacion           TIMESTAMPTZ,
    fecha_creacion              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_operaciones               PRIMARY KEY (id),
    CONSTRAINT fk_operaciones_persona       FOREIGN KEY (persona_id)
        REFERENCES cobranza.personas(id),
    CONSTRAINT ck_operaciones_estado        CHECK (estado IN ('ACTIVA','ANULADA','CERRADA','PAGADA')),
    CONSTRAINT ck_operaciones_moneda        CHECK (moneda = 'CLP'),
    CONSTRAINT ck_operaciones_capital       CHECK (capital >= 0),
    CONSTRAINT ck_operaciones_interes_penal CHECK (interes_penal >= 0),
    CONSTRAINT ck_operaciones_gastos        CHECK (gastos_cobranza >= 0),
    CONSTRAINT ck_operaciones_total         CHECK (total_vigente IS NULL OR total_vigente >= 0)
);

CREATE INDEX idx_operaciones_persona ON cobranza.operaciones (persona_id);
-- Índice parcial: solo operaciones activas se consultan frecuentemente para descarga
CREATE INDEX idx_operaciones_activas ON cobranza.operaciones (persona_id)
    WHERE estado = 'ACTIVA';

-- Unicidad por identificador externo y sistema de origen
CREATE UNIQUE INDEX uq_operaciones_externo
    ON cobranza.operaciones (sistema_origen, identificador_externo)
    WHERE identificador_externo IS NOT NULL;

-- ============================================================
-- CUOTAS
-- Actualizables en cada sincronización (RN-15).
-- Estados: VENCIDA, VIGENTE, FUTURA (se descargan); PAGADA (no se descarga).
-- ============================================================
CREATE TABLE cobranza.cuotas (
    id                          UUID          NOT NULL,
    operacion_id                UUID          NOT NULL,
    numero_cuota                INTEGER       NOT NULL,
    identificador_externo       VARCHAR(100),
    estado                      VARCHAR(20)   NOT NULL DEFAULT 'VIGENTE',
    capital                     NUMERIC(15,2),
    interes                     NUMERIC(15,2),
    interes_penal               NUMERIC(15,2) NOT NULL DEFAULT 0,
    gastos_cobranza             NUMERIC(15,2) NOT NULL DEFAULT 0,
    monto_total                 NUMERIC(15,2) NOT NULL,
    saldo                       NUMERIC(15,2),
    fecha_vencimiento           DATE          NOT NULL,
    fecha_pago                  DATE,
    fecha_actualizacion_origen  TIMESTAMPTZ,
    fecha_importacion           TIMESTAMPTZ,
    fecha_creacion              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_cuotas                    PRIMARY KEY (id),
    CONSTRAINT fk_cuotas_operacion          FOREIGN KEY (operacion_id)
        REFERENCES cobranza.operaciones(id),
    CONSTRAINT uq_cuotas_operacion_numero   UNIQUE (operacion_id, numero_cuota),
    CONSTRAINT ck_cuotas_estado             CHECK (estado IN ('VENCIDA','VIGENTE','FUTURA','PAGADA')),
    CONSTRAINT ck_cuotas_numero_cuota       CHECK (numero_cuota > 0),
    CONSTRAINT ck_cuotas_interes_penal      CHECK (interes_penal >= 0),
    CONSTRAINT ck_cuotas_gastos             CHECK (gastos_cobranza >= 0),
    CONSTRAINT ck_cuotas_monto_total        CHECK (monto_total >= 0)
);

CREATE INDEX idx_cuotas_operacion ON cobranza.cuotas (operacion_id);
-- Índice para filtrado en descarga al dispositivo (RN-10)
CREATE INDEX idx_cuotas_descarga ON cobranza.cuotas (operacion_id, estado)
    WHERE estado IN ('VENCIDA','VIGENTE','FUTURA');

-- Unicidad por identificador externo dentro de la operación
CREATE UNIQUE INDEX uq_cuotas_externo
    ON cobranza.cuotas (operacion_id, identificador_externo)
    WHERE identificador_externo IS NOT NULL;
