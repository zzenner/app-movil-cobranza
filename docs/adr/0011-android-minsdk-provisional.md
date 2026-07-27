# ADR-0011: Versión mínima de Android provisional (API 29 / Android 10)

**Estado:** Provisional — pendiente de confirmación con inventario de dispositivos.
**Fecha:** 2026-07-26

## Contexto

La app Android se desplegará en teléfonos corporativos. La versión mínima de Android (`minSdk`) determina qué APIs están disponibles, qué dependencias pueden usarse y el alcance de compatibilidad del binario. Esta decisión impacta:

- Las APIs de Android disponibles (WorkManager, Room, Jetpack Compose, etc.).
- El acceso al almacenamiento seguro (EncryptedSharedPreferences, KeyStore).
- La política de permisos en tiempo de ejecución.
- El porcentaje de dispositivos corporativos cubiertos por la app.

La decisión definitiva requiere el inventario real de dispositivos corporativos con: modelo, versión de Android instalada, RAM, almacenamiento y política de actualización del SO.

## Decisión

Se establece provisionalmente `minSdk = 29` (Android 10) como límite inferior de compatibilidad.

- Android 10 tiene soporte extendido en la mayoría de fabricantes corporativos.
- A partir de API 29 están disponibles sin restricciones: Room, WorkManager, Jetpack Compose (en API 21+, pero con rendimiento óptimo en 29+), cifrado de base de datos (`SQLCipher` / `EncryptedSharedPreferences`), detección de ubicación simulada (`isMock`), y manejo de conectividad con `ConnectivityManager` mejorado.
- No se creará el proyecto Android ni se configurará `build.gradle` hasta confirmar el valor definitivo con el inventario.

## Consecuencias

- Quedan excluidos dispositivos corporativos con Android 9 (API 28) o inferior, si existieran.
- Si el inventario revela dispositivos con API 28 o inferior, la decisión deberá revisarse.
- La confirmación definitiva desbloquea la creación del proyecto Android.

## Alternativas consideradas

| Alternativa      | Razón de descarte                                                        |
|------------------|--------------------------------------------------------------------------|
| API 26 (Android 8.0) | Cubre más dispositivos viejos, pero limita APIs de seguridad y rendimiento. |
| API 31 (Android 12)  | Simplifica permisos, pero podría excluir dispositivos corporativos vigentes. |
| API 29 (Android 10)  | Balance entre modernidad y cobertura esperada. Seleccionado provisionalmente. |

## Referencias

- `docs/dominio/REGLAS_NEGOCIO.md` — RN-27
- `docs/gestion/STATUS.md` — P-07
