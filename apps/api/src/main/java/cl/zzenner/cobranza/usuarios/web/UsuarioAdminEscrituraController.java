package cl.zzenner.cobranza.usuarios.web;

import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioAdminEscrituraService;
import cl.zzenner.cobranza.usuarios.dominio.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasAuthority('PERM_USUARIOS_ADMINISTRAR')")
class UsuarioAdminEscrituraController {

    private final UsuarioAdminEscrituraService service;

    UsuarioAdminEscrituraController(UsuarioAdminEscrituraService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<RespuestaCrearUsuario> crear(
            @Valid @RequestBody SolicitudCrearUsuario solicitud,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        UUID id = service.crearUsuario(
                solicitud.nombreUsuario(),
                solicitud.nombres(),
                solicitud.apellidoPaterno(),
                solicitud.apellidoMaterno(),
                solicitud.correo(),
                solicitud.contrasena(),
                solicitud.rolesIniciales(),
                actorId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(new RespuestaCrearUsuario(id));
    }

    @PutMapping("/{id}/datos-basicos")
    ResponseEntity<Void> actualizarDatosBasicos(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudActualizarDatosBasicosUsuario solicitud,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        service.actualizarDatosBasicos(
                id,
                solicitud.nombres(),
                solicitud.apellidoPaterno(),
                solicitud.apellidoMaterno(),
                solicitud.correo(),
                solicitud.version(),
                actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activar")
    ResponseEntity<Void> activar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        service.activar(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    ResponseEntity<Void> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        service.desactivar(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/bloquear")
    ResponseEntity<Void> bloquear(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        service.bloquear(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desbloquear")
    ResponseEntity<Void> desbloquear(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        service.desbloquear(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restablecer-contrasena")
    ResponseEntity<Void> restablecerContrasena(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudRestablecerContrasena solicitud,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        service.restablecerContrasena(id, solicitud.nuevaContrasena(), actorId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RolNoEncontradoException.class)
    ResponseEntity<ProblemDetail> handleRolNoEncontrado(
            RolNoEncontradoException ex, HttpServletRequest req) {
        return conflict(400, "rol-no-encontrado", "Rol no encontrado",
                ex.getMessage(), "ROL_NO_ENCONTRADO", req);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    ResponseEntity<ProblemDetail> handleUsuarioNoEncontrado(
            UsuarioNoEncontradoException ex, HttpServletRequest req) {
        return conflict(404, "usuario-no-encontrado", "Usuario no encontrado",
                ex.getMessage(), "USUARIO_NO_ENCONTRADO", req);
    }

    @ExceptionHandler(NombreUsuarioDuplicadoException.class)
    ResponseEntity<ProblemDetail> handleNombreUsuarioDuplicado(
            NombreUsuarioDuplicadoException ex, HttpServletRequest req) {
        return conflict(409, "nombre-usuario-duplicado", "Nombre de usuario duplicado",
                ex.getMessage(), "NOMBRE_USUARIO_DUPLICADO", req);
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    ResponseEntity<ProblemDetail> handleCorreoDuplicado(
            CorreoDuplicadoException ex, HttpServletRequest req) {
        return conflict(409, "correo-duplicado", "Correo electrónico duplicado",
                ex.getMessage(), "CORREO_DUPLICADO", req);
    }

    @ExceptionHandler(AutoLockoutException.class)
    ResponseEntity<ProblemDetail> handleAutoLockout(
            AutoLockoutException ex, HttpServletRequest req) {
        return conflict(409, "operacion-no-permitida-propia-cuenta",
                "Operación no permitida sobre la propia cuenta",
                ex.getMessage(), "OPERACION_NO_PERMITIDA_PROPIA_CUENTA", req);
    }

    @ExceptionHandler(SinAdministradorActivoException.class)
    ResponseEntity<ProblemDetail> handleSinAdministradorActivo(
            SinAdministradorActivoException ex, HttpServletRequest req) {
        return conflict(409, "ultimo-administrador", "Último administrador activo",
                ex.getMessage(), "ULTIMO_ADMINISTRADOR", req);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return conflict(409, "conflicto-version", "Conflicto de versión",
                "El recurso fue modificado por otra operación. Vuelva a cargar y reintente.",
                "CONFLICTO_VERSION", req);
    }

    private ResponseEntity<ProblemDetail> conflict(int status, String tipo, String title,
                                                    String detail, String code,
                                                    HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/" + tipo));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return ResponseEntity.status(status).body(problem);
    }
}
