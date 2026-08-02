package cl.zzenner.cobranza.autenticacion.aplicacion;

import cl.zzenner.cobranza.autenticacion.dominio.RefreshToken;
import cl.zzenner.cobranza.autenticacion.dominio.SesionAutenticacion;
import cl.zzenner.cobranza.autenticacion.infraestructura.RefreshTokenRepository;
import cl.zzenner.cobranza.autenticacion.infraestructura.SesionRepository;
import cl.zzenner.cobranza.autenticacion.web.RespuestaToken;
import cl.zzenner.cobranza.dispositivos.api.DatosDispositivo;
import cl.zzenner.cobranza.dispositivos.api.DispositivoConsultaApi;
import cl.zzenner.cobranza.dispositivos.api.DispositivoDeOtroUsuarioException;
import cl.zzenner.cobranza.dispositivos.api.DispositivoNoValidoException;
import cl.zzenner.cobranza.usuarios.api.CredencialesUsuario;
import cl.zzenner.cobranza.usuarios.api.UsuarioConsultaApi;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AutenticacionService {

    private final UsuarioConsultaApi usuarioConsultaApi;
    private final DispositivoConsultaApi dispositivoConsultaApi;
    private final SesionRepository sesionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GestorTokens gestorTokens;
    private final PropiedadesJwt propiedades;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AutenticacionService(UsuarioConsultaApi usuarioConsultaApi,
                                 DispositivoConsultaApi dispositivoConsultaApi,
                                 SesionRepository sesionRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 GestorTokens gestorTokens,
                                 PropiedadesJwt propiedades,
                                 PasswordEncoder passwordEncoder,
                                 Clock clock) {
        this.usuarioConsultaApi = usuarioConsultaApi;
        this.dispositivoConsultaApi = dispositivoConsultaApi;
        this.sesionRepository = sesionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.gestorTokens = gestorTokens;
        this.propiedades = propiedades;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Autentica usuario y dispositivo, devuelve tokens.
     * Las credenciales se validan ANTES de operar sobre el dispositivo.
     * Todos los errores de usuario retornan la misma excepción genérica (sin enumeración).
     */
    @Transactional
    public RespuestaToken login(String nombreUsuario, String contrasenaCruda,
                                 String identificadorInstalacion, String ipOrigen, String userAgent) {
        CredencialesUsuario credenciales = usuarioConsultaApi.buscarParaAutenticacion(nombreUsuario)
                .orElseThrow(() -> new BadCredentialsException("credenciales incorrectas"));

        Instant ahora = clock.instant();

        // Validaciones con respuesta genérica para evitar enumeración de usuarios
        if (!credenciales.isActivo() || credenciales.isBloqueado()
                || (credenciales.getBloqueadoHasta() != null && ahora.isBefore(credenciales.getBloqueadoHasta()))) {
            throw new BadCredentialsException("credenciales incorrectas");
        }

        if (!passwordEncoder.matches(contrasenaCruda, credenciales.getContrasenaHash())) {
            int intentos = usuarioConsultaApi.registrarIntentoFallido(credenciales.getId());
            if (intentos >= propiedades.maxIntentosFallidos()) {
                usuarioConsultaApi.aplicarBloqueoTemporal(
                        credenciales.getId(), ahora.plus(propiedades.duracionBloqueoTemporal()));
            }
            throw new BadCredentialsException("credenciales incorrectas");
        }

        // Validar o registrar dispositivo (solo después de credenciales válidas).
        // DispositivoDeOtroUsuarioException → 409 (no revela propietario real)
        // DispositivoNoValidoException → 401 genérico
        DatosDispositivo dispositivo;
        try {
            dispositivo = dispositivoConsultaApi.buscarORegistrar(identificadorInstalacion, credenciales.getId());
        } catch (DispositivoDeOtroUsuarioException e) {
            throw e;  // propagado como 409 por GlobalExceptionHandler
        } catch (DispositivoNoValidoException e) {
            throw new BadCredentialsException("credenciales incorrectas");
        }

        usuarioConsultaApi.registrarAccesoExitoso(credenciales.getId());

        // Cerrar sesión previa si existe.
        // saveAndFlush fuerza el UPDATE antes del INSERT de la nueva sesión,
        // evitando que Hibernate viole el índice parcial uq_sesiones_activa_usuario_dispositivo.
        sesionRepository.findActivaByUsuarioIdAndDispositivoId(credenciales.getId(), dispositivo.id())
                .ifPresent(s -> {
                    refreshTokenRepository.revocarActivosDeSesion(s.getId(), ahora);
                    s.cerrar(SesionAutenticacion.MotivoCierre.LOGOUT);
                    sesionRepository.saveAndFlush(s);
                });

        Instant vencimientoAbs = ahora.plus(propiedades.duracionSesionAbsoluta());
        SesionAutenticacion sesion = new SesionAutenticacion(
                credenciales.getId(), dispositivo.id(), ipOrigen, userAgent, vencimientoAbs);
        sesion = sesionRepository.save(sesion);

        return emitirTokens(credenciales, sesion, ahora);
    }

    /**
     * Rota el refresh token. Atómico con bloqueo pesimista.
     * Si el token ya fue consumido → sesión comprometida → revocar todo.
     */
    @Transactional
    public RespuestaToken renovar(String refreshTokenCrudo) {
        String hash = gestorTokens.hashearRefreshToken(refreshTokenCrudo);
        Instant ahora = clock.instant();

        RefreshToken token = refreshTokenRepository.findByHashTokenWithLock(hash)
                .orElseThrow(() -> new BadCredentialsException("token inválido"));

        if (token.getEstado() == RefreshToken.Estado.CONSUMIDO) {
            // Reuse detection: comprometer sesión y revocar todos los tokens
            SesionAutenticacion sesion = sesionRepository.findByIdWithLock(token.getSesionId())
                    .orElseThrow(() -> new BadCredentialsException("token inválido"));
            sesion.comprometer();
            sesionRepository.save(sesion);
            refreshTokenRepository.revocarActivosDeSesion(sesion.getId(), ahora);
            throw new BadCredentialsException("token inválido");
        }

        if (token.getEstado() != RefreshToken.Estado.ACTIVO || !token.estaVigente(ahora)) {
            throw new BadCredentialsException("token inválido");
        }

        SesionAutenticacion sesion = sesionRepository.findByIdWithLock(token.getSesionId())
                .orElseThrow(() -> new BadCredentialsException("token inválido"));

        if (!sesion.estaVigente(ahora)) {
            throw new BadCredentialsException("token inválido");
        }

        token.consumir(ahora);
        // saveAndFlush fuerza el UPDATE (CONSUMIDO) antes del INSERT del nuevo token,
        // evitando violar el índice parcial uq_refresh_tokens_activo_sesion.
        refreshTokenRepository.saveAndFlush(token);
        sesion.actualizarUltimoAcceso(ahora);
        sesionRepository.save(sesion);

        CredencialesUsuario credenciales = usuarioConsultaApi
                .buscarCredencialesPorId(sesion.getUsuarioId())
                .orElseThrow(() -> new BadCredentialsException("token inválido"));

        return emitirTokens(credenciales, sesion, ahora);
    }

    /** Logout idempotente. Si el JWT ya expiró, el filtro habrá bloqueado antes de llegar aquí. */
    @Transactional
    public void logout(UUID sesionId, Instant ahora) {
        Optional<SesionAutenticacion> sesionOpt = sesionRepository.findByIdWithLock(sesionId);
        if (sesionOpt.isEmpty() || !sesionOpt.get().estaActiva()) return;
        SesionAutenticacion sesion = sesionOpt.get();
        refreshTokenRepository.revocarActivosDeSesion(sesionId, ahora);
        sesion.cerrar(SesionAutenticacion.MotivoCierre.LOGOUT);
        sesionRepository.save(sesion);
    }

    private RespuestaToken emitirTokens(CredencialesUsuario credenciales,
                                         SesionAutenticacion sesion, Instant ahora) {
        String accessToken = gestorTokens.emitirAccessToken(
                credenciales, sesion.getId(), sesion.getDispositivoId());

        String refreshCrudo = gestorTokens.generarRefreshTokenCrudo();
        String refreshHash = gestorTokens.hashearRefreshToken(refreshCrudo);
        Instant vencimientoRefresh = gestorTokens.calcularVencimientoRefreshToken(
                ahora, sesion.getFechaVencimientoAbs());

        RefreshToken nuevoRefresh = new RefreshToken(sesion.getId(), refreshHash, vencimientoRefresh);
        refreshTokenRepository.save(nuevoRefresh);

        return new RespuestaToken(
                accessToken, refreshCrudo,
                propiedades.duracionAccessToken().getSeconds(),
                sesion.getFechaVencimientoAbs());
    }
}
