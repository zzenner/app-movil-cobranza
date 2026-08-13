package cl.zzenner.cobranza.asignaciones.web;

import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionAdminQueryService;
import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionAdminQueryService.*;
import cl.zzenner.cobranza.asignaciones.aplicacion.AsignacionAdminService;
import cl.zzenner.cobranza.asignaciones.dominio.AsignacionNoEncontradaException;
import cl.zzenner.cobranza.asignaciones.dominio.PersonaFueraDeAsignacionMensualException;
import cl.zzenner.cobranza.asignaciones.dominio.PersonaYaEnAsignacionException;
import cl.zzenner.cobranza.asignaciones.dominio.TransicionEstadoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/asignaciones")
class AsignacionAdminController {

    private final AsignacionAdminQueryService queryService;
    private final AsignacionAdminService adminService;

    AsignacionAdminController(AsignacionAdminQueryService queryService,
                               AsignacionAdminService adminService) {
        this.queryService = queryService;
        this.adminService = adminService;
    }

    // ── Consultas ──────────────────────────────────────────────────────────────

    @GetMapping("/periodos")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_VER')")
    ResponseEntity<List<ItemPeriodo>> listarPeriodos(
            @RequestParam(required = false) UUID carteraId,
            @RequestParam(required = false) UUID supervisorId,
            @RequestParam(required = false) UUID ejecutivoId) {
        return ResponseEntity.ok(queryService.listarPeriodos(carteraId, supervisorId, ejecutivoId));
    }

    @GetMapping("/mensuales")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_VER')")
    ResponseEntity<List<ItemAsignacionMensualAdmin>> listarMensuales(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) UUID carteraId,
            @RequestParam(required = false) UUID supervisorId,
            @RequestParam(required = false) UUID ejecutivoId) {
        return ResponseEntity.ok(queryService.listarMensuales(periodo, carteraId, supervisorId, ejecutivoId));
    }

    @GetMapping("/mensuales/{mensualId}/personas-disponibles")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_VER')")
    ResponseEntity<List<ItemPersonaDisponible>> listarPersonasDisponibles(
            @PathVariable UUID mensualId) {
        return ResponseEntity.ok(queryService.listarPersonasDisponibles(mensualId));
    }

    @GetMapping("/diarias")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_VER')")
    ResponseEntity<List<ItemAsignacionDiariaAdmin>> listarDiarias(
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID carteraId,
            @RequestParam(required = false) UUID supervisorId,
            @RequestParam(required = false) UUID ejecutivoId) {
        return ResponseEntity.ok(queryService.listarDiarias(fecha, estado, carteraId, supervisorId, ejecutivoId));
    }

    @GetMapping("/diarias/{id}")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_VER')")
    ResponseEntity<DetalleAsignacionDiariaAdmin> obtenerDetalle(@PathVariable UUID id) {
        DetalleAsignacionDiariaAdmin detalle = queryService.obtenerDetalleDiaria(id);
        if (detalle == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(detalle);
    }

    // ── Escritura ──────────────────────────────────────────────────────────────

    @PostMapping("/diarias")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_ADMINISTRAR')")
    ResponseEntity<RespuestaCreacion> crearBorrador(
            @Valid @RequestBody SolicitudCrearBorrador solicitud,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest req) {

        UUID supervisorId = UUID.fromString(jwt.getSubject());
        UUID diariaId = adminService.crearBorrador(
                solicitud.asignacionMensualId(), supervisorId,
                solicitud.fecha(), solicitud.personaIds() != null ? solicitud.personaIds() : List.of());

        return ResponseEntity
                .created(URI.create(req.getRequestURI() + "/" + diariaId))
                .body(new RespuestaCreacion(diariaId));
    }

    @PutMapping("/diarias/{id}/personas")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_ADMINISTRAR')")
    ResponseEntity<Void> actualizarPersonas(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudActualizarPersonas solicitud) {

        adminService.actualizarPersonas(id, solicitud.personaIds(), null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/diarias/{id}/publicar")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_ADMINISTRAR')")
    ResponseEntity<Void> publicar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID publicadoPorId = UUID.fromString(jwt.getSubject());
        adminService.publicar(id, publicadoPorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/diarias/{id}/cancelar")
    @PreAuthorize("hasAuthority('PERM_ASIGNACIONES_ADMINISTRAR')")
    ResponseEntity<Void> cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudCancelar solicitud) {

        adminService.cancelar(id, solicitud.motivo());
        return ResponseEntity.noContent().build();
    }

    // ── Manejo de excepciones ──────────────────────────────────────────────────

    @ExceptionHandler(AsignacionNoEncontradaException.class)
    ResponseEntity<ProblemDetail> handleNoEncontrada(
            AsignacionNoEncontradaException ex, HttpServletRequest req) {
        return problemDetail(404, "asignacion-no-encontrada", "Asignación no encontrada",
                ex.getMessage(), "ASIGNACION_NO_ENCONTRADA", req);
    }

    @ExceptionHandler(PersonaYaEnAsignacionException.class)
    ResponseEntity<ProblemDetail> handlePersonaYaEnAsignacion(
            PersonaYaEnAsignacionException ex, HttpServletRequest req) {
        return problemDetail(409, "persona-ya-en-asignacion", "Persona ya asignada",
                ex.getMessage(), "PERSONA_YA_EN_ASIGNACION", req);
    }

    @ExceptionHandler(PersonaFueraDeAsignacionMensualException.class)
    ResponseEntity<ProblemDetail> handlePersonaFuera(
            PersonaFueraDeAsignacionMensualException ex, HttpServletRequest req) {
        return problemDetail(422, "persona-fuera-de-mensual", "Persona fuera de asignación mensual",
                ex.getMessage(), "PERSONA_FUERA_DE_MENSUAL", req);
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    ResponseEntity<ProblemDetail> handleTransicionInvalida(
            TransicionEstadoInvalidaException ex, HttpServletRequest req) {
        return problemDetail(409, "transicion-estado-invalida", "Transición de estado inválida",
                ex.getMessage(), "TRANSICION_ESTADO_INVALIDA", req);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> handleIllegalState(
            IllegalStateException ex, HttpServletRequest req) {
        return problemDetail(422, "operacion-invalida", "Operación inválida",
                ex.getMessage(), "OPERACION_INVALIDA", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        return problemDetail(400, "argumento-invalido", "Argumento inválido",
                ex.getMessage(), "ARGUMENTO_INVALIDO", req);
    }

    private ResponseEntity<ProblemDetail> problemDetail(
            int status, String type, String title, String detail, String code, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create("https://cobranza.zzenner.cl/errores/" + type));
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setProperty("code", code);
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());
        return ResponseEntity.status(status).body(pd);
    }

    // ── DTOs de solicitud/respuesta ────────────────────────────────────────────

    record SolicitudCrearBorrador(
            @NotNull UUID asignacionMensualId,
            @NotNull LocalDate fecha,
            List<UUID> personaIds) {}

    record SolicitudActualizarPersonas(@NotNull List<UUID> personaIds) {}

    record SolicitudCancelar(@NotNull @Size(min = 1, max = 500) String motivo) {}

    record RespuestaCreacion(UUID id) {}
}
