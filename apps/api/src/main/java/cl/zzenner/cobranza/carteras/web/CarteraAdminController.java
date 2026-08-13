package cl.zzenner.cobranza.carteras.web;

import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/carteras")
class CarteraAdminController {

    private final CarteraService service;

    CarteraAdminController(CarteraService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CARTERAS_VER')")
    ResponseEntity<List<ItemCatalogoCartera>> listarCatalogo() {
        List<ItemCatalogoCartera> items = service.listarTodas().stream()
                .map(c -> new ItemCatalogoCartera(
                        c.getId(), c.getCodigoOrigen(), c.getNombre(),
                        c.getDescripcion(), c.isActiva()))
                .toList();
        return ResponseEntity.ok(items);
    }

    /** Endpoint legacy: usado por el módulo de importación con DATOS_IMPORTAR. */
    @GetMapping("/activas")
    @PreAuthorize("hasAuthority('PERM_DATOS_IMPORTAR')")
    ResponseEntity<List<ItemCarteraAdmin>> listarActivas() {
        List<ItemCarteraAdmin> items = service.listarActivas().stream()
                .map(c -> new ItemCarteraAdmin(c.getId(), c.getNombre()))
                .toList();
        return ResponseEntity.ok(items);
    }

    record ItemCatalogoCartera(UUID id, String codigoOrigen, String nombre,
                               String descripcion, boolean activa) {}

    record ItemCarteraAdmin(UUID id, String nombre) {}
}
