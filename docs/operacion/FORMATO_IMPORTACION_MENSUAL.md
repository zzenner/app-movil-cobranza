# Formato del archivo CSV de importación mensual

**Versión del contrato:** v2 (Fase 5D — definitivo)

## Descripción general

El archivo de importación mensual es un CSV con separador `;`, codificación **UTF-8 estricto**, que contiene una fila por cuota. Un crédito con N cuotas ocupa N filas. Una persona con varios créditos ocupa la suma de todas sus cuotas.

## Estructura

El archivo debe tener exactamente las siguientes **26 columnas** en la primera fila (encabezado), en cualquier orden y sin distinguir mayúsculas:

| Columna | Tipo | Requerido | Descripción |
|---|---|---|---|
| `PERIODO` | Texto | Sí | Período de importación en formato `YYYY-MM`. Mes válido 01–12. |
| `RUT_NUMERO` | Entero | Sí | Parte numérica del RUT sin puntos ni dígito verificador |
| `RUT_DV` | Texto | Sí | Dígito verificador (0–9 o K). Se valida con módulo 11. |
| `NOMBRE_PERSONA` | Texto | Sí | Nombre completo del deudor |
| `OPERACION_NUMERO` | Texto | Sí | Número de operación/crédito. **No debe contener notación científica** (ej: `6,00403E+11` indica truncamiento en Excel) |
| `OPERACION_TIPO` | Texto | Sí | Tipo de crédito (ej: `Credito de Consumo`, `Credito Hipotecario`) |
| `OPERACION_ESTADO` | Texto | Sí | Estado del crédito (`VIGENTE`, `VENCIDO`, `CASTIGADO`) |
| `OPERACION_CAPITAL` | Decimal | Sí | Capital total de la operación |
| `OPERACION_INTERES_PENAL` | Decimal | Sí | Interés penal acumulado de la operación |
| `OPERACION_GASTOS` | Decimal | Sí | Gastos de cobranza de la operación |
| `OPERACION_TOTAL_VIGENTE` | Decimal | No | Total vigente de la operación |
| `OPERACION_FECHA_VTO` | Fecha | Sí | Fecha de vencimiento de la operación (`YYYY-MM-DD`) |
| `CUOTA_NUMERO` | Entero | Sí | Número correlativo de la cuota dentro de la operación |
| `CUOTA_ESTADO` | Texto | **Sí** | Estado de la cuota. Requerido. Valores típicos: `VIGENTE`, `VENCIDA`, `FUTURA`, `PAGADA` |
| `CUOTA_MONTO_TOTAL` | Decimal | Sí | Monto total de la cuota |
| `CUOTA_CAPITAL` | Decimal | No | Capital de la cuota |
| `CUOTA_INTERES` | Decimal | No | Interés ordinario de la cuota |
| `CUOTA_INTERES_PENAL` | Decimal | Sí | Interés penal de la cuota |
| `CUOTA_GASTOS` | Decimal | Sí | Gastos de la cuota |
| `CUOTA_SALDO` | Decimal | No | Saldo pendiente de la cuota |
| `CUOTA_FECHA_VTO` | Fecha | Sí | Fecha de vencimiento de la cuota (`YYYY-MM-DD`) |
| `CODIGO_EJECUTIVO` | Texto | Sí | Código del ejecutivo en el sistema origen (ej: `1001`, `2477`). Se resuelve a usuario interno vía campo `codigo_ejecutivo_origen` en la tabla `usuarios`. |
| `DIR_PARTICULAR` | Texto | Sí | Dirección domicilio del deudor. Se almacena como tipo `DOMICILIO`, principal. |
| `DIR_COMERCIAL` | Texto | No | Dirección comercial del deudor. Se almacena como tipo `COMERCIAL` si está presente. |
| `CODIGO_CARTERA` | Texto | Sí | Código de la cartera según catálogo. Valores válidos: `1` (Temprana), `2` (Vigente), `3` (Vigente Judicial), `4` (Castigada). |
| `MARCA_JUDICIAL` | Texto | Sí | Indica si el caso tiene marca judicial. Valores: `S` o `N`. Independiente de `CODIGO_CARTERA`. |

**Total: 26 columnas**

## Reglas de formato

- **Separador**: `;` (punto y coma)
- **Codificación**: **UTF-8 estricto**. El sistema rechaza archivos no UTF-8 con error `ENCODING_INVALIDO`. No se acepta CP850 ni ISO-8859-1.
- **BOM UTF-8**: Se acepta y descarta automáticamente.
- **Decimales**: punto `.` o coma `,` como separador decimal (ambos se aceptan)
- **Fechas**: `YYYY-MM-DD` (año-mes-día con guiones)
- **Sin comillas obligatorias**: los valores de texto no requieren comillas a menos que contengan el separador

## Reglas de negocio

- El dígito verificador del RUT se valida con módulo 11. Filas con DV inválido se rechazan con error `RUT_INVALIDO_MODULO_11`.
- `OPERACION_NUMERO` es la clave natural de importación junto con `sistema_origen`. Se rechaza si contiene notación científica (`OPERACION_NUMERO_NOTACION_CIENTIFICA`).
- `PERIODO` define la dimensión temporal: un mismo ejecutivo puede tener distintos clientes en distintos periodos sin generar error de inconsistencia.
- `CODIGO_EJECUTIVO` se resuelve contra el campo `codigo_ejecutivo_origen` de la tabla `usuarios`. Si el código no existe, la persona queda vinculada a la cartera sin asignación mensual. **No se crean usuarios automáticamente.**
- `CODIGO_CARTERA` se resuelve contra el catálogo interno por `codigo_origen`. Si el código no está en el catálogo (`1`–`4`), la fila se rechaza. **No se crean carteras automáticamente.**
- Un cliente puede aparecer con distintos ejecutivos entre archivos del mismo periodo, pero no dentro del mismo archivo para el mismo `PERIODO+RUT+CARTERA` (validación `PERSONA_EJECUTIVOS_MULTIPLES`).
- La clave de posición que previene duplicados dentro del archivo es `PERIODO+RUT+OPERACION+CUOTA+CARTERA`.
- Una persona puede tener solo una cartera activa a la vez. Si la importación la asigna a una cartera distinta, la anterior se desactiva.
- Filas completamente vacías (26 campos vacíos o en blanco) se ignoran silenciosamente. Se cuentan en `totalFilas`.

## Catálogo de carteras

| `CODIGO_CARTERA` | Nombre | UUID interno |
|---|---|---|
| `1` | Temprana | `00000000-0000-0000-0001-000000000001` |
| `2` | Vigente | `00000000-0000-0000-0001-000000000002` |
| `3` | Vigente Judicial | `00000000-0000-0000-0001-000000000003` |
| `4` | Castigada | `00000000-0000-0000-0001-000000000004` |

## Ejemplo mínimo

```
PERIODO;RUT_NUMERO;RUT_DV;NOMBRE_PERSONA;OPERACION_NUMERO;OPERACION_TIPO;OPERACION_ESTADO;OPERACION_CAPITAL;OPERACION_INTERES_PENAL;OPERACION_GASTOS;OPERACION_TOTAL_VIGENTE;OPERACION_FECHA_VTO;CUOTA_NUMERO;CUOTA_ESTADO;CUOTA_MONTO_TOTAL;CUOTA_CAPITAL;CUOTA_INTERES;CUOTA_INTERES_PENAL;CUOTA_GASTOS;CUOTA_SALDO;CUOTA_FECHA_VTO;CODIGO_EJECUTIVO;DIR_PARTICULAR;DIR_COMERCIAL;CODIGO_CARTERA;MARCA_JUDICIAL
2026-08;12345678;5;JUAN PEREZ ROJAS;600001000001;Credito de Consumo;VIGENTE;1000000.00;0.00;0.00;1020000.00;2027-12-31;1;VIGENTE;50000.00;45000.00;3000.00;1500.00;500.00;50000.00;2026-09-30;1001;AV LIBERTADOR BERNARDO OHIGGINS 1234 SANTIAGO;;2;N
```

## Diferencias respecto al contrato v1 (Fase 5C)

| Aspecto | Contrato v1 (Fase 5C) | Contrato v2 (Fase 5D) |
|---|---|---|
| Columnas | 24 | 26 |
| Encoding | CP850 / ISO-8859-1 | UTF-8 estricto |
| Fechas | DD-MM-YYYY | YYYY-MM-DD |
| Período | Parámetro HTTP | Columna `PERIODO` en CSV |
| Cartera | Parámetro HTTP | Columna `CODIGO_CARTERA` por fila |
| Ejecutivo | `EJECUTIVO_USERNAME` + `NOMBRE_EJECUTIVO` | `CODIGO_EJECUTIVO` (código origen) |
| CUOTA_ESTADO | Opcional (default VIGENTE) | **Requerido** |
| Filas vacías | No especificado | Ignoradas silenciosamente |

## Límites

- Tamaño máximo del archivo: **50 MB**
- El archivo debe tener extensión `.csv`
- El procesamiento se hace en lotes por RUT para gestión de memoria

## Errores comunes

| Código de error | Descripción |
|---|---|
| `ENCODING_INVALIDO` | El archivo no está en UTF-8. El sistema origen debe exportar en UTF-8. |
| `COLUMNA_REQUERIDA_FALTANTE` | Una columna obligatoria no está en el encabezado |
| `COLUMNA_DESCONOCIDA` | Una columna del archivo no pertenece al contrato (advertencia, no bloquea) |
| `RUT_INVALIDO_MODULO_11` | El dígito verificador del RUT no corresponde al módulo 11 |
| `FORMATO_PERIODO_INVALIDO` | `PERIODO` no tiene formato YYYY-MM o el mes es inválido (01–12) |
| `CODIGO_CARTERA_INVALIDO` | `CODIGO_CARTERA` no es 1, 2, 3 ni 4 |
| `MARCA_JUDICIAL_INVALIDA` | `MARCA_JUDICIAL` no es S ni N |
| `OPERACION_NUMERO_NOTACION_CIENTIFICA` | `OPERACION_NUMERO` contiene notación científica y puede haber perdido precisión |
| `OPERACION_DATOS_INCONSISTENTES` | Misma operación aparece con tipo inconsistente en distintas filas |
| `PERSONA_EJECUTIVOS_MULTIPLES` | La misma persona (PERIODO+RUT+CARTERA) tiene más de un ejecutivo en el archivo |
| `POSICION_DUPLICADA` | Dos filas con el mismo PERIODO+RUT+OPERACION+CUOTA+CARTERA |
| `CAMPO_REQUERIDO` | Un campo obligatorio está vacío |
| `FORMATO_DECIMAL` | Un campo decimal no puede convertirse a número |
| `FORMATO_FECHA` | Una fecha no tiene el formato `YYYY-MM-DD` o no es una fecha válida |
| `FORMATO_ENTERO` | Un campo entero no puede convertirse a número |
