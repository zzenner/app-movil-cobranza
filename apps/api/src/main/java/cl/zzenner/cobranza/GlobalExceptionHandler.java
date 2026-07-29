package cl.zzenner.cobranza;

import cl.zzenner.cobranza.dispositivos.api.DispositivoDeOtroUsuarioException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DispositivoDeOtroUsuarioException.class)
    ProblemDetail handleDispositivoConflicto(DispositivoDeOtroUsuarioException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/conflicto-dispositivo"));
        problem.setTitle("Conflicto de dispositivo");
        problem.setDetail("El dispositivo no puede asociarse a esta sesión.");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ex.getBody();
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/validacion"));
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "inválido",
                        (a, b) -> a));
        problem.setProperty("campos", campos);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create("https://cobranza.zzenner.cl/errores/validacion"));
        problem.setTitle("Error de validación");
        Map<String, String> violaciones = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a));
        problem.setProperty("campos", violaciones);
        return problem;
    }
}
