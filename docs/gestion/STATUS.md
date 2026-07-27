# Estado del proyecto

**Última actualización:** 2026-07-26
**Fase actual:** Fase 0 — Inicialización del repositorio (completada) / En espera de inicio de Fase 1

## Resumen

| Item                                      | Estado                |
|-------------------------------------------|-----------------------|
| Estructura del monorepo                   | Completado            |
| Documentación base (Fase 0)               | Completado            |
| Decisiones funcionales del dominio        | Completado            |
| ADR iniciales (0001–0010)                 | Completado            |
| Docker Compose (PostgreSQL)               | Completado            |
| Scripts de entorno local                  | Completado            |
| Plantillas GitHub                         | Completado            |
| API (Spring Boot)                         | No iniciado           |
| Admin Web (Angular)                       | No iniciado           |
| App Android (Kotlin)                      | No iniciado           |
| Despliegue en VPS                         | No iniciado           |

## Bloqueantes activos

**Docker Desktop no integrado con WSL2:** La distro WSL2 activa no tiene integración con Docker Desktop. Los scripts de entorno local (`./scripts/start.sh`) no funcionarán hasta habilitar la integración en Docker Desktop → Settings → Resources → WSL Integration.

## Decisiones funcionales incorporadas (tercera sesión, 2026-07-26)

| Decisión                                                             | Documento de referencia                          |
|----------------------------------------------------------------------|--------------------------------------------------|
| Estados asignación diaria CONFIRMADOS: BORRADOR/PUBLICADA/FINALIZADA/CANCELADA(opcional) | RN-22, CICLOS_DE_VIDA, GLOSARIO |
| DESCARGADA eliminado como estado funcional; descarga = evento técnico `descarga_asignacion_diaria` | RN-22, MODELO_DATOS, CICLOS_DE_VIDA |
| Alcance descarga: TODAS las operaciones activas + TODAS las cuotas vencidas + futuras vigentes | RN-10, RF-04c, ESTRATEGIA_OFFLINE |
| No se descargan: operaciones anuladas, cerradas sin saldo, completamente pagadas | RN-10, MODELO_DATOS            |
| Política logout MVP CONFIRMADA: bloquear si pendientes+sin red; sincronizar si pendientes+con red | RN-24, RF-01g, RESOLUCION_CONFLICTOS, CICLOS_DE_VIDA |
| Android minSdk PROVISIONAL: API 29 / Android 10 (pendiente inventario) | RN-27, ADR-0011                 |

## Decisiones funcionales incorporadas (segunda sesión, 2026-07-26)

| Decisión                                                 | Documento de referencia         |
|----------------------------------------------------------|---------------------------------|
| Supervisor crea y publica la asignación diaria           | RN-21, MODELO_DOMINIO, RF-03c   |
| Estados de asignación diaria: BORRADOR/PUBLICADA/DESCARGADA/CERRADA (preliminares) | RN-22, CICLOS_DE_VIDA |
| Retención de datos locales: no borrar pendientes         | RN-23, ESTRATEGIA_OFFLINE       |
| Sesión local persistente: no PIN/biometría en MVP        | RN-24, ESTRATEGIA_OFFLINE       |
| Access token + refresh token como conceptos separados    | RN-24, PROTOCOLO_SINCRONIZACION |
| Logout advierte sobre pendientes; no borra silenciosamente | RN-24, CICLOS_DE_VIDA         |
| `observacion_direccion` como entidad simple (no corrección activa) | RN-25, MODELO_DATOS  |
| Avales: solo lectura mínima (rut_numero, rut_dv, nombre) | RN-26, MODELO_DATOS             |

## Decisiones funcionales incorporadas (primera sesión, 2026-07-26)

Las siguientes decisiones fueron confirmadas y están documentadas:

- **Estructura de cobranza:** `cartera → persona → operaciones → cuotas`. Unidad principal: persona.
- **RUT:** almacenado como `rut_numero` + `rut_dv` (ADR-0007).
- **Asignación mensual:** ~300–400 personas, carga por CSV, historial conservado.
- **Asignación diaria:** ~50 personas, base de la ruta, unidad de descarga al móvil (ADR-0008).
- **Roles:** `JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`.
- **Tipos de gestión:** `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`.
- **Gestiones inmutables:** sin rectificaciones ni anulaciones en el MVP (ADR-0009).
- **Geolocalización obligatoria:** sin coordenadas no se permite guardar (ADR-0010).
- **Estados de sincronización:** `PENDIENTE_ENVIO`, `ENVIANDO`, `SINCRONIZADA`, `ERROR_REINTENTABLE`, `ERROR_PERMANENTE`.
- **Compromiso sin monto:** solo fecha de compromiso en `COMPROMISO_PAGO`.
- **Búsqueda:** local en asignación diaria + global por API con conexión.
- **Direcciones reportadas:** conservan dirección original, no la sobrescriben.
- **Estado de dispositivos:** visible en la aplicación administrativa.

## Preguntas todavía pendientes

| ID   | Pregunta                                                                                                      |
|------|---------------------------------------------------------------------------------------------------------------|
| P-01 | ¿El ejecutivo puede registrar gestiones sobre personas fuera de su asignación diaria activa?                 |
| P-02 | ¿Los ejecutivos ven gestiones de otros ejecutivos sobre la misma persona en la app Android?                  |
| P-03 | ¿Cuál es el catálogo completo futuro de tipos de gestión? (los tres iniciales están confirmados)             |
| P-04 | ¿Se implementa exportación a Excel en la Fase 1 o en una posterior?                                         |
| P-05 | ¿Puede el supervisor modificar una asignación `PUBLICADA` o debe crear una nueva?                           |
| P-06 | ¿Se implementa `CANCELADA` como estado de asignación diaria en el MVP?                                      |
| P-07 | Confirmar minSdk definitivo con inventario de dispositivos corporativos (provisional: API 29 / Android 10).  |
| P-08 | Confirmar si el aval se asocia a persona u operación en el sistema externo definitivo (no bloqueante).       |

## Próximo paso recomendado

Iniciar la **Fase 1** con la creación del proyecto Spring Boot base (`apps/api/`) y el módulo de autenticación.
Resolver las preguntas P-01 y P-02 antes de implementar la autenticación offline.

## Historial de fases

| Fase   | Descripción                              | Estado     | Fecha      |
|--------|------------------------------------------|------------|------------|
| Fase 0 | Inicialización del repositorio           | Completado | 2026-07-26 |
