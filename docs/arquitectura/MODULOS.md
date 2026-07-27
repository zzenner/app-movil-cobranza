# Módulos del sistema

## Módulos de la API (Spring Modulith)

Los módulos son unidades de negocio con fronteras explícitas. Cada módulo expone solo lo necesario hacia los demás.

| Módulo           | Responsabilidad                                                    | Estado       |
|------------------|--------------------------------------------------------------------|--------------|
| `autenticacion`  | Login, emisión y validación de tokens, control de sesiones.        | PENDIENTE    |
| `usuarios`       | Gestión de cuentas de usuario, roles y credenciales.               | PENDIENTE    |
| `carteras`       | Creación y gestión de carteras de cobranza.                        | PENDIENTE    |
| `asignaciones`   | Asignación de carteras a cobradores con fechas de vigencia.        | PENDIENTE    |
| `personas`       | Datos de personas (titulares de créditos). Solo lectura en Fase 1. | PENDIENTE    |
| `creditos`       | Créditos y cuotas asociados a personas.                            | PENDIENTE    |
| `gestiones`      | Recepción, persistencia y consulta de gestiones registradas.       | PENDIENTE    |
| `sincronizacion` | Exposición de endpoints de sincronización para Android.            | PENDIENTE    |
| `auditoria`      | Registro transversal de operaciones (si se implementa como módulo).| PENDIENTE    |

### Dependencias entre módulos (preliminares)

```
autenticacion <-- usuarios
asignaciones  --> carteras, usuarios
gestiones     --> asignaciones, personas, creditos
sincronizacion --> gestiones, carteras, personas, creditos
```

**PENDIENTE:** Validar este grafo de dependencias al iniciar la Fase 1 y ajustarlo para cumplir con la separación de módulos que impone Spring Modulith.

## Módulos de la app Android

| Módulo / feature     | Responsabilidad                                                   |
|----------------------|-------------------------------------------------------------------|
| `autenticacion`      | Pantallas de login y gestión de tokens locales.                   |
| `cartera`            | Pantallas de lista de personas y detalle de créditos/cuotas.      |
| `gestiones`          | Pantallas de registro y listado de gestiones.                     |
| `sincronizacion`     | WorkManager, cola outbox, lógica de sincronización.               |
| `core:network`       | Cliente HTTP, interceptores, manejo de conectividad.              |
| `core:database`      | Room, DAOs, migraciones de Room.                                  |
| `core:ui`            | Componentes Compose reutilizables, temas, estilos.                |

## Módulos de la web Angular

| Módulo / feature   | Responsabilidad                                                    |
|--------------------|--------------------------------------------------------------------|
| `auth`             | Login y gestión de sesión en el navegador.                         |
| `usuarios`         | Pantallas de gestión de usuarios.                                  |
| `carteras`         | Pantallas de gestión de carteras y asignaciones.                   |
| `gestiones`        | Pantallas de visualización de gestiones registradas.               |
| `shared`           | Componentes UI reutilizables, guards, interceptores HTTP.          |

## PENDIENTE

- Confirmar si `auditoria` se implementa como módulo Spring Modulith independiente o como aspecto transversal.
- Definir el módulo que gestiona el almacenamiento de fotografías (S3 compatible).
- Validar si `sincronizacion` es un módulo separado o responsabilidad interna de `gestiones`.
