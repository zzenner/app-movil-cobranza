# Formato del archivo CSV de importación mensual

## Descripción general

El archivo de importación mensual es un CSV con separador `;`, codificación UTF-8 (con o sin BOM), que contiene una fila por cuota. Un crédito con N cuotas ocupa N filas. Una persona con varios créditos ocupa la suma de todas sus cuotas.

## Estructura

El archivo debe tener exactamente las siguientes columnas en la primera fila (encabezado), en cualquier orden y sin distinguir mayúsculas:

| Columna | Tipo | Descripción |
|---|---|---|
| `RUT_NUMERO` | Entero | Parte numérica del RUT sin puntos ni dígito verificador |
| `RUT_DV` | Texto | Dígito verificador (0–9 o K) |
| `NOMBRE_PERSONA` | Texto | Nombre completo del deudor |
| `COD_EXT_PERSONA` | Texto | Identificador de la persona en el sistema externo |
| `DIRECCION_TEXTO` | Texto | Dirección completa del deudor |
| `DIRECCION_TIPO` | Texto | Tipo de dirección (`DOMICILIO`, `TRABAJO`, `OTRO`) |
| `DIRECCION_COMUNA` | Texto | Comuna |
| `DIRECCION_CIUDAD` | Texto | Ciudad |
| `COD_EXT_DIRECCION` | Texto | Identificador de la dirección en el sistema externo |
| `OPERACION_NUMERO` | Texto | Número de operación/crédito |
| `OPERACION_ID_EXT` | Texto | Identificador de la operación en el sistema externo |
| `OPERACION_TIPO` | Texto | Tipo de crédito (ej: `CREDITO_CONSUMO`, `CREDITO_HIPOTECARIO`) |
| `OPERACION_ESTADO` | Texto | Estado (`VIGENTE`, `VENCIDO`, `CASTIGADO`) |
| `OPERACION_CAPITAL` | Decimal | Capital total de la operación |
| `OPERACION_INTERES_PENAL` | Decimal | Interés penal acumulado de la operación |
| `OPERACION_GASTOS` | Decimal | Gastos de cobranza de la operación |
| `OPERACION_TOTAL_VIGENTE` | Decimal | Total vigente de la operación |
| `OPERACION_FECHA_VTO` | Fecha | Fecha de vencimiento de la operación (`YYYY-MM-DD`) |
| `CUOTA_NUMERO` | Entero | Número correlativo de la cuota dentro de la operación |
| `CUOTA_ID_EXT` | Texto | Identificador de la cuota en el sistema externo |
| `CUOTA_ESTADO` | Texto | Estado de la cuota (`VIGENTE`, `VENCIDA`, `FUTURA`, `PAGADA`) |
| `CUOTA_MONTO_TOTAL` | Decimal | Monto total de la cuota |
| `CUOTA_CAPITAL` | Decimal | Capital de la cuota |
| `CUOTA_INTERES` | Decimal | Interés ordinario de la cuota |
| `CUOTA_INTERES_PENAL` | Decimal | Interés penal de la cuota |
| `CUOTA_GASTOS` | Decimal | Gastos de la cuota |
| `CUOTA_SALDO` | Decimal | Saldo pendiente de la cuota |
| `CUOTA_FECHA_VTO` | Fecha | Fecha de vencimiento de la cuota (`YYYY-MM-DD`) |
| `EJECUTIVO_USERNAME` | Texto | `nombre_usuario` del ejecutivo asignado (debe existir en el sistema) |

**Total: 29 columnas**

## Reglas de formato

- **Separador**: `;` (punto y coma)
- **Codificación**: UTF-8 (se acepta BOM de 3 bytes)
- **Decimales**: punto `.` o coma `,` como separador decimal (ambos se aceptan)
- **Fechas**: `YYYY-MM-DD`
- **Sin comillas obligatorias**: los valores de texto no requieren comillas a menos que contengan el separador

## Reglas de negocio

- El dígito verificador del RUT se valida con módulo 11. Filas con DV inválido se rechazan.
- El campo `COD_EXT_PERSONA` debe ser consistente: el mismo RUT siempre debe usar el mismo `COD_EXT_PERSONA` dentro del archivo.
- El campo `EJECUTIVO_USERNAME` debe coincidir con un usuario activo con rol `EJECUTIVO_TERRENO` en el sistema.
- El supervisor se asigna automáticamente desde la tabla `supervision_usuarios` (no viene en el CSV).
- Una persona puede tener solo una cartera activa a la vez. Si la importación asigna a una persona a una cartera distinta, la anterior se desactiva.
- No se incluye `CUOTA_FECHA_PAGO` en este MVP (campo reservado para versiones futuras).

## Ejemplo mínimo

```
RUT_NUMERO;RUT_DV;NOMBRE_PERSONA;COD_EXT_PERSONA;DIRECCION_TEXTO;DIRECCION_TIPO;DIRECCION_COMUNA;DIRECCION_CIUDAD;COD_EXT_DIRECCION;OPERACION_NUMERO;OPERACION_ID_EXT;OPERACION_TIPO;OPERACION_ESTADO;OPERACION_CAPITAL;OPERACION_INTERES_PENAL;OPERACION_GASTOS;OPERACION_TOTAL_VIGENTE;OPERACION_FECHA_VTO;CUOTA_NUMERO;CUOTA_ID_EXT;CUOTA_ESTADO;CUOTA_MONTO_TOTAL;CUOTA_CAPITAL;CUOTA_INTERES;CUOTA_INTERES_PENAL;CUOTA_GASTOS;CUOTA_SALDO;CUOTA_FECHA_VTO;EJECUTIVO_USERNAME
12345678;5;JUAN PEREZ ROJAS;EXT-P-001;AV LIBERTADOR 1234;DOMICILIO;SANTIAGO;SANTIAGO;EXT-D-001;OP-001;EXT-OP-001;CREDITO_CONSUMO;VIGENTE;1000000,00;15000,00;5000,00;1020000,00;2027-12-31;1;EXT-CTA-001;VIGENTE;50000,00;45000,00;3000,00;1500,00;500,00;50000,00;2026-09-30;jlopez
```

## Límites

- Tamaño máximo del archivo: **50 MB**
- No hay límite de filas en el contrato, pero el procesamiento se hace en lotes de 100 para gestión de memoria

## Errores comunes

| Código de error | Descripción |
|---|---|
| `RUT_DV_INVALIDO` | El dígito verificador del RUT no corresponde al módulo 11 |
| `COD_EXT_PERSONA_INCONSISTENTE` | El mismo RUT aparece con dos COD_EXT_PERSONA distintos |
| `EJECUTIVO_NO_ENCONTRADO` | El nombre de usuario del ejecutivo no existe en el sistema |
| `EJECUTIVO_INACTIVO` | El ejecutivo existe pero está inactivo o bloqueado |
| `COLUMNA_REQUERIDA_AUSENTE` | Una columna obligatoria no está en el encabezado |
| `VALOR_DECIMAL_INVALIDO` | Un campo decimal no puede convertirse a número |
| `FECHA_INVALIDA` | Una fecha no tiene el formato `YYYY-MM-DD` o no es una fecha válida |
