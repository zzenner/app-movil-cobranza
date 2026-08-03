package cl.zzenner.cobranza.feature.asignacion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zzenner.cobranza.core.database.dao.PersonaConDetalle
import cl.zzenner.cobranza.feature.asignacion.data.AsignacionRepository
import cl.zzenner.cobranza.feature.asignacion.domain.PersonaResumen
import cl.zzenner.cobranza.feature.asignacion.domain.SyncMetadataUi
import cl.zzenner.cobranza.feature.asignacion.domain.formatearRut
import cl.zzenner.cobranza.feature.asignacion.domain.normalizarRut
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AsignacionUiState(
    val cargando: Boolean = false,
    val personas: List<PersonaResumen> = emptyList(),
    val syncMetadata: SyncMetadataUi = SyncMetadataUi(),
    val asignacionDiariaId: String? = null,
    val isOnline: Boolean = false,
    val error: String? = null,
    val textoBusqueda: String = "",
)

@HiltViewModel
class AsignacionViewModel @Inject constructor(
    private val repository: AsignacionRepository,
    private val scheduler: AsignacionSyncScheduler,
) : ViewModel() {

    private val _textoBusqueda = MutableStateFlow("")

    val uiState: StateFlow<AsignacionUiState> = combine(
        repository.observePersonasDeAsignacion(),
        repository.observeSyncMetadata(),
        _textoBusqueda,
    ) { personasConDetalle, metadata, busqueda ->
        val todasPersonas = personasConDetalle.map { it.toResumen() }
        val personasFiltradas = if (busqueda.isBlank()) {
            todasPersonas
        } else {
            val query = normalizarRut(busqueda)
            todasPersonas.filter { p ->
                normalizarRut(p.rutFormateado).contains(query) ||
                    p.nombre.contains(busqueda, ignoreCase = true)
            }
        }
        AsignacionUiState(
            personas = personasFiltradas,
            syncMetadata = metadata?.toUi() ?: SyncMetadataUi(),
            asignacionDiariaId = metadata?.asignacionIdAlmacenada,
            textoBusqueda = busqueda,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AsignacionUiState(cargando = true),
    )

    fun sincronizarManual() {
        scheduler.programarInmediato()
    }

    fun onBusquedaChanged(texto: String) {
        _textoBusqueda.update { texto }
    }

    private fun PersonaConDetalle.toResumen() = PersonaResumen(
        id = persona.id,
        nombre = persona.nombre,
        rutFormateado = formatearRut(persona.rutNumero, persona.rutDv),
        numOperaciones = operaciones.size,
    )

    private fun cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity.toUi(): SyncMetadataUi {
        val formateada = ultimaDescargaExitosa?.let { epoch ->
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(Date(epoch))
        }
        return SyncMetadataUi(
            estado = estado,
            ultimaDescargaExitosa = ultimaDescargaExitosa,
            ultimaDescargaFormateada = formateada,
            datosAnterioresDisponibles = datosAnterioresDisponibles,
            datosMarcadosComoDesactualizados = datosMarcadosComoDesactualizados,
            fechaAsignacionAlmacenada = fechaAsignacionAlmacenada,
        )
    }
}
