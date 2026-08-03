package cl.zzenner.cobranza.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.zzenner.cobranza.feature.asignacion.ui.RUTA_LISTA_ASIGNACION
import cl.zzenner.cobranza.feature.asignacion.ui.asignacionNavGraph
import cl.zzenner.cobranza.feature.auth.navigation.authNavGraph
import cl.zzenner.cobranza.feature.busqueda.RUTA_BUSQUEDA_DIRECTA
import cl.zzenner.cobranza.feature.busqueda.busquedaNavGraph
import cl.zzenner.cobranza.feature.gestion.ui.RUTA_HISTORIAL_GESTION
import cl.zzenner.cobranza.feature.gestion.ui.RUTA_FORM_GESTION_BUSQUEDA
import cl.zzenner.cobranza.feature.gestion.ui.gestionNavGraph
import cl.zzenner.cobranza.ui.HomeScreen

/**
 * NavHost principal. Coordina las features sin crear dependencias entre ellas:
 * - authNavGraph: flujo check/login
 * - home: pantalla principal
 * - asignacionNavGraph: lista y detalle de asignación
 * - gestionNavGraph: formulario de gestión e historial
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
                onIrABusqueda = {
                    navController.navigate(RUTA_BUSQUEDA_DIRECTA)
                },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        asignacionNavGraph(
            onNavigateToDetalle = { personaId, asignacionDiariaId ->
                navController.navigate("asignacion/persona/$asignacionDiariaId/$personaId")
            },
            onNavigateBack = { navController.popBackStack() },
            onRegistrarGestion = { personaId, asignacionDiariaId ->
                navController.navigate("gestion/form/$personaId/$asignacionDiariaId")
            },
            onVerHistorial = { personaId ->
                navController.navigate("gestion/historial/$personaId")
            },
        )

        gestionNavGraph(
            onNavigateBack = { navController.popBackStack() },
        )

        busquedaNavGraph(
            onRegistrarGestion = { personaId ->
                navController.navigate("gestion/form/busqueda/$personaId")
            },
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
