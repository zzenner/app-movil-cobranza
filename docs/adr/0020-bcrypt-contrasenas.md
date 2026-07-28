# ADR-0020: BCrypt para hashing de contraseñas, costo configurable

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

Las contraseñas de usuarios deben almacenarse de forma segura. Se debe elegir un algoritmo de hashing adecuado y definir la estrategia de configuración para distintos entornos.

## Decisión

Se usa BCrypt (implementado via `spring-security-crypto`, sin Spring Security Web completo) con las siguientes características:

- **Costo en producción**: 12 (configurable via `security.bcrypt.strength`)
- **Costo en pruebas**: 4 (via `@TestPropertySource(properties = "security.bcrypt.strength=4")`)
- **Interfaz pública**: `CodificadorContrasena` (puerto en `usuarios.aplicacion`)
- **Adaptador**: `BcryptCodificadorContrasena` (package-private en `usuarios.infraestructura`)

El hash **nunca** se incluye en `toString()`, logs, respuestas HTTP ni mensajes de error.

## Consecuencias

**Positivas**:
- BCrypt incorpora salt aleatorio por diseño — dos hashes del mismo valor son distintos.
- Resistente a ataques de fuerza bruta por el factor de costo ajustable.
- La interfaz `CodificadorContrasena` permite sustituir el algoritmo sin cambiar el código de aplicación.
- El costo bajo en pruebas (4) mantiene los tests de integración en tiempo razonable.

**Negativas**:
- BCrypt está limitado a contraseñas de máximo 72 bytes (suficiente para el caso de uso).
- Costo 12 puede generar latencia notable (~250ms) en login; aceptable para el volumen esperado.

## Alternativas consideradas

- **Argon2**: Más moderno, pero requiere dependencia nativa adicional. Se puede migrar en el futuro.
- **SCrypt**: Similar ventajas a Argon2; misma razón de descarte.
- **PBKDF2**: Compatible con FIPS; descartado por menor resistencia relativa a BCrypt con parámetros equivalentes.
