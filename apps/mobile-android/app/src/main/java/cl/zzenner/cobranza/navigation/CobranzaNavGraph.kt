package cl.zzenner.cobranza.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.zzenner.cobranza.feature.asignacion.ui.RUTA_LISTA_ASIGNACION
import cl.zzenner.cobranza.feature.asignacion.ui.asignacionNavGraph
import cl.zzenner.cobranza.feature.auth.navigation.authNavGraph
import cl.zzenner.cobranza.ui.HomeScreen

/**
 * NavHost principal de la aplicación.
 *
 * Coordina la navegación entre las features sin crear dependencias entre ellas:
 * - authNavGraph: flujo check/login (en :feature:auth)
 * - home: pantalla principal con acceso a asignación (en :app)
 * - asignacionNavGraph: lista y detalle de asignación (en :feature:asignacion)
 *
 * Ninguna feature conoce a otra — toda la wiring está aquí.
 */
@Composable
fun CobranzaNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = "auth") {

        authNavGraph(
            navController = navController,
            onLoginExitoso = {
                navController.navigate("home") {
                    popUpTo("auth") { inclusive = true }
                }
            },
        )

        composable("home") {
            HomeScreen(
                onIrAAsignacion = {
                    navController.navigate(RUTA_LISTA_ASIGNACION)
                },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        asignacionNavGraph(
            onNavigateToDetalle = { personaId ->
                navController.navigate("asignacion/persona/$personaId")
            },
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
}
