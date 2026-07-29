package cl.zzenner.cobranza.autenticacion.web;

import cl.zzenner.cobranza.autenticacion.aplicacion.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Login, renovación de token y logout")
class AutenticacionController {

    private final AutenticacionService autenticacionService;

    AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @Operation(summary = "Autenticar usuario y dispositivo")
    @PostMapping("/login")
    ResponseEntity<RespuestaToken> login(@Valid @RequestBody SolicitudLogin solicitud,
                                          HttpServletRequest request) {
        String ip = obtenerIp(request);
        String ua = request.getHeader("User-Agent");
        RespuestaToken respuesta = autenticacionService.login(
                solicitud.nombreUsuario(), solicitud.contrasena(),
                solicitud.dispositivoId(), ip, ua);
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Renovar access token usando refresh token")
    @PostMapping("/refresh")
    ResponseEntity<RespuestaToken> renovar(@Valid @RequestBody SolicitudRenovacion solicitud) {
        return ResponseEntity.ok(autenticacionService.renovar(solicitud.refreshToken()));
    }

    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca el refresh token activo de la sesión. " +
                    "El access token sigue válido hasta su expiración natural (≤15 min). " +
                    "Este comportamiento es un riesgo aceptado del MVP."
    )
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        UUID sesionId = UUID.fromString(jwt.getClaimAsString("sid"));
        autenticacionService.logout(sesionId, Instant.now());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Información del usuario autenticado",
            description = "Devuelve los datos del JWT. Roles y permisos pueden estar desactualizados " +
                    "hasta que el token expire o se renueve."
    )
    @GetMapping("/me")
    ResponseEntity<RespuestaInfoUsuario> me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> permisos = jwt.getClaimAsStringList("permisos");
        var info = new RespuestaInfoUsuario(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString("sid")),
                UUID.fromString(jwt.getClaimAsString("did")),
                jwt.getClaimAsString("preferred_username"),
                roles != null ? roles : List.of(),
                permisos != null ? permisos : List.of());
        return ResponseEntity.ok(info);
    }

    private String obtenerIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
