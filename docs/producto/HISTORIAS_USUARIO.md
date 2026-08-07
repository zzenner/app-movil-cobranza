# Historias de usuario

Las historias siguen el formato: "Como [rol], quiero [acción], para [valor]."
Estado: BORRADOR — pendiente de refinamiento y estimación.

## Módulo de autenticación

### HU-001 — Login del cobrador
**Como** cobrador, **quiero** iniciar sesión con mis credenciales institucionales, **para** acceder a mi cartera y registrar gestiones en terreno.

**Criterios de aceptación:**
- Puedo iniciar sesión con usuario y contraseña.
- Si mis credenciales son incorrectas, recibo un mensaje de error claro.
- Al autenticarme correctamente, se descarga mi cartera actual.
- La sesión persiste offline por el tiempo acordado.

**PENDIENTE:** Duración exacta de la sesión offline.

---

### HU-002 — Sesión offline del cobrador
**Como** cobrador, **quiero** seguir usando la app sin conexión a internet después de haber iniciado sesión, **para** registrar gestiones aunque esté en zonas sin cobertura.

**Criterios de aceptación:**
- Puedo consultar mi cartera y registrar gestiones sin conexión.
- Recibo una notificación cuando la sesión offline está próxima a expirar.
- Al expirar, se me solicita reconectarme.

---

## Módulo de carteras

### HU-003 — Ver mi cartera asignada
**Como** cobrador, **quiero** ver la lista de personas y créditos de mi cartera asignada, **para** planificar y ejecutar mis gestiones.

**Criterios de aceptación:**
- Veo las personas asignadas a mi cartera con sus datos principales.
- Puedo buscar una persona por RUT.
- Los datos están disponibles offline después de la última sincronización.

---

### HU-004 — Detalle de persona
**Como** cobrador, **quiero** ver el detalle de una persona de mi cartera (créditos y cuotas), **para** conocer su situación de deuda antes de visitarla.

**Criterios de aceptación:**
- Veo los créditos vigentes de la persona.
- Para cada crédito veo el detalle de cuotas (número, monto, fecha de vencimiento, estado).
- La información está disponible offline.

---

## Módulo de gestiones

### HU-005 — Registrar gestión
**Como** cobrador, **quiero** registrar el resultado de un contacto con un deudor, **para** dejar constancia de la gestión realizada.

**Criterios de aceptación:**
- Puedo seleccionar el tipo de resultado de la gestión desde un catálogo.
- Puedo ingresar observaciones de texto libre.
- Puedo registrar opcionalmente un compromiso de pago con fecha de compromiso (sin monto).
- La geolocalización se captura en el momento del registro (obligatoria; sin GPS no se puede guardar).
- La gestión queda registrada localmente aunque no haya conexión.

**PENDIENTE:** Catálogo de resultados.
**DIFERIDO (ADR-0030):** Fotografías opcionales — fuera del alcance de la Fase 4C.

---

### HU-006 — Historial de gestiones
**Como** cobrador, **quiero** ver el historial de gestiones que he realizado, **para** recordar lo que acordé con cada deudor.

**Criterios de aceptación:**
- Veo mis gestiones ordenadas por fecha descendente.
- Puedo filtrar por persona.
- Las gestiones no sincronizadas se muestran con indicador de estado pendiente.

---

## Módulo de administración web

### HU-007 — Gestión de usuarios (administrador)
**Como** administrador, **quiero** crear y gestionar usuarios del sistema, **para** dar acceso a cobradores y administradores.

**Criterios de aceptación — Fase 5B-1 ✅ ENTREGADA (solo lectura):**
- Puedo ver el listado de usuarios con sus roles, estado calculado y supervisor.
- Puedo filtrar por nombre de usuario, estado y rol.
- Puedo ver el detalle completo de un usuario: roles vigentes, permisos efectivos, historial de bloqueo y supervisor asignado.
- El acceso está restringido al permiso `USUARIOS_VER` (JEFE_SUPERVISORES, TECNOLOGIA).
- No se exponen contraseñas, tokens de sesión ni identificadores de dispositivo.

**Criterios de aceptación — Fase 5B-2 ✅ COMPLETADA:**
- ✅ Puedo crear un usuario con nombre, correo, rol y contraseña inicial.
- ✅ Puedo editar los datos básicos de un usuario existente (con protección de concurrencia por campo `version`).
- ✅ Puedo activar o desactivar un usuario sin eliminarlo.
- ✅ Los usuarios desactivados no pueden iniciar sesión.
- ✅ Puedo bloquear y desbloquear un usuario de forma independiente al estado activo/inactivo.
- ✅ Puedo restablecer la contraseña de un usuario.
- ✅ El sistema impide dejar el sistema sin administrador activo (último administrador protegido).
- ✅ No puedo desactivarme ni bloquearme a mí mismo.
- ✅ Las operaciones de seguridad (desactivar, bloquear, restablecer-contraseña) revocan las sesiones activas del usuario afectado.

---

### HU-008 — Asignar cartera a cobrador (administrador)
**Como** administrador, **quiero** asignar una cartera de cobranza a un cobrador, **para** que pueda gestionarla en terreno.

**Criterios de aceptación:**
- Puedo seleccionar un cobrador y asignarle una cartera.
- El cobrador recibe la cartera en su próxima sincronización.
- Puedo ver qué carteras están asignadas y a quién.

---

### HU-009 — Ver gestiones del equipo (administrador)
**Como** administrador, **quiero** ver las gestiones registradas por los cobradores, **para** supervisar el trabajo en terreno.

**Criterios de aceptación:**
- Veo todas las gestiones del sistema con filtro por cobrador, fecha y estado.
- Puedo ver el detalle de cada gestión incluyendo fotografías.
- La geolocalización de la gestión se puede visualizar en un mapa (PENDIENTE: ¿mapa integrado?).

---

---

### HU-010 — Buscar y gestionar persona fuera de la asignación diaria (Fase 4C-B ✅)
**Como** cobrador, **quiero** buscar una persona por RUT aunque no esté en mi asignación diaria, **para** registrar una gestión cuando me encuentro con un deudor de forma no planificada.

**Criterios de aceptación:**
- Puedo acceder a la pantalla de búsqueda desde el menú principal.
- Ingresan el número de RUT y el dígito verificador por separado.
- Si el RUT es válido y la persona existe, veo sus datos y puedo registrar una gestión.
- La gestión se registra con `origenGestion = BUSQUEDA_DIRECTA` (sin `asignacionDiariaId`).
- Puedo registrar la gestión aunque pierda conexión después de la búsqueda (los datos de la persona quedan guardados localmente).
- Si el RUT no existe en el sistema, recibo un mensaje claro de "no encontrado".
- Si el RUT tiene un dígito verificador incorrecto, recibo un mensaje de validación antes de consultar la API.

**Implementado en:** `:feature:busqueda`, `POST /api/v1/personas/busquedas`, Room `persona_directa`. Ver ADR-0041, ADR-0042.

---

## PENDIENTE

- HU pendientes de identificar: reportes, corrección de gestiones, reasignación de carteras.
- Definir si el cobrador puede ver gestiones de otros cobradores sobre el mismo deudor.
- Definir si el administrador puede corregir o anular gestiones.
