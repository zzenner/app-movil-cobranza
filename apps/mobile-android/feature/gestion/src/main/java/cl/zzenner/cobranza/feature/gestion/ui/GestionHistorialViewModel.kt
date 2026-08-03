package cl.zzenner.cobranza.feature.gestion.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.GestionResumen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class GestionHistorialViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GestionRepository,
) : ViewModel() {

    private val personaId: String = checkNotNull(savedStateHandle["personaId"])

    val gestiones: Flow<List<GestionResumen>> = repository.observarGestionesPorPersona(personaId)
}
