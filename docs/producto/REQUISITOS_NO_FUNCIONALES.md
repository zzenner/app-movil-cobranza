# Requisitos no funcionales

## RNF-01 Disponibilidad offline

- La app Android debe ser funcional sin conexión a internet para lectura y registro de gestiones.
- La sincronización debe completarse cuando se restablezca la conexión, sin pérdida de datos.

## RNF-02 Rendimiento

- Las consultas locales por RUT en la cartera del cobrador deben responder en menos de 500 ms en un dispositivo Android de gama media.
- La API debe responder a peticiones de consulta en menos de 2 segundos bajo carga normal (definición de carga normal: PENDIENTE).

## RNF-03 Seguridad

- Todas las comunicaciones entre la app y la API deben realizarse por HTTPS.
- Los tokens de sesión deben almacenarse en el almacenamiento seguro del sistema operativo.
- No se deben almacenar secretos en el repositorio de código.
- La base de datos local del dispositivo debe protegerse con cifrado (SQLite cifrado: PENDIENTE evaluar SQLCipher u opciones nativas).
- Ver `docs/seguridad/SEGURIDAD.md` para detalle completo.

## RNF-04 Escalabilidad

- El sistema debe soportar un número de cobradores y carteras acordes al tamaño institucional (valores concretos: PENDIENTE).
- La arquitectura de monolito modular debe permitir escalar horizontalmente la API si fuera necesario en el futuro.

## RNF-05 Mantenibilidad

- El código debe organizarse en módulos coherentes con el dominio (Spring Modulith en la API, feature modules en Android).
- Las migraciones de base de datos deben ser versionadas con Flyway.
- Las decisiones arquitectónicas significativas deben documentarse con ADR.

## RNF-06 Portabilidad y despliegue

- El entorno de producción debe poder desplegarse en un VPS Ubuntu estándar con Docker Compose y Nginx.
- El entorno local de desarrollo debe funcionar con Docker Desktop y WSL2 en Windows.

## RNF-07 Trazabilidad y auditoría

- El sistema debe registrar quién realizó cada operación significativa, con marca de tiempo.
- Las gestiones deben ser inmutables una vez sincronizadas (las correcciones se registran como nuevas entradas o como eventos de corrección: PENDIENTE de definir).

## RNF-08 Confiabilidad de sincronización

- La sincronización debe ser resistente a fallos de red parciales.
- Ninguna gestión registrada en el dispositivo puede perderse por fallo en la sincronización.
- Los reintentos deben seguir una política de backoff exponencial.

## PENDIENTE

- Definir umbrales de rendimiento y carga concreta (número de usuarios, carteras, gestiones diarias).
- Evaluar SQLCipher u otra solución de cifrado para la base de datos local Android.
- Definir SLA para el entorno de producción.
- Definir política de corrección de gestiones ya sincronizadas.
