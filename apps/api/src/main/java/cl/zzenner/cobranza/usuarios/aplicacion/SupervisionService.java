package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.dominio.*;
import cl.zzenner.cobranza.usuarios.infraestructura.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
     * Valida roles, estado de usuario y unicidad de supervisión activa.
     */
    public UUID asignarEjecutivo(UUID supervisorId, UUID ejecutivoId, LocalDate fechaInicio) {
        validarUsuarioOperativo(supervisorId);
        validarRolActivo(supervisorId, CodigoRol.SUPERVISOR, "El supervisor debe tener el rol SUPERVISOR.");
        validarUsuarioOperativo(ejecutivoId);
        validarRolActivo(ejecutivoId, CodigoRol.EJECUTIVO_TERRENO,
                "El ejecutivo debe tener el rol EJECUTIVO_TERRENO.");

        Optional<SupervisionUsuario> actual = supervisionRepository.findByEjecutivoIdAndActivoTrue(ejecutivoId);
        if (actual.isPresent()) {
            if (actual.get().getSupervisorId().equals(supervisorId)) {
                return actual.get().getId();
            }
            throw new RelacionSupervisionInvalidaException(
                    "El ejecutivo ya tiene una supervisión activa. Use reasignar para cambiar el supervisor.");
        }

        SupervisionUsuario supervision = new SupervisionUsuario(supervisorId, ejecutivoId, fechaInicio);
        return supervisionRepository.save(supervision).getId();
    }

    /**
     * Reasigna un ejecutivo a un nuevo supervisor de forma atómica:
     * cierra la relación activa actual y crea una nueva.
     */
    public UUID reasignarEjecutivo(UUID nuevoSupervisorId, UUID ejecutivoId, LocalDate fecha) {
        validarUsuarioOperativo(nuevoSupervisorId);
        validarRolActivo(nuevoSupervisorId, CodigoRol.SUPERVISOR,
                "El nuevo supervisor debe tener el rol SUPERVISOR.");
        validarUsuarioOperativo(ejecutivoId);
        validarRolActivo(ejecutivoId, CodigoRol.EJECUTIVO_TERRENO,
                "El ejecutivo debe tener el rol EJECUTIVO_TERRENO.");

        Optional<SupervisionUsuario> actual = supervisionRepository.findByEjecutivoIdAndActivoTrue(ejecutivoId);
        if (actual.isPresent()) {
            if (actual.get().getSupervisorId().equals(nuevoSupervisorId)) {
                return actual.get().getId();
            }
            actual.get().finalizar(fecha);
            supervisionRepository.saveAndFlush(actual.get());
        }

        SupervisionUsuario nueva = new SupervisionUsuario(nuevoSupervisorId, ejecutivoId, fecha);
        return supervisionRepository.save(nueva).getId();
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

    /**
     * Actualiza el código de ejecutivo origen (identificador del sistema legado).
     * El código se guarda en texto para preservar ceros iniciales.
     */
    public void actualizarCodigoEjecutivo(UUID ejecutivoId, String codigo) {
        Usuario ejecutivo = usuarioRepository.findById(ejecutivoId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(ejecutivoId));

        validarRolActivo(ejecutivoId, CodigoRol.EJECUTIVO_TERRENO,
                "Solo los ejecutivos de terreno pueden tener código de ejecutivo origen.");

        String codigoNormalizado = (codigo != null && !codigo.isBlank()) ? codigo.strip() : null;

        if (codigoNormalizado != null) {
            boolean duplicado = usuarioRepository.existsByCodigoEjecutivoOrigenAndIdNot(
                    codigoNormalizado, ejecutivoId);
            if (duplicado) {
                throw new CodigoEjecutivoDuplicadoException(codigoNormalizado);
            }
        }

        ejecutivo.setCodigoEjecutivoOrigen(codigoNormalizado);
        usuarioRepository.save(ejecutivo);
    }

    /**
     * Consulta la supervisión activa de un ejecutivo, si existe.
     */
    @Transactional(readOnly = true)
    public Optional<SupervisionUsuario> obtenerSupervisionActiva(UUID ejecutivoId) {
        return supervisionRepository.findByEjecutivoIdAndActivoTrue(ejecutivoId);
    }

    /**
     * Retorna los IDs de ejecutivos supervisados por un supervisor dado.
     */
    @Transactional(readOnly = true)
    public List<UUID> obtenerEjecutivosDeSupervision(UUID supervisorId) {
        return supervisionRepository.findAllBySupervisorIdAndActivoTrue(supervisorId).stream()
                .map(SupervisionUsuario::getEjecutivoId)
                .toList();
    }

    private void validarUsuarioOperativo(UUID usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));
        if (!u.isActivo()) {
            throw new RelacionSupervisionInvalidaException(
                    "El usuario " + usuarioId + " está inactivo.");
        }
        if (u.isBloqueado()) {
            throw new RelacionSupervisionInvalidaException(
                    "El usuario " + usuarioId + " está bloqueado.");
        }
    }

    private void validarRolActivo(UUID usuarioId, CodigoRol codigoRol, String mensaje) {
        Rol rol = rolRepository.findByCodigo(codigoRol.name())
                .orElseThrow(() -> new RolNoEncontradoException(codigoRol.name()));
        if (!usuarioRolRepository.existsByUsuarioIdAndRolIdAndActivoTrue(usuarioId, rol.getId())) {
            throw new RelacionSupervisionInvalidaException(mensaje);
        }
    }
}
