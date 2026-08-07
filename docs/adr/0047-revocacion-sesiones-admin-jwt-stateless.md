# ADR-0047: Revocación de sesiones en operaciones administrativas sobre usuarios

**Estado:** Aceptado
**Fecha:** 2026-08-06
**Decisores:** Equipo de desarrollo

## Contexto

Cuando un administrador desactiva, bloquea o restablece la contraseña de un usuario, el JWT de acceso
vigente del usuario afectado permanece técnicamente válido hasta su expiración (15 min). Esto genera
una ventana en la que el usuario podría seguir operando aunque su cuenta haya sido intervenida.

## Decisión

Al ejecutar cualquiera de las operaciones de escritura que modifican la seguridad del usuario
(`desactivar`, `bloquear`, `restablecer-contrasena`), se publica un `SeguridadUsuarioModificadaEvent`
(definido en `usuarios.api`) que el módulo `autenticacion` escucha con
`@TransactionalEventListener(phase = BEFORE_COMMIT)`.

El listener revoca todos los refresh tokens del usuario (estados ACTIVO y CONSUMIDO en sesiones
no cerradas) y cierra todas sus sesiones activas con motivo `REVOCACION_ADMIN`.

La operación es atómica con la modificación del usuario: si el listener falla, la transacción completa
se revierte.

## Consecuencias

- **Positivo:** El usuario no puede renovar tokens tras la intervención administrativa.
- **Positivo:** Comunicación entre módulos sin dependencia de compilación inversa (no crea ciclo en Spring Modulith).
- **Limitación aceptada:** El JWT de acceso actual sigue siendo válido hasta su TTL (máx. 15 min).
  Esta ventana es aceptable dado el TTL corto y que el evento ya impide la renovación.
- **Alternativa descartada:** Invalidar el JWT de acceso en un store centralizado (Redis)
  añadiría infraestructura y latencia a cada petición.
