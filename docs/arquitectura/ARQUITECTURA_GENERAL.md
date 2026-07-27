# Arquitectura general

## Estilo arquitectónico

El sistema sigue un estilo de **monolito modular con cliente móvil offline-first**:

- La API es un único proceso Spring Boot estructurado con Spring Modulith, donde los módulos tienen fronteras explícitas de dominio pero comparten despliegue y base de datos.
- No se usan microservicios para evitar la complejidad operacional innecesaria en esta etapa.
- La app Android opera como cliente offline-first: la fuente de verdad local es Room/SQLite, y la sincronización con la API ocurre en segundo plano.

## Componentes principales

### API (`apps/api/`)
- Java 21, Spring Boot 3.x, Spring Modulith.
- Autenticación: JWT o sesiones (PENDIENTE de definir mecanismo exacto).
- Contrato: OpenAPI 3 (`contracts/openapi/`).
- Migraciones de esquema: Flyway.
- Base de datos: PostgreSQL + PostGIS.

**Módulos internos previstos (preliminares):**
- `autenticacion` — login, tokens, sesiones.
- `usuarios` — gestión de cuentas y roles.
- `carteras` — creación y gestión de carteras.
- `asignaciones` — asignación cobrador-cartera.
- `personas` — datos de titulares de créditos.
- `creditos` — créditos y cuotas.
- `gestiones` — recepción y persistencia de gestiones desde Android.

### App Android (`apps/mobile-android/`)
- Kotlin, Jetpack Compose, Room, WorkManager.
- Arquitectura interna: Clean Architecture con capas (datos, dominio, presentación).
- Fuente local: Room (SQLite) — autoridad para la interfaz en modo offline.
- Sincronización: WorkManager con backoff exponencial.
- Patrón outbox para gestiones creadas sin conexión.

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
