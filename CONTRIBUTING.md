# Guía de contribución

Este documento describe cómo colaborar en este repositorio.

## Requisitos previos

- Docker Desktop con soporte WSL2.
- Git configurado con nombre y correo personal.
- Leer `README.md`, `docs/gestion/STATUS.md` y `docs/gestion/ROADMAP.md` antes de comenzar.

## Flujo de trabajo

1. Crear una rama desde `main` con nombre descriptivo:
   - `feature/nombre-de-la-funcionalidad`
   - `fix/descripcion-del-error`
   - `docs/actualizacion-documentacion`
   - `chore/tarea-de-mantenimiento`
2. Trabajar en una sola historia o tarea por rama.
3. No modificar módulos no relacionados con la tarea.
4. Abrir un Pull Request con la plantilla correspondiente.
5. Esperar revisión antes de fusionar.

## Convenciones de commits

Usar prefijos semánticos en inglés:

```
feat: descripción breve en español
fix: descripción breve en español
docs: actualización de documentación
chore: tarea de mantenimiento o configuración
test: adición o corrección de pruebas
refactor: reestructuración sin cambio de comportamiento
```

Un commit por cambio lógico. Mensajes claros y en español cuando describan el dominio.

## Convenciones de código

- Español para nombres de entidades, módulos y tablas de dominio.
- Inglés para términos técnicos ampliamente reconocidos.
- `snake_case` en PostgreSQL; sin tildes, eñes ni caracteres especiales.
- No almacenar secretos ni credenciales reales en el repositorio.
- Ver [`CLAUDE.md`](CLAUDE.md) para reglas adicionales relevantes a sesiones de IA.

## Documentación

- Actualizar `docs/gestion/CHANGELOG.md` con cada cambio relevante.
- Actualizar `docs/gestion/STATUS.md` si el estado de la fase cambia.
- Registrar deuda técnica real en `docs/gestion/DEUDA_TECNICA.md`.
- Crear un ADR en `docs/adr/` si la decisión afecta la arquitectura.
- No duplicar documentación que ya existe en otro archivo.

## Pruebas

- Ejecutar todas las pruebas disponibles antes de abrir un PR.
- No declarar una tarea completa si existen pruebas fallidas.
- Documentar pruebas manuales realizadas en el PR.

## Seguridad

- No agregar secretos, contraseñas ni tokens al repositorio.
- Los valores de ejemplo van en `.env.example`, nunca en `.env`.
- No hacer push sin autorización explícita.
