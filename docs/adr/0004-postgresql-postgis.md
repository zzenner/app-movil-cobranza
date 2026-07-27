# ADR-0004 — PostgreSQL con PostGIS como base de datos principal

## Estado
Aceptado.

## Contexto
El sistema necesita almacenar datos relacionales complejos (personas, créditos, cuotas, gestiones) y datos geoespaciales (coordenadas de gestiones en terreno). Se necesita elegir el motor de base de datos.

## Decisión
Usar PostgreSQL 16 con la extensión PostGIS 3.4 como único motor de base de datos relacional del sistema.

## Consecuencias

**Positivas:**
- PostgreSQL es el motor relacional open source más completo y maduro disponible.
- PostGIS añade soporte geoespacial de primera clase: tipos `GEOMETRY`, funciones de distancia, índices espaciales GIST. No requiere un motor separado para datos de ubicación.
- Excelente integración con Spring Boot via Spring Data JPA e Hibernate Spatial.
- Amplia compatibilidad con herramientas de administración, backup y monitoreo.
- Soporte nativo de UUID, JSONB y tipos de rango que pueden ser útiles en el dominio.

**Negativas:**
- Operativamente más complejo que SQLite puro (requiere servidor, configuración, backup).
- La extensión PostGIS añade tamaño a la imagen Docker y requiere activación explícita.

## Alternativas consideradas

**MySQL / MariaDB:** Sin soporte geoespacial equivalente a PostGIS. Menor expresividad en tipos de datos. Se descartó.

**MongoDB:** No relacional. Las relaciones del dominio (personas → créditos → cuotas) se modelan mejor en un esquema relacional. Se descartó.

**SQLite (solo):** No adecuado para el servidor; sí se usa en Android via Room.

## Notas
La imagen oficial `postgis/postgis:16-3.4` activa PostGIS automáticamente en la base de datos por defecto, simplificando la configuración.
