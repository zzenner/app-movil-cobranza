# ADR-0015 — Stack técnico de la API: Java 21, Spring Boot 3.5, Maven

**Estado:** Aceptado
**Fecha:** 2026-07-27
**Fase:** 1B

---

## Contexto

La Fase 1B requiere inicializar el proyecto `apps/api/` con un stack técnico definido. Las decisiones de base de datos (PostgreSQL + PostGIS) y diseño modular (Spring Modulith) ya están establecidas en ADR anteriores. Falta definir el framework de aplicación, el lenguaje y la herramienta de build.

---

## Decisión

Se usa:

- **Java 21 (LTS)** como plataforma de ejecución.
- **Spring Boot 3.5.16** como framework principal.
- **Spring Modulith 1.4.12** como capa de modularidad (importado como BOM).
- **Maven 3.8.7 con Maven Wrapper** como herramienta de build.
- **Springdoc OpenAPI 2.8.17** para generación y exposición de documentación de la API.

---

## Alternativas descartadas

| Alternativa | Motivo de descarte |
|---|---|
| Kotlin en lugar de Java | Sin experiencia del equipo. Java 21 con records y sealed classes cubre las necesidades. |
| Gradle en lugar de Maven | Maven es más estable y predecible para proyectos empresariales. Se descarta Gradle explícitamente en las restricciones de la Fase 1B. |
| Spring WebFlux | Agrega complejidad innecesaria. El dominio no requiere programación reactiva. Descartado explícitamente. |
| Quarkus | Menor madurez en el ecosistema Spring Modulith. |

---

## Consecuencias

- El proyecto compila y ejecuta con Java 21 o superior.
- El Maven Wrapper (`./mvnw`) garantiza reproducibilidad del build sin depender de la versión de Maven instalada en el desarrollador.
- Spring Boot gestiona la mayoría de las versiones de dependencias transitivas, minimizando conflictos.
- Springdoc debe declararse explícitamente con versión porque Spring Boot no lo gestiona.

---

## Referencias

- [Spring Boot 3.5.x Release Notes](https://spring.io/projects/spring-boot)
- [Spring Modulith](https://spring.io/projects/spring-modulith)
- ADR-0001 — Arquitectura modular con Spring Modulith
