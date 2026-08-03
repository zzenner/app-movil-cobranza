package cl.zzenner.cobranza.feature.gestion.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val RUTA_FORM_GESTION = "gestion/form/{personaId}/{asignacionDiariaId}"
const val RUTA_HISTORIAL_GESTION = "gestion/historial/{personaId}"

fun NavGraphBuilder.gestionNavGraph(
    onNavigateBack: () -> Unit,
) {
    composable(RUTA_FORM_GESTION) {
        GestionFormScreen(onNavigateBack = onNavigateBack)
    }
    composable(RUTA_HISTORIAL_GESTION) {
        GestionHistorialScreen(onNavigateBack = onNavigateBack)
    }
}
