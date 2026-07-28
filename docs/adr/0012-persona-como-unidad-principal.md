# ADR-0012 — Persona como unidad principal de gestión

## Estado
Aceptado.

## Contexto
En cobranza, la deuda puede modelarse desde diferentes perspectivas: el crédito (operación), la cuota vencida, el cliente (persona), o un caso de cobranza agregado. Antes de implementar el modelo de datos, se necesita decidir cuál es la entidad central del sistema.

## Decisión
La **persona** es la unidad principal de gestión. El cobrador visita personas, no operaciones ni créditos. La jerarquía de datos es:

```
cartera → persona → operaciones → cuotas
```

No existe una entidad `caso_cobranza`. Las gestiones se registran sobre personas, no sobre operaciones individuales.

## Consecuencias

**Positivas:**
- Refleja el flujo real de trabajo en terreno: el cobrador va a visitar a una persona, que puede tener varios créditos.
- Simplifica el modelo de la app móvil: una pantalla por persona con todas sus operaciones.
- Las gestiones genéricas (sin contacto, contacto familiar) no requieren asociar a una operación específica.
- El historial de gestiones se consulta por RUT de persona, que es el identificador natural del negocio.

**Negativas:**
- Los `COMPROMISO_PAGO` no se asocian a una cuota o crédito específico (por decisión de negocio). Si en el futuro se requiere trazabilidad por operación, se deberá añadir la relación.
- Las métricas de gestión se agregan por persona, lo que puede dificultar reportes de efectividad por línea de crédito.

## Alternativas consideradas

**Caso de cobranza:** Una entidad `caso_cobranza` que agrupa operaciones bajo gestión activa. Más flexible, pero añade una capa de abstracción que no representa una entidad del negocio actual. Se descartó para el MVP.

**Operación como unidad:** Cada gestión se asocia a una operación específica. Más granular, pero contradice el flujo de trabajo real donde el cobrador visita a la persona (no al crédito). Se descartó.

## Referencias

- `docs/dominio/REGLAS_NEGOCIO.md` — RN-01, RN-02
- `docs/dominio/MODELO_DOMINIO.md` — Jerarquía principal
