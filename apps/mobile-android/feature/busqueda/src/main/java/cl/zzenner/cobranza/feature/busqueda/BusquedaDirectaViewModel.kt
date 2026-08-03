package cl.zzenner.cobranza.feature.busqueda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BusquedaState(
    val rutNumero: String = "",
    val rutDv: String = "",
    val buscando: Boolean = false,
    val rutInvalido: Boolean = false,
    val noEncontrada: Boolean = false,
    val personaEncontradaId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class BusquedaDirectaViewModel @Inject constructor(
    private val repository: BusquedaDirectaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BusquedaState())
    val state: StateFlow<BusquedaState> = _state.asStateFlow()

    fun onRutNumeroChanged(value: String) {
        _state.update {
            it.copy(
                rutNumero = value.filter { c -> c.isDigit() }.take(8),
                rutInvalido = false,
                noEncontrada = false,
                personaEncontradaId = null,
                error = null,
            )
        }
    }

    fun onRutDvChanged(value: String) {
        _state.update {
            it.copy(
                rutDv = value.take(1).uppercase(),
                rutInvalido = false,
                noEncontrada = false,
                personaEncontradaId = null,
                error = null,
            )
        }
    }

    fun buscar() {
        val s = _state.value
        if (s.buscando) return
        _state.update { it.copy(buscando = true, error = null, rutInvalido = false, noEncontrada = false, personaEncontradaId = null) }

        viewModelScope.launch {
            when (val resultado = repository.buscar(s.rutNumero, s.rutDv)) {
                is ResultadoBusqueda.Encontrada ->
                    _state.update { it.copy(buscando = false, personaEncontradaId = resultado.personaId) }
                ResultadoBusqueda.NoEncontrada ->
                    _state.update { it.copy(buscando = false, noEncontrada = true) }
                ResultadoBusqueda.RutInvalido ->
                    _state.update { it.copy(buscando = false, rutInvalido = true) }
                ResultadoBusqueda.SinConexion ->
                    _state.update { it.copy(buscando = false, error = "Sin conexión. Verifique su red e intente nuevamente.") }
                is ResultadoBusqueda.Error ->
                    _state.update { it.copy(buscando = false, error = resultado.mensaje ?: "Error inesperado al buscar") }
            }
        }
    }

    fun limpiarNavegacion() {
        _state.update { it.copy(personaEncontradaId = null) }
    }
}
