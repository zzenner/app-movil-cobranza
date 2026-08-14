package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.api.UsuarioSeedApi;
import cl.zzenner.cobranza.usuarios.dominio.NombreUsuarioDuplicadoException;
import cl.zzenner.cobranza.usuarios.dominio.Usuario;
import cl.zzenner.cobranza.usuarios.infraestructura.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
class UsuarioSeedService implements UsuarioSeedApi {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    UsuarioSeedService(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    public boolean crearSiNoExiste(String nombreUsuario, String password, String email, String rol) {
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario.toLowerCase().strip())) {
            return false;
        }
        try {
            UUID id = usuarioService.crearUsuario(nombreUsuario, "Dev", "Admin", null, email, password);
            usuarioService.asignarRol(id, rol, null);
            return true;
        } catch (NombreUsuarioDuplicadoException e) {
            return false;
        }
    }

    @Override
    public UUID crearSupervisorDemoSiNoExiste(String nombreUsuario, String nombres, String apellido,
                                               String password) {
        String usernorm = nombreUsuario.toLowerCase().strip();
        return usuarioRepository.findByNombreUsuario(usernorm)
                .map(Usuario::getId)
                .orElseGet(() -> {
                    try {
                        UUID id = usuarioService.crearUsuario(nombreUsuario, nombres, apellido,
                                null, null, password);
                        usuarioService.asignarRol(id, "SUPERVISOR", null);
                        return id;
                    } catch (NombreUsuarioDuplicadoException e) {
                        return usuarioRepository.findByNombreUsuario(usernorm)
                                .orElseThrow().getId();
                    }
                });
    }

    @Override
    public UUID crearEjecutivoDemoSiNoExiste(String nombreUsuario, String nombres, String apellido,
                                              String password, String codigoEjecutivo) {
        String usernorm = nombreUsuario.toLowerCase().strip();

        // Si el username ya existe, reusar sin modificar
        Optional<Usuario> porUsername = usuarioRepository.findByNombreUsuario(usernorm);
        if (porUsername.isPresent()) {
            return porUsername.get().getId();
        }

        // Si el código ya fue asignado a otro usuario existente, reusar ese usuario
        Optional<Usuario> porCodigo = usuarioRepository.findByCodigoEjecutivoOrigen(codigoEjecutivo);
        if (porCodigo.isPresent()) {
            return porCodigo.get().getId();
        }

        // Crear nuevo ejecutivo con código de origen
        try {
            UUID id = usuarioService.crearUsuario(nombreUsuario, nombres, apellido,
                    null, null, password);
            usuarioService.asignarRol(id, "EJECUTIVO_TERRENO", null);
            // Persist el código exactamente (preserva ceros iniciales al ser String)
            Usuario u = usuarioRepository.findById(id).orElseThrow();
            u.setCodigoEjecutivoOrigen(codigoEjecutivo);
            usuarioRepository.save(u);
            return id;
        } catch (NombreUsuarioDuplicadoException e) {
            return usuarioRepository.findByNombreUsuario(usernorm).orElseThrow().getId();
        }
    }

    @Override
    public UUID findIdByNombreUsuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario.toLowerCase().strip())
                .map(Usuario::getId)
                .orElse(null);
    }
}
