package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import cl.zzenner.cobranza.importacion.dominio.FilaCsv;
import cl.zzenner.cobranza.importacion.dominio.NivelError;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorIntraArchivoTest {

    private final ValidadorIntraArchivo validador = new ValidadorIntraArchivo();
    private final UUID importacionId = UUID.randomUUID();

    private static final LocalDate FECHA_VTO = LocalDate.of(2027, 12, 31);

    // --- Test 24: Mismo ejecutivo en todas las filas → sin error PERSONA_EJECUTIVOS_MULTIPLES
    @Test
    void mismo_ejecutivo_en_todas_las_filas_no_genera_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "5", "2026-08", "600001", "TipoCred", "1001", "2"),
                fila(2, "12345678", "5", "2026-08", "600001", "TipoCred", "1001", "2")
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores.stream()
                .filter(e -> "PERSONA_EJECUTIVOS_MULTIPLES".equals(e.getCodigoError())).toList()).isEmpty();
    }

    // --- Test 25: Dos ejecutivos para mismo RUT+PERIODO+CARTERA → error
    @Test
    void dos_ejecutivos_para_mismo_periodo_rut_cartera_genera_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "5", "2026-08", "600001", "TipoCred", "1001", "2"),
                fila(2, "12345678", "5", "2026-08", "600002", "TipoCred", "1002", "2")
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores).anyMatch(e ->
                "PERSONA_EJECUTIVOS_MULTIPLES".equals(e.getCodigoError()) &&
                NivelError.ERROR.equals(e.getNivel()));
    }

    // --- Test 26: Ejecutivo distinto en PERIODO distinto → NO es error (dimensión temporal)
    @Test
    void ejecutivo_distinto_en_periodo_distinto_no_genera_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "5", "2026-08", "600001", "TipoCred", "1001", "2"),
                fila(2, "12345678", "5", "2026-09", "600001", "TipoCred", "1002", "2")
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores.stream()
                .filter(e -> "PERSONA_EJECUTIVOS_MULTIPLES".equals(e.getCodigoError())).toList()).isEmpty();
    }

    // --- Test 27: Operación con mismo numero y tipo diferente → error OPERACION_DATOS_INCONSISTENTES
    @Test
    void operacion_con_mismo_numero_y_tipo_diferente_genera_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "5", "2026-08", "600001", "TipoA", "1001", "2"),
                fila(2, "12345678", "5", "2026-08", "600001", "TipoB", "1001", "2")
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores).anyMatch(e ->
                "OPERACION_DATOS_INCONSISTENTES".equals(e.getCodigoError()) &&
                NivelError.ERROR.equals(e.getNivel()));
    }

    // --- Test 28: RUT inválido por módulo 11 → error RUT_INVALIDO_MODULO_11
    @Test
    void rut_invalido_modulo_11_genera_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "9", "2026-08", "600001", "TipoCred", "1001", "2")  // DV 9 es incorrecto
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores).anyMatch(e ->
                "RUT_INVALIDO_MODULO_11".equals(e.getCodigoError()) &&
                NivelError.ERROR.equals(e.getNivel()));
    }

    // --- Test 29: Posición duplicada (mismo PERIODO+RUT+OP+CUOTA+CARTERA) → error
    @Test
    void posicion_duplicada_genera_error() {
        // Dos filas con exactamente el mismo PERIODO+RUT+OP+CUOTA+CARTERA
        FilaCsv f1 = new FilaCsv(1, "2026-08", "12345678", "5", "JUAN",
                "600001", "TipoCred", "VIGENTE",
                BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO, null,
                FECHA_VTO, 1, "VIGENTE", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000), BigDecimal.valueOf(3000),
                BigDecimal.valueOf(1500), BigDecimal.valueOf(500),
                BigDecimal.valueOf(50000), FECHA_VTO,
                "1001", "AV LIBERTADOR 1234", null, "2", "N");
        FilaCsv f2 = new FilaCsv(2, "2026-08", "12345678", "5", "JUAN",
                "600001", "TipoCred", "VIGENTE",
                BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO, null,
                FECHA_VTO, 1, "VIGENTE", BigDecimal.valueOf(50000),  // CUOTA 1 duplicada
                BigDecimal.valueOf(45000), BigDecimal.valueOf(3000),
                BigDecimal.valueOf(1500), BigDecimal.valueOf(500),
                BigDecimal.valueOf(50000), FECHA_VTO,
                "1001", "AV LIBERTADOR 1234", null, "2", "N");

        List<ErrorImportacion> errores = validador.validar(importacionId, List.of(f1, f2));

        assertThat(errores).anyMatch(e -> "POSICION_DUPLICADA".equals(e.getCodigoError()));
    }

    // --- Test 30: Mismo RUT en distinta cartera con distinto ejecutivo → NO es error
    @Test
    void mismo_rut_distinta_cartera_distintos_ejecutivos_no_genera_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "5", "2026-08", "600001", "TipoCred", "1001", "1"),
                fila(2, "12345678", "5", "2026-08", "600002", "TipoCred", "1002", "2")
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores.stream()
                .filter(e -> "PERSONA_EJECUTIVOS_MULTIPLES".equals(e.getCodigoError())).toList()).isEmpty();
    }

    // --- Test 31: Múltiples períodos en el mismo archivo → error PERIODOS_MULTIPLES_ARCHIVO
    @Test
    void multiples_periodos_en_archivo_generan_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "5", "2026-08", "600001", "TipoCred", "1001", "2"),
                fila(2, "23456789", "K", "2026-09", "600002", "TipoCred", "1001", "2")
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores).anyMatch(e ->
                "PERIODOS_MULTIPLES_ARCHIVO".equals(e.getCodigoError()) &&
                NivelError.ERROR.equals(e.getNivel()) &&
                e.getMensaje().contains("2026-08") &&
                e.getMensaje().contains("2026-09"));
    }

    // --- Test 32: Un solo período en el archivo → sin error PERIODOS_MULTIPLES_ARCHIVO
    @Test
    void un_solo_periodo_en_archivo_no_genera_error() {
        List<FilaCsv> filas = List.of(
                fila(1, "12345678", "5", "2026-08", "600001", "TipoCred", "1001", "2"),
                fila(2, "23456789", "K", "2026-08", "600002", "TipoCred", "1001", "2")
        );

        List<ErrorImportacion> errores = validador.validar(importacionId, filas);

        assertThat(errores.stream()
                .filter(e -> "PERIODOS_MULTIPLES_ARCHIVO".equals(e.getCodigoError())).toList()).isEmpty();
    }

    // --- Test 33: dvEsValido calcula módulo 11 correctamente
    @Test
    void dv_es_valido_calcula_modulo_11_correctamente() {
        assertThat(ValidadorIntraArchivo.dvEsValido(12345678, "5")).isTrue();
        assertThat(ValidadorIntraArchivo.dvEsValido(12345678, "9")).isFalse();
        assertThat(ValidadorIntraArchivo.dvEsValido(98765432, "5")).isTrue();
        assertThat(ValidadorIntraArchivo.dvEsValido(11111111, "1")).isTrue();
    }

    // --- Utilidad para construir FilaCsv de prueba (cuota 1 por defecto)

    private FilaCsv fila(int numeroFila, String rut, String dv, String periodo,
                          String opNumero, String opTipo,
                          String codigoEjecutivo, String codigoCartera) {
        return new FilaCsv(
                numeroFila,
                periodo,
                rut, dv, "NOMBRE PRUEBA",
                opNumero, opTipo, "VIGENTE",
                BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(1000000), FECHA_VTO,
                1, "VIGENTE", BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000), BigDecimal.valueOf(3000),
                BigDecimal.valueOf(1500), BigDecimal.valueOf(500),
                BigDecimal.valueOf(50000), FECHA_VTO,
                codigoEjecutivo,
                "AV PRINCIPAL 123", null,
                codigoCartera, "N"
        );
    }
}
