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

        // Mapas para detectar inconsistencias
        Map<String, String> rutPorCodigoExtPersona = new HashMap<>();
        Map<String, String> codigoExtPorRut = new HashMap<>();
        Map<String, String> personaPorOperacionIdExt = new HashMap<>();
        Map<String, Set<String>> ejecutivosPorRut = new HashMap<>();
        Map<String, FilaCsv> ultimaOperacionPorIdExt = new HashMap<>();
        Map<String, Set<String>> direccionesPorRut = new HashMap<>();

        for (FilaCsv fila : filas) {
            String rutKey = fila.rutNumero() + "-" + fila.rutDv();

            validarRut(fila, importacionId, errores);

            if (fila.codigoExtPersona() != null && !fila.codigoExtPersona().isBlank()) {
                String rutExistente = rutPorCodigoExtPersona.get(fila.codigoExtPersona());
                if (rutExistente != null && !rutExistente.equals(rutKey)) {
                    errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                            "CODIGO_EXT_PERSONA", "EXT_PERSONA_RUT_INCONSISTENTE", NivelError.ERROR,
                            "Mismo código externo de persona apunta a distintos RUT (filas " +
                                    fila.numeroFila() + ")"));
                } else {
                    rutPorCodigoExtPersona.put(fila.codigoExtPersona(), rutKey);
                }

                String codigoExistente = codigoExtPorRut.get(rutKey);
                if (codigoExistente != null && !codigoExistente.equals(fila.codigoExtPersona())) {
                    errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                            "CODIGO_EXT_PERSONA", "RUT_CODIGO_EXT_INCONSISTENTE", NivelError.ERROR,
                            "Mismo RUT con distintos códigos externos de persona (fila " +
                                    fila.numeroFila() + ")"));
                } else {
                    codigoExtPorRut.put(rutKey, fila.codigoExtPersona());
                }
            }

            // Operación idExt debe pertenecer siempre al mismo RUT
            String rutOperacion = personaPorOperacionIdExt.get(fila.operacionIdExt());
            if (rutOperacion != null && !rutOperacion.equals(rutKey)) {
                errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                        "OPERACION_ID_EXT", "OPERACION_PERSONA_INCONSISTENTE", NivelError.ERROR,
                        "Operación externa " + fila.operacionIdExt() +
                                " aparece con distinta persona (fila " + fila.numeroFila() + ")"));
            } else {
                personaPorOperacionIdExt.put(fila.operacionIdExt(), rutKey);
            }

            // Operación datos maestros inconsistentes
            FilaCsv prev = ultimaOperacionPorIdExt.get(fila.operacionIdExt());
            if (prev != null) {
                if (!Objects.equals(prev.operacionNumero(), fila.operacionNumero()) ||
                        !Objects.equals(prev.operacionTipo(), fila.operacionTipo())) {
                    errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                            "OPERACION_ID_EXT", "OPERACION_DATOS_INCONSISTENTES", NivelError.ERROR,
                            "Operación " + fila.operacionIdExt() +
                                    " tiene datos maestros inconsistentes (fila " + fila.numeroFila() + ")"));
                }
            } else {
                ultimaOperacionPorIdExt.put(fila.operacionIdExt(), fila);
            }

            // Un ejecutivo por persona
            Set<String> ejecutivos = ejecutivosPorRut.computeIfAbsent(rutKey, k -> new HashSet<>());
            ejecutivos.add(fila.ejecutivoUsername());
            if (ejecutivos.size() > 1) {
                errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                        "EJECUTIVO_USERNAME", "PERSONA_EJECUTIVOS_MULTIPLES", NivelError.ERROR,
                        "La persona " + rutKey + " tiene más de un ejecutivo asignado (fila " +
                                fila.numeroFila() + ")"));
            }

            // Dirección incompatible por tipo
            String dirKey = fila.direccionTipo();
            Set<String> dirs = direccionesPorRut.computeIfAbsent(rutKey + "|" + dirKey, k -> new HashSet<>());
            dirs.add(fila.direccionTexto());
            if (dirs.size() > 1) {
                errores.add(new ErrorImportacion(importacionId, fila.numeroFila(),
                        "DIRECCION_TEXTO", "PERSONA_DIRECCION_INCONSISTENTE", NivelError.ADVERTENCIA,
                        "La persona " + rutKey + " tiene direcciones distintas del mismo tipo " +
                                dirKey + " (fila " + fila.numeroFila() + ")"));
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
