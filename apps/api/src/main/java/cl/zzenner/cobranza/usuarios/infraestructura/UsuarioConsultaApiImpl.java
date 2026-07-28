package cl.zzenner.cobranza.usuarios.infraestructura;

import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
class UsuarioConsultaApiImpl implements UsuarioConsultaApi {

    private final UsuarioRepository usuarioRepository;

    UsuarioConsultaApiImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean existeYEstaHabilitado(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(u -> u.estaHabilitado())
                .orElse(false);
    }
}
