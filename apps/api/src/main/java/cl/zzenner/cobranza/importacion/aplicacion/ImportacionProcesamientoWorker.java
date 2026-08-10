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
import java.util.List;
import java.util.UUID;

@Component
class ImportacionProcesamientoWorker {

    private static final Logger log = LoggerFactory.getLogger(ImportacionProcesamientoWorker.class);

    private final ImportacionEstadoService estadoService;
    private final ImportacionMensualRepository repository;
    private final ArchivoImportacionStorage storage;
    private final CsvImportacionParser parser;
    private final ImportacionPersistenciaService persistenciaService;

    ImportacionProcesamientoWorker(ImportacionEstadoService estadoService,
                                    ImportacionMensualRepository repository,
                                    ArchivoImportacionStorage storage,
                                    CsvImportacionParser parser,
                                    ImportacionPersistenciaService persistenciaService) {
        this.estadoService = estadoService;
        this.repository = repository;
        this.storage = storage;
        this.parser = parser;
        this.persistenciaService = persistenciaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("importacionExecutor")
    public void onProcesar(ProcesarImportacionEvento evento) {
        UUID importacionId = evento.importacionId();
        log.info("[IMPORTACION] Iniciando procesamiento importacion={}", importacionId);

        try {
            ImportacionMensual im = cargarImportacion(importacionId);
            if (im == null || im.getRutaArchivo() == null) {
                estadoService.marcarFallida(importacionId, "Archivo no disponible para procesamiento");
                return;
            }

            CsvImportacionParser.ResultadoParser resultadoParser;
            try (InputStream is = storage.leer(im.getRutaArchivo())) {
                resultadoParser = parser.parsear(importacionId, is);
            }

            List<FilaCsv> filas = resultadoParser.filas();
            ResultadoProcesamiento resultado = persistenciaService.procesarFilas(
                    filas, im.getCarteraId(), im.getPeriodo(), im.getSistemaOrigen(), importacionId);

            guardarResultadoProcesamiento(importacionId, resultado, im.getRutaArchivo());

        } catch (Exception e) {
            log.error("[IMPORTACION] Error procesando importacion={}", importacionId, e);
            estadoService.marcarFallida(importacionId, "Error interno durante procesamiento: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void guardarResultadoProcesamiento(UUID importacionId, ResultadoProcesamiento resultado, String rutaArchivo) {
        ImportacionMensual im = repository.findById(importacionId)
                .orElseThrow(() -> new ImportacionMensualNoEncontradaException(importacionId));

        im.registrarResultadoProcesamiento(
                resultado.filasProcesadas(), resultado.filasRechazadas(), resultado.filasAdvertencia(),
                resultado.personasCreadas(), resultado.personasActualizadas(),
                resultado.operacionesCreadas(), resultado.operacionesActualizadas(),
                resultado.cuotasCreadas(), resultado.cuotasActualizadas());
        im.transicionarA(EstadoImportacion.COMPLETADA);
        im.limpiarRutaArchivo();
        repository.save(im);

        if (rutaArchivo != null) storage.eliminar(rutaArchivo);
        log.info("[IMPORTACION] Completada importacion={}", importacionId);
    }

    @Transactional(readOnly = true)
    ImportacionMensual cargarImportacion(UUID id) {
        return repository.findById(id).orElse(null);
    }
}
