package cl.zzenner.cobranza.gestiones.web;

import cl.zzenner.cobranza.gestiones.aplicacion.ComandoCrearGestion;
import cl.zzenner.cobranza.gestiones.aplicacion.GestionService;
import cl.zzenner.cobranza.gestiones.api.ResultadoRecepcion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gestiones")
@Tag(name = "Gestiones", description = "Recepción de gestiones de cobranza desde dispositivos Android")
@SecurityRequirement(name = "bearerAuth")
class GestionController {

    private final GestionService gestionService;

    GestionController(GestionService gestionService) {
        this.gestionService = gestionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EJECUTIVO_TERRENO')")
    @Operation(
        summary = "Registrar gestión",
        description = "Recibe una gestión generada en el dispositivo Android. Idempotente: el mismo UUID retorna 200 si el contenido es idéntico, 409 si es conflictivo."
    )
    @ApiResponse(responseCode = "201", description = "Gestión insertada")
    @ApiResponse(responseCode = "200", description = "Gestión ya existía (idempotente)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Conflicto: mismo UUID con contenido distinto")
    ResponseEntity<RespuestaCrearGestion> crear(
            @Valid @RequestBody SolicitudCrearGestion solicitud,
            @AuthenticationPrincipal Jwt jwt) {

        UUID ejecutivoId = UUID.fromString(jwt.getSubject());

        ComandoCrearGestion cmd = new ComandoCrearGestion(
            solicitud.id(),
            solicitud.origenGestion().name(),
            solicitud.asignacionDiariaId(),
            solicitud.personaId(),
            ejecutivoId,
            solicitud.tipoGestion().name(),
            solicitud.fechaGestion(),
            solicitud.observacion(),
            solicitud.observacionDireccion(),
            solicitud.latitud(),
            solicitud.longitud(),
            solicitud.precisionMetros().floatValue(),
            solicitud.proveedorGps(),
            solicitud.ubicacionSimulada(),
            solicitud.fechaCapturaGps(),
            solicitud.fechaCompromiso()
        );

        ResultadoRecepcion resultado = gestionService.recibirGestion(cmd);
        RespuestaCrearGestion cuerpo = new RespuestaCrearGestion(solicitud.id(), resultado);

        return resultado == ResultadoRecepcion.INSERTADA
            ? ResponseEntity.status(201).body(cuerpo)
            : ResponseEntity.ok(cuerpo);
    }
}
