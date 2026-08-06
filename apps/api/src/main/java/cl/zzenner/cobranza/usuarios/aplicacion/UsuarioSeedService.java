package cl.zzenner.cobranza.usuarios.aplicacion;

import cl.zzenner.cobranza.usuarios.api.UsuarioSeedApi;
import cl.zzenner.cobranza.usuarios.dominio.NombreUsuarioDuplicadoException;
import cl.zzenner.cobranza.usuarios.infraestructura.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
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
}
