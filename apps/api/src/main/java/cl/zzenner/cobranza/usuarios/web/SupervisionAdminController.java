package cl.zzenner.cobranza.usuarios.web;

import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionAdminQueryService;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionAdminQueryService.ItemEjecutivoAdmin;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionAdminQueryService.ItemSupervisorAdmin;
import cl.zzenner.cobranza.usuarios.aplicacion.SupervisionService;
import cl.zzenner.cobranza.usuarios.dominio.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/supervision")
class SupervisionAdminController {

    private final SupervisionAdminQueryService queryService;
    private final SupervisionService supervisionService;

    SupervisionAdminController(SupervisionAdminQueryService queryService,
                                SupervisionService supervisionService) {
        this.queryService = queryService;
        this.supervisionService = supervisionService;
    }

    // ── Consultas ──────────────────────────────────────────────────────────────

    @GetMapping("/ejecutivos")
    @PreAuthorize("hasAuthority('PERM_SUPERVISION_VER')")
    ResponseEntity<List<ItemEjecutivoAdmin>> listarEjecutivos(
            @RequestParam(required = false) String nombreUsuario,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String supervisorId,
            @RequestParam(defaultValue = "false") boolean sinSupervisor) {

        return ResponseEntity.ok(queryService.listarEjecutivos(nombreUsuario, codigo, supervisorId, sinSupervisor));
    }

    @GetMapping("/supervisores")
    @PreAuthorize("hasAuthority('PERM_SUPERVISION_VER')")
    ResponseEntity<List<ItemSupervisorAdmin>> listarSupervisores() {
        return ResponseEntity.ok(queryService.listarSupervisoresActivos());
    }

    // ── Asignar / reasignar ────────────────────────────────────────────────────

    @PostMapping("/ejecutivos/{ejecutivoId}/supervisor")
    @PreAuthorize("hasAuthority('PERM_SUPERVISION_ADMINISTRAR')")
    ResponseEntity<Void> asignarOReaasignar(
            @PathVariable UUID ejecutivoId,
            @Valid @RequestBody SolicitudAsignarSupervisor solicitud) {

        UUID supervisorId = solicitud.supervisorId();
        LocalDate hoy = LocalDate.now();

        if (supervisionService.obtenerSupervisionActiva(ejecutivoId).isPresent()) {
            supervisionService.reasignarEjecutivo(supervisorId, ejecutivoId, hoy);
        } else {
            supervisionService.asignarEjecutivo(supervisorId, ejecutivoId, hoy);
        }
        return ResponseEntity.noContent().build();
    }

    // ── Quitar supervisión ─────────────────────────────────────────────────────

    @DeleteMapping("/ejecutivos/{ejecutivoId}/supervisor")
    @PreAuthorize("hasAuthority('PERM_SUPERVISION_ADMINISTRAR')")
    ResponseEntity<Void> quitarSupervision(@PathVariable UUID ejecutivoId) {
        supervisionService.finalizarSupervision(ejecutivoId, LocalDate.now());
        return ResponseEntity.noContent().build();
    }

    // ── Código ejecutivo origen ────────────────────────────────────────────────

    @PatchMapping("/ejecutivos/{ejecutivoId}/codigo")
    @PreAuthorize("hasAuthority('PERM_SUPERVISION_ADMINISTRAR')")
    ResponseEntity<Void> actualizarCodigo(
            @PathVariable UUID ejecutivoId,
            @Valid @RequestBody SolicitudActualizarCodigo solicitud) {

        supervisionService.actualizarCodigoEjecutivo(ejecutivoId, solicitud.codigo());
        return ResponseEntity.noContent().build();
    }

    // ── Manejo de errores ──────────────────────────────────────────────────────

    @ExceptionHandler(RelacionSupervisionInvalidaException.class)
    ResponseEntity<ProblemDetail> handleSupervisionInvalida(
            RelacionSupervisionInvalidaException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(409);
        pd.setType(URI.create("https://cobranza.zzenner.cl/errores/supervision-invalida"));
        pd.setTitle("Operación de supervisión inválida");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", "SUPERVISION_INVALIDA");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());
        return ResponseEntity.status(409).body(pd);
    }

    @ExceptionHandler(CodigoEjecutivoDuplicadoException.class)
    ResponseEntity<ProblemDetail> handleCodigoDuplicado(
            CodigoEjecutivoDuplicadoException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(409);
        pd.setType(URI.create("https://cobranza.zzenner.cl/errores/codigo-ejecutivo-duplicado"));
        pd.setTitle("Código de ejecutivo duplicado");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", "CODIGO_EJECUTIVO_DUPLICADO");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());
        return ResponseEntity.status(409).body(pd);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    ResponseEntity<ProblemDetail> handleUsuarioNoEncontrado(
            UsuarioNoEncontradoException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(404);
        pd.setType(URI.create("https://cobranza.zzenner.cl/errores/usuario-no-encontrado"));
        pd.setTitle("Usuario no encontrado");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", "USUARIO_NO_ENCONTRADO");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());
        return ResponseEntity.status(404).body(pd);
    }

    // ── DTOs de solicitud ──────────────────────────────────────────────────────

    record SolicitudAsignarSupervisor(UUID supervisorId) {
        public SolicitudAsignarSupervisor {
            if (supervisorId == null) throw new IllegalArgumentException("supervisorId es requerido");
        }
    }

    record SolicitudActualizarCodigo(@Size(max = 50) String codigo) {}
}
