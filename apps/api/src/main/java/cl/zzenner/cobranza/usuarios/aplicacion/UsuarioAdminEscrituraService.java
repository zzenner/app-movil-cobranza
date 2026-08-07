package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.api.SeguridadUsuarioModificadaEvent;
import cl.zzenner.cobranza.usuarios.dominio.*;
import cl.zzenner.cobranza.usuarios.infraestructura.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UsuarioAdminEscrituraService {

    private static final int BCRYPT_MAX_BYTES = 72;
    private static final int CONTRASENA_MIN_CHARS = 8;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final CodificadorContrasena codificadorContrasena;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public UsuarioAdminEscrituraService(UsuarioRepository usuarioRepository,
                                        RolRepository rolRepository,
                                        UsuarioRolRepository usuarioRolRepository,
                                        CodificadorContrasena codificadorContrasena,
                                        ApplicationEventPublisher eventos,
                                        Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.codificadorContrasena = codificadorContrasena;
        this.eventos = eventos;
        this.clock = clock;
    }

    public UUID crearUsuario(String nombreUsuario, String nombres, String apellidoPaterno,
                              String apellidoMaterno, String correo, String contrasenaCruda,
                              List<String> rolesIniciales, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);
        validarContrasena(contrasenaCruda);

        String userNorm = normalizar(nombreUsuario);
        if (usuarioRepository.existsByNombreUsuario(userNorm)) {
            throw new NombreUsuarioDuplicadoException(userNorm);
        }

        String correoNorm = correo != null && !correo.isBlank() ? normalizar(correo) : null;
        if (correoNorm != null && usuarioRepository.existsByCorreo(correoNorm)) {
            throw new CorreoDuplicadoException(correoNorm);
        }

        List<Rol> roles = rolesIniciales.stream()
                .map(codigo -> rolRepository.findByCodigo(codigo)
                        .orElseThrow(() -> new RolNoEncontradoException(codigo)))
                .toList();

        String hash = codificadorContrasena.codificar(contrasenaCruda);
        Usuario usuario = new Usuario(userNorm, nombres, apellidoPaterno,
                apellidoMaterno, correoNorm, hash);
        usuario = usuarioRepository.save(usuario);

        UUID nuevoId = usuario.getId();
        for (Rol rol : roles) {
            usuarioRolRepository.save(new UsuarioRol(nuevoId, rol.getId(), actorId));
        }

        return nuevoId;
    }

    public void actualizarDatosBasicos(UUID id, String nombres, String apellidoPaterno,
                                        String apellidoMaterno, String correo,
                                        long versionCliente, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        if (usuario.getVersion() != versionCliente) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
                    Usuario.class, id);
        }

        String correoNorm = correo != null && !correo.isBlank() ? normalizar(correo) : null;
        if (correoNorm != null && usuarioRepository.existsByCorreoAndIdNot(correoNorm, id)) {
            throw new CorreoDuplicadoException(correoNorm);
        }

        usuario.setNombres(nombres);
        usuario.setApellidoPaterno(apellidoPaterno);
        usuario.setApellidoMaterno(apellidoMaterno);
        usuario.setCorreo(correoNorm);
        usuarioRepository.save(usuario);
    }

    public void activar(UUID id, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    public void desactivar(UUID id, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);
        verificarNoPropiaCuenta(id, actorId);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        verificarNoUltimoAdministrador(id);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        eventos.publishEvent(new SeguridadUsuarioModificadaEvent(id,
                SeguridadUsuarioModificadaEvent.Motivo.DESACTIVACION));
    }

    public void bloquear(UUID id, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);
        verificarNoPropiaCuenta(id, actorId);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        verificarNoUltimoAdministrador(id);

        usuario.bloquear();
        usuarioRepository.save(usuario);

        eventos.publishEvent(new SeguridadUsuarioModificadaEvent(id,
                SeguridadUsuarioModificadaEvent.Motivo.BLOQUEO_ADMINISTRATIVO));
    }

    public void desbloquear(UUID id, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        usuario.desbloquear();
        usuarioRepository.save(usuario);
    }

    public void restablecerContrasena(UUID id, String nuevaContrasena, UUID actorId) {
        Instant ahora = clock.instant();
        validarActor(actorId, ahora);
        validarContrasena(nuevaContrasena);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        String hash = codificadorContrasena.codificar(nuevaContrasena);
        usuario.setContrasenaHash(hash);
        usuarioRepository.save(usuario);

        eventos.publishEvent(new SeguridadUsuarioModificadaEvent(id,
                SeguridadUsuarioModificadaEvent.Motivo.RESTABLECIMIENTO_CONTRASENA));
    }

    private void validarActor(UUID actorId, Instant ahora) {
        Usuario actor = usuarioRepository.findById(actorId)
                .orElseThrow(() -> new AccessDeniedException("Actor no encontrado"));

        if (!actor.isActivo() || actor.isBloqueado()
                || (actor.getBloqueadoHasta() != null && ahora.isBefore(actor.getBloqueadoHasta()))) {
            throw new AccessDeniedException("El actor no tiene acceso activo");
        }
    }

    private void verificarNoPropiaCuenta(UUID usuarioId, UUID actorId) {
        if (usuarioId.equals(actorId)) {
            throw new AutoLockoutException();
        }
    }

    private void verificarNoUltimoAdministrador(UUID usuarioId) {
        long otrosAdmins = usuarioRolRepository.contarAdministradoresActivosExcluyendo(usuarioId);
        if (otrosAdmins == 0) {
            throw new SinAdministradorActivoException();
        }
    }

    private void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar en blanco");
        }
        if (contrasena.length() < CONTRASENA_MIN_CHARS) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + CONTRASENA_MIN_CHARS + " caracteres");
        }
        int bytes = contrasena.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > BCRYPT_MAX_BYTES) {
            throw new IllegalArgumentException(
                    "La contraseña supera el límite de " + BCRYPT_MAX_BYTES + " bytes UTF-8");
        }
    }

    private String normalizar(String valor) {
        return valor.toLowerCase().strip();
    }
}
