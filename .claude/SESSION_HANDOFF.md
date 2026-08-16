# SESSION_HANDOFF — App Móvil Cobranza

**Última actualización:** 2026-08-16 04:15 (sesión actual)
**Rama:** main
**Commit HEAD:** 8eb4cd9 (chore(dev): preparar escenario demo para android)

## Incidencia investigada 2026-08-16 — Pantalla negra + ANR en MainActivity (Android)

**Síntoma:** Al ejecutar la app Android desde Android Studio, quedaba en pantalla negra con ANR "Input dispatching timed out (Application does not have a focused window)".

**Causa raíz:** NO es un bug de la app. El AVD local `Medium_Phone` usaba la imagen de sistema experimental `android-37.1;google_apis_playstore_ps16k;x86_64` (preview de 16KB page-size). En esa imagen, ningún contenido de Compose se pinta (demostrado con un `Box` rojo de pantalla completa — seguía negro), aunque el árbol de composición y los frames se generan correctamente. Ver detalle completo en `docs/gestion/DEUDA_TECNICA.md` (DT-010).

**No relacionado con:** sesión/auth, threading (no se encontró ningún `runBlocking`/bloqueo en el main thread), navegación, Room, WorkManager, ni con la corrección previa de conectividad `10.0.2.2:8081` (esa sigue vigente y correcta).

**Acción tomada:** Se instaló `cmdline-tools`, se descargó la imagen estable `android-36;google_apis_playstore;x86_64`, y se creó el AVD `Cobranza_API36_Stable`. La misma APK renderiza correctamente ahí (verificado con screenshot: título "Cobranza" y botón "Ingresar" visibles).

**Sin cambios de código** — no hay commit asociado a esta incidencia. El único diff pendiente en el árbol (`gradle.properties`, `org.gradle.tooling.parallel=true`) es preexistente y no relacionado; no se tocó.

**Siguiente acción para quien continúe:** Usar el AVD `Cobranza_API36_Stable` (o cualquier imagen NO preview) para desarrollo/pruebas Android. El AVD `Medium_Phone` (ps16k) puede eliminarse o dejarse solo para pruebas específicas de compatibilidad 16KB.

---

## Estado del entorno DEV

- Docker: API + PostgreSQL corriendo en localhost:8080
- Seed completado: 1 admin, 3 supervisores demo, 18 ejecutivos demo, supervisión asignada
- Escenario asignación demo: 1 diaria PUBLICADA para ej_demo_133 / 2026-08-13 / 5 personas

## Cambios en esta sesión (pendientes de commit)

### Archivos nuevos
- `apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/api/DemoAsignacionSeedApi.java`
- `apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/aplicacion/DemoAsignacionSeedService.java`
- `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/api/SupervisionSeedApi.java`
- `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/SupervisionSeedService.java`

### Archivos modificados
- `DevSeedRunner.java` — agrega DemoAsignacionSeedApi, captura ejDemo133Id, llama prepararEscenarioDemo
- `DevSeedRunnerTest.java` — mock DemoAsignacionSeedApi, test nuevo prepara_escenario_asignacion_demo
- `PersonaConsultaApi.java` — agrega findIdsByCarteraIdActiva(UUID, int)
- `PersonaConsultaApiImpl.java` — implementa findIdsByCarteraIdActiva
- `UsuarioSeedApi.java` — agrega findIdByNombreUsuario(String)
- `UsuarioSeedService.java` — implementa findIdByNombreUsuario
- `scripts/smoke-test.sh` — paso 7.16 usa ?tamanio=50

## Resultados de validación

- `mvn test`: 494/494 OK, BUILD SUCCESS
- Smoke tests: 79/79 OK
- Endpoint Android /api/v1/asignaciones/diaria/activa: 200 con 5 personas para ej_demo_133
- No hay PII, CSV ni secretos en ningún archivo rastreado

## Siguiente acción exacta

Commit pendiente con mensaje:
  "chore(dev): preparar datos y entorno demo para pruebas end-to-end"

Archivos a incluir:
- apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/api/DemoAsignacionSeedApi.java
- apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/aplicacion/DemoAsignacionSeedService.java
- apps/api/src/main/java/cl/zzenner/cobranza/usuarios/api/SupervisionSeedApi.java
- apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/SupervisionSeedService.java
- apps/api/src/main/java/cl/zzenner/cobranza/DevSeedRunner.java
- apps/api/src/main/java/cl/zzenner/cobranza/personas/api/PersonaConsultaApi.java
- apps/api/src/main/java/cl/zzenner/cobranza/personas/infraestructura/PersonaConsultaApiImpl.java
- apps/api/src/main/java/cl/zzenner/cobranza/usuarios/api/UsuarioSeedApi.java
- apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/UsuarioSeedService.java
- apps/api/src/test/java/cl/zzenner/cobranza/DevSeedRunnerTest.java
- scripts/smoke-test.sh
- .claude/SESSION_HANDOFF.md

Pendiente después del commit:
- Actualizar STATUS.md y CHANGELOG.md
- Solicitar autorización para push a origin/main
- Validación visual del Admin Web (opcional si hay emulador disponible)
