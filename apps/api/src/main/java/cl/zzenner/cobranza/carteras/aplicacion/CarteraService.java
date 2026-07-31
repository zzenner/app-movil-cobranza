package cl.zzenner.cobranza.carteras.aplicacion;

import cl.zzenner.cobranza.carteras.api.CarteraNoEncontradaException;
import cl.zzenner.cobranza.carteras.dominio.Cartera;
import cl.zzenner.cobranza.carteras.infraestructura.CarteraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CarteraService {

    private final CarteraRepository carteraRepository;

    public CarteraService(CarteraRepository carteraRepository) {
        this.carteraRepository = carteraRepository;
    }

    public Cartera registrar(String nombre, String descripcion) {
        var cartera = new Cartera(nombre, descripcion);
        return carteraRepository.save(cartera);
    }

    public Cartera desactivar(UUID id) {
        Cartera cartera = carteraRepository.findById(id)
                .orElseThrow(() -> new CarteraNoEncontradaException(id));
        cartera.desactivar();
        return carteraRepository.save(cartera);
    }

    public Cartera activar(UUID id) {
        Cartera cartera = carteraRepository.findById(id)
                .orElseThrow(() -> new CarteraNoEncontradaException(id));
        cartera.activar();
        return carteraRepository.save(cartera);
    }

    @Transactional(readOnly = true)
    public List<Cartera> listarActivas() {
        return carteraRepository.findAll().stream()
                .filter(Cartera::isActiva)
                .toList();
    }
}
