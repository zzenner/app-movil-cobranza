package cl.zzenner.cobranza.usuarios.web;

import cl.zzenner.cobranza.usuarios.dominio.Rol;
import cl.zzenner.cobranza.usuarios.infraestructura.RolRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasAuthority('PERM_USUARIOS_ADMINISTRAR')")
class RolAdminController {

    private final RolRepository rolRepository;

    RolAdminController(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @GetMapping
    ResponseEntity<List<ItemRolAdmin>> listar() {
        List<ItemRolAdmin> roles = rolRepository.findAll().stream()
                .filter(Rol::isActivo)
                .map(r -> new ItemRolAdmin(r.getId(), r.getCodigo(), r.getNombre()))
                .sorted((a, b) -> a.nombre().compareTo(b.nombre()))
                .toList();
        return ResponseEntity.ok(roles);
    }
}
