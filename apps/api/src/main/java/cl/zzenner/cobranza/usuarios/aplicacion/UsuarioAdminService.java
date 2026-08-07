package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.dominio.*;
import cl.zzenner.cobranza.usuarios.infraestructura.*;
import cl.zzenner.cobranza.usuarios.web.*;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final PermisoRepository permisoRepository;
    private final SupervisionRepository supervisionRepository;
    private final Clock clock;

    public UsuarioAdminService(UsuarioRepository usuarioRepository,
                               UsuarioRolRepository usuarioRolRepository,
                               RolRepository rolRepository,
                               RolPermisoRepository rolPermisoRepository,
                               PermisoRepository permisoRepository,
                               SupervisionRepository supervisionRepository,
                               Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.rolRepository = rolRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.permisoRepository = permisoRepository;
        this.supervisionRepository = supervisionRepository;
        this.clock = clock;
    }

    public RespuestaListadoUsuarios listar(int pagina, int tamanio,
                                           String nombreUsuario, String estado, String rol) {
        validarFiltroRol(rol);

        Pageable pageable = PageRequest.of(pagina, tamanio,
                Sort.by(Sort.Order.asc("nombreUsuario"), Sort.Order.asc("id")));

        Specification<Usuario> spec = construirSpecificacion(nombreUsuario, estado, rol);
        Page<Usuario> page = usuarioRepository.findAll(spec, pageable);

        List<Usuario> usuarios = page.getContent();
        if (usuarios.isEmpty()) {
            return new RespuestaListadoUsuarios(List.of(), pagina, tamanio,
                    page.getTotalElements(), page.getTotalPages());
        }

        List<UUID> ids = usuarios.stream().map(Usuario::getId).toList();
        Instant ahora = Instant.now(clock);

        Map<UUID, List<String>> rolesPorUsuario = cargarRolesEnBatch(ids);
        Map<UUID, UUID> supervisorIdPorEjecutivo = cargarSupervisionEnBatch(ids);
        Map<UUID, String> nombresPorSupervisorId = cargarNombresSupervisores(supervisorIdPorEjecutivo);

        List<ItemListadoUsuarioAdmin> contenido = usuarios.stream()
                .map(u -> {
                    UUID supId = supervisorIdPorEjecutivo.get(u.getId());
                    String supNombre = supId != null ? nombresPorSupervisorId.get(supId) : null;
                    return new ItemListadoUsuarioAdmin(
                            u.getId(),
                            u.getNombreUsuario(),
                            u.getNombres(),
                            u.getApellidoPaterno(),
                            u.getApellidoMaterno(),
                            u.getCorreo(),
                            calcularEstado(u, ahora),
                            u.getBloqueadoHasta(),
                            rolesPorUsuario.getOrDefault(u.getId(), List.of()),
                            supId,
                            supNombre,
                            u.getFechaCreacion()
                    );
                })
                .toList();

        return new RespuestaListadoUsuarios(contenido, pagina, tamanio,
                page.getTotalElements(), page.getTotalPages());
    }

    public DetalleUsuarioAdmin obtenerDetalle(UUID id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        Instant ahora = Instant.now(clock);
        List<UsuarioRol> usuarioRoles = usuarioRolRepository.findAllByUsuarioIdAndActivoTrue(id);

        Map<UUID, Rol> rolesPorId = cargarRolesPorId(
                usuarioRoles.stream().map(UsuarioRol::getRolId).toList());

        List<RolVigenteAdmin> rolesVigentes = usuarioRoles.stream()
                .filter(ur -> rolesPorId.containsKey(ur.getRolId()))
                .map(ur -> new RolVigenteAdmin(
                        rolesPorId.get(ur.getRolId()).getCodigo(),
                        ur.getFechaAsignacion()))
                .toList();

        List<String> permisosEfectivos = calcularPermisosEfectivos(
                usuarioRoles.stream().map(UsuarioRol::getRolId).toList());

        Optional<SupervisionUsuario> supervision =
                supervisionRepository.findByEjecutivoIdAndActivoTrue(id);

        UUID supervisorId = supervision.map(SupervisionUsuario::getSupervisorId).orElse(null);
        String supervisorNombreUsuario = null;
        if (supervisorId != null) {
            supervisorNombreUsuario = usuarioRepository.findById(supervisorId)
                    .map(Usuario::getNombreUsuario)
                    .orElse(null);
        }

        return new DetalleUsuarioAdmin(
                u.getId(),
                u.getNombreUsuario(),
                u.getNombres(),
                u.getApellidoPaterno(),
                u.getApellidoMaterno(),
                u.getCorreo(),
                calcularEstado(u, ahora),
                u.isActivo(),
                u.isBloqueado(),
                u.getBloqueadoHasta(),
                u.getIntentosFallidos(),
                u.getFechaUltimoAcceso(),
                rolesVigentes,
                permisosEfectivos,
                supervisorId,
                supervisorNombreUsuario,
                u.getFechaCreacion(),
                u.getFechaActualizacion(),
                u.getVersion()
        );
    }

    public EstadoUsuario calcularEstado(Usuario u, Instant ahora) {
        if (!u.isActivo()) return EstadoUsuario.INACTIVO;
        if (u.isBloqueado()) return EstadoUsuario.BLOQUEADO;
        if (u.getBloqueadoHasta() != null && ahora.isBefore(u.getBloqueadoHasta())) {
            return EstadoUsuario.BLOQUEADO_TEMPORAL;
        }
        return EstadoUsuario.ACTIVO;
    }

    private void validarFiltroRol(String codigoRol) {
        if (codigoRol == null || codigoRol.isBlank()) return;
        if (!rolRepository.existsByCodigo(codigoRol)) {
            throw new IllegalArgumentException("Código de rol inexistente: " + codigoRol);
        }
    }

    private Specification<Usuario> construirSpecificacion(String nombreUsuario,
                                                           String estado, String codigoRol) {
        Specification<Usuario> spec = Specification.where(null);

        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            String patron = "%" + nombreUsuario.strip().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nombreUsuario")), patron));
        }

        if (estado != null && !estado.isBlank()) {
            spec = spec.and(especificacionEstado(estado));
        }

        if (codigoRol != null && !codigoRol.isBlank()) {
            spec = spec.and(especificacionRol(codigoRol));
        }

        return spec;
    }

    private Specification<Usuario> especificacionEstado(String estado) {
        return (root, query, cb) -> switch (estado) {
            case "INACTIVO" -> cb.equal(root.get("activo"), false);
            case "BLOQUEADO" -> cb.and(
                    cb.equal(root.get("activo"), true),
                    cb.equal(root.get("bloqueado"), true));
            case "BLOQUEADO_TEMPORAL" -> {
                Instant ahora = Instant.now(clock);
                yield cb.and(
                        cb.equal(root.get("activo"), true),
                        cb.equal(root.get("bloqueado"), false),
                        cb.isNotNull(root.get("bloqueadoHasta")),
                        cb.greaterThan(root.get("bloqueadoHasta"), ahora));
            }
            case "ACTIVO" -> {
                Instant ahora = Instant.now(clock);
                yield cb.and(
                        cb.equal(root.get("activo"), true),
                        cb.equal(root.get("bloqueado"), false),
                        cb.or(
                                cb.isNull(root.get("bloqueadoHasta")),
                                cb.lessThanOrEqualTo(root.get("bloqueadoHasta"), ahora)));
            }
            default -> throw new IllegalArgumentException("Estado inválido: " + estado);
        };
    }

    private Specification<Usuario> especificacionRol(String codigoRol) {
        return (root, query, cb) -> {
            assert query != null;
            Subquery<UUID> subquery = query.subquery(UUID.class);
            var urRoot = subquery.from(cl.zzenner.cobranza.usuarios.dominio.UsuarioRol.class);
            var rolSubquery = query.subquery(UUID.class);
            var rolRoot = rolSubquery.from(cl.zzenner.cobranza.usuarios.dominio.Rol.class);
            rolSubquery.select(rolRoot.get("id"))
                    .where(cb.equal(rolRoot.get("codigo"), codigoRol));

            subquery.select(urRoot.get("usuarioId"))
                    .where(cb.and(
                            cb.equal(urRoot.get("usuarioId"), root.get("id")),
                            cb.equal(urRoot.get("activo"), true),
                            urRoot.get("rolId").in(rolSubquery)));

            return cb.exists(subquery);
        };
    }

    private Map<UUID, List<String>> cargarRolesEnBatch(List<UUID> usuarioIds) {
        List<UsuarioRol> todos = usuarioRolRepository.findAllByUsuarioIdInAndActivoTrue(usuarioIds);
        if (todos.isEmpty()) return Map.of();

        Set<UUID> rolIds = todos.stream().map(UsuarioRol::getRolId).collect(Collectors.toSet());
        Map<UUID, String> codigoPorRolId = rolRepository.findAllById(rolIds).stream()
                .collect(Collectors.toMap(Rol::getId, Rol::getCodigo));

        Map<UUID, List<String>> resultado = new HashMap<>();
        for (UsuarioRol ur : todos) {
            String codigo = codigoPorRolId.get(ur.getRolId());
            if (codigo != null) {
                resultado.computeIfAbsent(ur.getUsuarioId(), k -> new ArrayList<>()).add(codigo);
            }
        }
        return resultado;
    }

    private Map<UUID, UUID> cargarSupervisionEnBatch(List<UUID> usuarioIds) {
        List<SupervisionUsuario> supervisiones =
                supervisionRepository.findAllByEjecutivoIdInAndActivoTrue(usuarioIds);
        return supervisiones.stream()
                .collect(Collectors.toMap(
                        SupervisionUsuario::getEjecutivoId,
                        SupervisionUsuario::getSupervisorId));
    }

    private Map<UUID, String> cargarNombresSupervisores(Map<UUID, UUID> supervisorIdPorEjecutivo) {
        if (supervisorIdPorEjecutivo.isEmpty()) return Map.of();
        Set<UUID> supervisorIds = new HashSet<>(supervisorIdPorEjecutivo.values());
        return usuarioRepository.findAllByIdIn(supervisorIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombreUsuario));
    }

    private Map<UUID, Rol> cargarRolesPorId(List<UUID> rolIds) {
        if (rolIds.isEmpty()) return Map.of();
        return rolRepository.findAllById(rolIds).stream()
                .collect(Collectors.toMap(Rol::getId, r -> r));
    }

    private List<String> calcularPermisosEfectivos(List<UUID> rolIds) {
        if (rolIds.isEmpty()) return List.of();
        Set<UUID> permisoIds = rolPermisoRepository.findAllByIdRolIdIn(rolIds).stream()
                .map(rp -> rp.getId().getPermisoId())
                .collect(Collectors.toSet());
        return permisoRepository.findAllById(permisoIds).stream()
                .map(Permiso::getCodigo)
                .sorted()
                .toList();
    }
}
