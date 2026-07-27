# Backup y restauración

> **Estado: FUTURO — no implementado.**
>
> Este documento describe la estrategia prevista de backup para el entorno de producción.

## Alcance

El único estado persistente crítico del sistema es la base de datos PostgreSQL. La app Android almacena datos locales, pero la fuente de verdad es la base de datos de la API.

## Estrategia prevista

### Backup de PostgreSQL

- **Herramienta:** `pg_dump` o `pg_basebackup`.
- **Frecuencia:** diaria como mínimo (frecuencia exacta: PENDIENTE según volumen de datos).
- **Retención:** PENDIENTE de definir (sugerencia: 7 días diarios + 4 semanas mensuales).
- **Destino:** almacenamiento externo al VPS (S3 compatible, NAS institucional, etc.).
- **Verificación:** restauración periódica en entorno de prueba para verificar integridad.

### Backup de archivos (fotografías)

Cuando se implemente el almacenamiento de fotografías (S3 compatible), se definirá la estrategia de backup junto con el proveedor de almacenamiento.

## Procedimiento de restauración (borrador)

1. Detener la API para evitar escrituras durante la restauración.
2. Restaurar el backup con `pg_restore` o volcado SQL.
3. Verificar la integridad de los datos restaurados.
4. Reiniciar la API.
5. Documentar el incidente y el punto de recuperación.

## TODO (fases futuras)

- [ ] Crear script de backup automatizado.
- [ ] Integrar backup en la infraestructura de producción.
- [ ] Documentar y probar el procedimiento completo de restauración.
- [ ] Definir RTO (Recovery Time Objective) y RPO (Recovery Point Objective).
- [ ] Configurar alertas si el backup falla.

## PENDIENTE

- Definir destino de backup y credenciales de acceso.
- Definir frecuencia y retención.
- Definir RTO y RPO acordados con el equipo.
