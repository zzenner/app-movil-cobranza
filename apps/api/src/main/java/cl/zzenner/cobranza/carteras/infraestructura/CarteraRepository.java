package cl.zzenner.cobranza.carteras.infraestructura;

import cl.zzenner.cobranza.carteras.dominio.Cartera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarteraRepository extends JpaRepository<Cartera, UUID> {}
