package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import cl.zzenner.cobranza.importacion.dominio.FilaCsv;
import cl.zzenner.cobranza.importacion.dominio.NivelError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
class CsvImportacionParser {

    static final String[] COLUMNAS_REQUERIDAS = {
            "RUT_NUMERO", "RUT_DV", "NOMBRE_PERSONA", "CODIGO_EXT_PERSONA",
            "DIRECCION_TEXTO", "DIRECCION_TIPO", "DIRECCION_COMUNA", "DIRECCION_CIUDAD", "CODIGO_EXT_DIRECCION",
            "OPERACION_NUMERO", "OPERACION_ID_EXT", "OPERACION_TIPO", "OPERACION_ESTADO",
            "OPERACION_CAPITAL", "OPERACION_INTERES_PENAL", "OPERACION_GASTOS",
            "OPERACION_TOTAL_VIGENTE", "OPERACION_FECHA_VTO",
            "CUOTA_NUMERO", "CUOTA_ID_EXT", "CUOTA_ESTADO", "CUOTA_MONTO_TOTAL",
            "CUOTA_CAPITAL", "CUOTA_INTERES", "CUOTA_INTERES_PENAL", "CUOTA_GASTOS",
            "CUOTA_SALDO", "CUOTA_FECHA_VTO",
            "EJECUTIVO_USERNAME"
    };

    record ResultadoParser(List<FilaCsv> filas, List<ErrorImportacion> errores, int totalFilas) {}

    ResultadoParser parsear(UUID importacionId, InputStream inputStream) {
        List<FilaCsv> filas = new ArrayList<>();
        List<ErrorImportacion> errores = new ArrayList<>();
        int totalFilas = 0;

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter(';')
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (InputStreamReader reader = new InputStreamReader(
                stripBom(inputStream), StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, formato)) {

            verificarColumnasRequeridas(parser.getHeaderNames(), importacionId, errores);
            if (!errores.isEmpty()) {
                return new ResultadoParser(filas, errores, 0);
            }

            detectarColumnasDesconocidas(parser.getHeaderNames(), importacionId, errores);

            for (CSVRecord record : parser) {
                totalFilas++;
                int numeroFila = (int) record.getRecordNumber() + 1;
                try {
                    FilaCsv fila = parsearFila(record, numeroFila);
                    filas.add(fila);
                } catch (FilaInvalidaException e) {
                    errores.add(new ErrorImportacion(importacionId, numeroFila,
                            e.getColumna(), e.getCodigo(), NivelError.ERROR, e.getMessage()));
                }
            }
        } catch (IOException e) {
            errores.add(new ErrorImportacion(importacionId, null, null,
                    "FORMATO_INVALIDO", NivelError.ERROR,
                    "Error al leer el archivo CSV: " + e.getMessage()));
        }

        return new ResultadoParser(filas, errores, totalFilas);
    }

    private void verificarColumnasRequeridas(List<String> encabezados,
                                              UUID importacionId,
                                              List<ErrorImportacion> errores) {
        List<String> encabezadosUpper = encabezados.stream()
                .map(String::toUpperCase).toList();

        for (String requerida : COLUMNAS_REQUERIDAS) {
            if (!encabezadosUpper.contains(requerida)) {
                errores.add(new ErrorImportacion(importacionId, null, requerida,
                        "COLUMNA_REQUERIDA_FALTANTE", NivelError.ERROR,
                        "Columna requerida ausente: " + requerida));
            }
        }
    }

    private void detectarColumnasDesconocidas(List<String> encabezados,
                                               UUID importacionId,
                                               List<ErrorImportacion> errores) {
        List<String> conocidas = List.of(COLUMNAS_REQUERIDAS);
        for (String enc : encabezados) {
            if (!conocidas.contains(enc.toUpperCase())) {
                errores.add(new ErrorImportacion(importacionId, null, enc,
                        "COLUMNA_DESCONOCIDA", NivelError.ADVERTENCIA,
                        "Columna desconocida ignorada: " + enc));
            }
        }
    }

    private FilaCsv parsearFila(CSVRecord rec, int numeroFila) {
        String rutNumero = requerido(rec, "RUT_NUMERO", numeroFila);
        String rutDv = requerido(rec, "RUT_DV", numeroFila);
        String nombrePersona = requerido(rec, "NOMBRE_PERSONA", numeroFila);
        String codigoExtPersona = opcional(rec, "CODIGO_EXT_PERSONA");

        String direccionTexto = requerido(rec, "DIRECCION_TEXTO", numeroFila);
        String direccionTipo = requerido(rec, "DIRECCION_TIPO", numeroFila);
        String direccionComuna = opcional(rec, "DIRECCION_COMUNA");
        String direccionCiudad = opcional(rec, "DIRECCION_CIUDAD");
        String codigoExtDireccion = opcional(rec, "CODIGO_EXT_DIRECCION");

        String operacionNumero = requerido(rec, "OPERACION_NUMERO", numeroFila);
        String operacionIdExt = requerido(rec, "OPERACION_ID_EXT", numeroFila);
        String operacionTipo = opcional(rec, "OPERACION_TIPO");
        String operacionEstado = requerido(rec, "OPERACION_ESTADO", numeroFila);
        BigDecimal operacionCapital = decimal(rec, "OPERACION_CAPITAL", numeroFila);
        BigDecimal operacionInteresPenal = decimal(rec, "OPERACION_INTERES_PENAL", numeroFila);
        BigDecimal operacionGastos = decimal(rec, "OPERACION_GASTOS", numeroFila);
        BigDecimal operacionTotalVigente = decimalOpcional(rec, "OPERACION_TOTAL_VIGENTE");
        LocalDate operacionFechaVto = fecha(rec, "OPERACION_FECHA_VTO", numeroFila);

        Integer cuotaNumero = entero(rec, "CUOTA_NUMERO", numeroFila);
        String cuotaIdExt = opcional(rec, "CUOTA_ID_EXT");
        String cuotaEstado = requerido(rec, "CUOTA_ESTADO", numeroFila);
        BigDecimal cuotaMontoTotal = decimal(rec, "CUOTA_MONTO_TOTAL", numeroFila);
        BigDecimal cuotaCapital = decimalOpcional(rec, "CUOTA_CAPITAL");
        BigDecimal cuotaInteres = decimalOpcional(rec, "CUOTA_INTERES");
        BigDecimal cuotaInteresPenal = decimal(rec, "CUOTA_INTERES_PENAL", numeroFila);
        BigDecimal cuotaGastos = decimal(rec, "CUOTA_GASTOS", numeroFila);
        BigDecimal cuotaSaldo = decimalOpcional(rec, "CUOTA_SALDO");
        LocalDate cuotaFechaVto = fecha(rec, "CUOTA_FECHA_VTO", numeroFila);

        String ejecutivoUsername = requerido(rec, "EJECUTIVO_USERNAME", numeroFila);

        return new FilaCsv(numeroFila,
                rutNumero, rutDv, nombrePersona, codigoExtPersona,
                direccionTexto, direccionTipo, direccionComuna, direccionCiudad, codigoExtDireccion,
                operacionNumero, operacionIdExt, operacionTipo, operacionEstado,
                operacionCapital, operacionInteresPenal, operacionGastos, operacionTotalVigente, operacionFechaVto,
                cuotaNumero, cuotaIdExt, cuotaEstado, cuotaMontoTotal,
                cuotaCapital, cuotaInteres, cuotaInteresPenal, cuotaGastos, cuotaSaldo, cuotaFechaVto,
                ejecutivoUsername);
    }

    private String requerido(CSVRecord rec, String col, int fila) {
        String val = rec.get(col.toLowerCase());
        if (val == null || val.isBlank()) {
            throw new FilaInvalidaException(fila, col, "CAMPO_REQUERIDO",
                    "El campo " + col + " es requerido");
        }
        return val.strip();
    }

    private String opcional(CSVRecord rec, String col) {
        try {
            String val = rec.get(col.toLowerCase());
            return (val == null || val.isBlank()) ? null : val.strip();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal decimal(CSVRecord rec, String col, int fila) {
        String raw = requerido(rec, col, fila);
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new FilaInvalidaException(fila, col, "FORMATO_DECIMAL",
                    "El campo " + col + " no es un número decimal válido: " + raw);
        }
    }

    private BigDecimal decimalOpcional(CSVRecord rec, String col) {
        String raw = opcional(rec, col);
        if (raw == null) return null;
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate fecha(CSVRecord rec, String col, int fila) {
        String raw = requerido(rec, col, fila);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new FilaInvalidaException(fila, col, "FORMATO_FECHA",
                    "El campo " + col + " no tiene formato de fecha válido (YYYY-MM-DD): " + raw);
        }
    }

    private Integer entero(CSVRecord rec, String col, int fila) {
        String raw = requerido(rec, col, fila);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new FilaInvalidaException(fila, col, "FORMATO_ENTERO",
                    "El campo " + col + " no es un número entero válido: " + raw);
        }
    }

    private InputStream stripBom(InputStream is) throws IOException {
        // Wrap in BufferedInputStream to ensure mark/reset support
        BufferedInputStream bis = (is instanceof BufferedInputStream b) ? b : new BufferedInputStream(is);
        bis.mark(3);
        byte[] bom = bis.readNBytes(3);
        if (bom.length == 3 && bom[0] == (byte) 0xEF
                && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            return bis;
        }
        bis.reset();
        return bis;
    }

    static class FilaInvalidaException extends RuntimeException {
        private final String columna;
        private final String codigo;

        FilaInvalidaException(int fila, String columna, String codigo, String mensaje) {
            super(mensaje);
            this.columna = columna;
            this.codigo = codigo;
        }

        String getColumna() { return columna; }
        String getCodigo() { return codigo; }
    }
}
