package cl.zzenner.cobranza.carteras.web;

import cl.zzenner.cobranza.carteras.aplicacion.CarteraService;
import cl.zzenner.cobranza.carteras.dominio.Cartera;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/carteras")
@PreAuthorize("hasAuthority('PERM_DATOS_IMPORTAR')")
class CarteraAdminController {

    private final CarteraService service;

    CarteraAdminController(CarteraService service) {
        this.service = service;
    }

    @GetMapping("/activas")
    ResponseEntity<List<ItemCarteraAdmin>> listarActivas() {
        List<ItemCarteraAdmin> items = service.listarActivas().stream()
                .map(c -> new ItemCarteraAdmin(c.getId(), c.getNombre()))
                .toList();
        return ResponseEntity.ok(items);
    }

    record ItemCarteraAdmin(UUID id, String nombre) {}
}
