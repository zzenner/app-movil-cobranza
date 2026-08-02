# Módulos del sistema

## Módulos de la API (Spring Modulith)

Los módulos son unidades de negocio con fronteras explícitas. Cada módulo expone solo lo necesario hacia los demás.
Paquete raíz: `cl.zzenner.cobranza`. Los módulos son sub-paquetes directos detectados automáticamente por Spring Modulith.

| Módulo           | Responsabilidad                                                          | Estado                      |
|------------------|--------------------------------------------------------------------------|-----------------------------|
| `autenticacion`  | Login, emisión y renovación de tokens JWT RS256, logout, sesiones.       | Implementado (Fase 2 ✅)    |
| `usuarios`       | Gestión de usuarios, roles y relaciones de supervisión.                  | Implementado (Fase 1C ✅)   |
| `dispositivos`   | Registro, activación y revocación de dispositivos Android.               | Implementado (Fase 1C ✅)   |
| `carteras`       | Carteras de cobranza y relación N:M con personas (historial).            | Implementado (Fase 3A ✅)   |
| `asignaciones`   | Asignaciones mensuales y diarias de personas a ejecutivos.               | Implementado (Fase 3B ✅)   |
| `personas`       | Copia operacional de personas, avales, direcciones y observaciones.      | Implementado (Fase 3A ✅)   |
| `operaciones`    | Copia operacional de créditos y cuotas asociados a personas.             | Implementado (Fase 3A ✅)   |
| `gestiones`      | Recepción idempotente, persistencia y consulta de gestiones de terreno.  | Implementado (Fase 3C ✅)   |
| `sincronizacion` | Bundle de descarga completo para dispositivos Android (Fase 3D).         | Implementado (Fase 3D ✅)   |
| `auditoria`      | Trazabilidad de operaciones críticas. Escribe en esquema `auditoria`.    | Stub (pendiente)            |
| `compartido`     | Utilidades transversales sin lógica de dominio. No depende de módulos.   | Stub (pendiente)            |

### Dependencias conceptuales entre módulos (Fase 2+)

```
autenticacion --> usuarios, dispositivos
asignaciones  --> personas, usuarios
operaciones   --> personas
gestiones     --> personas, usuarios, dispositivos
sincronizacion --> asignaciones, gestiones, dispositivos
auditoria     --> (consume eventos publicados, no importa módulos)
compartido    --> (sin dependencias de dominio)
```

> Nota: El módulo `creditos` fue renombrado a `operaciones` para alinearse con el dominio confirmado en la Fase 1A (crédito = operación en el vocabulario del sistema externo).

## Módulos de la app Android

### Fase 4A — Base implementada

| Módulo / feature     | Responsabilidad                                                   | Estado               |
|----------------------|-------------------------------------------------------------------|----------------------|
| `:app`               | Actividad principal, Hilt, grafo de navegación raíz.             | Implementado (4A ✅) |
| `:core:network`      | Cliente HTTP público y autenticado, single-flight refresh.        | Implementado (4A ✅) |
| `:core:security`     | Keystore AES-256-GCM para refresh token, DataStore para sesión.   | Implementado (4A ✅) |
| `:feature:auth`      | LoginViewModel, pantallas Check/Login/Home, SessionRepository.    | Implementado (4A ✅) |

### Fases futuras (pendiente)

| Módulo / feature     | Responsabilidad                                                   |
|----------------------|-------------------------------------------------------------------|
| `feature:cartera`    | Pantallas de lista de personas y detalle de créditos/cuotas.      |
| `feature:gestiones`  | Pantallas de registro y listado de gestiones.                     |
| `feature:sincronizacion` | WorkManager, cola outbox, lógica de sincronización.           |
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
