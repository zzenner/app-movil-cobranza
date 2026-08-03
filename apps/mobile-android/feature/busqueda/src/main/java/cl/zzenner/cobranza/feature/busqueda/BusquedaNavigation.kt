package cl.zzenner.cobranza.feature.busqueda

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val RUTA_BUSQUEDA_DIRECTA = "busqueda/directa"

fun NavGraphBuilder.busquedaNavGraph(
    onRegistrarGestion: (personaId: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable(RUTA_BUSQUEDA_DIRECTA) {
        BusquedaDirectaScreen(
            onRegistrarGestion = onRegistrarGestion,
            onNavigateBack = onNavigateBack,
        )
    }
}
