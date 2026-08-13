package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.dominio.*;
import cl.zzenner.cobranza.usuarios.infraestructura.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SupervisionAdminQueryService {

    public record ItemEjecutivoAdmin(
            UUID usuarioId,
            String nombreUsuario,
            String nombreCompleto,
            boolean activo,
            String codigoEjecutivoOrigen,
            UUID supervisorId,
            String supervisorNombre) {}

    public record ItemSupervisorAdmin(
            UUID usuarioId,
            String nombreUsuario,
            String nombreCompleto) {}

    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final UsuarioRepository usuarioRepository;
    private final SupervisionRepository supervisionRepository;

    public SupervisionAdminQueryService(RolRepository rolRepository,
                                        UsuarioRolRepository usuarioRolRepository,
                                        UsuarioRepository usuarioRepository,
                                        SupervisionRepository supervisionRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.usuarioRepository = usuarioRepository;
        this.supervisionRepository = supervisionRepository;
    }

    public List<ItemEjecutivoAdmin> listarEjecutivos(String nombreUsuario, String codigo,
                                                      String supervisorIdParam, boolean sinSupervisor) {
        Rol rolEjecutivo = rolRepository.findByCodigo(CodigoRol.EJECUTIVO_TERRENO.name())
                .orElseThrow(() -> new RolNoEncontradoException(CodigoRol.EJECUTIVO_TERRENO.name()));

        List<UUID> ejecutivoIds = usuarioRolRepository
                .findAllByRolIdAndActivoTrue(rolEjecutivo.getId()).stream()
                .map(UsuarioRol::getUsuarioId)
                .distinct()
                .toList();

        if (ejecutivoIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Usuario> ejecutivos = usuarioRepository.findAllByIdIn(ejecutivoIds);

        List<SupervisionUsuario> supervisionesActivas =
                supervisionRepository.findAllByEjecutivoIdInAndActivoTrue(ejecutivoIds);

        Map<UUID, SupervisionUsuario> supervisionPorEjecutivo = supervisionesActivas.stream()
                .collect(Collectors.toMap(SupervisionUsuario::getEjecutivoId, s -> s));

        Set<UUID> supervisorIds = supervisionesActivas.stream()
                .map(SupervisionUsuario::getSupervisorId)
                .collect(Collectors.toSet());

        Map<UUID, Usuario> supervisoresMap = supervisorIds.isEmpty()
                ? Collections.emptyMap()
                : usuarioRepository.findAllByIdIn(supervisorIds).stream()
                        .collect(Collectors.toMap(Usuario::getId, u -> u));

        UUID filtroSupervisorId = supervisorIdParam != null && !supervisorIdParam.isBlank()
                ? UUID.fromString(supervisorIdParam) : null;

        return ejecutivos.stream()
                .filter(e -> filtrarPorNombre(e, nombreUsuario))
                .filter(e -> filtrarPorCodigo(e, codigo))
                .filter(e -> filtrarPorSupervisorId(e, supervisionPorEjecutivo, filtroSupervisorId))
                .filter(e -> !sinSupervisor || !supervisionPorEjecutivo.containsKey(e.getId()))
                .map(e -> {
                    SupervisionUsuario sup = supervisionPorEjecutivo.get(e.getId());
                    UUID supId = sup != null ? sup.getSupervisorId() : null;
                    String supNombre = supId != null ? nombreCompleto(supervisoresMap.get(supId)) : null;
                    return new ItemEjecutivoAdmin(
                            e.getId(),
                            e.getNombreUsuario(),
                            nombreCompleto(e),
                            e.isActivo(),
                            e.getCodigoEjecutivoOrigen(),
                            supId,
                            supNombre);
                })
                .sorted(Comparator.comparing(ItemEjecutivoAdmin::nombreCompleto))
                .toList();
    }

    public List<ItemSupervisorAdmin> listarSupervisoresActivos() {
        Rol rolSupervisor = rolRepository.findByCodigo(CodigoRol.SUPERVISOR.name())
                .orElseThrow(() -> new RolNoEncontradoException(CodigoRol.SUPERVISOR.name()));

        List<UUID> supervisorIds = usuarioRolRepository
                .findAllByRolIdAndActivoTrue(rolSupervisor.getId()).stream()
                .map(UsuarioRol::getUsuarioId)
                .distinct()
                .toList();

        if (supervisorIds.isEmpty()) {
            return Collections.emptyList();
        }

        return usuarioRepository.findAllByIdIn(supervisorIds).stream()
                .filter(Usuario::isActivo)
                .map(u -> new ItemSupervisorAdmin(u.getId(), u.getNombreUsuario(), nombreCompleto(u)))
                .sorted(Comparator.comparing(ItemSupervisorAdmin::nombreCompleto))
                .toList();
    }

    private boolean filtrarPorNombre(Usuario e, String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) return true;
        String filtro = nombreUsuario.strip().toLowerCase();
        return e.getNombreUsuario().toLowerCase().contains(filtro)
                || e.getNombres().toLowerCase().contains(filtro)
                || e.getApellidoPaterno().toLowerCase().contains(filtro);
    }

    private boolean filtrarPorCodigo(Usuario e, String codigo) {
        if (codigo == null || codigo.isBlank()) return true;
        return codigo.strip().equals(e.getCodigoEjecutivoOrigen());
    }

    private boolean filtrarPorSupervisorId(Usuario e,
                                            Map<UUID, SupervisionUsuario> supervisionPorEjecutivo,
                                            UUID filtroSupervisorId) {
        if (filtroSupervisorId == null) return true;
        SupervisionUsuario sup = supervisionPorEjecutivo.get(e.getId());
        return sup != null && filtroSupervisorId.equals(sup.getSupervisorId());
    }

    private static String nombreCompleto(Usuario u) {
        if (u == null) return null;
        String ap = u.getApellidoPaterno();
        String am = u.getApellidoMaterno();
        String apellidos = (am != null && !am.isBlank()) ? ap + " " + am : ap;
        return u.getNombres() + " " + apellidos;
    }
}
