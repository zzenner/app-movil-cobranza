-- Extensiones requeridas por el sistema de cobranza.
-- La imagen postgis/postgis ya activa PostGIS en la base de datos por defecto,
-- pero se incluye aquí para que sea explícito y funcione en cualquier imagen compatible.

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- uuid-ossp permite generar UUIDs directamente en PostgreSQL.
-- Se usa para claves primarias generadas en la base de datos cuando aplique.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- pg_trgm habilita búsquedas por similitud de texto (útil para búsqueda por RUT o nombre).
CREATE EXTENSION IF NOT EXISTS pg_trgm;
