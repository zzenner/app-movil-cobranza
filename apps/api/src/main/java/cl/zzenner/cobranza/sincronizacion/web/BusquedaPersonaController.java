package cl.zzenner.cobranza.sincronizacion.web;

import cl.zzenner.cobranza.sincronizacion.aplicacion.BusquedaPersonaService;
import cl.zzenner.cobranza.sincronizacion.aplicacion.RutInvalidoEnBusquedaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/personas")
class BusquedaPersonaController {

    private final BusquedaPersonaService service;

    BusquedaPersonaController(BusquedaPersonaService service) {
        this.service = service;
    }

    @PostMapping("/busquedas")
    @PreAuthorize("hasRole('EJECUTIVO_TERRENO')")
    ResponseEntity<RespuestaBusquedaPersona> buscar(
            @Valid @RequestBody SolicitudBusquedaPersona solicitud,
            @AuthenticationPrincipal Jwt jwt) {

        UUID ejecutivoId   = UUID.fromString(jwt.getSubject());
        String correlationId = UUID.randomUUID().toString();

        RespuestaBusquedaPersona respuesta = service.buscar(
                solicitud.rutNumero(), solicitud.rutDv(), ejecutivoId, correlationId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(respuesta);
    }

    @ExceptionHandler(RutInvalidoEnBusquedaException.class)
    ProblemDetail handleRutInvalido(RutInvalidoEnBusquedaException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/rut-invalido"));
        problem.setTitle("RUT inválido");
        problem.setDetail("El RUT proporcionado no es válido");
        problem.setProperty("code", "RUT_INVALIDO");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }
}
