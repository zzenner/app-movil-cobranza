# App Móvil Cobranza

Plataforma modular para la gestión de cobranza en terreno. Permite a cobradores registrar gestiones desde dispositivos Android con soporte offline, y a administradores supervisar asignaciones y resultados desde una interfaz web.

## Estado actual

**Fase 0 — Inicialización del repositorio.** No existe código de negocio funcional todavía.

Ver [`docs/gestion/STATUS.md`](docs/gestion/STATUS.md) y [`docs/gestion/ROADMAP.md`](docs/gestion/ROADMAP.md) para el estado detallado.

## Componentes del sistema

| Componente        | Ubicación              | Stack                                  |
|-------------------|------------------------|----------------------------------------|
| API central       | `apps/api/`            | Java 21 + Spring Boot + Spring Modulith |
| Aplicación web    | `apps/admin-web/`      | Angular (standalone components)        |
| App Android       | `apps/mobile-android/` | Kotlin + Jetpack Compose + Room        |
| Base de datos     | Docker Compose         | PostgreSQL + PostGIS                   |

## Inicio rápido (entorno local)

**Requisitos previos:** Docker Desktop con WSL2.

```bash
# Copiar variables de entorno
cp .env.example .env
# Ajustar .env con valores locales si es necesario

# Iniciar base de datos
./scripts/start.sh

# Ver estado
./scripts/status.sh

# Ver logs
./scripts/logs.sh

# Detener
./scripts/stop.sh
```

Ver [`docs/operacion/DESARROLLO_LOCAL.md`](docs/operacion/DESARROLLO_LOCAL.md) para instrucciones completas.

## Documentación

| Sección             | Descripción                                          |
|---------------------|------------------------------------------------------|
| `docs/contexto/`    | Visión, alcance y glosario del producto              |
| `docs/producto/`    | Requisitos funcionales, no funcionales e historias   |
| `docs/arquitectura/`| Arquitectura general, módulos y diagrama             |
| `docs/dominio/`     | Modelo de dominio, datos y reglas de negocio         |
| `docs/sincronizacion/` | Estrategia offline y protocolo de sincronización  |
| `docs/seguridad/`   | Principios y controles de seguridad                  |
| `docs/pruebas/`     | Estrategia de pruebas                                |
| `docs/operacion/`   | Desarrollo local, despliegue y backup                |
| `docs/gestion/`     | Roadmap, status, changelog y deuda técnica           |
| `docs/adr/`         | Registros de decisiones arquitectónicas              |

## Convenciones

- Español para nombres de dominio (tablas, módulos, entidades).
- Inglés para términos técnicos estándar.
- `snake_case` en PostgreSQL; sin tildes ni eñes en identificadores.
- Secretos solo en `.env` (ignorado por Git); valores ejemplo en `.env.example`.
- Ver [`CONTRIBUTING.md`](CONTRIBUTING.md) para guía de contribución.
- Ver [`CLAUDE.md`](CLAUDE.md) para instrucciones a sesiones de IA.

## Licencia

PENDIENTE — definir licencia según política institucional.
