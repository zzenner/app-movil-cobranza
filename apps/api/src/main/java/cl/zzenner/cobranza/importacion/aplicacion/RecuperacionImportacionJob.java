package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.EstadoImportacion;
import cl.zzenner.cobranza.importacion.dominio.ImportacionMensual;
import cl.zzenner.cobranza.importacion.infraestructura.ImportacionMensualRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
class RecuperacionImportacionJob {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionImportacionJob.class);

    private final ImportacionMensualRepository repository;
    private final ArchivoImportacionStorage storage;
    private final ImportacionEstadoService estadoService;
    private final Clock clock;
    private final Duration thresholdHuerfanas;
    private final Duration ttlValidadas;

    RecuperacionImportacionJob(
            ImportacionMensualRepository repository,
            ArchivoImportacionStorage storage,
            ImportacionEstadoService estadoService,
            Clock clock,
            @Value("${app.importacion.recuperacion.threshold-huerfanas:PT2H}") Duration thresholdHuerfanas,
            @Value("${app.importacion.recuperacion.ttl-validadas:PT48H}") Duration ttlValidadas) {
        this.repository = repository;
        this.storage = storage;
        this.estadoService = estadoService;
        this.clock = clock;
        this.thresholdHuerfanas = thresholdHuerfanas;
        this.ttlValidadas = ttlValidadas;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarAlArrancar() {
        ejecutarRecuperacion();
    }

    @Scheduled(fixedDelayString = "${app.importacion.recuperacion.intervalo:PT30M}")
    @Transactional
    public void ejecutarRecuperacion() {
        Instant umbralHuerfanas = clock.instant().minus(thresholdHuerfanas);
        Instant umbralValidadas = clock.instant().minus(ttlValidadas);

        List<ImportacionMensual> huerfanas = repository.findHuerfanas(umbralHuerfanas);
        for (ImportacionMensual im : huerfanas) {
            log.warn("[IMPORTACION] Marcando huérfana como FALLIDA importacion={}", im.getId());
            estadoService.marcarFallida(im.getId(),
                    "Trabajo abandonado detectado por job de recuperación");
            if (im.getRutaArchivo() != null) {
                storage.eliminar(im.getRutaArchivo());
            }
        }

        List<ImportacionMensual> validadasExpiradas = repository.findValidadasExpiradas(umbralValidadas);
        for (ImportacionMensual im : validadasExpiradas) {
            log.info("[IMPORTACION] Expirando VALIDADA por TTL importacion={}", im.getId());
            estadoService.marcarExpirada(im.getId());
        }

        if (!huerfanas.isEmpty() || !validadasExpiradas.isEmpty()) {
            log.info("[IMPORTACION] Recuperación: {} huérfanas, {} validadas expiradas",
                    huerfanas.size(), validadasExpiradas.size());
        }
    }
}
