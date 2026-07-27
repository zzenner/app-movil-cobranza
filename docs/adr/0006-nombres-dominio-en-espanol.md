# ADR-0006 — Nombres de dominio en español

## Estado
Aceptado.

## Contexto
El equipo de desarrollo y los usuarios del sistema son hispanohablantes. Las entidades del dominio (persona, cartera, gestión, cobrador) tienen nombres claros en español. Muchos proyectos técnicos en la región usan inglés incluso para el dominio, lo que genera una brecha entre el lenguaje del código y el lenguaje del negocio.

## Decisión
Usar español para nombres de módulos, clases de dominio, tablas y columnas cuando la traducción sea natural y no genere confusión técnica. Usar inglés cuando sea el estándar técnico reconocido y traducirlo resultaría artificial.

Reglas específicas:
- Tablas PostgreSQL: `snake_case` en español (ej: `personas`, `gestiones`, `asignaciones`, `fecha_creacion`).
- Sin tildes, eñes ni caracteres especiales en identificadores técnicos.
- Términos técnicos se mantienen en inglés: `id`, `uuid`, `timestamp`, `WorkManager`, `DAO`, `JWT`, `endpoint`, `outbox`.

## Consecuencias

**Positivas:**
- El código habla el mismo idioma que el negocio. Un analista puede leer un nombre de tabla sin traducción mental.
- Se elimina la brecha entre el lenguaje de los usuarios y el lenguaje del código (principio de Ubiquitous Language de DDD).
- Los nuevos desarrolladores del equipo encuentran los nombres de dominio autoexplicativos.

**Negativas:**
- Herramientas o librerías que esperan nombres en inglés pueden requerir configuración adicional (ej: convenciones de nombres de Hibernate).
- Algunos desarrolladores externos o libs de generación de código asumen inglés por defecto.

## Alternativas consideradas

**Todo en inglés:** Más alineado con el ecosistema técnico internacional, pero genera una distancia innecesaria entre el código y el lenguaje de negocio del equipo.

**Mezcla sin criterio claro:** Peor opción. Genera inconsistencia y confusión. Se descartó.

## Ejemplos de aplicación

| Concepto de negocio | Identificador técnico |
|---------------------|-----------------------|
| Persona             | `persona`             |
| Cartera de cobranza | `cartera`             |
| Gestión en terreno  | `gestion`             |
| Fecha de creación   | `fecha_creacion`      |
| Número de cuota     | `numero_cuota`        |
| Gastos de cobranza  | `gastos_cobranza`     |
| WorkManager (Android) | `WorkManager`       |
| Data Access Object  | `DAO`                 |
