# Instrucciones para sesiones de IA — App Móvil Cobranza

Este archivo guía a cualquier sesión de Claude Code que trabaje en este repositorio.

## Lectura obligatoria antes de cualquier tarea

Antes de implementar, modificar o proponer cambios, leer en este orden:

1. [`README.md`](README.md) — visión general del proyecto y estructura.
2. [`docs/gestion/STATUS.md`](docs/gestion/STATUS.md) — fase actual y tareas en curso.
3. [`docs/gestion/ROADMAP.md`](docs/gestion/ROADMAP.md) — fases planificadas.
4. La documentación específica del módulo afectado (`docs/arquitectura/`, `docs/dominio/`, etc.).
5. Los ADR relacionados con la decisión o el área de trabajo (`docs/adr/`).

## Reglas de trabajo

### Alcance
- Trabajar una historia o tarea acotada a la vez.
- No modificar módulos no relacionados con la tarea en curso.
- Presentar un plan breve y esperar aprobación antes de implementar cambios significativos.
- Revisar el código existente antes de crear código nuevo.

### Calidad
- No asumir que una dependencia está instalada; verificar primero.
- Ejecutar las pruebas y validaciones disponibles.
- No ocultar errores ni omitir advertencias relevantes.
- No declarar una tarea completada si existen pruebas fallidas o errores no resueltos.

### Documentación
- Actualizar `docs/gestion/STATUS.md` y `docs/gestion/CHANGELOG.md` al completar cada tarea.
- Registrar deuda técnica real en `docs/gestion/DEUDA_TECNICA.md`.
- Crear un ADR en `docs/adr/` cuando una decisión afecte la arquitectura del sistema.
- No duplicar documentación que ya existe en otro archivo; enlazar en su lugar.

### Seguridad
- No almacenar secretos, contraseñas, tokens ni claves privadas en ningún archivo del repositorio.
- Los valores de ejemplo van en `.env.example`; el archivo `.env` está en `.gitignore`.
- No hacer push al repositorio remoto sin autorización explícita.

### Convenciones de nombres
- Español para nombres de entidades de dominio, módulos y tablas.
- Inglés para términos técnicos ampliamente reconocidos.
- `snake_case` en identificadores de PostgreSQL.
- Sin tildes, eñes ni caracteres especiales en identificadores técnicos.

## Informe al finalizar

Al terminar cualquier tarea, entregar un resumen con:

1. Archivos creados o modificados.
2. Comandos ejecutados y sus resultados.
3. Pruebas realizadas y resultado.
4. Problemas encontrados y cómo se resolvieron.
5. Problemas pendientes o bloqueantes.
6. Recomendación para la siguiente tarea.

## Contexto del sistema

La plataforma tiene tres aplicaciones:

- **API** (`apps/api/`): Java 21 + Spring Boot + Spring Modulith. Autoridad de datos financieros.
- **Admin Web** (`apps/admin-web/`): Angular standalone. Administración de usuarios, carteras y gestiones.
- **Android** (`apps/mobile-android/`): Kotlin + Jetpack Compose + Room. Cliente offline-first.

La base de datos es PostgreSQL con PostGIS, gestionada con Flyway y levantada con Docker Compose.

Ver `docs/arquitectura/ARQUITECTURA_GENERAL.md` para más detalle.
