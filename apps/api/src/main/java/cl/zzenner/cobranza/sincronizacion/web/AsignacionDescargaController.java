package cl.zzenner.cobranza.sincronizacion.web;

import cl.zzenner.cobranza.sincronizacion.aplicacion.DescargaAsignacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/asignaciones")
@Tag(name = "Asignaciones", description = "Descarga de asignaciones diarias para sincronización con dispositivos Android")
@SecurityRequirement(name = "bearerAuth")
class AsignacionDescargaController {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Santiago");

    private final DescargaAsignacionService descargaService;

    AsignacionDescargaController(DescargaAsignacionService descargaService) {
        this.descargaService = descargaService;
    }

    @GetMapping("/diaria/activa")
    @PreAuthorize("hasRole('EJECUTIVO_TERRENO')")
    @Operation(
        summary = "Descargar asignación diaria activa",
        description = "Retorna el bundle completo (personas, direcciones, avales, operaciones, gestiones) " +
                      "de la asignación diaria PUBLICADA del ejecutivo para la fecha indicada. " +
                      "Si no existe asignación activa para esa fecha retorna 204."
    )
    @ApiResponse(responseCode = "200", description = "Bundle de asignación diaria")
    @ApiResponse(responseCode = "204", description = "Sin asignación activa para la fecha indicada")
    @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido")
    @ApiResponse(responseCode = "403", description = "Sin rol EJECUTIVO_TERRENO")
    ResponseEntity<RespuestaAsignacionDiaria> obtenerActiva(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @AuthenticationPrincipal Jwt jwt) {

        UUID ejecutivoId = UUID.fromString(jwt.getSubject());
        LocalDate fechaConsulta = Optional.ofNullable(fecha)
            .orElseGet(() -> LocalDate.now(descargaService.getClock().withZone(ZONA_NEGOCIO)));

        return descargaService.descargarAsignacionDiariaActiva(ejecutivoId, fechaConsulta)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
