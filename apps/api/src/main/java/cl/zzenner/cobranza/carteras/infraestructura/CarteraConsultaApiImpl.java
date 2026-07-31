package cl.zzenner.cobranza.carteras.infraestructura;

import cl.zzenner.cobranza.carteras.api.CarteraConsultaApi;
import cl.zzenner.cobranza.carteras.api.DatosCartera;
import cl.zzenner.cobranza.carteras.dominio.Cartera;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class CarteraConsultaApiImpl implements CarteraConsultaApi {

    private final CarteraRepository carteraRepository;

    CarteraConsultaApiImpl(CarteraRepository carteraRepository) {
        this.carteraRepository = carteraRepository;
    }

    @Override
    public Optional<DatosCartera> findById(UUID id) {
        return carteraRepository.findById(id).map(this::toDto);
    }

    @Override
    public boolean existeActiva(UUID id) {
        return carteraRepository.findById(id)
                .map(Cartera::isActiva)
                .orElse(false);
    }

    private DatosCartera toDto(Cartera c) {
        return new DatosCartera(c.getId(), c.getNombre(), c.isActiva());
    }
}
