# Estado de sesión — Conectividad Android corregida

**Fecha:** 2026-08-13
**Rama:** main
**Estado:** COMPLETADA Y CERRADA ✅ — commit f1a1b22

## Resumen de lo implementado

### Correcciones de conectividad Android (commit f1a1b22)

| Item | Resultado |
|------|-----------|
| `NetworkModule.kt` — URL corregida de `8080` a `8081` vía `BuildConfig.BASE_URL` | ✅ |
| `core/network/build.gradle.kts` — `buildConfig = true`, `BASE_URL` en `debug`/`release` | ✅ |
| `app/src/debug/res/xml/network_security_config.xml` — cleartext permitido para `10.0.2.2`, `localhost` | ✅ |
| `app/src/main/res/xml/network_security_config.xml` — release: sin cleartext | ✅ |
| `AndroidManifest.xml` — `android:networkSecurityConfig` declarado | ✅ |
| `LoginViewModel.kt` — `ConnectException` → `ERROR_SERVIDOR` (antes caía en `IOException` → `SIN_CONEXION`) | ✅ |
| `LoginViewModelTest.kt` — test nuevo: `servidor inaccesible devuelve estado Error ERROR_SERVIDOR` | ✅ |
| `local.properties.example` — eliminada referencia falsa a `api.base.url`; documentada fuente real | ✅ |
| `docs/operacion/DOCKER_LOCAL.md` — nueva sección de conectividad Android | ✅ |
| 165 pruebas JVM — BUILD SUCCESSFUL | ✅ |
| assembleDebug — BUILD SUCCESSFUL | ✅ |
| lintDebug — BUILD SUCCESSFUL | ✅ |

## Estado del repositorio

- Rama: main
- HEAD: f1a1b22
- Docker: 3 servicios healthy (postgres, api:8081, admin-web:8080)
- origin/main: aún en 2cf9340 — pendiente push (requiere autorización)

## Causa raíz del problema

Dos problemas combinados:
1. **Puerto incorrecto**: `NetworkModule.kt` usaba `8080` (admin-web) en lugar de `8081` (API)
2. **Cleartext bloqueado**: Desde API 28+, Android bloquea HTTP por defecto; sin `network_security_config.xml`, OkHttp lanzaba `IOException` que aparecía como "Sin conexión a Internet"

## Siguiente acción exacta

Para publicar en el repositorio remoto (requiere autorización explícita):
```bash
git push origin main
```

## Instrucciones de validación manual

1. Verificar Docker: `docker compose ps` → 3 servicios healthy
2. Verificar API: `curl http://localhost:8081/actuator/health`
3. Instalar APK debug en emulador (AVD) o usar Run desde Android Studio
4. Verificar desde Chrome del emulador: `http://10.0.2.2:8081/actuator/health` → `{"status":"UP"}`
5. Abrir la app → pantalla Login
6. Ingresar las credenciales definidas en `DEV_ADMIN_USERNAME` / `DEV_ADMIN_PASSWORD` del `.env`
7. Esperar respuesta — debe alcanzar estado `Autenticado`

## Próxima fase recomendada

**Fase 6B — Asignaciones diarias desde supervisión** (backend + Android)
- Asignar carteras a ejecutivos por mes
- Distribuir personas a ejecutivos diariamente
- Requiere UI en Admin Web y sincronización con app Android

**Alternativa: Fase 6C — Supervisión en app Android**
- Lista de ejecutivos a cargo del supervisor
- Estado de gestiones por ejecutivo en la app

## Deuda técnica registrada

- `ErrorTipo.DISPOSITIVO_REVOCADO` y `SESION_EXPIRADA` están definidos en el enum pero nunca se asignan en `LoginViewModel` ni `SessionRepository`. Son casos de manejo incompleto que se implementarán cuando el backend exponga esos flujos.
- `BuildConfig.BASE_URL` en release es cadena vacía `""`. Debe configurarse con la URL de producción antes de compilar un release.
