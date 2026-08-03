package cl.zzenner.cobranza.feature.asignacion.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val RUTA_LISTA_ASIGNACION = "asignacion/lista"
const val RUTA_DETALLE_PERSONA = "asignacion/persona/{asignacionDiariaId}/{personaId}"

fun NavGraphBuilder.asignacionNavGraph(
    onNavigateToDetalle: (personaId: String, asignacionDiariaId: String) -> Unit,
    onNavigateBack: () -> Unit,
    onRegistrarGestion: (personaId: String, asignacionDiariaId: String) -> Unit,
    onVerHistorial: (personaId: String) -> Unit,
) {
    composable(RUTA_LISTA_ASIGNACION) {
        AsignacionListScreen(onNavigateToDetalle = onNavigateToDetalle)
    }
    composable(RUTA_DETALLE_PERSONA) { backStackEntry ->
        val personaId = backStackEntry.arguments?.getString("personaId") ?: return@composable
        val asignacionDiariaId =
            backStackEntry.arguments?.getString("asignacionDiariaId") ?: return@composable
        PersonaDetalleScreen(
            personaId = personaId,
            asignacionDiariaId = asignacionDiariaId,
            onNavigateBack = onNavigateBack,
            onRegistrarGestion = { onRegistrarGestion(personaId, asignacionDiariaId) },
            onVerHistorial = { onVerHistorial(personaId) },
        )
    }
}
