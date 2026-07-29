package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.api.CredencialesUsuario;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import cl.zzenner.cobranza.usuarios.dominio.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class UsuarioConsultaApiImpl implements UsuarioConsultaApi {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final PermisoRepository permisoRepository;
    private final Clock clock;

    UsuarioConsultaApiImpl(UsuarioRepository usuarioRepository,
                            UsuarioRolRepository usuarioRolRepository,
                            RolRepository rolRepository,
                            RolPermisoRepository rolPermisoRepository,
                            PermisoRepository permisoRepository,
                            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.rolRepository = rolRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.permisoRepository = permisoRepository;
        this.clock = clock;
    }

    @Override
    public boolean existeYEstaHabilitado(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(u -> u.estaHabilitadoEn(clock.instant()))
                .orElse(false);
    }

    @Override
    public Optional<CredencialesUsuario> buscarParaAutenticacion(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario)
                .map(this::construirCredenciales);
    }

    @Override
    public Optional<CredencialesUsuario> buscarCredencialesPorId(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(this::construirCredenciales);
    }

    @Override
    @Transactional
    public int registrarIntentoFallido(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        // No incrementar si está en período de bloqueo temporal activo
        Instant ahora = clock.instant();
        if (usuario.getBloqueadoHasta() != null && ahora.isBefore(usuario.getBloqueadoHasta())) {
            return usuario.getIntentosFallidos();
        }
        usuario.registrarIntentoFallido();
        usuarioRepository.save(usuario);
        return usuario.getIntentosFallidos();
    }

    @Override
    @Transactional
    public void aplicarBloqueoTemporal(UUID usuarioId, Instant hasta) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        usuario.setBloqueadoHasta(hasta);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void registrarAccesoExitoso(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        usuario.registrarAccesoExitoso();
        usuarioRepository.save(usuario);
    }

    private CredencialesUsuario construirCredenciales(Usuario u) {
        List<UUID> rolIds = usuarioRolRepository.findAllByUsuarioIdAndActivoTrue(u.getId())
                .stream().map(ur -> ur.getRolId()).toList();

        List<String> codigosRoles = rolIds.stream()
                .flatMap(rid -> rolRepository.findById(rid).stream())
                .map(r -> r.getCodigo())
                .toList();

        List<String> codigosPermisos = rolIds.stream()
                .flatMap(rid -> rolPermisoRepository.findAllByIdRolId(rid).stream())
                .map(rp -> rp.getId().getPermisoId())
                .flatMap(pid -> permisoRepository.findById(pid).stream())
                .filter(p -> p.isActivo())
                .map(p -> p.getCodigo())
                .distinct()
                .toList();

        return new CredencialesUsuario(
                u.getId(), u.getNombreUsuario(), u.getContrasenaHash(),
                u.isActivo(), u.isBloqueado(), u.getBloqueadoHasta(),
                u.getIntentosFallidos(), codigosRoles, codigosPermisos);
    }
}
