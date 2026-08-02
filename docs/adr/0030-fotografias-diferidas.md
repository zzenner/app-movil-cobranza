# ADR-0030: Fotografías de gestión diferidas a Fase 3D

**Estado:** Aceptada  
**Fecha:** 2026-08-01

## Contexto

Las gestiones de cobranza pueden incluir fotografías como evidencia. Sin embargo, el manejo de archivos binarios requiere decisiones adicionales: almacenamiento (filesystem, S3, PostgreSQL bytea), límites de tamaño, compresión, sincronización offline de blobs, y permisos de acceso.

## Decisión

La tabla `fotografias_gestion` **no se crea en la Fase 3C**. Las fotografías son completamente diferidas a la Fase 3D.

La tabla `gestiones` no incluye ninguna columna relacionada con archivos.

## Consecuencias

- Alcance de Fase 3C está acotado a la gestión textual + GPS
- No hay deuda técnica activa: es una función prevista, no una omisión accidental
- La Fase 3D deberá decidir: almacenamiento externo (S3/similar) vs. bytea en PostgreSQL
- La relación `fotografias_gestion.gestion_id → gestiones.id` estará disponible cuando se cree la tabla en Fase 3D
