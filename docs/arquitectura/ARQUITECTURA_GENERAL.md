# Arquitectura general

## Estilo arquitectónico

El sistema sigue un estilo de **monolito modular con cliente móvil offline-first**:

- La API es un único proceso Spring Boot estructurado con Spring Modulith, donde los módulos tienen fronteras explícitas de dominio pero comparten despliegue y base de datos.
- No se usan microservicios para evitar la complejidad operacional innecesaria en esta etapa.
- La app Android opera como cliente offline-first: la fuente de verdad local es Room/SQLite, y la sincronización con la API ocurre en segundo plano.

## Componentes principales

### API (`apps/api/`)
- Java 21, Spring Boot 3.5.16, Spring Modulith 1.4.12.
- Autenticación: JWT RS256 (access token 15 min) + refresh tokens opacos con rotación (ADR-0022, ADR-0023).
- Contrato: OpenAPI 3.1 en `contracts/openapi/cobranza-api.yaml` (v0.9.0).
- Migraciones de esquema: Flyway (V001–V010).
- Base de datos: PostgreSQL 16 + PostGIS 3.4.

**Módulos implementados (Fases 1B–3D):**
- `autenticacion` — login con auto-registro de dispositivo (ADR-0031), tokens JWT, sesiones.
- `usuarios` — usuarios, roles, permisos, supervisión (BCrypt).
- `dispositivos` — registro y revocación de dispositivos Android.
- `carteras` — carteras y relación N:M con personas (historial).
- `asignaciones` — asignaciones mensuales y diarias con estados y máquina de transición.
- `personas` — copia operacional: personas, avales, direcciones.
- `operaciones` — copia operacional: créditos y cuotas.
- `gestiones` — recepción idempotente con `INSERT ... ON CONFLICT`.
- `sincronizacion` — bundle de descarga para Android (8 queries IN, sin N+1).

### App Android (`apps/mobile-android/`)
- Kotlin 2.4.10 + Jetpack Compose (BOM 2026.06.01) + Hilt 2.60.1.
- Arquitectura multi-módulo: `:app`, `:core:network`, `:core:security`, `:feature:auth`.
- Almacenamiento seguro: refresh token cifrado con AES-256-GCM vía Android Keystore; datos de sesión en Preferences DataStore.
- Red: Retrofit 3.0.0 + OkHttp 5.4.0; cliente público y autenticado separados; single-flight refresh con Mutex.
- Backup deshabilitado: `allowBackup=false` + `data_extraction_rules.xml`.
- **Fases futuras:** Room (offline-first), WorkManager (sincronización), pantallas de cartera y gestiones.

### Administración web (`apps/admin-web/`)
- Angular con componentes standalone.
- Consume la API REST. No tiene base de datos propia.
- Sin soporte offline.

### Base de datos
- PostgreSQL 16 con PostGIS 3.4.
- Esquemas lógicos: `cobranza`, `auditoria`.
- Migraciones versionadas con Flyway (solo en la API).

### Infraestructura local y producción
- Docker Compose para entorno local (solo PostgreSQL en Fase 0).
- Producción futura: VPS Ubuntu, Docker Compose, Nginx como proxy inverso.
- Almacenamiento de archivos: diseñado para ser compatible con S3, no implementado en Fase 1.

## Comunicación entre componentes

```
App Android  <-->  API REST (HTTPS/JSON)  <-->  PostgreSQL
Admin Web    <-->  API REST (HTTPS/JSON)
```

- La app Android se comunica únicamente con la API REST.
- La administración web se comunica únicamente con la API REST.
- No hay comunicación directa entre Android y la base de datos.
- No hay comunicación directa entre la web admin y la base de datos.

## Principios de diseño

- **Offline first:** la app Android funciona correctamente sin conexión.
- **API como autoridad:** los datos financieros son autoritativos solo en la API.
- **Modularidad explícita:** los módulos tienen interfaces claras aunque estén en el mismo proceso.
- **Simplicidad operacional:** un solo `compose.yaml` para producción inicial.
- **Trazabilidad:** todas las operaciones significativas se auditan.

## Ver también

- [`docs/arquitectura/MODULOS.md`](MODULOS.md) — detalle de módulos de la API.
- [`docs/arquitectura/DIAGRAMA_CONTENEDORES.md`](DIAGRAMA_CONTENEDORES.md) — diagrama C4 de contenedores.
- [`docs/adr/`](../adr/) — decisiones arquitectónicas registradas.
