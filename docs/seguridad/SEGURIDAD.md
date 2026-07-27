# Seguridad

## Principios generales

El sistema aplica los siguientes principios de seguridad desde el diseño inicial.

### 1. HTTPS obligatorio
- Toda comunicación entre la app Android y la API, y entre la web admin y la API, se realiza exclusivamente por HTTPS.
- No se permite tráfico HTTP en producción.
- En desarrollo local, HTTPS puede omitirse entre contenedores en la red interna de Docker.

### 2. Almacenamiento seguro de tokens
- Los tokens de autenticación en la app Android se almacenan en Android Keystore o en Encrypted SharedPreferences, nunca en almacenamiento plano.
- Los tokens no se escriben en logs ni se exponen en la interfaz.

### 3. Sesión offline limitada
- La sesión offline tiene una duración máxima definida (valor exacto: PENDIENTE).
- Al expirar, el cobrador debe reconectarse para obtener un nuevo token.
- El servidor puede revocar tokens antes de su expiración (revocación de dispositivos: PENDIENTE de implementar).

### 4. Mínimo privilegio
- Cada usuario tiene acceso únicamente a los datos y funciones que su rol permite.
- Un cobrador solo accede a su cartera asignada.
- Un administrador accede a la gestión del sistema, no a funciones de cobrador.
- La API aplica control de acceso en cada endpoint.

### 5. Sin secretos en Git
- Ningún secreto, contraseña, token ni clave privada se almacena en el repositorio.
- Los valores de ejemplo van en `.env.example`; los valores reales van en `.env` (ignorado por Git).
- Las claves de firma, certificados y credenciales de producción se gestionan fuera del repositorio.

### 6. Protección de datos locales
- La base de datos local del dispositivo Android (Room/SQLite) se evaluará para cifrado en reposo (SQLCipher u opción nativa).
- Las fotografías almacenadas localmente deben estar en el directorio privado de la app, no en almacenamiento público.

### 7. Auditoría
- Todas las operaciones significativas se registran con usuario, timestamp y acción.
- Los registros de auditoría no son modificables por los usuarios del sistema.
- Los logs de la aplicación no deben contener datos sensibles de personas (RUT, nombres, etc.).

### 8. Revocación futura de dispositivos
- Se contempla la posibilidad de revocar el acceso de un dispositivo específico sin deshabilitar al usuario.
- El mecanismo exacto (invalidación de tokens por dispositivo, lista de dispositivos autorizados) se definirá en una fase posterior.

## PENDIENTE

- Definir el mecanismo exacto de autenticación: JWT con tiempo de expiración + refresh token, o sesiones con cookie httpOnly.
- Definir duración del token de acceso y del período offline máximo.
- Evaluar y decidir solución de cifrado para la base de datos local Android.
- Diseñar el mecanismo de revocación de dispositivos.
- Definir política de contraseñas (longitud mínima, complejidad, rotación).
- Evaluar si se usa 2FA para administradores.
- Definir política de retención de logs de auditoría.
- Revisar cumplimiento con Ley de Protección de Datos Personales (Ley 19.628 y sus modificaciones vigentes en Chile).
