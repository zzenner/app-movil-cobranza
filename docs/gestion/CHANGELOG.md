# Changelog

Todos los cambios significativos del proyecto se documentan aquí.
Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

---

## [Sin versión] — 2026-07-27 — Fase 1B: Base técnica modular de la API ✅

### Resultado
Proyecto `apps/api/` creado y verificado. 10/10 pruebas pasan. Todos los endpoints de infraestructura responden correctamente.

### Añadido

**Proyecto Maven:**
- `apps/api/pom.xml` — Spring Boot 3.5.16, Spring Modulith 1.4.12, Flyway, JPA, Actuator, Springdoc 2.8.17.
- `apps/api/mvnw` + `apps/api/.mvn/` — Maven Wrapper.

**Código fuente:**
- `CobranzaApplication.java` — clase principal.
- 11 módulos Spring Modulith con `package-info.java` + `@ApplicationModule`: autenticacion, usuarios, dispositivos, carteras, asignaciones, personas, operaciones, gestiones, sincronizacion, auditoria, compartido.
- `GlobalExceptionHandler.java` — `ProblemDetail` para validación.
- `OpenApiConfig.java` — bean OpenAPI con título y versión.

**Configuración:**
- `application.yml` — configuración base (sin datasource, ddl-auto=none, probes, springdoc).
- `application-local.yml` — datasource local desde variables de entorno.

**Base de datos:**
- `V001__crear_esquemas_base.sql` — primera migración Flyway, crea `cobranza` y `auditoria`.

**Pruebas:**
- `ModularidadTest` — verifica estructura Spring Modulith.
- `InfraestructuraTest` — Testcontainers PostGIS, esquemas Flyway, PostGIS disponible, Hibernate sin tablas.
- `ActuatorTest` — health, liveness, readiness, OpenAPI accesibles.

**OpenAPI:**
- `contracts/openapi/cobranza-api.yaml` — contrato v0.1.0 con `paths: {}`.

**Scripts:**
- `scripts/api-run.sh`, `scripts/api-test.sh`, `scripts/api-check.sh`.

**CI:**
- `.github/workflows/api-ci.yml` — Java 21, Maven Wrapper, cache, activado en `apps/api/**`.

**ADRs:**
- `docs/adr/0015-stack-tecnico-api.md` — Java 21, Spring Boot 3.5.16, Maven.
- `docs/adr/0016-flyway-propietario-esquema.md` — Flyway como propietario exclusivo del esquema.
- `docs/adr/0017-testcontainers-pruebas-integracion.md` — Testcontainers con PostGIS.

**Documentación:**
- `docs/arquitectura/API_BASE_TECNICA.md` — documentación completa de la base técnica.
- `apps/api/README.md` — guía de comandos y endpoints.

### Modificado

- `infrastructure/postgres/init/02_schemas.sql` — retirada la creación de esquemas (responsabilidad migrada a Flyway V001).
- `docs/arquitectura/MODULOS.md` — módulos actualizados con nombres correctos (operaciones en lugar de creditos, dispositivos añadido, estados actualizados).
- `docs/gestion/DEUDA_TECNICA.md` — DT-007 marcado como resuelto (DT-R06).
- `docs/gestion/STATUS.md`, `ROADMAP.md` — Fase 1B marcada como completada.

### Validación realizada

- `./mvnw verify`: 10/10 pruebas ✅
- `docker compose up -d` + `./scripts/api-run.sh`: API inicia correctamente ✅
- Flyway V001 aplicada: esquemas `cobranza` y `auditoria` creados ✅
- Endpoints verificados: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/info`, `/v3/api-docs`, `/swagger-ui/index.html` ✅

---

## [Sin versión] — 2026-07-26 — Auditoría final Fase 1A ✅

### Resultado
**FASE 1A APROBADA.** Toda la documentación de dominio es consistente con las decisiones funcionales confirmadas. No se encontraron contradicciones activas. Se crearon documentos faltantes y se corrigieron tres inconsistencias menores.

### Contradicciones corregidas

| Archivo | Detalle |
|---|---|
| `docs/producto/HISTORIAS_USUARIO.md` | HU-005: eliminado "y monto" en COMPROMISO_PAGO (contradecía RF-05c y RN-12). |
| `docs/contexto/ALCANCE.md` | Sección PENDIENTE: items resueltos (reapertura offline → RN-24; operaciones descargadas → RN-10) movidos a sección "Resuelto". |
| `docs/gestion/DEUDA_TECNICA.md` | DT-001 (PIN/biometría) → DT-R04 (resuelto). DT-003 (minSdk) → DT-R05 (resuelto provisionalmente). |

### Añadido

**Documentos de dominio obligatorios:**
- `docs/dominio/DIAGRAMA_ENTIDAD_RELACION.md` — Diagrama Mermaid con 20 entidades y sus relaciones.
- `docs/dominio/DICCIONARIO_DATOS_PRELIMINAR.md` — Descripción detallada de columnas de todas las entidades candidatas.
- `docs/dominio/MATRIZ_AUTORIDAD_DATOS.md` — Tabla de autoridad por dato: sistema externo / API / Android / Admin Web.
- `docs/dominio/DECISIONES_PENDIENTES.md` — Consolidación de DP-01 a DP-08 con impacto y bloqueantes.

**ADR faltantes:**
- `docs/adr/0012-persona-como-unidad-principal.md`
- `docs/adr/0013-uuid-generado-en-dispositivo.md`
- `docs/adr/0014-copia-operacional-datos-externos.md`

### Actualizado

- `docs/gestion/ROADMAP.md` — Fase 1A registrada como completada; Fase 1B descrita con prerequisitos.
- `docs/gestion/STATUS.md` — Veredicto de auditoría, resumen de correcciones, historial de fases actualizado.

---

## [Sin versión] — 2026-07-26 — Decisiones finales Fase 1A (tercera sesión)

### Actualizado
- `docs/dominio/CICLOS_DE_VIDA.md` — Estados de asignación diaria reescritos: BORRADOR/PUBLICADA/FINALIZADA/CANCELADA(opcional); DESCARGADA eliminado como estado funcional; concepto `descarga_asignacion_diaria` introducido; diagrama de logout con operaciones pendientes actualizado con política MVP confirmada.
- `docs/dominio/MODELO_DOMINIO.md` — Estados de asignación diaria y descarga como evento técnico.
- `docs/dominio/MODELO_DATOS.md` — `asignaciones_diarias.estado` actualizado; tabla `descargas_asignacion_diaria` añadida; nota de alcance de cuotas actualizada.
- `docs/dominio/REGLAS_NEGOCIO.md` — RN-10 (alcance operaciones/cuotas), RN-22 (estados confirmados), RN-24 (política logout completa), RN-27 (minSdk provisional). PENDIENTE reducido a 8 ítems.
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — Sección de datos locales con alcance completo de operaciones; política logout MVP completa.
- `docs/sincronizacion/RESOLUCION_CONFLICTOS.md` — Sección de logout con operaciones pendientes actualizada con política MVP confirmada (ya no PENDIENTE).
- `docs/producto/REQUISITOS_FUNCIONALES.md` — RF-01g con política logout completa; RF-04c con alcance de operaciones activas y cuotas vigentes. PENDIENTE actualizado.
- `docs/contexto/GLOSARIO.md` — Estados asignación diaria actualizados (DESCARGADA eliminado, FINALIZADA/CANCELADA añadidos); concepto `descarga_asignacion_diaria` añadido. PENDIENTE actualizado.
- `docs/gestion/STATUS.md` — Tercera sesión de decisiones; preguntas pendientes actualizadas.

### Añadido
- `docs/adr/0011-android-minsdk-provisional.md` — ADR para decisión provisional de minSdk API 29 / Android 10.

---

## [Sin versión] — 2026-07-26 — Complemento de decisiones funcionales Fase 1A

### Actualizado
- `docs/dominio/MODELO_DOMINIO.md` — Aval simplificado a solo lectura (rut, nombre); asignación diaria con supervisor y estados preliminares; dirección reportada → observación de dirección; relaciones actualizadas.
- `docs/dominio/MODELO_DATOS.md` — Tabla `avales` simplificada; `asignaciones_diarias` con supervisor_id, estado, fecha_publicacion; `direcciones_reportadas` reemplazada por `observaciones_direccion`.
- `docs/dominio/REGLAS_NEGOCIO.md` — RN-05, RN-09, RN-17 actualizadas; nuevas RN-21 a RN-26; pendientes reducidos y re-numerados.
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — Sección de retención de datos locales; sección de sesión local y tokens.
- `docs/sincronizacion/PROTOCOLO_SINCRONIZACION.md` — Flujo de sincronización con paso de validación de autenticación al recuperar conectividad.
- `docs/sincronizacion/RESOLUCION_CONFLICTOS.md` — Reescritura completa: conflictos de asignación con pendientes, sesión revocada, logout con pendientes.
- `docs/producto/REQUISITOS_FUNCIONALES.md` — RF-01 completo con sesión persistente y tokens; RF-03 con supervisor; RF-05 con tipos confirmados y observación de dirección; RF-06 con retención.
- `docs/contexto/GLOSARIO.md` — Estados de asignación diaria; sesión/tokens; observación_direccion.
- `docs/gestion/STATUS.md` — Nuevas decisiones incorporadas; pendientes actualizados.

### Añadido
- `docs/dominio/CICLOS_DE_VIDA.md` — Ciclos de asignación diaria (con estados y justificación), gestión (estados técnicos de sincronización), sesión Android, y política de retención de datos locales.

---

## [Sin versión] — 2026-07-26 — Decisiones funcionales del dominio

### Actualizado
- `docs/contexto/GLOSARIO.md` — Nuevos roles (`JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`), nuevos términos (asignación mensual, asignación diaria, tipos de gestión, estados de sincronización técnicos).
- `docs/dominio/MODELO_DOMINIO.md` — Modelo completo con jerarquía confirmada: `cartera → persona → operaciones → cuotas`; avales, asignaciones mensuales y diarias, gestiones, fotografías, direcciones importadas y reportadas, supervisión, dispositivos.
- `docs/dominio/MODELO_DATOS.md` — Tablas candidatas detalladas para todos los módulos confirmados; columnas `rut_numero` y `rut_dv` en `personas`.
- `docs/dominio/REGLAS_NEGOCIO.md` — 20 reglas confirmadas (RN-01 a RN-20); 7 preguntas pendientes reducidas de 13 anteriores.
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — Estados de sincronización actualizados a los confirmados; asignación diaria como unidad de descarga; indicadores de estado en la interfaz; sincronización manual.
- `docs/gestion/STATUS.md` — Estado actualizado con decisiones incorporadas y preguntas pendientes reducidas.

### Añadido
- `docs/adr/0007-rut-separado-numero-dv.md` — RUT almacenado como `rut_numero` + `rut_dv`.
- `docs/adr/0008-asignacion-diaria-unidad-sincronizacion.md` — Asignación diaria (~50 personas) como unidad de descarga al móvil.
- `docs/adr/0009-gestiones-inmutables.md` — Gestiones inmutables: sin rectificaciones ni anulaciones en el MVP.
- `docs/adr/0010-geolocalizacion-obligatoria.md` — Geolocalización puntual obligatoria para registrar gestiones.

---

## [Sin versión] — 2026-07-26 — Fase 0: Inicialización

### Añadido
- Estructura de directorios del monorepo (`apps/`, `contracts/`, `infrastructure/`, `docs/`, `scripts/`, `.github/`).
- Archivos raíz de configuración: `README.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `.editorconfig`, `.gitattributes`, `.gitignore`, `.env.example`.
- `compose.yaml` con PostgreSQL 16 + PostGIS 3.4 y perfil `tools` para Adminer.
- Scripts de gestión del entorno local: `start.sh`, `stop.sh`, `status.sh`, `logs.sh`, `clean.sh`, `check.sh`.
- Scripts SQL de inicialización: extensiones (postgis, uuid-ossp, pg_trgm) y esquemas lógicos (`cobranza`, `auditoria`).
- Documentación de contexto: `VISION.md`, `ALCANCE.md`, `GLOSARIO.md`.
- Documentación de producto: `REQUISITOS_FUNCIONALES.md`, `REQUISITOS_NO_FUNCIONALES.md`, `HISTORIAS_USUARIO.md`.
- Documentación de arquitectura: `ARQUITECTURA_GENERAL.md`, `MODULOS.md`, `DIAGRAMA_CONTENEDORES.md`.
- Documentación de dominio: `MODELO_DOMINIO.md`, `MODELO_DATOS.md`, `REGLAS_NEGOCIO.md`.
- Documentación de sincronización: `ESTRATEGIA_OFFLINE.md`, `RESOLUCION_CONFLICTOS.md`, `PROTOCOLO_SINCRONIZACION.md`.
- Documentación de seguridad: `SEGURIDAD.md`.
- Documentación de pruebas: `ESTRATEGIA_PRUEBAS.md`.
- Documentación de operación: `DESARROLLO_LOCAL.md`, `DESPLIEGUE_VPS.md`, `BACKUP_RESTAURACION.md`.
- Documentación de gestión: `ROADMAP.md`, `STATUS.md`, `CHANGELOG.md`, `DEUDA_TECNICA.md`.
- ADR iniciales: `0001` a `0006`.
- Plantillas de GitHub: PR template, issue template de historia, issue template de error.
