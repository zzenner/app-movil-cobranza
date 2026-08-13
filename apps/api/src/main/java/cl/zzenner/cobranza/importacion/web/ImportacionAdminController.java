package cl.zzenner.cobranza.importacion.web;

import cl.zzenner.cobranza.importacion.aplicacion.ImportacionService;
import cl.zzenner.cobranza.importacion.dominio.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/importaciones/mensuales")
@PreAuthorize("hasAuthority('PERM_DATOS_IMPORTAR')")
class ImportacionAdminController {

    private final ImportacionService service;

    ImportacionAdminController(ImportacionService service) {
        this.service = service;
    }

    // Contrato v2: carteraId y periodo provienen del CSV por fila
    @PostMapping(consumes = "multipart/form-data")
    ResponseEntity<RespuestaCrearImportacion> crear(
            @RequestParam(defaultValue = "LEGADO") String sistemaOrigen,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        UUID importacionId = service.recibirImportacion(sistemaOrigen, archivo, actorId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(importacionId)
                .toUri();

        return ResponseEntity.accepted()
                .location(location)
                .body(new RespuestaCrearImportacion(importacionId, "RECIBIDA",
                        null, archivo.getOriginalFilename()));
    }

    @GetMapping
    ResponseEntity<RespuestaPaginaImportaciones> listar(
            @RequestParam(required = false) UUID carteraId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Page<ImportacionMensual> page = service.listar(carteraId, pagina, tamanio);
        var respuesta = new RespuestaPaginaImportaciones(
                page.getContent().stream().map(ImportacionResumen::desde).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    ResponseEntity<ImportacionDetalle> obtener(@PathVariable UUID id) {
        ImportacionMensual im = service.obtener(id);
        return ResponseEntity.ok(ImportacionDetalle.desde(im));
    }

    @GetMapping("/{id}/errores")
    ResponseEntity<RespuestaPaginaErrores> listarErrores(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {

        Page<ErrorImportacion> page = service.listarErrores(id, pagina, tamanio);
        var respuesta = new RespuestaPaginaErrores(
                page.getContent().stream().map(ErrorImportacionDto::desde).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/confirmar")
    ResponseEntity<Void> confirmar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        service.confirmar(id, actorId);
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(ImportacionMensualNoEncontradaException.class)
    ResponseEntity<ProblemDetail> handleNoEncontrada(
            ImportacionMensualNoEncontradaException ex, HttpServletRequest req) {
        return problem(404, "importacion-no-encontrada", "Importación no encontrada",
                ex.getMessage(), "IMPORTACION_NO_ENCONTRADA", req);
    }

    @ExceptionHandler(EstadoInvalidoParaConfirmarException.class)
    ResponseEntity<ProblemDetail> handleEstadoInvalido(
            EstadoInvalidoParaConfirmarException ex, HttpServletRequest req) {
        return problem(409, "estado-invalido-para-confirmar", "Estado inválido para confirmar",
                ex.getMessage(), "ESTADO_INVALIDO_PARA_CONFIRMAR", req);
    }

    @ExceptionHandler(ArchivoYaImportadoException.class)
    ResponseEntity<ProblemDetail> handleArchivoYaImportado(
            ArchivoYaImportadoException ex, HttpServletRequest req) {
        ProblemDetail pd = buildProblem(409, "archivo-ya-importado",
                "Archivo ya importado", ex.getMessage(), "ARCHIVO_YA_IMPORTADO", req);
        pd.setProperty("importacionId", ex.getImportacionExistenteId().toString());
        return ResponseEntity.status(409).body(pd);
    }

    @ExceptionHandler(CarteraNoActivaException.class)
    ResponseEntity<ProblemDetail> handleCarteraNoActiva(
            CarteraNoActivaException ex, HttpServletRequest req) {
        return problem(404, "cartera-no-encontrada", "Cartera no encontrada o inactiva",
                ex.getMessage(), "CARTERA_NO_ENCONTRADA", req);
    }

    @ExceptionHandler(PeriodoAnteriorNoPermitidoException.class)
    ResponseEntity<ProblemDetail> handlePeriodoAnterior(
            PeriodoAnteriorNoPermitidoException ex, HttpServletRequest req) {
        return problem(422, "periodo-anterior-no-permitido",
                "Período anterior al último completado",
                ex.getMessage(), "PERIODO_ANTERIOR_NO_PERMITIDO", req);
    }

    @ExceptionHandler(ImportacionEnProgresoException.class)
    ResponseEntity<ProblemDetail> handleEnProgreso(
            ImportacionEnProgresoException ex, HttpServletRequest req) {
        ProblemDetail pd = buildProblem(409, "importacion-en-progreso",
                "Ya existe una importación en progreso",
                ex.getMessage(), "IMPORTACION_EN_PROGRESO", req);
        pd.setProperty("importacionEnProgresoId", ex.getImportacionEnProgresoId().toString());
        return ResponseEntity.status(409).body(pd);
    }

    private ResponseEntity<ProblemDetail> problem(int status, String tipo, String title,
                                                   String detail, String code,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(status).body(buildProblem(status, tipo, title, detail, code, req));
    }

    private ProblemDetail buildProblem(int status, String tipo, String title,
                                        String detail, String code, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create("https://cobranza.zzenner.cl/errores/" + tipo));
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setProperty("code", code);
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }
}
