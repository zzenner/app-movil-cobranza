package cl.zzenner.cobranza.feature.asignacion.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cl.zzenner.cobranza.core.database.dao.OperacionConCuotas
import cl.zzenner.cobranza.core.database.dao.OperacionDao
import cl.zzenner.cobranza.core.database.dao.PersonaConDetalle
import cl.zzenner.cobranza.core.database.dao.PersonaDao
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.dao.GestionHistoricaDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class PersonaDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaDao: PersonaDao,
    private val operacionDao: OperacionDao,
    private val gestionHistoricaDao: GestionHistoricaDao,
) : ViewModel() {

    private val personaId: String = checkNotNull(savedStateHandle["personaId"])

    val personaConDetalle: Flow<PersonaConDetalle?> =
        personaDao.getPersonaConDetalle(personaId)

    val operacionesConCuotas: Flow<List<OperacionConCuotas>> =
        operacionDao.getOperacionesConCuotas(personaId)

    val gestionesHistoricas: Flow<List<GestionHistoricaEntity>> =
        gestionHistoricaDao.getGestionesDePersona(personaId)
}
