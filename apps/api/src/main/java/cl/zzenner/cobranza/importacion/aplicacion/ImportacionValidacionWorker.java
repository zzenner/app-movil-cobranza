package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.*;
import cl.zzenner.cobranza.importacion.infraestructura.ImportacionMensualRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
class ImportacionValidacionWorker {

    private static final Logger log = LoggerFactory.getLogger(ImportacionValidacionWorker.class);

    private final ImportacionEstadoService estadoService;
    private final ImportacionMensualRepository repository;
    private final ArchivoImportacionStorage storage;
    private final CsvImportacionParser parser;
    private final ValidadorIntraArchivo validador;
    private final ImportacionPersistenciaService persistenciaService;

    ImportacionValidacionWorker(ImportacionEstadoService estadoService,
                                 ImportacionMensualRepository repository,
                                 ArchivoImportacionStorage storage,
                                 CsvImportacionParser parser,
                                 ValidadorIntraArchivo validador,
                                 ImportacionPersistenciaService persistenciaService) {
        this.estadoService = estadoService;
        this.repository = repository;
        this.storage = storage;
        this.parser = parser;
        this.validador = validador;
        this.persistenciaService = persistenciaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("importacionExecutor")
    public void onValidar(ValidarImportacionEvento evento) {
        UUID importacionId = evento.importacionId();
        log.info("[IMPORTACION] Iniciando validación importacion={}", importacionId);

        boolean transicionOk = estadoService.intentarTransicionarAValidando(importacionId);
        if (!transicionOk) {
            log.warn("[IMPORTACION] No se pudo transicionar a VALIDANDO importacion={}", importacionId);
            return;
        }

        try {
            ImportacionMensual im = cargarImportacion(importacionId);
            if (im == null || im.getRutaArchivo() == null) {
                estadoService.marcarFallida(importacionId, "Archivo temporal no disponible");
                return;
            }

            CsvImportacionParser.ResultadoParser resultadoParser;
            try (InputStream is = storage.leer(im.getRutaArchivo())) {
                resultadoParser = parser.parsear(importacionId, is);
            }

            List<ErrorImportacion> todosErrores = new ArrayList<>(resultadoParser.errores());

            boolean parseoLimpio = todosErrores.stream()
                    .noneMatch(e -> e.getNivel() == NivelError.ERROR);
            if (parseoLimpio && !resultadoParser.filas().isEmpty()) {
                List<ErrorImportacion> intra = validador.validar(importacionId, resultadoParser.filas());
                todosErrores.addAll(intra);
            }

            boolean tieneErrores = todosErrores.stream()
                    .anyMatch(e -> e.getNivel() == NivelError.ERROR);
            int filasRechazadas = tieneErrores ? (int) todosErrores.stream()
                    .filter(e -> e.getNivel() == NivelError.ERROR).count() : 0;
            int filasAdvert = (int) todosErrores.stream()
                    .filter(e -> e.getNivel() == NivelError.ADVERTENCIA).count();

            guardarResultadoValidacion(importacionId, todosErrores,
                    resultadoParser.totalFilas(), filasRechazadas, filasAdvert, tieneErrores,
                    im.getRutaArchivo());

        } catch (Exception e) {
            log.error("[IMPORTACION] Error validando importacion={}", importacionId, e);
            estadoService.marcarFallida(importacionId, "Error interno: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void guardarResultadoValidacion(UUID importacionId, List<ErrorImportacion> errores,
                                     int totalFilas, int filasRechazadas, int filasAdvertencia,
                                     boolean tieneErrores, String rutaArchivo) {
        ImportacionMensual im = repository.findById(importacionId)
                .orElseThrow(() -> new ImportacionMensualNoEncontradaException(importacionId));

        persistenciaService.guardarErroresBatch(errores);
        im.registrarResultadoValidacion(totalFilas, filasRechazadas, filasAdvertencia);

        if (tieneErrores) {
            im.transicionarA(EstadoImportacion.CON_ERRORES);
            im.limpiarRutaArchivo();
            repository.save(im);
            if (rutaArchivo != null) storage.eliminar(rutaArchivo);
        } else {
            im.transicionarA(EstadoImportacion.VALIDADA);
            repository.save(im);
            expirarValidadasAnteriores(im);
        }
    }

    private void expirarValidadasAnteriores(ImportacionMensual nueva) {
        List<ImportacionMensual> anteriores = repository
                .findBySistemaOrigenAndEstado(nueva.getSistemaOrigen(), EstadoImportacion.VALIDADA);
        for (ImportacionMensual ant : anteriores) {
            if (!ant.getId().equals(nueva.getId())) {
                String ruta = ant.getRutaArchivo();
                ant.transicionarA(EstadoImportacion.EXPIRADA);
                ant.limpiarRutaArchivo();
                repository.save(ant);
                if (ruta != null) storage.eliminar(ruta);
            }
        }
    }

    @Transactional(readOnly = true)
    ImportacionMensual cargarImportacion(UUID id) {
        return repository.findById(id).orElse(null);
    }
}
