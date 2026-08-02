# ADR-0028: Gestión inmutable — sin fecha_actualizacion ni @Version

**Estado:** Aceptada  
**Fecha:** 2026-08-01

## Contexto

Las gestiones de cobranza representan hechos registrados en el terreno. Una vez enviadas al servidor, no deben modificarse: no hay rectificaciones ni anulaciones en el MVP (véase ADR-0009).

## Decisión

La entidad `Gestion` y la tabla `cobranza.gestiones` son inmutables por diseño:

- **Sin columna `fecha_actualizacion`**: la tabla no tiene esta columna. No hay concepto de "última modificación".
- **Sin `@Version`**: no se usa control optimista de concurrencia (la entidad no se actualiza).
- **Todos los campos JPA tienen `updatable = false`**: Hibernate no generará sentencias `UPDATE` incluso si se modificaran los valores en memoria (no hay setters de negocio).
- **Sin métodos de modificación de estado**: la única forma de crear una gestión es a través del constructor.

La tabla es append-only a nivel de diseño.

## Consecuencias

- La auditabilidad se basa exclusivamente en `fecha_creacion_servidor` (timestamp del servidor) y `fecha_gestion` (timestamp del dispositivo)
- Si se detecta un error en una gestión registrada, el proceso de negocio correcto es registrar una nueva gestión que lo aclare (no editar la anterior)
- Simplifica la lógica de sincronización: el cliente solo envía inserciones, nunca actualizaciones
