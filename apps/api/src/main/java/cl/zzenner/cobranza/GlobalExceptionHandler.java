package cl.zzenner.cobranza;

import cl.zzenner.cobranza.asignaciones.api.AsignacionDiariaNoEncontradaException;
import cl.zzenner.cobranza.dispositivos.api.DispositivoDeOtroUsuarioException;
import cl.zzenner.cobranza.gestiones.api.GestionConflictivaException;
import cl.zzenner.cobranza.personas.api.PersonaNoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PersonaNoEncontradaException.class)
    ProblemDetail handlePersonaNoEncontrada(PersonaNoEncontradaException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/persona-no-encontrada"));
        problem.setTitle("Persona no encontrada");
        problem.setDetail(ex.getMessage());
        problem.setProperty("code", "PERSONA_NO_ENCONTRADA");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    @ExceptionHandler(AsignacionDiariaNoEncontradaException.class)
    ProblemDetail handleAsignacionDiariaNoEncontrada(AsignacionDiariaNoEncontradaException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/asignacion-diaria-no-encontrada"));
        problem.setTitle("Asignación diaria no encontrada");
        problem.setDetail(ex.getMessage());
        problem.setProperty("code", "ASIGNACION_DIARIA_NO_ENCONTRADA");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    @ExceptionHandler(GestionConflictivaException.class)
    ProblemDetail handleGestionConflictiva(GestionConflictivaException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/gestion-conflictiva"));
        problem.setTitle("Conflicto de gestión");
        problem.setDetail(ex.getMessage());
        problem.setProperty("code", "GESTION_CONFLICTIVA");
        problem.setProperty("id", ex.getId().toString());
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/argumento-invalido"));
        problem.setTitle("Argumento inválido");
        problem.setDetail(ex.getMessage());
        problem.setProperty("code", "ARGUMENTO_INVALIDO");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/estado-invalido"));
        problem.setTitle("Estado inválido para la operación");
        problem.setDetail(ex.getMessage());
        problem.setProperty("code", "ESTADO_INVALIDO");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    @ExceptionHandler(DispositivoDeOtroUsuarioException.class)
    ProblemDetail handleDispositivoConflicto(DispositivoDeOtroUsuarioException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/conflicto-dispositivo"));
        problem.setTitle("Conflicto de dispositivo");
        problem.setDetail("El dispositivo no puede asociarse a esta sesión.");
        problem.setProperty("code", "CONFLICTO_DISPOSITIVO");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail problem = ex.getBody();
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/validacion"));
        problem.setProperty("code", "VALIDACION");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "inválido",
                        (a, b) -> a));
        problem.setProperty("campos", campos);
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/conflicto-version"));
        problem.setTitle("Conflicto de versión");
        problem.setDetail("El recurso fue modificado por otra operación. Vuelva a cargar y reintente.");
        problem.setProperty("code", "CONFLICTO_VERSION");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/validacion"));
        problem.setTitle("Error de validación");
        problem.setProperty("code", "VALIDACION");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", req.getRequestURI());
        Map<String, String> violaciones = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a));
        problem.setProperty("campos", violaciones);
        return problem;
    }
}
