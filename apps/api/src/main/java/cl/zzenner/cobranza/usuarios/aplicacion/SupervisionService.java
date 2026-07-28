package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.dominio.*;
import cl.zzenner.cobranza.usuarios.infraestructura.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class SupervisionService {

    private final SupervisionRepository supervisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;

    public SupervisionService(SupervisionRepository supervisionRepository,
                               UsuarioRepository usuarioRepository,
                               UsuarioRolRepository usuarioRolRepository,
                               RolRepository rolRepository) {
        this.supervisionRepository = supervisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.rolRepository = rolRepository;
    }

    /**
     * Asigna un ejecutivo a un supervisor.
     * Valida roles y unicidad de supervisión activa.
     */
    public UUID asignarEjecutivo(UUID supervisorId, UUID ejecutivoId, LocalDate fechaInicio) {
        validarRolActivo(supervisorId, CodigoRol.SUPERVISOR, "El supervisor debe tener el rol SUPERVISOR.");
        validarRolActivo(ejecutivoId, CodigoRol.EJECUTIVO_TERRENO,
                "El ejecutivo debe tener el rol EJECUTIVO_TERRENO.");

        if (supervisionRepository.existsByEjecutivoIdAndActivoTrue(ejecutivoId)) {
            throw new RelacionSupervisionInvalidaException(
                    "El ejecutivo ya tiene una supervisión activa.");
        }

        SupervisionUsuario supervision = new SupervisionUsuario(supervisorId, ejecutivoId, fechaInicio);
        return supervisionRepository.save(supervision).getId();
    }

    /**
     * Finaliza la relación de supervisión activa de un ejecutivo.
     */
    public void finalizarSupervision(UUID ejecutivoId, LocalDate fechaTermino) {
        SupervisionUsuario supervision = supervisionRepository
                .findByEjecutivoIdAndActivoTrue(ejecutivoId)
                .orElseThrow(() -> new RelacionSupervisionInvalidaException(
                        "No existe supervisión activa para el ejecutivo: " + ejecutivoId));
        supervision.finalizar(fechaTermino);
        supervisionRepository.save(supervision);
    }

    private void validarRolActivo(UUID usuarioId, CodigoRol codigoRol, String mensaje) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));

        Rol rol = rolRepository.findByCodigo(codigoRol.name())
                .orElseThrow(() -> new RolNoEncontradoException(codigoRol.name()));

        if (!usuarioRolRepository.existsByUsuarioIdAndRolIdAndActivoTrue(usuarioId, rol.getId())) {
            throw new RelacionSupervisionInvalidaException(mensaje);
        }
    }
}
