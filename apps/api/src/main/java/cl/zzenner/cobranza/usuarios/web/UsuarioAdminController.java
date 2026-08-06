package cl.zzenner.cobranza.usuarios.web;

import cl.zzenner.cobranza.usuarios.aplicacion.UsuarioAdminService;
import cl.zzenner.cobranza.usuarios.dominio.UsuarioNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasAuthority('PERM_USUARIOS_VER')")
class UsuarioAdminController {

    private final UsuarioAdminService service;

    UsuarioAdminController(UsuarioAdminService service) {
        this.service = service;
    }

    @GetMapping
    ResponseEntity<RespuestaListadoUsuarios> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(required = false) String nombreUsuario,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String rol) {

        if (pagina < 0) throw new IllegalArgumentException("pagina no puede ser negativa");
        if (tamanio < 1 || tamanio > 50)
            throw new IllegalArgumentException("tamanio debe estar entre 1 y 50");
        if (nombreUsuario != null && nombreUsuario.length() > 50)
            throw new IllegalArgumentException("nombreUsuario no puede exceder 50 caracteres");
        validarEstado(estado);
        return ResponseEntity.ok(service.listar(pagina, tamanio, nombreUsuario, estado, rol));
    }

    @GetMapping("/{id}")
    ResponseEntity<DetalleUsuarioAdmin> detalle(@PathVariable String id) {
        UUID uuid = parsearUuid(id);
        return ResponseEntity.ok(service.obtenerDetalle(uuid));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    ResponseEntity<ProblemDetail> handleUsuarioNoEncontrado(
            UsuarioNoEncontradoException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/usuario-no-encontrado"));
        problem.setTitle("Usuario no encontrado");
        problem.setDetail(ex.getMessage());
        problem.setProperty("code", "USUARIO_NO_ENCONTRADO");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return ResponseEntity.status(404).body(problem);
    }

    private UUID parsearUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("UUID inválido: " + id);
        }
    }

    private void validarEstado(String estado) {
        if (estado == null || estado.isBlank()) return;
        switch (estado) {
            case "ACTIVO", "BLOQUEADO_TEMPORAL", "BLOQUEADO", "INACTIVO" -> {}
            default -> throw new IllegalArgumentException(
                    "Estado inválido: " + estado +
                    ". Valores válidos: ACTIVO, BLOQUEADO_TEMPORAL, BLOQUEADO, INACTIVO");
        }
    }
}
