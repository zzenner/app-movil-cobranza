# ADR-0046: Política de acceso a consultas administrativas de usuarios

**Estado:** Aprobado
**Fecha:** 2026-08-05
**Fase:** 5B-1
**Decisores:** Equipo Tecnología

---

## Contexto

La Fase 5B-1 introduce los endpoints de consulta administrativa de usuarios:

- `GET /api/v1/admin/usuarios` — listado paginado
- `GET /api/v1/admin/usuarios/{id}` — detalle

Era necesario definir qué roles del sistema tienen acceso y bajo qué modelo de autorización.

Los roles del sistema son:
- **JEFE_SUPERVISORES** — posee todos los permisos
- **TECNOLOGIA** — posee `USUARIOS_VER`, `USUARIOS_ADMINISTRAR`, `SINCRONIZACION_VER`
- **SUPERVISOR** — posee `ASIGNACIONES_VER`, `ASIGNACIONES_ADMINISTRAR`, `GESTIONES_VER`
- **EJECUTIVO_TERRENO** — posee `GESTIONES_CREAR`

---

## Decisión

### 1. Autorización basada en permiso, no en rol

Los endpoints de consulta de usuarios utilizan el permiso `USUARIOS_VER` como autoridad
de acceso. La comprobación en la API es:

```java
@PreAuthorize("hasAuthority('PERM_USUARIOS_VER')")
```

El Angular controla la visibilidad del menú y la ruta `/usuarios` mediante el permiso
`USUARIOS_VER` del perfil, no mediante el nombre de rol.

### 2. Acceso por rol derivado del permiso existente

- **JEFE_SUPERVISORES:** acceso — posee `USUARIOS_VER` (todos los permisos)
- **TECNOLOGIA:** acceso — posee `USUARIOS_VER` explícitamente
- **SUPERVISOR:** sin acceso — no posee `USUARIOS_VER`
- **EJECUTIVO_TERRENO:** sin acceso — no posee `USUARIOS_VER`

### 3. SUPERVISOR no recibe USUARIOS_VER

No se agrega el permiso `USUARIOS_VER` al rol SUPERVISOR. El supervisor no necesita
visibilidad del directorio de usuarios para realizar su trabajo (publicar asignaciones
diarias para ejecutivos).

Si en el futuro un supervisor necesita ver solo a los ejecutivos de su equipo, se creará
un permiso específico (`USUARIOS_EQUIPO_VER` o similar) y se implementará lógica de
filtrado por equipo. Ese trabajo pertenece a una fase posterior y requiere diseño propio.

### 4. Prohibición de autorización ad hoc

No se implementa ninguna forma de autorización que no pase por el modelo de permisos
formal (tabla `cobranza.rol_permisos`). En particular:

- No se inspecciona el campo `supervision_usuarios` para determinar acceso.
- No se agrega lógica especial basada en si el usuario autenticado tiene ejecutivos a cargo.
- Cualquier acceso futuro limitado por equipo requerirá un nuevo permiso explícito.

---

## Consecuencias

### Positivas

- La lógica de autorización es uniforme y verificable en la BD.
- El Spring Security `@PreAuthorize` con `hasAuthority` es la única fuente de decisión.
- El Angular no necesita comparar roles sino solo verificar si el array `permisos` incluye `USUARIOS_VER`.
- La matriz rol-permiso en `V003` es la autoridad; no hay permisos hardcodeados en código.

### Negativas / riesgos

- SUPERVISOR no puede ver usuarios; si hay requerimientos futuros de gestión de equipo,
  requerirán un permiso nuevo y una fase de desarrollo adicional.

---

## Deuda técnica registrada

- `USUARIOS_EQUIPO_VER` — permiso futuro para supervisores que necesiten ver solo su equipo.
  Ver `docs/gestion/DEUDA_TECNICA.md`.

- Auditoría administrativa persistente pendiente: el módulo `auditoria` no tiene API adecuada.
  Mientras tanto, los accesos a endpoints protegidos son visibles en logs de Spring Security,
  pero no se persisten en tabla de auditoría. Ver deuda técnica registrada en fase 5B-1.

---

## Alternativas descartadas

| Alternativa | Razón de descarte |
|---|---|
| Agregar `USUARIOS_VER` a SUPERVISOR | Requeriría autorización de negocio; el supervisor no necesita el directorio completo |
| Autorización basada en `supervision_usuarios` | Lógica ad hoc fuera del modelo de permisos; viola la invariante del sistema |
| Nueva tabla de acceso por función | Sobrediseño para el alcance actual |
