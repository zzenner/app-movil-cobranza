package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.ErrorImportacion;
import cl.zzenner.cobranza.importacion.dominio.FilaCsv;
import cl.zzenner.cobranza.importacion.dominio.NivelError;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
class ValidadorIntraArchivo {

    List<ErrorImportacion> validar(UUID importacionId, List<FilaCsv> filas) {
        List<ErrorImportacion> errores = new ArrayList<>();

        // Clave de posición: PERIODO+RUT+OP+CUOTA+CARTERA
        Set<String> posicionesVistas = new HashSet<>();
        // Ejecutivo único por PERIODO+RUT+CARTERA
        Map<String, Set<String>> ejecutivosPorPeriodoRutCartera = new HashMap<>();
        // Consistencia de tipo por operacion
        Map<String, String> tipoPorOperacion = new HashMap<>();

        for (FilaCsv fila : filas) {
            String rutKey = fila.rutNumero() + "-" + fila.rutDv();

            validarRut(fila, importacionId, errores);

            // Posición única: no puede haber dos filas con mismo periodo+rut+op+cuota+cartera
            String clavePos = fila.periodo() + "|" + rutKey + "|" +
                    fila.operacionNumero() + "|" + fila.cuotaNumero() + "|" + fila.codigoCartera();
            if (!posicionesVistas.add(clavePos)) {
                errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                        "OPERACION_NUMERO", "POSICION_DUPLICADA", NivelError.ERROR,
                        "Posición duplicada (PERIODO+RUT+OPERACION+CUOTA+CARTERA) en fila " +
                                fila.numeroFila() + ": " + clavePos));
            }

            // Ejecutivo único por PERIODO+RUT+CARTERA
            String claveEjec = fila.periodo() + "|" + rutKey + "|" + fila.codigoCartera();
            Set<String> ejecutivos = ejecutivosPorPeriodoRutCartera
                    .computeIfAbsent(claveEjec, k -> new LinkedHashSet<>());
            ejecutivos.add(fila.codigoEjecutivo());
            if (ejecutivos.size() > 1) {
                errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                        "CODIGO_EJECUTIVO", "PERSONA_EJECUTIVOS_MULTIPLES", NivelError.ERROR,
                        "La persona " + rutKey + " tiene más de un ejecutivo en PERIODO=" +
                                fila.periodo() + " CARTERA=" + fila.codigoCartera() +
                                ": " + String.join(", ", ejecutivos)));
            }

            // Consistencia del tipo de operación: mismo numero → mismo tipo
            String tipoPrev = tipoPorOperacion.get(fila.operacionNumero());
            if (tipoPrev != null) {
                if (!Objects.equals(tipoPrev, fila.operacionTipo())) {
                    errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                            "OPERACION_TIPO", "OPERACION_DATOS_INCONSISTENTES", NivelError.ERROR,
                            "Operación " + fila.operacionNumero() +
                                    " tiene tipo inconsistente: '" + tipoPrev +
                                    "' vs '" + fila.operacionTipo() + "' en fila " + fila.numeroFila()));
                }
            } else {
                tipoPorOperacion.put(fila.operacionNumero(), fila.operacionTipo());
            }
        }

        return errores;
    }

    private void validarRut(FilaCsv fila, UUID importacionId, List<ErrorImportacion> errores) {
        try {
            int numero = Integer.parseInt(fila.rutNumero());
            if (!dvEsValido(numero, fila.rutDv())) {
                errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                        "RUT_NUMERO", "RUT_INVALIDO_MODULO_11", NivelError.ERROR,
                        "El RUT " + fila.rutNumero() + "-" + fila.rutDv() +
                                " no es válido (módulo 11) en fila " + fila.numeroFila()));
            }
        } catch (NumberFormatException e) {
            errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                    "RUT_NUMERO", "RUT_FORMATO_INVALIDO", NivelError.ERROR,
                    "El RUT_NUMERO no es un número válido en fila " + fila.numeroFila()));
        }
    }

    static boolean dvEsValido(int rutNumero, String dvIngresado) {
        int suma = 0;
        int multiplicador = 2;
        int num = rutNumero;
        while (num > 0) {
            suma += (num % 10) * multiplicador;
            num /= 10;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        String dvCalculado = switch (resto) {
            case 11 -> "0";
            case 10 -> "K";
            default -> String.valueOf(resto);
        };
        return dvCalculado.equalsIgnoreCase(dvIngresado);
    }
}
