# Seguridad — Modelo base de usuarios

Este documento describe el modelo físico de usuarios, roles, permisos y dispositivos implementado en la Fase 1C.

## Módulos involucrados

| Módulo          | Responsabilidad                                                        |
|-----------------|------------------------------------------------------------------------|
| `usuarios`      | Entidad `Usuario`, roles, permisos, supervisión. Expone `UsuarioConsultaApi`. |
| `dispositivos`  | Registro y revocación de dispositivos Android. Depende de `usuarios::api`. |
| `autenticacion` | (Fase 2) Login, JWT, refresh tokens. Dependerá de `usuarios::api`.    |

Ver [ADR-0021](../adr/0021-separacion-usuarios-autenticacion.md) para la justificación de esta separación.

## Tablas en `cobranza` schema

```
roles                 — Roles del sistema (4 iniciales cargados por V003)
permisos              — Permisos granulares (7 iniciales cargados por V003)
usuarios              — Cuentas de acceso con hash BCrypt
usuario_roles         — Asignación histórica de roles a usuarios
rol_permisos          — Matriz rol → permisos (composite PK)
dispositivos          — Dispositivos Android registrados
supervision_usuarios  — Relación supervisor → ejecutivo
```

## Roles y permisos iniciales

| Rol                  | UUID estable                              | Permisos                                                                                       |
|----------------------|-------------------------------------------|------------------------------------------------------------------------------------------------|
| JEFE_SUPERVISORES    | `a1b2c3d4-0001-0001-0001-000000000001`   | Todos (7)                                                                                      |
| TECNOLOGIA           | `a1b2c3d4-0001-0001-0001-000000000002`   | USUARIOS_VER, USUARIOS_ADMINISTRAR, SINCRONIZACION_VER                                        |
| SUPERVISOR           | `a1b2c3d4-0001-0001-0001-000000000003`   | ASIGNACIONES_VER, GESTIONES_VER, SINCRONIZACION_VER                                           |
| EJECUTIVO_TERRENO    | `a1b2c3d4-0001-0001-0001-000000000004`   | GESTIONES_CREAR                                                                                |

Ver [ADR-0018](../adr/0018-roles-permisos-en-tablas.md) para la decisión de usar tablas en lugar de enums Java.

## Invariantes de negocio

### Usuarios
- `nombre_usuario` normalizado: `lower(btrim(valor))` — verificado por CHECK en BD y por `UsuarioService` antes de persistir.
- `correo` normalizado si no es nulo: misma regla.
- Contraseña almacenada como hash BCrypt (costo 12). Ver [ADR-0020](../adr/0020-bcrypt-contrasenas.md).
- `@Version` en `usuarios` y `dispositivos` para detección de escrituras concurrentes (optimistic locking).

### Roles
- Un usuario puede tener como máximo **un rol activo** por tipo de rol (índice parcial `WHERE activo = TRUE`).
- El historial de asignaciones se conserva (`activo = FALSE`, `fecha_termino` no nula).
- El enum `CodigoRol` en Java no es la fuente de verdad — es solo un auxiliar para validaciones en código.

### Supervisión
- Cada ejecutivo puede tener **un único supervisor activo** (índice parcial `WHERE activo = TRUE` en `ejecutivo_id`).
- Al finalizar, `fecha_termino` es obligatoria y debe ser >= `fecha_inicio`.
- Un usuario no puede supervisarse a sí mismo (CHECK `supervisor_id != ejecutivo_id`).

### Dispositivos
- `identificador_instalacion` es un UUID generado por la app Android en la primera instalación. Ver [ADR-0019](../adr/0019-identificador-instalacion-uuid.md).
- Revocación coherente: `revocado=TRUE` implica `activo=FALSE` y `fecha_revocacion IS NOT NULL` (enforced por CHECK y por `Dispositivo.revocar()`).
- `Dispositivo` no tiene `@ManyToOne` a `Usuario` — usa `UUID usuarioId` para respetar el límite de módulo.

## Límites de módulo (Spring Modulith)

```
dispositivos  →  puede usar   usuarios::api  (UsuarioConsultaApi)
dispositivos  →  NO puede usar  usuarios.dominio, usuarios.infraestructura, usuarios.aplicacion
autenticacion →  puede usar   usuarios::api  (configuración futura)
```

El `package-info.java` de `dispositivos` declara `allowedDependencies = "usuarios::api"`.
La verificación automática se ejecuta con `ApplicationModules.of(CobranzaApplication.class).verify()` en `ModularidadTest`.

## Seguridad operacional

- El hash de contraseña **nunca** aparece en `toString()`, logs, respuestas HTTP ni mensajes de error.
- No hay secretos, tokens ni claves en ningún archivo del repositorio.
- El actuator no expone variables de entorno, beans ni información sensible (ver `application.yml`).
- BCrypt con costo 12 en producción; costo 4 solo en pruebas de integración.
