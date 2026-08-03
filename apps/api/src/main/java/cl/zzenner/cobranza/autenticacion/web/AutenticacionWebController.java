package cl.zzenner.cobranza.autenticacion.web;

import cl.zzenner.cobranza.autenticacion.aplicacion.AutenticacionService;
import cl.zzenner.cobranza.autenticacion.aplicacion.ResultadoAutenticacionWeb;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/web")
@Tag(name = "AutenticacionWeb", description = "Login, renovación de token y logout para el panel administrativo")
class AutenticacionWebController {

    static final String COOKIE_NOMBRE = "rt_web";
    static final String COOKIE_PATH = "/api/v1/auth/web/refresh";

    private final AutenticacionService autenticacionService;

    @Value("${app.web.cookie.secure:false}")
    private boolean cookieSecure;

    AutenticacionWebController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @Operation(summary = "Autenticar usuario web — devuelve access token en body y refresh token en cookie HttpOnly")
    @PostMapping("/login")
    ResponseEntity<RespuestaLoginWeb> login(@Valid @RequestBody SolicitudLoginWeb solicitud,
                                             HttpServletRequest request) {
        ResultadoAutenticacionWeb resultado = autenticacionService.loginWeb(
                solicitud.nombreUsuario(), solicitud.clave(),
                obtenerIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, crearCookieRefresh(resultado.refreshTokenCrudo(),
                        resultado.refreshExpiresAt()).toString())
                .body(new RespuestaLoginWeb(resultado.accessToken(),
                        resultado.expiresInSeconds(), resultado.sessionExpiresAt()));
    }

    @Operation(summary = "Renovar access token usando refresh token de cookie HttpOnly")
    @PostMapping("/refresh")
    ResponseEntity<RespuestaLoginWeb> renovar(
            @CookieValue(name = COOKIE_NOMBRE, required = false) String refreshTokenCrudo) {
        if (refreshTokenCrudo == null || refreshTokenCrudo.isBlank()) {
            throw new BadCredentialsException("token inválido");
        }
        ResultadoAutenticacionWeb resultado = autenticacionService.renovarWeb(refreshTokenCrudo);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, crearCookieRefresh(resultado.refreshTokenCrudo(),
                        resultado.refreshExpiresAt()).toString())
                .body(new RespuestaLoginWeb(resultado.accessToken(),
                        resultado.expiresInSeconds(), resultado.sessionExpiresAt()));
    }

    @Operation(summary = "Cerrar sesión web — revoca tokens y elimina cookie")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        UUID sesionId = UUID.fromString(jwt.getClaimAsString("sid"));
        autenticacionService.logout(sesionId, Instant.now());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, limpiarCookieRefresh().toString())
                .build();
    }

    private ResponseCookie crearCookieRefresh(String tokenCrudo, Instant expira) {
        long maxAge = Math.max(0, Duration.between(Instant.now(), expira).getSeconds());
        return ResponseCookie.from(COOKIE_NOMBRE, tokenCrudo)
                .httpOnly(true)
                .sameSite("Strict")
                .secure(cookieSecure)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie limpiarCookieRefresh() {
        return ResponseCookie.from(COOKIE_NOMBRE, "")
                .httpOnly(true)
                .sameSite("Strict")
                .secure(cookieSecure)
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    private String obtenerIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
