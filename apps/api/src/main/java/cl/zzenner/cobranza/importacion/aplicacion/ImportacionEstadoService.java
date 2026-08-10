package cl.zzenner.cobranza.importacion.aplicacion;

import cl.zzenner.cobranza.importacion.dominio.EstadoImportacion;
import cl.zzenner.cobranza.importacion.dominio.ImportacionMensual;
import cl.zzenner.cobranza.importacion.dominio.ImportacionMensualNoEncontradaException;
import cl.zzenner.cobranza.importacion.infraestructura.ImportacionMensualRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ImportacionEstadoService {

    private final ImportacionMensualRepository repository;

    public ImportacionEstadoService(ImportacionMensualRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean intentarTransicionarAValidando(UUID importacionId) {
        ImportacionMensual im = repository.findByIdConBloqueo(importacionId)
                .orElseThrow(() -> new ImportacionMensualNoEncontradaException(importacionId));

        if (im.getEstado() != EstadoImportacion.RECIBIDA) {
            return false;
        }
        im.transicionarA(EstadoImportacion.VALIDANDO);
        repository.save(im);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean intentarTransicionarAProcesando(UUID importacionId) {
        ImportacionMensual im = repository.findByIdConBloqueo(importacionId)
                .orElseThrow(() -> new ImportacionMensualNoEncontradaException(importacionId));

        if (im.getEstado() != EstadoImportacion.VALIDADA) {
            return false;
        }
        im.transicionarA(EstadoImportacion.PROCESANDO);
        repository.save(im);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallida(UUID importacionId, String mensajeError) {
        repository.findById(importacionId).ifPresent(im -> {
            im.registrarError(mensajeError);
            im.transicionarA(EstadoImportacion.FALLIDA);
            repository.save(im);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarExpirada(UUID importacionId) {
        repository.findById(importacionId).ifPresent(im -> {
            if (im.getEstado() == EstadoImportacion.VALIDADA) {
                im.transicionarA(EstadoImportacion.EXPIRADA);
                im.limpiarRutaArchivo();
                repository.save(im);
            }
        });
    }
}
