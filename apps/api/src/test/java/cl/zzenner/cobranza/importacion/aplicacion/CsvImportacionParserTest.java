package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.FilaCsv;
import cl.zzenner.cobranza.importacion.dominio.NivelError;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Tests en el paquete aplicacion para acceder al parser (package-private)
class CsvImportacionParserTest {

    private static final String HEADER =
            "PERIODO;RUT_NUMERO;RUT_DV;NOMBRE_PERSONA;" +
            "OPERACION_NUMERO;OPERACION_TIPO;OPERACION_ESTADO;" +
            "OPERACION_CAPITAL;OPERACION_INTERES_PENAL;OPERACION_GASTOS;OPERACION_TOTAL_VIGENTE;" +
            "OPERACION_FECHA_VTO;CUOTA_NUMERO;CUOTA_ESTADO;CUOTA_MONTO_TOTAL;" +
            "CUOTA_CAPITAL;CUOTA_INTERES;CUOTA_INTERES_PENAL;CUOTA_GASTOS;CUOTA_SALDO;" +
            "CUOTA_FECHA_VTO;CODIGO_EJECUTIVO;DIR_PARTICULAR;DIR_COMERCIAL;" +
            "CODIGO_CARTERA;MARCA_JUDICIAL";

    private static final String FILA_VALIDA =
            "2026-08;12345678;5;JUAN PEREZ ROJAS;" +
            "600001000001;Credito de Consumo;VIGENTE;" +
            "1000000;0;0;1020000;" +
            "2027-12-31;1;VIGENTE;50000;" +
            "45000;3000;1500;500;50000;" +
            "2026-09-30;1001;AV LIBERTADOR 1234 SANTIAGO;;" +
            "2;N";

    private final CsvImportacionParser parser = new CsvImportacionParser();
    private final UUID importacionId = UUID.randomUUID();

    // --- Test 1: CSV con 26 columnas → filas parseadas correctamente
    @Test
    void csv_con_26_columnas_es_parseado_correctamente() {
        String csv = HEADER + "\n" + FILA_VALIDA;
        var resultado = parsear(csv);

        assertThat(resultado.errores().stream()
                .filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas()).hasSize(1);

        FilaCsv fila = resultado.filas().get(0);
        assertThat(fila.periodo()).isEqualTo("2026-08");
        assertThat(fila.rutNumero()).isEqualTo("12345678");
        assertThat(fila.rutDv()).isEqualTo("5");
        assertThat(fila.nombrePersona()).isEqualTo("JUAN PEREZ ROJAS");
        assertThat(fila.operacionNumero()).isEqualTo("600001000001");
        assertThat(fila.operacionTipo()).isEqualTo("Credito de Consumo");
        assertThat(fila.codigoEjecutivo()).isEqualTo("1001");
        assertThat(fila.codigoCartera()).isEqualTo("2");
        assertThat(fila.marcaJudicial()).isEqualTo("N");
    }

    // --- Test 2: Columna requerida faltante → error COLUMNA_REQUERIDA_FALTANTE
    @Test
    void columna_requerida_faltante_genera_error() {
        // Header sin CODIGO_EJECUTIVO
        String headerSinEjec =
                "PERIODO;RUT_NUMERO;RUT_DV;NOMBRE_PERSONA;" +
                "OPERACION_NUMERO;OPERACION_TIPO;OPERACION_ESTADO;" +
                "OPERACION_CAPITAL;OPERACION_INTERES_PENAL;OPERACION_GASTOS;OPERACION_TOTAL_VIGENTE;" +
                "OPERACION_FECHA_VTO;CUOTA_NUMERO;CUOTA_ESTADO;CUOTA_MONTO_TOTAL;" +
                "CUOTA_CAPITAL;CUOTA_INTERES;CUOTA_INTERES_PENAL;CUOTA_GASTOS;CUOTA_SALDO;" +
                "CUOTA_FECHA_VTO;DIR_PARTICULAR;DIR_COMERCIAL;CODIGO_CARTERA;MARCA_JUDICIAL";
        String csv = headerSinEjec + "\n2026-08;x;x;x;x;x;x;0;0;0;0;2027-01-01;1;VIGENTE;0;0;0;0;0;0;2027-01-01;dir;;2;N";

        var resultado = parsear(csv);

        assertThat(resultado.errores()).anyMatch(e ->
                "COLUMNA_REQUERIDA_FALTANTE".equals(e.getCodigoError()) &&
                "CODIGO_EJECUTIVO".equals(e.getColumna()));
    }

    // --- Test 3: Columna desconocida → advertencia COLUMNA_DESCONOCIDA
    @Test
    void columna_desconocida_genera_advertencia() {
        String headerConExtra = HEADER + ";COLUMNA_EXTRA";
        String filaConExtra = FILA_VALIDA + ";valorExtra";
        String csv = headerConExtra + "\n" + filaConExtra;

        var resultado = parsear(csv);

        assertThat(resultado.errores()).anyMatch(e ->
                "COLUMNA_DESCONOCIDA".equals(e.getCodigoError()) &&
                NivelError.ADVERTENCIA.equals(e.getNivel()));
    }

    // --- Test 4: OPERACION_NUMERO largo conserva precisión
    @Test
    void operacion_numero_largo_conserva_precision() {
        String fila = "2026-08;12345678;5;JUAN;600403165281;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).operacionNumero()).isEqualTo("600403165281");
    }

    // --- Test 5: OPERACION_NUMERO en notación científica → error
    @Test
    void operacion_numero_notacion_cientifica_genera_error() {
        String fila = "2026-08;12345678;5;JUAN;6,00403E+11;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores()).anyMatch(e ->
                "OPERACION_NUMERO_NOTACION_CIENTIFICA".equals(e.getCodigoError()));
    }

    // --- Test 6: Fecha YYYY-MM-DD parseada correctamente
    @Test
    void fecha_yyyy_mm_dd_es_parseada_correctamente() {
        var resultado = parsear(HEADER + "\n" + FILA_VALIDA);

        assertThat(resultado.filas()).hasSize(1);
        FilaCsv fila = resultado.filas().get(0);
        assertThat(fila.operacionFechaVto()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(fila.cuotaFechaVto()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    // --- Test 7: Fecha en formato DD-MM-YYYY (incorrecto) → error FORMATO_FECHA
    @Test
    void fecha_formato_incorrecto_genera_error() {
        String fila = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;" +
                "31-12-2027;1;VIGENTE;50000;" +   // DD-MM-YYYY → incorrecto
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores()).anyMatch(e ->
                "FORMATO_FECHA".equals(e.getCodigoError()) &&
                "OPERACION_FECHA_VTO".equals(e.getColumna()));
    }

    // --- Test 8: CUOTA_ESTADO vacío → error CAMPO_REQUERIDO (ya no tiene default)
    @Test
    void cuota_estado_vacio_genera_error_campo_requerido() {
        String fila = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;;50000;" +  // CUOTA_ESTADO vacío
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores()).anyMatch(e ->
                "CAMPO_REQUERIDO".equals(e.getCodigoError()) &&
                "CUOTA_ESTADO".equals(e.getColumna()));
    }

    // --- Test 9: DIR_COMERCIAL vacía → null en FilaCsv (válido)
    @Test
    void dir_comercial_vacia_es_null_y_valido() {
        var resultado = parsear(HEADER + "\n" + FILA_VALIDA);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).dirComercial()).isNull();
    }

    // --- Test 10: CODIGO_EJECUTIVO numérico aceptado como texto
    @Test
    void codigo_ejecutivo_numerico_es_aceptado_como_texto() {
        var resultado = parsear(HEADER + "\n" + FILA_VALIDA);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).codigoEjecutivo()).isEqualTo("1001");
    }

    // --- Test 11: PERIODO con formato incorrecto → error FORMATO_PERIODO_INVALIDO
    @Test
    void periodo_con_formato_incorrecto_genera_error() {
        String fila = "08-2026;12345678;5;JUAN;600001;Consumo;VIGENTE;" +  // formato incorrecto
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores()).anyMatch(e ->
                "FORMATO_PERIODO_INVALIDO".equals(e.getCodigoError()) &&
                "PERIODO".equals(e.getColumna()));
    }

    // --- Test 12: PERIODO con mes inválido → error FORMATO_PERIODO_INVALIDO
    @Test
    void periodo_con_mes_invalido_genera_error() {
        String fila = "2026-13;12345678;5;JUAN;600001;Consumo;VIGENTE;" +  // mes 13 inválido
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores()).anyMatch(e ->
                "FORMATO_PERIODO_INVALIDO".equals(e.getCodigoError()));
    }

    // --- Test 13: CODIGO_CARTERA inválido (no es 1,2,3,4) → error
    @Test
    void codigo_cartera_invalido_genera_error() {
        String fila = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;" +
                "9;N";  // cartera 9 inválida
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores()).anyMatch(e ->
                "CODIGO_CARTERA_INVALIDO".equals(e.getCodigoError()));
    }

    // --- Test 14: MARCA_JUDICIAL inválida (no es S ni N) → error
    @Test
    void marca_judicial_invalida_genera_error() {
        String fila = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;" +
                "X";  // X inválido
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores()).anyMatch(e ->
                "MARCA_JUDICIAL_INVALIDA".equals(e.getCodigoError()));
    }

    // --- Test 15: Fila totalmente vacía → ignorada silenciosamente
    @Test
    void fila_totalmente_vacia_es_ignorada() {
        // La línea de separadores pura (sin datos) se ignora
        String csv = HEADER + "\n" + FILA_VALIDA + "\n" +
                ";;;;;;;;;;;;;;;;;;;;;;;;;;";  // 25 separadores = 26 campos vacíos
        var resultado = parsear(csv);

        // Debe haber 2 líneas totales pero solo 1 fila parseada
        assertThat(resultado.totalFilas()).isEqualTo(2);
        assertThat(resultado.filas()).hasSize(1);
        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
    }

    // --- Test 16: Archivo Windows-1252 con ñ → aceptado y nombre preservado
    @Test
    void archivo_windows1252_con_acento_es_aceptado_y_nombre_preservado() {
        Charset win1252 = Charset.forName("windows-1252");
        String filaConAcento = "2026-08;12345678;5;JOSE NUÑEZ RAMOS;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        String csv = HEADER + "\n" + filaConAcento;

        byte[] bytes = csv.getBytes(win1252);
        var resultado = parsearBytes(bytes);

        assertThat(resultado.errores().stream()
                .filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas()).hasSize(1);
        assertThat(resultado.filas().get(0).nombrePersona()).isEqualTo("JOSE NUÑEZ RAMOS");
    }

    // --- Test 16b: UTF-8 con acentos y ñ → aceptado
    @Test
    void archivo_utf8_con_acentos_es_aceptado() {
        // áéíóúñÑ en UTF-8
        String filaConAcento = "2026-08;12345678;5;TOMÁS ÑOÑO GARCÍA;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV GÓMEZ 123;;2;N";
        String csv = HEADER + "\n" + filaConAcento;

        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        var resultado = parsearBytes(bytes);

        assertThat(resultado.errores().stream()
                .filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).nombrePersona()).isEqualTo("TOMÁS ÑOÑO GARCÍA");
    }

    // --- Test 16c: UTF-8 con BOM → BOM descartado, contenido correcto
    @Test
    void archivo_utf8_con_bom_es_aceptado() {
        String csv = HEADER + "\n" + FILA_VALIDA;
        byte[] cuerpo = csv.getBytes(StandardCharsets.UTF_8);
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] conBom = new byte[bom.length + cuerpo.length];
        System.arraycopy(bom, 0, conBom, 0, bom.length);
        System.arraycopy(cuerpo, 0, conBom, bom.length, cuerpo.length);

        var resultado = parsearBytes(conBom);

        assertThat(resultado.errores().stream()
                .filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas()).hasSize(1);
        assertThat(resultado.filas().get(0).periodo()).isEqualTo("2026-08");
    }

    // --- Test 17: Múltiples filas con mismo RUT → todas parseadas
    @Test
    void multiples_filas_mismo_rut_son_todas_parseadas() {
        String fila2 = "2026-08;12345678;5;JUAN PEREZ ROJAS;" +
                "600001000001;Credito de Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;2;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-10-31;1001;AV LIBERTADOR 1234;;2;N";
        String csv = HEADER + "\n" + FILA_VALIDA + "\n" + fila2;

        var resultado = parsear(csv);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas()).hasSize(2);
    }

    // --- Test 18: CUOTA_ESTADO=VIGENTE explícito → aceptado
    @Test
    void cuota_estado_vigente_explicito_es_aceptado() {
        var resultado = parsear(HEADER + "\n" + FILA_VALIDA);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).cuotaEstado()).isEqualTo("VIGENTE");
    }

    // --- Test 19: Decimal con coma como separador → aceptado
    @Test
    void decimal_con_coma_como_separador_es_aceptado() {
        String fila = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1.000.000,00;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        // El parser reemplaza coma por punto al parsear decimal
        // Nota: "1.000.000,00" → "1.000.000.00" → NumberFormatException esperada
        // Solo probamos coma simple como decimal
        String filaConComa = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000,50;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + filaConComa);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).operacionCapital().toPlainString()).isEqualTo("1000000.50");
    }

    // --- Test 20: MARCA_JUDICIAL=S → aceptado
    @Test
    void marca_judicial_s_es_aceptada() {
        String fila = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;S";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).marcaJudicial()).isEqualTo("S");
    }

    // --- Test 21: CODIGO_CARTERA=1 válido → aceptado
    @Test
    void codigo_cartera_1_es_valido() {
        String fila = "2026-08;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;" +
                "1;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).codigoCartera()).isEqualTo("1");
    }

    // --- Test 22: PERIODO válido 2026-01 (mes 01) → aceptado
    @Test
    void periodo_mes_enero_es_valido() {
        String fila = "2026-01;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
        assertThat(resultado.filas().get(0).periodo()).isEqualTo("2026-01");
    }

    // --- Test 23: PERIODO mes 12 válido → aceptado
    @Test
    void periodo_mes_diciembre_es_valido() {
        String fila = "2026-12;12345678;5;JUAN;600001;Consumo;VIGENTE;" +
                "1000000;0;0;1020000;2027-12-31;1;VIGENTE;50000;" +
                "45000;3000;1500;500;50000;2026-09-30;1001;AV LIBERTADOR 1234;;2;N";
        var resultado = parsear(HEADER + "\n" + fila);

        assertThat(resultado.errores().stream().filter(e -> e.getNivel() == NivelError.ERROR).toList()).isEmpty();
    }

    // --- Utilidades

    private CsvImportacionParser.ResultadoParser parsear(String csv) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return parsearBytes(bytes);
    }

    private CsvImportacionParser.ResultadoParser parsearBytes(byte[] bytes) {
        InputStream is = new ByteArrayInputStream(bytes);
        return parser.parsear(importacionId, is);
    }
}
