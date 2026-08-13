package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import cl.zzenner.cobranza.importacion.dominio.FilaCsv;
import cl.zzenner.cobranza.importacion.dominio.NivelError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
class CsvImportacionParser {

    // Contrato CSV definitivo — Fase 5D: 26 columnas
    static final String[] COLUMNAS_REQUERIDAS = {
            "PERIODO",
            "RUT_NUMERO", "RUT_DV", "NOMBRE_PERSONA",
            "OPERACION_NUMERO", "OPERACION_TIPO", "OPERACION_ESTADO",
            "OPERACION_CAPITAL", "OPERACION_INTERES_PENAL", "OPERACION_GASTOS",
            "OPERACION_TOTAL_VIGENTE", "OPERACION_FECHA_VTO",
            "CUOTA_NUMERO", "CUOTA_ESTADO", "CUOTA_MONTO_TOTAL",
            "CUOTA_CAPITAL", "CUOTA_INTERES", "CUOTA_INTERES_PENAL",
            "CUOTA_GASTOS", "CUOTA_SALDO", "CUOTA_FECHA_VTO",
            "CODIGO_EJECUTIVO",
            "DIR_PARTICULAR", "DIR_COMERCIAL",
            "CODIGO_CARTERA", "MARCA_JUDICIAL"
    };

    // Notación científica: 6,00403E+11 o 6.00403E+11 o 6E+11
    static final Pattern NOTACION_CIENTIFICA = Pattern.compile(
            ".*[0-9][,.]?[0-9]*[Ee][+\\-][0-9]+.*");

    // PERIODO: YYYY-MM
    static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("yyyy-MM");

    // Fechas del sistema origen: YYYY-MM-DD
    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Encodings soportados: UTF-8 (preferido) y Windows-1252 (legacy del sistema origen)
    static final Charset ENCODING_UTF8 = StandardCharsets.UTF_8;
    static final Charset ENCODING_WIN1252 = Charset.forName("windows-1252");

    // Carteras válidas del catálogo (códigos de origen)
    static final Set<String> CARTERAS_VALIDAS = Set.of("1", "2", "3", "4");

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
                .setIgnoreEmptyLines(false)
                .build();

        byte[] bytes;
        try {
            bytes = inputStream.readAllBytes();
        } catch (IOException e) {
            errores.add(new ErrorImportacion(importacionId, null, null,
                    "FORMATO_INVALIDO", NivelError.ERROR,
                    "Error al leer el archivo CSV: " + e.getMessage()));
            return new ResultadoParser(filas, errores, 0);
        }

        // Detectar encoding: UTF-8 preferido, fallback a Windows-1252
        Charset charset = detectarEncoding(bytes);
        byte[] data = stripBomBytes(bytes);

        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(data), charset);
             CSVParser parser = CSVParser.parse(reader, formato)) {

            verificarColumnasRequeridas(parser.getHeaderNames(), importacionId, errores);
            if (errores.stream().anyMatch(e -> e.getNivel() == NivelError.ERROR)) {
                return new ResultadoParser(filas, errores, 0);
            }

            detectarColumnasDesconocidas(parser.getHeaderNames(), importacionId, errores);

            for (CSVRecord record : parser) {
                totalFilas++;
                if (esFilaTotalmenteVacia(record)) {
                    continue;
                }
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

    // Detecta si el archivo es UTF-8 válido; si falla, usa Windows-1252.
    // Windows-1252 acepta cualquier secuencia de bytes, por lo que el fallback siempre tiene éxito.
    private Charset detectarEncoding(byte[] bytes) {
        try {
            ENCODING_UTF8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return ENCODING_UTF8;
        } catch (CharacterCodingException e) {
            return ENCODING_WIN1252;
        }
    }

    private byte[] stripBomBytes(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF) {
            return Arrays.copyOfRange(bytes, 3, bytes.length);
        }
        return bytes;
    }

    private boolean esFilaTotalmenteVacia(CSVRecord rec) {
        for (int i = 0; i < rec.size(); i++) {
            String val = rec.get(i);
            if (val != null && !val.isBlank()) return false;
        }
        return true;
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
        String periodo = periodoRequerido(rec, numeroFila);

        String rutNumero = requerido(rec, "RUT_NUMERO", numeroFila);
        String rutDv = requerido(rec, "RUT_DV", numeroFila);
        String nombrePersona = requerido(rec, "NOMBRE_PERSONA", numeroFila);

        String operacionNumero = operacionNumeroRequerido(rec, "OPERACION_NUMERO", numeroFila);
        String operacionTipo = requerido(rec, "OPERACION_TIPO", numeroFila);
        String operacionEstado = requerido(rec, "OPERACION_ESTADO", numeroFila);
        BigDecimal operacionCapital = decimal(rec, "OPERACION_CAPITAL", numeroFila);
        BigDecimal operacionInteresPenal = decimal(rec, "OPERACION_INTERES_PENAL", numeroFila);
        BigDecimal operacionGastos = decimal(rec, "OPERACION_GASTOS", numeroFila);
        BigDecimal operacionTotalVigente = decimalOpcional(rec, "OPERACION_TOTAL_VIGENTE");
        LocalDate operacionFechaVto = fecha(rec, "OPERACION_FECHA_VTO", numeroFila);

        Integer cuotaNumero = entero(rec, "CUOTA_NUMERO", numeroFila);
        String cuotaEstado = requerido(rec, "CUOTA_ESTADO", numeroFila);
        BigDecimal cuotaMontoTotal = decimal(rec, "CUOTA_MONTO_TOTAL", numeroFila);
        BigDecimal cuotaCapital = decimalOpcional(rec, "CUOTA_CAPITAL");
        BigDecimal cuotaInteres = decimalOpcional(rec, "CUOTA_INTERES");
        BigDecimal cuotaInteresPenal = decimal(rec, "CUOTA_INTERES_PENAL", numeroFila);
        BigDecimal cuotaGastos = decimal(rec, "CUOTA_GASTOS", numeroFila);
        BigDecimal cuotaSaldo = decimalOpcional(rec, "CUOTA_SALDO");
        LocalDate cuotaFechaVto = fecha(rec, "CUOTA_FECHA_VTO", numeroFila);

        String codigoEjecutivo = requerido(rec, "CODIGO_EJECUTIVO", numeroFila);
        String dirParticular = requerido(rec, "DIR_PARTICULAR", numeroFila);
        String dirComercial = opcional(rec, "DIR_COMERCIAL");
        String codigoCartera = codigoCarteraRequerido(rec, numeroFila);
        String marcaJudicial = marcaJudicialRequerida(rec, numeroFila);

        return new FilaCsv(numeroFila,
                periodo,
                rutNumero, rutDv, nombrePersona,
                operacionNumero, operacionTipo, operacionEstado,
                operacionCapital, operacionInteresPenal, operacionGastos,
                operacionTotalVigente, operacionFechaVto,
                cuotaNumero, cuotaEstado, cuotaMontoTotal,
                cuotaCapital, cuotaInteres, cuotaInteresPenal,
                cuotaGastos, cuotaSaldo, cuotaFechaVto,
                codigoEjecutivo,
                dirParticular, dirComercial,
                codigoCartera, marcaJudicial);
    }

    private String periodoRequerido(CSVRecord rec, int fila) {
        String val = requerido(rec, "PERIODO", fila);
        try {
            YearMonth.parse(val, FORMATO_PERIODO);
        } catch (DateTimeParseException e) {
            throw new FilaInvalidaException(fila, "PERIODO", "FORMATO_PERIODO_INVALIDO",
                    "PERIODO debe tener formato YYYY-MM (ej: 2026-08): " + val);
        }
        return val;
    }

    private String codigoCarteraRequerido(CSVRecord rec, int fila) {
        String val = requerido(rec, "CODIGO_CARTERA", fila);
        if (!CARTERAS_VALIDAS.contains(val)) {
            throw new FilaInvalidaException(fila, "CODIGO_CARTERA", "CODIGO_CARTERA_INVALIDO",
                    "CODIGO_CARTERA debe ser 1, 2, 3 o 4: " + val);
        }
        return val;
    }

    private String marcaJudicialRequerida(CSVRecord rec, int fila) {
        String val = requerido(rec, "MARCA_JUDICIAL", fila);
        if (!"S".equals(val) && !"N".equals(val)) {
            throw new FilaInvalidaException(fila, "MARCA_JUDICIAL", "MARCA_JUDICIAL_INVALIDA",
                    "MARCA_JUDICIAL debe ser S o N: " + val);
        }
        return val;
    }

    private String operacionNumeroRequerido(CSVRecord rec, String col, int fila) {
        String val = requerido(rec, col, fila);
        if (NOTACION_CIENTIFICA.matcher(val).matches()) {
            throw new FilaInvalidaException(fila, col, "OPERACION_NUMERO_NOTACION_CIENTIFICA",
                    "OPERACION_NUMERO contiene notación científica y puede haber perdido precisión: " + val +
                    ". El origen debe exportar el número completo sin conversión.");
        }
        return val;
    }

    private String requerido(CSVRecord rec, String col, int fila) {
        String val;
        try {
            val = rec.get(col.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new FilaInvalidaException(fila, col, "COLUMNA_REQUERIDA_FALTANTE",
                    "Columna requerida no disponible: " + col);
        }
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
            return LocalDate.parse(raw, FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new FilaInvalidaException(fila, col, "FORMATO_FECHA",
                    "El campo " + col + " debe tener formato YYYY-MM-DD: " + raw);
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
