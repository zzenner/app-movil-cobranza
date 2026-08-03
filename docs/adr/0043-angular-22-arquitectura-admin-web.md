# ADR-0043: Arquitectura Angular 22 para el panel administrativo web

**Fecha:** 2026-08-03
**Estado:** Aceptado
**Contexto:** Fase 5A — Base del administrador web

## Contexto

Se necesita un panel web administrativo para gestionar usuarios, carteras y asignaciones de la plataforma Cobranza. El equipo tiene experiencia en Android/Kotlin pero no en frontend web.

## Decisión

Se usa **Angular 22.1.0** con las siguientes elecciones técnicas:

| Aspecto | Decisión | Alternativa descartada |
|---|---|---|
| Framework | Angular 22 (standalone) | React, Vue |
| TypeScript | 6.0.x (exigido por Angular 22) | Versión libre |
| Testing unitario | Vitest 4.1.10 (integrado en `@angular/build`) | Karma (obsoleto) |
| Testing E2E | Playwright 1.62.1 | Cypress |
| UI | Angular Material 22.1.0 | Custom CSS |
| State | Signals (`signal`, `computed`) | NgRx, BehaviorSubject |
| Routing | Angular Router con lazy loading | — |

## Consecuencias

**Positivas:**
- Vitest integrado en Angular Build — configuración mínima, salida estándar.
- Playwright con intercepción de red — tests E2E sin servidor real.
- Signals eliminan boilerplate de subscripciones y `async pipe`.
- Standalone components = sin módulos NgModule, estructura más plana.

**Negativas / Riesgo:**
- Angular 22 es reciente (agosto 2026); posibles cambios de API menores.
- Equipo nuevo en Angular — curva de aprendizaje.

**Notas:**
- Karma/Jasmine NO se usan. Si aparecen en una dependencia, son para tests internos de terceros.
- GlobalNG CLI no se instala globalmente; se usa `npx @angular/cli@22.1.2`.
