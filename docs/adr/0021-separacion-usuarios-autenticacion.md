# ADR-0021: Separación entre módulo `usuarios` y módulo `autenticacion`

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

El modelo de datos de usuarios (entidad, roles, permisos, dispositivos) y la lógica de autenticación (login, JWT, refresh tokens) son responsabilidades distintas. Se debe decidir si coexisten en un mismo módulo o se separan.

## Decisión

Se implementan como módulos separados:

- **`usuarios`**: Propietario de la entidad `Usuario`, roles, permisos y dispositivos. Expone `UsuarioConsultaApi` como `@NamedInterface("api")`.
- **`autenticacion`** (pendiente de implementar): Responsable de verificar credenciales, emitir tokens y validar sesiones. Dependerá de `usuarios::api`.

En la Fase 1C solo se implementa el módulo `usuarios`. El módulo `autenticacion` se implementará en Fase 2.

## Consecuencias

**Positivas**:
- `usuarios` no conoce nada de JWT ni sesiones — puede testearse en aislamiento.
- `autenticacion` puede cambiar el mecanismo de tokens sin afectar `usuarios`.
- El límite de módulo fuerza un contrato explícito: `autenticacion` solo puede llamar métodos de `UsuarioConsultaApi`.

**Negativas**:
- Requiere una interfaz explícita (`UsuarioConsultaApi`) para cruzar el límite de módulo.
- En Fase 1C el sistema no tiene aún flujo de autenticación funcional — solo el modelo de datos.

## Alternativas consideradas

- **Módulo único `seguridad`**: Descartado por mezclar responsabilidades de modelo de dominio con lógica de sesión/tokens.
