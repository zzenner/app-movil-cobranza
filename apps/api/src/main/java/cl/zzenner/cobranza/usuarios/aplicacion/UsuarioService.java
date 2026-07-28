package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.dominio.*;
import cl.zzenner.cobranza.usuarios.infraestructura.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final CodificadorContrasena codificadorContrasena;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          UsuarioRolRepository usuarioRolRepository,
                          CodificadorContrasena codificadorContrasena) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.codificadorContrasena = codificadorContrasena;
    }

    /**
     * Crea un nuevo usuario. Normaliza nombre_usuario y correo antes de persistir.
     *
     * @return UUID del usuario creado
     */
    public UUID crearUsuario(String nombreUsuario, String nombres, String apellidoPaterno,
                              String apellidoMaterno, String correo, String contrasenaCruda) {
        String userNorm = normalizar(nombreUsuario);
        if (usuarioRepository.existsByNombreUsuario(userNorm)) {
            throw new NombreUsuarioDuplicadoException(userNorm);
        }
        String correoNorm = correo != null && !correo.isBlank() ? normalizar(correo) : null;
        if (correoNorm != null && usuarioRepository.existsByCorreo(correoNorm)) {
            throw new CorreoDuplicadoException(correoNorm);
        }

        String hash = codificadorContrasena.codificar(contrasenaCruda);

        Usuario usuario = new Usuario(userNorm, nombres, apellidoPaterno,
                apellidoMaterno, correoNorm, hash);

        return usuarioRepository.save(usuario).getId();
    }

    /**
     * Asigna un rol a un usuario.
     * Si el usuario ya tiene el rol activo, lanza excepción.
     */
    public void asignarRol(UUID usuarioId, String codigoRol, UUID asignadoPor) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));

        Rol rol = rolRepository.findByCodigo(codigoRol)
                .orElseThrow(() -> new RolNoEncontradoException(codigoRol));

        if (usuarioRolRepository.existsByUsuarioIdAndRolIdAndActivoTrue(usuarioId, rol.getId())) {
            throw new IllegalStateException(
                    "El usuario ya tiene el rol activo: " + codigoRol);
        }

        usuarioRolRepository.save(new UsuarioRol(usuarioId, rol.getId(), asignadoPor));
    }

    /**
     * Finaliza la asignación activa de un rol a un usuario (historial conservado).
     */
    public void finalizarRol(UUID usuarioId, String codigoRol) {
        Rol rol = rolRepository.findByCodigo(codigoRol)
                .orElseThrow(() -> new RolNoEncontradoException(codigoRol));

        usuarioRolRepository.findAllByUsuarioIdAndActivoTrue(usuarioId).stream()
                .filter(ur -> ur.getRolId().equals(rol.getId()))
                .findFirst()
                .ifPresent(ur -> {
                    ur.finalizar();
                    usuarioRolRepository.save(ur);
                });
    }

    private String normalizar(String valor) {
        return valor.toLowerCase().strip();
    }
}
