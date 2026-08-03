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
| `sincronizacion` | Bundle de descarga y búsqueda directa por RUT para dispositivos Android.  | Implementado (Fases 3D + 4C-B ✅) |
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

### Implementados

| Módulo / feature        | Responsabilidad                                                                    | Estado               |
|-------------------------|------------------------------------------------------------------------------------|----------------------|
| `:app`                  | Actividad principal, Hilt, NavHost completo, LogoutUseCase, CobranzaApp.           | Implementado (4B ✅) |
| `:core:network`         | Cliente HTTP público y autenticado, single-flight refresh, DTOs de sincronización. | Implementado (4B ✅) |
| `:core:security`        | Keystore AES-256-GCM para refresh token, DataStore para sesión.                    | Implementado (4A ✅) |
| `:core:database`        | Room v3: 11 entidades (incluye `gestion_local`, `persona_directa`), 10 DAOs, BundleReplacementTransaction. `exportSchema=true`; esquemas `1.json`, `2.json`, `3.json`. Migraciones 1→2, 2→3 explícitas. Sin `fallbackToDestructiveMigration`. | Implementado (4C-B ✅) |
| `:feature:auth`         | LoginViewModel, pantallas Check/Login, SessionRepository (@Singleton), AuthModule. | Implementado (4B ✅) |
| `:feature:asignacion`   | AsignacionRepository, DescargaAsignacionWorker, AsignacionViewModel, pantallas.    | Implementado (4B ✅) |
| `:feature:gestion`      | GestionRepository (outbox + Mutex), EnvioGestionWorker (@HiltWorker), GestionSyncScheduler, LocationProvider (LocationManager), GestionFormScreen, GestionHistorialScreen. `GestionForm.origenGestion` explícito. | Implementado (4C-B ✅) |
| `:feature:busqueda`     | RutValidator (Módulo 11), BusquedaDirectaRepository (snapshot en persona_directa), BusquedaDirectaViewModel, BusquedaDirectaScreen. Depende solo de `:core:database` y `:core:network`. | Implementado (4C-B ✅) |

### Grafo de dependencias Gradle

```
:app --> :feature:auth, :feature:asignacion, :feature:gestion, :feature:busqueda, :core:database, :core:network, :core:security
:feature:asignacion --> :core:database, :core:network
:feature:gestion    --> :core:database, :core:network
:feature:busqueda   --> :core:database, :core:network
:feature:auth       --> :core:network, :core:security
:core:database      --> (solo Room, Hilt, Coroutines)
:core:network       --> (solo Retrofit, OkHttp, Hilt, kotlinx.serialization)
:core:security      --> (solo DataStore, Hilt, Coroutines)
```

Los módulos `:feature:*` no dependen entre sí. `:app` es el único que conoce todos los features.

### Fases futuras (pendiente)

| Módulo / feature       | Responsabilidad                                                    |
|------------------------|--------------------------------------------------------------------|
| `:core:ui`             | Componentes Compose reutilizables, temas, estilos.                 |

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
