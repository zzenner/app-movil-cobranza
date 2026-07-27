# ADR-0007 — RUT almacenado como rut_numero y rut_dv

## Estado
Aceptado.

## Contexto
El RUT chileno tiene dos partes: una parte numérica (hasta 8 dígitos) y un dígito verificador (0–9 o K). Se necesita decidir cómo almacenar el RUT en la base de datos y en los modelos de dominio.

## Decisión
Almacenar el RUT en dos columnas separadas: `rut_numero` (VARCHAR) para la parte numérica y `rut_dv` (VARCHAR(1)) para el dígito verificador. La clave de unicidad se aplica sobre la combinación `(rut_numero, rut_dv)`.

## Consecuencias

**Positivas:**
- Separación clara de las dos partes del RUT, que tienen naturaleza diferente (número vs. carácter de control).
- Facilita la validación del dígito verificador de forma independiente.
- Permite ordenar, filtrar y comparar por parte numérica sin manipulación de strings.
- Evita ambigüedades de formato (con puntos, sin puntos, con guión, sin guión): cada parte se almacena limpia.
- Búsquedas por RUT parcial (solo número) son más directas.

**Negativas:**
- Para mostrar el RUT completo en la interfaz, se requiere concatenar las dos partes (lógica en la capa de presentación).
- Los endpoints de búsqueda por RUT deben aceptar ambas partes o el RUT ya parseado.

## Alternativas consideradas

**RUT como string único sin puntos:** `"12345678K"`. Más simple, pero mezcla la parte numérica y el DV, dificultando validación y comparación. Se descartó.

**RUT como string con formato estándar con guión:** `"12345678-K"`. Mismo problema; además requiere normalización de entrada. Se descartó.

**RUT como entero + char:** El número como `BIGINT` y el DV como `CHAR(1)`. Posible, pero VARCHAR ofrece la misma funcionalidad sin pérdida y con más flexibilidad para el dígito `K`. Se descartó por no añadir ventaja significativa.
