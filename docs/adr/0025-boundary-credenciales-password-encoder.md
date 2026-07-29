# ADR-0025: Frontera de credenciales y PasswordEncoder compartido

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

El módulo `autenticacion` necesita verificar contraseñas y el módulo `usuarios` necesita codificarlas. Ambos necesitan el mismo `PasswordEncoder` para que las operaciones sean compatibles. Se requiere evitar la exposición accidental del hash de contraseña.

## Decisión

Se define un único bean `PasswordEncoder` (BCrypt, costo 12 en producción, costo 4 en tests) en `SeguridadBaseConfig` a nivel raíz de la aplicación. Ambos módulos lo inyectan.

La clase `CredencialesUsuario` en `usuarios::api` es `final`, no es un `record`, tiene `@JsonIgnoreType` (impide serialización JSON), y `toString()` excluye el hash. Solo puede ser consumida desde código de autenticación, no desde controladores.

El módulo `autenticacion` accede al hash únicamente a través de `UsuarioConsultaApi.buscarParaAutenticacion()` y `buscarCredencialesPorId()`. El hash nunca viaja por HTTP ni aparece en logs.

## Consecuencias

- Si en el futuro se cambia el algoritmo de hashing, debe actualizarse el único bean `PasswordEncoder`.
- El contrato de `CredencialesUsuario` como clase sensible debe respetarse: nunca retornarla desde un endpoint.
- CSRF deshabilitado porque Bearer tokens en `Authorization` header no son vulnerables a CSRF. Si en el futuro se implementa autenticación por cookie HttpOnly (para Angular admin web), CSRF debe habilitarse.
