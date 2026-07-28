# ADR-0019: `identificador_instalacion` como UUID de instalación Android (no IMEI)

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

Cada instalación de la app Android debe identificarse de forma única en el servidor para controlar acceso y revocar dispositivos específicos. Las opciones comunes son IMEI, número de serie del dispositivo, o un UUID generado en la primera instalación.

## Decisión

El campo `identificador_instalacion` es un UUID generado por la app Android en la primera instalación y almacenado en `SharedPreferences` o equivalente. Es **distinto** del `id` (PK) de la fila en `cobranza.dispositivos`.

## Consecuencias

**Positivas**:
- No requiere permiso `READ_PHONE_STATE` (sensible y restringido desde Android 10).
- El UUID generado es estable durante la vida de la instalación y se regenera solo si la app se desinstala.
- El servidor puede identificar dispositivos sin acceder a información de hardware del fabricante.
- La separación entre `id` (PK del servidor) e `identificador_instalacion` permite que el cliente genere su propio identificador sin colisiones.

**Negativas**:
- Si el usuario desinstala y reinstala la app, se genera un nuevo `identificador_instalacion` y el dispositivo anterior queda inactivo (no se reutiliza automáticamente).
- No identifica hardware físico — un usuario malintencionado podría reinstalar la app para obtener un nuevo `identificador_instalacion`.

## Alternativas consideradas

- **IMEI**: Requiere permisos restringidos en Android 10+, prohibido sin justificación válida por Google Play.
- **Android ID**: Cambia al hacer factory reset; también tiene restricciones de acceso desde Android 8.
