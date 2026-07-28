# ADR-0018: Roles y permisos almacenados en tablas de base de datos

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

El sistema necesita un modelo de autorización flexible que permita asignar roles con conjuntos de permisos a usuarios. La pregunta es si los roles y permisos deben ser enumeraciones Java (código) o registros en base de datos.

## Decisión

Los roles y permisos se almacenan en las tablas `cobranza.roles` y `cobranza.permisos`. Los UUIDs de los registros iniciales son estables y definidos explícitamente en la migración V003.

El enum `CodigoRol` en Java **no es la fuente de verdad** — existe únicamente para uso en código de validación de negocio. La fuente de verdad es la base de datos.

## Permisos definidos (V003)

| Código                     | Descripción                              |
|----------------------------|------------------------------------------|
| USUARIOS_VER               | Ver lista y detalle de usuarios          |
| USUARIOS_ADMINISTRAR       | Crear, modificar y desactivar usuarios   |
| ASIGNACIONES_VER           | Ver carteras y asignaciones              |
| ASIGNACIONES_ADMINISTRAR   | Crear y modificar asignaciones           |
| GESTIONES_VER              | Ver gestiones de cobranza                |
| GESTIONES_CREAR            | Registrar gestiones de cobranza          |
| SINCRONIZACION_VER         | Ver estado de sincronización             |

## Consecuencias

**Positivas**:
- Permite agregar, modificar o inactivar roles y permisos sin redeployar la aplicación.
- El administrador puede crear roles personalizados en el futuro.
- Los UUIDs estables facilitan migraciones entre entornos.

**Negativas**:
- El código de negocio que depende de permisos específicos debe consultar la BD o tener los UUIDs hardcoded.
- El enum `CodigoRol` puede quedar desincronizado si se agregan roles directamente en BD sin actualizar el enum.

## Alternativas consideradas

- **Enum Java como fuente de verdad**: Descartado porque no permite modificación sin redeploy y complica la gestión de permisos por un administrador no técnico.
