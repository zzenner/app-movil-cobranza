package cl.zzenner.cobranza.feature.asignacion.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val RUTA_LISTA_ASIGNACION = "asignacion/lista"
const val RUTA_DETALLE_PERSONA = "asignacion/persona/{personaId}"

fun NavGraphBuilder.asignacionNavGraph(
    onNavigateToDetalle: (personaId: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable(RUTA_LISTA_ASIGNACION) {
        AsignacionListScreen(onNavigateToDetalle = onNavigateToDetalle)
    }
    composable(RUTA_DETALLE_PERSONA) { backStackEntry ->
        val personaId = backStackEntry.arguments?.getString("personaId") ?: return@composable
        PersonaDetalleScreen(personaId = personaId, onNavigateBack = onNavigateBack)
    }
}
