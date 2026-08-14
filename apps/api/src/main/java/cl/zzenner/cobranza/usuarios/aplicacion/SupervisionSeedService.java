package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.api.SupervisionSeedApi;
import cl.zzenner.cobranza.usuarios.infraestructura.SupervisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
class SupervisionSeedService implements SupervisionSeedApi {

    private final SupervisionService supervisionService;
    private final SupervisionRepository supervisionRepository;

    SupervisionSeedService(SupervisionService supervisionService,
                            SupervisionRepository supervisionRepository) {
        this.supervisionService = supervisionService;
        this.supervisionRepository = supervisionRepository;
    }

    @Override
    public void asignarEjecutivoSiNoAsignado(UUID supervisorId, UUID ejecutivoId) {
        var actual = supervisionRepository.findByEjecutivoIdAndActivoTrue(ejecutivoId);

        if (actual.isPresent()) {
            if (actual.get().getSupervisorId().equals(supervisorId)) {
                return; // ya tiene el supervisor correcto
            }
            // tiene otro supervisor — reasignar sin try-catch para no marcar rollback-only
            supervisionService.reasignarEjecutivo(supervisorId, ejecutivoId, LocalDate.now());
        } else {
            supervisionService.asignarEjecutivo(supervisorId, ejecutivoId, LocalDate.now());
        }
    }
}
