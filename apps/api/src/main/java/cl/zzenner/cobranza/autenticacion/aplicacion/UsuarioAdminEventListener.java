package cl.zzenner.cobranza.autenticacion.aplicacion;

import cl.zzenner.cobranza.autenticacion.infraestructura.RefreshTokenRepository;
import cl.zzenner.cobranza.autenticacion.infraestructura.SesionRepository;
import cl.zzenner.cobranza.usuarios.api.SeguridadUsuarioModificadaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Instant;

@Component
public class UsuarioAdminEventListener {

    private final SesionRepository sesionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public UsuarioAdminEventListener(SesionRepository sesionRepository,
                                     RefreshTokenRepository refreshTokenRepository,
                                     Clock clock) {
        this.sesionRepository = sesionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onSeguridadModificada(SeguridadUsuarioModificadaEvent evento) {
        Instant ahora = clock.instant();
        refreshTokenRepository.revocarTodosDeUsuario(evento.usuarioId(), ahora);
        sesionRepository.cerrarActivasPorUsuario(evento.usuarioId(), ahora);
    }
}
