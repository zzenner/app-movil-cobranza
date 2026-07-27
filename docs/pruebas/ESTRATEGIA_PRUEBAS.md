# Estrategia de pruebas

## Objetivo

Garantizar la calidad y corrección del sistema en todas sus capas, con especial énfasis en la confiabilidad de la sincronización offline y la integridad de los datos.

## Niveles de prueba

### 1. Pruebas unitarias

**Dónde aplica:** API (Java), App Android (Kotlin), Admin Web (TypeScript).

- Prueban una unidad de lógica de forma aislada (clase, función, use case).
- No requieren base de datos ni red.
- Herramientas: JUnit 5 + Mockito (API), JUnit 4/5 + Mockk (Android), Jasmine + Karma (Angular).
- Deben ejecutarse en cada build local.

### 2. Pruebas de integración

**Dónde aplica:** API.

- Prueban la interacción entre módulos y con la base de datos real.
- Usan PostgreSQL + PostGIS en un contenedor Docker (Testcontainers).
- Verifican que las migraciones Flyway se aplican correctamente.
- Herramientas: Spring Boot Test + Testcontainers.

### 3. Pruebas de contratos (contract testing)

**Dónde aplica:** entre la API y sus clientes (Android y Admin Web).

- Verifican que la API cumple el contrato OpenAPI definido en `contracts/openapi/`.
- Detectan cambios que rompen la compatibilidad antes de que lleguen a integración.
- Herramientas: Spring Cloud Contract o validación contra spec OpenAPI (a definir).

### 4. Pruebas Android instrumentadas

**Dónde aplica:** App Android.

- Prueban componentes que requieren el entorno Android real: Room, WorkManager, UI con Compose.
- Ejecutan en un emulador o dispositivo físico.
- Herramientas: AndroidX Test, Espresso, Compose UI Test.

### 5. Pruebas de sincronización sin conectividad

**Dónde aplica:** App Android + API (integración).

- Verifican que las gestiones registradas sin conexión se sincronizan correctamente al restablecer la red.
- Verifican idempotencia: reenviar la misma gestión no produce duplicados.
- Verifican el comportamiento de la cola outbox y los estados de sincronización.
- Estas pruebas son críticas para la confiabilidad del sistema en campo.

### 6. Pruebas de migración de PostgreSQL

**Dónde aplica:** infraestructura y API.

- Verifican que cada migración Flyway se aplica correctamente sobre el esquema anterior.
- Se ejecutan con Testcontainers en la pipeline de CI.
- No se debe ejecutar código en producción sin pasar estas pruebas.

### 7. Pruebas de migración de Room

**Dónde aplica:** App Android.

- Verifican que cada migración de esquema de Room es correcta y no destruye datos existentes.
- Son especialmente importantes porque los dispositivos en campo pueden tener versiones anteriores de la app.
- Herramientas: `androidx.room:room-testing`.

### 8. Pruebas manuales en dispositivo real

- Antes de cada release, se prueba la app en un dispositivo Android físico real.
- Se verifican flujos críticos: login, descarga de cartera, registro de gestión offline, sincronización.
- Se documenta el resultado en el PR correspondiente.
- Los emuladores no reemplazan las pruebas en dispositivo real para la app de campo.

## PENDIENTE

- Definir cobertura mínima de pruebas unitarias (porcentaje de líneas o branches).
- Definir herramienta de contract testing (Spring Cloud Contract, Pact u otra).
- Definir si se usan pruebas E2E para la web admin (Cypress, Playwright).
- Definir cómo se integran las pruebas Android en la pipeline de CI (Firebase Test Lab, GitHub Actions con emulador, etc.).
- Definir política de pruebas de regresión para actualizaciones de la app en producción.
