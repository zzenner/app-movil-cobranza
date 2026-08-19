# Deuda técnica

Este documento registra deuda técnica real identificada, con contexto suficiente para que se entienda su impacto y se decida cuándo abordarla.

## Deuda activa

### ~~DT-012 — Gestiones en `ERROR_PERMANENTE`/`CONFLICTO` bloquean el logout indefinidamente~~ → Resuelto

Ver **DT-R08** en la sección de deuda resuelta.

### DT-013 — Sin sincronización proactiva de gestiones al recuperar conectividad
**Área:** Android, `feature:gestion`.
**Descripción:** A diferencia de `feature:asignacion` (que expone un ícono manual de
"Sincronizar" en `AsignacionListScreen`), `GestionSyncScheduler` solo se dispara desde
`programarEnvioInmediato()` (al guardar una gestión nueva) o `programarPeriodico()` (cada 1
hora). No hay `ConnectivityManager`/`NetworkCallback` ni botón manual visible para forzar el
reenvío de gestiones pendientes. Una gestión offline puede quedar sin sincronizar hasta 1 hora
después de recuperar conexión, salvo que el usuario registre otra gestión mientras tanto.
**Impacto:** Bajo — no hay pérdida de datos, solo demora en la visibilidad de la gestión en el
backend/supervisión.
**Decisión recomendada:** Evaluar agregar un botón manual de "Sincronizar" en
`PersonaDetalleScreen`/`GestionHistorialScreen` equivalente al de asignación, y/o un
`NetworkCallback` que dispare `programarEnvioInmediato()` al recuperar conectividad.
**Referencia:** Detectado durante la ronda funcional de Gestiones (2026-08-18).

### DT-014 — `GestionFormScreen` no muestra nombre ni RUT de la persona al registrar una gestión
**Área:** Android, `app` (`GestionFormScreen.kt`), compartido por los orígenes `ASIGNACION_DIARIA` y `BUSQUEDA_DIRECTA`.
**Descripción:** Tras una búsqueda exitosa en "Buscar persona por RUT" (`feature:busqueda`), la
navegación abre directamente `Registrar gestión` sin ningún encabezado o card que confirme el
nombre/RUT de la persona encontrada — el título de la TopAppBar es genérico ("Registrar
gestión") y el resto del formulario (tipo de gestión, GPS, observaciones) no referencia a la
persona. Se verificó leyendo `GestionFormScreen.kt` completo: no existe ningún `Text` que
muestre `persona.nombre` ni RUT. El mismo formulario se usa para el origen `ASIGNACION_DIARIA`,
así que la carencia no es exclusiva de búsqueda directa.
**Impacto:** El ejecutivo no tiene una confirmación visual explícita de qué persona está
gestionando en este formulario específico, especialmente relevante en búsqueda directa donde el
usuario recién tecleó un RUT manualmente (mayor probabilidad de error de tipeo que en asignación,
donde la persona se elige de una lista con nombre visible). No se detectó ningún caso en que se
muestre el nombre equivocado — es una omisión de UI, no un error de datos.
**Decisión recomendada:** Agregar un encabezado o card en `GestionFormScreen` con nombre y RUT
formateado de la persona (dato ya disponible vía `personaId` → `PersonaDao`/`PersonaDirectaDao`
según origen). Bajo riesgo, cambio acotado a una pantalla.
**Referencia:** Detectado durante la ronda de validación de Búsqueda por RUT (2026-08-19).

### DT-011 — Test instrumentado de `core:security` no ejecuta por runner faltante en el classpath
**Área:** Android, entorno de pruebas (`core:security`).
**Descripción:** `SecureTokenStoreInstrumentedTest` (androidTest) falla al arrancar con
`ClassNotFoundException: androidx.test.runner.AndroidJUnitRunner` — `testInstrumentationRunner`
en `core/security/build.gradle.kts:13` apunta a la clase del artefacto legacy `androidx.test:runner`,
que no está declarado como dependencia `androidTestImplementation` del módulo (solo está
`androidx.test.ext:junit`, que provee `androidx.test.ext.junit.runners.AndroidJUnitRunner`, una
clase distinta). No relacionado con JDK/Robolectric — es un test instrumentado real (requiere
emulador/dispositivo), separado de los tests JVM unitarios.
**Impacto:** El único test instrumentado del proyecto (cifrado/descifrado AES-GCM del refresh
token vía Android Keystore) no puede ejecutarse ni en CI ni localmente contra un emulador.
**Decisión recomendada:** Agregar la dependencia `androidx.test:runner` a
`androidTestImplementation` en `core/security/build.gradle.kts`, o cambiar
`testInstrumentationRunner` a `androidx.test.ext.junit.runners.AndroidJUnitRunner` si esa es la
única clase disponible en el classpath del módulo.
**Referencia:** Detectado durante la ronda de estabilización del entorno Java (2026-08-16).

---

### DT-010 — AVD local con imagen de sistema experimental (16KB page size) rompe el renderizado de Compose
**Área:** Android, entorno de desarrollo local (no código de la app).
**Descripción:** El AVD `Medium_Phone` creado localmente usaba la imagen de sistema `system-images;android-37.1;google_apis_playstore_ps16k;x86_64` (imagen preview/experimental de Android 17 con page-size de 16KB). En ese AVD, **ningún** contenido de Jetpack Compose se pinta en pantalla (queda negro), aunque el árbol de composición se construye correctamente (confirmado con `uiautomator dump`) y los frames se entregan/intercambian en HWUI (confirmado con logs `Davey!`/`SwapBuffersCompleted`). Se demostró reemplazando toda la UI por un `Box` rojo de pantalla completa: seguía renderizando negro. Solo la barra de estado del sistema (vistas nativas, no-Compose) se veía. Se creó un AVD nuevo (`Cobranza_API36_Stable`, imagen estable `system-images;android-36;google_apis_playstore;x86_64`) y la misma APK renderizó correctamente (título "Cobranza", botón "Ingresar" visibles).
**Impacto:** Cualquier desarrollador que cree un AVD nuevo eligiendo por error una imagen de sistema "preview"/experimental (ej. variantes `_ps16k`, `_pgagnostic`, canary) puede reproducir una pantalla negra indistinguible de un bug real de la app, perdiendo tiempo de diagnóstico. No afecta builds de CI ni dispositivos físicos.
**Decisión recomendada:** Usar siempre imágenes de sistema **estables** (no preview/canary) para emuladores de desarrollo — p. ej. `system-images;android-36;google_apis_playstore;x86_64` (o la que corresponda a `targetSdk`). Documentar esto en el README de onboarding Android. No se requiere ningún cambio de código.
**Referencia:** Investigación de incidencia "pantalla negra + ANR en MainActivity" (2026-08-16).

---

### DT-IMX-001 — Cola de mensajes para workers de importación
**Área:** API, infraestructura.
**Descripción:** Los workers de validación y procesamiento (`@Async`) no tienen reintentos automáticos. Si el worker falla (OOM, error no controlado), el estado queda en VALIDANDO/PROCESANDO y el job de recuperación lo marca FALLIDA. El operador debe volver a subir el archivo.
**Impacto:** Experiencia del operador: un fallo transitorio en la infraestructura obliga a resubir el archivo. Sin observabilidad directa sobre las causas del fallo del worker.
**Decisión recomendada:** Migrar a RabbitMQ o Kafka con reintentos configurables cuando el volumen de importaciones lo justifique. Ver ADR-0050.

---

### ~~DT-IMX-002~~ — Tests Angular para módulo de importación ✅ RESUELTA (2026-08-10)
**Área:** Admin Web.
**Resolución:** Creados 4 spec files con 54 tests en total: `importacion.service.spec.ts` (9 tests, 100% cobertura), `importacion-list.component.spec.ts` (8 tests), `importacion-nueva.component.spec.ts` (15 tests), `importacion-detail.component.spec.ts` (22 tests). Cobertura global ≥80%.

---

### ~~DT-IMX-003~~ — Smoke tests de importación ✅ RESUELTA (2026-08-10)
**Área:** Scripts CI.
**Resolución:** Añadida sección 8 en `scripts/smoke-test.sh` con 21 escenarios de importación mensual. Ejecuta con skips graceful cuando no hay carteras activas (entorno sin seed). Verificado 49/49 OK en entorno Docker local.

---

### DT-007 — Depuración de refresh tokens consumidos
**Área:** API, DB.
**Descripción:** Los tokens `CONSUMIDO` se conservan en `refresh_tokens` para detección de reutilización, pero no existe proceso de purga. La tabla crecerá indefinidamente.
**Impacto:** Crecimiento de storage a largo plazo; consultas podrían degradarse sin índices de limpieza.
**Decisión recomendada:** Implementar job de depuración en Fase 3 (DELETE WHERE estado IN ('CONSUMIDO', 'REVOCADO') AND fecha_consumo/revocacion < now() - interval '90 days').

---

### DT-008 — Rotación de claves RSA sin plan operacional
**Área:** API, operaciones.
**Descripción:** No existe proceso definido para rotar el par RSA (vencimiento, compromiso). La rotación requiere actualizar variables de entorno y reiniciar el servicio, lo que invalida todos los access tokens activos.
**Impacto:** Seguridad operacional; sin rotación periódica aumenta el riesgo de explotación de clave comprometida.
**Decisión recomendada:** Definir política de rotación anual + procedimiento de emergencia antes del despliegue en producción.

---

### DT-009 — Revocación de sesión al revocar dispositivo no es en tiempo real
**Área:** API.
**Descripción:** Si se revoca un dispositivo desde el admin, la sesión activa de ese dispositivo sigue siendo válida hasta que el próximo refresh falle o el access token expire.
**Impacto:** Ventana de acceso post-revocación de hasta 15 minutos (access token) más el tiempo hasta el próximo refresh.
**Decisión recomendada:** En Fase 3, invalidar activamente sesiones al revocar dispositivo.

---

### ~~DT-001 — Mecanismo de autenticación offline por confirmar~~ → Resuelto

Ver **DT-R04** en la sección de deuda resuelta.

---

### DT-002 — Cifrado de base de datos local Android no evaluado
**Área:** Android.
**Descripción:** No se ha evaluado el impacto en rendimiento de SQLCipher u otra opción de cifrado para Room, ni la compatibilidad con las versiones de Android objetivo.
**Impacto:** Seguridad de datos en dispositivos no protegidos o perdidos.
**Decisión recomendada:** Evaluar en la Fase 3 antes de implementar Room. Dado que los teléfonos son corporativos, verificar si el cifrado del sistema operativo Android es suficiente.

---

### ~~DT-003 — Versión mínima de Android sin definir~~ → Resuelta provisionalmente

Ver **DT-R05** en la sección de deuda resuelta.

---

### DT-004 — Almacenamiento de fotografías sin diseño
**Área:** Android, API.
**Descripción:** El sistema contempla fotografías en gestiones, pero no se ha definido la solución de almacenamiento (S3 propio, MinIO, otro), el proceso de subida desde Android ni el tamaño máximo por imagen.
**Impacto:** Afecta el contrato OpenAPI, el módulo de sincronización Android y la infraestructura de producción.
**Decisión recomendada:** Diseñar antes de implementar el registro de gestiones en la Fase 3. El sistema se diseña para ser compatible con S3, pero no se implementa en Fase 1.

---

### DT-005 — Número máximo de reintentos de sincronización sin definir
**Área:** Android (WorkManager).
**Descripción:** No se ha definido el umbral de reintentos antes de marcar una gestión como `ERROR_PERMANENTE` ni los parámetros exactos del backoff exponencial (intervalo inicial, factor, máximo).
**Impacto:** Afecta cuánto tiempo puede quedar una gestión en estado `ERROR_REINTENTABLE` y la experiencia del ejecutivo en zonas de conectividad intermitente.
**Decisión recomendada:** Definir empíricamente en la Fase 3 mediante pruebas con conectividad real. Valores iniciales razonables: 3 reintentos, backoff de 30s–1min–2min.

---

### DT-006 — Integración con sistema externo pendiente de diseño
**Área:** API, importación.
**Descripción:** La asignación mensual se cargará inicialmente mediante CSV, pero la integración definitiva es mediante API del sistema corporativo externo. El contrato de esa API no está disponible todavía.
**Impacto:** El módulo de importación debe diseñarse para soportar ambos mecanismos. La migración de CSV a API es un cambio significativo.
**Decisión recomendada:** Diseñar el módulo de importación con una abstracción que permita cambiar la fuente (CSV, API externa) sin reescribir la lógica de carga y validación.

---

### ~~DT-007 — Docker Desktop no integrado con la distro WSL2 de desarrollo~~ → Resuelto

Ver **DT-R06** en la sección de deuda resuelta.

---

## Deuda resuelta

### DT-R08 — Gestiones en `ERROR_PERMANENTE`/`CONFLICTO` bloqueaban el logout indefinidamente (resuelto 2026-08-19)
**Descripción original:** `GestionLocalDao.getElegibles()` solo recoge `PENDIENTE_ENVIO`/
`ERROR_REINTENTABLE` — una gestión en `ERROR_PERMANENTE` o `CONFLICTO` nunca vuelve a intentarse
sola, pero `HomeViewModel.solicitarLogout()` bloqueaba el logout mientras
`contarNoResueltas() > 0` (cualquier estado != `SINCRONIZADA`), sin distinguir "el worker todavía
puede enviarla" de "nadie la va a enviar sola". Una sola gestión en esos estados terminales dejaba
al ejecutivo sin forma de cerrar sesión en ese dispositivo desde la UI.

**Hallazgo adicional durante la corrección:** `BundleReplacementTransaction.limpiarTodo()`
(invocado en cada logout) borraba `gestion_local` de forma **incondicional**
(`DELETE FROM gestion_local` sin filtro), incluyendo registros no sincronizados — un test
existente (`LogoutIntegrationTest`) documentaba literalmente ese comportamiento. Esto nunca se
manifestaba en producción porque el logout jamás se alcanzaba con pendientes reales, pero
representaba una violación latente de RN-24 ("no se eliminan silenciosamente gestiones... bajo
ninguna circunstancia") que quedaría expuesta en cuanto se permitiera salir con gestiones
permanentes sin resolver. Se corrigió como parte necesaria de este mismo fix.

**Resolución:**
- `GestionLocalDao`/`GestionRepository` — nuevos `contarReintentables()` (PENDIENTE_ENVIO +
  ERROR_REINTENTABLE) y `contarNoRecuperables()` (ERROR_PERMANENTE + CONFLICTO), separando lo
  que el propio código anterior de `getElegibles()` ya distinguía implícitamente pero
  `contarNoResueltas()` no exponía.
- `HomeViewModel` — `solicitarLogout()`/`sincronizarYLogout()` verifican primero reintentables
  (comportamiento RN-24 sin cambios: bloquea, ofrece sincronizar) y solo si ese conteo llega a 0
  revisan no-recuperables. Si hay no-recuperables, nuevo estado
  `ConfirmarLogoutConNoRecuperables` — requiere una acción explícita del usuario
  (`confirmarLogoutConNoRecuperables()`), no cierra sesión automáticamente.
- `HomeScreen` — nuevo diálogo en lenguaje simple ("Hay gestiones que no se pudieron enviar...
  avise a su supervisor... cerrar sesión no significa que esos datos ya fueron enviados"), sin
  exponer `ERROR_PERMANENTE` ni otros tecnicismos al usuario.
- `GestionLocalDao.deleteSincronizadas()` (nuevo) + `BundleReplacementTransaction.limpiarTodo()`
  ahora solo borra `gestion_local` con `estadoSincronizacion = 'SINCRONIZADA'` — cualquier
  gestión no sincronizada (pendiente, en reintento o con error permanente) sobrevive al logout,
  con su código y mensaje de error intactos para trazabilidad.
- Verificado end-to-end: gestión forzada a `ERROR_PERMANENTE` (manipulación controlada de Room,
  sin tocar backend) → diálogo correcto → "Cerrar sesión de todas formas" → logout exitoso → la
  gestión permanece íntegra en Room tras el logout (`asignacion_diaria`/`persona` sí se limpian,
  como corresponde a caché re-descargable) → relogin sin crash ni ANR.
- Tests: `HomeViewModelTest` (nuevo, 8 casos incluida la combinación reintentable+permanente) y
  `LogoutIntegrationTest` actualizado (la aserción que documentaba el borrado incondicional se
  corrigió para reflejar la retención correcta).
**Referencia:** `.claude/SESSION_HANDOFF.md`, RN-24.

### DT-R07 — Gradle/tests JVM ejecutándose bajo JDK incorrecto (JDK 25) rompía Robolectric (resuelto 2026-08-16)
**Descripción:** El proyecto fija `sourceCompatibility`/`targetCompatibility = VERSION_17` en todos
los módulos, pero no declaraba ningún `kotlin { jvmToolchain(17) }` que forzara el JDK real del
daemon de Gradle. En Windows, sin `JAVA_HOME` explícito, Gradle terminaba ejecutándose bajo el
JBR de Android Studio (JDK 25.0.2), causando que **todos** los tests Robolectric de
`core:database` (39/39) y parte de `feature:gestion` (4/27) fallaran con
`java.lang.IllegalArgumentException: Unsupported class file major version 69` en
`org.objectweb.asm.ClassReader` — el ASM embebido en Robolectric 4.14.1 no soporta parsear
bytecode de JDK 25 al instrumentar clases (`InstrumentingClassWriter.getCommonSuperClass`). Los
tests JVM puros sin Robolectric (feature:auth, feature:asignacion) no se veían afectados.
**Resolución:** Se estandarizó JDK 17 (Microsoft Build of OpenJDK 17.0.20+8 LTS) como JDK del
proyecto para Windows. Instalado vía `winget install Microsoft.OpenJDK.17`. `JAVA_HOME`
configurado a nivel de Usuario de Windows (`[Environment]::SetEnvironmentVariable`, scope
`User`), persistente para nuevas terminales/sesiones. Confirmado que con JDK 17 los 39 tests de
`core:database` y los 27 de `feature:gestion` pasan sin modificar Robolectric, ASM, AGP, Gradle
Wrapper ni ninguna otra dependencia — la causa era puramente el JDK de ejecución, no una
incompatibilidad de versiones de librerías.
**Referencia:** `.claude/SESSION_HANDOFF.md` (sección "Entorno Java del proyecto").

### DT-R04 — Mecanismo de reapertura de la app estando offline (resuelto 2026-07-26)
**Descripción:** No se usa PIN local ni biometría dentro de la app en el MVP. La sesión local Android persiste hasta que el usuario ejecute logout explícitamente. La seguridad física del dispositivo la gestiona el SO del teléfono corporativo (bloqueo de pantalla del sistema).
**Referencia:** RN-24, RF-01b, RF-01d.

### DT-R05 — Versión mínima de Android (provisionalmente resuelta 2026-07-26)
**Descripción:** Se establece `minSdk = API 29` (Android 10) como valor provisional. La decisión definitiva depende del inventario de dispositivos corporativos. No se crea el proyecto Android hasta confirmar con el inventario.
**Referencia:** ADR-0011, RN-27, STATUS.md P-07.

### DT-R06 — Docker Desktop integrado con WSL2 (resuelto 2026-07-27)
**Descripción:** Docker Desktop 4.60.0 con integración WSL2 habilitada. Docker Compose v5.0.2 disponible. PostgreSQL/PostGIS levantado correctamente con `./scripts/start.sh`.
**Referencia:** ADR-0017, Fase 1B.

### DT-R01 — Formato de RUT (resuelto 2026-07-26)
**Descripción:** Se almacena como `rut_numero` + `rut_dv` en dos columnas separadas.
**Referencia:** ADR-0007.

### DT-R02 — Origen de datos de personas y créditos (resuelto 2026-07-26)
**Descripción:** La plataforma consume datos del sistema externo; no los gestiona directamente. La carga inicial es por CSV; luego se integrará por API.
**Referencia:** RN-08, DT-006 (deuda residual de integración).

### DT-R03 — Catálogo de tipos de gestión (resuelto 2026-07-26)
**Descripción:** `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`. Pueden añadirse más en el futuro.
**Referencia:** RN-11.
