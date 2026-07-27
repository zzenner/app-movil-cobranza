# ADR-0017 — Testcontainers para pruebas de integración con PostgreSQL/PostGIS

**Estado:** Aceptado
**Fecha:** 2026-07-27
**Fase:** 1B

---

## Contexto

Las pruebas de integración de la API necesitan una base de datos PostgreSQL con PostGIS. Las opciones son: base de datos real (local o CI), base de datos en memoria (H2), o base de datos en contenedor efímero (Testcontainers).

---

## Decisión

Se usa **Testcontainers** con la imagen `postgis/postgis:16-3.4` para todas las pruebas de integración que requieren base de datos.

La configuración usa:
- `@Testcontainers` y `@Container` de la librería `testcontainers:junit-jupiter`.
- `@ServiceConnection` de `spring-boot-testcontainers` para inyectar automáticamente las credenciales del contenedor en el contexto Spring.
- `DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres")` para que Testcontainers reconozca la imagen PostGIS como sustituto de la imagen estándar de PostgreSQL.

La imagen es la misma que usa `compose.yaml` para garantizar paridad de entorno.

---

## Alternativas descartadas

| Alternativa | Motivo de descarte |
|---|---|
| H2 en memoria | No tiene PostGIS. No reproduce el comportamiento de PostgreSQL. Descartado explícitamente. |
| Base de datos local pre-existente | No reproducible en CI. Requiere que el desarrollador tenga PostgreSQL configurado correctamente. |
| `@DataJpaTest` con H2 | Mismas limitaciones que H2. |
| Docker Compose externo en CI | Más configuración de CI. Testcontainers es más portable. |

---

## Consecuencias

- Las pruebas de integración requieren Docker disponible en el entorno de ejecución (local y CI).
- Cada clase de test que use Testcontainers levanta su propio contenedor. El contexto Spring puede reutilizarse entre clases con la misma configuración (Spring Test Context caching).
- Los tests de CI corren en `ubuntu-latest` que tiene Docker disponible por defecto con GitHub Actions.
- El desarrollador debe tener Docker Desktop con integración WSL2 habilitada para correr los tests localmente.
- `ModularidadTest` no requiere Docker (no levanta Spring context ni Testcontainers).

---

## Referencias

- [Testcontainers para Spring Boot](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- ADR-0002 — PostgreSQL como base de datos relacional
- DT-007 — Docker Desktop integrado con WSL2 (resuelto en Fase 1B)
