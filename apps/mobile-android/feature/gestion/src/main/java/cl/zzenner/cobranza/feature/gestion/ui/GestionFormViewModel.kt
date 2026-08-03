package cl.zzenner.cobranza.feature.gestion.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zzenner.cobranza.core.database.dao.PersonaDao
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.ErrorValidacion
import cl.zzenner.cobranza.feature.gestion.domain.GestionForm
import cl.zzenner.cobranza.feature.gestion.domain.GestionValidator
import cl.zzenner.cobranza.feature.gestion.domain.TipoGestion
import cl.zzenner.cobranza.feature.gestion.domain.UbicacionCapturada
import cl.zzenner.cobranza.feature.gestion.location.LocationProvider
import cl.zzenner.cobranza.feature.gestion.location.ResultadoUbicacion
import cl.zzenner.cobranza.feature.gestion.location.esSimulada
import cl.zzenner.cobranza.feature.gestion.worker.GestionSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GestionFormState(
    val tipoGestion: TipoGestion? = null,
    val observacion: String = "",
    val observacionDireccion: String = "",
    val fechaCompromiso: String = "",
    val gpsState: GpsState = GpsState.Idle,
    val errores: List<ErrorValidacion> = emptyList(),
    val isSubmitting: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val errorGeneral: String? = null,
)

sealed class GpsState {
    data object Idle : GpsState()
    data object Capturando : GpsState()
    data class Capturado(val ubicacion: UbicacionCapturada) : GpsState()
    data class Error(val mensaje: String) : GpsState()
}

@HiltViewModel
class GestionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaDao: PersonaDao,
    private val repository: GestionRepository,
    private val locationProvider: LocationProvider,
    private val scheduler: GestionSyncScheduler,
) : ViewModel() {

    val personaId: String = checkNotNull(savedStateHandle["personaId"])
    val asignacionDiariaId: String = checkNotNull(savedStateHandle["asignacionDiariaId"])

    private val _state = MutableStateFlow(GestionFormState())
    val state: StateFlow<GestionFormState> = _state.asStateFlow()

    private var personaRutNumero: String = ""
    private var personaRutDv: String = ""
    private var personaNombre: String = ""

    init {
        viewModelScope.launch {
            personaDao.getPersonaConDetalle(personaId).collect { pcd ->
                if (pcd != null) {
                    personaRutNumero = pcd.persona.rutNumero
                    personaRutDv = pcd.persona.rutDv
                    personaNombre = pcd.persona.nombre
                }
            }
        }
    }

    fun onTipoGestionChanged(tipo: TipoGestion) {
        _state.update { it.copy(tipoGestion = tipo, errores = emptyList()) }
    }

    fun onObservacionChanged(texto: String) {
        _state.update { it.copy(observacion = texto) }
    }

    fun onObservacionDireccionChanged(texto: String) {
        _state.update { it.copy(observacionDireccion = texto) }
    }

    fun onFechaCompromisoChanged(fecha: String) {
        _state.update { it.copy(fechaCompromiso = fecha) }
    }

    fun capturarUbicacion() {
        if (_state.value.gpsState is GpsState.Capturando) return
        _state.update { it.copy(gpsState = GpsState.Capturando) }

        viewModelScope.launch {
            when (val resultado = locationProvider.obtenerUbicacion()) {
                is ResultadoUbicacion.Exito -> {
                    val loc = resultado.location
                    _state.update {
                        it.copy(
                            gpsState = GpsState.Capturado(
                                UbicacionCapturada(
                                    latitud = loc.latitude,
                                    longitud = loc.longitude,
                                    precisionMetros = loc.accuracy,
                                    ubicacionSimulada = loc.esSimulada(),
                                    proveedorGps = loc.provider,
                                    fechaCapturaEpoch = loc.time,
                                ),
                            ),
                        )
                    }
                }
                ResultadoUbicacion.PermisoDenegado ->
                    _state.update { it.copy(gpsState = GpsState.Error("Permiso de ubicación denegado")) }
                ResultadoUbicacion.Timeout ->
                    _state.update { it.copy(gpsState = GpsState.Error("No se obtuvo ubicación en 30 segundos")) }
                ResultadoUbicacion.ProveedorDeshabilitado ->
                    _state.update { it.copy(gpsState = GpsState.Error("GPS deshabilitado en el dispositivo")) }
            }
        }
    }

    fun guardar() {
        val s = _state.value
        if (s.isSubmitting) return

        val ubicacion = (s.gpsState as? GpsState.Capturado)?.ubicacion ?: run {
            _state.update { it.copy(errores = listOf(ErrorValidacion.UbicacionRequerida)) }
            return
        }

        val tipo = s.tipoGestion ?: run {
            _state.update { it.copy(errores = listOf(ErrorValidacion.TipoGestionRequerido)) }
            return
        }

        val form = GestionForm(
            personaId = personaId,
            personaRutNumero = personaRutNumero,
            personaRutDv = personaRutDv,
            personaNombre = personaNombre,
            asignacionDiariaId = asignacionDiariaId,
            tipoGestion = tipo,
            observacion = s.observacion.trim().ifBlank { null },
            observacionDireccion = s.observacionDireccion.trim().ifBlank { null },
            fechaCompromiso = s.fechaCompromiso.trim().ifBlank { null },
            ubicacion = ubicacion,
        )

        val errores = GestionValidator.validar(form, LocalDate.now().toString())
        if (errores.isNotEmpty()) {
            _state.update { it.copy(errores = errores) }
            return
        }

        _state.update { it.copy(isSubmitting = true, errores = emptyList()) }

        viewModelScope.launch {
            try {
                repository.guardarLocal(form)
                scheduler.programarEnvioInmediato()
                _state.update { it.copy(isSubmitting = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSubmitting = false, errorGeneral = e.message ?: "Error al guardar")
                }
            }
        }
    }

    fun limpiarError() {
        _state.update { it.copy(errorGeneral = null) }
    }
}
