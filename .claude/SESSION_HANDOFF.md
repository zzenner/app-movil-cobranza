# SESSION_HANDOFF — App Móvil Cobranza

**Última actualización:** 2026-08-13 23:20 (sesión actual)
**Rama:** main
**Commit HEAD:** ec63cfc (chore(fase-6b): validaciones finales y cierre de fase 6B)

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
