package cl.zzenner.cobranza.feature.gestion.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zzenner.cobranza.core.database.dao.PersonaConDetalle
import cl.zzenner.cobranza.core.database.dao.PersonaDao
import cl.zzenner.cobranza.core.database.dao.PersonaDirectaDao
import cl.zzenner.cobranza.core.database.entity.PersonaDirectaEntity
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.ErrorValidacion
import cl.zzenner.cobranza.feature.gestion.domain.GestionForm
import cl.zzenner.cobranza.feature.gestion.domain.GestionValidator
import cl.zzenner.cobranza.feature.gestion.domain.OrigenGestion
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
    val identidad: PersonaIdentidadState = PersonaIdentidadState.Cargando,
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

sealed class PersonaIdentidadState {
    data object Cargando : PersonaIdentidadState()
    data class Disponible(val nombre: String, val rutNumero: String, val rutDv: String) : PersonaIdentidadState()
    data object NoDisponible : PersonaIdentidadState()
}

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
    private val personaDirectaDao: PersonaDirectaDao,
    private val repository: GestionRepository,
    private val locationProvider: LocationProvider,
    private val scheduler: GestionSyncScheduler,
) : ViewModel() {

    val personaId: String = checkNotNull(savedStateHandle["personaId"])
    val asignacionDiariaId: String? = savedStateHandle["asignacionDiariaId"]

    private val _state = MutableStateFlow(GestionFormState())
    val state: StateFlow<GestionFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (asignacionDiariaId != null) {
                personaDao.getPersonaConDetalle(personaId).collect { pcd ->
                    _state.update { it.copy(identidad = pcd.toIdentidadState()) }
                }
            } else {
                val pd = personaDirectaDao.findById(personaId)
                _state.update { it.copy(identidad = pd.toIdentidadState()) }
            }
        }
    }

    private fun PersonaConDetalle?.toIdentidadState(): PersonaIdentidadState =
        this?.let { PersonaIdentidadState.Disponible(it.persona.nombre, it.persona.rutNumero, it.persona.rutDv) }
            ?: PersonaIdentidadState.NoDisponible

    private fun PersonaDirectaEntity?.toIdentidadState(): PersonaIdentidadState =
        this?.let { PersonaIdentidadState.Disponible(it.nombre, it.rutNumero, it.rutDv) }
            ?: PersonaIdentidadState.NoDisponible

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
        // guardadoExitoso se mantiene true hasta que la navegación de salida ocurra; sin este
        // chequeo, un segundo tap en la ventana entre "guardado local completo" (isSubmitting
        // vuelve a false) y la navegación efectiva (LaunchedEffect en la pantalla) reenvía el
        // mismo formulario y crea una gestión local duplicada con un UUID distinto.
        if (s.isSubmitting || s.guardadoExitoso) return

        val identidad = s.identidad as? PersonaIdentidadState.Disponible ?: run {
            _state.update { it.copy(errorGeneral = "No se pudo cargar la información de la persona") }
            return
        }

        val ubicacion = (s.gpsState as? GpsState.Capturado)?.ubicacion ?: run {
            _state.update { it.copy(errores = listOf(ErrorValidacion.UbicacionRequerida)) }
            return
        }

        val tipo = s.tipoGestion ?: run {
            _state.update { it.copy(errores = listOf(ErrorValidacion.TipoGestionRequerido)) }
            return
        }

        val origenGestion = if (asignacionDiariaId != null) OrigenGestion.ASIGNACION_DIARIA else OrigenGestion.BUSQUEDA_DIRECTA

        val form = GestionForm(
            personaId = personaId,
            personaRutNumero = identidad.rutNumero,
            personaRutDv = identidad.rutDv,
            personaNombre = identidad.nombre,
            origenGestion = origenGestion,
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
